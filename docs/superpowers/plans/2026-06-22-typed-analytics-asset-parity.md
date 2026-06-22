# 分类/标签统计界面对齐资产优化 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 给分类统计/标签统计界面（同一个 `TypedAnalyticsScreen`）补齐资产详情页的「月份切换器 + 收入/支出/结余汇总卡 + 按日分组」。

**Architecture:** 复用既有 `DateSelectionEntity`（四态 + `toDateRange()` 半开区间毫秒 + `getDisplayText()`）做周期模型；标签路径补齐毫秒区间查询全链路（DAO→Repository→UseCase）；新增类型/标签通用 `GetTypedMonthSummaryUseCase`（净自付口径）；抽资产页私有 header/汇总/日期头为 `feature:records` 共享 internal 组件供两屏共用。

**Tech Stack:** Kotlin、Jetpack Compose、Room、Hilt、Paging3、Coroutines/Flow、JUnit4 + Truth、Roborazzi。

## Global Constraints

> 每个 Task 的要求隐含包含本节，逐条 verbatim 来自 spec / 项目 CLAUDE.md。

- 金额单位：分（Long）。汇总口径 = **净自付** `analyticsPieNetAmount(typeCategory, finalAmount, amount, charges, concessions)`（`core/model/.../model/RecordAmount.kt:66`），**刻意区别于资产页 `recordAmount`，禁止照抄 recordAmount**。
- **TRANSFER 必须在求和前 `continue` 排除**（CLAUDE.md 金丝雀，选错静默算错）。
- 周期模型**复用 `DateSelectionEntity`**（`core/model/.../entity/DateSelectionEntity.kt`），**不另造 period 类型 / 不新增第三份日期解析函数**。
- 设计系统封装强制：用 `Cb*` 组件，禁止直接用 Material3（`CbCard`/`CbIconButton`/`CbScaffold`/`CbTopAppBar` 等）；访问 `MaterialTheme.colorScheme/.typography` 属性允许。
- 测试替身（`FakeRecordRepository`/`FakeRecordDao`）必须**忠实复刻真实 SQL 语义**（按 id 过滤 + `record_time` 半开区间 `>=start && <end` + booksId），禁止宽松 `drop/take` 桩。
- 表查询必须 `books_id=:booksId` 隔离；新 `@Query` 用 `:param` 命名绑定，禁止字符串拼接。
- License Header：每个新建 Kotlin 文件须含 Apache 2.0 License Header（照抄同模块既有文件头部）。
- Compose/ViewModel 签名变更必须同步更新该模块 `src/test` 下截图测试与 ViewModelTest，否则整模块 `testDebugUnitTest` 编译失败。
- 提交信息格式：`[类型|模块|功能][公共]说明`，结尾附 `Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>`。

## 模块测试任务名速查
- JVM 库（`core:model`）：`./gradlew :core:model:test`
- Android 库（`core:data`/`core:domain`/`feature:records`）：`./gradlew :core:data:testDebugUnitTest` 等
- 截图：`./gradlew :feature:records:verifyRoborazziDebug` / `recordRoborazziDebug`
- DAO instrumented（设备门控）：`./gradlew :core:database:connectedDebugAndroidTest`

---

## Task 1: `DateSelectionEntity.fromDisplayTextOrNull`（core:model）

把入口 `date` 显示文本逆向解析为 `DateSelectionEntity`，替代旧 `TypedAnalyticsViewModel.DateData` 解析。

**Files:**
- Modify: `core/model/src/main/kotlin/cn/wj/android/cashbook/core/model/entity/DateSelectionEntity.kt`
- Test: `core/model/src/test/kotlin/cn/wj/android/cashbook/core/model/entity/DateSelectionEntityTest.kt`（新建，若已存在则追加）

**Interfaces:**
- Produces: `DateSelectionEntity.fromDisplayTextOrNull(text: String): DateSelectionEntity?`（companion）。`""`/`全部`/非法 → 对应 `All` 或 `null`（见用例）；`YYYY-MM`→`ByMonth`；`YYYY`→`ByYear`；`YYYY-MM-DD~YYYY-MM-DD`→`DateRange`；`YYYY-MM-DD`→`ByDay`。

- [ ] **Step 1: 写失败测试**

```kotlin
// core/model/src/test/kotlin/cn/wj/android/cashbook/core/model/entity/DateSelectionEntityTest.kt
package cn.wj.android.cashbook.core.model.entity

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class DateSelectionEntityTest {

    @Test
    fun fromDisplayTextOrNull_month() {
        assertThat(DateSelectionEntity.fromDisplayTextOrNull("2024-06"))
            .isEqualTo(DateSelectionEntity.ByMonth(YearMonth.of(2024, 6)))
    }

    @Test
    fun fromDisplayTextOrNull_year() {
        assertThat(DateSelectionEntity.fromDisplayTextOrNull("2024"))
            .isEqualTo(DateSelectionEntity.ByYear(2024))
    }

    @Test
    fun fromDisplayTextOrNull_range() {
        assertThat(DateSelectionEntity.fromDisplayTextOrNull("2024-01-01~2024-03-31"))
            .isEqualTo(
                DateSelectionEntity.DateRange(
                    LocalDate.of(2024, 1, 1),
                    LocalDate.of(2024, 3, 31),
                ),
            )
    }

    @Test
    fun fromDisplayTextOrNull_all() {
        assertThat(DateSelectionEntity.fromDisplayTextOrNull("全部")).isEqualTo(DateSelectionEntity.All)
    }

    @Test
    fun fromDisplayTextOrNull_blank_returns_null() {
        assertThat(DateSelectionEntity.fromDisplayTextOrNull("")).isNull()
        assertThat(DateSelectionEntity.fromDisplayTextOrNull("   ")).isNull()
    }

    @Test
    fun fromDisplayTextOrNull_invalid_returns_null() {
        assertThat(DateSelectionEntity.fromDisplayTextOrNull("2024-13")).isNull()
        assertThat(DateSelectionEntity.fromDisplayTextOrNull("abc")).isNull()
    }
}
```

- [ ] **Step 2: 跑测试确认失败**

Run: `./gradlew :core:model:test --tests "*DateSelectionEntityTest"`
Expected: 编译失败 / FAIL（`fromDisplayTextOrNull` 未定义）

- [ ] **Step 3: 实现**

在 `DateSelectionEntity.kt` 内、`sealed class DateSelectionEntity(...) {` 体内 `getDisplayText()` 之后加 companion（已 import `LocalDate`/`YearMonth`）：

```kotlin
    companion object {
        /**
         * 将 [getDisplayText] 产出的显示文本逆向解析为 [DateSelectionEntity]。
         * 空白/非法 → null；`全部` → [All]；`YYYY-MM` → [ByMonth]；`YYYY` → [ByYear]；
         * `YYYY-MM-DD~YYYY-MM-DD` → [DateRange]；`YYYY-MM-DD` → [ByDay]。
         */
        fun fromDisplayTextOrNull(text: String): DateSelectionEntity? {
            val s = text.trim()
            if (s.isBlank()) return null
            if (s == "全部") return All
            return runCatching {
                if (s.contains("~")) {
                    val parts = s.split("~", limit = 2)
                    DateRange(LocalDate.parse(parts[0].trim()), LocalDate.parse(parts[1].trim()))
                } else {
                    val seg = s.split("-")
                    when (seg.size) {
                        1 -> ByYear(seg[0].toInt())
                        2 -> ByMonth(YearMonth.of(seg[0].toInt(), seg[1].toInt()))
                        3 -> ByDay(LocalDate.of(seg[0].toInt(), seg[1].toInt(), seg[2].toInt()))
                        else -> null
                    }
                }
            }.getOrNull()
        }
    }
```

- [ ] **Step 4: 跑测试确认通过**

Run: `./gradlew :core:model:test --tests "*DateSelectionEntityTest"`
Expected: PASS（6 用例）

- [ ] **Step 5: 提交**

```bash
git add core/model/src/main/kotlin/cn/wj/android/cashbook/core/model/entity/DateSelectionEntity.kt core/model/src/test/kotlin/cn/wj/android/cashbook/core/model/entity/DateSelectionEntityTest.kt
git commit -m "$(printf '[feat|core|周期解析][公共]DateSelectionEntity 加 fromDisplayTextOrNull 逆向解析\n\nCo-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>')"
```

