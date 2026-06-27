# 提醒通知（N1 信用卡 + N2 待报销）Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 Cashbook 增加信用卡账单/还款日当天提醒（N1）与待报销每月提醒（N2），单一每日 WorkManager Worker 调度 + Proto DataStore 两个全局开关 + 独立通知渠道 + 补发机制。

**Architecture:** 提醒判定为 top-level `internal fun` 纯函数（纯 JVM 单测），`DailyReminderWorker` 薄壳取数据→调纯函数→发通知；调度在 `InitWorker` 内注册（对齐既有约定）；开关存 `app_settings.proto`，补发用 `lastReminderCheckMs` 记录上次检查日逐日补查。

**Tech Stack:** Kotlin, WorkManager (PeriodicWorkRequest + DelegatingWorker 委派), Hilt (HiltWorker), Proto DataStore, NotificationCompat, java.time, JUnit4 + Truth (纯 JVM 单测), Roborazzi (设置页截图).

**对应 spec:** `docs/superpowers/specs/2026-06-26-reminder-notifications-design.md`

## Global Constraints

> 每个 task 的要求隐含包含本节。

- **proto 字段号**：`app_settings.proto` 接续加 `26/27/28`（现最大 25），**不复用、不改已有号**。
- **AppSettingsModel 加字段**：3 个新字段置 data class **末位** 且**写 Kotlin 默认值**（`= false` / `= 0L`），避免破坏 4 个命名构造站点。
- **设置链路贯穿 4 个构造站点 + 2 个接口实现**：构造站点 = `CombineProtoDataSource.kt:70`(读映射) + `FakeSettingRepository.kt:36` + `FakeCombineProtoDataSource.kt:45`；接口实现 = `SettingRepositoryImpl`(setter 委托) + `FakeSettingRepository`。**`CombineProtoDataSource` 读映射必加 3 行否则 proto 值不传播（静默 bug）。**
- **无 Room migration**（Proto DataStore 非 Room；proto3 标量默认 false/0 向后兼容）。
- **通知安全**：PendingIntent 用 `PendingIntent.getActivity(..., FLAG_IMMUTABLE)` + 显式组件 `Intent(context, MainActivity::class.java)`；渠道/通知 `setVisibility(NotificationCompat.VISIBILITY_PRIVATE)` + `setPublicVersion` 脱敏；卡名作 `getString(res, name)` 实参不当格式串。
- **深链禁绕安全门**：深链经 Intent extra → `MainActivity.onCreate`/`onNewIntent` → `MainApp.kt:227` 受 `if (!needRequestProtocol && !needVerity)`(:230) 门控的 `LaunchedEffect` 导航（复用 shortcutsType 机制）；**禁新增 `<deepLink>`/`VIEW` intent-filter**。
- **N1 已知限制**：仅当前账本 + 仅可见信用卡（`currentVisibleAssetListData`）。
- **设计系统**：设置页用 `core:design` 的 Cb* 封装（禁裸 Material3，lint `Design` 拦截）；新通知渠道 string 走资源。
- **License Header**：所有新建 .kt 须含 Apache 2.0 header（Spotless 检查，模板见 `spotless/`）；提交前 `./gradlew spotlessApply --init-script gradle/init.gradle.kts --no-configuration-cache`。
- **app 模块编译/测试带 flavor**：`:app:compileOnlineDebugKotlin`（非 `:app:compileDebugKotlin`）；feature/core 模块无 flavor。
- **测试替身忠实复刻**：Fake 方法须复刻真实语义，禁空桩假阳性。

---

## File Structure

| 文件 | 责任 | 动作 |
|---|---|---|
| `core/datastore-proto/src/main/proto/app_settings.proto` | proto 加 3 字段 | Modify |
| `core/model/.../model/AppSettingsModel.kt` | model 加 3 字段（Kotlin 默认值） | Modify |
| `core/datastore/.../datasource/CombineProtoDataSource.kt` | 读映射 + 3 writer | Modify |
| `core/data/.../repository/SettingRepository.kt` | 接口加 3 方法 | Modify |
| `core/data/.../repository/impl/SettingRepositoryImpl.kt` | 委托实现 | Modify |
| `core/testing/.../repository/FakeSettingRepository.kt` | Fake 同步 | Modify |
| `core/data/.../testdoubles/FakeCombineProtoDataSource.kt` | Fake 同步 | Modify |
| `sync/work/.../reminder/ReminderModels.kt` | `ReminderItem` sealed + `CreditCardReminderInfo` | Create |
| `sync/work/.../reminder/ReminderLogic.kt` | `computeReminders` + `reminderCheckDates` 纯函数 | Create |
| `sync/work/src/test/.../reminder/ReminderLogicTest.kt` | 纯 JVM 单测 | Create |
| `sync/work/.../initializers/SyncWorkHelpers.kt` | `ReminderNotificationChannel` + builder | Modify |
| `sync/work/.../initializers/SyncInitializer.kt` | `ReminderWorkName` 常量 | Modify |
| `sync/work/.../workers/DailyReminderWorker.kt` | 薄壳 Worker | Create |
| `sync/work/.../workers/InitWorker.kt` | 注册周期任务 | Modify |
| `app/.../MainActivity.kt` | 解析提醒深链 extra | Modify |
| `app/.../ui/MainApp.kt` | 门控 LaunchedEffect 消费深链 | Modify |
| `core/common/.../*` | 提醒深链类型常量（仿 `SHORTCUTS_TYPE_*`） | Modify |
| `feature/settings/.../screen/SettingScreen.kt` | 「提醒」分组 2 开关 | Modify |
| `feature/settings/.../viewmodel/SettingViewModel.kt` | 开关状态/方法 | Modify |
| 通知 string 资源（sync 模块 res） | 渠道名/文案 | Modify |

---

## Phase 1 — 数据层开关 + 补发字段

### Task 1.1: proto + AppSettingsModel + CombineProtoDataSource

**Files:**
- Modify: `core/datastore-proto/src/main/proto/app_settings.proto`
- Modify: `core/model/src/main/kotlin/cn/wj/android/cashbook/core/model/model/AppSettingsModel.kt`
- Modify: `core/datastore/src/main/kotlin/cn/wj/android/cashbook/core/datastore/datasource/CombineProtoDataSource.kt`

**Interfaces:**
- Produces: `AppSettingsModel.creditCardReminderEnable: Boolean`、`.reimbursementReminderEnable: Boolean`、`.lastReminderCheckMs: Long`；`CombineProtoDataSource.updateCreditCardReminderEnable(Boolean)`、`.updateReimbursementReminderEnable(Boolean)`、`.updateLastReminderCheckMs(Long)`。

- [ ] **Step 1: 改 proto，加 3 字段**

`app_settings.proto` 在 `imageQuality = 25;` 后追加：
```proto
  bool creditCardReminderEnable = 26;       // 信用卡账单/还款提醒开关
  bool reimbursementReminderEnable = 27;    // 待报销提醒开关
  sint64 lastReminderCheckMs = 28;          // 上次提醒检查日期(epoch ms)，0=从未；补发用
```

- [ ] **Step 2: 改 AppSettingsModel，末位加 3 字段（含 Kotlin 默认值）**

