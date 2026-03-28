# 导出一日记账 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在"备份与恢复"页面新增"导出到一日记账"功能，用户选择账本和日期范围后导出 CSV 文件并通过系统分享发送。

**Architecture:** 遵循项目三层架构（DAO → Repository → UseCase → ViewModel → Composable）。数据库层新增 JOIN 查询获取导出记录（含类型/资产名），数据层新增 CSV 生成器，领域层新增导出用例编排查询与生成。UI 层在现有 BackupAndRecoveryScreen 新增入口 + Bottom Sheet。

**Tech Stack:** Room DAO (JOIN query), Kotlin Flow, Hilt DI, Jetpack Compose (CbModalBottomSheet, DateRangePickerDialog), FileProvider + Share Intent, CSV (UTF-8 BOM)

**Design Spec:** `docs/superpowers/specs/2026-03-28-export-daily-account-design.md`

---

## File Structure

### New Files

| File | Responsibility |
|---|---|
| `core/database/src/main/kotlin/cn/wj/android/cashbook/core/database/relation/ExportRecordRelation.kt` | 导出查询结果的 Room 映射数据类 |
| `core/model/src/main/kotlin/cn/wj/android/cashbook/core/model/model/ExportRecordModel.kt` | 导出记录的领域模型 |
| `core/data/src/main/kotlin/cn/wj/android/cashbook/core/data/helper/DailyAccountExporter.kt` | CSV 生成器 |
| `core/data/src/test/kotlin/cn/wj/android/cashbook/core/data/helper/DailyAccountExporterTest.kt` | CSV 生成器测试 |
| `core/domain/src/main/kotlin/cn/wj/android/cashbook/domain/usecase/ExportRecordUseCase.kt` | 导出用例：编排查询 + CSV 生成 |

### Modified Files

| File | Changes |
|---|---|
| `core/database/src/main/kotlin/cn/wj/android/cashbook/core/database/dao/RecordDao.kt` | 新增 3 个查询方法 |
| `core/data/src/main/kotlin/cn/wj/android/cashbook/core/data/repository/RecordRepository.kt` | 接口新增 3 个方法 |
| `core/data/src/main/kotlin/cn/wj/android/cashbook/core/data/repository/impl/RecordRepositoryImpl.kt` | 实现 3 个新方法 |
| `app/src/main/res/xml/file_paths.xml` | 新增 cache-path 配置 |
| `core/ui/src/main/res/values/strings_settings.xml` | 新增导出相关字符串 |
| `feature/settings/src/main/kotlin/cn/wj/android/cashbook/feature/settings/viewmodel/BackupAndRecoveryViewModel.kt` | 新增导出状态和方法 |
| `feature/settings/src/main/kotlin/cn/wj/android/cashbook/feature/settings/screen/BackupAndRecoveryScreen.kt` | 新增导出入口 + ExportBottomSheet |

---

### Task 1: 数据模型

**Files:**
- Create: `core/database/src/main/kotlin/cn/wj/android/cashbook/core/database/relation/ExportRecordRelation.kt`
- Create: `core/model/src/main/kotlin/cn/wj/android/cashbook/core/model/model/ExportRecordModel.kt`
- Reference: `core/database/src/main/kotlin/cn/wj/android/cashbook/core/database/relation/RecordViewsRelation.kt`（遵循同样的模式）

- [ ] **Step 1: 创建 ExportRecordRelation**

在 `core/database` 的 `relation` 包中新建数据类，字段名必须与 Task 2 中 SQL 查询的列别名一致：

```kotlin
// core/database/src/main/kotlin/cn/wj/android/cashbook/core/database/relation/ExportRecordRelation.kt
package cn.wj.android.cashbook.core.database.relation

/**
 * 导出记录的 Room 查询结果映射类
 *
 * 字段名与 RecordDao.queryExportRecords() SQL 列别名对应
 */
data class ExportRecordRelation(
    val recordTime: Long,
    val typeCategory: Int,
    val assetName: String,
    val categoryName: String,
    val subCategoryName: String,
    val amount: Long,
    val remark: String,
)
```

- [ ] **Step 2: 创建 ExportRecordModel**

在 `core/model` 的 `model` 包中新建领域模型，字段与 Relation 一一对应：

```kotlin
// core/model/src/main/kotlin/cn/wj/android/cashbook/core/model/model/ExportRecordModel.kt
package cn.wj.android.cashbook.core.model.model

/**
 * 导出记录的领域模型
 *
 * @param recordTime 记录时间戳（毫秒）
 * @param typeCategory 类型分类：0=支出, 1=收入
 * @param assetName 资产名称
 * @param categoryName 父分类名称（一级类型时为自身名称）
 * @param subCategoryName 子分类名称（一级类型时为空字符串）
 * @param amount 金额（分）
 * @param remark 备注
 */
data class ExportRecordModel(
    val recordTime: Long,
    val typeCategory: Int,
    val assetName: String,
    val categoryName: String,
    val subCategoryName: String,
    val amount: Long,
    val remark: String,
)
```

- [ ] **Step 3: 运行格式化**

```bash
./gradlew spotlessApply --init-script gradle/init.gradle.kts --no-configuration-cache
```

- [ ] **Step 4: 编译验证**

```bash
./gradlew :core:database:compileDebugKotlin :core:model:compileKotlin
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add core/database/src/main/kotlin/cn/wj/android/cashbook/core/database/relation/ExportRecordRelation.kt \
       core/model/src/main/kotlin/cn/wj/android/cashbook/core/model/model/ExportRecordModel.kt
git commit -m "[feat|all|导出一日记账][公共]新增导出记录数据模型"
```