---

## Task 2: 标签按日期范围查询 DAO（core:database）

**Files:**
- Modify: `core/database/src/main/kotlin/cn/wj/android/cashbook/core/database/dao/RecordDao.kt`（在 `queryRecordByTagId`（:241-260）后新增）
- Test: `core/database/src/androidTest/.../RecordDaoTest.kt`（设备门控，追加用例；本机无设备只编译，跑在 Task 8 后或有设备时）

**Interfaces:**
- Produces: `suspend fun queryRecordByTagIdBetween(booksId: Long, tagId: Long, startDate: Long, endDate: Long, pageNum: Int, pageSize: Int): List<RecordTable>`

- [ ] **Step 1: 新增 DAO 查询**

在 `RecordDao.kt` 的 `queryRecordByTagId(...)` 方法之后插入：

```kotlin
    /** 标签 id 为 [tagId] 且 record_time 在 [[startDate], [endDate]) 半开区间的第 [pageNum] 页 [pageSize] 条记录 */
    @Query(
        """
        SELECT * FROM db_record
        WHERE books_id=:booksId
        AND record_time>=:startDate AND record_time<:endDate
        AND id IN (
            SELECT record_id FROM db_tag_with_record
            WHERE tag_id=:tagId
        )
        ORDER BY record_time DESC LIMIT :pageSize OFFSET :pageNum
    """,
    )
    suspend fun queryRecordByTagIdBetween(
        booksId: Long,
        tagId: Long,
        startDate: Long,
        endDate: Long,
        pageNum: Int,
        pageSize: Int,
    ): List<RecordTable>
```

- [ ] **Step 2: 追加 androidTest 用例（设备门控）**

在 `RecordDaoTest.kt` 追加（按文件既有 fixture 风格构造记录/标签关联；`db_tag_with_record` 列 `tag_id`/`record_id`）：

```kotlin
    @Test
    fun queryRecordByTagIdBetween_filters_by_tag_and_halfOpen_range() = runTest {
        // 插入：tag=10 的两条记录（一条在区间内、一条在区间外）+ tag=20 的一条（区间内但 tag 不匹配）
        // 断言：仅返回 tag=10 且 record_time in [start, end) 的那一条
        // （具体插入辅助沿用本测试类既有 helper，如 insertRecord/insertTagRelation）
    }
```

> 本机无模拟器，本步只保证编译通过；运行在有设备时执行 `./gradlew :core:database:connectedDebugAndroidTest`。JVM 级忠实覆盖由 Task 3 的 Fake + Task 4 用例承担。

- [ ] **Step 3: 编译验证**

Run: `./gradlew :core:database:compileDebugKotlin :core:database:compileDebugAndroidTestKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: 提交**

```bash
git add core/database/src/main/kotlin/cn/wj/android/cashbook/core/database/dao/RecordDao.kt core/database/src/androidTest
git commit -m "$(printf '[feat|core|标签日期查询][公共]RecordDao 新增 queryRecordByTagIdBetween 半开区间查询\n\nCo-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>')"
```

---

## Task 3: Repository 标签区间分页 + 类型/标签汇总全量查询（core:data）

**Files:**
- Modify: `core/data/src/main/kotlin/cn/wj/android/cashbook/core/data/repository/RecordRepository.kt`（接口）
- Modify: `core/data/src/main/kotlin/cn/wj/android/cashbook/core/data/repository/impl/RecordRepositoryImpl.kt`
- Modify: `core/testing/src/main/kotlin/cn/wj/android/cashbook/core/testing/repository/FakeRecordRepository.kt`
- Test: `core/data/src/test/kotlin/cn/wj/android/cashbook/core/data/repository/impl/RecordRepositoryImplTest.kt`（追加，若 impl 直接可测）

**Interfaces:**
- Produces（接口新增三方法）:
  - `suspend fun queryPagingRecordListByTagIdBetween(tagId: Long, startDate: Long, endDate: Long, page: Int, pageSize: Int): List<RecordModel>`
  - `suspend fun queryRecordsByTypeIdInRange(typeId: Long, startDate: Long, endDate: Long, includeChildTypes: Boolean): List<RecordModel>`
  - `suspend fun queryRecordsByTagIdInRange(tagId: Long, startDate: Long, endDate: Long): List<RecordModel>`
- Produces（FakeRecordRepository 新增）: `fun addTagRelation(tagId: Long, recordId: Long)` 测试辅助 + 上述三方法忠实 override + **修既有宽松 `queryPagingRecordListByTagId` 桩**。

- [ ] **Step 1: 接口新增三方法**

在 `RecordRepository.kt` 的 `queryPagingRecordListByTagId(...)`（:88-92）之后插入：

```kotlin
    /** 标签 [tagId] 在 [[startDate], [endDate]) 的分页记录 */
    suspend fun queryPagingRecordListByTagIdBetween(
        tagId: Long,
        startDate: Long,
        endDate: Long,
        page: Int,
        pageSize: Int,
    ): List<RecordModel>

    /** 类型 [typeId]（按 [includeChildTypes]）在 [[startDate], [endDate]) 的全量记录（汇总用，非分页） */
    suspend fun queryRecordsByTypeIdInRange(
        typeId: Long,
        startDate: Long,
        endDate: Long,
        includeChildTypes: Boolean,
    ): List<RecordModel>

    /** 标签 [tagId] 在 [[startDate], [endDate]) 的全量记录（汇总用，非分页） */
    suspend fun queryRecordsByTagIdInRange(
        tagId: Long,
        startDate: Long,
        endDate: Long,
    ): List<RecordModel>
```

- [ ] **Step 2: impl 实现**

在 `RecordRepositoryImpl.kt` 的 `queryPagingRecordListByTagId(...)`（:259-272）之后插入（复用 Task 2 新 DAO 与既有类型 between DAO；booksId 取 `currentBookId`）：

```kotlin
    override suspend fun queryPagingRecordListByTagIdBetween(
        tagId: Long,
        startDate: Long,
        endDate: Long,
        page: Int,
        pageSize: Int,
    ): List<RecordModel> = withContext(coroutineContext) {
        recordDao.queryRecordByTagIdBetween(
            booksId = combineProtoDataSource.recordSettingsData.first().currentBookId,
            tagId = tagId,
            startDate = startDate,
            endDate = endDate,
            pageNum = page * pageSize,
            pageSize = pageSize,
        ).map { it.asModel() }
    }

    override suspend fun queryRecordsByTypeIdInRange(
        typeId: Long,
        startDate: Long,
        endDate: Long,
        includeChildTypes: Boolean,
    ): List<RecordModel> = withContext(coroutineContext) {
        val booksId = combineProtoDataSource.recordSettingsData.first().currentBookId
        if (includeChildTypes) {
            recordDao.queryRecordByTypeIdBetween(booksId, typeId, startDate, endDate, pageNum = 0, pageSize = Int.MAX_VALUE)
        } else {
            recordDao.queryRecordByTypeIdExactBetween(booksId, typeId, startDate, endDate, pageNum = 0, pageSize = Int.MAX_VALUE)
        }.map { it.asModel() }
    }

    override suspend fun queryRecordsByTagIdInRange(
        tagId: Long,
        startDate: Long,
        endDate: Long,
    ): List<RecordModel> = withContext(coroutineContext) {
        recordDao.queryRecordByTagIdBetween(
            booksId = combineProtoDataSource.recordSettingsData.first().currentBookId,
            tagId = tagId,
            startDate = startDate,
            endDate = endDate,
            pageNum = 0,
            pageSize = Int.MAX_VALUE,
        ).map { it.asModel() }
    }
```

- [ ] **Step 3: FakeRecordRepository 忠实桩（写失败测试前先备齐替身）**

在 `FakeRecordRepository.kt`：① `private val records = ...` 附近加标签关联存储与辅助；② 修既有宽松 `queryPagingRecordListByTagId`；③ 新增三 override：

```kotlin
    // 顶部 records 字段附近新增：
    private val tagRecordIds = mutableMapOf<Long, MutableSet<Long>>()

    fun addTagRelation(tagId: Long, recordId: Long) {
        tagRecordIds.getOrPut(tagId) { mutableSetOf() }.add(recordId)
    }

    private fun recordsOfTag(tagId: Long): List<RecordModel> {
        val ids = tagRecordIds[tagId].orEmpty()
        return records.filter { it.id in ids }
    }