`AppSettingsModel.kt` 在 `imageQuality: ImageQualityEnum,` 后追加：
```kotlin
    /** 信用卡账单/还款提醒开关 */
    val creditCardReminderEnable: Boolean = false,
    /** 待报销提醒开关 */
    val reimbursementReminderEnable: Boolean = false,
    /** 上次提醒检查日期(epoch ms)，0=从未 */
    val lastReminderCheckMs: Long = 0L,
```

- [ ] **Step 3: 改 CombineProtoDataSource 读映射 + 加 3 writer**

`appSettingsData` 的 `AppSettingsModel(...)` 在 `imageQuality = ...,` 后加：
```kotlin
            creditCardReminderEnable = it.creditCardReminderEnable,
            reimbursementReminderEnable = it.reimbursementReminderEnable,
            lastReminderCheckMs = it.lastReminderCheckMs,
```
在 `updateLogcatInRelease` 后加 3 个 writer（仿 `updateCanary` 模式）：
```kotlin
    suspend fun updateCreditCardReminderEnable(enable: Boolean) {
        appSettings.updateData { it.copy { this.creditCardReminderEnable = enable } }
    }

    suspend fun updateReimbursementReminderEnable(enable: Boolean) {
        appSettings.updateData { it.copy { this.reimbursementReminderEnable = enable } }
    }

    suspend fun updateLastReminderCheckMs(ms: Long) {
        appSettings.updateData { it.copy { this.lastReminderCheckMs = ms } }
    }
```

- [ ] **Step 4: 编译验证 datastore-proto + model + datastore**

Run: `./gradlew :core:datastore-proto:build :core:model:compileDebugKotlin :core:datastore:compileDebugKotlin --no-configuration-cache`
Expected: BUILD SUCCESSFUL（proto 重新生成含新字段，model 默认值不破坏构造点）。
> 经代理拉依赖时加 `-Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyPort=7897 -Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=7897`，暖缓存后可 `--offline`。

- [ ] **Step 5: Commit**

```bash
git add core/datastore-proto/src/main/proto/app_settings.proto core/model/.../AppSettingsModel.kt core/datastore/.../CombineProtoDataSource.kt
git commit -m "[feat|提醒|通知][公共]app_settings 加提醒开关+补发字段(26/27/28)+CombineProtoDataSource 映射/writer"
```

### Task 1.2: SettingRepository 接口 + Impl + 两 Fake

**Files:**
- Modify: `core/data/src/main/kotlin/cn/wj/android/cashbook/core/data/repository/SettingRepository.kt`
- Modify: `core/data/src/main/kotlin/cn/wj/android/cashbook/core/data/repository/impl/SettingRepositoryImpl.kt`
- Modify: `core/testing/src/main/kotlin/cn/wj/android/cashbook/core/testing/repository/FakeSettingRepository.kt`
- Modify: `core/data/src/test/kotlin/cn/wj/android/cashbook/core/data/testdoubles/FakeCombineProtoDataSource.kt`

**Interfaces:**
- Consumes: Task 1.1 的 `CombineProtoDataSource.updateXxx`。
- Produces: `SettingRepository.updateCreditCardReminderEnable(Boolean)`、`.updateReimbursementReminderEnable(Boolean)`、`.updateLastReminderCheckMs(Long)`。

- [ ] **Step 1: 接口加 3 方法**

`SettingRepository.kt` 在 `updateMonthStartDay` 后加：
```kotlin
    /** 更新信用卡提醒开关 */
    suspend fun updateCreditCardReminderEnable(enable: Boolean)

    /** 更新待报销提醒开关 */
    suspend fun updateReimbursementReminderEnable(enable: Boolean)

    /** 更新上次提醒检查日期(epoch ms) */
    suspend fun updateLastReminderCheckMs(ms: Long)
```

- [ ] **Step 2: SettingRepositoryImpl 委托实现**

`SettingRepositoryImpl` 加（仿现有 setter 委托 `combineProtoDataSource.updateXxx`，先读现有 `updateCanary` 委托确认写法）：
```kotlin
    override suspend fun updateCreditCardReminderEnable(enable: Boolean) {
        combineProtoDataSource.updateCreditCardReminderEnable(enable)
    }

    override suspend fun updateReimbursementReminderEnable(enable: Boolean) {
        combineProtoDataSource.updateReimbursementReminderEnable(enable)
    }

    override suspend fun updateLastReminderCheckMs(ms: Long) {
        combineProtoDataSource.updateLastReminderCheckMs(ms)
    }
```

- [ ] **Step 3: FakeSettingRepository 同步（默认值 + 3 override）**

`_appSettingsModel` 初值的 `AppSettingsModel(...)` 因 Task 1.1 给了 Kotlin 默认值，**可不改**（默认 false/0L）。在 `updateMonthStartDay` 后加 3 个 override：
```kotlin
    override suspend fun updateCreditCardReminderEnable(enable: Boolean) {
        _appSettingsModel.value = _appSettingsModel.value.copy(creditCardReminderEnable = enable)
    }

    override suspend fun updateReimbursementReminderEnable(enable: Boolean) {
        _appSettingsModel.value = _appSettingsModel.value.copy(reimbursementReminderEnable = enable)
    }

    override suspend fun updateLastReminderCheckMs(ms: Long) {
        _appSettingsModel.value = _appSettingsModel.value.copy(lastReminderCheckMs = ms)
    }
```

- [ ] **Step 4: FakeCombineProtoDataSource 同步 3 方法**

`FakeCombineProtoDataSource` 在 `updateLogcatInRelease` 后加：
```kotlin
    suspend fun updateCreditCardReminderEnable(enable: Boolean) {
        _appSettings.update { it.copy(creditCardReminderEnable = enable) }
    }

    suspend fun updateReimbursementReminderEnable(enable: Boolean) {
        _appSettings.update { it.copy(reimbursementReminderEnable = enable) }
    }

    suspend fun updateLastReminderCheckMs(ms: Long) {
        _appSettings.update { it.copy(lastReminderCheckMs = ms) }
    }
```
（`_appSettings` 初值 `AppSettingsModel(...)` 同样靠 Kotlin 默认值，不改。）

- [ ] **Step 5: 编译验证 core:data + core:testing**

Run: `./gradlew :core:data:compileDebugKotlin :core:data:compileDebugUnitTestKotlin :core:testing:compileDebugKotlin --no-configuration-cache`
Expected: BUILD SUCCESSFUL（接口 2 实现都补齐、`:core:data:testDebugUnitTest` 编译不因 Fake 缺实现失败）。

- [ ] **Step 6: spotless + Commit**

```bash
./gradlew spotlessApply --init-script gradle/init.gradle.kts --no-configuration-cache
git add core/data/.../SettingRepository.kt core/data/.../SettingRepositoryImpl.kt core/testing/.../FakeSettingRepository.kt core/data/.../FakeCombineProtoDataSource.kt
git commit -m "[feat|提醒|通知][公共]SettingRepository 加提醒开关/补发 setter + 两 Fake 同步"
```

---

## Phase 2 — 提醒判定 + 补发纯函数（核心逻辑，纯 JVM 单测）

### Task 2.1: ReminderItem 模型 + computeReminders

**Files:**
- Create: `sync/work/src/main/kotlin/cn/wj/android/cashbook/sync/reminder/ReminderModels.kt`
- Create: `sync/work/src/main/kotlin/cn/wj/android/cashbook/sync/reminder/ReminderLogic.kt`
- Test: `sync/work/src/test/kotlin/cn/wj/android/cashbook/sync/reminder/ReminderLogicTest.kt`

