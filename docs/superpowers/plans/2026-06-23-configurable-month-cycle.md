# 全应用可配置月周期（月起始日）实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 新增可配置「月起始日」D（1–28，默认 1=自然月），使首页/数据分析/分类·标签统计/资产详情的「本月」统计按 `[本月D日, 次月D日)` 周期计算；日历保持自然月。

**Architecture:** 统一所有月区间经 `DateSelectionEntity.toDateRange(monthStartDay)` 纯函数计算；新增 `currentMonthPeriod(today, D)` 推导当前周期；D 存于 Proto DataStore（`record_settings`），经 `SettingRepository` 响应式注入各 ViewModel。消除分类列表的字符串自然月解析器、修复柱状图按自然月建桶，使 D≠1 时全应用一致。

**Tech Stack:** Kotlin, Jetpack Compose, Hilt, Proto DataStore, Room, Coroutines/Flow, JUnit + Truth, Roborazzi。

**对应 spec:** `docs/superpowers/specs/2026-06-23-configurable-month-cycle-design.md`

## Global Constraints

- 月起始日 D 取值 **1–28**，默认 **1**（=自然月）；proto3 缺省 0 / 越界 → 归一化为 1。
- **不变量**：D=1 时全应用产出与现状**逐字节等价**（每个改造点需金丝雀测试守护）。
- 口径不变：净自付（`finalAmount` / `analyticsPieNetAmount`），本计划**只改月边界，不碰金额计算**。
- 日历屏（`CalendarViewModel` / `queryRecordByYearMonth`）**保持自然月，不改**。
- **无 Room schema 变更**，不需要 DB migration（monthStartDay 在 Proto DataStore）。
- 改 ViewModel 构造签名必须**同步改对应 `*ViewModelTest`**，否则模块 `testDebugUnitTest` 整体编译失败（项目强约定）。
- UI 用 `core/design` 的 `Cb*` 封装（禁止裸 Material3）；源文件含 Apache 2.0 License Header；提交前 `spotlessApply`。
- 测试任务名：JVM 库 `core:model` → `:core:model:test`；Android 库 `core:data`/`core:domain` → `:<m>:testDebugUnitTest`；feature → `:feature:<m>:testDebugUnitTest`。
- 提交信息格式 `[类型|模块|功能][公共]说明` + `Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>`。

---

## 文件结构总览

| 文件 | 责任 | 任务 |
|---|---|---|
| `core/model/.../entity/DateSelectionEntity.kt` | `toDateRange(monthStartDay)` + `currentMonthPeriod` + `normalizeMonthStartDay` 纯函数 | T1 |
| `core/datastore-proto/.../proto/record_settings.proto` | 新增 `monthStartDay=8` | T2 |
| `core/model/.../model/RecordSettingsModel.kt` | 加 `monthStartDay` 字段 | T2 |
| `core/datastore/.../CombineProtoDataSource.kt` | 映射 clamp + `updateMonthStartDay` | T2 |
| `core/data/.../repository/SettingRepository.kt` (+Impl) | `updateMonthStartDay` | T2 |
| `core/testing/.../FakeSettingRepository.kt`、`core/data/test/.../FakeCombineProtoDataSource.kt` | 构造点补字段 | T2 |
| `core/domain/.../TransRecordViewsToAnalyticsBarUseCase.kt` | C2 周期建桶 + D 参 | T3 |
| `core/data/.../repository/RecordRepository.kt`(+Impl)、`core/domain/.../GetTypeRecordViewsUseCase.kt`、`core/testing/.../FakeRecordRepository.kt` | C1 类型列表毫秒区间 | T4 |
| `feature/records/.../viewmodel/LauncherContentViewModel.kt` | 首页走周期 | T5 |
| `feature/records/.../viewmodel/AnalyticsViewModel.kt` | 分析走周期 + bar D | T6 |
| `feature/records/.../viewmodel/TypedAnalyticsViewModel.kt` | 分类·标签走周期（含 C1 列表） | T7 |
| `feature/records/.../viewmodel/AssetInfoContentViewModel.kt` | 资产详情走周期 | T8 |
| `feature/settings/.../{enums/SettingDialogEnum,viewmodel/SettingViewModel,screen/SettingScreen}.kt` | 月起始日设置项 + 对话框 | T9 |

> 任务顺序：T1→T2 是基础，T3/T4 是 C2/C1 修复（依赖 T1 的 `toDateRange`），T5–T8 是 VM 接线（依赖 T1–T4），T9 是设置入口（依赖 T2）。

---

### Task 1: 月区间纯函数（toDateRange(D) + currentMonthPeriod + normalizeMonthStartDay）

**Files:**
- Modify: `core/model/src/main/kotlin/cn/wj/android/cashbook/core/model/entity/DateSelectionEntity.kt`
- Test: `core/model/src/test/kotlin/cn/wj/android/cashbook/core/model/entity/DateSelectionEntityMonthCycleTest.kt`（新建）

**Interfaces:**
- Produces:
  - `fun normalizeMonthStartDay(raw: Int): Int`（top-level，`core.model.entity` 包）— `raw in 1..28 ? raw : 1`
  - `DateSelectionEntity.toDateRange(monthStartDay: Int = 1): Pair<Long, Long>`（既有方法**加默认参**，仅 `ByMonth` 分支受 D 影响）
  - `DateSelectionEntity.Companion.currentMonthPeriod(today: LocalDate, monthStartDay: Int): DateSelectionEntity.ByMonth`

- [ ] **Step 1: 写失败测试**

新建 `core/model/src/test/kotlin/cn/wj/android/cashbook/core/model/entity/DateSelectionEntityMonthCycleTest.kt`（含 Apache License Header）：

