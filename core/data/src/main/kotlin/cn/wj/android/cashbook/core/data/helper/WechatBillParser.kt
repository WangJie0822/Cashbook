/*
 * Copyright 2021 The Cashbook Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.wj.android.cashbook.core.data.helper

import cn.wj.android.cashbook.core.common.ext.logger
import cn.wj.android.cashbook.core.model.model.BillDirection
import cn.wj.android.cashbook.core.model.model.BillSummary
import cn.wj.android.cashbook.core.model.model.ImportedBillItem
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.zip.ZipInputStream

/**
 * 微信支付 xlsx 账单解析器
 *
 * 解析逻辑：
 * 1. 用 ZipInputStream 解压 xlsx（本质为 ZIP）
 * 2. 读取 xl/sharedStrings.xml 获取字符串池
 * 3. 读取 xl/worksheets/sheet1.xml 获取单元格数据
 * 4. 跳过前 17 行文件头 + 1 行列标题，从第 19 行开始解析数据
 */
object WechatBillParser {

    /** 微信账单数据起始行（前17行为文件头信息，第18行为列标题） */
    private const val DATA_START_ROW = 19

    /** 微信账单列数 */
    private const val COLUMN_COUNT = 11

    /** 时间格式 */
    private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    /**
     * 解析微信支付 xlsx 账单文件
     *
     * @param inputStream xlsx 文件输入流
     * @return 解析结果，包含账单条目列表和汇总信息；解析失败返回 null
     */
    fun parse(inputStream: InputStream): ParseResult? {
        return try {
            val zipData = readZipEntries(inputStream)
            val sharedStrings = zipData.sharedStrings ?: return null
            val sheetData = zipData.sheetData ?: return null

            val stringPool = parseSharedStrings(sharedStrings)
            val rows = parseSheet(sheetData, stringPool)
            val items = rows.mapNotNull { convertToItem(it) }

            if (items.isEmpty()) return null

            val summary = BillSummary(
                totalCount = items.size,
                incomeCount = items.count { it.direction == BillDirection.INCOME },
                incomeAmount = items.filter { it.direction == BillDirection.INCOME }.sumOf { it.amount },
                expenditureCount = items.count { it.direction == BillDirection.EXPENDITURE },
                expenditureAmount = items.filter { it.direction == BillDirection.EXPENDITURE }.sumOf { it.amount },
            )

            ParseResult(items = items, summary = summary)
        } catch (e: Exception) {
            logger().e(e, "parse wechat bill failed")
            null
        }
    }