---

### Task 2: DAO 查询

**Files:**
- Modify: `core/database/src/main/kotlin/cn/wj/android/cashbook/core/database/dao/RecordDao.kt`
- Reference: 现有 `queryViewsBetweenDate` 方法的 JOIN 模式（约第 89-111 行）

- [ ] **Step 1: 添加导出记录查询方法**

在 `RecordDao.kt` 中添加（在 `queryByTimeAndAmount` 方法之后）：

```kotlin
import cn.wj.android.cashbook.core.database.relation.ExportRecordRelation

/** 查询指定账本和日期范围内的记录用于导出（排除转账），含类型父子关系和资产名 */
@Query(
    value = """
        SELECT db_record.record_time AS recordTime,
               db_type.type_category AS typeCategory,
               COALESCE(db_asset.name, '') AS assetName,
               CASE WHEN db_type.parent_id = -1 THEN db_type.name
                    ELSE COALESCE(parent_type.name, db_type.name)
               END AS categoryName,
               CASE WHEN db_type.parent_id = -1 THEN ''
                    ELSE db_type.name
               END AS subCategoryName,
               db_record.amount AS amount,
               db_record.remark AS remark
        FROM db_record
        JOIN db_type ON db_type.id = db_record.type_id
        LEFT JOIN db_type AS parent_type ON parent_type.id = db_type.parent_id
        LEFT JOIN db_asset ON db_asset.id = db_record.asset_id
        WHERE db_record.books_id = :booksId
          AND db_record.record_time >= :startDate
          AND db_record.record_time < :endDate
          AND db_type.type_category != 2
        ORDER BY db_record.record_time ASC
    """,
)
suspend fun queryExportRecords(
    booksId: Long,
    startDate: Long,
    endDate: Long,
): List<ExportRecordRelation>
```

- [ ] **Step 2: 添加导出记录计数查询**

```kotlin
/** 查询导出记录数量（排除转账），用于 Bottom Sheet 按钮显示 */
@Query(
    value = """
        SELECT COUNT(*)
        FROM db_record
        JOIN db_type ON db_type.id = db_record.type_id
        WHERE db_record.books_id = :booksId
          AND db_record.record_time >= :startDate
          AND db_record.record_time < :endDate
          AND db_type.type_category != 2
    """,
)
suspend fun countExportRecords(
    booksId: Long,
    startDate: Long,
    endDate: Long,
): Int
```

- [ ] **Step 3: 添加最早记录时间查询**

```kotlin
/** 查询指定账本中最早的记录时间，用于日期选择器的默认起始日期 */
@Query("SELECT MIN(record_time) FROM db_record WHERE books_id = :booksId")
suspend fun queryEarliestRecordTime(booksId: Long): Long?
```

- [ ] **Step 4: 运行格式化**

```bash
./gradlew spotlessApply --init-script gradle/init.gradle.kts --no-configuration-cache
```

- [ ] **Step 5: 编译验证**

```bash
./gradlew :core:database:compileDebugKotlin
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add core/database/src/main/kotlin/cn/wj/android/cashbook/core/database/dao/RecordDao.kt
git commit -m "[feat|all|导出一日记账][公共]新增导出记录DAO查询方法"
```

---

### Task 3: Repository 层

**Files:**
- Modify: `core/data/src/main/kotlin/cn/wj/android/cashbook/core/data/repository/RecordRepository.kt`
- Modify: `core/data/src/main/kotlin/cn/wj/android/cashbook/core/data/repository/impl/RecordRepositoryImpl.kt`
- Reference: 现有 `queryByWechatTransactionId` 的接口/实现模式

- [ ] **Step 1: 在 RecordRepository 接口添加方法**

在 `RecordRepository.kt` 接口中（`batchImportRecords` 方法之后）添加：

```kotlin
import cn.wj.android.cashbook.core.model.model.ExportRecordModel

/**
 * 查询指定账本和日期范围内的导出记录（排除转账）
 *
 * @param booksId 账本 ID
 * @param startDate 起始时间（inclusive，毫秒）
 * @param endDate 结束时间（exclusive，毫秒）
 */
suspend fun queryExportRecords(
    booksId: Long,
    startDate: Long,
    endDate: Long,
): List<ExportRecordModel>

/**
 * 查询导出记录数量（排除转账）
 */
suspend fun countExportRecords(
    booksId: Long,
    startDate: Long,
    endDate: Long,
): Int

/**
 * 查询指定账本最早的记录时间
 *
 * @return 最早记录的时间戳（毫秒），无记录时返回 null
 */
suspend fun queryEarliestRecordTime(booksId: Long): Long?
```

- [ ] **Step 2: 在 RecordRepositoryImpl 实现方法**

在 `RecordRepositoryImpl.kt` 中（`batchImportRecords` 实现之后）添加：

```kotlin
import cn.wj.android.cashbook.core.model.model.ExportRecordModel

override suspend fun queryExportRecords(
    booksId: Long,
    startDate: Long,
    endDate: Long,
): List<ExportRecordModel> = withContext(coroutineContext) {
    recordDao.queryExportRecords(booksId, startDate, endDate).map { relation ->
        ExportRecordModel(
            recordTime = relation.recordTime,
            typeCategory = relation.typeCategory,
            assetName = relation.assetName,
            categoryName = relation.categoryName,
            subCategoryName = relation.subCategoryName,
            amount = relation.amount,
            remark = relation.remark,
        )
    }
}

override suspend fun countExportRecords(
    booksId: Long,
    startDate: Long,
    endDate: Long,
): Int = withContext(coroutineContext) {
    recordDao.countExportRecords(booksId, startDate, endDate)
}

override suspend fun queryEarliestRecordTime(booksId: Long): Long? =
    withContext(coroutineContext) {
        recordDao.queryEarliestRecordTime(booksId)
    }
```