**Interfaces:**
- Produces:
  - `sealed interface ReminderItem`：`CreditCardBilling(assetId: Long, assetName: String)` / `CreditCardRepayment(assetId: Long, assetName: String)` / `Reimbursement(count: Int)`
  - `data class CreditCardReminderInfo(assetId: Long, name: String, billingDate: String, repaymentDate: String)`
  - `internal fun computeReminders(date: LocalDate, creditCardEnable: Boolean, reimbursementEnable: Boolean, creditCards: List<CreditCardReminderInfo>, monthStartDay: Int, reimbursableCount: Int): List<ReminderItem>`

- [ ] **Step 1: Write the failing test**

`ReminderLogicTest.kt`（含 license header）：
```kotlin
package cn.wj.android.cashbook.sync.reminder

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate

class ReminderLogicTest {

    private fun card(id: Long, name: String, billing: String, repay: String) =
        CreditCardReminderInfo(id, name, billing, repay)

    @Test
    fun billingDay_match_emitsBilling() {
        val result = computeReminders(
            date = LocalDate.of(2024, 1, 15),
            creditCardEnable = true, reimbursementEnable = false,
            creditCards = listOf(card(1L, "招行", "15", "5")),
            monthStartDay = 1, reimbursableCount = 0,
        )
        assertThat(result).containsExactly(ReminderItem.CreditCardBilling(1L, "招行"))
    }

    @Test
    fun repaymentDay_match_emitsRepayment() {
        val result = computeReminders(
            LocalDate.of(2024, 1, 5), true, false,
            listOf(card(1L, "招行", "15", "5")), 1, 0,
        )
        assertThat(result).containsExactly(ReminderItem.CreditCardRepayment(1L, "招行"))
    }

    @Test
    fun creditCardDisabled_emitsNothing() {
        val result = computeReminders(
            LocalDate.of(2024, 1, 15), false, false,
            listOf(card(1L, "招行", "15", "5")), 1, 0,
        )
        assertThat(result).isEmpty()
    }

    @Test
    fun monthEnd_dayNotExist_skips() {
        // 2 月无 30 号，billingDate="30" 在 2024-02 任何一天都不命中
        val days = (1..29).map { LocalDate.of(2024, 2, it) }
        days.forEach { d ->
            val result = computeReminders(d, true, false, listOf(card(1L, "卡", "30", "30")), 1, 0)
            assertThat(result).isEmpty()
        }
    }

    @Test
    fun dirtyDate_invalidOrOutOfRange_skipsThatCard() {
        val result = computeReminders(
            LocalDate.of(2024, 1, 15), true, false,
            creditCards = listOf(
                card(1L, "脏卡", "abc", ""),   // 非数字
                card(2L, "越界卡", "0", "99"),  // 越界
                card(3L, "正常卡", "15", "5"),  // 命中账单
            ),
            monthStartDay = 1, reimbursableCount = 0,
        )
        assertThat(result).containsExactly(ReminderItem.CreditCardBilling(3L, "正常卡"))
    }

    @Test
    fun multipleCards_sameBillingDay_emitsEach() {
        val result = computeReminders(
            LocalDate.of(2024, 1, 15), true, false,
            listOf(card(1L, "A", "15", "1"), card(2L, "B", "15", "2")), 1, 0,
        )
        assertThat(result).containsExactly(
            ReminderItem.CreditCardBilling(1L, "A"),
            ReminderItem.CreditCardBilling(2L, "B"),
        )
    }

    @Test
    fun reimbursement_onMonthStartDay_withCount_emits() {
        val result = computeReminders(
            LocalDate.of(2024, 1, 15), false, true,
            emptyList(), monthStartDay = 15, reimbursableCount = 3,
        )
        assertThat(result).containsExactly(ReminderItem.Reimbursement(3))
    }

    @Test
    fun reimbursement_zeroCount_emitsNothing() {
        val result = computeReminders(
            LocalDate.of(2024, 1, 15), false, true,
            emptyList(), monthStartDay = 15, reimbursableCount = 0,
        )
        assertThat(result).isEmpty()
    }

    @Test
    fun reimbursement_notMonthStartDay_emitsNothing() {
        val result = computeReminders(
            LocalDate.of(2024, 1, 14), false, true,
            emptyList(), monthStartDay = 15, reimbursableCount = 3,
        )
        assertThat(result).isEmpty()
    }

    @Test
    fun monthStartDay_zeroNormalizedToOne() {
        // monthStartDay=0 视为 1（自然月），1 号命中
        val result = computeReminders(
            LocalDate.of(2024, 1, 1), false, true,
            emptyList(), monthStartDay = 0, reimbursableCount = 2,
        )
        assertThat(result).containsExactly(ReminderItem.Reimbursement(2))
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :sync:work:testDebugUnitTest --tests "*ReminderLogicTest*" --no-configuration-cache`
Expected: FAIL（`computeReminders` / `ReminderItem` unresolved）。

- [ ] **Step 3: 写 ReminderModels.kt**

```kotlin
package cn.wj.android.cashbook.sync.reminder

/** 一条待发送的提醒 */
sealed interface ReminderItem {
    /** 信用卡账单日提醒 */
    data class CreditCardBilling(val assetId: Long, val assetName: String) : ReminderItem
    /** 信用卡还款日提醒 */
    data class CreditCardRepayment(val assetId: Long, val assetName: String) : ReminderItem
    /** 待报销提醒 */
    data class Reimbursement(val count: Int) : ReminderItem
}

/** 信用卡提醒所需信息（从 AssetModel 提取的值类型，便于纯函数测试） */
data class CreditCardReminderInfo(
    val assetId: Long,
    val name: String,
    val billingDate: String,
    val repaymentDate: String,
)
```

- [ ] **Step 4: 写 ReminderLogic.kt 的 computeReminders**

