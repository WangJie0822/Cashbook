# 微信账单导入功能实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在备份与恢复页面新增微信账单导入功能，支持从 `.xlsx` 文件解析微信支付账单并批量导入为记录。

**Architecture:** 新建 `feature/record-import` 模块负责导入 UI，`core/data` 新增解析器和批量导入能力。xlsx 通过 ZipInputStream + XmlPullParser 自行解析，零额外依赖。导入流程：选文件 → 解析预览 → 映射配置 → 确认导入。

**Tech Stack:** Kotlin, Jetpack Compose, Room, Hilt, Coroutines/Flow, ZipInputStream, XmlPullParser

---

## 文件结构

### 新建文件

| 文件 | 职责 |
|------|------|
| `core/model/src/main/kotlin/.../model/ImportedBillItem.kt` | 账单条目中间模型 + BillDirection 枚举 |
| `core/model/src/main/kotlin/.../model/PaymentMethodMapping.kt` | 支付方式映射模型 |
| `core/model/src/main/kotlin/.../model/ImportPreviewItem.kt` | 导入预览条目模型 + DuplicateStatus 枚举 |
| `core/model/src/main/kotlin/.../model/BillSummary.kt` | 账单汇总模型 |
| `core/data/src/main/kotlin/.../helper/WechatBillParser.kt` | 微信 xlsx 解析器 |
| `core/data/src/main/kotlin/.../helper/BillCategoryMatcher.kt` | 分类关键词匹配器 |
| `core/data/src/main/kotlin/.../helper/BillPaymentMatcher.kt` | 支付方式自动匹配器 |
| `feature/record-import/build.gradle.kts` | 模块构建配置 |
| `feature/record-import/src/main/AndroidManifest.xml` | 空 manifest |
| `feature/record-import/src/main/kotlin/.../navigation/RecordImportNavigation.kt` | 路由和导航 |
| `feature/record-import/src/main/kotlin/.../viewmodel/RecordImportViewModel.kt` | 状态管理 |
| `feature/record-import/src/main/kotlin/.../screen/RecordImportScreen.kt` | 导入主页面 |
| `feature/record-import/src/main/kotlin/.../component/ImportSummarySection.kt` | 概览区域组件 |
| `feature/record-import/src/main/kotlin/.../component/PaymentMappingSection.kt` | 支付方式映射区域 |
| `feature/record-import/src/main/kotlin/.../component/ImportPreviewList.kt` | 记录预览列表 |

### 修改文件

| 文件 | 修改内容 |
|------|----------|
| `settings.gradle.kts` | 添加 `include(":feature:record-import")` |
| `app/build.gradle.kts` | 添加 `implementation(projects.feature.recordImport)` |
| `app/src/main/kotlin/.../ui/MainApp.kt` | 在 NavHost 中注册导入页面路由 |
| `core/database/src/main/kotlin/.../dao/RecordDao.kt` | 新增按 remark 和日期+金额查重的查询方法 |
| `core/database/src/main/kotlin/.../dao/TransactionDao.kt` | 新增批量插入记录事务方法 |
| `core/data/src/main/kotlin/.../repository/RecordRepository.kt` | 新增 batchImportRecords 方法 |
| `core/data/src/main/kotlin/.../repository/impl/RecordRepositoryImpl.kt` | 实现 batchImportRecords |
| `feature/settings/src/main/kotlin/.../screen/BackupAndRecoveryScreen.kt` | 新增"导入账单"入口区域 |
| `feature/settings/src/main/kotlin/.../navigation/SettingsNavigation.kt` | 添加导航跳转参数 |
| `core/ui/src/main/res/values/strings_settings.xml` | 新增导入相关字符串资源 |

---

### Task 1: 新建 core/model 数据模型

**Files:**
- Create: `core/model/src/main/kotlin/cn/wj/android/cashbook/core/model/model/ImportedBillItem.kt`
- Create: `core/model/src/main/kotlin/cn/wj/android/cashbook/core/model/model/PaymentMethodMapping.kt`
- Create: `core/model/src/main/kotlin/cn/wj/android/cashbook/core/model/model/ImportPreviewItem.kt`
- Create: `core/model/src/main/kotlin/cn/wj/android/cashbook/core/model/model/BillSummary.kt`

- [ ] **Step 1: 创建 ImportedBillItem.kt**

```kotlin
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

package cn.wj.android.cashbook.core.model.model

/**
 * 导入的账单条目（格式无关的中间模型）
 *
 * @param transactionTime 交易时间戳
 * @param transactionType 原始交易类型（如"商户消费"）
 * @param counterparty 交易对方
 * @param description 商品/描述
 * @param direction 收入/支出方向
 * @param amount 金额
 * @param paymentMethod 支付方式原始文本
 * @param status 当前状态
 * @param transactionId 交易单号（用于去重）
 * @param merchantId 商户单号
 * @param remark 备注
 */
data class ImportedBillItem(
    val transactionTime: Long,
    val transactionType: String,
    val counterparty: String,
    val description: String,
    val direction: BillDirection,
    val amount: Double,
    val paymentMethod: String,
    val status: String,
    val transactionId: String,
    val merchantId: String,
    val remark: String,
)

/** 账单收支方向 */
enum class BillDirection {
    /** 收入 */
    INCOME,

    /** 支出 */
    EXPENDITURE,
}
```

- [ ] **Step 2: 创建 PaymentMethodMapping.kt**

```kotlin
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

package cn.wj.android.cashbook.core.model.model

/**
 * 支付方式到资产的映射
 *
 * @param originalName 原始文本，如"民生银行储蓄卡(1420)"
 * @param matchedAssetId 匹配到的资产 ID，-1 表示未匹配
 * @param matchedAssetName 资产名称（用于展示）
 */
data class PaymentMethodMapping(
    val originalName: String,
    val matchedAssetId: Long,
    val matchedAssetName: String,
)
```

- [ ] **Step 3: 创建 ImportPreviewItem.kt**

```kotlin
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

package cn.wj.android.cashbook.core.model.model

/**
 * 单条导入预览
 *
 * @param billItem 原始账单条目
 * @param mappedTypeId 自动匹配的分类 ID
 * @param mappedTypeName 分类名称
 * @param mappedAssetId 映射的资产 ID
 * @param duplicateStatus 重复检测结果
 * @param selected 用户是否选择导入此条
 */
data class ImportPreviewItem(
    val billItem: ImportedBillItem,
    val mappedTypeId: Long,
    val mappedTypeName: String,
    val mappedAssetId: Long,
    val duplicateStatus: DuplicateStatus,
    val selected: Boolean,
)

/** 重复检测结果 */
enum class DuplicateStatus {
    /** 无重复 */
    NONE,

    /** 可能重复（金额+时间匹配） */
    POSSIBLE,

    /** 精确重复（交易单号匹配） */
    EXACT,
}
```

- [ ] **Step 4: 创建 BillSummary.kt**

```kotlin
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

package cn.wj.android.cashbook.core.model.model

/**
 * 账单汇总信息
 *
 * @param totalCount 总记录数
 * @param incomeCount 收入笔数
 * @param incomeAmount 收入总金额
 * @param expenditureCount 支出笔数
 * @param expenditureAmount 支出总金额
 */
data class BillSummary(
    val totalCount: Int,
    val incomeCount: Int,
    val incomeAmount: Double,
    val expenditureCount: Int,
    val expenditureAmount: Double,
)
```

- [ ] **Step 5: 提交**

```bash
git add core/model/src/main/kotlin/cn/wj/android/cashbook/core/model/model/ImportedBillItem.kt \
  core/model/src/main/kotlin/cn/wj/android/cashbook/core/model/model/PaymentMethodMapping.kt \
  core/model/src/main/kotlin/cn/wj/android/cashbook/core/model/model/ImportPreviewItem.kt \
  core/model/src/main/kotlin/cn/wj/android/cashbook/core/model/model/BillSummary.kt
git commit -m "[feat|model|账单导入][公共]新增账单导入相关数据模型"
```

---

### Task 2: 微信 xlsx 解析器

**Files:**
- Create: `core/data/src/main/kotlin/cn/wj/android/cashbook/core/data/helper/WechatBillParser.kt`

- [ ] **Step 1: 创建 WechatBillParser.kt**

解析器负责将微信支付 xlsx 文件解析为 `ImportedBillItem` 列表。xlsx 本质是 ZIP 包，核心解析 `xl/sharedStrings.xml`（字符串池）和 `xl/worksheets/sheet1.xml`（数据）。

```kotlin
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
```

- [ ] **Step 2: 提交**