```kotlin
package cn.wj.android.cashbook.core.model.entity

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

class DateSelectionEntityMonthCycleTest {

    private fun ms(y: Int, m: Int, d: Int): Long =
        LocalDate.of(y, m, d).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    @Test
    fun normalizeMonthStartDay_clampsOutOfRangeToOne() {
        assertThat(normalizeMonthStartDay(0)).isEqualTo(1)
        assertThat(normalizeMonthStartDay(-5)).isEqualTo(1)
        assertThat(normalizeMonthStartDay(29)).isEqualTo(1)
        assertThat(normalizeMonthStartDay(31)).isEqualTo(1)
        assertThat(normalizeMonthStartDay(999)).isEqualTo(1)
        assertThat(normalizeMonthStartDay(1)).isEqualTo(1)
        assertThat(normalizeMonthStartDay(15)).isEqualTo(15)
        assertThat(normalizeMonthStartDay(28)).isEqualTo(28)
    }

    @Test
    fun toDateRange_d1_isByteEquivalentToLegacy_allBranches() {
        val cases = listOf(
            DateSelectionEntity.ByDay(LocalDate.of(2024, 1, 15)),
            DateSelectionEntity.ByMonth(YearMonth.of(2024, 1)),
            DateSelectionEntity.ByYear(2024),
            DateSelectionEntity.DateRange(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31)),
            DateSelectionEntity.All,
        )
        for (sel in cases) {
            assertThat(sel.toDateRange(1)).isEqualTo(sel.toDateRange())
        }
    }

    @Test
    fun toDateRange_byMonth_d15_spansAcrossMonths() {
        val range = DateSelectionEntity.ByMonth(YearMonth.of(2024, 1)).toDateRange(15)
        assertThat(range.first).isEqualTo(ms(2024, 1, 15))
        assertThat(range.second).isEqualTo(ms(2024, 2, 15))
    }

    @Test
    fun toDateRange_byMonth_d28_decemberCrossesYear() {
        val range = DateSelectionEntity.ByMonth(YearMonth.of(2024, 12)).toDateRange(28)
        assertThat(range.first).isEqualTo(ms(2024, 12, 28))
        assertThat(range.second).isEqualTo(ms(2025, 1, 28))
    }

    @Test
    fun toDateRange_byMonth_illegalDIsNormalized() {
        // D=0/29 归一化为 1，等价自然月
        assertThat(DateSelectionEntity.ByMonth(YearMonth.of(2024, 1)).toDateRange(0))
            .isEqualTo(DateSelectionEntity.ByMonth(YearMonth.of(2024, 1)).toDateRange(1))
        assertThat(DateSelectionEntity.ByMonth(YearMonth.of(2024, 2)).toDateRange(29))
            .isEqualTo(DateSelectionEntity.ByMonth(YearMonth.of(2024, 2)).toDateRange(1))
    }

    @Test
    fun toDateRange_nonByMonthBranchesIgnoreD() {
        val day = DateSelectionEntity.ByDay(LocalDate.of(2024, 1, 15))
        assertThat(day.toDateRange(15)).isEqualTo(day.toDateRange(1))
        assertThat(DateSelectionEntity.All.toDateRange(15)).isEqualTo(0L to Long.MAX_VALUE)
    }

    @Test
    fun currentMonthPeriod_dayGteD_usesThisMonth() {
        val p = DateSelectionEntity.currentMonthPeriod(LocalDate.of(2024, 3, 20), 15)
        assertThat(p.yearMonth).isEqualTo(YearMonth.of(2024, 3))
    }

    @Test
    fun currentMonthPeriod_dayLtD_usesPreviousMonth() {
        val p = DateSelectionEntity.currentMonthPeriod(LocalDate.of(2024, 3, 5), 15)
        assertThat(p.yearMonth).isEqualTo(YearMonth.of(2024, 2))
    }

    @Test
    fun currentMonthPeriod_january_dayLtD_crossesToPreviousYear() {
        val p = DateSelectionEntity.currentMonthPeriod(LocalDate.of(2024, 1, 5), 15)
        assertThat(p.yearMonth).isEqualTo(YearMonth.of(2023, 12))
    }

    @Test
    fun currentMonthPeriod_d1_isAlwaysThisMonth() {
        val p = DateSelectionEntity.currentMonthPeriod(LocalDate.of(2024, 3, 1), 1)
        assertThat(p.yearMonth).isEqualTo(YearMonth.of(2024, 3))
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `./gradlew :core:model:test --tests "*DateSelectionEntityMonthCycleTest*"`
Expected: 编译失败 / FAIL —— `normalizeMonthStartDay` 未定义、`toDateRange(Int)` 无此重载、`currentMonthPeriod` 未定义。

- [ ] **Step 3: 实现纯函数**

在 `DateSelectionEntity.kt` 中：①把 `ByMonth` 分支改为用 D；②给 `toDateRange` 加默认参；③companion 加 `currentMonthPeriod`；④文件末尾加 top-level `normalizeMonthStartDay`。

将 `toDateRange()` 签名与 `ByMonth` 分支改为：

```kotlin
    /**
     * 将日期选择转换为时间戳范围（毫秒），使用半开区间 [start, end)。
     * [monthStartDay] 仅影响 [ByMonth]：周期为 [yearMonth.atDay(D), yearMonth.plusMonths(1).atDay(D))；
     * D=1（默认）时与自然月一致。非法 D 经 [normalizeMonthStartDay] 归一化为 1。
     */
    fun toDateRange(monthStartDay: Int = 1): Pair<Long, Long> {
        val zone = ZoneId.systemDefault()
        return when (this) {
            is ByDay -> {
                val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
                val end = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
                start to end
            }

            is ByMonth -> {
                val d = normalizeMonthStartDay(monthStartDay)
                val start = yearMonth.atDay(d).atStartOfDay(zone).toInstant().toEpochMilli()
                val end = yearMonth.plusMonths(1).atDay(d).atStartOfDay(zone).toInstant().toEpochMilli()
                start to end
            }

            is ByYear -> {
                val start = LocalDate.of(year, 1, 1).atStartOfDay(zone).toInstant().toEpochMilli()
                val end = LocalDate.of(year + 1, 1, 1).atStartOfDay(zone).toInstant().toEpochMilli()
                start to end
            }

            is DateRange -> {
                val start = from.atStartOfDay(zone).toInstant().toEpochMilli()
                val end = to.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
                start to end
            }

            is All -> {
                0L to Long.MAX_VALUE
            }
        }
    }
```

在 `companion object` 内（`fromDisplayTextOrNull` 之后）追加：

```kotlin
        /**
         * 推导包含 [today] 的当前月周期（以 [monthStartDay] 为每月起点）。
         * today.dayOfMonth >= D → 本月 label；否则 → 上月 label（跨年由 YearMonth.minusMonths 处理）。
         */
        fun currentMonthPeriod(today: LocalDate, monthStartDay: Int): ByMonth {
            val d = normalizeMonthStartDay(monthStartDay)
            val ym = YearMonth.from(today)
            return if (today.dayOfMonth >= d) ByMonth(ym) else ByMonth(ym.minusMonths(1))
        }
```

在文件末尾（class 之外，同包 top-level）追加：

```kotlin
/** 归一化月起始日：合法范围 1..28，否则回落为 1（自然月）。 */
fun normalizeMonthStartDay(raw: Int): Int = if (raw in 1..28) raw else 1
```

- [ ] **Step 4: 运行测试确认通过**

Run: `./gradlew :core:model:test --tests "*DateSelectionEntityMonthCycleTest*"`
Expected: PASS（全部用例）。

- [ ] **Step 5: spotless + 提交**

```bash
./gradlew :core:model:spotlessApply --init-script gradle/init.gradle.kts --no-configuration-cache
git add core/model/src/main/kotlin/cn/wj/android/cashbook/core/model/entity/DateSelectionEntity.kt core/model/src/test/kotlin/cn/wj/android/cashbook/core/model/entity/DateSelectionEntityMonthCycleTest.kt
git commit -m "[feat|core|月周期][公共]DateSelectionEntity.toDateRange(monthStartDay)+currentMonthPeriod+normalizeMonthStartDay 纯函数

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

### Task 2: 月起始日存储链路（proto + model + datastore + SettingRepository + Fakes）

**Files:**
- Modify: `core/datastore-proto/src/main/proto/record_settings.proto`
- Modify: `core/model/src/main/kotlin/cn/wj/android/cashbook/core/model/model/RecordSettingsModel.kt`
- Modify: `core/datastore/src/main/kotlin/cn/wj/android/cashbook/core/datastore/datasource/CombineProtoDataSource.kt`
- Modify: `core/data/src/main/kotlin/cn/wj/android/cashbook/core/data/repository/SettingRepository.kt`
- Modify: `core/data/src/main/kotlin/cn/wj/android/cashbook/core/data/repository/impl/SettingRepositoryImpl.kt`
- Modify: `core/testing/src/main/kotlin/cn/wj/android/cashbook/core/testing/repository/FakeSettingRepository.kt`
- Modify: `core/data/src/test/.../FakeCombineProtoDataSource.kt`（若存在；先 grep 确认）

**Interfaces:**
- Consumes: `normalizeMonthStartDay`（T1）
- Produces:
  - `RecordSettingsModel.monthStartDay: Int`（默认 `1`）
  - `SettingRepository.updateMonthStartDay(monthStartDay: Int)`
  - `recordSettingsModel` 流的 `monthStartDay` 已 clamp 到 1..28

- [ ] **Step 1: proto 加字段**

`record_settings.proto` 在 `bool topUpInTotal = 7;` 后加：

```proto
  int32 monthStartDay = 8; // 月起始日（1-28，默认 0 视为 1=自然月）
```

- [ ] **Step 2: RecordSettingsModel 加字段（带默认值降低构造点破坏）**

`RecordSettingsModel.kt` 在 `topUpInTotal` 后加（注意：原字段无默认值，新字段给默认 `= 1`）：

```kotlin
    /** 充值账号计入总资产 */
    val topUpInTotal: Boolean,
    /** 月起始日（1-28，1=自然月） */
    val monthStartDay: Int = 1,
```

- [ ] **Step 3: 写失败测试（归一化映射 + Fake 默认）**

> 说明：`CombineProtoDataSource` 映射依赖 Android DataStore，不做 JVM 单测；clamp 逻辑由 T1 `normalizeMonthStartDay` 单测覆盖。本步加一个 `core:data` 的 `FakeSettingRepository` 回归测试，确认 `RecordSettingsModel` 默认 `monthStartDay=1` 且可被 `setRecordSettings` 覆盖。