> **注意**：确认 `RecordRepositoryImpl` 中 `recordDao` 的成员变量名和 `coroutineContext`（IO Dispatcher）的用法与现有实现一致。参考 `batchImportRecords` 的实现模式。

- [ ] **Step 3: 运行格式化 + 编译验证**

```bash
./gradlew spotlessApply --init-script gradle/init.gradle.kts --no-configuration-cache && \
./gradlew :core:data:compileDebugKotlin
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add core/data/src/main/kotlin/cn/wj/android/cashbook/core/data/repository/RecordRepository.kt \
       core/data/src/main/kotlin/cn/wj/android/cashbook/core/data/repository/impl/RecordRepositoryImpl.kt
git commit -m "[feat|all|导出一日记账][公共]Repository层新增导出记录查询方法"
```

---

### Task 4: CSV 生成器（TDD）

**Files:**
- Create: `core/data/src/main/kotlin/cn/wj/android/cashbook/core/data/helper/DailyAccountExporter.kt`
- Create: `core/data/src/test/kotlin/cn/wj/android/cashbook/core/data/helper/DailyAccountExporterTest.kt`
- Reference: `core/data/src/main/kotlin/cn/wj/android/cashbook/core/data/helper/WechatBillParser.kt`（同包 helper 模式）

- [ ] **Step 1: 编写测试**

创建 `core/data/src/test/kotlin/cn/wj/android/cashbook/core/data/helper/DailyAccountExporterTest.kt`：

```kotlin
package cn.wj.android.cashbook.core.data.helper

import cn.wj.android.cashbook.core.model.model.ExportRecordModel
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.util.Calendar

class DailyAccountExporterTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val exporter = DailyAccountExporter()

    /** 构造指定日期时间的时间戳 */
    private fun timestamp(
        year: Int,
        month: Int,
        day: Int,
        hour: Int = 0,
        minute: Int = 0,
        second: Int = 0,
    ): Long = Calendar.getInstance().apply {
        set(year, month - 1, day, hour, minute, second)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    @Test
    fun `export empty list produces header only`() {
        val file = tempFolder.newFile("empty.csv")
        val count = exporter.export(emptyList(), file)

        assertThat(count).isEqualTo(0)
        val lines = file.readLines(Charsets.UTF_8)
        assertThat(lines).hasSize(1)
        // BOM + header
        assertThat(lines[0]).startsWith("\uFEFF")
        assertThat(lines[0]).contains("日期,类型,账户,类别,子类别,金额,备注,货币类型,图片")
    }

    @Test
    fun `export single expenditure record`() {
        val records = listOf(
            ExportRecordModel(
                recordTime = timestamp(2024, 12, 8, 13, 46, 11),
                typeCategory = 0,
                assetName = "中国银行(3319)",
                categoryName = "餐饮",
                subCategoryName = "早餐",
                amount = 1300L,
                remark = "这是一条备注",
            ),
        )
        val file = tempFolder.newFile("single.csv")
        val count = exporter.export(records, file)

        assertThat(count).isEqualTo(1)
        val lines = file.readLines(Charsets.UTF_8)
        assertThat(lines).hasSize(2)

        val dataLine = lines[1]
        assertThat(dataLine).contains("2024/12/8 13:46:11")
        assertThat(dataLine).contains("支出")
        assertThat(dataLine).contains("中国银行(3319)")
        assertThat(dataLine).contains("餐饮")
        assertThat(dataLine).contains("早餐")
        assertThat(dataLine).contains("13.00")
        assertThat(dataLine).contains("这是一条备注")
        assertThat(dataLine).contains("CNY")
    }

    @Test
    fun `export income record`() {
        val records = listOf(
            ExportRecordModel(
                recordTime = timestamp(2024, 1, 5, 9, 0, 0),
                typeCategory = 1,
                assetName = "工商银行",
                categoryName = "工资",
                subCategoryName = "",
                amount = 1_000_000L,
                remark = "1月工资",
            ),
        )
        val file = tempFolder.newFile("income.csv")
        exporter.export(records, file)

        val lines = file.readLines(Charsets.UTF_8)
        val dataLine = lines[1]
        assertThat(dataLine).contains("收入")
        assertThat(dataLine).contains("10000.00")
    }

    @Test
    fun `export escapes csv special characters`() {
        val records = listOf(
            ExportRecordModel(
                recordTime = timestamp(2024, 6, 1, 12, 0, 0),
                typeCategory = 0,
                assetName = "现金",
                categoryName = "购物",
                subCategoryName = "日用品",
                amount = 5990L,
                remark = "备注含,逗号",
            ),
        )
        val file = tempFolder.newFile("escape.csv")
        exporter.export(records, file)

        val content = file.readText(Charsets.UTF_8)
        // 含逗号的字段应被双引号包裹
        assertThat(content).contains("\"备注含,逗号\"")
    }

    @Test
    fun `export escapes double quotes in fields`() {
        val records = listOf(
            ExportRecordModel(
                recordTime = timestamp(2024, 3, 15, 10, 30, 0),
                typeCategory = 0,
                assetName = "现金",
                categoryName = "餐饮",
                subCategoryName = "",
                amount = 2000L,
                remark = "包含\"引号\"的备注",
            ),
        )
        val file = tempFolder.newFile("quotes.csv")
        exporter.export(records, file)

        val content = file.readText(Charsets.UTF_8)
        // 内部引号应转义为 ""
        assertThat(content).contains("\"包含\"\"引号\"\"的备注\"")
    }

    @Test
    fun `export first level type has empty subcategory`() {
        val records = listOf(
            ExportRecordModel(
                recordTime = timestamp(2024, 7, 20, 8, 0, 0),
                typeCategory = 0,
                assetName = "现金",
                categoryName = "餐饮",
                subCategoryName = "",
                amount = 1000L,
                remark = "",
            ),
        )
        val file = tempFolder.newFile("firstlevel.csv")
        exporter.export(records, file)

        val lines = file.readLines(Charsets.UTF_8)
        val parts = lines[1].split(",")
        // 子类别（index 4）应为空
        assertThat(parts[4]).isEmpty()
    }

    @Test
    fun `export multiple records preserves order`() {
        val records = listOf(
            ExportRecordModel(
                recordTime = timestamp(2024, 1, 1, 8, 0, 0),
                typeCategory = 0,
                assetName = "现金",
                categoryName = "餐饮",
                subCategoryName = "早餐",
                amount = 1500L,
                remark = "第一条",
            ),
            ExportRecordModel(
                recordTime = timestamp(2024, 1, 1, 12, 0, 0),
                typeCategory = 0,
                assetName = "微信",
                categoryName = "交通",
                subCategoryName = "",
                amount = 300L,
                remark = "第二条",
            ),
        )
        val file = tempFolder.newFile("multi.csv")
        val count = exporter.export(records, file)

        assertThat(count).isEqualTo(2)
        val lines = file.readLines(Charsets.UTF_8)
        assertThat(lines).hasSize(3) // header + 2 data rows
        assertThat(lines[1]).contains("第一条")
        assertThat(lines[2]).contains("第二条")
    }

    @Test
    fun `export file uses utf8 with bom`() {
        val file = tempFolder.newFile("bom.csv")
        exporter.export(emptyList(), file)

        val bytes = file.readBytes()
        // UTF-8 BOM: EF BB BF
        assertThat(bytes[0]).isEqualTo(0xEF.toByte())
        assertThat(bytes[1]).isEqualTo(0xBB.toByte())
        assertThat(bytes[2]).isEqualTo(0xBF.toByte())
    }

    @Test
    fun `export amount formats to two decimal places`() {
        val records = listOf(
            ExportRecordModel(
                recordTime = timestamp(2024, 5, 10, 10, 0, 0),
                typeCategory = 0,
                assetName = "现金",
                categoryName = "餐饮",
                subCategoryName = "",
                amount = 10L, // 0.10 元
                remark = "",
            ),
        )
        val file = tempFolder.newFile("decimal.csv")
        exporter.export(records, file)

        val lines = file.readLines(Charsets.UTF_8)
        assertThat(lines[1]).contains("0.10")
    }
}
```