```bash
git add core/data/src/main/kotlin/cn/wj/android/cashbook/core/data/helper/WechatBillParser.kt
git commit -m "[feat|data|账单导入][公共]新增微信 xlsx 账单解析器"
```

---

### Task 3: 分类关键词匹配器

**Files:**
- Create: `core/data/src/main/kotlin/cn/wj/android/cashbook/core/data/helper/BillCategoryMatcher.kt`

- [ ] **Step 1: 创建 BillCategoryMatcher.kt**

基于交易对方和商品描述做关键词匹配，返回匹配到的分类名称。匹配逻辑依赖运行时查询数据库中的分类列表获取 typeId。

```kotlin
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

import cn.wj.android.cashbook.core.model.model.RecordTypeModel

/**
 * 账单分类关键词匹配器
 *
 * 根据交易对方和商品描述，通过内置关键词规则表匹配分类名称，
 * 再在用户的分类列表中查找对应的 typeId。
 */
object BillCategoryMatcher {

    /**
     * 分类关键词规则表
     *
     * key = 分类名称（需与数据库中一级或二级分类名称匹配）
     * value = 匹配关键词列表
     */
    private val CATEGORY_RULES: Map<String, List<String>> = mapOf(
        "餐饮" to listOf("餐", "饭", "小吃", "奶茶", "咖啡", "外卖", "美团", "饿了么", "肯德基", "麦当劳", "烧烤", "烤串", "火锅", "面馆", "酒楼", "食堂", "快餐", "点点", "蜜雪", "瑞幸", "星巴克", "卡旺卡", "沙县"),
        "交通" to listOf("充电", "加油", "停车", "打车", "滴滴", "高速", "地铁", "公交", "出行", "汽车", "车辆", "ETC", "城泊", "星星充电"),
        "购物" to listOf("超市", "商城", "淘宝", "京东", "拼多多", "天猫", "唯品会", "商店", "百货"),
        "通讯" to listOf("中国电信", "中国移动", "中国联通", "话费"),
        "住房" to listOf("房租", "物业", "水电", "燃气", "公寓", "公共支付平台"),
        "娱乐" to listOf("电影", "游戏", "KTV", "网吧", "音乐", "视频会员"),
        "医疗" to listOf("医院", "药店", "药房", "诊所", "体检"),
        "教育" to listOf("学校", "培训", "课程", "教育", "图书", "书店"),
    )

    /**
     * 匹配分类
     *
     * @param counterparty 交易对方
     * @param description 商品描述
     * @param typeList 用户的分类列表（一级分类）
     * @return 匹配到的分类，未匹配返回 null
     */
    fun match(
        counterparty: String,
        description: String,
        typeList: List<RecordTypeModel>,
    ): RecordTypeModel? {
        val text = "$counterparty $description"
        for ((categoryName, keywords) in CATEGORY_RULES) {
            if (keywords.any { text.contains(it) }) {
                // 在用户的分类列表中查找名称匹配的分类
                return typeList.find { it.name == categoryName }
                    ?: typeList.find { it.name.contains(categoryName) || categoryName.contains(it.name) }
            }
        }
        return null
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add core/data/src/main/kotlin/cn/wj/android/cashbook/core/data/helper/BillCategoryMatcher.kt
git commit -m "[feat|data|账单导入][公共]新增账单分类关键词匹配器"
```

---

### Task 4: 支付方式匹配器

**Files:**
- Create: `core/data/src/main/kotlin/cn/wj/android/cashbook/core/data/helper/BillPaymentMatcher.kt`

- [ ] **Step 1: 创建 BillPaymentMatcher.kt**

```kotlin
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

import cn.wj.android.cashbook.core.model.enums.AssetClassificationEnum
import cn.wj.android.cashbook.core.model.model.AssetModel
import cn.wj.android.cashbook.core.model.model.PaymentMethodMapping

/**
 * 支付方式自动匹配器
 *
 * 将微信账单中的支付方式文本（如"民生银行储蓄卡(1420)"、"零钱"）
 * 匹配到 Cashbook 中已有的资产账户。
 *
 * 匹配策略按优先级：
 * 1. 精确匹配资产名称
 * 2. 关键词匹配银行卡（识别银行名称 + 卡号后四位）
 * 3. 匹配 classification 类型（如"零钱" → WECHAT）
 * 4. 未匹配
 */
object BillPaymentMatcher {

    /** 银行关键词 → AssetClassificationEnum 映射 */
    private val BANK_KEYWORD_MAP: Map<String, AssetClassificationEnum> = mapOf(
        "中国银行" to AssetClassificationEnum.BANK_CARD_ZG,
        "招商" to AssetClassificationEnum.BANK_CARD_ZS,
        "工商" to AssetClassificationEnum.BANK_CARD_GS,
        "农业" to AssetClassificationEnum.BANK_CARD_NY,
        "建设" to AssetClassificationEnum.BANK_CARD_JS,
        "交通银行" to AssetClassificationEnum.BANK_CARD_JT,
        "邮政" to AssetClassificationEnum.BANK_CARD_YZ,
        "邮储" to AssetClassificationEnum.BANK_CARD_YZ,
        "华夏" to AssetClassificationEnum.BANK_CARD_HX,
        "北京" to AssetClassificationEnum.BANK_CARD_BJ,
        "民生" to AssetClassificationEnum.BANK_CARD_MS,
        "光大" to AssetClassificationEnum.BANK_CARD_GD,
        "中信" to AssetClassificationEnum.BANK_CARD_ZX,
        "广发" to AssetClassificationEnum.BANK_CARD_GF,
        "浦发" to AssetClassificationEnum.BANK_CARD_PF,
        "兴业" to AssetClassificationEnum.BANK_CARD_XY,
    )

    /** 特殊支付方式关键词 → classification 映射 */
    private val SPECIAL_PAYMENT_MAP: Map<String, AssetClassificationEnum> = mapOf(
        "零钱" to AssetClassificationEnum.WECHAT,
        "微信" to AssetClassificationEnum.WECHAT,
        "花呗" to AssetClassificationEnum.ANT_CREDIT_PAY,
        "支付宝" to AssetClassificationEnum.ALIPAY,
        "京东白条" to AssetClassificationEnum.JD_IOUS,
    )

    /**
     * 批量匹配所有支付方式
     *
     * @param paymentMethods 去重后的支付方式原始文本列表
     * @param assets 当前账本下的所有资产
     * @return 映射结果列表
     */
    fun matchAll(
        paymentMethods: List<String>,
        assets: List<AssetModel>,
    ): List<PaymentMethodMapping> {
        return paymentMethods.map { method ->
            matchSingle(method, assets)
        }
    }

    /**
     * 匹配单个支付方式
     */
    private fun matchSingle(
        paymentMethod: String,
        assets: List<AssetModel>,
    ): PaymentMethodMapping {
        if (paymentMethod.isBlank()) {
            return PaymentMethodMapping(
                originalName = paymentMethod,
                matchedAssetId = -1L,
                matchedAssetName = "",
            )
        }

        // 策略1：精确匹配资产名称
        assets.find { it.name == paymentMethod }?.let { asset ->
            return PaymentMethodMapping(
                originalName = paymentMethod,
                matchedAssetId = asset.id,
                matchedAssetName = asset.name,
            )
        }

        // 策略2：银行关键词匹配（含卡号后四位）
        val cardSuffix = extractCardSuffix(paymentMethod)
        for ((keyword, classification) in BANK_KEYWORD_MAP) {
            if (paymentMethod.contains(keyword)) {
                val matched = if (cardSuffix != null) {
                    // 优先匹配银行类型 + 卡号
                    assets.find {
                        it.classification == classification && it.cardNo.endsWith(cardSuffix)
                    } ?: assets.find { it.classification == classification }
                } else {
                    assets.find { it.classification == classification }
                }
                if (matched != null) {
                    return PaymentMethodMapping(
                        originalName = paymentMethod,
                        matchedAssetId = matched.id,
                        matchedAssetName = matched.name,
                    )
                }
            }
        }

        // 策略3：特殊支付方式关键词匹配
        for ((keyword, classification) in SPECIAL_PAYMENT_MAP) {
            if (paymentMethod.contains(keyword)) {
                assets.find { it.classification == classification }?.let { asset ->
                    return PaymentMethodMapping(
                        originalName = paymentMethod,
                        matchedAssetId = asset.id,
                        matchedAssetName = asset.name,
                    )
                }
            }
        }

        // 策略4：未匹配
        return PaymentMethodMapping(
            originalName = paymentMethod,
            matchedAssetId = -1L,
            matchedAssetName = "",
        )
    }

    /**
     * 从支付方式文本中提取卡号后四位
     *
     * 示例："民生银行储蓄卡(1420)" → "1420"
     */
    private fun extractCardSuffix(text: String): String? {
        val regex = Regex("""\((\d{4})\)""")
        return regex.find(text)?.groupValues?.getOrNull(1)
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add core/data/src/main/kotlin/cn/wj/android/cashbook/core/data/helper/BillPaymentMatcher.kt
git commit -m "[feat|data|账单导入][公共]新增支付方式自动匹配器"
```