新建 `core/data/src/test/kotlin/cn/wj/android/cashbook/core/data/RecordSettingsModelMonthStartDayTest.kt`（含 License Header）：

```kotlin
package cn.wj.android.cashbook.core.data

import cn.wj.android.cashbook.core.model.model.RecordSettingsModel
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class RecordSettingsModelMonthStartDayTest {

    @Test
    fun defaultMonthStartDayIsOne() {
        val model = RecordSettingsModel(
            currentBookId = 1L,
            defaultTypeId = 1L,
            lastAssetId = -1L,
            refundTypeId = -1L,
            reimburseTypeId = -1L,
            creditCardPaymentTypeId = -1L,
            topUpInTotal = false,
        )
        assertThat(model.monthStartDay).isEqualTo(1)
    }

    @Test
    fun monthStartDayCanBeSet() {
        val model = RecordSettingsModel(
            currentBookId = 1L,
            defaultTypeId = 1L,
            lastAssetId = -1L,
            refundTypeId = -1L,
            reimburseTypeId = -1L,
            creditCardPaymentTypeId = -1L,
            topUpInTotal = false,
            monthStartDay = 15,
        )
        assertThat(model.monthStartDay).isEqualTo(15)
    }
}
```

- [ ] **Step 4: 运行确认失败**

Run: `./gradlew :core:data:testDebugUnitTest --tests "*RecordSettingsModelMonthStartDayTest*"`
Expected: 编译通过则测试 PASS（因默认值已加）——若 Step 2 未加默认值则编译失败。本步主要确认字段存在；如已绿，直接进入 Step 5 接线（接线无独立单测，靠编译 + 既有测试守护）。

- [ ] **Step 5: 接线 datastore 映射 + update**

`CombineProtoDataSource.kt`：①`recordSettingsData` map（`:101-109` 区域）在 `topUpInTotal = it.topUpInTotal,` 后加（需 import `normalizeMonthStartDay`）：

```kotlin
                topUpInTotal = it.topUpInTotal,
                monthStartDay = normalizeMonthStartDay(it.monthStartDay),
```

文件顶部 import 区加：

```kotlin
import cn.wj.android.cashbook.core.model.entity.normalizeMonthStartDay
```

②在 `updateTopUpInTotal`（`:372-374`）后加：

```kotlin
    suspend fun updateMonthStartDay(monthStartDay: Int) {
        recordSettings.updateData { it.copy { this.monthStartDay = normalizeMonthStartDay(monthStartDay) } }
    }
```

> 注：`splitAppPreferences`（`:178-188`）**不加** monthStartDay——老 AppPreferences 无此字段，新表默认 0 经映射归一化为 1，行为正确。

- [ ] **Step 6: 接线 SettingRepository（接口 + 实现）**

`SettingRepository.kt` 接口末尾（`updateLogcatInRelease` 后）加：

```kotlin
    /** 更新月起始日（1-28） */
    suspend fun updateMonthStartDay(monthStartDay: Int)
```

`SettingRepositoryImpl.kt` 末尾（`updateLogcatInRelease` 实现后）加：

```kotlin
    override suspend fun updateMonthStartDay(monthStartDay: Int) =
        withContext(coroutineContext) {
            combineProtoDataSource.updateMonthStartDay(monthStartDay)
        }
```

- [ ] **Step 7: 补 Fake 构造点（防编译失败）**

`FakeSettingRepository.kt`：①`_recordSettingsModel` 的 `RecordSettingsModel(...)` 构造（`:65-` 区域）因 `monthStartDay` 有默认值无需必改，但建议显式加 `monthStartDay = 1,` 以便测试可改；②实现接口新方法：

```kotlin
    override suspend fun updateMonthStartDay(monthStartDay: Int) {
        _recordSettingsModel.value = _recordSettingsModel.value.copy(monthStartDay = monthStartDay)
    }
```

先 grep 确认 `core/data/src/test` 是否有 `FakeCombineProtoDataSource` 或其它 `RecordSettingsModel(` / `SettingRepository` 实现需补：

```bash
grep -rn "RecordSettingsModel(" core/ --include=*.kt
grep -rln ": SettingRepository" core/ --include=*.kt
```

对每个**显式列出全部 7 个旧参且未给 monthStartDay** 的构造点——因新字段有默认值，编译不会失败，无需强改；对每个 `SettingRepository` 的**其它实现类/匿名实现**，必须补 `updateMonthStartDay` override（否则编译失败）。

- [ ] **Step 8: 编译 + 跑相关模块测试**

Run:
```bash
./gradlew :core:datastore:compileDebugKotlin :core:data:testDebugUnitTest :core:testing:compileDebugKotlin
```
Expected: BUILD SUCCESSFUL；`RecordSettingsModelMonthStartDayTest` PASS；既有测试不回归。

- [ ] **Step 9: spotless + 提交**

```bash
./gradlew :core:data:spotlessApply :core:datastore:spotlessApply :core:model:spotlessApply :core:testing:spotlessApply --init-script gradle/init.gradle.kts --no-configuration-cache
git add core/datastore-proto core/model core/datastore core/data core/testing
git commit -m "[feat|core|月周期][公共]record_settings 加 monthStartDay(1-28,clamp)+SettingRepository.updateMonthStartDay 全链路

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

### Task 3: C2 — 柱状图按真实周期建桶（TransRecordViewsToAnalyticsBarUseCase）

**Files:**
- Modify: `core/domain/src/main/kotlin/cn/wj/android/cashbook/domain/usecase/TransRecordViewsToAnalyticsBarUseCase.kt`
- Test: `core/domain/src/test/.../TransRecordViewsToAnalyticsBarUseCaseTest.kt`（新建或追加）

**Interfaces:**
- Consumes: `normalizeMonthStartDay`（T1）
- Produces: `TransRecordViewsToAnalyticsBarUseCase.invoke(dateSelection, recordViewsList, monthStartDay: Int = 1)` —— `ByMonth` 分支日桶覆盖 `[ym.atDay(D), ym.plusMonths(1).atDay(D))`，含跨月「次月 1..D-1」。

- [ ] **Step 1: 写失败测试**

新建 `core/domain/src/test/kotlin/cn/wj/android/cashbook/domain/usecase/TransRecordViewsToAnalyticsBarUseCaseTest.kt`（License Header）。构造一条落在「次月 1..D-1」的 EXPENDITURE 记录，断言 D≠1 时它被计入对应日桶、且 Σ桶支出 == 该记录额；并断言 D=1 时桶为自然月（31 个日桶，2024-01）。

```kotlin
package cn.wj.android.cashbook.domain.usecase

import cn.wj.android.cashbook.core.model.entity.DateSelectionEntity
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.model.enums.TypeLevelEnum
import cn.wj.android.cashbook.core.model.model.RecordTypeModel
import cn.wj.android.cashbook.core.model.model.RecordViewsModel
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import kotlin.coroutines.EmptyCoroutineContext

class TransRecordViewsToAnalyticsBarUseCaseTest {

    private val useCase = TransRecordViewsToAnalyticsBarUseCase(EmptyCoroutineContext)

    private fun ms(y: Int, m: Int, d: Int): Long =
        LocalDate.of(y, m, d).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private fun expenditureRecord(timeMs: Long, finalAmount: Long): RecordViewsModel =
        RecordViewsModel(
            id = 1L, typeId = 100L, assetId = -1L, intoAssetId = -1L, booksId = 1L,
            amount = finalAmount, finalAmount = finalAmount, concessions = 0L, charges = 0L,
            remark = "", reimbursable = false, recordTime = timeMs,
            type = RecordTypeModel(
                id = 100L, parentId = -1L, name = "餐饮", iconName = "",
                typeLevel = TypeLevelEnum.FIRST, typeCategory = RecordTypeCategoryEnum.EXPENDITURE,
                protected = false, sort = 0, needRelated = false,
            ),
            asset = null, relatedAsset = null, relatedTags = emptyList(),
        )