- [ ] **Step 2: 运行测试验证失败**

```bash
./gradlew :core:data:testDebugUnitTest --tests "cn.wj.android.cashbook.core.data.helper.DailyAccountExporterTest" --info
```

Expected: FAIL — `DailyAccountExporter` 类不存在

- [ ] **Step 3: 实现 DailyAccountExporter**

创建 `core/data/src/main/kotlin/cn/wj/android/cashbook/core/data/helper/DailyAccountExporter.kt`：

```kotlin
package cn.wj.android.cashbook.core.data.helper

import cn.wj.android.cashbook.core.model.model.ExportRecordModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * 一日记账通用导入格式 CSV 生成器
 *
 * 输出格式：UTF-8 with BOM，逗号分隔，9 列
 * 列顺序：日期,类型,账户,类别,子类别,金额,备注,货币类型,图片
 */
class DailyAccountExporter @Inject constructor() {

    /**
     * 将记录列表导出为一日记账格式的 CSV 文件
     *
     * @param records 要导出的记录列表（已按时间排序）
     * @param outputFile 输出文件
     * @return 导出的记录数量
     */
    fun export(records: List<ExportRecordModel>, outputFile: File): Int {
        val dateFormat = SimpleDateFormat(DATE_PATTERN, Locale.getDefault())

        outputFile.parentFile?.mkdirs()
        outputFile.bufferedWriter(Charsets.UTF_8).use { writer ->
            // UTF-8 BOM
            writer.write(BOM)
            // 表头
            writer.write(HEADER)
            writer.newLine()
            // 数据行
            for (record in records) {
                val line = buildString {
                    append(dateFormat.format(Date(record.recordTime)))
                    append(SEPARATOR)
                    append(if (record.typeCategory == TYPE_EXPENDITURE) "支出" else "收入")
                    append(SEPARATOR)
                    append(escapeCsv(record.assetName))
                    append(SEPARATOR)
                    append(escapeCsv(record.categoryName))
                    append(SEPARATOR)
                    append(escapeCsv(record.subCategoryName))
                    append(SEPARATOR)
                    append(String.format(Locale.US, "%.2f", record.amount / 100.0))
                    append(SEPARATOR)
                    append(escapeCsv(record.remark))
                    append(SEPARATOR)
                    append(CURRENCY)
                    append(SEPARATOR)
                    // 图片列留空
                }
                writer.write(line)
                writer.newLine()
            }
        }
        return records.size
    }

    companion object {
        private const val BOM = "\uFEFF"
        private const val SEPARATOR = ","
        private const val CURRENCY = "CNY"
        private const val DATE_PATTERN = "yyyy/M/d HH:mm:ss"
        private const val HEADER = "日期,类型,账户,类别,子类别,金额,备注,货币类型,图片"
        private const val TYPE_EXPENDITURE = 0

        /** CSV 字段转义：含逗号、双引号或换行时用双引号包裹 */
        fun escapeCsv(value: String): String {
            return if (value.contains(',') || value.contains('"') || value.contains('\n') || value.contains('\r')) {
                "\"${value.replace("\"", "\"\"")}\""
            } else {
                value
            }
        }
    }
}
```