---

### Task 5: 数据库层 - 新增查重和批量插入

**Files:**
- Modify: `core/database/src/main/kotlin/cn/wj/android/cashbook/core/database/dao/RecordDao.kt`
- Modify: `core/database/src/main/kotlin/cn/wj/android/cashbook/core/database/dao/TransactionDao.kt`

- [ ] **Step 1: 在 RecordDao 中新增查重方法**

在 `RecordDao.kt` 接口末尾（`queryImagesByRecordId` 方法之后）添加：

```kotlin
@Query(
    """
    SELECT * FROM db_record
    WHERE books_id=:booksId
    AND remark LIKE '%[微信单号:' || :transactionId || ']%'
""",
)
suspend fun queryByWechatTransactionId(booksId: Long, transactionId: String): List<RecordTable>

@Query(
    """
    SELECT * FROM db_record
    WHERE books_id=:booksId
    AND record_time>=:startTime
    AND record_time<=:endTime
    AND amount=:amount
""",
)
suspend fun queryByTimeAndAmount(
    booksId: Long,
    startTime: Long,
    endTime: Long,
    amount: Double,
): List<RecordTable>
```

- [ ] **Step 2: 在 TransactionDao 中新增批量导入事务方法**

在 `TransactionDao.kt` 接口中添加（在 `insertRecordTransaction` 方法之后）：

```kotlin
/**
 * 批量导入记录事务
 *
 * 在单个事务中插入多条记录并更新对应资产余额。
 *
 * @param records 要插入的记录列表
 * @return 插入后的记录 ID 列表
 */
@Transaction
suspend fun batchImportRecordsTransaction(
    records: List<RecordTable>,
): List<Long> {
    val insertedIds = mutableListOf<Long>()

    // 按资产分组汇总余额变化
    data class BalanceChange(
        val assetId: Long,
        var incomeTotal: java.math.BigDecimal = java.math.BigDecimal.ZERO,
        var expenditureTotal: java.math.BigDecimal = java.math.BigDecimal.ZERO,
    )

    val balanceChanges = mutableMapOf<Long, BalanceChange>()

    for (record in records) {
        val type = queryTypeById(record.typeId) ?: continue
        val category = cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum.ordinalOf(type.typeCategory)

        // 计算实际金额（与 insertRecordTransaction 逻辑一致）
        val recordAmount = if (category == cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum.INCOME) {
            record.amount.toBigDecimal() - record.charge.toBigDecimal()
        } else {
            record.amount.toBigDecimal() + record.charge.toBigDecimal() - record.concessions.toBigDecimal()
        }

        // 插入记录
        val id = insertRecord(record.copy(finalAmount = recordAmount.toDouble()))
        insertedIds.add(id)

        // 累计余额变化
        if (record.assetId > 0) {
            val change = balanceChanges.getOrPut(record.assetId) {
                BalanceChange(assetId = record.assetId)
            }
            if (category == cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum.INCOME) {
                change.incomeTotal += recordAmount
            } else {
                change.expenditureTotal += recordAmount
            }
        }
    }

    // 批量更新资产余额
    for ((assetId, change) in balanceChanges) {
        val asset = queryAssetById(assetId) ?: continue
        val isCreditCard = cn.wj.android.cashbook.core.model.enums.ClassificationTypeEnum.ordinalOf(asset.type) ==
            cn.wj.android.cashbook.core.model.enums.ClassificationTypeEnum.CREDIT_CARD_ACCOUNT

        val balance = if (isCreditCard) {
            // 信用卡：收入减少已用额度，支出增加已用额度
            asset.balance.toBigDecimal() - change.incomeTotal + change.expenditureTotal
        } else {
            // 非信用卡：收入增加余额，支出减少余额
            asset.balance.toBigDecimal() + change.incomeTotal - change.expenditureTotal
        }

        updateAsset(
            asset.copy(
                balance = cn.wj.android.cashbook.core.common.ext.decimalFormat(balance).toDouble(),
            ),
        )
    }

    return insertedIds
}
```

注意：`TransactionDao` 中已有 `queryTypeById`、`queryAssetById`、`updateAsset`、`insertRecord` 方法可直接调用。需确认 `decimalFormat` 扩展函数的具体导入路径，参考已有代码中 `balance.decimalFormat()` 的用法（它是 `BigDecimal` 的扩展函数 `cn.wj.android.cashbook.core.common.ext.decimalFormat`）。

- [ ] **Step 3: 提交**

```bash
git add core/database/src/main/kotlin/cn/wj/android/cashbook/core/database/dao/RecordDao.kt \
  core/database/src/main/kotlin/cn/wj/android/cashbook/core/database/dao/TransactionDao.kt
git commit -m "[feat|database|账单导入][公共]新增账单查重查询和批量导入事务方法"
```

---

### Task 6: Repository 层 - 新增批量导入方法

**Files:**
- Modify: `core/data/src/main/kotlin/cn/wj/android/cashbook/core/data/repository/RecordRepository.kt`
- Modify: `core/data/src/main/kotlin/cn/wj/android/cashbook/core/data/repository/impl/RecordRepositoryImpl.kt`

- [ ] **Step 1: 在 RecordRepository 接口中添加方法**

在 `RecordRepository.kt` 接口末尾添加：

```kotlin
/**
 * 查询指定账本中是否存在包含微信交易单号的记录
 */
suspend fun queryByWechatTransactionId(booksId: Long, transactionId: String): List<RecordModel>

/**
 * 查询指定账本中指定时间范围和金额的记录（用于模糊去重）
 */
suspend fun queryByTimeAndAmount(
    booksId: Long,
    startTime: Long,
    endTime: Long,
    amount: Double,
): List<RecordModel>

/**
 * 批量导入记录
 *
 * @param records 要导入的记录列表（RecordTable 格式）
 * @return 插入后的记录 ID 列表
 */
suspend fun batchImportRecords(records: List<cn.wj.android.cashbook.core.database.table.RecordTable>): List<Long>
```

- [ ] **Step 2: 在 RecordRepositoryImpl 中实现**

在 `RecordRepositoryImpl.kt` 类末尾添加实现方法：

```kotlin
override suspend fun queryByWechatTransactionId(
    booksId: Long,
    transactionId: String,
): List<RecordModel> = withContext(coroutineContext) {
    recordDao.queryByWechatTransactionId(booksId, transactionId).map { it.asModel() }
}

override suspend fun queryByTimeAndAmount(
    booksId: Long,
    startTime: Long,
    endTime: Long,
    amount: Double,
): List<RecordModel> = withContext(coroutineContext) {
    recordDao.queryByTimeAndAmount(booksId, startTime, endTime, amount).map { it.asModel() }
}

override suspend fun batchImportRecords(
    records: List<cn.wj.android.cashbook.core.database.table.RecordTable>,
): List<Long> = withContext(coroutineContext) {
    val ids = transactionDao.batchImportRecordsTransaction(records)
    recordDataVersion.updateVersion()
    assetDataVersion.updateVersion()
    ids
}
```

- [ ] **Step 3: 提交**

```bash
git add core/data/src/main/kotlin/cn/wj/android/cashbook/core/data/repository/RecordRepository.kt \
  core/data/src/main/kotlin/cn/wj/android/cashbook/core/data/repository/impl/RecordRepositoryImpl.kt
git commit -m "[feat|data|账单导入][公共]新增批量导入和查重 Repository 方法"
```

---

### Task 7: 新建 feature/record-import 模块骨架

**Files:**
- Create: `feature/record-import/build.gradle.kts`
- Create: `feature/record-import/src/main/AndroidManifest.xml`
- Modify: `settings.gradle.kts`：在 `include(":feature:settings")` 之后添加 `include(":feature:record-import")`
- Modify: `app/build.gradle.kts`：在 `implementation(projects.feature.settings)` 之后添加 `implementation(projects.feature.recordImport)`

- [ ] **Step 1: 创建 build.gradle.kts**