    @Test
    fun byMonth_d15_includesNextMonthRecordsInBuckets() = runTest {
        // 周期 [2024-01-15, 2024-02-15)，记录落在 2024-02-03（自然月 1 月看不到、但属本周期）
        val record = expenditureRecord(ms(2024, 2, 3), 5000L)
        val bars = useCase(DateSelectionEntity.ByMonth(YearMonth.of(2024, 1)), listOf(record), monthStartDay = 15)
        // 应存在 2024-02-03 日桶且其支出=5000；Σ桶支出==5000（不丢记录）
        assertThat(bars.any { it.date == "2024-02-03" }).isTrue()
        assertThat(bars.sumOf { it.expenditure }).isEqualTo(5000L)
    }

    @Test
    fun byMonth_d1_isNaturalMonth() = runTest {
        val bars = useCase(DateSelectionEntity.ByMonth(YearMonth.of(2024, 1)), emptyList(), monthStartDay = 1)
        assertThat(bars.first().date).isEqualTo("2024-01-01")
        assertThat(bars.last().date).isEqualTo("2024-01-31")
        assertThat(bars.size).isEqualTo(31)
    }
}
```

> 实施时先核对 `RecordViewsModel` 构造参数名/顺序（读 `core/model/.../model/RecordViewsModel.kt`），按实际签名调整测试构造。

- [ ] **Step 2: 运行确认失败**

Run: `./gradlew :core:domain:testDebugUnitTest --tests "*TransRecordViewsToAnalyticsBarUseCaseTest*"`
Expected: FAIL —— `invoke` 无 `monthStartDay` 参 / D=15 时次月记录无桶被丢、`bars.sumOf=0`。

- [ ] **Step 3: 实现周期建桶**

`TransRecordViewsToAnalyticsBarUseCase.kt`：①`invoke` 加参 `monthStartDay: Int = 1`；②`ByMonth` 分支（`:69-76`）改为按 D 周期逐日建桶：

```kotlin
            is DateSelectionEntity.ByMonth -> {
                granularity = AnalyticsBarGranularity.DAY
                val d = normalizeMonthStartDay(monthStartDay)
                val ym = dateSelection.yearMonth
                var date = ym.atDay(d)
                val endExclusive = ym.plusMonths(1).atDay(d)
                while (date.isBefore(endExclusive)) {
                    dateList.add("${date.year}-${date.monthValue.completeZero()}-${date.dayOfMonth.completeZero()}")
                    date = date.plusDays(1L)
                }
            }