- [ ] **Step 4: 运行测试验证通过**

```bash
./gradlew :core:data:testDebugUnitTest --tests "cn.wj.android.cashbook.core.data.helper.DailyAccountExporterTest" --info
```

Expected: ALL TESTS PASSED

- [ ] **Step 5: 运行格式化**

```bash
./gradlew spotlessApply --init-script gradle/init.gradle.kts --no-configuration-cache
```

- [ ] **Step 6: Commit**

```bash
git add core/data/src/main/kotlin/cn/wj/android/cashbook/core/data/helper/DailyAccountExporter.kt \
       core/data/src/test/kotlin/cn/wj/android/cashbook/core/data/helper/DailyAccountExporterTest.kt
git commit -m "[feat|all|导出一日记账][公共]新增一日记账CSV生成器及单元测试"
```

---

### Task 5: 导出用例

**Files:**
- Create: `core/domain/src/main/kotlin/cn/wj/android/cashbook/domain/usecase/ExportRecordUseCase.kt`
- Reference: `core/domain/src/main/kotlin/cn/wj/android/cashbook/domain/usecase/SaveRecordUseCase.kt`（Hilt 注入 + IO Dispatcher 模式）

- [ ] **Step 1: 创建 ExportRecordUseCase**

```kotlin
// core/domain/src/main/kotlin/cn/wj/android/cashbook/domain/usecase/ExportRecordUseCase.kt
package cn.wj.android.cashbook.domain.usecase

import cn.wj.android.cashbook.core.common.annotation.CashbookDispatchers
import cn.wj.android.cashbook.core.common.annotation.Dispatcher
import cn.wj.android.cashbook.core.data.helper.DailyAccountExporter
import cn.wj.android.cashbook.core.data.repository.RecordRepository
import java.io.File
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.withContext

/**
 * 导出记录为一日记账格式的 CSV 文件
 *
 * 编排流程：查询导出记录 → 生成 CSV 文件 → 返回导出数量
 */
class ExportRecordUseCase @Inject constructor(
    private val recordRepository: RecordRepository,
    private val exporter: DailyAccountExporter,
    @Dispatcher(CashbookDispatchers.IO) private val coroutineContext: CoroutineContext,
) {

    /**
     * @param booksId 账本 ID
     * @param startDate 起始时间（inclusive，毫秒）
     * @param endDate 结束时间（exclusive，毫秒）
     * @param outputFile 输出 CSV 文件
     * @return 导出的记录数量
     */
    suspend operator fun invoke(
        booksId: Long,
        startDate: Long,
        endDate: Long,
        outputFile: File,
    ): Int = withContext(coroutineContext) {
        val records = recordRepository.queryExportRecords(booksId, startDate, endDate)
        exporter.export(records, outputFile)
    }
}
```

> **注意**：确认 `@Dispatcher` 和 `CashbookDispatchers` 的导入路径与项目中 `SaveRecordUseCase.kt` 一致。如果包名不同需要调整 import。

- [ ] **Step 2: 运行格式化 + 编译**

```bash
./gradlew spotlessApply --init-script gradle/init.gradle.kts --no-configuration-cache && \
./gradlew :core:domain:compileDebugKotlin
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add core/domain/src/main/kotlin/cn/wj/android/cashbook/domain/usecase/ExportRecordUseCase.kt
git commit -m "[feat|all|导出一日记账][公共]新增导出记录用例"
```

---

### Task 6: FileProvider 配置 + 字符串资源

**Files:**
- Modify: `app/src/main/res/xml/file_paths.xml`
- Modify: `core/ui/src/main/res/values/strings_settings.xml`

- [ ] **Step 1: 更新 FileProvider 路径配置**

在 `app/src/main/res/xml/file_paths.xml` 中添加 `cache-path`，使 `context.cacheDir/export/` 下的文件可通过 FileProvider 分享。

读取文件，在最后一个 `</paths>` 之前添加：

```xml
<cache-path
    name="export"
    path="export/" />
```

- [ ] **Step 2: 添加字符串资源**

在 `core/ui/src/main/res/values/strings_settings.xml` 中，`import_select_category` 之后添加：

```xml
<!-- 导出 -->
<string name="export_bill">导出账单</string>
<string name="export_to_daily_account">导出到一日记账</string>
<string name="export_to_daily_account_hint">导出记录为一日记账通用导入格式(.csv)</string>
<string name="export_sheet_title">导出到一日记账</string>
<string name="export_target_book">账本</string>
<string name="export_date_range">日期范围</string>
<string name="export_start_date">开始日期</string>
<string name="export_end_date">结束日期</string>
<string name="export_confirm">导出 (%1$d条)</string>
<string name="export_confirm_empty">导出 (0条)</string>
<string name="export_exporting">正在导出…</string>
<string name="export_success">成功导出 %1$d 条记录</string>
<string name="export_failed">导出失败：%1$s</string>
```

- [ ] **Step 3: 运行格式化 + 编译**

```bash
./gradlew spotlessApply --init-script gradle/init.gradle.kts --no-configuration-cache && \
./gradlew :core:ui:compileDebugKotlin :app:compileOnlineDebugKotlin
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/res/xml/file_paths.xml \
       core/ui/src/main/res/values/strings_settings.xml
git commit -m "[feat|all|导出一日记账][公共]新增FileProvider缓存路径和导出字符串资源"
```