    /**
     * 从 ZIP 中读取所需的 XML 数据
     */
    private fun readZipEntries(inputStream: InputStream): ZipData {
        var sharedStrings: ByteArray? = null
        var sheetData: ByteArray? = null

        ZipInputStream(inputStream).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                when (entry.name) {
                    "xl/sharedStrings.xml" -> sharedStrings = zip.readBytes()
                    "xl/worksheets/sheet1.xml" -> sheetData = zip.readBytes()
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        return ZipData(sharedStrings = sharedStrings, sheetData = sheetData)
    }

    /**
     * 解析 sharedStrings.xml，提取字符串池
     *
     * 结构示例：
     * ```xml
     * <sst>
     *   <si><t>文本内容</t></si>
     *   ...
     * </sst>
     * ```
     */
    private fun parseSharedStrings(data: ByteArray): List<String> {
        val strings = mutableListOf<String>()
        val parser = createParser(data)
        var inT = false
        val textBuilder = StringBuilder()

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    if (parser.name == "t") {
                        inT = true
                        textBuilder.clear()
                    }
                }

                XmlPullParser.TEXT -> {
                    if (inT) {
                        textBuilder.append(parser.text)
                    }
                }

                XmlPullParser.END_TAG -> {
                    if (parser.name == "t") {
                        inT = false
                        strings.add(textBuilder.toString())
                    }
                }
            }
            eventType = parser.next()
        }
        return strings
    }

    /**
     * 解析 sheet1.xml，提取从 DATA_START_ROW 开始的行数据
     *
     * 结构示例：
     * ```xml
     * <sheetData>
     *   <row r="19">
     *     <c r="A19" t="s"><v>42</v></c>  <!-- t="s" 表示引用字符串池 -->
     *     <c r="B19" t="d"><v>2026-03-26T11:50:04</v></c>  <!-- t="d" 表示日期 -->
     *     <c r="C19"><v>99.8</v></c>  <!-- 无 t 属性表示数值 -->
     *   </row>
     * </sheetData>
     * ```
     */
    private fun parseSheet(data: ByteArray, stringPool: List<String>): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        val parser = createParser(data)

        var currentRow = 0
        var currentCellType = ""
        var cellValue = ""
        var currentRowCells = mutableListOf<String>()
        var inValue = false

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "row" -> {
                            val rowNum = parser.getAttributeValue(null, "r")?.toIntOrNull() ?: 0
                            currentRow = rowNum
                            currentRowCells = mutableListOf()
                        }

                        "c" -> {
                            currentCellType = parser.getAttributeValue(null, "t") ?: ""
                            cellValue = ""
                        }

                        "v" -> {
                            inValue = true
                            cellValue = ""
                        }
                    }
                }

                XmlPullParser.TEXT -> {
                    if (inValue) {
                        cellValue += parser.text
                    }
                }

                XmlPullParser.END_TAG -> {
                    when (parser.name) {
                        "v" -> {
                            inValue = false
                            val resolvedValue = when (currentCellType) {
                                "s" -> {
                                    // 字符串类型，从字符串池获取
                                    val index = cellValue.toIntOrNull() ?: 0
                                    stringPool.getOrElse(index) { "" }
                                }

                                "d" -> {
                                    // 日期类型，保持原始 ISO 格式
                                    cellValue
                                }

                                else -> {
                                    // 数值或其他
                                    cellValue
                                }
                            }
                            currentRowCells.add(resolvedValue)
                        }

                        "c" -> {
                            // 如果单元格没有 <v> 子元素，补空字符串
                            if (currentRowCells.size < getExpectedCellCount(currentRow, currentRowCells)) {
                                // 不补，因为我们在 row 结束时统一处理
                            }
                        }

                        "row" -> {
                            if (currentRow >= DATA_START_ROW && currentRowCells.isNotEmpty()) {
                                rows.add(currentRowCells)
                            }
                        }
                    }
                }
            }
            eventType = parser.next()
        }
        return rows
    }

    private fun getExpectedCellCount(row: Int, cells: MutableList<String>): Int {
        return cells.size + 1
    }

    /**
     * 将一行数据转换为 ImportedBillItem
     *
     * 列顺序（第18行列标题）：
     * 0: 交易时间, 1: 交易类型, 2: 交易对方, 3: 商品, 4: 收/支,
     * 5: 金额(元), 6: 支付方式, 7: 当前状态, 8: 交易单号, 9: 商户单号, 10: 备注
     */
    private fun convertToItem(row: List<String>): ImportedBillItem? {
        if (row.size < COLUMN_COUNT) return null

        val timeStr = row[0]
        val transactionTime = parseDateTime(timeStr) ?: return null

        val directionStr = row[4].trim()
        val direction = when (directionStr) {
            "收入" -> BillDirection.INCOME
            "支出" -> BillDirection.EXPENDITURE
            else -> return null // 跳过"中性交易"等
        }

        val amountStr = row[5].toString().trim()
        val amount = amountStr.replace("¥", "").replace(",", "").toDoubleOrNull() ?: return null

        val remark = row[10].trim().let { if (it == "/") "" else it }

        return ImportedBillItem(
            transactionTime = transactionTime,
            transactionType = row[1].trim(),
            counterparty = row[2].trim(),
            description = row[3].trim(),
            direction = direction,
            amount = amount,
            paymentMethod = row[6].trim().let { if (it == "/") "" else it },
            status = row[7].trim(),
            transactionId = row[8].trim().let { if (it == "/") "" else it },
            merchantId = row[9].trim().let { if (it == "/") "" else it },
            remark = remark,
        )
    }

    /**
     * 解析日期时间字符串
     *
     * 支持格式：
     * - ISO 格式：2026-03-26T11:50:04（xlsx 的 t="d" 类型）
     * - 标准格式：2026-03-26 11:50:04
     */
    private fun parseDateTime(dateStr: String): Long? {
        return try {
            val normalized = dateStr.replace("T", " ").trim()
            DATE_FORMAT.parse(normalized)?.time
        } catch (e: Exception) {
            logger().e(e, "parse date failed: $dateStr")
            null
        }
    }

    private fun createParser(data: ByteArray): XmlPullParser {
        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = false
        val parser = factory.newPullParser()
        parser.setInput(data.inputStream(), "UTF-8")
        return parser
    }

    /** ZIP 内容数据 */
    private data class ZipData(
        val sharedStrings: ByteArray?,
        val sheetData: ByteArray?,
    )

    /** 解析结果 */
    data class ParseResult(
        val items: List<ImportedBillItem>,
        val summary: BillSummary,
    )
}
