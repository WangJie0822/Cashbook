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
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.Calendar

/**
 * DailyAccountExporter 单元测试
 */
class DailyAccountExporterTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val exporter = DailyAccountExporter()

    // ========== 辅助方法 ==========

    /** 创建指定年月日时分秒的时间戳（本地时区） */
    private fun timestamp(year: Int, month: Int, day: Int, hour: Int, minute: Int, second: Int): Long {
        return Calendar.getInstance().apply {
            set(year, month - 1, day, hour, minute, second)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun createExpenditureRecord(
        year: Int = 2024,
        month: Int = 3,
        day: Int = 15,
        hour: Int = 10,
        minute: Int = 30,
        second: Int = 0,
        assetName: String = "招商银行",
        categoryName: String = "餐饮",
        subCategoryName: String = "午餐",
        amountCents: Long = 1300L,
        remark: String = "午饭",
    ) = ExportRecordModel(
        recordTime = timestamp(year, month, day, hour, minute, second),
        typeCategory = 0,
        assetName = assetName,
        categoryName = categoryName,
        subCategoryName = subCategoryName,
        amount = amountCents,
        remark = remark,
    )

    private fun createIncomeRecord(
        amountCents: Long = 1000000L,
        remark: String = "工资",
    ) = ExportRecordModel(
        recordTime = timestamp(2024, 3, 1, 9, 0, 0),
        typeCategory = 1,
        assetName = "招商银行",
        categoryName = "收入",
        subCategoryName = "工资",
        amount = amountCents,
        remark = remark,
    )

    private fun outputFile(): File = tempFolder.newFile("test_export.csv")

    // ========== 空列表测试 ==========

    @Test
    fun `export empty list produces header only`() {
        val file = outputFile()
        val count = exporter.export(emptyList(), file)

        assertThat(count).isEqualTo(0)

        val lines = file.readLines(Charsets.UTF_8)
        // BOM 会作为第一行内容的前缀；用 UTF-8 读取时 BOM 在 UTF-8 中为零宽字符 \uFEFF
        assertThat(lines).hasSize(1)
        val header = lines[0].trimStart('\uFEFF')
        assertThat(header).isEqualTo("日期,类型,账户,类别,子类别,金额,备注,货币类型,图片")
    }

    // ========== 支出记录测试 ==========

    @Test
    fun `export single expenditure record`() {
        val record = createExpenditureRecord(
            year = 2024,
            month = 3,
            day = 15,
            hour = 10,
            minute = 30,
            second = 0,
            assetName = "招商银行",
            categoryName = "餐饮",
            subCategoryName = "午餐",
            amountCents = 1300L,
            remark = "午饭",
        )
        val file = outputFile()
        exporter.export(listOf(record), file)

        val lines = file.readLines(Charsets.UTF_8)
        assertThat(lines).hasSize(2)
        val dataLine = lines[1]
        val fields = dataLine.split(",")

        assertThat(fields[0]).isEqualTo("2024/3/15 10:30:00")
        assertThat(fields[1]).isEqualTo("支出")
        assertThat(fields[2]).isEqualTo("招商银行")
        assertThat(fields[3]).isEqualTo("餐饮")
        assertThat(fields[4]).isEqualTo("午餐")
        assertThat(fields[5]).isEqualTo("13.00")
        assertThat(fields[6]).isEqualTo("午饭")
        assertThat(fields[7]).isEqualTo("CNY")
    }

    // ========== 收入记录测试 ==========

    @Test
    fun `export income record`() {
        val record = createIncomeRecord(amountCents = 1000000L)
        val file = outputFile()
        exporter.export(listOf(record), file)

        val lines = file.readLines(Charsets.UTF_8)
        assertThat(lines).hasSize(2)
        val fields = lines[1].split(",")

        assertThat(fields[1]).isEqualTo("收入")
        assertThat(fields[5]).isEqualTo("10000.00")
    }

    // ========== CSV 转义测试：含逗号的备注 ==========

    @Test
    fun `export escapes csv special characters`() {
        val record = createExpenditureRecord(remark = "吃饭,喝水")
        val file = outputFile()
        exporter.export(listOf(record), file)

        val content = file.readText(Charsets.UTF_8)
        // 含逗号的字段应被双引号包围
        assertThat(content).contains("\"吃饭,喝水\"")
    }

    // ========== CSV 转义测试：含双引号的字段 ==========

    @Test
    fun `export escapes double quotes in fields`() {
        val record = createExpenditureRecord(remark = "说\"你好\"")
        val file = outputFile()
        exporter.export(listOf(record), file)

        val content = file.readText(Charsets.UTF_8)
        // 内部双引号应被转义为 ""，整体被双引号包围
        assertThat(content).contains("\"说\"\"你好\"\"\"")
    }

    // ========== 一级类型无子类别 ==========

    @Test
    fun `export first level type has empty subcategory`() {
        val record = createExpenditureRecord(subCategoryName = "")
        val file = outputFile()
        exporter.export(listOf(record), file)

        val lines = file.readLines(Charsets.UTF_8)
        assertThat(lines).hasSize(2)
        val fields = lines[1].split(",")
        // 子类别为空
        assertThat(fields[4]).isEqualTo("")
    }

    // ========== 多记录顺序测试 ==========

    @Test
    fun `export multiple records preserves order`() {
        val record1 = createExpenditureRecord(remark = "第一条", amountCents = 500L)
        val record2 = createIncomeRecord(remark = "第二条", amountCents = 200L)
        val file = outputFile()
        val count = exporter.export(listOf(record1, record2), file)

        assertThat(count).isEqualTo(2)
        val lines = file.readLines(Charsets.UTF_8)
        assertThat(lines).hasSize(3)
        assertThat(lines[1]).contains("第一条")
        assertThat(lines[2]).contains("第二条")
    }

    // ========== UTF-8 BOM 验证 ==========

    @Test
    fun `export file uses utf8 with bom`() {
        val file = outputFile()
        exporter.export(emptyList(), file)

        val bytes = file.readBytes()
        assertThat(bytes.size).isAtLeast(3)
        // BOM: EF BB BF
        assertThat(bytes[0]).isEqualTo(0xEF.toByte())
        assertThat(bytes[1]).isEqualTo(0xBB.toByte())
        assertThat(bytes[2]).isEqualTo(0xBF.toByte())
    }

    // ========== 金额格式化（两位小数）==========

    @Test
    fun `export amount formats to two decimal places`() {
        // 10 分 = 0.10 元
        val record = createExpenditureRecord(amountCents = 10L)
        val file = outputFile()
        exporter.export(listOf(record), file)

        val lines = file.readLines(Charsets.UTF_8)
        val fields = lines[1].split(",")
        assertThat(fields[5]).isEqualTo("0.10")
    }
}