```kotlin
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
plugins {
    alias(conventionLibs.plugins.cashbook.android.library.feature)
    alias(conventionLibs.plugins.cashbook.android.library.compose)
    alias(conventionLibs.plugins.cashbook.android.library.jacoco)
}

android {
    namespace = "cn.wj.android.cashbook.feature.record.imports"
}

dependencies {

    // 架构
    implementation(projects.core.model)
    implementation(projects.core.common)
    implementation(projects.core.data)
    implementation(projects.core.database)

    testImplementation(projects.core.testing)
}
```

- [ ] **Step 2: 创建 AndroidManifest.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<!--
     Copyright 2021 The Cashbook Open Source Project

     Licensed under the Apache License, Version 2.0 (the "License");
     you may not use this file except in compliance with the License.
     You may obtain a copy of the License at

          http://www.apache.org/licenses/LICENSE-2.0

     Unless required by applicable law or agreed to in writing, software
     distributed under the License is distributed on an "AS IS" BASIS,
     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
     See the License for the specific language governing permissions and
     limitations under the License.
-->
<manifest />
```

- [ ] **Step 3: 在 settings.gradle.kts 注册模块**

在 `include(":feature:settings")` 行之后添加：

```kotlin
include(":feature:record-import")
```

- [ ] **Step 4: 在 app/build.gradle.kts 添加依赖**

在 `implementation(projects.feature.settings)` 行之后添加：

```kotlin
implementation(projects.feature.recordImport)
```

- [ ] **Step 5: 验证 Gradle sync 成功**

```bash
cd /Users/wj/Work/Owner/StudioProjects/Cashbook/.claude/worktrees/record-import && ./gradlew :feature:record-import:assemble --dry-run 2>&1 | tail -5
```

Expected: 任务列表输出（无错误）

- [ ] **Step 6: 提交**

```bash
git add feature/record-import/build.gradle.kts \
  feature/record-import/src/main/AndroidManifest.xml \
  settings.gradle.kts \
  app/build.gradle.kts
git commit -m "[feat|all|账单导入][公共]新建 feature/record-import 模块骨架"
```

---

### Task 8: 字符串资源

**Files:**
- Modify: `core/ui/src/main/res/values/strings_settings.xml`

- [ ] **Step 1: 在 strings_settings.xml 末尾（`</resources>` 之前）添加导入相关字符串**

```xml
    <string name="import_bill">导入账单</string>
    <string name="import_from_wechat">从微信导入</string>
    <string name="import_from_wechat_hint">导入微信支付账单(.xlsx)</string>
    <string name="import_summary_total">共 %1$d 条记录</string>
    <string name="import_summary_income">收入 %1$d 笔 %2$s 元</string>
    <string name="import_summary_expenditure">支出 %1$d 笔 %2$s 元</string>
    <string name="import_target_book">目标账本</string>
    <string name="import_payment_mapping">支付方式映射</string>
    <string name="import_payment_unmapped">未映射</string>
    <string name="import_payment_click_to_select">点击选择资产</string>
    <string name="import_preview_list">记录预览</string>
    <string name="import_duplicate_exact">已存在</string>
    <string name="import_duplicate_possible">可能重复</string>
    <string name="import_selected_count">已选 %1$d 条</string>
    <string name="import_selected_amount">共 ¥%1$s</string>
    <string name="import_confirm">确认导入</string>
    <string name="import_has_unmapped_payment">存在未映射的支付方式</string>
    <string name="import_success">成功导入 %1$d 条记录，跳过 %2$d 条</string>
    <string name="import_parsing">正在解析账单…</string>
    <string name="import_importing">正在导入…</string>
    <string name="import_file_format_error">文件格式不支持，请选择微信支付导出的 xlsx 文件</string>
    <string name="import_select_all">全选</string>
    <string name="import_deselect_all">取消全选</string>
    <string name="import_select_asset">选择资产</string>
    <string name="import_select_category">选择分类</string>
```

- [ ] **Step 2: 提交**

```bash
git add core/ui/src/main/res/values/strings_settings.xml
git commit -m "[feat|UI|账单导入][公共]新增账单导入相关字符串资源"
```

---

### Task 9: Navigation 路由定义

**Files:**
- Create: `feature/record-import/src/main/kotlin/cn/wj/android/cashbook/feature/record/imports/navigation/RecordImportNavigation.kt`

- [ ] **Step 1: 创建 RecordImportNavigation.kt**

```kotlin
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

package cn.wj.android.cashbook.feature.record.imports.navigation

import androidx.compose.material3.SnackbarResult
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import cn.wj.android.cashbook.feature.record.imports.screen.RecordImportRoute

/** 账单导入路由 */
private const val ROUTE_RECORD_IMPORT = "record_import/{fileUri}"

/** 路由参数 - 文件 URI */
private const val KEY_FILE_URI = "fileUri"

/**
 * 跳转到账单导入界面
 *
 * @param fileUri 选择的账单文件 URI（需要 URL 编码）
 */
fun NavController.naviToRecordImport(fileUri: String) {
    this.navigate("record_import/${java.net.URLEncoder.encode(fileUri, "UTF-8")}")
}

/**
 * 账单导入界面
 *
 * @param onRequestPopBackStack 导航到上一级
 * @param onShowSnackbar 显示提示
 */