```

```kotlin
    // 替换既有宽松 queryPagingRecordListByTagId（:168-174）为忠实按 tagId 过滤：
    override suspend fun queryPagingRecordListByTagId(
        tagId: Long,
        page: Int,
        pageSize: Int,
    ): List<RecordModel> {
        return recordsOfTag(tagId)
            .sortedByDescending { it.recordTime }
            .drop(page * pageSize)
            .take(pageSize)
    }

    override suspend fun queryPagingRecordListByTagIdBetween(
        tagId: Long,
        startDate: Long,
        endDate: Long,
        page: Int,
        pageSize: Int,
    ): List<RecordModel> {
        return recordsOfTag(tagId)
            .filter { it.recordTime >= startDate && it.recordTime < endDate }
            .sortedByDescending { it.recordTime }
            .drop(page * pageSize)
            .take(pageSize)
    }

    override suspend fun queryRecordsByTypeIdInRange(
        typeId: Long,
        startDate: Long,
        endDate: Long,
        includeChildTypes: Boolean,
    ): List<RecordModel> {
        // Fake 不建模父子类型，按精确 typeId + 半开区间过滤（test 用直接 typeId）
        return records.filter { it.typeId == typeId && it.recordTime >= startDate && it.recordTime < endDate }
    }

    override suspend fun queryRecordsByTagIdInRange(
        tagId: Long,
        startDate: Long,
        endDate: Long,
    ): List<RecordModel> {
        return recordsOfTag(tagId).filter { it.recordTime >= startDate && it.recordTime < endDate }
    }
```

- [ ] **Step 4: 编译 + 跑现有 core:data / core:testing 依赖测试确认未破坏**

Run: `./gradlew :core:data:testDebugUnitTest`
Expected: BUILD SUCCESSFUL（FakeRecordRepository 接口实现完整，既有测试仍绿）

- [ ] **Step 5: 提交**

```bash
git add core/data/src/main/kotlin/cn/wj/android/cashbook/core/data/repository/RecordRepository.kt core/data/src/main/kotlin/cn/wj/android/cashbook/core/data/repository/impl/RecordRepositoryImpl.kt core/testing/src/main/kotlin/cn/wj/android/cashbook/core/testing/repository/FakeRecordRepository.kt
git commit -m "$(printf '[feat|core|标签区间查询][公共]Repository 标签区间分页+类型标签汇总全量查询+Fake 忠实桩\n\nCo-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>')"
```

---

## Task 4: `GetTagRecordViewsUseCase` 加日期区间（core:domain）

**Files:**
- Modify: `core/domain/src/main/kotlin/cn/wj/android/cashbook/domain/usecase/GetTagRecordViewsUseCase.kt`
- Test: `core/domain/src/test/kotlin/cn/wj/android/cashbook/domain/usecase/GetTagRecordViewsUseCaseTest.kt`（新建，若已存在则追加）

**Interfaces:**
- Consumes: `RecordRepository.queryPagingRecordListByTagIdBetween`（Task 3）
- Produces: `suspend operator fun invoke(tagId: Long, startDate: Long, endDate: Long, pageNum: Int, pageSize: Int): List<RecordViewsEntity>`（始终走区间；全部 = `0..Long.MAX_VALUE`）

- [ ] **Step 1: 写失败测试**

```kotlin
// core/domain/src/test/kotlin/cn/wj/android/cashbook/domain/usecase/GetTagRecordViewsUseCaseTest.kt
package cn.wj.android.cashbook.domain.usecase

import cn.wj.android.cashbook.core.model.model.RecordModel
import cn.wj.android.cashbook.core.testing.repository.FakeAssetRepository
import cn.wj.android.cashbook.core.testing.repository.FakeRecordRepository
import cn.wj.android.cashbook.core.testing.repository.FakeTagRepository
import cn.wj.android.cashbook.core.testing.repository.FakeTypeRepository
import cn.wj.android.cashbook.core.testing.util.TestDispatcherRule
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class GetTagRecordViewsUseCaseTest {

    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    private fun createRecord(id: Long, time: Long) = RecordModel(
        id = id, booksId = 1L, typeId = 1L, assetId = -1L, relatedAssetId = -1L,
        amount = 100L, finalAmount = 100L, charges = 0L, concessions = 0L,
        remark = "", reimbursable = false, recordTime = time,
    )

    private fun useCase(repo: FakeRecordRepository): GetTagRecordViewsUseCase {
        val trans = RecordModelTransToViewsUseCase(
            recordRepository = repo,
            typeRepository = FakeTypeRepository(),
            assetRepository = FakeAssetRepository(),
            tagRepository = FakeTagRepository(),
            coroutineContext = dispatcherRule.testDispatcher,
        )
        return GetTagRecordViewsUseCase(repo, trans, dispatcherRule.testDispatcher)
    }

    @Test
    fun invoke_filters_by_tag_and_range() = runTest {
        val repo = FakeRecordRepository()
        repo.addRecord(createRecord(1L, 1_000L)) // in range, tag 10
        repo.addRecord(createRecord(2L, 9_000L)) // out of range, tag 10
        repo.addRecord(createRecord(3L, 1_000L)) // in range, tag 20
        repo.addTagRelation(10L, 1L)
        repo.addTagRelation(10L, 2L)
        repo.addTagRelation(20L, 3L)

        val result = useCase(repo)(tagId = 10L, startDate = 0L, endDate = 5_000L, pageNum = 0, pageSize = 20)

        assertThat(result.map { it.id }).containsExactly(1L)
    }

    @Test
    fun invoke_full_range_returns_all_of_tag() = runTest {
        val repo = FakeRecordRepository()
        repo.addRecord(createRecord(1L, 1_000L))
        repo.addRecord(createRecord(2L, 9_000L))
        repo.addTagRelation(10L, 1L)
        repo.addTagRelation(10L, 2L)

        val result = useCase(repo)(tagId = 10L, startDate = 0L, endDate = Long.MAX_VALUE, pageNum = 0, pageSize = 20)

        assertThat(result.map { it.id }).containsExactly(1L, 2L)
    }

    @Test
    fun invoke_invalid_tag_returns_empty() = runTest {
        val result = useCase(FakeRecordRepository())(tagId = -1L, startDate = 0L, endDate = Long.MAX_VALUE, pageNum = 0, pageSize = 20)
        assertThat(result).isEmpty()
    }
}
```

- [ ] **Step 2: 跑测试确认失败**

Run: `./gradlew :core:domain:testDebugUnitTest --tests "*GetTagRecordViewsUseCaseTest"`
Expected: 编译失败（invoke 签名不匹配）

- [ ] **Step 3: 改 UseCase 签名**

替换 `GetTagRecordViewsUseCase.kt` 的 `invoke`：

```kotlin
    suspend operator fun invoke(
        tagId: Long,
        startDate: Long,
        endDate: Long,
        pageNum: Int,
        pageSize: Int,
    ): List<RecordViewsEntity> = withContext(coroutineContext) {
        if (tagId == -1L) {
            return@withContext emptyList()
        }
        recordModelTransToViewsUseCase(
            recordRepository.queryPagingRecordListByTagIdBetween(tagId, startDate, endDate, pageNum, pageSize)
                .sortedByDescending { it.recordTime },
        ).map { it.asEntity() }
    }
