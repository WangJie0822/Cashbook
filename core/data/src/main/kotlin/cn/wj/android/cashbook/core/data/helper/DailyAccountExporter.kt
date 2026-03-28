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

import cn.wj.android.cashbook.core.model.model.ExportRecordModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * 一日记账 CSV 导出器
 *
 * 将 [ExportRecordModel] 列表导出为 UTF-8 with BOM 的 CSV 文件，
 * 可直接用 Excel 打开并正确显示中文。
 */
class DailyAccountExporter @Inject constructor() {

    /** CSV 列标题 */
    private val header = "日期,类型,账户,类别,子类别,金额,备注,货币类型,图片"

    /** 日期格式：yyyy/M/d HH:mm:ss（月和日无前导零） */
    private val dateFormat = SimpleDateFormat("yyyy/M/d HH:mm:ss", Locale.getDefault())

    /**
     * 将记录列表导出为 CSV 文件
     *
     * @param records 要导出的记录列表
     * @param outputFile 目标输出文件
     * @return 实际写入的记录数量
     */
    fun export(records: List<ExportRecordModel>, outputFile: File): Int {
        outputFile.parentFile?.mkdirs()
        outputFile.outputStream().bufferedWriter(Charsets.UTF_8).use { writer ->
            // 写入 UTF-8 BOM，让 Excel 等工具正确识别编码
            writer.write("\uFEFF")
            writer.write(header)
            for (record in records) {
                writer.newLine()
                writer.write(buildCsvLine(record))
            }
        }
        return records.size
    }

    /** 将单条记录构建为 CSV 行 */
    private fun buildCsvLine(record: ExportRecordModel): String {
        val date = dateFormat.format(Date(record.recordTime))
        val type = if (record.typeCategory == 0) "支出" else "收入"
        // 金额：分 → 元，保留两位小数，使用 Locale.US 确保小数点为 "."
        val amount = String.format(Locale.US, "%.2f", record.amount / 100.0)
        val currency = "CNY"
        val image = ""

        return listOf(
            date,
            type,
            record.assetName,
            record.categoryName,
            record.subCategoryName,
            amount,
            record.remark,
            currency,
            image,
        ).joinToString(",") { escapeCsv(it) }
    }

    companion object {
        /**
         * CSV 字段转义
         *
         * 若字段包含逗号、双引号或换行符，则将整体用双引号包围，
         * 并将内部的双引号替换为两个双引号（""）。
         */
        fun escapeCsv(value: String): String {
            val needsQuoting = value.contains(',') || value.contains('"') ||
                value.contains('\n') || value.contains('\r')
            return if (needsQuoting) {
                "\"${value.replace("\"", "\"\"")}\""
            } else {
                value
            }
        }
    }
}