---

### Task 7: ViewModel

**Files:**
- Modify: `feature/settings/src/main/kotlin/cn/wj/android/cashbook/feature/settings/viewmodel/BackupAndRecoveryViewModel.kt`
- Reference: 现有 ViewModel 结构（约第 51-253 行）

- [ ] **Step 1: 阅读 ViewModel 现有代码**

阅读 `BackupAndRecoveryViewModel.kt` 全文，确认：
1. 构造函数的依赖注入模式
2. UiState 的定义位置（文件末尾 sealed interface）
3. 现有的 StateFlow 和方法
4. `viewModelScope.launch` 使用模式

- [ ] **Step 2: 添加依赖注入**

在构造函数中添加 `booksRepository` 和 `exportRecordUseCase`：

```kotlin
import cn.wj.android.cashbook.core.data.repository.BooksRepository
import cn.wj.android.cashbook.domain.usecase.ExportRecordUseCase

@HiltViewModel
class BackupAndRecoveryViewModel @Inject constructor(
    private val settingRepository: SettingRepository,
    private val recordRepository: RecordRepository,
    private val booksRepository: BooksRepository,
    private val backupRecoveryManager: BackupRecoveryManager,
    private val networkMonitor: NetworkMonitor,
    private val exportRecordUseCase: ExportRecordUseCase,
) : ViewModel() {
```

- [ ] **Step 3: 添加导出相关状态**

在 ViewModel 类内部添加：

```kotlin
import cn.wj.android.cashbook.core.model.model.BooksModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

/** 账本列表（用于导出 Bottom Sheet 的账本选择器） */
val booksList: StateFlow<List<BooksModel>> = booksRepository.booksListData
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

/** 当前选中账本（用于导出的默认账本） */
val currentBook: StateFlow<BooksModel?> = booksRepository.currentBook
    .map<BooksModel, BooksModel?> { it }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

/** 导出状态 */
private val _exportState = MutableStateFlow<ExportState>(ExportState.Idle)
val exportState: StateFlow<ExportState> = _exportState.asStateFlow()
```

- [ ] **Step 4: 添加导出方法**

```kotlin
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 查询指定账本最早的记录时间 */
suspend fun getEarliestRecordTime(booksId: Long): Long? =
    recordRepository.queryEarliestRecordTime(booksId)

/** 查询导出记录数量（endDate 为 exclusive） */
suspend fun countExportRecords(booksId: Long, startDate: Long, endDate: Long): Int =
    recordRepository.countExportRecords(booksId, startDate, endDate)

/**
 * 执行导出
 *
 * @param booksId 账本 ID
 * @param startDate 起始日期（inclusive，毫秒）
 * @param endDate 结束日期（exclusive，毫秒）
 * @param bookName 账本名称（用于文件名）
 * @param displayStartDate 显示用起始日期（inclusive，用于文件名）
 * @param displayEndDate 显示用结束日期（inclusive，用于文件名）
 * @param cacheDir 应用缓存目录
 */
fun exportRecords(
    booksId: Long,
    startDate: Long,
    endDate: Long,
    bookName: String,
    displayStartDate: Long,
    displayEndDate: Long,
    cacheDir: File,
) {
    viewModelScope.launch {
        _exportState.value = ExportState.Exporting
        try {
            val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
            val fileName = "一日记账_${bookName}_${dateFormat.format(Date(displayStartDate))}_${dateFormat.format(Date(displayEndDate))}.csv"
            val outputFile = File(File(cacheDir, "export"), fileName)
            val count = exportRecordUseCase(booksId, startDate, endDate, outputFile)
            _exportState.value = ExportState.Done(outputFile.absolutePath, count)
        } catch (e: Exception) {
            _exportState.value = ExportState.Error(e.message ?: "导出失败")
        }
    }
}

/** 重置导出状态 */
fun resetExportState() {
    _exportState.value = ExportState.Idle
}
```

- [ ] **Step 5: 添加 ExportState sealed interface**

在文件末尾（`BackupAndRecoveryUiState` 之后）添加：

```kotlin
/** 导出状态 */
sealed interface ExportState {
    /** 空闲 */
    data object Idle : ExportState

    /** 导出中 */
    data object Exporting : ExportState

    /** 导出完成 */
    data class Done(val filePath: String, val count: Int) : ExportState

    /** 导出失败 */
    data class Error(val message: String) : ExportState
}
```

- [ ] **Step 6: 运行格式化 + 编译**

```bash
./gradlew spotlessApply --init-script gradle/init.gradle.kts --no-configuration-cache && \
./gradlew :feature:settings:compileDebugKotlin
```

Expected: BUILD SUCCESSFUL

> 如果编译失败，检查：
> 1. `BooksRepository` 的导入路径是否正确（参考现有 import）
> 2. `ExportRecordUseCase` 的导入路径是否正确
> 3. `booksRepository.booksListData` 属性名是否与接口一致（可能是 `booksListData` 或其他名称）

- [ ] **Step 7: Commit**

```bash
git add feature/settings/src/main/kotlin/cn/wj/android/cashbook/feature/settings/viewmodel/BackupAndRecoveryViewModel.kt
git commit -m "[feat|all|导出一日记账][公共]ViewModel新增导出状态和方法"
```

---

### Task 8: UI — 导出入口 + Bottom Sheet

**Files:**
- Modify: `feature/settings/src/main/kotlin/cn/wj/android/cashbook/feature/settings/screen/BackupAndRecoveryScreen.kt`
- Reference: 现有 "导入账单" 区块（约第 720-770 行）
- Reference: `CbModalBottomSheet`（`core/design/component/ModalBottomSheet.kt`）
- Reference: `DateRangePickerDialog`（`core/design/component/DateTimePicker.kt`）