```

- [ ] **Step 4: 跑测试确认通过**

Run: `./gradlew :core:domain:testDebugUnitTest --tests "*GetTagRecordViewsUseCaseTest"`
Expected: PASS（3 用例）

- [ ] **Step 5: 提交**

```bash
git add core/domain/src/main/kotlin/cn/wj/android/cashbook/domain/usecase/GetTagRecordViewsUseCase.kt core/domain/src/test/kotlin/cn/wj/android/cashbook/domain/usecase/GetTagRecordViewsUseCaseTest.kt
git commit -m "$(printf '[feat|core|标签区间用例][公共]GetTagRecordViewsUseCase 加日期区间参数\n\nCo-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>')"
```

---

## Task 5: `GetTypedMonthSummaryUseCase`（core:domain）

类型/标签通用的「收入/支出/结余」汇总，净自付口径，TRANSFER 排除。

**Files:**
- Create: `core/domain/src/main/kotlin/cn/wj/android/cashbook/domain/usecase/GetTypedMonthSummaryUseCase.kt`
- Test: `core/domain/src/test/kotlin/cn/wj/android/cashbook/domain/usecase/GetTypedMonthSummaryUseCaseTest.kt`

**Interfaces:**
- Consumes: `RecordRepository.queryRecordsByTypeIdInRange`/`queryRecordsByTagIdInRange`（Task 3）、`TypeRepository.getRecordTypeCategories`、`analyticsPieNetAmount`
- Produces: `suspend operator fun invoke(isType: Boolean, id: Long, startDate: Long, endDate: Long, includeChildTypes: Boolean): AssetMonthSummaryModel`

- [ ] **Step 1: 写失败测试**

```kotlin
// core/domain/src/test/kotlin/cn/wj/android/cashbook/domain/usecase/GetTypedMonthSummaryUseCaseTest.kt
package cn.wj.android.cashbook.domain.usecase

import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.model.enums.TypeLevelEnum
import cn.wj.android.cashbook.core.model.model.RecordModel
import cn.wj.android.cashbook.core.model.model.RecordTypeModel
import cn.wj.android.cashbook.core.testing.repository.FakeRecordRepository
import cn.wj.android.cashbook.core.testing.repository.FakeTypeRepository
import cn.wj.android.cashbook.core.testing.util.TestDispatcherRule
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class GetTypedMonthSummaryUseCaseTest {

    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    private val typeRepository = FakeTypeRepository()

    private fun type(id: Long, category: RecordTypeCategoryEnum) = RecordTypeModel(
        id = id, parentId = -1L, name = "t$id", iconName = "i",
        typeLevel = TypeLevelEnum.FIRST, typeCategory = category,
        protected = false, sort = 0, needRelated = false,
    )

    private fun record(id: Long, typeId: Long, amount: Long, finalAmount: Long, time: Long = 1_000L) = RecordModel(
        id = id, booksId = 1L, typeId = typeId, assetId = -1L, relatedAssetId = -1L,
        amount = amount, finalAmount = finalAmount, charges = 0L, concessions = 0L,
        remark = "", reimbursable = false, recordTime = time,
    )

    private fun useCase(repo: FakeRecordRepository) =
        GetTypedMonthSummaryUseCase(repo, typeRepository, dispatcherRule.testDispatcher)

    @Test
    fun expenditure_type_uses_finalAmount_net() = runTest {
        typeRepository.addType(type(1L, RecordTypeCategoryEnum.EXPENDITURE))
        val repo = FakeRecordRepository()
        repo.addRecord(record(id = 1L, typeId = 1L, amount = 500L, finalAmount = 0L)) // 被报销，净自付 0
        repo.addRecord(record(id = 2L, typeId = 1L, amount = 300L, finalAmount = 300L))

        val s = useCase(repo)(isType = true, id = 1L, startDate = 0L, endDate = Long.MAX_VALUE, includeChildTypes = true)

        assertThat(s.expenditure).isEqualTo(300L) // 0 + 300（净自付）
        assertThat(s.income).isEqualTo(0L)
        assertThat(s.balance).isEqualTo(-300L)
    }

    @Test
    fun transfer_type_excluded_returns_zero() = runTest {
        typeRepository.addType(type(2L, RecordTypeCategoryEnum.TRANSFER))
        val repo = FakeRecordRepository()
        repo.addRecord(record(id = 1L, typeId = 2L, amount = 1000L, finalAmount = 1000L))

        val s = useCase(repo)(isType = true, id = 2L, startDate = 0L, endDate = Long.MAX_VALUE, includeChildTypes = true)

        assertThat(s.income).isEqualTo(0L)
        assertThat(s.expenditure).isEqualTo(0L)
        assertThat(s.balance).isEqualTo(0L)
    }

    @Test
    fun tag_mixed_income_and_expenditure_both_nonzero() = runTest {
        typeRepository.addType(type(1L, RecordTypeCategoryEnum.EXPENDITURE))
        typeRepository.addType(type(3L, RecordTypeCategoryEnum.INCOME))
        val repo = FakeRecordRepository()
        repo.addRecord(record(id = 1L, typeId = 1L, amount = 200L, finalAmount = 200L))
        repo.addRecord(record(id = 2L, typeId = 3L, amount = 500L, finalAmount = 500L))
        repo.addTagRelation(10L, 1L)
        repo.addTagRelation(10L, 2L)

        val s = useCase(repo)(isType = false, id = 10L, startDate = 0L, endDate = Long.MAX_VALUE, includeChildTypes = true)

        assertThat(s.income).isEqualTo(500L)
        assertThat(s.expenditure).isEqualTo(200L)
        assertThat(s.balance).isEqualTo(300L)
    }

    @Test
    fun range_filters_out_records() = runTest {
        typeRepository.addType(type(1L, RecordTypeCategoryEnum.EXPENDITURE))
        val repo = FakeRecordRepository()
        repo.addRecord(record(id = 1L, typeId = 1L, amount = 100L, finalAmount = 100L, time = 1_000L))
        repo.addRecord(record(id = 2L, typeId = 1L, amount = 999L, finalAmount = 999L, time = 9_000L))

        val s = useCase(repo)(isType = true, id = 1L, startDate = 0L, endDate = 5_000L, includeChildTypes = true)

        assertThat(s.expenditure).isEqualTo(100L)
    }
}
```

- [ ] **Step 2: 跑测试确认失败**

Run: `./gradlew :core:domain:testDebugUnitTest --tests "*GetTypedMonthSummaryUseCaseTest"`
Expected: 编译失败（类未定义）

- [ ] **Step 3: 实现 UseCase**

```kotlin
// core/domain/src/main/kotlin/cn/wj/android/cashbook/domain/usecase/GetTypedMonthSummaryUseCase.kt
/* Apache 2.0 License Header —— 照抄 GetAssetMonthSummaryUseCase.kt 头部 */
package cn.wj.android.cashbook.domain.usecase

import cn.wj.android.cashbook.core.common.annotation.CashbookDispatchers
import cn.wj.android.cashbook.core.common.annotation.Dispatcher
import cn.wj.android.cashbook.core.data.repository.RecordRepository
import cn.wj.android.cashbook.core.data.repository.TypeRepository
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.model.model.AssetMonthSummaryModel
import cn.wj.android.cashbook.core.model.model.analyticsPieNetAmount
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

/**
 * 计算指定类型/标签在 [[startDate], [endDate]) 的收入/支出/结余汇总。
 * 净自付口径（[analyticsPieNetAmount]，对齐数据分析饼图，刻意区别于 GetAssetMonthSummaryUseCase 的 recordAmount）。
 * TRANSFER 记录不计入收支卡（金丝雀：必须在求和前 continue）。
 */