fun NavGraphBuilder.recordImportScreen(
    onRequestPopBackStack: () -> Unit,
    onShowSnackbar: suspend (String, String?) -> SnackbarResult,
) {
    composable(
        route = ROUTE_RECORD_IMPORT,
        arguments = listOf(
            navArgument(KEY_FILE_URI) {
                type = NavType.StringType
            },
        ),
    ) {
        RecordImportRoute(
            onRequestPopBackStack = onRequestPopBackStack,
            onShowSnackbar = onShowSnackbar,
        )
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add feature/record-import/src/main/kotlin/cn/wj/android/cashbook/feature/record/imports/navigation/RecordImportNavigation.kt
git commit -m "[feat|record-import|账单导入][公共]新增账单导入路由定义"
```

---

### Task 10: ViewModel

**Files:**
- Create: `feature/record-import/src/main/kotlin/cn/wj/android/cashbook/feature/record/imports/viewmodel/RecordImportViewModel.kt`

- [ ] **Step 1: 创建 RecordImportViewModel.kt**

```kotlin
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

package cn.wj.android.cashbook.feature.record.imports.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.wj.android.cashbook.core.common.annotation.CashbookDispatchers
import cn.wj.android.cashbook.core.common.annotation.Dispatcher
import cn.wj.android.cashbook.core.common.ext.logger
import cn.wj.android.cashbook.core.common.ext.toBigDecimalOrZero
import cn.wj.android.cashbook.core.data.helper.BillCategoryMatcher
import cn.wj.android.cashbook.core.data.helper.BillPaymentMatcher
import cn.wj.android.cashbook.core.data.helper.WechatBillParser
import cn.wj.android.cashbook.core.data.repository.AssetRepository
import cn.wj.android.cashbook.core.data.repository.BooksRepository
import cn.wj.android.cashbook.core.data.repository.RecordRepository
import cn.wj.android.cashbook.core.data.repository.TypeRepository
import cn.wj.android.cashbook.core.database.table.RecordTable
import cn.wj.android.cashbook.core.model.model.BillDirection
import cn.wj.android.cashbook.core.model.model.BillSummary
import cn.wj.android.cashbook.core.model.model.BooksModel
import cn.wj.android.cashbook.core.model.model.DuplicateStatus
import cn.wj.android.cashbook.core.model.model.ImportPreviewItem
import cn.wj.android.cashbook.core.model.model.ImportedBillItem
import cn.wj.android.cashbook.core.model.model.PaymentMethodMapping
import cn.wj.android.cashbook.core.model.model.RecordTypeModel
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.net.URLDecoder
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

/**
 * 账单导入 ViewModel
 */
@HiltViewModel
class RecordImportViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val application: Application,
    private val recordRepository: RecordRepository,
    private val typeRepository: TypeRepository,
    private val assetRepository: AssetRepository,
    private val booksRepository: BooksRepository,
    @Dispatcher(CashbookDispatchers.IO) private val coroutineContext: CoroutineContext,
) : ViewModel() {

    private val fileUri: String = URLDecoder.decode(
        savedStateHandle.get<String>("fileUri") ?: "",
        "UTF-8",
    )

    private val _uiState = MutableStateFlow<RecordImportUiState>(RecordImportUiState.Parsing)
    val uiState: StateFlow<RecordImportUiState> = _uiState

    private var parsedItems: List<ImportedBillItem> = emptyList()
    private var parsedSummary: BillSummary? = null
    private var expenditureTypes: List<RecordTypeModel> = emptyList()
    private var incomeTypes: List<RecordTypeModel> = emptyList()
    private var defaultExpenditureType: RecordTypeModel? = null
    private var defaultIncomeType: RecordTypeModel? = null

    init {
        parseFile()
    }

    private fun parseFile() {
        viewModelScope.launch {
            _uiState.value = RecordImportUiState.Parsing
            try {
                val uri = Uri.parse(fileUri)
                val inputStream = application.contentResolver.openInputStream(uri)
                    ?: run {
                        _uiState.value = RecordImportUiState.Error("")
                        return@launch
                    }

                val result = inputStream.use { WechatBillParser.parse(it) }
                if (result == null) {
                    _uiState.value = RecordImportUiState.Error("")
                    return@launch
                }

                parsedItems = result.items
                parsedSummary = result.summary

                // 加载依赖数据
                val booksList = booksRepository.booksListData.first()
                val currentBook = booksRepository.currentBook.first()
                expenditureTypes = typeRepository.firstExpenditureTypeListData.first()
                incomeTypes = typeRepository.firstIncomeTypeListData.first()
                defaultExpenditureType = expenditureTypes.lastOrNull()
                defaultIncomeType = incomeTypes.lastOrNull()

                // 匹配支付方式
                val assets = assetRepository.getVisibleAssetsByBookId(currentBook.id)
                val paymentMethods = parsedItems.map { it.paymentMethod }.filter { it.isNotBlank() }.distinct()
                val mappings = BillPaymentMatcher.matchAll(paymentMethods, assets)

                // 构建映射表：支付方式原始文本 → assetId
                val mappingMap = mappings.associate { it.originalName to it.matchedAssetId }

                // 检测重复 + 匹配分类
                val previewItems = buildPreviewItems(
                    items = parsedItems,
                    booksId = currentBook.id,
                    mappingMap = mappingMap,
                )

                _uiState.value = RecordImportUiState.Ready(
                    fileName = uri.lastPathSegment ?: "",
                    summary = result.summary,
                    selectedBooksId = currentBook.id,
                    booksList = booksList,
                    paymentMappings = mappings,
                    previewItems = previewItems,
                    hasUnmappedPayments = mappings.any { it.matchedAssetId == -1L },
                    visibleAssets = assets,
                    expenditureTypes = expenditureTypes,
                    incomeTypes = incomeTypes,
                )
            } catch (e: Exception) {
                logger().e(e, "parseFile failed")
                _uiState.value = RecordImportUiState.Error("")
            }
        }
    }

    private suspend fun buildPreviewItems(
        items: List<ImportedBillItem>,
        booksId: Long,
        mappingMap: Map<String, Long>,
    ): List<ImportPreviewItem> {
        return items.map { item ->
            // 匹配分类
            val types = if (item.direction == BillDirection.EXPENDITURE) expenditureTypes else incomeTypes
            val defaultType = if (item.direction == BillDirection.EXPENDITURE) defaultExpenditureType else defaultIncomeType
            val matchedType = BillCategoryMatcher.match(item.counterparty, item.description, types) ?: defaultType

            // 检测重复
            val duplicateStatus = checkDuplicate(item, booksId)

            // 映射资产
            val assetId = if (item.paymentMethod.isBlank()) -1L else (mappingMap[item.paymentMethod] ?: -1L)

            ImportPreviewItem(
                billItem = item,
                mappedTypeId = matchedType?.id ?: -1L,
                mappedTypeName = matchedType?.name ?: "",
                mappedAssetId = assetId,
                duplicateStatus = duplicateStatus,
                selected = duplicateStatus != DuplicateStatus.EXACT,
            )
        }
    }

    private suspend fun checkDuplicate(
        item: ImportedBillItem,
        booksId: Long,
    ): DuplicateStatus {
        // 精确匹配：交易单号
        if (item.transactionId.isNotBlank()) {
            val existing = recordRepository.queryByWechatTransactionId(booksId, item.transactionId)
            if (existing.isNotEmpty()) return DuplicateStatus.EXACT
        }

        // 模糊匹配：同天 + 同金额
        val dayStart = item.transactionTime / 86400000 * 86400000 // 当天0点
        val dayEnd = dayStart + 86400000 - 1 // 当天23:59:59
        val similar = recordRepository.queryByTimeAndAmount(booksId, dayStart, dayEnd, item.amount)
        if (similar.isNotEmpty()) return DuplicateStatus.POSSIBLE

        return DuplicateStatus.NONE
    }

    /** 切换目标账本 */
    fun selectBook(booksId: Long) {
        val state = _uiState.value as? RecordImportUiState.Ready ?: return
        viewModelScope.launch {
            val assets = assetRepository.getVisibleAssetsByBookId(booksId)
            val paymentMethods = parsedItems.map { it.paymentMethod }.filter { it.isNotBlank() }.distinct()
            val mappings = BillPaymentMatcher.matchAll(paymentMethods, assets)
            val mappingMap = mappings.associate { it.originalName to it.matchedAssetId }

            val previewItems = buildPreviewItems(parsedItems, booksId, mappingMap)

            _uiState.value = state.copy(
                selectedBooksId = booksId,
                paymentMappings = mappings,
                previewItems = previewItems,
                hasUnmappedPayments = mappings.any { it.matchedAssetId == -1L },
                visibleAssets = assets,
            )
        }
    }

    /** 更新支付方式映射 */
    fun updatePaymentMapping(originalName: String, assetId: Long, assetName: String) {
        val state = _uiState.value as? RecordImportUiState.Ready ?: return
        val updatedMappings = state.paymentMappings.map { mapping ->
            if (mapping.originalName == originalName) {
                mapping.copy(matchedAssetId = assetId, matchedAssetName = assetName)
            } else {
                mapping
            }
        }
        // 同步更新预览条目中的 assetId
        val mappingMap = updatedMappings.associate { it.originalName to it.matchedAssetId }
        val updatedPreviews = state.previewItems.map { preview ->
            val newAssetId = if (preview.billItem.paymentMethod.isBlank()) {
                -1L
            } else {
                mappingMap[preview.billItem.paymentMethod] ?: -1L
            }
            preview.copy(mappedAssetId = newAssetId)
        }
        _uiState.value = state.copy(
            paymentMappings = updatedMappings,
            previewItems = updatedPreviews,
            hasUnmappedPayments = updatedMappings.any { it.matchedAssetId == -1L },
        )
    }

    /** 切换单条记录的选中状态 */
    fun toggleItemSelection(index: Int) {
        val state = _uiState.value as? RecordImportUiState.Ready ?: return
        val updatedItems = state.previewItems.toMutableList()
        val item = updatedItems[index]
        updatedItems[index] = item.copy(selected = !item.selected)
        _uiState.value = state.copy(previewItems = updatedItems)
    }

    /** 全选/取消全选 */
    fun selectAll(selected: Boolean) {
        val state = _uiState.value as? RecordImportUiState.Ready ?: return
        val updatedItems = state.previewItems.map { it.copy(selected = selected) }
        _uiState.value = state.copy(previewItems = updatedItems)
    }

    /** 更新单条记录的分类 */
    fun updateItemType(index: Int, typeId: Long, typeName: String) {
        val state = _uiState.value as? RecordImportUiState.Ready ?: return
        val updatedItems = state.previewItems.toMutableList()
        updatedItems[index] = updatedItems[index].copy(mappedTypeId = typeId, mappedTypeName = typeName)
        _uiState.value = state.copy(previewItems = updatedItems)
    }

    /** 确认导入 */
    fun confirmImport() {
        val state = _uiState.value as? RecordImportUiState.Ready ?: return
        val selectedItems = state.previewItems.filter { it.selected }
        if (selectedItems.isEmpty()) return

        viewModelScope.launch {
            _uiState.value = RecordImportUiState.Importing

            try {
                val records = selectedItems.map { preview ->
                    val remarkText = buildString {
                        append(preview.billItem.counterparty)
                        if (preview.billItem.description.isNotBlank()) {
                            append(" - ")
                            append(preview.billItem.description)
                        }
                        if (preview.billItem.transactionId.isNotBlank()) {
                            append(" [微信单号:")
                            append(preview.billItem.transactionId)
                            append("]")
                        }
                    }

                    RecordTable(
                        id = null,
                        typeId = preview.mappedTypeId,
                        assetId = preview.mappedAssetId,
                        intoAssetId = -1L,
                        booksId = state.selectedBooksId,
                        amount = preview.billItem.amount,
                        finalAmount = preview.billItem.amount,
                        concessions = 0.0,
                        charge = 0.0,
                        remark = remarkText,
                        reimbursable = cn.wj.android.cashbook.core.common.SWITCH_INT_OFF,
                        recordTime = preview.billItem.transactionTime,
                    )
                }

                val ids = recordRepository.batchImportRecords(records)
                val skipped = state.previewItems.size - selectedItems.size
                _uiState.value = RecordImportUiState.Done(
                    imported = ids.size,
                    skipped = skipped,
                )
            } catch (e: Exception) {
                logger().e(e, "import failed")
                _uiState.value = RecordImportUiState.Error("")
            }
        }
    }
}

/** 导入界面 UI 状态 */
sealed interface RecordImportUiState {
    /** 解析中 */
    data object Parsing : RecordImportUiState

    /** 就绪，可配置映射和预览 */
    data class Ready(
        val fileName: String,
        val summary: BillSummary,
        val selectedBooksId: Long,
        val booksList: List<BooksModel>,
        val paymentMappings: List<PaymentMethodMapping>,
        val previewItems: List<ImportPreviewItem>,
        val hasUnmappedPayments: Boolean,
        val visibleAssets: List<cn.wj.android.cashbook.core.model.model.AssetModel>,
        val expenditureTypes: List<RecordTypeModel>,
        val incomeTypes: List<RecordTypeModel>,
    ) : RecordImportUiState

    /** 导入中 */
    data object Importing : RecordImportUiState

    /** 导入完成 */
    data class Done(val imported: Int, val skipped: Int) : RecordImportUiState

    /** 错误 */
    data class Error(val message: String) : RecordImportUiState
}
```

- [ ] **Step 2: 提交**

```bash
git add feature/record-import/src/main/kotlin/cn/wj/android/cashbook/feature/record/imports/viewmodel/RecordImportViewModel.kt
git commit -m "[feat|record-import|账单导入][公共]新增账单导入 ViewModel"
```

---

### Task 11: UI 组件 - 概览和映射区域

**Files:**
- Create: `feature/record-import/src/main/kotlin/cn/wj/android/cashbook/feature/record/imports/component/ImportSummarySection.kt`
- Create: `feature/record-import/src/main/kotlin/cn/wj/android/cashbook/feature/record/imports/component/PaymentMappingSection.kt`

- [ ] **Step 1: 创建 ImportSummarySection.kt**

```kotlin
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

package cn.wj.android.cashbook.feature.record.imports.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cn.wj.android.cashbook.core.model.model.BillSummary
import cn.wj.android.cashbook.core.model.model.BooksModel
import cn.wj.android.cashbook.core.ui.R

/**
 * 导入概览区域
 *
 * @param fileName 文件名
 * @param summary 账单汇总
 * @param selectedBooksId 当前选中的账本 ID
 * @param booksList 可选账本列表
 * @param onBookSelected 账本选择回调
 */
@Composable
internal fun ImportSummarySection(
    fileName: String,
    summary: BillSummary,
    selectedBooksId: Long,
    booksList: List<BooksModel>,
    onBookSelected: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            text = fileName,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Text(
            text = stringResource(R.string.import_summary_total, summary.totalCount),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 4.dp),
        )

        Row(modifier = Modifier.padding(top = 2.dp)) {
            Text(
                text = stringResource(
                    R.string.import_summary_income,
                    summary.incomeCount,
                    "%.2f".format(summary.incomeAmount),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "  ",
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = stringResource(
                    R.string.import_summary_expenditure,
                    summary.expenditureCount,
                    "%.2f".format(summary.expenditureAmount),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        // 账本选择器
        val selectedBook = booksList.find { it.id == selectedBooksId }
        var expanded by remember { mutableStateOf(false) }
        Row(
            modifier = Modifier
                .padding(top = 8.dp)
                .clickable { expanded = true },
        ) {
            Text(
                text = stringResource(R.string.import_target_book) + "：",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = selectedBook?.name ?: "",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                booksList.forEach { book ->
                    DropdownMenuItem(
                        text = { Text(text = book.name) },
                        onClick = {
                            expanded = false
                            onBookSelected(book.id)
                        },
                    )
                }
            }
        }
    }
}
```

- [ ] **Step 2: 创建 PaymentMappingSection.kt**

```kotlin
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

package cn.wj.android.cashbook.feature.record.imports.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cn.wj.android.cashbook.core.design.icon.CbIcons
import cn.wj.android.cashbook.core.model.model.PaymentMethodMapping
import cn.wj.android.cashbook.core.ui.R

/**
 * 支付方式映射区域（可折叠）
 *
 * @param mappings 支付方式映射列表
 * @param onMappingClick 点击某个映射项，弹出资产选择
 */
@Composable
internal fun PaymentMappingSection(
    mappings: List<PaymentMethodMapping>,
    onMappingClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(true) }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.import_payment_mapping),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = if (expanded) CbIcons.KeyboardArrowUp else CbIcons.KeyboardArrowDown,
                contentDescription = null,
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column {
                mappings.forEach { mapping ->
                    PaymentMappingItem(
                        mapping = mapping,
                        onClick = { onMappingClick(mapping.originalName) },
                    )
                }
            }
        }
    }
}