```

`invoke` 签名改为：

```kotlin
    suspend operator fun invoke(
        dateSelection: DateSelectionEntity,
        recordViewsList: List<RecordViewsModel>,
        monthStartDay: Int = 1,
    ): List<AnalyticsRecordBarEntity> = withContext(coroutineContext) {
```

顶部 import 加：

```kotlin
import cn.wj.android.cashbook.core.model.entity.normalizeMonthStartDay
import java.time.LocalDate
```

（其余分支不变；`completeZero` 已 import。）

- [ ] **Step 4: 运行确认通过**

Run: `./gradlew :core:domain:testDebugUnitTest --tests "*TransRecordViewsToAnalyticsBarUseCaseTest*"`
Expected: PASS。

- [ ] **Step 5: spotless + 提交**

```bash
./gradlew :core:domain:spotlessApply --init-script gradle/init.gradle.kts --no-configuration-cache
git add core/domain/src/main/kotlin/cn/wj/android/cashbook/domain/usecase/TransRecordViewsToAnalyticsBarUseCase.kt core/domain/src/test
git commit -m "[fix|core|月周期][公共]C2 柱状图 ByMonth 按真实周期[atDay(D),次月atDay(D))建桶，消跨月记录丢失

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

### Task 4: C1 — 分类列表毫秒区间（消除字符串自然月解析器）

**Files:**
- Modify: `core/data/src/main/kotlin/cn/wj/android/cashbook/core/data/repository/RecordRepository.kt`
- Modify: `core/data/src/main/kotlin/cn/wj/android/cashbook/core/data/repository/impl/RecordRepositoryImpl.kt`
- Modify: `core/domain/src/main/kotlin/cn/wj/android/cashbook/domain/usecase/GetTypeRecordViewsUseCase.kt`
- Modify: `core/testing/src/main/kotlin/cn/wj/android/cashbook/core/testing/repository/FakeRecordRepository.kt`
- Test: `core/domain/src/test/.../GetTypeRecordViewsUseCaseTest.kt`（若存在则改，否则新建）

**Interfaces:**
- Produces:
  - `RecordRepository.queryPagingRecordListByTypeIdInRange(typeId: Long, startDate: Long, endDate: Long, page: Int, pageSize: Int, includeChildTypes: Boolean): List<RecordModel>`
  - `GetTypeRecordViewsUseCase.invoke(typeId: Long, startDate: Long, endDate: Long, pageNum: Int, pageSize: Int, includeChildTypes: Boolean = true): List<RecordViewsEntity>`（**签名由 `dateRange: String` 改为毫秒区间**）

- [ ] **Step 1: 写失败测试（GetTypeRecordViewsUseCase 毫秒区间 + 忠实桩）**

读现有 `GetTypeRecordViewsUseCaseTest`（若有）了解构造方式；新增/改写用例：构造跨周期记录（如 2024-02-03），用 `[ms(2024,1,15), ms(2024,2,15))` 区间查询，断言命中；用 `[ms(2024,1,1), ms(2024,2,1))` 自然月断言不命中。`FakeRecordRepository.queryPagingRecordListByTypeIdInRange` 须忠实复刻半开区间过滤（仿 `queryRecordsByTypeIdInRange` `:204-212`）：

```kotlin
@Test
fun typeRecords_areFilteredByHalfOpenMillisRange() = runTest {
    val repo = FakeRecordRepository().apply {
        setRecords(listOf(/* typeId=100, recordTime=ms(2024,2,3) 的一条 */))
    }
    val useCase = GetTypeRecordViewsUseCase(repo, recordModelTransToViewsUseCase, EmptyCoroutineContext)
    val inPeriod = useCase(100L, ms(2024, 1, 15), ms(2024, 2, 15), 0, 20)
    val inNatural = useCase(100L, ms(2024, 1, 1), ms(2024, 2, 1), 0, 20)
    assertThat(inPeriod).isNotEmpty()
    assertThat(inNatural).isEmpty()
}
```

> 按现有 `GetTypeRecordViewsUseCaseTest` 的实际辅助方法/构造适配（读取后对齐）。

- [ ] **Step 2: 运行确认失败**

Run: `./gradlew :core:domain:testDebugUnitTest --tests "*GetTypeRecordViewsUseCaseTest*"`
Expected: FAIL —— `invoke` 仍是 `dateRange: String` / `queryPagingRecordListByTypeIdInRange` 未定义。

- [ ] **Step 3: 加 Repository 毫秒区间方法**

`RecordRepository.kt` 在 `queryPagingRecordListByTypeIdBetweenDate` 声明后加：

```kotlin
    /** 类型 [typeId]（按 [includeChildTypes]）在 [[startDate],[endDate]) 半开区间的第 [page] 页 [pageSize] 条（分页，按时间倒序） */
    suspend fun queryPagingRecordListByTypeIdInRange(
        typeId: Long,
        startDate: Long,
        endDate: Long,
        page: Int,
        pageSize: Int,
        includeChildTypes: Boolean,
    ): List<RecordModel>
```

`RecordRepositoryImpl.kt` 加实现（复用既有毫秒 DAO 方法）：

```kotlin
    override suspend fun queryPagingRecordListByTypeIdInRange(
        typeId: Long,
        startDate: Long,
        endDate: Long,
        page: Int,
        pageSize: Int,
        includeChildTypes: Boolean,
    ): List<RecordModel> = withContext(coroutineContext) {
        val booksId = combineProtoDataSource.recordSettingsData.first().currentBookId
        if (includeChildTypes) {
            recordDao.queryRecordByTypeIdBetween(booksId, typeId, startDate, endDate, page * pageSize, pageSize)
        } else {
            recordDao.queryRecordByTypeIdExactBetween(booksId, typeId, startDate, endDate, page * pageSize, pageSize)
        }.map { it.asModel() }
    }
```

- [ ] **Step 4: 改 GetTypeRecordViewsUseCase 为毫秒区间**

`GetTypeRecordViewsUseCase.kt` 的 `invoke` 改为：

```kotlin
    suspend operator fun invoke(
        typeId: Long,
        startDate: Long,
        endDate: Long,
        pageNum: Int,
        pageSize: Int,
        includeChildTypes: Boolean = true,
    ): List<RecordViewsEntity> = withContext(coroutineContext) {
        if (typeId == -1L) {
            return@withContext emptyList()
        }
        val records = recordRepository.queryPagingRecordListByTypeIdInRange(
            typeId = typeId,
            startDate = startDate,
            endDate = endDate,
            page = pageNum,
            pageSize = pageSize,
            includeChildTypes = includeChildTypes,
        ).sortedByDescending { it.recordTime }
        recordModelTransToViewsUseCase(records).map { it.asEntity() }
    }
```

> `All` 态由调用方（T7）传 `(0L, Long.MAX_VALUE)`，`queryRecordByTypeIdBetween` 用 `record_time>=0 AND <MAX` 返回全部，无需单独全量分支。

- [ ] **Step 5: 补 FakeRecordRepository 忠实桩**

`FakeRecordRepository.kt` 加（忠实复刻半开区间，仿 `queryRecordsByTypeIdInRange` `:204-212`）：

```kotlin
    override suspend fun queryPagingRecordListByTypeIdInRange(
        typeId: Long,
        startDate: Long,
        endDate: Long,
        page: Int,
        pageSize: Int,
        includeChildTypes: Boolean,
    ): List<RecordModel> {
        return records.filter { it.typeId == typeId && it.recordTime >= startDate && it.recordTime < endDate }
            .sortedByDescending { it.recordTime }
            .drop(page * pageSize)
            .take(pageSize)
    }
```

- [ ] **Step 6: 清理无用的字符串解析器（确认无其它调用方后删除）**

```bash
grep -rn "queryPagingRecordListByTypeIdBetweenDate" --include=*.kt
```
若仅 `RecordRepository`/`RecordRepositoryImpl`/`FakeRecordRepository` 三处声明/实现（无生产调用方），删除：`RecordRepository.kt` 的声明、`RecordRepositoryImpl.kt` 的实现（含 `:148-201` 字符串解析）、`FakeRecordRepository.kt` 的 override。若发现其它调用方，**保留**该方法，仅本计划不再新增其调用。

- [ ] **Step 7: 运行确认通过**

Run:
```bash
./gradlew :core:domain:testDebugUnitTest --tests "*GetTypeRecordViewsUseCaseTest*" :core:data:testDebugUnitTest :core:testing:compileDebugKotlin
```
Expected: PASS / BUILD SUCCESSFUL。

- [ ] **Step 8: spotless + 提交**

```bash
./gradlew :core:data:spotlessApply :core:domain:spotlessApply :core:testing:spotlessApply --init-script gradle/init.gradle.kts --no-configuration-cache
git add core/data core/domain core/testing
git commit -m "[fix|core|月周期][公共]C1 类型列表改走毫秒区间 queryPagingRecordListByTypeIdInRange，消字符串自然月解析器

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

### Task 5: 首页 LauncherContentViewModel 走周期

**Files:**
- Modify: `feature/records/src/main/kotlin/cn/wj/android/cashbook/feature/records/viewmodel/LauncherContentViewModel.kt`
- Test: `feature/records/src/test/.../LauncherContentViewModelTest.kt`（若存在则补；该 VM 已注入 SettingRepository）

**Interfaces:**
- Consumes: `toDateRange(D)`、`currentMonthPeriod`（T1）、`SettingRepository.recordSettingsModel`（T2）

- [ ] **Step 1: 写/补失败测试**

读现有 `LauncherContentViewModelTest`（若有）。补一条：设 `FakeSettingRepository.setRecordSettings(monthStartDay=15)`，将 `_dateSelection` 设为 `ByMonth(2024-01)`（用 `updateDateSelection`），断言 `_summaryData`/查询使用区间 `[ms(2024,1,15), ms(2024,2,15))`。若无现成 Fake 注入查询区间的手段，则断言 summary 汇总值随 D 变化（用注入不同月记录的 FakeRecordRepository）。

> 若该模块无 `LauncherContentViewModelTest`，新建一个最小用例覆盖「D 影响周期区间」。VM 测试用 `LocalDate.now()` 的 init 会引入时间依赖——测试中通过 `updateDateSelection(ByMonth(固定 YM))` 覆盖初值，避免依赖 now()。

- [ ] **Step 2: 运行确认失败**

Run: `./gradlew :feature:records:testDebugUnitTest --tests "*LauncherContentViewModelTest*"`
Expected: FAIL（D 未生效，区间仍自然月）。

- [ ] **Step 3: 实现 D 响应式 + 当前周期初始化**

`LauncherContentViewModel.kt`：①顶部 import：

```kotlin
import cn.wj.android.cashbook.core.model.entity.normalizeMonthStartDay
import kotlinx.coroutines.flow.distinctUntilChanged
import java.time.LocalDate
```

②加 monthStartDay 流（class 内，`settingRepository` 已是构造参 `:61`）：

```kotlin
    private val _monthStartDay = settingRepository.recordSettingsModel
        .map { normalizeMonthStartDay(it.monthStartDay) }
        .distinctUntilChanged()
```

③`init` 块末尾追加（设当前周期；D=1 时等于 ByMonth(now)，无变化）：

```kotlin
        viewModelScope.launch {
            val d = settingRepository.recordSettingsModel.first().monthStartDay
            _dateSelection.value = DateSelectionEntity.currentMonthPeriod(LocalDate.now(), d)
        }
```

④`_summaryData`（`:116-119`）改为 combine D：

```kotlin
    private val _summaryData: Flow<List<RecordViewSummaryModel>> =
        combine(_dateSelection, _monthStartDay) { selection, d -> selection.toDateRange(d) }
            .flatMapLatest { (startDate, endDate) ->
                recordRepository.queryRecordViewSummariesFlow(startDate, endDate)
            }
```

⑤`recordPagingData`（`:131-133`）同理：

```kotlin
    val recordPagingData: Flow<PagingData<LauncherListItem>> =
        combine(_dateSelection, _monthStartDay) { selection, d -> selection.toDateRange(d) }
            .flatMapLatest { (startDate, endDate) ->
                recordRepository.getRecordPagingData(startDate, endDate)
                    // ... 其余 map 链保持不变
```

（`combine`/`first` 已 import；确认 `import kotlinx.coroutines.flow.combine` 存在。）

- [ ] **Step 4: 运行确认通过**

Run: `./gradlew :feature:records:testDebugUnitTest --tests "*LauncherContentViewModelTest*"`
Expected: PASS。

- [ ] **Step 5: spotless + 提交**

```bash
./gradlew :feature:records:spotlessApply --init-script gradle/init.gradle.kts --no-configuration-cache
git add feature/records/src/main/kotlin/cn/wj/android/cashbook/feature/records/viewmodel/LauncherContentViewModel.kt feature/records/src/test
git commit -m "[feat|feature|月周期][公共]首页 LauncherContentViewModel 走可配置周期(monthStartDay 响应式+currentMonthPeriod 初值)

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

### Task 6: AnalyticsViewModel 走周期 + 柱状图传 D

**Files:**
- Modify: `feature/records/.../viewmodel/AnalyticsViewModel.kt`
- Test: `feature/records/src/test/.../AnalyticsViewModelTest.kt`（构造点须补 `FakeSettingRepository`）

**Interfaces:**
- Consumes: `toDateRange(D)`/`currentMonthPeriod`（T1）、`TransRecordViewsToAnalyticsBarUseCase(..., monthStartDay)`（T3）、`SettingRepository`（T2）

- [ ] **Step 1: 改测试构造（加 FakeSettingRepository）+ 写失败用例**

`AnalyticsViewModelTest.kt`：在 `buildViewModel()`（约 `:87`）的 `AnalyticsViewModel(...)` 构造补 `settingRepository = FakeSettingRepository().apply { setRecordSettings(...monthStartDay = 15) }`。新增用例：设 D=15、选 `ByMonth(2024-01)`、注入一条 2024-02-03 EXPENDITURE 记录（属周期 [1/15,2/15)），断言 `uiState` 的 `totalExpenditure` 含该记录、且对应日桶存在（验证 bar 传 D + 区间走 D 两件事）。

- [ ] **Step 2: 运行确认失败**

Run: `./gradlew :feature:records:testDebugUnitTest --tests "*AnalyticsViewModelTest*"`
Expected: FAIL（构造缺参编译失败 → 补参后用例 FAIL：D 未生效）。

- [ ] **Step 3: 实现**

`AnalyticsViewModel.kt`：①构造加 `private val settingRepository: SettingRepository,`（import `cn.wj.android.cashbook.core.data.repository.SettingRepository`）；②加 monthStartDay 流 + import（同 T5 Step3①②）；③`_selectionWithRecords`（`:88-90`）与 uiState 绑定 D。最小改法：把 D 并入 selection-records 绑定，并在 bar 调用传 D：

```kotlin
    private val _monthStartDay = settingRepository.recordSettingsModel
        .map { normalizeMonthStartDay(it.monthStartDay) }
        .distinctUntilChanged()

    private val _selectionWithRecords =
        combine(_dateSelection, _monthStartDay) { selection, d -> selection to d }
            .mapLatest { (selection, d) ->
                Triple(selection, d, getRecordViewsBetweenDateUseCase(selection, d))
            }

    val uiState = _selectionWithRecords.mapLatest { (selection, d, list) ->
        // ...
        val barList = transRecordViewsToAnalyticsBarUseCase(selection, list, d)
        // ... 其余不变
    }
```

④`showSheet`（`:158`）的 `_selectionWithRecords.first().second` 改为 `.third`（取记录列表，因元组从 Pair 变 Triple）。
⑤`init`（class 无 init 则新增）加 currentMonthPeriod 初值（同 T5 Step3③，用 `settingRepository`）。

> 依赖：`GetRecordViewsBetweenDateUseCase` 需支持传 D —— 见 Step 3.5。

- [ ] **Step 3.5: 给 GetRecordViewsBetweenDateUseCase 加 D 参**

`core/domain/.../GetRecordViewsBetweenDateUseCase.kt`（`:39-47`）加默认参：

```kotlin
    suspend operator fun invoke(
        dateSelection: DateSelectionEntity,
        monthStartDay: Int = 1,
    ): List<RecordViewsModel> = withContext(coroutineContext) {
        val (from, to) = dateSelection.toDateRange(monthStartDay)
        recordModelTransToViewsUseCase(
            recordRepository.queryRecordListBetweenDate(from, to),
        )
    }
```

（默认参 → 其它调用方源码兼容。其测试 `GetRecordViewsBetweenDateUseCaseTest` 无需改；如需可加 D≠1 用例。）

- [ ] **Step 4: 运行确认通过**

Run: `./gradlew :feature:records:testDebugUnitTest --tests "*AnalyticsViewModelTest*" :core:domain:testDebugUnitTest --tests "*GetRecordViewsBetweenDateUseCaseTest*"`
Expected: PASS。

- [ ] **Step 5: spotless + 提交**

```bash
./gradlew :feature:records:spotlessApply :core:domain:spotlessApply --init-script gradle/init.gradle.kts --no-configuration-cache
git add feature/records/src core/domain/src
git commit -m "[feat|feature|月周期][公共]AnalyticsViewModel 走可配置周期+柱状图传 D(GetRecordViewsBetweenDateUseCase 加 monthStartDay)

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

### Task 7: TypedAnalyticsViewModel 走周期（含 C1 类型列表 + summary）

**Files:**
- Modify: `feature/records/.../viewmodel/TypedAnalyticsViewModel.kt`
- Test: `feature/records/src/test/.../TypedAnalyticsViewModelTest.kt`（构造补 `FakeSettingRepository`）

**Interfaces:**
- Consumes: `toDateRange(D)`/`currentMonthPeriod`（T1）、`GetTypeRecordViewsUseCase(typeId, start, end, ...)`（T4，毫秒区间）、`GetTypedMonthSummaryUseCase`、`GetTagRecordViewsUseCase(tagId, start, end, ...)`、`SettingRepository`（T2）

- [ ] **Step 1: 改测试构造 + 写失败用例**

`TypedAnalyticsViewModelTest.kt`：`buildViewModel()`（约 `:82`）构造补 `settingRepository = FakeSettingRepository().apply { setRecordSettings(...monthStartDay = 15) }`。新增 C1 金丝雀用例：D=15、`updateData(typeId=100, date="2024-01", ...)`、注入一条 2024-02-03 type=100 EXPENDITURE 记录，断言 **summary 与 recordList 同口径**：summary.expenditure 含该记录 **且** recordList 分页首页含该记录（证明列表与汇总区间一致，不再分裂）。

- [ ] **Step 2: 运行确认失败**

Run: `./gradlew :feature:records:testDebugUnitTest --tests "*TypedAnalyticsViewModelTest*"`
Expected: FAIL（构造缺参 → 补后：列表走自然月不含该记录、与 summary 分裂）。

- [ ] **Step 3: 实现（D 注入 + 列表/汇总统一毫秒区间）**

`TypedAnalyticsViewModel.kt`：①构造加 `private val settingRepository: SettingRepository,` + import；②加 `_monthStartDay` 流 + import（同前）；③把 `recordList` 的 combine 加入 `_monthStartDay`，并在数据类里携带毫秒区间，paging source 改为接收 `(start,end)`：

将 `GetTypedRecordData` 改为携带毫秒区间：

```kotlin
data class GetTypedRecordData(
    val isType: Boolean,
    val id: Long,
    val startDate: Long,
    val endDate: Long,
    val includeChildTypes: Boolean,
)
```

`recordList`（`:103-135`）combine 加 `_monthStartDay`，构造 data 时算区间：

```kotlin
    val recordList = combine(
        _tagIdData, _typeIdData, _includeChildTypes, _dateSelection, _monthStartDay, recordDataVersion,
    ) { values ->
        val tagId = values[0] as Long
        val typeId = values[1] as Long
        val includeChild = values[2] as Boolean
        val selection = values[3] as DateSelectionEntity
        val d = values[4] as Int
        val isType = typeId != -1L
        val (start, end) = selection.toDateRange(d)
        GetTypedRecordData(isType, if (isType) typeId else tagId, start, end, includeChild)
    }
        .flatMapLatest { data ->
            Pager(
                config = PagingConfig(pageSize = DEFAULT_PAGE_SIZE, initialLoadSize = DEFAULT_PAGE_SIZE),
                pagingSourceFactory = {
                    if (data.isType) {
                        TypeRecordPagingSource(data.id, data.startDate, data.endDate, data.includeChildTypes, getTypeRecordViewsUseCase)
                    } else {
                        TagRecordPagingSource(data.id, data.startDate, data.endDate, getTagRecordViewsUseCase)
                    }
                },
            ).flow
                .map { paging -> paging.map { LauncherListItem.Record(it) as LauncherListItem } }
                .map { paging -> paging.insertSeparators { before, after -> recordDaySeparator(before, after) } }
        }
        .cachedIn(viewModelScope)
```

> 注：`combine` 6 参用 vararg 形式（Kotlin `combine` 最多 5 个具名 lambda 参，6 个须用 `combine(vararg)` + 索引取值，如上）。

④`summary`（`:138-158`）combine 加 `_monthStartDay`，`toDateRange()` → `toDateRange(d)`：

```kotlin
    val summary: StateFlow<AssetMonthSummaryModel> = combine(
        _tagIdData, _typeIdData, _includeChildTypes, _dateSelection, _monthStartDay, recordDataVersion,
    ) { values ->
        val tagId = values[0] as Long
        val typeId = values[1] as Long
        val includeChild = values[2] as Boolean
        val selection = values[3] as DateSelectionEntity
        val d = values[4] as Int
        val isType = typeId != -1L
        val id = if (isType) typeId else tagId
        if (id == -1L) {
            AssetMonthSummaryModel(0L, 0L, 0L)
        } else {
            val (start, end) = selection.toDateRange(d)
            getTypedMonthSummaryUseCase(isType, id, start, end, includeChild)
        }
    }
        .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5_000L), initialValue = AssetMonthSummaryModel(0L, 0L, 0L))