class GetTypedMonthSummaryUseCase @Inject constructor(
    private val recordRepository: RecordRepository,
    private val typeRepository: TypeRepository,
    @Dispatcher(CashbookDispatchers.IO) private val coroutineContext: CoroutineContext,
) {

    suspend operator fun invoke(
        isType: Boolean,
        id: Long,
        startDate: Long,
        endDate: Long,
        includeChildTypes: Boolean,
    ): AssetMonthSummaryModel = withContext(coroutineContext) {
        if (id == -1L) return@withContext AssetMonthSummaryModel(0L, 0L, 0L)
        val records = if (isType) {
            recordRepository.queryRecordsByTypeIdInRange(id, startDate, endDate, includeChildTypes)
        } else {
            recordRepository.queryRecordsByTagIdInRange(id, startDate, endDate)
        }
        val categoryMap = typeRepository.getRecordTypeCategories(records.map { it.typeId })
        var income = 0L
        var expenditure = 0L
        for (record in records) {
            val category = categoryMap[record.typeId] ?: continue
            if (category == RecordTypeCategoryEnum.TRANSFER) continue
            val amount = analyticsPieNetAmount(
                typeCategory = category,
                finalAmount = record.finalAmount,
                amount = record.amount,
                charges = record.charges,
                concessions = record.concessions,
            )
            when (category) {
                RecordTypeCategoryEnum.INCOME -> income += amount
                RecordTypeCategoryEnum.EXPENDITURE -> expenditure += amount
                else -> Unit
            }
        }
        AssetMonthSummaryModel(income = income, expenditure = expenditure, balance = income - expenditure)
    }
}
```

- [ ] **Step 4: 跑测试确认通过**

Run: `./gradlew :core:domain:testDebugUnitTest --tests "*GetTypedMonthSummaryUseCaseTest"`
Expected: PASS（4 用例）

- [ ] **Step 5: 提交**

```bash
git add core/domain/src/main/kotlin/cn/wj/android/cashbook/domain/usecase/GetTypedMonthSummaryUseCase.kt core/domain/src/test/kotlin/cn/wj/android/cashbook/domain/usecase/GetTypedMonthSummaryUseCaseTest.kt
git commit -m "$(printf '[feat|core|分类标签汇总][公共]新增 GetTypedMonthSummaryUseCase 净自付口径 TRANSFER 排除\n\nCo-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>')"
```

---

## Task 6: 抽共享 `RecordMonthSummaryHeader` 组件（feature:records，独立 commit）

把资产页私有 header/汇总列/日期头抽为共享 internal 组件，资产页改调共享版（恒走月份模式），靠 Roborazzi 0 diff + 既有 ViewModelTest 守回调。

**Files:**
- Create: `feature/records/src/main/kotlin/cn/wj/android/cashbook/feature/records/view/RecordMonthSummaryHeader.kt`
- Modify: `feature/records/src/main/kotlin/cn/wj/android/cashbook/feature/records/screen/AssetInfoContentScreen.kt`（删私有 `AssetMonthHeader`/`SummaryColumn`/`AssetRecordDayHeader`，改用共享组件）

**Interfaces:**
- Produces:
  - `internal fun RecordMonthSummaryHeader(periodText: String, monthSwitchable: Boolean, summary: AssetMonthSummaryModel, showTransferHint: Boolean, onPreviousMonth: () -> Unit, onNextMonth: () -> Unit)`
  - `internal fun RecordDayHeader(item: LauncherListItem.DayHeader)`

- [ ] **Step 1: 新建共享组件**（行为 = 现 `AssetMonthHeader` 月份模式 + 固定周期模式 + 转账提示模式）

```kotlin
// feature/records/src/main/kotlin/cn/wj/android/cashbook/feature/records/view/RecordMonthSummaryHeader.kt
/* Apache 2.0 License Header —— 照抄同模块文件头部 */
package cn.wj.android.cashbook.feature.records.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cn.wj.android.cashbook.core.common.ext.toMoneyCNY
import cn.wj.android.cashbook.core.design.component.CbCard
import cn.wj.android.cashbook.core.design.component.CbHorizontalDivider
import cn.wj.android.cashbook.core.design.component.CbIconButton
import cn.wj.android.cashbook.core.design.icon.CbIcons
import cn.wj.android.cashbook.core.model.model.AssetMonthSummaryModel
import cn.wj.android.cashbook.core.ui.R
import cn.wj.android.cashbook.feature.records.viewmodel.LauncherListItem

/**
 * 月份切换器 / 固定周期文字 + 收入/支出/结余 3 列汇总卡。
 *
 * @param periodText 当前周期显示文本
 * @param monthSwitchable true 显示前后翻月箭头；false 仅居中文字（固定周期）
 * @param summary 收支结余汇总
 * @param showTransferHint true 时以提示文案代替 3 列（转账类型不计入收支）
 */