- [ ] **Step 1: 阅读 BackupAndRecoveryScreen 现有代码**

完整阅读 `BackupAndRecoveryScreen.kt`，重点关注：
1. `BackupAndRecoveryRoute` composable 的参数和 ViewModel 连接方式
2. `BackupAndRecoveryScreen` composable 的参数传递
3. `BackupAndRecoveryScaffoldContent` 的末尾（"导入账单"区块之后的位置）
4. 现有的 Dialog/Sheet 显示模式
5. Snackbar 回调的传递方式

- [ ] **Step 2: 在 BackupAndRecoveryRoute 中收集新状态**

在 `BackupAndRecoveryRoute` composable 中，收集新增的 ViewModel 状态：

```kotlin
val booksList by viewModel.booksList.collectAsStateWithLifecycle()
val currentBook by viewModel.currentBook.collectAsStateWithLifecycle()
val exportState by viewModel.exportState.collectAsStateWithLifecycle()
```

将这些值传递给 `BackupAndRecoveryScreen` composable。

- [ ] **Step 3: 更新 BackupAndRecoveryScreen composable 签名**

在 `BackupAndRecoveryScreen` composable 中新增参数（传递到内部 `BackupAndRecoveryScaffoldContent`）：

```kotlin
booksList: List<BooksModel>,
currentBook: BooksModel?,
exportState: ExportState,
onExportRecords: (booksId: Long, startDate: Long, endDate: Long, bookName: String, displayStartDate: Long, displayEndDate: Long, cacheDir: File) -> Unit,
onResetExportState: () -> Unit,
onCountExportRecords: suspend (booksId: Long, startDate: Long, endDate: Long) -> Int,
onGetEarliestRecordTime: suspend (booksId: Long) -> Long?,
```

- [ ] **Step 4: 在 ScaffoldContent 末尾添加导出入口**

在 `BackupAndRecoveryScaffoldContent` 中，"导入账单"的 `CbListItem`（微信导入）之后添加：

```kotlin
// ---- 导出账单 ----
Text(
    text = stringResource(id = R.string.export_bill),
    color = MaterialTheme.colorScheme.primary,
    modifier = Modifier.padding(start = 16.dp),
)

CbListItem(
    headlineContent = { Text(text = stringResource(id = R.string.export_to_daily_account)) },
    supportingContent = {
        Text(text = stringResource(id = R.string.export_to_daily_account_hint))
    },
    modifier = Modifier.clickable { showExportSheet = true },
)
```

其中 `showExportSheet` 为局部 `remember` 状态：

```kotlin
var showExportSheet by remember { mutableStateOf(false) }
```

- [ ] **Step 5: 实现 ExportBottomSheet**

在 `BackupAndRecoveryScaffoldContent` 末尾（return 之前），添加 Bottom Sheet 及其状态管理：

```kotlin
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.core.content.FileProvider
import android.content.Intent
import cn.wj.android.cashbook.core.design.component.CbModalBottomSheet
import cn.wj.android.cashbook.core.design.component.DateRangePickerDialog
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// -- Export Bottom Sheet --
if (showExportSheet) {
    val context = LocalContext.current
    val displayDateFormat = remember { SimpleDateFormat("yyyy/M/d", Locale.getDefault()) }

    // 本地状态
    var selectedBooksId by remember { mutableStateOf(currentBook?.id ?: -1L) }
    var selectedStartDate by remember { mutableStateOf(0L) }
    var selectedEndDate by remember { mutableStateOf(0L) }
    var recordCount by remember { mutableIntStateOf(0) }
    var showDatePicker by remember { mutableStateOf(false) }
    var booksExpanded by remember { mutableStateOf(false) }

    val selectedBook = booksList.find { it.id == selectedBooksId }
    val isExporting = exportState is ExportState.Exporting

    // 初始化日期范围：最早记录 ~ 今天
    LaunchedEffect(selectedBooksId) {
        val earliest = onGetEarliestRecordTime(selectedBooksId)
        if (earliest != null) {
            // 截取到当天零点
            val startCal = Calendar.getInstance().apply {
                timeInMillis = earliest
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            selectedStartDate = startCal.timeInMillis
        } else {
            val todayCal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            selectedStartDate = todayCal.timeInMillis
        }
        val todayCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        selectedEndDate = todayCal.timeInMillis
    }

    // 响应式查询记录数
    LaunchedEffect(selectedBooksId, selectedStartDate, selectedEndDate) {
        if (selectedBooksId > 0 && selectedStartDate > 0 && selectedEndDate >= selectedStartDate) {
            // endDate + 1 天作为 exclusive 查询参数
            val queryEndDate = selectedEndDate + 86_400_000L
            recordCount = onCountExportRecords(selectedBooksId, selectedStartDate, queryEndDate)
        } else {
            recordCount = 0
        }
    }

    // 导出完成/失败处理
    LaunchedEffect(exportState) {
        when (val state = exportState) {
            is ExportState.Done -> {
                // 触发系统分享
                val file = File(state.filePath)
                if (file.exists()) {
                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.FileProvider",
                        file,
                    )
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/csv"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, null))
                }
                onShowSnackbar(context.getString(R.string.export_success, state.count))
                onResetExportState()
                showExportSheet = false
            }
            is ExportState.Error -> {
                onShowSnackbar(context.getString(R.string.export_failed, state.message))
                onResetExportState()
            }
            else -> {}
        }
    }

    CbModalBottomSheet(
        onDismissRequest = {
            if (!isExporting) {
                showExportSheet = false
            }
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // 标题
            Text(
                text = stringResource(id = R.string.export_sheet_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )

            // 账本选择器
            Row(
                modifier = Modifier.clickable(enabled = !isExporting) {
                    booksExpanded = true
                },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(id = R.string.export_target_book) + "：",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = selectedBook?.name ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                DropdownMenu(
                    expanded = booksExpanded,
                    onDismissRequest = { booksExpanded = false },
                ) {
                    booksList.forEach { book ->
                        DropdownMenuItem(
                            text = { Text(text = book.name) },
                            onClick = {
                                booksExpanded = false
                                selectedBooksId = book.id
                            },
                        )
                    }
                }
            }

            // 日期范围
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // 开始日期
                Row(
                    modifier = Modifier.clickable(enabled = !isExporting) {
                        showDatePicker = true
                    },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(id = R.string.export_start_date) + "：",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = if (selectedStartDate > 0) displayDateFormat.format(Date(selectedStartDate)) else "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                // 结束日期
                Row(
                    modifier = Modifier.clickable(enabled = !isExporting) {
                        showDatePicker = true
                    },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(id = R.string.export_end_date) + "：",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = if (selectedEndDate > 0) displayDateFormat.format(Date(selectedEndDate)) else "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            // 导出按钮
            Button(
                onClick = {
                    val queryEndDate = selectedEndDate + 86_400_000L
                    onExportRecords(
                        selectedBooksId,
                        selectedStartDate,
                        queryEndDate,
                        selectedBook?.name ?: "未知账本",
                        selectedStartDate,
                        selectedEndDate,
                        context.cacheDir,
                    )
                },
                enabled = recordCount > 0 && !isExporting,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (isExporting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text(
                        text = if (recordCount > 0) {
                            stringResource(id = R.string.export_confirm, recordCount)
                        } else {
                            stringResource(id = R.string.export_confirm_empty)
                        },
                    )
                }
            }
        }
    }

    // 日期范围选择器
    if (showDatePicker) {
        DateRangePickerDialog(
            onDismissRequest = { showDatePicker = false },
            onPositiveButtonClick = { (start, end) ->
                selectedStartDate = start
                selectedEndDate = end
                showDatePicker = false
            },
            onNegativeButtonClick = { showDatePicker = false },
            selection = if (selectedStartDate > 0 && selectedEndDate > 0) {
                selectedStartDate to selectedEndDate
            } else {
                null
            },
        )
    }
}
```