@Composable
private fun PaymentMappingItem(
    mapping: PaymentMethodMapping,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isUnmapped = mapping.matchedAssetId == -1L
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = mapping.originalName,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = " → ",
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = if (isUnmapped) {
                stringResource(R.string.import_payment_unmapped)
            } else {
                mapping.matchedAssetName
            },
            style = MaterialTheme.typography.bodyMedium,
            color = if (isUnmapped) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.primary
            },
        )
    }
}
```

- [ ] **Step 3: 提交**

```bash
git add feature/record-import/src/main/kotlin/cn/wj/android/cashbook/feature/record/imports/component/ImportSummarySection.kt \
  feature/record-import/src/main/kotlin/cn/wj/android/cashbook/feature/record/imports/component/PaymentMappingSection.kt
git commit -m "[feat|record-import|账单导入][公共]新增导入概览和支付方式映射 UI 组件"
```

---

### Task 12: UI 组件 - 预览列表

**Files:**
- Create: `feature/record-import/src/main/kotlin/cn/wj/android/cashbook/feature/record/imports/component/ImportPreviewList.kt`

- [ ] **Step 1: 创建 ImportPreviewList.kt**

```kotlin
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

package cn.wj.android.cashbook.feature.record.imports.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cn.wj.android.cashbook.core.common.tools.DATE_FORMAT_NO_SECONDS
import cn.wj.android.cashbook.core.common.tools.dateFormat
import cn.wj.android.cashbook.core.model.model.BillDirection
import cn.wj.android.cashbook.core.model.model.DuplicateStatus
import cn.wj.android.cashbook.core.model.model.ImportPreviewItem
import cn.wj.android.cashbook.core.ui.R

/**
 * 记录预览列表
 *
 * @param items 预览条目列表
 * @param onToggleSelection 切换选中状态
 * @param onTypeClick 点击分类标签
 * @param listState LazyColumn 状态
 */
@Composable
internal fun ImportPreviewList(
    items: List<ImportPreviewItem>,
    onToggleSelection: (Int) -> Unit,
    onTypeClick: (Int) -> Unit,
    listState: LazyListState,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        state = listState,
        modifier = modifier,
    ) {
        itemsIndexed(items) { index, item ->
            ImportPreviewRow(
                item = item,
                onToggleSelection = { onToggleSelection(index) },
                onTypeClick = { onTypeClick(index) },
            )
        }
    }
}