@Composable
internal fun RecordMonthSummaryHeader(
    periodText: String,
    monthSwitchable: Boolean,
    summary: AssetMonthSummaryModel,
    showTransferHint: Boolean,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
) {
    CbCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                if (monthSwitchable) {
                    CbIconButton(onClick = onPreviousMonth) {
                        Icon(
                            imageVector = CbIcons.ArrowBack,
                            contentDescription = stringResource(id = R.string.cd_previous),
                        )
                    }
                }
                Text(
                    text = periodText,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
                if (monthSwitchable) {
                    CbIconButton(onClick = onNextMonth) {
                        Icon(
                            imageVector = CbIcons.KeyboardArrowRight,
                            contentDescription = stringResource(id = R.string.cd_next),
                        )
                    }
                }
            }

            if (showTransferHint) {
                Text(
                    text = stringResource(id = R.string.transfer_not_counted_in_summary),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                ) {
                    SummaryColumn(stringResource(id = R.string.month_income), summary.income, Modifier.weight(1f))
                    SummaryColumn(stringResource(id = R.string.month_expend), summary.expenditure, Modifier.weight(1f))
                    SummaryColumn(stringResource(id = R.string.month_balance), summary.balance, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun SummaryColumn(label: String, amount: Long, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = amount.toMoneyCNY(), style = MaterialTheme.typography.titleSmall)
    }
}

/** 记录列表按日分组头（资产/分类/标签统计共用） */
@Composable
internal fun RecordDayHeader(item: LauncherListItem.DayHeader) {
    val dayTypeSuffix = when (item.dayType) {
        0 -> stringResource(id = R.string.today_with_brackets)
        -1 -> stringResource(id = R.string.yesterday_with_brackets)
        -2 -> stringResource(id = R.string.before_yesterday_with_brackets)
        else -> ""
    }
    Column {
        Text(
            text = "${item.day}${stringResource(id = R.string.day)}$dayTypeSuffix",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        )
        CbHorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
    }
}
```

> 注：原 `AssetMonthHeader` 月份文字在两箭头之间无 `weight`，本共享版给文字 `weight(1f)+居中`。若资产页 0 diff 失败（文字位置漂移），保留原 `AssetMonthHeader` 的精确布局（文字不加 weight，固定周期模式另写分支），再重验。

- [ ] **Step 2: 新增字符串资源**

在 `core/ui/src/main/res/values/strings.xml`（与 `month_income` 等同文件）加：

```xml
    <string name="transfer_not_counted_in_summary">转账不计入收支统计</string>
```

- [ ] **Step 3: 资产页改用共享组件**

`AssetInfoContentScreen.kt`：删私有 `AssetMonthHeader`（:209-274）、`SummaryColumn`（:279-299）、`AssetRecordDayHeader`（:180-199）；`item { AssetMonthHeader(...) }` 改为：

```kotlin
            item {
                RecordMonthSummaryHeader(
                    periodText = dateSelection.getDisplayText(),
                    monthSwitchable = true,
                    summary = summary,
                    showTransferHint = false,
                    onPreviousMonth = onPreviousMonth,
                    onNextMonth = onNextMonth,
                )
            }
```

`AssetRecordDayHeader(item = item)` 调用改为 `RecordDayHeader(item = item)`；并 `import cn.wj.android.cashbook.feature.records.view.RecordMonthSummaryHeader` 与 `RecordDayHeader`。删除不再使用的 import（`CbCard`/`CbIconButton`/`CbHorizontalDivider`/`CbIcons`/`toMoneyCNY` 等若仅 header 用）。

- [ ] **Step 4: 编译**

Run: `./gradlew :feature:records:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: 资产页截图 0 diff 验证 + 既有 ViewModelTest 守回调**

Run: `./gradlew :feature:records:verifyRoborazziDebug --tests "*AssetInfoContentScreenScreenshotTests" :feature:records:testDebugUnitTest --tests "*AssetInfoContentViewModelTest"`
Expected: 截图 0 diff（资产 14 张） + ViewModelTest PASS。若截图非 0 diff → 按 Step 1 注释回退布局到精确等价再重验（不接受像素漂移）。

- [ ] **Step 6: 提交（独立 commit）**

```bash
git add feature/records/src/main/kotlin/cn/wj/android/cashbook/feature/records/view/RecordMonthSummaryHeader.kt feature/records/src/main/kotlin/cn/wj/android/cashbook/feature/records/screen/AssetInfoContentScreen.kt core/ui/src/main/res/values/strings.xml
git commit -m "$(printf '[refactor|feature|共享统计头][公共]抽 RecordMonthSummaryHeader/RecordDayHeader 资产页改用 0 diff\n\nCo-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>')"
```

---

## Task 7: `TypedAnalyticsViewModel` 改造（feature:records）

**Files:**
- Modify: `feature/records/src/main/kotlin/cn/wj/android/cashbook/feature/records/viewmodel/TypedAnalyticsViewModel.kt`
- Modify: `feature/records/src/test/kotlin/cn/wj/android/cashbook/feature/records/viewmodel/TypedAnalyticsViewModelTest.kt`

**Interfaces:**
- Consumes: `GetTypedMonthSummaryUseCase`（T5）、`GetTagRecordViewsUseCase(tagId,startDate,endDate,pageNum,pageSize)`（T4）、`GetTypeRecordViewsUseCase(typeId,dateStr,page,pageSize,includeChildTypes)`（既有）、`DateSelectionEntity.fromDisplayTextOrNull`（T1）、`recordDaySeparator`/`LauncherListItem`
- Produces:
  - `dateSelection: StateFlow<DateSelectionEntity>`、`summary: StateFlow<AssetMonthSummaryModel>`、`monthSwitchable: StateFlow<Boolean>`、`recordList: Flow<PagingData<LauncherListItem>>`
  - `fun updateMonth(yearMonth: YearMonth)`
  - `TypedAnalyticsUiState.Success(isType: Boolean, titleText: String, isTransferType: Boolean)`（删 `subTitleText`，加 `isTransferType`）

- [ ] **Step 1: 改 ViewModelTest（先红）**

替换 `TypedAnalyticsViewModelTest.kt` 中三个 subTitle 用例（`when_updateData_with_yearMonth_date_then_subtitleText_contains_date` / `..._year_..` / `..._empty_..`，:174-262）为周期/汇总用例，并在 setup 注入新 UseCase：

```kotlin
// setup() 内 viewModel 构造前补：
val getTypedMonthSummaryUseCase = GetTypedMonthSummaryUseCase(
    recordRepository = recordRepository,
    typeRepository = typeRepository,
    coroutineContext = dispatcherRule.testDispatcher,
)
// viewModel 构造加参 getTypedMonthSummaryUseCase = getTypedMonthSummaryUseCase
```

```kotlin
    @Test
    fun when_updateData_with_yearMonth_then_dateSelection_is_byMonth_and_switchable() = runTest {
        viewModel.updateData(tagId = -1L, typeId = 1L, date = "2024-06")
        assertThat(viewModel.dateSelection.value)
            .isEqualTo(cn.wj.android.cashbook.core.model.entity.DateSelectionEntity.ByMonth(java.time.YearMonth.of(2024, 6)))
        assertThat(viewModel.monthSwitchable.value).isTrue()
    }

    @Test
    fun when_updateData_with_year_then_fixed_period_not_switchable() = runTest {
        viewModel.updateData(tagId = -1L, typeId = 1L, date = "2024")
        assertThat(viewModel.dateSelection.value)
            .isEqualTo(cn.wj.android.cashbook.core.model.entity.DateSelectionEntity.ByYear(2024))
        assertThat(viewModel.monthSwitchable.value).isFalse()
    }

    @Test
    fun when_updateData_with_empty_then_default_current_month() = runTest {
        viewModel.updateData(tagId = -1L, typeId = 1L, date = "")
        assertThat(viewModel.dateSelection.value)
            .isInstanceOf(cn.wj.android.cashbook.core.model.entity.DateSelectionEntity.ByMonth::class.java)
        assertThat(viewModel.monthSwitchable.value).isTrue()
    }

    @Test
    fun when_updateMonth_then_dateSelection_updates() = runTest {
        viewModel.updateData(tagId = -1L, typeId = 1L, date = "2024-06")
        viewModel.updateMonth(java.time.YearMonth.of(2024, 8))
        assertThat(viewModel.dateSelection.value)
            .isEqualTo(cn.wj.android.cashbook.core.model.entity.DateSelectionEntity.ByMonth(java.time.YearMonth.of(2024, 8)))
    }

    @Test
    fun when_transfer_type_then_isTransferType_true() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        typeRepository.addType(
            RecordTypeModel(
                id = 5L, parentId = -1L, name = "转账", iconName = "i",
                typeLevel = TypeLevelEnum.FIRST, typeCategory = RecordTypeCategoryEnum.TRANSFER,
                protected = false, sort = 0, needRelated = false,
            ),
        )
        viewModel.updateData(tagId = -1L, typeId = 5L, date = "")
        val s = viewModel.uiState.value as TypedAnalyticsUiState.Success
        assertThat(s.isTransferType).isTrue()
    }
```

- [ ] **Step 2: 跑测试确认失败**

Run: `./gradlew :feature:records:testDebugUnitTest --tests "*TypedAnalyticsViewModelTest"`
Expected: 编译失败（dateSelection/monthSwitchable/updateMonth/isTransferType/新构造参 未定义）

- [ ] **Step 3: 重写 ViewModel**

```kotlin
// 替换 TypedAnalyticsViewModel.kt（保留 License Header + package）
package cn.wj.android.cashbook.feature.records.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.cachedIn
import androidx.paging.insertSeparators
import androidx.paging.map
import cn.wj.android.cashbook.core.common.DEFAULT_PAGE_SIZE
import cn.wj.android.cashbook.core.common.ext.logger
import cn.wj.android.cashbook.core.common.model.recordDataVersion
import cn.wj.android.cashbook.core.data.repository.TagRepository
import cn.wj.android.cashbook.core.data.repository.TypeRepository
import cn.wj.android.cashbook.core.model.entity.DateSelectionEntity
import cn.wj.android.cashbook.core.model.entity.RecordViewsEntity
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.model.model.AssetMonthSummaryModel
import cn.wj.android.cashbook.domain.usecase.GetTagRecordViewsUseCase
import cn.wj.android.cashbook.domain.usecase.GetTypeRecordViewsUseCase
import cn.wj.android.cashbook.domain.usecase.GetTypedMonthSummaryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.YearMonth
import javax.inject.Inject

@HiltViewModel
class TypedAnalyticsViewModel @Inject constructor(
    private val typeRepository: TypeRepository,
    private val tagRepository: TagRepository,
    private val getTypeRecordViewsUseCase: GetTypeRecordViewsUseCase,
    private val getTagRecordViewsUseCase: GetTagRecordViewsUseCase,
    private val getTypedMonthSummaryUseCase: GetTypedMonthSummaryUseCase,
) : ViewModel() {

    var viewRecord by mutableStateOf<RecordViewsEntity?>(null)
        private set

    private val _tagIdData = MutableStateFlow(-1L)
    private val _typeIdData = MutableStateFlow(-1L)
    private val _includeChildTypes = MutableStateFlow(true)
    private val _dateSelection = MutableStateFlow<DateSelectionEntity>(DateSelectionEntity.ByMonth(YearMonth.now()))
    val dateSelection: StateFlow<DateSelectionEntity> = _dateSelection

    val monthSwitchable: StateFlow<Boolean> = _dateSelection
        .map { it is DateSelectionEntity.ByMonth }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), true)

    val uiState = combine(_tagIdData, _typeIdData) { tagId, typeId ->
        if (tagId == typeId) {
            TypedAnalyticsUiState.Loading
        } else {
            val isType = typeId != -1L
            val type = if (isType) typeRepository.getRecordTypeById(typeId) else null
            TypedAnalyticsUiState.Success(
                isType = isType,
                titleText = (if (isType) type?.name else tagRepository.getTagById(tagId)?.name).orEmpty(),
                isTransferType = isType && type?.typeCategory == RecordTypeCategoryEnum.TRANSFER,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), TypedAnalyticsUiState.Loading)

    val recordList = combine(
        _tagIdData, _typeIdData, _includeChildTypes, _dateSelection, recordDataVersion,
    ) { tagId, typeId, includeChild, selection, _ ->
        val isType = typeId != -1L
        GetTypedRecordData(isType, if (isType) typeId else tagId, selection, includeChild)
    }
        .flatMapLatest { data ->
            Pager(
                config = PagingConfig(pageSize = DEFAULT_PAGE_SIZE, initialLoadSize = DEFAULT_PAGE_SIZE),
                pagingSourceFactory = {
                    if (data.isType) {
                        TypeRecordPagingSource(data.id, data.selection, data.includeChildTypes, getTypeRecordViewsUseCase)
                    } else {
                        TagRecordPagingSource(data.id, data.selection, getTagRecordViewsUseCase)
                    }
                },
            ).flow
                .map { paging -> paging.map { LauncherListItem.Record(it) as LauncherListItem } }
                .map { paging -> paging.insertSeparators { before, after -> recordDaySeparator(before, after) } }
        }
        .cachedIn(viewModelScope)

    val summary: StateFlow<AssetMonthSummaryModel> = combine(
        _tagIdData, _typeIdData, _includeChildTypes, _dateSelection, recordDataVersion,
    ) { tagId, typeId, includeChild, selection, _ ->
        val isType = typeId != -1L
        val id = if (isType) typeId else tagId
        if (id == -1L) {
            AssetMonthSummaryModel(0L, 0L, 0L)
        } else {
            val (start, end) = selection.toDateRange()
            getTypedMonthSummaryUseCase(isType, id, start, end, includeChild)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), AssetMonthSummaryModel(0L, 0L, 0L))

    fun updateData(tagId: Long, typeId: Long, date: String, includeChildTypes: Boolean = true) {
        _tagIdData.tryEmit(tagId)
        _typeIdData.tryEmit(typeId)
        _includeChildTypes.tryEmit(includeChildTypes)
        _dateSelection.tryEmit(
            DateSelectionEntity.fromDisplayTextOrNull(date) ?: DateSelectionEntity.ByMonth(YearMonth.now()),
        )
        logger().i("updateData(tagId=$tagId, typeId=$typeId, date=$date, includeChildTypes=$includeChildTypes)")
    }

    fun updateMonth(yearMonth: YearMonth) {
        _dateSelection.tryEmit(DateSelectionEntity.ByMonth(yearMonth))
    }

    fun showRecordDetailsSheet(item: RecordViewsEntity) { viewRecord = item }
    fun dismissRecordDetailSheet() { viewRecord = null }
}

data class GetTypedRecordData(
    val isType: Boolean,
    val id: Long,
    val selection: DateSelectionEntity,
    val includeChildTypes: Boolean,
)

sealed interface TypedAnalyticsUiState {
    data object Loading : TypedAnalyticsUiState
    data class Success(
        val isType: Boolean,
        val titleText: String,
        val isTransferType: Boolean,
    ) : TypedAnalyticsUiState
}

private class TypeRecordPagingSource(
    private val typeId: Long,
    private val selection: DateSelectionEntity,
    private val includeChildTypes: Boolean,
    private val getTypeRecordViewsUseCase: GetTypeRecordViewsUseCase,
) : PagingSource<Int, RecordViewsEntity>() {
    override fun getRefreshKey(state: PagingState<Int, RecordViewsEntity>): Int? = null
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, RecordViewsEntity> = runCatching {
        val page = params.key ?: 0
        val pageSize = params.loadSize
        val dateStr = if (selection is DateSelectionEntity.All) "" else selection.getDisplayText()
        val items = getTypeRecordViewsUseCase(typeId, dateStr, page, pageSize, includeChildTypes)
        LoadResult.Page(items, if (page > 0) page - 1 else null, if (items.isNotEmpty()) page + 1 else null)
    }.getOrElse { LoadResult.Error(it) }
}

private class TagRecordPagingSource(
    private val tagId: Long,
    private val selection: DateSelectionEntity,
    private val getTagRecordViewsUseCase: GetTagRecordViewsUseCase,
) : PagingSource<Int, RecordViewsEntity>() {
    override fun getRefreshKey(state: PagingState<Int, RecordViewsEntity>): Int? = null
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, RecordViewsEntity> = runCatching {
        val page = params.key ?: 0
        val pageSize = params.loadSize
        val (start, end) = selection.toDateRange()
        val items = getTagRecordViewsUseCase(tagId, start, end, page, pageSize)
        LoadResult.Page(items, if (page > 0) page - 1 else null, if (items.isNotEmpty()) page + 1 else null)
    }.getOrElse { LoadResult.Error(it) }
}
```

> 删除旧 `DateData` 类（原 :164-187）。`combine` 用 5-arg 类型化重载。

- [ ] **Step 4: 跑测试确认通过**

Run: `./gradlew :feature:records:testDebugUnitTest --tests "*TypedAnalyticsViewModelTest"`
Expected: PASS（含原有 viewRecord/uiState 用例 + 新周期/汇总/transfer 用例）

> 此步 Screen 尚未改，feature:records 整模块编译会因 Screen/ScreenshotTests 旧签名失败属预期，Task 8 修复。若需本 Task 单独绿，可临时只跑该测试类（Gradle 仍需整模块编译——故 Task 7、8 可作为一个连续工作单元，提交各自 commit，但模块级 `testDebugUnitTest` 在 Task 8 末统一转绿）。

- [ ] **Step 5: 提交**

```bash
git add feature/records/src/main/kotlin/cn/wj/android/cashbook/feature/records/viewmodel/TypedAnalyticsViewModel.kt feature/records/src/test/kotlin/cn/wj/android/cashbook/feature/records/viewmodel/TypedAnalyticsViewModelTest.kt
git commit -m "$(printf '[feat|feature|分类标签统计VM][公共]TypedAnalyticsViewModel 周期/汇总/按日分组改造\n\nCo-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>')"
```

---

## Task 8: `TypedAnalyticsScreen` 改造 + 截图（feature:records）

**Files:**
- Modify: `feature/records/src/main/kotlin/cn/wj/android/cashbook/feature/records/screen/TypedAnalyticsScreen.kt`
- Modify: `feature/records/src/test/kotlin/cn/wj/android/cashbook/feature/records/screen/TypedAnalyticsScreenScreenshotTests.kt`
- Baseline: `feature/records/src/test/screenshots/.../TypedAnalyticsScreen*.png`（重录）

**Interfaces:**
- Consumes: `RecordMonthSummaryHeader`/`RecordDayHeader`（T6）、`LauncherListItem`、`RecordListItem`、ViewModel 新 StateFlow（T7）

- [ ] **Step 1: 改 Route + Screen 签名与渲染**

`TypedAnalyticsRoute`：collect 新增 `dateSelection`/`monthSwitchable`/`summary`，传入 Screen + `onPreviousMonth/onNextMonth`：

```kotlin
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val recordList = viewModel.recordList.collectAsLazyPagingItems()
    val dateSelection by viewModel.dateSelection.collectAsStateWithLifecycle()
    val monthSwitchable by viewModel.monthSwitchable.collectAsStateWithLifecycle()
    val summary by viewModel.summary.collectAsStateWithLifecycle()
    val currentMonth = (dateSelection as? DateSelectionEntity.ByMonth)?.yearMonth ?: YearMonth.now()

    TypedAnalyticsScreen(
        viewRecord = viewModel.viewRecord,
        onRequestShowRecordDetailsSheet = viewModel::showRecordDetailsSheet,
        onRequestDismissBottomSheet = viewModel::dismissRecordDetailSheet,
        uiState = uiState,
        recordList = recordList,
        dateSelection = dateSelection,
        monthSwitchable = monthSwitchable,
        summary = summary,
        onPreviousMonth = { viewModel.updateMonth(currentMonth.minusMonths(1)) },
        onNextMonth = { viewModel.updateMonth(currentMonth.plusMonths(1)) },
        onRequestNaviToEditRecord = onRequestNaviToEditRecord,
        onRequestNaviToAssetInfo = onRequestNaviToAssetInfo,
        onRequestPopBackStack = onRequestPopBackStack,
        modifier = modifier,
    )
```

`TypedAnalyticsScreen` 签名加 `recordList: LazyPagingItems<LauncherListItem>`、`dateSelection: DateSelectionEntity`、`monthSwitchable: Boolean`、`summary: AssetMonthSummaryModel`、`onPreviousMonth/onNextMonth: () -> Unit`；标题栏去 `subTitleText`（只留 `uiState.titleText`）；Success 分支的 `LazyColumn` 内容改为：

```kotlin
                    is TypedAnalyticsUiState.Success -> {
                        LazyColumn(
                            content = {
                                item {
                                    RecordMonthSummaryHeader(
                                        periodText = dateSelection.getDisplayText(),
                                        monthSwitchable = monthSwitchable,
                                        summary = summary,
                                        showTransferHint = uiState.isTransferType,
                                        onPreviousMonth = onPreviousMonth,
                                        onNextMonth = onNextMonth,
                                    )
                                }
                                if (recordList.itemCount <= 0) {
                                    item {
                                        Empty(
                                            hintText = stringResource(id = R.string.asset_no_record_data_hint),
                                            modifier = Modifier.fillMaxWidth(),
                                        )
                                    }
                                } else {
                                    items(
                                        count = recordList.itemCount,
                                        key = { index ->
                                            when (val item = recordList.peek(index)) {
                                                is LauncherListItem.DayHeader -> "header_${item.dateStr}"
                                                is LauncherListItem.Record -> "record_${item.entity.id}"
                                                null -> "placeholder_$index"
                                            }
                                        },
                                    ) { index ->
                                        when (val item = recordList[index]) {
                                            is LauncherListItem.DayHeader -> RecordDayHeader(item = item)
                                            is LauncherListItem.Record -> RecordListItem(
                                                item = item.entity,
                                                showDate = false,
                                                modifier = Modifier.clickable(
                                                    onClick = rememberHapticOnClick {
                                                        onRequestShowRecordDetailsSheet(item.entity)
                                                    },
                                                ),
                                            )
                                            null -> Unit
                                        }
                                    }
                                    item { Footer(hintText = stringResource(id = R.string.footer_hint_default)) }
                                }
                            },
                        )
                    }
```

补充 import：`LazyPagingItems`、`DateSelectionEntity`、`YearMonth`、`AssetMonthSummaryModel`、`LauncherListItem`、`RecordListItem`、`RecordMonthSummaryHeader`、`RecordDayHeader`、`clickable`、`rememberHapticOnClick`、`Footer`。

- [ ] **Step 2: 改截图测试构造**

`TypedAnalyticsScreenScreenshotTests.kt`：`successUiState` 去 `subTitleText` 加 `isTransferType = false`；sampleRecords 改为 `LauncherListItem`；每个 `TypedAnalyticsScreen(...)` 调用补新参。示例（success 月份模式）：

```kotlin
    private val successUiState = TypedAnalyticsUiState.Success(
        isType = true,
        titleText = "餐饮",
        isTransferType = false,
    )

    private val sampleItems = listOf<LauncherListItem>(
        LauncherListItem.DayHeader(dateStr = "2024-01-15", day = 15, dayType = 1),
        LauncherListItem.Record(/* 原 sampleRecords[0] 那个 RecordViewsEntity */),
    )

    private val summary = AssetMonthSummaryModel(income = 0L, expenditure = 50_00L, balance = -50_00L)

    // 每处调用：
    TypedAnalyticsScreen(
        viewRecord = null,
        onRequestShowRecordDetailsSheet = {},
        onRequestDismissBottomSheet = {},
        uiState = successUiState,
        recordList = flowOf(PagingData.from(sampleItems)).collectAsLazyPagingItems(),
        dateSelection = DateSelectionEntity.ByMonth(java.time.YearMonth.of(2024, 1)),
        monthSwitchable = true,
        summary = summary,
        onPreviousMonth = {},
        onNextMonth = {},
        onRequestNaviToEditRecord = {},
        onRequestNaviToAssetInfo = {},
        onRequestPopBackStack = {},
    )
```

新增两态用例（固定周期无切换器、转账提示）：

```kotlin
    @Test
    fun typedAnalyticsScreen_fixedPeriod_multipleThemes() {
        composeTestRule.captureMultiTheme(name = "TypedAnalyticsScreen", overrideFileName = "TypedAnalyticsScreen_fixedPeriod") {
            TypedAnalyticsScreen(
                /* 同上，但 */ dateSelection = DateSelectionEntity.ByYear(2024), monthSwitchable = false,
                /* 其余参数同 success */
            )
        }
    }

    @Test
    fun typedAnalyticsScreen_transferHint_multipleThemes() {
        composeTestRule.captureMultiTheme(name = "TypedAnalyticsScreen", overrideFileName = "TypedAnalyticsScreen_transferHint") {
            TypedAnalyticsScreen(
                /* uiState = successUiState.copy(isTransferType = true), monthSwitchable = true，其余同 success */
            )
        }
    }
```

- [ ] **Step 3: 编译整模块 + 跑 ViewModelTest**

Run: `./gradlew :feature:records:testDebugUnitTest --tests "*TypedAnalytics*"`
Expected: 编译通过 + ViewModelTest PASS（截图测试此时无基线，下步录制）

- [ ] **Step 4: 录制基线 + 校验**

```bash
./gradlew :feature:records:recordRoborazziDebug --tests "*TypedAnalyticsScreenScreenshotTests"
git status   # 确认新增 TypedAnalyticsScreen*.png 落在 feature/records/src/test/screenshots/
./gradlew :feature:records:verifyRoborazziDebug --tests "*TypedAnalyticsScreenScreenshotTests"
```
Expected: record 生成新基线（含 loading/success/fixedPeriod/transferHint × multiTheme+multiDevice）；verify 0 diff。人工 Read 抽查 1-2 张 PNG 确认非塌陷/正常渲染。

- [ ] **Step 5: 整模块回归**

Run: `./gradlew :feature:records:testDebugUnitTest :feature:records:verifyRoborazziDebug && ./gradlew spotlessApply --init-script gradle/init.gradle.kts --no-configuration-cache`
Expected: 全绿 + 格式化无遗留

- [ ] **Step 6: 提交**

```bash
git add feature/records/src/main/kotlin/cn/wj/android/cashbook/feature/records/screen/TypedAnalyticsScreen.kt feature/records/src/test/kotlin/cn/wj/android/cashbook/feature/records/screen/TypedAnalyticsScreenScreenshotTests.kt feature/records/src/test/screenshots
git commit -m "$(printf '[feat|feature|分类标签统计屏][公共]TypedAnalyticsScreen 月份切换+汇总卡+按日分组+转账提示+截图基线\n\nCo-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>')"
```

---

## 完整链路验证（全部 Task 后）

- [ ] 全量模块测试：`./gradlew :core:model:test :core:data:testDebugUnitTest :core:domain:testDebugUnitTest :feature:records:testDebugUnitTest :feature:records:verifyRoborazziDebug`
- [ ] Spotless：`./gradlew spotlessCheck --init-script gradle/init.gradle.kts --no-configuration-cache`
- [ ] 设备就绪时：`./gradlew :core:database:connectedDebugAndroidTest`（Task 2 标签区间 DAO androidTest）
- [ ] 模拟器黑盒（可选）：我的分类→某支出分类→统计数据，验月份切换/汇总卡/按日分组；转账 tab→某转账类型→验提示文案；我的标签→某标签→验当月默认 + 切换。

---

## Self-Review（plan 对 spec 覆盖）

- spec §3 周期模式复用 DateSelectionEntity → T1（解析）+ T7（状态机/monthSwitchable）✓
- spec §4.1 标签区间 DAO → T2 ✓
- spec §4.2 标签区间分页 + 汇总全量查询（独立非分页）→ T3 ✓
- spec §4.3 日期解析去重（删 DataData，统一 toDateRange）→ T7（删 DateData）✓
- spec §4.4 GetTypedMonthSummaryUseCase（净自付/TRANSFER continue/平账省略兜底/口径区别）→ T5 ✓
- spec §5 ViewModel/Screen（dateSelection/summary/recordList LauncherListItem/isTransferType/去 subTitleText）→ T7+T8 ✓
- spec §6 共享组件独立 commit + 0 diff + 回调守护 → T6 ✓
- spec §7 测试（UseCase/解析/VM/截图三态/DAO androidTest/Fake 忠实桩）→ T1-T8 各 Step ✓
- spec §8 影响面（GetTagRecordViewsUseCase 调用处仅 PagingSource；强制必改测试清单）→ T4+T7+T8 ✓
- 转账显提示文案（节点1 决策）→ T6（组件 transfer 模式）+ T8（isTransferType 接线）✓

**类型一致性核对**：`GetTagRecordViewsUseCase.invoke(tagId,startDate,endDate,pageNum,pageSize)`（T4）= ViewModel `TagRecordPagingSource` 调用（T7）✓；`GetTypedMonthSummaryUseCase.invoke(isType,id,startDate,endDate,includeChildTypes)`（T5）= ViewModel summary（T7）✓；`RecordMonthSummaryHeader(periodText,monthSwitchable,summary,showTransferHint,onPreviousMonth,onNextMonth)`（T6）= 资产页（T6）+ TypedAnalyticsScreen（T8）调用 ✓；`queryRecordsByType/TagIdInRange`（T3）= UseCase 消费（T5）✓。