> **关键注意事项**：
> 1. 所有组件必须使用 Cb* 封装版本（`CbModalBottomSheet`、`CbListItem` 等），不能直接用 Material3
> 2. `onShowSnackbar` 回调名需与 Screen 中现有的 Snackbar 回调一致（阅读现有代码确认）
> 3. `FileProvider` authority 格式为 `${context.packageName}.FileProvider`（大写 F，与 AndroidManifest 一致）
> 4. `DateRangePickerDialog` 返回的时间戳是 UTC midnight，用于查询时需要 +1 天作为 exclusive end
> 5. `CircularProgressIndicator` 的 `size` 通过 `Modifier.size()` 设置

- [ ] **Step 6: 运行格式化**

```bash
./gradlew spotlessApply --init-script gradle/init.gradle.kts --no-configuration-cache
```

- [ ] **Step 7: 运行 Lint 检查**

```bash
./gradlew :feature:settings:lintDebug
```

Expected: 无 Design Detector error（所有组件使用 Cb* 版本）

如果有 lint error，检查是否不小心使用了 Material3 原始组件。

- [ ] **Step 8: 编译验证**

```bash
./gradlew :app:assembleOnlineDebug
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 9: Commit**

```bash
git add feature/settings/src/main/kotlin/cn/wj/android/cashbook/feature/settings/screen/BackupAndRecoveryScreen.kt
git commit -m "[feat|all|导出一日记账][公共]新增导出入口和ExportBottomSheet"
```

---

### Task 9: 集成验证

- [ ] **Step 1: 运行所有单元测试**

```bash
./gradlew testOnlineDebugUnitTest
```

Expected: ALL TESTS PASSED

- [ ] **Step 2: 运行完整 Lint**

```bash
./gradlew :app:lintOnlineRelease -Dlint.baselines.continue=true
```

Expected: 无新增 error

- [ ] **Step 3: 运行格式化检查**

```bash
./gradlew spotlessCheck --init-script gradle/init.gradle.kts --no-configuration-cache
```

Expected: BUILD SUCCESSFUL（无格式问题）

- [ ] **Step 4: 构建 APK**

```bash
./gradlew :app:assembleOnlineDebug
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 5: 手动功能验证**

安装到设备/模拟器后验证：
1. 打开 设置 → 备份与恢复，确认"导出账单"区块显示在"导入账单"下方
2. 点击"导出到一日记账"，确认 Bottom Sheet 弹出
3. 确认默认账本为当前账本，日期范围为最早记录到今天
4. 切换账本，确认记录数更新
5. 修改日期范围，确认记录数更新
6. 点击"导出"，确认系统分享 Intent 弹出
7. 选择"保存到文件"，保存 CSV 后打开验证内容格式
8. 验证 CSV：UTF-8 BOM、表头、日期格式、金额两位小数、分类/子分类正确

- [ ] **Step 6: 验证通过后 Commit（如有修复）**

```bash
git add -A
git commit -m "[fix|all|导出一日记账][公共]集成验证修复"
```

仅在验证过程中发现并修复了问题时才执行此 commit。