@Composable
private fun ImportPreviewRow(
    item: ImportPreviewItem,
    onToggleSelection: () -> Unit,
    onTypeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onToggleSelection)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = item.selected,
            onCheckedChange = { onToggleSelection() },
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = item.billItem.counterparty,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Text(
                    text = "${if (item.billItem.direction == BillDirection.EXPENDITURE) "-" else "+"}¥${"%.2f".format(item.billItem.amount)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (item.billItem.direction == BillDirection.EXPENDITURE) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = item.billItem.transactionTime.dateFormat(DATE_FORMAT_NO_SECONDS),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // 重复状态标签
                    when (item.duplicateStatus) {
                        DuplicateStatus.EXACT -> {
                            DuplicateTag(
                                text = stringResource(R.string.import_duplicate_exact),
                                isExact = true,
                            )
                        }

                        DuplicateStatus.POSSIBLE -> {
                            DuplicateTag(
                                text = stringResource(R.string.import_duplicate_possible),
                                isExact = false,
                            )
                        }

                        DuplicateStatus.NONE -> { /* 不显示 */ }
                    }

                    // 分类标签
                    if (item.mappedTypeName.isNotBlank()) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier
                                .padding(start = 4.dp)
                                .clickable(onClick = onTypeClick),
                        ) {
                            Text(
                                text = item.mappedTypeName,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DuplicateTag(
    text: String,
    isExact: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = if (isExact) {
            MaterialTheme.colorScheme.errorContainer
        } else {
            MaterialTheme.colorScheme.tertiaryContainer
        },
        shape = MaterialTheme.shapes.small,
        modifier = modifier,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = if (isExact) {
                MaterialTheme.colorScheme.onErrorContainer
            } else {
                MaterialTheme.colorScheme.onTertiaryContainer
            },
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add feature/record-import/src/main/kotlin/cn/wj/android/cashbook/feature/record/imports/component/ImportPreviewList.kt
git commit -m "[feat|record-import|账单导入][公共]新增导入记录预览列表组件"
```

---

### Task 13: 主页面 RecordImportScreen

**Files:**
- Create: `feature/record-import/src/main/kotlin/cn/wj/android/cashbook/feature/record/imports/screen/RecordImportScreen.kt`

- [ ] **Step 1: 创建 RecordImportScreen.kt**

```kotlin
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

package cn.wj.android.cashbook.feature.record.imports.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.wj.android.cashbook.core.design.component.CbAlertDialog
import cn.wj.android.cashbook.core.design.component.CbScaffold
import cn.wj.android.cashbook.core.design.component.CbTopAppBar
import cn.wj.android.cashbook.core.design.component.Loading
import cn.wj.android.cashbook.core.model.model.BillDirection
import cn.wj.android.cashbook.core.ui.R
import cn.wj.android.cashbook.feature.record.imports.component.ImportPreviewList
import cn.wj.android.cashbook.feature.record.imports.component.ImportSummarySection
import cn.wj.android.cashbook.feature.record.imports.component.PaymentMappingSection
import cn.wj.android.cashbook.feature.record.imports.viewmodel.RecordImportUiState
import cn.wj.android.cashbook.feature.record.imports.viewmodel.RecordImportViewModel
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * 账单导入界面入口
 */
@Composable
internal fun RecordImportRoute(
    onRequestPopBackStack: () -> Unit,
    onShowSnackbar: suspend (String, String?) -> SnackbarResult,
    modifier: Modifier = Modifier,
    viewModel: RecordImportViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 资产选择弹窗状态
    var selectedMappingName by remember { mutableStateOf<String?>(null) }
    // 分类选择弹窗状态
    var selectedTypeIndex by remember { mutableStateOf(-1) }

    RecordImportScreen(
        uiState = uiState,
        onBookSelected = viewModel::selectBook,
        onMappingClick = { originalName -> selectedMappingName = originalName },
        onUpdatePaymentMapping = viewModel::updatePaymentMapping,
        onToggleSelection = viewModel::toggleItemSelection,
        onSelectAll = viewModel::selectAll,
        onTypeClick = { index -> selectedTypeIndex = index },
        onUpdateItemType = viewModel::updateItemType,
        onConfirmImport = viewModel::confirmImport,
        onBackClick = onRequestPopBackStack,
        onShowSnackbar = onShowSnackbar,
        selectedMappingName = selectedMappingName,
        onDismissMappingDialog = { selectedMappingName = null },
        selectedTypeIndex = selectedTypeIndex,
        onDismissTypeDialog = { selectedTypeIndex = -1 },
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RecordImportScreen(
    uiState: RecordImportUiState,
    onBookSelected: (Long) -> Unit,
    onMappingClick: (String) -> Unit,
    onUpdatePaymentMapping: (String, Long, String) -> Unit,
    onToggleSelection: (Int) -> Unit,
    onSelectAll: (Boolean) -> Unit,
    onTypeClick: (Int) -> Unit,
    onUpdateItemType: (Int, Long, String) -> Unit,
    onConfirmImport: () -> Unit,
    onBackClick: () -> Unit,
    onShowSnackbar: suspend (String, String?) -> SnackbarResult,
    selectedMappingName: String?,
    onDismissMappingDialog: () -> Unit,
    selectedTypeIndex: Int,
    onDismissTypeDialog: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // 处理导入完成和错误状态
    val context = LocalContext.current

    LaunchedEffect(uiState) {
        when (uiState) {
            is RecordImportUiState.Done -> {
                onShowSnackbar(
                    context.getString(R.string.import_success, uiState.imported, uiState.skipped),
                    null,
                )
                onBackClick()
            }

            is RecordImportUiState.Error -> {
                onShowSnackbar(
                    context.getString(R.string.import_file_format_error),
                    null,
                )
                onBackClick()
            }

            else -> {}
        }
    }

    // 资产选择弹窗
    if (selectedMappingName != null && uiState is RecordImportUiState.Ready) {
        CbAlertDialog(
            onDismissRequest = onDismissMappingDialog,
            title = { Text(text = stringResource(R.string.import_select_asset)) },
            text = {
                LazyColumn {
                    items(uiState.visibleAssets.size) { index ->
                        val asset = uiState.visibleAssets[index]
                        Text(
                            text = asset.name,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onUpdatePaymentMapping(selectedMappingName!!, asset.id, asset.name)
                                    onDismissMappingDialog()
                                }
                                .padding(vertical = 8.dp, horizontal = 16.dp),
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onDismissMappingDialog) {
                    Text(text = stringResource(R.string.close))
                }
            },
        )
    }

    // 分类选择弹窗
    if (selectedTypeIndex >= 0 && uiState is RecordImportUiState.Ready) {
        val item = uiState.previewItems.getOrNull(selectedTypeIndex)
        val typeList = if (item?.billItem?.direction == BillDirection.EXPENDITURE) {
            uiState.expenditureTypes
        } else {
            uiState.incomeTypes
        }
        CbAlertDialog(
            onDismissRequest = onDismissTypeDialog,
            title = { Text(text = stringResource(R.string.import_select_category)) },
            text = {
                LazyColumn {
                    items(typeList.size) { index ->
                        val type = typeList[index]
                        Text(
                            text = type.name,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onUpdateItemType(selectedTypeIndex, type.id, type.name)
                                    onDismissTypeDialog()
                                }
                                .padding(vertical = 8.dp, horizontal = 16.dp),
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onDismissTypeDialog) {
                    Text(text = stringResource(R.string.close))
                }
            },
        )
    }

    CbScaffold(
        modifier = modifier,
        topBar = {
            CbTopAppBar(
                onBackClick = onBackClick,
                title = { Text(text = stringResource(R.string.import_bill)) },
            )
        },
        bottomBar = {
            if (uiState is RecordImportUiState.Ready) {
                ImportBottomBar(
                    uiState = uiState,
                    onSelectAll = onSelectAll,
                    onConfirmImport = onConfirmImport,
                )
            }
        },
        content = { paddingValues ->
            Box(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize(),
            ) {
                when (uiState) {
                    RecordImportUiState.Parsing,
                    RecordImportUiState.Importing,
                    -> {
                        Loading(modifier = Modifier.align(Alignment.Center))
                    }

                    is RecordImportUiState.Ready -> {
                        ReadyContent(
                            uiState = uiState,
                            onBookSelected = onBookSelected,
                            onMappingClick = onMappingClick,
                            onToggleSelection = onToggleSelection,
                            onTypeClick = onTypeClick,
                        )
                    }

                    is RecordImportUiState.Done,
                    is RecordImportUiState.Error,
                    -> {
                        // 由 LaunchedEffect 处理
                    }
                }
            }
        },
    )
}

@Composable
private fun ReadyContent(
    uiState: RecordImportUiState.Ready,
    onBookSelected: (Long) -> Unit,
    onMappingClick: (String) -> Unit,
    onToggleSelection: (Int) -> Unit,
    onTypeClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    Column(modifier = modifier.fillMaxSize()) {
        // 概览区域
        ImportSummarySection(
            fileName = uiState.fileName,
            summary = uiState.summary,
            selectedBooksId = uiState.selectedBooksId,
            booksList = uiState.booksList,
            onBookSelected = onBookSelected,
        )

        HorizontalDivider()

        // 支付方式映射
        if (uiState.paymentMappings.isNotEmpty()) {
            PaymentMappingSection(
                mappings = uiState.paymentMappings,
                onMappingClick = onMappingClick,
            )
            HorizontalDivider()
        }

        // 记录预览列表标题
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.import_preview_list),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        // 记录预览列表
        ImportPreviewList(
            items = uiState.previewItems,
            onToggleSelection = onToggleSelection,
            onTypeClick = onTypeClick,
            listState = listState,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ImportBottomBar(
    uiState: RecordImportUiState.Ready,
    onSelectAll: (Boolean) -> Unit,
    onConfirmImport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedCount = uiState.previewItems.count { it.selected }
    val selectedAmount = uiState.previewItems
        .filter { it.selected }
        .sumOf { item ->
            if (item.billItem.direction == BillDirection.EXPENDITURE) item.billItem.amount else item.billItem.amount
        }
    val allSelected = uiState.previewItems.all { it.selected }
    val canImport = selectedCount > 0 && !uiState.hasUnmappedPayments

    BottomAppBar(modifier = modifier) {
        TextButton(
            onClick = { onSelectAll(!allSelected) },
            modifier = Modifier.padding(start = 8.dp),
        ) {
            Text(
                text = if (allSelected) {
                    stringResource(R.string.import_deselect_all)
                } else {
                    stringResource(R.string.import_select_all)
                },
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
        ) {
            Text(
                text = stringResource(R.string.import_selected_count, selectedCount),
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = stringResource(R.string.import_selected_amount, "%.2f".format(selectedAmount)),
                style = MaterialTheme.typography.bodySmall,
            )
        }

        FilledTonalButton(
            onClick = onConfirmImport,
            enabled = canImport,
            modifier = Modifier.padding(end = 16.dp),
        ) {
            Text(text = stringResource(R.string.import_confirm))
        }
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add feature/record-import/src/main/kotlin/cn/wj/android/cashbook/feature/record/imports/screen/RecordImportScreen.kt
git commit -m "[feat|record-import|账单导入][公共]新增账单导入主页面"
```

---

### Task 14: 备份页面入口 + 导航注册

**Files:**
- Modify: `feature/settings/src/main/kotlin/cn/wj/android/cashbook/feature/settings/screen/BackupAndRecoveryScreen.kt`
- Modify: `feature/settings/src/main/kotlin/cn/wj/android/cashbook/feature/settings/navigation/SettingsNavigation.kt`
- Modify: `app/src/main/kotlin/cn/wj/android/cashbook/ui/MainApp.kt`

- [ ] **Step 1: 修改 BackupAndRecoveryScreen - 添加导入入口**

在 `BackupAndRecoveryRoute` composable 中添加 `onRequestNaviToRecordImport` 参数，并在 `BackupAndRecoveryScreen` 中传递。

在 `BackupAndRecoveryRoute` 函数签名中，`onShowSnackbar` 参数之后添加：

```kotlin
onRequestNaviToRecordImport: (String) -> Unit,
```

在 `BackupAndRecoveryScreen` 调用处也加上这个参数的传递。

在 `BackupAndRecoveryScreen` composable 签名中，`onDbMigrateClick` 参数之后添加：

```kotlin
onRequestNaviToRecordImport: (String) -> Unit,
```

在 `BackupAndRecoveryScaffoldContent` composable 签名中，`onDbMigrateClick` 参数之后添加：

```kotlin
onRequestNaviToRecordImport: (String) -> Unit,
```

在 `BackupAndRecoveryScaffoldContent` 的 Column 末尾（`db_data_migrate` CbListItem 之后）添加：

```kotlin
Spacer(
    modifier = Modifier
        .fillMaxWidth()
        .height(32.dp),
)

Text(
    text = stringResource(id = R.string.import_bill),
    color = MaterialTheme.colorScheme.primary,
    modifier = Modifier.padding(start = 16.dp),
)

val selectFileLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument(),
    onResult = { uri ->
        uri?.let {
            // 获取持久化读取权限
            context.contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
            onRequestNaviToRecordImport(it.toString())
        }
    },
)

CbListItem(
    headlineContent = { Text(text = stringResource(id = R.string.import_from_wechat)) },
    supportingContent = { Text(text = stringResource(id = R.string.import_from_wechat_hint)) },
    modifier = Modifier.clickable {
        selectFileLauncher.launch(
            arrayOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
        )
    },
)
```

注意：`BackupAndRecoveryScaffoldContent` 中已有 `val context = LocalContext.current`（第 539 行），可直接使用。`rememberLauncherForActivityResult` 和 `ActivityResultContracts` 已有导入。需要新增 `ActivityResultContracts.OpenDocument` 的使用（已有 `ActivityResultContracts.OpenDocumentTree`）。

- [ ] **Step 2: 修改 SettingsNavigation.kt - 添加导航参数**

修改 `backupAndRecoveryScreen` 函数，增加 `onRequestNaviToRecordImport` 参数：

```kotlin
fun NavGraphBuilder.backupAndRecoveryScreen(
    onRequestNaviToRecordImport: (String) -> Unit,
    onRequestPopBackStack: () -> Unit,
    onShowSnackbar: suspend (String, String?) -> SnackbarResult,
) {
    composable(route = ROUTE_SETTINGS_BACKUP_AND_RECOVERY) {
        BackupAndRecoveryRoute(
            onRequestNaviToRecordImport = onRequestNaviToRecordImport,
            onRequestPopBackStack = onRequestPopBackStack,
            onShowSnackbar = onShowSnackbar,
        )
    }
}
```

- [ ] **Step 3: 修改 MainApp.kt - 注册导入页面路由**

在 `CashbookNavHost` 中，`backupAndRecoveryScreen(...)` 调用处添加 `onRequestNaviToRecordImport` 参数，并在 NavHost 块中注册导入页面：

在文件顶部添加导入：
```kotlin
import cn.wj.android.cashbook.feature.record.imports.navigation.naviToRecordImport
import cn.wj.android.cashbook.feature.record.imports.navigation.recordImportScreen
```

在 `backupAndRecoveryScreen` 调用中添加参数：
```kotlin
backupAndRecoveryScreen(
    onRequestNaviToRecordImport = { fileUri -> navController.naviToRecordImport(fileUri) },
    onRequestPopBackStack = { navController.popBackStack() },
    onShowSnackbar = onShowSnackbar,
)
```

在 NavHost 块中添加：
```kotlin
recordImportScreen(
    onRequestPopBackStack = { navController.popBackStack() },
    onShowSnackbar = onShowSnackbar,
)
```

- [ ] **Step 4: 提交**

```bash
git add feature/settings/src/main/kotlin/cn/wj/android/cashbook/feature/settings/screen/BackupAndRecoveryScreen.kt \
  feature/settings/src/main/kotlin/cn/wj/android/cashbook/feature/settings/navigation/SettingsNavigation.kt \
  app/src/main/kotlin/cn/wj/android/cashbook/ui/MainApp.kt
git commit -m "[feat|settings|账单导入][公共]在备份页面添加导入入口并注册导入页面路由"
```

---

### Task 15: 格式检查和编译验证

- [ ] **Step 1: 运行 spotlessApply 修复格式**

```bash
cd /Users/wj/Work/Owner/StudioProjects/Cashbook/.claude/worktrees/record-import && ./gradlew spotlessApply --init-script gradle/init.gradle.kts --no-configuration-cache
```

- [ ] **Step 2: 编译验证**

```bash
cd /Users/wj/Work/Owner/StudioProjects/Cashbook/.claude/worktrees/record-import && ./gradlew :feature:record-import:assembleDebug :app:assembleOnlineDebug 2>&1 | tail -20
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 修复编译错误（如有）**

根据编译输出修复问题，常见问题：
- 导入缺失（根据编译错误添加）
- 类型不匹配（如 `BigDecimal` 扩展函数路径）
- License Header 缺失（spotlessApply 应已修复）

- [ ] **Step 4: 提交修复（如有）**

```bash
git add -u
git commit -m "[fix|all|账单导入][公共]修复编译问题"
```

---

### Task 16: 集成冒烟测试

- [ ] **Step 1: 运行单元测试**

```bash
cd /Users/wj/Work/Owner/StudioProjects/Cashbook/.claude/worktrees/record-import && ./gradlew testOnlineDebugUnitTest 2>&1 | tail -30
```

Expected: 现有测试全部通过

- [ ] **Step 2: 验证 lint 检查**

```bash
cd /Users/wj/Work/Owner/StudioProjects/Cashbook/.claude/worktrees/record-import && ./gradlew :feature:record-import:lintDebug 2>&1 | tail -10
```

Expected: 无严重 lint 错误

- [ ] **Step 3: 修复问题（如有）并提交**

```bash
git add -u
git commit -m "[fix|all|账单导入][公共]修复 lint 和测试问题"
```