```

⑤改 `TypeRecordPagingSource`（`:208-231`）与 `TagRecordPagingSource`（`:236-...`）构造为接收 `startDate/endDate: Long`，`load` 内不再 `selection.toDateRange()`/`getDisplayText()`：

```kotlin
private class TypeRecordPagingSource(
    private val typeId: Long,
    private val startDate: Long,
    private val endDate: Long,
    private val includeChildTypes: Boolean,
    private val getTypeRecordViewsUseCase: GetTypeRecordViewsUseCase,
) : PagingSource<Int, RecordViewsEntity>() {
    override fun getRefreshKey(state: PagingState<Int, RecordViewsEntity>): Int? = null
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, RecordViewsEntity> = runCatching {
        val page = params.key ?: 0
        val pageSize = params.loadSize
        val items = getTypeRecordViewsUseCase(typeId, startDate, endDate, page, pageSize, includeChildTypes)
        LoadResult.Page(items, if (page > 0) page - 1 else null, if (items.isNotEmpty()) page + 1 else null)
    }.getOrElse { LoadResult.Error(it) }
}

private class TagRecordPagingSource(
    private val tagId: Long,
    private val startDate: Long,
    private val endDate: Long,
    private val getTagRecordViewsUseCase: GetTagRecordViewsUseCase,
) : PagingSource<Int, RecordViewsEntity>() {
    override fun getRefreshKey(state: PagingState<Int, RecordViewsEntity>): Int? = null
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, RecordViewsEntity> = runCatching {
        val page = params.key ?: 0
        val pageSize = params.loadSize
        val items = getTagRecordViewsUseCase(tagId, startDate, endDate, page, pageSize)
        LoadResult.Page(items, if (page > 0) page - 1 else null, if (items.isNotEmpty()) page + 1 else null)
    }.getOrElse { LoadResult.Error(it) }
}
```

> `All` 态：`toDateRange(d)` 对 `All` 返回 `(0, Long.MAX_VALUE)`，type/tag 毫秒查询天然返回全部，无需特判（去掉旧 `if (selection is All) ""` 空串逻辑）。保留 `logger()` 的 `.getOrElse` 错误日志（按原风格）。

⑥`updateData`（`:160-173`）的 `appliedDateKey` 守卫保留；初值仍用 `fromDisplayTextOrNull(date) ?: ByMonth(YearMonth.now())`——若 `date` 为空（无日期入口）应取当前周期：把兜底改为 `currentMonthPeriod`：

```kotlin
            _dateSelection.tryEmit(
                DateSelectionEntity.fromDisplayTextOrNull(date)
                    ?: DateSelectionEntity.currentMonthPeriod(LocalDate.now(), settingRepository.recordSettingsModel.first().monthStartDay),
            )