```kotlin
package cn.wj.android.cashbook.sync.reminder

import java.time.LocalDate

/** 月起始日合法化：仅 1..28 有效，其余（含 0/越界）归 1。与 RecordSettings normalizeMonthStartDay 同语义。 */
private fun normalizeStartDay(day: Int): Int = if (day in 1..28) day else 1

/** 解析信用卡日期字符串为 1..31 的合法日；非法/越界返回 null（跳过该卡）。 */
private fun parseCardDay(raw: String): Int? = raw.trim().toIntOrNull()?.takeIf { it in 1..31 }

/**
 * 给定逻辑日期与数据，计算当日应发的提醒列表（纯函数，无 Android/IO 依赖）。
 *
 * - N1：creditCardEnable 时，每张卡 billingDate/repaymentDate 解析为日，命中 date.dayOfMonth 即发；
 *   解析失败/越界跳过该卡（不中断整批）；当月无该日（如 "30" 在 2 月）自然不命中（跳过语义）。
 * - N2：reimbursementEnable 且 date.dayOfMonth == normalizeStartDay(monthStartDay) 且 count>0 时发。
 */
internal fun computeReminders(
    date: LocalDate,
    creditCardEnable: Boolean,
    reimbursementEnable: Boolean,
    creditCards: List<CreditCardReminderInfo>,
    monthStartDay: Int,
    reimbursableCount: Int,
): List<ReminderItem> {
    val items = mutableListOf<ReminderItem>()
    val today = date.dayOfMonth
    if (creditCardEnable) {
        creditCards.forEach { card ->
            parseCardDay(card.billingDate)?.let { if (it == today) items += ReminderItem.CreditCardBilling(card.assetId, card.name) }
            parseCardDay(card.repaymentDate)?.let { if (it == today) items += ReminderItem.CreditCardRepayment(card.assetId, card.name) }
        }
    }
    if (reimbursementEnable && today == normalizeStartDay(monthStartDay) && reimbursableCount > 0) {
        items += ReminderItem.Reimbursement(reimbursableCount)
    }
    return items
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `./gradlew :sync:work:testDebugUnitTest --tests "*ReminderLogicTest*" --no-configuration-cache`
Expected: PASS（10 用例全过）。

- [ ] **Step 6: spotless + Commit**

```bash
./gradlew spotlessApply --init-script gradle/init.gradle.kts --no-configuration-cache
git add sync/work/src/main/.../reminder/ sync/work/src/test/.../reminder/
git commit -m "[feat|提醒|通知][公共]computeReminders 纯函数 + ReminderItem 模型(日期边界/脏值/多卡/N2 单测 10 用例)"
```

### Task 2.2: reminderCheckDates 补发区间纯函数

**Files:**
- Modify: `sync/work/src/main/kotlin/cn/wj/android/cashbook/sync/reminder/ReminderLogic.kt`
- Modify: `sync/work/src/test/kotlin/cn/wj/android/cashbook/sync/reminder/ReminderLogicTest.kt`

**Interfaces:**
- Produces: `internal fun reminderCheckDates(lastCheckMs: Long, todayMs: Long, zoneId: ZoneId, maxBackfillDays: Int = 7): List<LocalDate>`

- [ ] **Step 1: Write the failing test**（追加到 `ReminderLogicTest.kt`）

```kotlin
    private val zone = java.time.ZoneId.of("UTC")
    private fun ms(d: LocalDate) = d.atStartOfDay(zone).toInstant().toEpochMilli()

    @Test
    fun checkDates_firstRun_onlyToday() {
        val today = LocalDate.of(2024, 1, 10)
        val result = reminderCheckDates(0L, ms(today), zone)
        assertThat(result).containsExactly(today)
    }

    @Test
    fun checkDates_yesterday_coversYesterdayAndToday() {
        val today = LocalDate.of(2024, 1, 10)
        val result = reminderCheckDates(ms(LocalDate.of(2024, 1, 9)), ms(today), zone)
        assertThat(result).containsExactly(LocalDate.of(2024, 1, 10)).inOrder()
        // lastCheck=昨天 → 补查从 lastCheck+1=今天 起，即 [今天]
    }

    @Test
    fun checkDates_gap5days_coversGap() {
        val today = LocalDate.of(2024, 1, 10)
        val result = reminderCheckDates(ms(LocalDate.of(2024, 1, 5)), ms(today), zone)
        // lastCheck+1=1/6 .. 1/10
        assertThat(result).containsExactly(
            LocalDate.of(2024, 1, 6), LocalDate.of(2024, 1, 7), LocalDate.of(2024, 1, 8),
            LocalDate.of(2024, 1, 9), LocalDate.of(2024, 1, 10),
        ).inOrder()
    }

    @Test
    fun checkDates_longGap_cappedAt7days() {
        val today = LocalDate.of(2024, 1, 20)
        val result = reminderCheckDates(ms(LocalDate.of(2024, 1, 1)), ms(today), zone)
        // 上限 7 天：[1/14 .. 1/20]
        assertThat(result).hasSize(7)
        assertThat(result.first()).isEqualTo(LocalDate.of(2024, 1, 14))
        assertThat(result.last()).isEqualTo(today)
    }

    @Test
    fun checkDates_alreadyCheckedToday_empty() {
        val today = LocalDate.of(2024, 1, 10)
        val result = reminderCheckDates(ms(today), ms(today), zone)
        assertThat(result).isEmpty()
    }
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :sync:work:testDebugUnitTest --tests "*ReminderLogicTest*" --no-configuration-cache`
Expected: FAIL（`reminderCheckDates` unresolved）。

- [ ] **Step 3: 实现 reminderCheckDates**（追加到 `ReminderLogic.kt`）

```kotlin
import java.time.Instant
import java.time.ZoneId

/**
 * 计算补发应检查的逻辑日期区间 [from..today]。
 * - lastCheckMs<=0（首次）：仅 [today]，不补历史。
 * - 否则 from = max(lastCheckDate+1, today-maxBackfillDays+1)；若 from>today 返回空（今天已查过）。
 * 上限 maxBackfillDays 防设备长期关机后通知轰炸。
 */