```

> `updateData` 当前非 suspend；`recordSettingsModel.first()` 需 suspend 上下文。改法：用 `viewModelScope.launch { ... }` 包裹该 emit，或保留同步兜底 `ByMonth(YearMonth.now())`（D=1 等价；D≠1 仅"无日期入口"分支轻微不精确，可接受）。**推荐**保留同步 `ByMonth(YearMonth.now())` 兜底以免 updateData 改 suspend 牵动 Route 调用——因为有日期入口时走 `fromDisplayTextOrNull` 不受影响，无日期入口本就少见。实施者二选一并在 commit 注明。

- [ ] **Step 4: 运行确认通过**

Run: `./gradlew :feature:records:testDebugUnitTest --tests "*TypedAnalyticsViewModelTest*"`
Expected: PASS（C1 金丝雀：列表与汇总同口径）。

- [ ] **Step 5: spotless + 提交**

```bash
./gradlew :feature:records:spotlessApply --init-script gradle/init.gradle.kts --no-configuration-cache
git add feature/records/src
git commit -m "[fix|feature|月周期][公共]C1 TypedAnalyticsViewModel 列表+汇总统一走 toDateRange(D)毫秒区间，消同屏口径分裂

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

### Task 8: AssetInfoContentViewModel 走周期

**Files:**
- Modify: `feature/records/.../viewmodel/AssetInfoContentViewModel.kt`
- Test: `feature/records/src/test/.../AssetInfoContentViewModelTest.kt`（构造补 `FakeSettingRepository`）

**Interfaces:**
- Consumes: `toDateRange(D)`/`currentMonthPeriod`（T1）、`SettingRepository`（T2）

- [ ] **Step 1: 改测试构造 + 写失败用例**

`AssetInfoContentViewModelTest.kt`：`buildViewModel()`（约 `:171`）构造补 `settingRepository = FakeSettingRepository().apply { setRecordSettings(...monthStartDay = 15) }`。新增用例：D=15、`updateAssetId` + `updateMonth(2024-01)`、注入 2024-02-03 该资产记录，断言 `summary`/`recordList` 区间含该记录（周期 [1/15,2/15)）。

- [ ] **Step 2: 运行确认失败**

Run: `./gradlew :feature:records:testDebugUnitTest --tests "*AssetInfoContentViewModelTest*"`
Expected: FAIL（构造缺参 → 补后 D 未生效）。

- [ ] **Step 3: 实现**

`AssetInfoContentViewModel.kt`：①构造加 `settingRepository: SettingRepository,` + import；②加 `_monthStartDay` 流 + import；③`recordList`（`:74-101`）combine 加 `_monthStartDay`、`toDateRange()`→`toDateRange(d)`：

```kotlin
    val recordList = combine(_assetIdData, _dateSelection, _monthStartDay, recordDataVersion) { assetId, selection, d, _ ->
        assetId to selection.toDateRange(d)
    }
        .flatMapLatest { (assetId, range) -> /* ... 原 Pager 链不变 ... */ }
        .cachedIn(viewModelScope)
```

④`summary`（`:104-113`）combine 加 `_monthStartDay`、`toDateRange()`→`toDateRange(d)`：

```kotlin
    val summary: StateFlow<AssetMonthSummaryModel> =
        combine(_assetIdData, _isCreditCard, _dateSelection, _monthStartDay, recordDataVersion) { values ->
            val id = values[0] as Long
            val isCreditCard = values[1] as Boolean
            val selection = values[2] as DateSelectionEntity
            val d = values[3] as Int
            val (startDate, endDate) = selection.toDateRange(d)
            getAssetMonthSummaryUseCase(id, isCreditCard, startDate, endDate)
        }
            .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5_000), initialValue = AssetMonthSummaryModel(0L, 0L, 0L))
```

> `recordList` 的 combine 已是 4 参（含 recordDataVersion），加 `_monthStartDay` 为 5 参，须改 vararg+索引形式（同 summary）。
⑤`init`/默认周期：资产详情入口由 `updateMonth` 驱动；可在 `updateAssetId` 后或 init 设 `currentMonthPeriod`。最小改：保留 `_dateSelection` 默认 `ByMonth(YearMonth.now())`（D=1 等价），不强加 init（资产页通常由调用方 `updateMonth` 指定）。若需 D≠1 默认精确，在 init launch 设 `currentMonthPeriod(LocalDate.now(), d)`。

- [ ] **Step 4: 运行确认通过**

Run: `./gradlew :feature:records:testDebugUnitTest --tests "*AssetInfoContentViewModelTest*"`
Expected: PASS。

- [ ] **Step 5: spotless + 提交**