internal fun reminderCheckDates(
    lastCheckMs: Long,
    todayMs: Long,
    zoneId: ZoneId,
    maxBackfillDays: Int = 7,
): List<LocalDate> {
    val today = Instant.ofEpochMilli(todayMs).atZone(zoneId).toLocalDate()
    if (lastCheckMs <= 0L) return listOf(today)
    val lastCheck = Instant.ofEpochMilli(lastCheckMs).atZone(zoneId).toLocalDate()
    val earliest = today.minusDays((maxBackfillDays - 1).toLong())
    var from = lastCheck.plusDays(1)
    if (from.isBefore(earliest)) from = earliest
    if (from.isAfter(today)) return emptyList()
    val dates = mutableListOf<LocalDate>()
    var d = from
    while (!d.isAfter(today)) { dates += d; d = d.plusDays(1) }
    return dates
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :sync:work:testDebugUnitTest --tests "*ReminderLogicTest*" --no-configuration-cache`
Expected: PASS（全部 15 用例）。

- [ ] **Step 5: spotless + Commit**

```bash
./gradlew spotlessApply --init-script gradle/init.gradle.kts --no-configuration-cache
git add sync/work/src/main/.../reminder/ReminderLogic.kt sync/work/src/test/.../reminder/ReminderLogicTest.kt
git commit -m "[feat|提醒|通知][公共]reminderCheckDates 补发区间纯函数(首次不补史/补缺/7天上限/已查空,5 用例)"
```

---

## Phase 3 — Worker + 通知 + 调度

### Task 3.1: ReminderNotificationChannel + 通知发送 helper

**Files:**
- Modify: `sync/work/src/main/kotlin/cn/wj/android/cashbook/sync/initializers/SyncWorkHelpers.kt`
- Modify: sync 模块 string 资源（`sync/work/src/main/res/values/strings*.xml`，定位现有 `sync_notification_channel_name` 所在文件）

**Interfaces:**
- Produces: `Context.reminderNotificationBuilder(): NotificationCompat.Builder`；常量 `ReminderNotificationChannelID`、`ReminderNotificationBaseId`。

- [ ] **Step 1: 加渠道常量 + builder**

`SyncWorkHelpers.kt` 末尾追加（仿 `noticeNotificationBuilder` :86-111，但加 `VISIBILITY_PRIVATE`）：
```kotlin
internal const val ReminderNotificationChannelID = "ReminderNotificationChannel"
/** 提醒通知 id 基址；N1 按 assetId 派生，N2 用 ReminderNotificationBaseId */
internal const val ReminderNotificationBaseId = 20016

internal fun Context.reminderNotificationBuilder(): NotificationCompat.Builder {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            ReminderNotificationChannelID,
            getString(R.string.reminder_notification_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = getString(R.string.reminder_notification_channel_description)
            lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        }
        val notificationManager: NotificationManager? =
            getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.createNotificationChannel(channel)
    }
    return NotificationCompat.Builder(this, ReminderNotificationChannelID)
        .setSmallIcon(R.drawable.ic_notification)
        .setWhen(System.currentTimeMillis())
        .setCategory(Notification.CATEGORY_REMINDER)
        .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setAutoCancel(true)
}
```

- [ ] **Step 2: 加 string 资源**

在 sync 模块 strings（含 `sync_notification_channel_name` 的文件）加：
```xml
<string name="reminder_notification_channel_name">提醒</string>
<string name="reminder_notification_channel_description">信用卡账单/还款、待报销提醒</string>
<string name="reminder_credit_billing">%1$s 今日账单已出</string>
<string name="reminder_credit_repayment">%1$s 今日还款，别忘了</string>
<string name="reminder_reimbursement">你有 %1$d 笔支出待报销</string>
<string name="reminder_public_title">您有一条记账提醒</string>
```

- [ ] **Step 3: 编译验证**

Run: `./gradlew :sync:work:compileDebugKotlin --no-configuration-cache`
Expected: BUILD SUCCESSFUL。

- [ ] **Step 4: spotless + Commit**

```bash
git add sync/work/src/main/.../SyncWorkHelpers.kt sync/work/src/main/res/
git commit -m "[feat|提醒|通知][公共]ReminderNotificationChannel(VISIBILITY_PRIVATE)+builder+string 资源"
```

### Task 3.2: DailyReminderWorker（薄壳：取数据→纯函数→发通知）

**Files:**
- Create: `sync/work/src/main/kotlin/cn/wj/android/cashbook/sync/workers/DailyReminderWorker.kt`
- Modify: `sync/work/src/main/kotlin/cn/wj/android/cashbook/sync/initializers/SyncInitializer.kt`（加 `ReminderWorkName`）

**Interfaces:**
- Consumes: `computeReminders`、`reminderCheckDates`、`CreditCardReminderInfo`、`ReminderItem`（Task 2）；`reminderNotificationBuilder`、`ReminderNotificationChannelID`、`ReminderNotificationBaseId`（Task 3.1）；`SettingRepository`(appSettingsModel/recordSettingsModel + updateLastReminderCheckMs)、`AssetRepository.currentVisibleAssetListData`、`RecordRepository.getReimbursableUnrelatedRecordList()`。
- Produces: `DailyReminderWorker.startUpPeriodicReminderWork(): PeriodicWorkRequest`；`ReminderWorkName` 常量。

- [ ] **Step 1: 加 ReminderWorkName 常量**

`SyncInitializer.kt` 末尾常量区加：
```kotlin
internal const val ReminderWorkName = "ReminderWorkName"
```

- [ ] **Step 2: 写 DailyReminderWorker**

注入 `SettingRepository`、`AssetRepository`、`RecordRepository`、ioDispatcher。`doWork()`：
```kotlin
@HiltWorker
class DailyReminderWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val settingRepository: SettingRepository,
    private val assetRepository: AssetRepository,
    private val recordRepository: RecordRepository,
    @Dispatcher(CashbookDispatchers.IO) private val ioDispatcher: CoroutineContext,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(ioDispatcher) {
        val settings = settingRepository.appSettingsModel.first()
        if (!settings.creditCardReminderEnable && !settings.reimbursementReminderEnable) {
            return@withContext Result.success()  // 两开关全关，空转
        }
        val zone = ZoneId.systemDefault()
        val todayMs = System.currentTimeMillis()
        val dates = reminderCheckDates(settings.lastReminderCheckMs, todayMs, zone)
        if (dates.isEmpty()) return@withContext Result.success()

        val monthStartDay = settingRepository.recordSettingsModel.first().monthStartDay
        val creditCards = if (settings.creditCardReminderEnable) {
            assetRepository.currentVisibleAssetListData.first()
                .filter { it.classification.isCreditCard }
                .map { CreditCardReminderInfo(it.id, it.name, it.billingDate, it.repaymentDate) }
        } else emptyList()
        val reimbursableCount = if (settings.reimbursementReminderEnable) {
            recordRepository.getReimbursableUnrelatedRecordList().size
        } else 0

        dates.forEach { date ->
            computeReminders(
                date, settings.creditCardReminderEnable, settings.reimbursementReminderEnable,
                creditCards, monthStartDay, reimbursableCount,
            ).forEach { item -> notify(item) }
        }
        settingRepository.updateLastReminderCheckMs(
            LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli(),
        )
        Result.success()
    }

    private fun notify(item: ReminderItem) {
        val nm = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val (id, contentIntent, text) = when (item) {
            is ReminderItem.CreditCardBilling -> Triple(
                ReminderNotificationBaseId + item.assetId.toInt() * 2,
                reminderDeepLinkIntent(appContext, REMINDER_TARGET_ASSET, item.assetId),
                appContext.getString(R.string.reminder_credit_billing, item.assetName),
            )
            is ReminderItem.CreditCardRepayment -> Triple(
                ReminderNotificationBaseId + item.assetId.toInt() * 2 + 1,
                reminderDeepLinkIntent(appContext, REMINDER_TARGET_ASSET, item.assetId),
                appContext.getString(R.string.reminder_credit_repayment, item.assetName),
            )
            is ReminderItem.Reimbursement -> Triple(
                ReminderNotificationBaseId,
                reminderDeepLinkIntent(appContext, REMINDER_TARGET_REIMBURSEMENT, -1L),
                appContext.getString(R.string.reminder_reimbursement, item.count),
            )
        }
        val notification = appContext.reminderNotificationBuilder()
            .setContentText(text)
            .setContentIntent(contentIntent)
            .setPublicVersion(
                appContext.reminderNotificationBuilder()
                    .setContentText(appContext.getString(R.string.reminder_public_title)).build(),
            )
            .build()
        nm.notify(id, notification)
    }

    companion object {
        fun startUpPeriodicReminderWork(): PeriodicWorkRequest =
            PeriodicWorkRequestBuilder<DelegatingWorker>(Duration.ofDays(1))
                .setInitialDelay(initialDelayToNext(hour = 10), TimeUnit.MILLISECONDS)
                .setInputData(DailyReminderWorker::class.delegatedData())
                .build()
    }
}
```
> `reminderDeepLinkIntent(...)`、`REMINDER_TARGET_ASSET`、`REMINDER_TARGET_REIMBURSEMENT`、`initialDelayToNext(hour)` 在 Task 3.3 / Phase 4 定义；本 Task 实现 `initialDelayToNext` 为私有顶层函数（见下 Step 3）。
> **依赖确认**：`sync/work/build.gradle.kts` 已含 `core:data`（AssetRepository/RecordRepository/SettingRepository 均在 core:data）；`DelegatingWorker`/`delegatedData()` 见同包 `AutoBackupWorker.kt:74`。

- [ ] **Step 3: 实现 initialDelayToNext 顶层私有函数**（同文件）

```kotlin
/** 计算从现在到下一个指定小时(本地时区)的毫秒延迟 */
internal fun initialDelayToNext(
    hour: Int,
    nowMs: Long = System.currentTimeMillis(),
    zone: ZoneId = ZoneId.systemDefault(),
): Long {
    val now = Instant.ofEpochMilli(nowMs).atZone(zone)
    var target = now.toLocalDate().atTime(hour, 0).atZone(zone)
    if (!target.isAfter(now)) target = target.plusDays(1)
    return target.toInstant().toEpochMilli() - nowMs
}
```

- [ ] **Step 4: 为 initialDelayToNext 补纯 JVM 单测**

`sync/work/src/test/.../workers/ReminderScheduleTest.kt`：
```kotlin
@Test
fun initialDelay_beforeTargetHour_sameDay() {
    val zone = ZoneId.of("UTC")
    val now = LocalDate.of(2024, 1, 10).atTime(8, 0).atZone(zone).toInstant().toEpochMilli()
    val delay = initialDelayToNext(10, now, zone)
    assertThat(delay).isEqualTo(Duration.ofHours(2).toMillis())
}

@Test
fun initialDelay_afterTargetHour_nextDay() {
    val zone = ZoneId.of("UTC")
    val now = LocalDate.of(2024, 1, 10).atTime(11, 0).atZone(zone).toInstant().toEpochMilli()
    val delay = initialDelayToNext(10, now, zone)
    assertThat(delay).isEqualTo(Duration.ofHours(23).toMillis())
}
```

- [ ] **Step 5: 编译 + 跑 schedule 测试**

Run: `./gradlew :sync:work:testDebugUnitTest --tests "*ReminderScheduleTest*" --no-configuration-cache`
Expected: PASS。（Worker 主体 doWork 不单测——薄壳依赖 Android NotificationManager，逻辑覆盖在纯函数；本步只验 initialDelay。）
> Phase 4 未完成前 `reminderDeepLinkIntent`/常量 unresolved，本 Task 编译可能需先桩出占位（或先做 Phase 4 的 Task 4.0 常量+helper）。**执行顺序建议：先做 Phase 4 Task 4.1（深链 helper + 常量），再回 Task 3.2 引用。** 见 Phase 4。

- [ ] **Step 6: spotless + Commit**（与 Phase 4 深链 helper 合并提交，见 Phase 4 收尾）

### Task 3.3: InitWorker 注册周期任务 + 启动补查

**Files:**
- Modify: `sync/work/src/main/kotlin/cn/wj/android/cashbook/sync/workers/InitWorker.kt`

**Interfaces:**
- Consumes: `DailyReminderWorker.startUpPeriodicReminderWork()`、`ReminderWorkName`。

- [ ] **Step 1: InitWorker.doWork 内追加注册（不 gate，始终注册）**

在现有 `WorkManager.getInstance(appContext).apply { ... }` 块内、自动备份 when 之后追加：
```kotlin
                // 提醒：始终注册周期任务（Worker 内读开关 gate），UPDATE 重算 initialDelay 对齐次日 10:00
                enqueueUniquePeriodicWork(
                    ReminderWorkName,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    DailyReminderWorker.startUpPeriodicReminderWork(),
                )
                // 启动补查一次（消"启动时补发遗漏"）
                enqueueUniqueWork(
                    "${ReminderWorkName}_OneTime",
                    ExistingWorkPolicy.REPLACE,
                    OneTimeWorkRequestBuilder<DelegatingWorker>()
                        .setInputData(DailyReminderWorker::class.delegatedData())
                        .build(),
                )