```bash
./gradlew :feature:records:spotlessApply --init-script gradle/init.gradle.kts --no-configuration-cache
git add feature/records/src
git commit -m "[feat|feature|月周期][公共]AssetInfoContentViewModel 走可配置周期(toDateRange(D)响应式)

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

### Task 9: 设置 UI — 月起始日选择项

**Files:**
- Modify: `feature/settings/.../enums/SettingDialogEnum.kt`
- Modify: `feature/settings/.../viewmodel/SettingViewModel.kt`
- Modify: `feature/settings/.../screen/SettingScreen.kt`
- Modify: `core/ui/src/main/res/values/strings.xml`（或 feature:settings 的 strings）—— 新增文案
- Test: `feature/settings/src/test/.../SettingScreenScreenshotTests.kt`（基线）/ `SettingViewModelTest`（若有）

**Interfaces:**
- Consumes: `SettingRepository.recordSettingsModel`/`updateMonthStartDay`（T2）

- [ ] **Step 1: 加 Dialog 枚举**

`SettingDialogEnum.kt` 末尾加：

```kotlin
    /** 月起始日 */
    MONTH_START_DAY,
```

- [ ] **Step 2: 读现有 imageQuality 接线作为模板**

读 `SettingViewModel.kt` 与 `SettingScreen.kt` 完整，定位 `imageQuality` 的全链路接线：`SettingUiState.imageQuality`、`onImageQualityClick`、`onImageQualitySelected`、`DialogContent` 内 `IMAGE_QUALITY` 分支对话框。月起始日严格按此模板平移（month 来自 `recordSettingsModel` 而非 `appSettingsModel`，注意 uiState 合成需 combine recordSettings）。

- [ ] **Step 3: SettingViewModel 加 monthStartDay**

①`SettingUiState`（成功态 data class）加 `val monthStartDay: Int`；②uiState 合成处把 `settingRepository.recordSettingsModel` combine 进来取 `monthStartDay`；③加：

```kotlin
    fun onMonthStartDayClick() {
        dialogState = DialogState.Shown(SettingDialogEnum.MONTH_START_DAY)
    }

    fun onMonthStartDaySelected(day: Int) {
        viewModelScope.launch {
            settingRepository.updateMonthStartDay(day)
            dismissDialog()
        }
    }
```

（`settingRepository`/`viewModelScope`/`DialogState`/`SettingDialogEnum` 按现有 import；新增 SettingDialogEnum import 若缺。）

- [ ] **Step 4: SettingScreen 加列表项 + 对话框**

①在 `SettingRoute` 的 `SettingScreen(...)` 调用补回调 `onMonthStartDayClick = viewModel::onMonthStartDayClick`、`onMonthStartDaySelected = viewModel::onMonthStartDaySelected`，并逐层透传到 `SettingScreen`→`SettingContent`→`DialogContent`（参数列表与 `onImageQualityClick/onImageQualitySelected` 并列添加；`DialogContent` 还需接 `monthStartDay = uiState.monthStartDay`）。
②在 `LazyColumn` 内某 `item {}`（紧邻图片质量项）加：

```kotlin
            item {
                CbListItem(
                    headlineContent = { Text(text = stringResource(id = R.string.month_start_day)) },
                    supportingContent = { Text(text = stringResource(id = R.string.month_start_day_hint)) },
                    trailingContent = { Text(text = "${uiState.monthStartDay}") },
                    modifier = Modifier.clickable(onClick = onMonthStartDayClick),
                )
            }
```

③在 `DialogContent` 加 `MONTH_START_DAY` 分支——单选 1..28（用 `CbAlertDialog` + 可滚动单选列表，仿 `IMAGE_QUALITY` 分支结构）：

```kotlin
        SettingDialogEnum.MONTH_START_DAY -> {
            CbAlertDialog(
                onDismissRequest = onRequestDismissDialog,
                title = { Text(text = stringResource(id = R.string.month_start_day)) },
                text = {
                    LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                        items((1..28).toList()) { day ->
                            CbListItem(
                                headlineContent = { Text(text = "$day") },
                                trailingContent = { if (day == monthStartDay) Icon(imageVector = CbIcons.Check, contentDescription = null) },
                                modifier = Modifier.clickable { onMonthStartDaySelected(day) },
                            )
                        }
                    }
                },
                confirmButton = { CbTextButton(onClick = onRequestDismissDialog) { Text(text = stringResource(id = R.string.cancel)) } },
            )
        }
```

> 严格对齐文件内 `IMAGE_QUALITY` 分支已用的组件/参数风格（`CbAlertDialog`/`CbListItem`/`CbTextButton`/`CbIcons` 实际签名以文件内现用为准）。

- [ ] **Step 5: 加文案**

在 strings.xml（与 `image_quality` 同文件）加：

```xml
    <string name="month_start_day">月起始日</string>
    <string name="month_start_day_hint">影响本月统计的起止（如 15 表示每月 15 日至次月 14 日）</string>
```

- [ ] **Step 6: 编译 + 截图基线 + 测试**

Run:
```bash
./gradlew :feature:settings:compileDebugKotlin
./gradlew recordRoborazziOnlineDebug -p . --tests "*SettingScreenScreenshotTests*" 2>/dev/null || ./gradlew :feature:settings:recordRoborazziDebug
./gradlew :feature:settings:testDebugUnitTest
```
Expected: 编译通过；若 `SettingScreenScreenshotTests` 存在则 record 新增项的基线（grep 确认基线 PNG 落 `feature/settings/src/test/screenshots/`，verify 0 diff）；`SettingViewModelTest`（若有）PASS（如签名变更同步改其构造）。

> 若 `SettingScreen` 测试不直接构造 VM（用 Composable 预览），则只需补/更新该屏截图基线；若有 `SettingViewModelTest` 构造 VM，按强约定同步补 `FakeSettingRepository`/参数。

- [ ] **Step 7: spotless + 提交**

```bash
./gradlew :feature:settings:spotlessApply --init-script gradle/init.gradle.kts --no-configuration-cache
git add feature/settings/src core/ui/src/main/res
git commit -m "[feat|feature|月周期][公共]设置页新增月起始日(1-28)选择项+对话框，接 SettingRepository.updateMonthStartDay

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## 完成后整体验证

- [ ] 全量回归：`./gradlew :core:model:test :core:data:testDebugUnitTest :core:domain:testDebugUnitTest :feature:records:testDebugUnitTest :feature:settings:testDebugUnitTest`
- [ ] D=1 等价金丝雀全绿（T1/T3/T5-8 中的 D=1 用例）。
- [ ] `spotlessCheck`：`./gradlew spotlessCheck --init-script gradle/init.gradle.kts --no-configuration-cache`
- [ ] 真机/模拟器自验（有设备时）：设置月起始日=15 → 首页/分析/分类统计「本月」按 [15,次15) 显示；日历仍自然月；设回 1 → 全部恢复现状。
- [ ] **节点2 full-review**（comprehensive-review:full-review）对本计划全部 git diff 做最终多维评审；blocking（Critical/High）交付前修复。

## Self-Review（计划对 spec 的覆盖核对）

- spec §3.1 存储/clamp → T2 ✓；§3.2 toDateRange(D)/currentMonthPeriod/normalize → T1 ✓；§3.3-A 经 toDateRange 路径 → T5(首页)/T6(分析)/T7(分类标签)/T8(资产) ✓；§3.3-B C1 类型列表 → T4+T7 ✓；§3.3-C C2 柱状图 → T3 ✓；§3.3-D 响应式 D 注入 → T5-T8 ✓；§3.4 设置 UI → T9 ✓；H1 clamp 防 atDay(0) → T1(normalize)+T2(映射) ✓。
- 日历不改：本计划无 CalendarViewModel/queryRecordByYearMonth 改动 ✓。
- 无 DB migration ✓。
- 类型一致性：`toDateRange(monthStartDay)`、`currentMonthPeriod(today, monthStartDay)`、`normalizeMonthStartDay(raw)`、`queryPagingRecordListByTypeIdInRange(...)`、`GetTypeRecordViewsUseCase(typeId,startDate,endDate,...)`、`updateMonthStartDay(Int)` 在各 Task 间一致。
- 已知留待实施确认点（已在步骤内标注）：①`RecordViewsModel` 构造参数名以实际为准（T3 测试）；②`combine` ≥6 参用 vararg 索引形式（T7）；③`updateData` 无日期入口兜底 suspend 取舍（T7 Step3⑥）；④Setting 截图测试是否构造 VM（T9 Step6）。