```
import 补 `ReminderWorkName`。

- [ ] **Step 2: 编译验证**

Run: `./gradlew :sync:work:compileDebugKotlin --no-configuration-cache`
Expected: BUILD SUCCESSFUL。

- [ ] **Step 3: Commit**

```bash
git add sync/work/src/main/.../InitWorker.kt
git commit -m "[feat|提醒|通知][公共]InitWorker 注册 DailyReminderWorker 周期任务(UPDATE)+启动补查"
```

---

## Phase 4 — 深链导航（独立交付项，禁绕安全门）

> **执行顺序**：本 Phase 的 Task 4.1（深链常量 + helper）是 Task 3.2 的依赖，**应在 Task 3.2 之前或并入**。这里独立成 Phase 便于评审深链的安全约束。

### Task 4.1: 深链常量 + reminderDeepLinkIntent helper

**Files:**
- Modify: `core/common/.../`（定位现有 `SHORTCUTS_TYPE_ADD`/`SHORTCUTS_TYPE_ASSET` 所在常量文件，追加提醒目标常量）
- Modify: `sync/work/.../workers/DailyReminderWorker.kt`（或同包新建 `ReminderDeepLink.kt`）

**Interfaces:**
- Produces: 常量 `REMINDER_TARGET_ASSET`/`REMINDER_TARGET_REIMBURSEMENT`、extra key `EXTRA_REMINDER_TARGET`/`EXTRA_REMINDER_ASSET_ID`；`reminderDeepLinkIntent(context, target, assetId): PendingIntent`。

- [ ] **Step 1: 加常量**（在 `SHORTCUTS_TYPE_*` 同文件，复用现有模式）

```kotlin
const val EXTRA_REMINDER_TARGET = "extra_reminder_target"
const val EXTRA_REMINDER_ASSET_ID = "extra_reminder_asset_id"
const val REMINDER_TARGET_NONE = 0
const val REMINDER_TARGET_ASSET = 1
const val REMINDER_TARGET_REIMBURSEMENT = 2
```

- [ ] **Step 2: 实现 reminderDeepLinkIntent（FLAG_IMMUTABLE + 显式 Intent）**

`sync/work` 新建 `ReminderDeepLink.kt`（显式组件指向 app 的 MainActivity——sync 模块无法直接引用 app 的 MainActivity 类，用 `Intent().setClassName(packageName, "cn.wj.android.cashbook.MainActivity")` 显式指定，类名以实际为准，executing 时核对 `MainActivity` 全限定名）：
```kotlin
internal fun reminderDeepLinkIntent(context: Context, target: Int, assetId: Long): PendingIntent {
    val intent = Intent().apply {
        setClassName(context.packageName, "cn.wj.android.cashbook.MainActivity")
        putExtra(EXTRA_REMINDER_TARGET, target)
        putExtra(EXTRA_REMINDER_ASSET_ID, assetId)
        addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
    }
    return PendingIntent.getActivity(
        context,
        target * 100000 + assetId.toInt(),  // requestCode 区分
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}
```
> **核对**：`MainActivity` 全限定类名以 `app/.../MainActivity.kt` 的 package 为准；若 sync 模块依赖不可见 app 的 applicationId，用 `context.packageName`。

- [ ] **Step 3: 编译 sync:work（连同 Task 3.2 的 DailyReminderWorker）**

Run: `./gradlew :sync:work:compileDebugKotlin :sync:work:testDebugUnitTest --tests "*Reminder*" --no-configuration-cache`
Expected: BUILD SUCCESSFUL + 纯函数测试 PASS。

- [ ] **Step 4: spotless + Commit（合并 Task 3.2 + 3.1 通知 + 4.1）**

```bash
./gradlew spotlessApply --init-script gradle/init.gradle.kts --no-configuration-cache
git add sync/work/src/ core/common/src/
git commit -m "[feat|提醒|通知][公共]DailyReminderWorker 薄壳+深链 PendingIntent(FLAG_IMMUTABLE+显式 Intent)+提醒目标常量"
```

### Task 4.2: MainActivity 解析 extra + MainApp 门控导航

**Files:**
- Modify: `app/src/main/kotlin/cn/wj/android/cashbook/MainActivity.kt`
- Modify: `app/src/main/kotlin/cn/wj/android/cashbook/ui/MainApp.kt`

**Interfaces:**
- Consumes: `EXTRA_REMINDER_TARGET`/`EXTRA_REMINDER_ASSET_ID`/`REMINDER_TARGET_*`（Task 4.1）；现有 `shortcutsType` 经 MainApp 的处理模式（`MainApp.kt:153/227-248`）。

- [ ] **Step 1: 阅读现有 shortcutsType 链路**

先读 `MainActivity.kt`（看 `SHORTCUTS_TYPE` 如何从 Intent 读 + 传给 MainApp）+ `MainApp.kt:153,227-248`（`shortcutsType` 参数 + 受 `!needRequestProtocol && !needVerity` 门控的 `LaunchedEffect` + `SHORTCUTS_TYPE_ADD/ASSET` 导航分支）。**复用同模式**：把 reminder target/assetId 当作和 shortcutsType 同级的"启动意图"，经同一受门控的 LaunchedEffect 消费。

- [ ] **Step 2: MainActivity 读 extra 并经 onCreate/onNewIntent 传入**

仿现有 `SHORTCUTS_TYPE` 读取，加 reminder target/assetId 的读取（`intent.getIntExtra(EXTRA_REMINDER_TARGET, REMINDER_TARGET_NONE)` + `getLongExtra(EXTRA_REMINDER_ASSET_ID, -1L)`）；singleTask 热启需 override `onNewIntent` 更新 state。具体写法对齐现有 shortcutsType 实现（executing 时按 MainActivity 实际结构落地）。

- [ ] **Step 3: MainApp 门控 LaunchedEffect 消费 reminder 深链**

在 `MainApp.kt:227-248` 现有受 `if (!needRequestProtocol && !needVerity)` 门控的 `LaunchedEffect` 内（或并列一个同样门控的 LaunchedEffect），加：
```kotlin
when (reminderTarget) {
    REMINDER_TARGET_ASSET -> if (reminderAssetId > 0) { /* 导航到资产详情，复用 SHORTCUTS_TYPE_ASSET 的导航调用 */ }
    REMINDER_TARGET_REIMBURSEMENT -> { /* 导航到待报销列表，复用现有 reimbursement 导航入口 */ }
}
```
> **关键**：必须在 `!needVerity` 门控内，验证未通过时不导航（安全门，spec finding#3）。导航目标复用现有路由（资产详情见 `SHORTCUTS_TYPE_ASSET` 分支；待报销列表见抽屉 `onReimbursementClick` 导航的 route），executing 时定位确切 navController 调用。

- [ ] **Step 4: 编译验证（app Online flavor，含跨模块 Hilt 图）**

Run: `./gradlew :app:compileOnlineDebugKotlin --no-configuration-cache`
Expected: BUILD SUCCESSFUL。

- [ ] **Step 5: 冷启/热启手测（模拟器，Phase 6 统一跑，此处记录步骤）**

冷启：kill app → adb 发带 extra 的 Intent → 验导航到资产详情/待报销；热启：app 前台 → 发 Intent → onNewIntent → 验导航；**开启启动安全验证后**：先弹密码/指纹门，验证通过后才落地目标屏。

- [ ] **Step 6: spotless + Commit**

```bash
git add app/src/main/.../MainActivity.kt app/src/main/.../ui/MainApp.kt
git commit -m "[feat|提醒|通知][公共]通知深链经 MainActivity extra+MainApp 受 needVerity 门控 LaunchedEffect 导航"
```

---

## Phase 5 — 设置 UI + 权限 + 引导

### Task 5.1: SettingViewModel 暴露开关状态 + 更新方法

**Files:**
- Modify: `feature/settings/.../viewmodel/SettingViewModel.kt`
- Modify: `feature/settings/.../screen/SettingScreen.kt`
- Test: `feature/settings/.../viewmodel/SettingViewModelTest.kt`（若已存在则追加）

**Interfaces:**
- Consumes: `SettingRepository.appSettingsModel` + `updateCreditCardReminderEnable`/`updateReimbursementReminderEnable`。
- Produces: ViewModel 暴露两开关状态 + `onCreditCardReminderChange(Boolean)`/`onReimbursementReminderChange(Boolean)`。

- [ ] **Step 1: 阅读现有设置项模式**

读 `SettingViewModel.kt` + `SettingScreen.kt` 中**月起始日**或某个 bool 开关项（如 dynamicColor/canary）的现有写法（状态从 `appSettingsModel` 派生 + 更新方法委托 repository），作为本 Task 模板。

- [ ] **Step 2: Write failing test（ViewModel 开关委托）**

`SettingViewModelTest.kt`（用 `FakeSettingRepository`）：
```kotlin
@Test
fun onCreditCardReminderChange_updatesRepository() = runTest {
    val repo = FakeSettingRepository()
    val vm = SettingViewModel(repo, /* 其他依赖按现有构造 */)
    vm.onCreditCardReminderChange(true)
    assertThat(repo.appSettingsModel.first().creditCardReminderEnable).isTrue()
}
```

- [ ] **Step 3: Run test → FAIL**（方法未定义）

Run: `./gradlew :feature:settings:testDebugUnitTest --tests "*SettingViewModelTest*" --no-configuration-cache`

- [ ] **Step 4: 实现 ViewModel 方法**（仿现有开关委托）

```kotlin
fun onCreditCardReminderChange(enable: Boolean) {
    viewModelScope.launch { settingRepository.updateCreditCardReminderEnable(enable) }
}
fun onReimbursementReminderChange(enable: Boolean) {
    viewModelScope.launch { settingRepository.updateReimbursementReminderEnable(enable) }
}
```
状态：在现有 uiState（从 appSettingsModel 派生）加 `creditCardReminderEnable`/`reimbursementReminderEnable` 两字段。

- [ ] **Step 5: Run test → PASS**

- [ ] **Step 6: SettingScreen 加「提醒」分组 UI**

在设置页加分组（仿现有分组 + Cb 封装开关组件），两个 `CbListItem` + 开关；待报销开关下方加说明文案「待报销提醒在每月记账起始日发出」（对齐 monthStartDay 耦合，spec finding#13）。具体 Composable 写法对齐现有设置项（executing 时按 SettingScreen 实际结构落地，用 Cb* 封装禁裸 Material3）。

- [ ] **Step 7: 编译 + 测试 + 截图基线**

Run: `./gradlew :feature:settings:testDebugUnitTest :feature:settings:recordRoborazziDebug --no-configuration-cache`
然后 `verifyRoborazziDebug` 0 diff；新「提醒」分组截图入库。
> record 前 grep `\.now()` 排查时间脆弱性（本分组无日期，应无）。

- [ ] **Step 8: spotless + Commit**

```bash
git add feature/settings/src/
git commit -m "[feat|提醒|通知][公共]设置页「提醒」分组2开关+ViewModel委托+monthStartDay耦合说明+截图基线"
```

### Task 5.2: POST_NOTIFICATIONS 权限请求（首次开启时）

**Files:**
- Modify: `feature/settings/.../screen/SettingScreen.kt`

- [ ] **Step 1: 开关开启时请求权限**

用 `rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission())`，在开关从关→开时，若 Android 13+ 且未授权则请求 `Manifest.permission.POST_NOTIFICATIONS`；拒绝不阻断（开关仍置开，记录意图）。`stringResource` 在 Composable 顶层取。
> 权限已在 `sync/work/AndroidManifest.xml:19` 声明，merge 进 app；此处仅运行时请求。

- [ ] **Step 2: 编译验证**

Run: `./gradlew :app:compileOnlineDebugKotlin --no-configuration-cache`
Expected: BUILD SUCCESSFUL。

- [ ] **Step 3: Commit**

```bash
git add feature/settings/src/
git commit -m "[feat|提醒|通知][公共]首次开启提醒开关请求 POST_NOTIFICATIONS(13+，拒绝不阻断)"
```

### Task 5.3: 轻量一次性引导

**Files:**
- Modify: 资产页 / 待报销页对应 screen（持卡 / 有待报销时一次性提示）
- Modify: 引导已展示标记存储（DataStore，新键或复用 tempKeys）

- [ ] **Step 1: 决定引导标记存储**

在 `temp_keys.proto`/`TempKeysModel` 加 `reminderGuideShown: Boolean`（仿 `db9To10DataMigrated` 模式，含 proto + model + CombineProtoDataSource 映射 + 两 Fake），或用更轻的方案（executing 时定）。

- [ ] **Step 2: 资产页/待报销页一次性提示**

持有信用卡资产（资产页）或有待报销记录（待报销页）且 `!reminderGuideShown` 时，显示一次性轻量提示（Snackbar/Banner），点击跳设置「提醒」分组，展示后置 `reminderGuideShown=true`。具体 UI 对齐各页现有模式。

- [ ] **Step 3: 编译 + 相关测试**

Run: `./gradlew :app:compileOnlineDebugKotlin --no-configuration-cache`

- [ ] **Step 4: spotless + Commit**

```bash
git commit -m "[feat|提醒|通知][公共]持卡/有待报销用户轻量一次性引导(reminderGuideShown 标记)"
```

---

## Phase 6 — 验收

### Task 6.1: 全量编译 + 单测 + 静态检查

- [ ] **Step 1: 跨模块编译（含 app Hilt 全图）**

Run: `./gradlew :app:compileOnlineDebugKotlin --no-configuration-cache`
Expected: BUILD SUCCESSFUL。

- [ ] **Step 2: 相关模块单测**

Run: `./gradlew :sync:work:testDebugUnitTest :core:data:testDebugUnitTest :feature:settings:testDebugUnitTest --no-configuration-cache`
Expected: 全 PASS（ReminderLogicTest 15 + ReminderScheduleTest 2 + SettingViewModelTest + 既有）。

- [ ] **Step 3: spotless + lint**

Run: `./gradlew spotlessCheck --init-script gradle/init.gradle.kts --no-configuration-cache` + `./gradlew :feature:settings:lintRelease --no-configuration-cache`
Expected: 无 `Design` 违规、license header 全过。

### Task 6.2: 节点 2 full-review

- [ ] **Step 1:** 调用 `comprehensive-review:full-review` 对本次 git diff 做最终审查（架构+安全+性能+测试+best-practices）；Critical/High 必修或经用户授权放行。

### Task 6.3: 模拟器 journey 黑盒（有设备时）

- [ ] **Step 1:** android-cli 启 Medium_Phone；开启两开关 → 改某信用卡 billingDate 为今日 → 触发 Worker（`adb shell cmd jobscheduler run` 或重启 app 触发启动补查）→ 验证账单通知弹出 + 点击经安全门落地资产详情；待报销同理（改 monthStartDay 为今日 + 造待报销记录）。
- [ ] **Step 2:** 验证锁屏通知为脱敏 publicVersion（不显卡名/笔数）。
- [ ] **Step 3:** 记录报告 `docs/testing/reports/2026-06-26-reminder-journey.md`。

---

## Self-Review

**1. Spec 覆盖：**
- §2 决策表（N1 当天/N2 每月D/当前账本可见卡/默认关/补发/引导）→ Task 2.1（判定）+ 2.2（补发）+ 3.2（数据源 currentVisibleAssetListData）+ 5.1（开关）+ 5.3（引导）✓
- §3 架构（4 构造站点/深链交付项/引导）→ Task 1.1/1.2（含 FakeCombineProtoDataSource）+ Phase 4 + 5.3 ✓
- §4 数据模型（26/27/28 + Kotlin 默认值）→ Task 1.1 ✓
- §5 调度（InitWorker UPDATE 注册 + DelegatingWorker + 补发 + initialDelay + per-card 校验）→ Task 2.1/2.2/3.2/3.3 ✓
- §6 通知（独立渠道 VISIBILITY_PRIVATE + publicVersion + FLAG_IMMUTABLE 显式 Intent + group/文案实参）→ Task 3.1/3.2/4.1 ✓（**group/summary 折叠：当前用 per-item notify + 各自 id，多卡同日多条；summary 折叠 spec §6 列为优化，未单独建 task → 见下 gap**）
- §7 设置 UI + monthStartDay 耦合说明 + 权限 + 引导 → Task 5.1/5.2/5.3 ✓
- §8 测试 → Task 2.1/2.2/3.2(schedule)/5.1 + Phase 6 ✓
- §9 已知限制（仅当前账本可见卡）→ Task 3.2 数据源即体现 ✓

**Gap 修正：** §6 通知 group/summary 折叠未单独成 task。**决策**：notification group/summary 为体验优化（spec finding#14 标 Low），并入 Task 3.2 的 `notify` —— 若需折叠，在 `reminderNotificationBuilder` 加 `setGroup("reminder")` 并发一条 summary。**为避免 Task 3.2 过载，此项标为 Task 3.2 的可选增强**：基础实现为多条独立通知（各自 id 防覆盖，已满足正确性）；group/summary 折叠若 executing 时间允许则加，否则记 backlog（不影响功能正确性，仅体验）。已在此显式声明，非静默裁剪。

**2. Placeholder 扫描：** Phase 4/5 的 UI/MainActivity 步骤标注"对齐现有 <file:line> 模式，executing 时按实际结构落地"——这是带精确范例的指引（非空泛占位），因 Compose/Activity 确切代码须读现有文件确定，凭空写违反事实优先。核心逻辑（Phase 1-2-3 数据层/纯函数/Worker/通知/调度）均完整代码。

**3. 类型一致性：** `computeReminders` 签名（Task 2.1 定义）与 Task 3.2 调用一致；`reminderCheckDates`（2.2）与 3.2 一致；`ReminderItem` 三子类与 notify when 分支一致；`CreditCardReminderInfo(assetId,name,billingDate,repaymentDate)` 与 AssetModel 字段映射一致；常量 `REMINDER_TARGET_*`/`EXTRA_*`（4.1）与 4.2 消费一致；`ReminderNotificationBaseId`/`ReminderNotificationChannelID`（3.1）与 3.2 一致。

---

> **执行顺序提示**：Phase 1 → Phase 2 → **Phase 4 Task 4.1（深链常量/helper）** → Phase 3（Worker 引用 4.1）→ Phase 4 Task 4.2 → Phase 5 → Phase 6。Task 4.1 提前是因 Task 3.2 的 `notify` 依赖 `reminderDeepLinkIntent`。
