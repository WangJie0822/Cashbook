# 提醒通知收尾 backlog 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 修复提醒/快捷方式深链「消费后不复位致弹回」bug（统一一次性消费），并把 `DailyReminderWorker.doWork` 编排逻辑抽为纯函数补齐测试。

**Architecture:** Item 2 先行（行为保持重构）：把 doWork 编排决策抽成 `reminderRun` + `ReminderItem.toNotificationSpec()` 两个纯函数（仿同模块 `AutoBackupWorker.mapBackupStateToResult` 先例），doWork 退化为 I/O 薄壳，纯 JVM 测全分支。Item 1 后行：把 `MainActivity` 的 3 个深链参数合并为单一 `PendingDeepLink` sealed 态 + `parsePendingDeepLink` 纯函数，`MainApp` 两个 `LaunchedEffect` 合并为一个，导航后经 `onConsumePendingDeepLink` 回调复位态 + 清 intent extra。

**Tech Stack:** Kotlin、JUnit4 + Google Truth、AndroidX WorkManager、Jetpack Compose Navigation、Hilt。

## Global Constraints

- 金额/语义/判定逻辑零改动：`computeReminders` / `reminderCheckDates` / `reminderNotificationId` 不动；Item 2 严格**行为保持**。
- 不改 Proto / Room schema / 跨模块接口。
- 源文件需 Apache 2.0 License Header（复制现有文件头）。
- ktlint(android mode) + spotless：提交前 `spotlessApply`；KDoc 禁用含 `..` 的方括号表达。
- `sync:work` 是 Android 库模块，测试任务 `:sync:work:testDebugUnitTest`；`app` 有 flavor，编译用 `:app:compileOnlineDebugKotlin`、测试用 `:app:testOnlineDebugUnitTest`。
- 纯函数优先 top-level `internal fun`（同模块 test 源集可直接调用）。
- 包名：`cn.wj.android.cashbook`。

---

## 涉及文件总览

| 文件 | 责任 | 动作 |
|---|---|---|
| `sync/work/src/main/.../reminder/ReminderLogic.kt` | 提醒派生逻辑 | 加 `ReminderRun` + `reminderRun` + `toNotificationSpec` |
| `sync/work/src/main/.../reminder/ReminderModels.kt` | 提醒值类型 | 加 `NotificationSpec` |
| `sync/work/src/main/.../workers/DailyReminderWorker.kt` | 每日提醒薄壳 | `doWork`/`notify` 改用纯函数 |
| `sync/work/src/test/.../reminder/ReminderRunTest.kt` | reminderRun 测试 | 新建 |
| `sync/work/src/test/.../reminder/NotificationSpecTest.kt` | toNotificationSpec 测试 | 新建 |
| `app/src/main/.../ui/PendingDeepLink.kt` | 深链意图模型 + 解析 | 新建 |
| `app/src/main/.../ui/MainActivity.kt` | Activity 入口 | 合并深链态 + consume 回调 |
| `app/src/main/.../ui/MainApp.kt` | 应用 Compose 入口 | 合并 LaunchedEffect + 消费 |
| `app/src/test/.../ui/PendingDeepLinkTest.kt` | parsePendingDeepLink 测试 | 新建 |

---

# Item 2：doWork 编排纯函数化（行为保持）

### Task 1: `reminderRun` 编排纯函数

**Files:**
- Modify: `sync/work/src/main/kotlin/cn/wj/android/cashbook/sync/reminder/ReminderLogic.kt`
- Test: `sync/work/src/test/kotlin/cn/wj/android/cashbook/sync/reminder/ReminderRunTest.kt`

**Interfaces:**
- Consumes: `computeReminders(date, creditCardEnable, reimbursementEnable, creditCards, monthStartDay, reimbursableCount): List<ReminderItem>`、`reminderCheckDates(lastCheckMs, todayMs, zoneId, maxBackfillDays=7): List<LocalDate>`、`CreditCardReminderInfo(assetId, name, billingDate, repaymentDate)`、`ReminderItem`（均同包，已存在）。
- Produces: `data class ReminderRun(val items: List<ReminderItem>, val newLastCheckMs: Long?)`；`internal fun reminderRun(lastReminderCheckMs: Long, todayMs: Long, zone: ZoneId, creditCardEnable: Boolean, reimbursementEnable: Boolean, creditCards: List<CreditCardReminderInfo>, monthStartDay: Int, reimbursableCount: Int): ReminderRun`。

- [ ] **Step 1: 写失败测试**

新建 `ReminderRunTest.kt`（复制 `ReminderLogicTest.kt` 的 License Header + package `cn.wj.android.cashbook.sync.reminder`）：

```kotlin
package cn.wj.android.cashbook.sync.reminder

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/** [reminderRun] 编排纯函数单测（纯 JVM）。 */
class ReminderRunTest {

    private val zone: ZoneId = ZoneId.of("Asia/Shanghai")

    private fun startOfDayMs(year: Int, month: Int, day: Int): Long =
        LocalDate.of(year, month, day).atStartOfDay(zone).toInstant().toEpochMilli()

    /** 当日 10:00 的毫秒（模拟 todayMs）。 */
    private fun atTenMs(year: Int, month: Int, day: Int): Long =
        LocalDate.of(year, month, day).atTime(10, 0).atZone(zone).toInstant().toEpochMilli()

    private fun card(billing: String, repay: String) =
        CreditCardReminderInfo(1L, "招行", billing, repay)

    @Test
    fun bothDisabled_returnsEmptyAndNoCheckpoint() {
        val run = reminderRun(
            lastReminderCheckMs = 0L,
            todayMs = atTenMs(2024, 1, 15),
            zone = zone,
            creditCardEnable = false,
            reimbursementEnable = false,
            creditCards = listOf(card("15", "5")),
            monthStartDay = 1,
            reimbursableCount = 3,
        )
        assertThat(run.items).isEmpty()
        assertThat(run.newLastCheckMs).isNull()
    }

    @Test
    fun datesEmpty_returnsEmptyAndNoCheckpoint() {
        // lastCheck == 今天 → reminderCheckDates 返回空
        val run = reminderRun(
            lastReminderCheckMs = startOfDayMs(2024, 1, 15),
            todayMs = atTenMs(2024, 1, 15),
            zone = zone,
            creditCardEnable = true,
            reimbursementEnable = false,
            creditCards = listOf(card("15", "5")),
            monthStartDay = 1,
            reimbursableCount = 0,
        )
        assertThat(run.items).isEmpty()
        assertThat(run.newLastCheckMs).isNull()
    }

    @Test
    fun firstRun_billingMatch_emitsItemAndAdvancesCheckpoint() {
        val run = reminderRun(
            lastReminderCheckMs = 0L, // 首次 → dates = [today]
            todayMs = atTenMs(2024, 1, 15),
            zone = zone,
            creditCardEnable = true,
            reimbursementEnable = false,
            creditCards = listOf(card("15", "5")),
            monthStartDay = 1,
            reimbursableCount = 0,
        )
        assertThat(run.items).containsExactly(ReminderItem.CreditCardBilling(1L, "招行"))
        assertThat(run.newLastCheckMs).isEqualTo(startOfDayMs(2024, 1, 15))
    }

    @Test
    fun backfill_multiDay_onlyMatchingDayEmits() {
        // lastCheck = Jan10 → dates = [Jan11, Jan12, Jan13]; billing "12" 仅 Jan12 命中
        val run = reminderRun(
            lastReminderCheckMs = startOfDayMs(2024, 1, 10),
            todayMs = atTenMs(2024, 1, 13),
            zone = zone,
            creditCardEnable = true,
            reimbursementEnable = false,
            creditCards = listOf(card("12", "28")),
            monthStartDay = 1,
            reimbursableCount = 0,
        )
        assertThat(run.items).containsExactly(ReminderItem.CreditCardBilling(1L, "招行"))
        assertThat(run.newLastCheckMs).isEqualTo(startOfDayMs(2024, 1, 13))
    }

    @Test
    fun datesNonEmpty_noMatch_advancesCheckpointWithEmptyItems() {
        val run = reminderRun(
            lastReminderCheckMs = 0L, // dates = [today=Jan15]
            todayMs = atTenMs(2024, 1, 15),
            zone = zone,
            creditCardEnable = true,
            reimbursementEnable = false,
            creditCards = listOf(card("20", "25")), // 当日无命中
            monthStartDay = 1,
            reimbursableCount = 0,
        )
        assertThat(run.items).isEmpty()
        assertThat(run.newLastCheckMs).isEqualTo(startOfDayMs(2024, 1, 15))
    }
}
```

- [ ] **Step 2: 跑测试确认失败**

Run: `./gradlew :sync:work:testDebugUnitTest --tests "*ReminderRunTest*" --offline --no-daemon --console=plain`
Expected: 编译失败（`reminderRun` / `ReminderRun` 未定义）。

- [ ] **Step 3: 实现 `reminderRun`**

在 `ReminderLogic.kt` 末尾（`reminderNotificationId` 之后）追加：

```kotlin
/**
 * 一次提醒检查的编排结果。
 *
 * @param items 待投递通知（按日期+顺序）
 * @param newLastCheckMs 需持久化的 checkpoint（毫秒），null=不推进
 */
internal data class ReminderRun(
    val items: List<ReminderItem>,
    val newLastCheckMs: Long?,
)

/**
 * 编排一次提醒检查（纯函数）：算补发区间 → 逐日 [computeReminders] 累积 → 决定 checkpoint。
 *
 * - 两开关全关 或 补发区间为空 → 空 items、不推进 checkpoint（newLastCheckMs=null）。
 * - 否则累积区间内全部提醒，checkpoint 推进到 [todayMs] 当日 0 点（即便当日无提醒也推进）。
 */
internal fun reminderRun(
    lastReminderCheckMs: Long,
    todayMs: Long,
    zone: ZoneId,
    creditCardEnable: Boolean,
    reimbursementEnable: Boolean,
    creditCards: List<CreditCardReminderInfo>,
    monthStartDay: Int,
    reimbursableCount: Int,
): ReminderRun {
    if (!creditCardEnable && !reimbursementEnable) return ReminderRun(emptyList(), null)
    val dates = reminderCheckDates(lastReminderCheckMs, todayMs, zone)
    if (dates.isEmpty()) return ReminderRun(emptyList(), null)
    val items = dates.flatMap { date ->
        computeReminders(
            date = date,
            creditCardEnable = creditCardEnable,
            reimbursementEnable = reimbursementEnable,
            creditCards = creditCards,
            monthStartDay = monthStartDay,
            reimbursableCount = reimbursableCount,
        )
    }
    val newLastCheckMs = Instant.ofEpochMilli(todayMs).atZone(zone)
        .toLocalDate().atStartOfDay(zone).toInstant().toEpochMilli()
    return ReminderRun(items, newLastCheckMs)
}
```

> `Instant` / `ZoneId` / `LocalDate` 已在 `ReminderLogic.kt:19-21` 导入，无需新增。

- [ ] **Step 4: 跑测试确认通过**

Run: `./gradlew :sync:work:testDebugUnitTest --tests "*ReminderRunTest*" --offline --no-daemon --console=plain`
Expected: BUILD SUCCESSFUL，5 测试通过。

- [ ] **Step 5: spotless + 提交**

```bash
./gradlew spotlessApply --init-script gradle/init.gradle.kts --no-configuration-cache
git add sync/work/src/main/kotlin/cn/wj/android/cashbook/sync/reminder/ReminderLogic.kt sync/work/src/test/kotlin/cn/wj/android/cashbook/sync/reminder/ReminderRunTest.kt
git commit -m "[test|提醒|通知][公共]抽 reminderRun 编排纯函数（dates→逐日 computeReminders→checkpoint 决策）+5 用例"
```

---

### Task 2: `ReminderItem.toNotificationSpec()` 映射纯函数

**Files:**
- Modify: `sync/work/src/main/kotlin/cn/wj/android/cashbook/sync/reminder/ReminderModels.kt`（加 `NotificationSpec`）
- Modify: `sync/work/src/main/kotlin/cn/wj/android/cashbook/sync/reminder/ReminderLogic.kt`（加 `toNotificationSpec`）
- Test: `sync/work/src/test/kotlin/cn/wj/android/cashbook/sync/reminder/NotificationSpecTest.kt`

**Interfaces:**
- Consumes: `ReminderItem`（sealed，同包）；`R.string.reminder_credit_billing` / `reminder_credit_repayment` / `reminder_reimbursement`（`cn.wj.android.cashbook.sync.R`，已存在，被现行 `notify` 使用）；`REMINDER_TARGET_ASSET` / `REMINDER_TARGET_REIMBURSEMENT`（`cn.wj.android.cashbook.core.common`）。
- Produces: `data class NotificationSpec(@StringRes val textRes: Int, val formatArgs: List<Any>, val target: Int, val assetId: Long)`；`internal fun ReminderItem.toNotificationSpec(): NotificationSpec`。

- [ ] **Step 1: 写失败测试**

新建 `NotificationSpecTest.kt`（License Header + package `cn.wj.android.cashbook.sync.reminder`）：

```kotlin
package cn.wj.android.cashbook.sync.reminder

import cn.wj.android.cashbook.core.common.REMINDER_TARGET_ASSET
import cn.wj.android.cashbook.core.common.REMINDER_TARGET_REIMBURSEMENT
import cn.wj.android.cashbook.sync.R
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** [toNotificationSpec] 映射纯函数单测（纯 JVM，断言 R 资源 id 常量 + 深链目标）。 */
class NotificationSpecTest {

    @Test
    fun billing_mapsToBillingTextAssetTarget() {
        val spec = ReminderItem.CreditCardBilling(5L, "招行").toNotificationSpec()
        assertThat(spec).isEqualTo(
            NotificationSpec(R.string.reminder_credit_billing, listOf("招行"), REMINDER_TARGET_ASSET, 5L),
        )
    }

    @Test
    fun repayment_mapsToRepaymentTextAssetTarget() {
        val spec = ReminderItem.CreditCardRepayment(7L, "中信").toNotificationSpec()
        assertThat(spec).isEqualTo(
            NotificationSpec(R.string.reminder_credit_repayment, listOf("中信"), REMINDER_TARGET_ASSET, 7L),
        )
    }

    @Test
    fun reimbursement_mapsToReimbursementTextReimbursementTarget() {
        val spec = ReminderItem.Reimbursement(4).toNotificationSpec()
        assertThat(spec).isEqualTo(
            NotificationSpec(R.string.reminder_reimbursement, listOf(4), REMINDER_TARGET_REIMBURSEMENT, -1L),
        )
    }
}
```

- [ ] **Step 2: 跑测试确认失败**

Run: `./gradlew :sync:work:testDebugUnitTest --tests "*NotificationSpecTest*" --offline --no-daemon --console=plain`
Expected: 编译失败（`NotificationSpec` / `toNotificationSpec` 未定义）。

- [ ] **Step 3: 实现 `NotificationSpec` + `toNotificationSpec`**

在 `ReminderModels.kt` 末尾追加（文件顶部 import 区加 `import androidx.annotation.StringRes`）：

```kotlin
/**
 * 一条通知的展示规格（从 [ReminderItem] 派生的值类型，便于纯函数测试）。
 *
 * @param textRes 文案字符串资源 id
 * @param formatArgs 文案格式化参数（assetName 或 count）
 * @param target 深链目标（REMINDER_TARGET_ASSET / REMINDER_TARGET_REIMBURSEMENT）
 * @param assetId 深链资产 id（非资产类为 -1）
 */
data class NotificationSpec(
    @StringRes val textRes: Int,
    val formatArgs: List<Any>,
    val target: Int,
    val assetId: Long,
)
```

在 `ReminderLogic.kt` 末尾追加（import 区加 `import cn.wj.android.cashbook.core.common.REMINDER_TARGET_ASSET`、`import cn.wj.android.cashbook.core.common.REMINDER_TARGET_REIMBURSEMENT`、`import cn.wj.android.cashbook.sync.R`）：

```kotlin
/** [ReminderItem] → [NotificationSpec] 文案/深链目标映射（纯函数）。 */
internal fun ReminderItem.toNotificationSpec(): NotificationSpec = when (this) {
    is ReminderItem.CreditCardBilling ->
        NotificationSpec(R.string.reminder_credit_billing, listOf(assetName), REMINDER_TARGET_ASSET, assetId)

    is ReminderItem.CreditCardRepayment ->
        NotificationSpec(R.string.reminder_credit_repayment, listOf(assetName), REMINDER_TARGET_ASSET, assetId)

    is ReminderItem.Reimbursement ->
        NotificationSpec(R.string.reminder_reimbursement, listOf(count), REMINDER_TARGET_REIMBURSEMENT, -1L)
}
```

- [ ] **Step 4: 跑测试确认通过**

Run: `./gradlew :sync:work:testDebugUnitTest --tests "*NotificationSpecTest*" --offline --no-daemon --console=plain`
Expected: BUILD SUCCESSFUL，3 测试通过。

- [ ] **Step 5: spotless + 提交**

```bash
./gradlew spotlessApply --init-script gradle/init.gradle.kts --no-configuration-cache
git add sync/work/src/main/kotlin/cn/wj/android/cashbook/sync/reminder/ReminderModels.kt sync/work/src/main/kotlin/cn/wj/android/cashbook/sync/reminder/ReminderLogic.kt sync/work/src/test/kotlin/cn/wj/android/cashbook/sync/reminder/NotificationSpecTest.kt
git commit -m "[test|提醒|通知][公共]抽 ReminderItem.toNotificationSpec 文案/深链目标映射纯函数+3 用例"
```

---

### Task 3: `doWork`/`notify` 改用纯函数（行为保持重构）

**Files:**
- Modify: `sync/work/src/main/kotlin/cn/wj/android/cashbook/sync/workers/DailyReminderWorker.kt:75-156`

**Interfaces:**
- Consumes: `reminderRun(...)`（Task 1）、`ReminderItem.toNotificationSpec()`（Task 2）。
- Produces: 无新对外接口（薄壳内部重构）。

- [ ] **Step 1: 重构 `doWork()`**

将 `DailyReminderWorker.kt` 的 `doWork()`（75-116 行）整体替换为：

```kotlin
    override suspend fun doWork(): Result = withContext(ioDispatcher) {
        this@DailyReminderWorker.logger().i("doWork(), reminder check")
        val settings = settingRepository.appSettingsModel.first()
        if (!settings.creditCardReminderEnable && !settings.reimbursementReminderEnable) {
            // 两开关全关，跳查 repo 空转
            return@withContext Result.success()
        }
        val zone = ZoneId.systemDefault()
        val todayMs = System.currentTimeMillis()
        val monthStartDay = settingRepository.recordSettingsModel.first().monthStartDay
        val creditCards = if (settings.creditCardReminderEnable) {
            assetRepository.currentVisibleAssetListData.first()
                .filter { it.classification.isCreditCard }
                .map { CreditCardReminderInfo(it.id, it.name, it.billingDate, it.repaymentDate) }
        } else {
            emptyList()
        }
        val reimbursableCount = if (settings.reimbursementReminderEnable) {
            recordRepository.getReimbursableUnrelatedRecordList().size
        } else {
            0
        }

        val run = reminderRun(
            lastReminderCheckMs = settings.lastReminderCheckMs,
            todayMs = todayMs,
            zone = zone,
            creditCardEnable = settings.creditCardReminderEnable,
            reimbursementEnable = settings.reimbursementReminderEnable,
            creditCards = creditCards,
            monthStartDay = monthStartDay,
            reimbursableCount = reimbursableCount,
        )
        run.items.forEach { notify(it) }
        run.newLastCheckMs?.let { settingRepository.updateLastReminderCheckMs(it) }
        Result.success()
    }
```

> 行为对照：① 两开关全关早退（壳保留微优化，与 `reminderRun` 内分支冗余但独立正确）；② `reminderCheckDates`/逐日 `computeReminders`/checkpoint 推进全部下沉 `reminderRun`，语义逐一等价；③ checkpoint 持久化由 `run.newLastCheckMs?.let{}` 守卫（null 不写 = 旧的两处早退不写 checkpoint）。

- [ ] **Step 2: 重构 `notify()`**

将 `notify(item: ReminderItem)`（118-144 行）整体替换为：

```kotlin
    private fun notify(item: ReminderItem) {
        val nm = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return
        val id = reminderNotificationId(ReminderNotificationBaseId, item)
        val spec = item.toNotificationSpec()
        sendNotification(
            nm = nm,
            id = id,
            text = appContext.getString(spec.textRes, *spec.formatArgs.toTypedArray()),
            intent = reminderDeepLinkIntent(appContext, spec.target, spec.assetId),
        )
    }
```

- [ ] **Step 3: 清理无用 import**

`DailyReminderWorker.kt` 删除不再使用的 import：
- `import cn.wj.android.cashbook.core.common.REMINDER_TARGET_ASSET`（line 27）
- `import cn.wj.android.cashbook.core.common.REMINDER_TARGET_REIMBURSEMENT`（line 28）
- `import cn.wj.android.cashbook.sync.reminder.computeReminders`（line 40）
- `import cn.wj.android.cashbook.sync.reminder.reminderCheckDates`（line 41）

> `REMINDER_TARGET_*` 已下沉到 `toNotificationSpec`；`computeReminders`/`reminderCheckDates` 已下沉到 `reminderRun`。新增 import `cn.wj.android.cashbook.sync.reminder.reminderRun`、`cn.wj.android.cashbook.sync.reminder.toNotificationSpec`。保留 `ReminderItem`、`CreditCardReminderInfo`、`reminderDeepLinkIntent`、`reminderNotificationId`、`Instant`、`ZoneId` 等仍用到的。`Duration`/`TimeUnit`/`Instant` 仍被 `initialDelayToNext`/companion 用，勿删。

- [ ] **Step 4: 编译 + 全量 sync:work 测试确认无回归**

Run: `./gradlew :sync:work:testDebugUnitTest --offline --no-daemon --console=plain`
Expected: BUILD SUCCESSFUL；`ReminderLogicTest` / `ReminderRunTest` / `NotificationSpecTest` / `AutoBackupWorkerTest` / `ReminderScheduleTest` 全通过（行为保持 = 既有测试不变绿）。

> 若 `--offline` 报缺依赖，去 `--offline` 并加 `-Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyPort=7897 -Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=7897` 暖缓存后重试。

- [ ] **Step 5: spotless + 提交**

```bash
./gradlew spotlessApply --init-script gradle/init.gradle.kts --no-configuration-cache
git add sync/work/src/main/kotlin/cn/wj/android/cashbook/sync/workers/DailyReminderWorker.kt
git commit -m "[refactor|提醒|通知][公共]DailyReminderWorker doWork/notify 改用 reminderRun+toNotificationSpec 薄壳化（行为保持）"
```

---

# Item 1：深链一次性消费（统一 shortcuts + reminder）

### Task 4: `PendingDeepLink` 模型 + `parsePendingDeepLink` 纯函数

**Files:**
- Create: `app/src/main/kotlin/cn/wj/android/cashbook/ui/PendingDeepLink.kt`
- Test: `app/src/test/kotlin/cn/wj/android/cashbook/ui/PendingDeepLinkTest.kt`

**Interfaces:**
- Consumes: `SHORTCUTS_TYPE_ADD`(=0) / `SHORTCUTS_TYPE_ASSET`(=1) / `REMINDER_TARGET_ASSET`(=1) / `REMINDER_TARGET_REIMBURSEMENT`(=2)（`cn.wj.android.cashbook.core.common`，已存在）。
- Produces: `sealed interface PendingDeepLink { None; AddRecord; MyAsset; data class AssetInfo(assetId: Long); Reimbursement }`；`internal fun parsePendingDeepLink(shortcutsType: Int, reminderTarget: Int, reminderAssetId: Long): PendingDeepLink`。

- [ ] **Step 1: 写失败测试**

新建 `PendingDeepLinkTest.kt`（License Header + package `cn.wj.android.cashbook.ui`）：

```kotlin
package cn.wj.android.cashbook.ui

import cn.wj.android.cashbook.core.common.REMINDER_TARGET_ASSET
import cn.wj.android.cashbook.core.common.REMINDER_TARGET_NONE
import cn.wj.android.cashbook.core.common.REMINDER_TARGET_REIMBURSEMENT
import cn.wj.android.cashbook.core.common.SHORTCUTS_TYPE_ADD
import cn.wj.android.cashbook.core.common.SHORTCUTS_TYPE_ASSET
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** [parsePendingDeepLink] 纯函数单测（纯 JVM）。 */
class PendingDeepLinkTest {

    @Test
    fun reminderAsset_parsesToAssetInfo() {
        val result = parsePendingDeepLink(
            shortcutsType = -1,
            reminderTarget = REMINDER_TARGET_ASSET,
            reminderAssetId = 9L,
        )
        assertThat(result).isEqualTo(PendingDeepLink.AssetInfo(9L))
    }

    @Test
    fun reminderReimbursement_parsesToReimbursement() {
        val result = parsePendingDeepLink(-1, REMINDER_TARGET_REIMBURSEMENT, -1L)
        assertThat(result).isEqualTo(PendingDeepLink.Reimbursement)
    }

    @Test
    fun shortcutAdd_parsesToAddRecord() {
        val result = parsePendingDeepLink(SHORTCUTS_TYPE_ADD, REMINDER_TARGET_NONE, -1L)
        assertThat(result).isEqualTo(PendingDeepLink.AddRecord)
    }

    @Test
    fun shortcutAsset_parsesToMyAsset() {
        val result = parsePendingDeepLink(SHORTCUTS_TYPE_ASSET, REMINDER_TARGET_NONE, -1L)
        assertThat(result).isEqualTo(PendingDeepLink.MyAsset)
    }

    @Test
    fun nothing_parsesToNone() {
        val result = parsePendingDeepLink(-1, REMINDER_TARGET_NONE, -1L)
        assertThat(result).isEqualTo(PendingDeepLink.None)
    }

    @Test
    fun reminderTakesPriorityOverShortcut() {
        // 同时带 reminder 与 shortcut → reminder 优先（构造上互斥，防御性）
        val result = parsePendingDeepLink(SHORTCUTS_TYPE_ADD, REMINDER_TARGET_ASSET, 3L)
        assertThat(result).isEqualTo(PendingDeepLink.AssetInfo(3L))
    }
}
```

- [ ] **Step 2: 跑测试确认失败**

Run: `./gradlew :app:testOnlineDebugUnitTest --tests "*PendingDeepLinkTest*" --offline --no-daemon --console=plain`
Expected: 编译失败（`PendingDeepLink` / `parsePendingDeepLink` 未定义）。

- [ ] **Step 3: 实现 `PendingDeepLink.kt`**

新建 `app/src/main/kotlin/cn/wj/android/cashbook/ui/PendingDeepLink.kt`（License Header + 以下内容）：

```kotlin
package cn.wj.android.cashbook.ui

import cn.wj.android.cashbook.core.common.REMINDER_TARGET_ASSET
import cn.wj.android.cashbook.core.common.REMINDER_TARGET_REIMBURSEMENT
import cn.wj.android.cashbook.core.common.SHORTCUTS_TYPE_ADD
import cn.wj.android.cashbook.core.common.SHORTCUTS_TYPE_ASSET

/**
 * 待消费的深链意图（统一应用快捷方式与提醒通知深链）。
 *
 * 一次性消费：导航后由调用方复位为 [None] 并清除 intent extra，避免 uiState 重发时弹回。
 */
sealed interface PendingDeepLink {
    /** 无待处理深链 */
    data object None : PendingDeepLink

    /** 快捷方式：记一笔 */
    data object AddRecord : PendingDeepLink

    /** 快捷方式：我的资产 */
    data object MyAsset : PendingDeepLink

    /** 提醒：信用卡资产详情 */
    data class AssetInfo(val assetId: Long) : PendingDeepLink

    /** 提醒：待报销列表 */
    data object Reimbursement : PendingDeepLink
}

/**
 * 合并解析 intent 三个 extra 为单一深链意图（纯函数）。
 * 优先级：reminder 高于 shortcuts（二者构造上互斥，此优先级为防御性）。
 */
internal fun parsePendingDeepLink(
    shortcutsType: Int,
    reminderTarget: Int,
    reminderAssetId: Long,
): PendingDeepLink = when {
    reminderTarget == REMINDER_TARGET_ASSET -> PendingDeepLink.AssetInfo(reminderAssetId)
    reminderTarget == REMINDER_TARGET_REIMBURSEMENT -> PendingDeepLink.Reimbursement
    shortcutsType == SHORTCUTS_TYPE_ADD -> PendingDeepLink.AddRecord
    shortcutsType == SHORTCUTS_TYPE_ASSET -> PendingDeepLink.MyAsset
    else -> PendingDeepLink.None
}
```

- [ ] **Step 4: 跑测试确认通过**

Run: `./gradlew :app:testOnlineDebugUnitTest --tests "*PendingDeepLinkTest*" --offline --no-daemon --console=plain`
Expected: BUILD SUCCESSFUL，6 测试通过。

- [ ] **Step 5: spotless + 提交**

```bash
./gradlew spotlessApply --init-script gradle/init.gradle.kts --no-configuration-cache
git add app/src/main/kotlin/cn/wj/android/cashbook/ui/PendingDeepLink.kt app/src/test/kotlin/cn/wj/android/cashbook/ui/PendingDeepLinkTest.kt
git commit -m "[feat|提醒|通知][公共]PendingDeepLink sealed 统一 shortcuts+reminder 深链意图+parsePendingDeepLink 纯函数+6 用例"
```

---

### Task 5: 接线 MainActivity + MainApp（合并态 + 一次性消费）

**Files:**
- Modify: `app/src/main/kotlin/cn/wj/android/cashbook/ui/MainActivity.kt`
- Modify: `app/src/main/kotlin/cn/wj/android/cashbook/ui/MainApp.kt`

**Interfaces:**
- Consumes: `PendingDeepLink` / `parsePendingDeepLink`（Task 4）；常量 `SHORTCUTS_TYPE` / `EXTRA_REMINDER_TARGET` / `EXTRA_REMINDER_ASSET_ID` / `REMINDER_TARGET_NONE`（core:common）；路由 `EditRecord` / `MyAsset` / `AssetInfo` / `Reimbursement` 与 `naviToEditRecord()` / `naviToMyAsset()` / `naviToAssetInfo(Long)` / `naviToReimbursement()`（已 import 于 MainApp.kt）。
- Produces: `MainApp(pendingDeepLink: PendingDeepLink, onConsumePendingDeepLink: () -> Unit, viewModel: MainAppViewModel = viewModel())`。

- [ ] **Step 1: 改 `MainActivity` 状态与 onCreate/onNewIntent**

`MainActivity.kt`：删除 53-59 行的三个属性，替换为单一属性：

```kotlin
    /** 待消费的深链意图（快捷方式 + 提醒通知统一） */
    private var pendingDeepLink by mutableStateOf<PendingDeepLink>(PendingDeepLink.None)
```

`onCreate` 的 66-70 行（读 extra + log）替换为：

```kotlin
        // 解析深链意图（快捷方式 + 提醒通知）
        pendingDeepLink = parsePendingDeepLink(
            shortcutsType = intent.getIntExtra(SHORTCUTS_TYPE, -1),
            reminderTarget = intent.getIntExtra(EXTRA_REMINDER_TARGET, REMINDER_TARGET_NONE),
            reminderAssetId = intent.getLongExtra(EXTRA_REMINDER_ASSET_ID, -1L),
        )
        logger().i("onCreate(), pendingDeepLink = <$pendingDeepLink>")
```

`onNewIntent`（129-135 行）替换为（关键：`setIntent(intent)` 使后续 `getIntent()` 反映最新 intent，consume 清除才作用于正确 intent）：

```kotlin
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingDeepLink = parsePendingDeepLink(
            shortcutsType = intent.getIntExtra(SHORTCUTS_TYPE, -1),
            reminderTarget = intent.getIntExtra(EXTRA_REMINDER_TARGET, REMINDER_TARGET_NONE),
            reminderAssetId = intent.getLongExtra(EXTRA_REMINDER_ASSET_ID, -1L),
        )
        logger().i("onNewIntent(), pendingDeepLink = <$pendingDeepLink>")
    }
```

- [ ] **Step 2: 改 `MainActivity` 的 MainApp 调用（118-124 行）**

```kotlin
                ProvideLocalState {
                    MainApp(
                        pendingDeepLink = pendingDeepLink,
                        onConsumePendingDeepLink = {
                            pendingDeepLink = PendingDeepLink.None
                            intent.removeExtra(SHORTCUTS_TYPE)
                            intent.removeExtra(EXTRA_REMINDER_TARGET)
                            intent.removeExtra(EXTRA_REMINDER_ASSET_ID)
                        },
                    )
                }
```

- [ ] **Step 3: 清理 `MainActivity` import**

删除不再使用的 import：`androidx.compose.runtime.mutableIntStateOf`（line 28）、`androidx.compose.runtime.mutableLongStateOf`（line 29）。保留 `mutableStateOf`（pendingDeepLink + uiState 用）。保留 `SHORTCUTS_TYPE` / `EXTRA_REMINDER_TARGET` / `EXTRA_REMINDER_ASSET_ID` / `REMINDER_TARGET_NONE`（parse + consume 用）。删除 `import cn.wj.android.cashbook.core.common.REMINDER_TARGET_NONE`？——仍用于 `getIntExtra` 默认值，**保留**。`PendingDeepLink`/`parsePendingDeepLink` 同包 `cn.wj.android.cashbook.ui`，无需 import。

- [ ] **Step 4: 改 `MainApp` 签名（157-162 行）**

```kotlin
@Composable
fun MainApp(
    pendingDeepLink: PendingDeepLink,
    onConsumePendingDeepLink: () -> Unit,
    viewModel: MainAppViewModel = viewModel(),
) {
```

- [ ] **Step 5: 合并两个 LaunchedEffect（234-278 行）为一个**

将 234-278 行（`// 快捷入口变化...` 的 LaunchedEffect 与 `// 提醒深链...` 的 LaunchedEffect）整体替换为：

```kotlin
                // 深链消费（快捷方式 + 提醒统一）：受安全验证门控（needVerity 未通过不导航不消费，
                // 验证通过后 uiState 重发再导航并消费 → 顺带修「安全门未过深链丢失」）
                LaunchedEffect(pendingDeepLink, uiState) {
                    (uiState as? MainAppUiState.Success)?.run {
                        if (!needRequestProtocol && !needVerity) {
                            when (val link = pendingDeepLink) {
                                PendingDeepLink.None -> Unit
                                PendingDeepLink.AddRecord ->
                                    if (navController.currentDestination?.hasRoute<EditRecord>() != true) {
                                        navController.naviToEditRecord()
                                    }

                                PendingDeepLink.MyAsset ->
                                    if (navController.currentDestination?.hasRoute<MyAsset>() != true) {
                                        navController.naviToMyAsset()
                                    }

                                is PendingDeepLink.AssetInfo ->
                                    if (link.assetId > 0 &&
                                        navController.currentDestination?.hasRoute<AssetInfo>() != true
                                    ) {
                                        navController.naviToAssetInfo(link.assetId)
                                    }

                                PendingDeepLink.Reimbursement ->
                                    if (navController.currentDestination?.hasRoute<Reimbursement>() != true) {
                                        navController.naviToReimbursement()
                                    }
                            }
                            if (pendingDeepLink != PendingDeepLink.None) {
                                onConsumePendingDeepLink()
                            }
                        }
                    }
                }
```

> 消费在 `when` 之后、`hasRoute` 守卫之外：即便已在目标路由（不重导航）或 `AssetInfo` assetId 非法（不导航），只要过了 needVerity/protocol 门控且非 None，也消费（深链已处理/丢弃，防再次弹回）。

- [ ] **Step 6: 清理 `MainApp` import**

删除不再使用的 import（已被 `PendingDeepLink` 分支取代）：
- `import cn.wj.android.cashbook.core.common.REMINDER_TARGET_ASSET`（line 62）
- `import cn.wj.android.cashbook.core.common.REMINDER_TARGET_NONE`（line 63）
- `import cn.wj.android.cashbook.core.common.REMINDER_TARGET_REIMBURSEMENT`（line 64）
- `import cn.wj.android.cashbook.core.common.SHORTCUTS_TYPE_ADD`（line 65）
- `import cn.wj.android.cashbook.core.common.SHORTCUTS_TYPE_ASSET`（line 66）

> 这 5 个常量仅在被替换的两个 LaunchedEffect + 旧 `reminderTarget` 默认值中使用。`PendingDeepLink` 同包无需 import。

- [ ] **Step 7: 编译（跨模块 Hilt 全图）+ app 单测**

Run:
```bash
./gradlew :app:compileOnlineDebugKotlin --offline --no-daemon --console=plain
./gradlew :app:testOnlineDebugUnitTest --offline --no-daemon --console=plain
```
Expected: 两条均 BUILD SUCCESSFUL；`PendingDeepLinkTest` 6 测试 + 既有 app 测试（`ActivityUiStateTest`/`MainViewModelTest`）全通过。

> `MainApp` 唯一调用方是 `MainActivity`（本任务已同步改），无截图/Preview 引用，签名变更无其它编译破坏。

- [ ] **Step 8: spotlessCheck + 提交**

```bash
./gradlew spotlessCheck --init-script gradle/init.gradle.kts --no-configuration-cache
git add app/src/main/kotlin/cn/wj/android/cashbook/ui/MainActivity.kt app/src/main/kotlin/cn/wj/android/cashbook/ui/MainApp.kt
git commit -m "[feat|提醒|通知][公共]深链一次性消费：MainActivity 合并 PendingDeepLink 态+consume 回调(复位+清 intent extra+setIntent)；MainApp 两 LaunchedEffect 合一+导航后消费(门控延后消费)"
```

---

## 节点 2 评审门（Task 5 后、Item 3 前）

所有代码改完（Task 1–5）后，调用 `comprehensive-review:full-review` 对本次 git diff（跨 `sync:work` + `app`，含导航行为变更）跑核心维度（架构+安全+性能+测试+best-practices）。blocking = Critical/High，交付前修复或经用户授权放行。

---

# Item 3：真机 journey 黑盒验证（模拟器，手动）

### Task 6: 模拟器 journey 验证（无代码改动，产物=验证报告）

**Files:**
- Create: `docs/testing/reports/2026-06-27-reminder-journey.md`（验证报告）

> 非 TDD 任务。需模拟器；本机无物理设备。前置查内存压力（可用 <1000MB 或 >90% → 中止问用户；关 Android Studio 腾内存）。

- [ ] **Step 1: 准备环境**

查内存：
```bash
powershell -NoProfile -Command 'chcp 65001 > $null; $os=Get-CimInstance Win32_OperatingSystem; "Avail: {0:N0}MB  Used%: {1:N1}" -f ($os.FreePhysicalMemory/1024), ((1-$os.FreePhysicalMemory/$os.TotalVisibleMemorySize)*100)'
```
通过后启 `Medium_Phone` 模拟器，装最新 Online Debug APK。

- [ ] **Step 2: 造数据**

- 设置页开「信用卡提醒」+「待报销提醒」两开关（首开触发 POST_NOTIFICATIONS 授权，允许）。
- 改某信用卡资产账单日/还款日为「今天」的 day-of-month。
- 造一笔可报销支出（进待报销列表）。

- [ ] **Step 3: 触发 Worker 并验证**

手动触发 `DailyReminderWorker`（`adb shell cmd jobscheduler` / WorkManager 测试入口 / 或临时把 `initialDelayToNext` 调小重装）。验证清单：
- [ ] N1 账单/还款通知投递、文案含资产名。
- [ ] N2 待报销通知投递、文案含笔数。
- [ ] 点击 N1 通知 → 落地信用卡资产详情（正确 assetId）。
- [ ] 点击 N2 通知 → 落地待报销列表。
- [ ] **安全门门控**：设置页开应用锁（密码/指纹）→ 点深链 → 验证通过前停在验证页、通过后落地目标页。
- [ ] **一次性消费**：落地目标页后返回首页 → 触发 uiState 变化（如锁屏再解锁 / 旋转屏幕）→ **不被弹回**目标页。

- [ ] **Step 4: 写报告 + 提交**

记录每条 PASS/FAIL + 截图/layout dump 证据到 `docs/testing/reports/2026-06-27-reminder-journey.md`。

```bash
git add docs/testing/reports/2026-06-27-reminder-journey.md
git commit -m "[test|提醒|通知][公共]提醒通知 + 深链一次性消费模拟器 journey 验证报告"
```

> 受黑盒工具限制（Compose 截图全白、icon 无 text）的项按 android-cli 方法论（`android layout` JSON dump + jq 取 center/text）处理。删账本/导入等非本特性入口不在范围。

---

## Self-Review

**Spec 覆盖：**
- Item 1 深链一次性消费 → Task 4（模型+解析）+ Task 5（接线+消费）。统一 shortcuts+reminder ✓、consume 复位+清 extra ✓、门控延后消费 ✓、setIntent 修正 ✓。
- Item 2 doWork 编排纯函数化 → Task 1（reminderRun）+ Task 2（toNotificationSpec）+ Task 3（薄壳重构）。行为保持 ✓。
- Item 3 真机验证 → Task 6。安全门 + 一次性消费 + 通知投递 + 深链落地 ✓。
- 测试矩阵：reminderRun 全分支 ✓、toNotificationSpec 三映射 ✓、parsePendingDeepLink 全分支 ✓、回归编译/测试/spotless ✓。
- 节点 2 full-review 门 ✓（Task 5 后）。

**占位符扫描：** 无 TBD/TODO；每个 code step 给出完整代码与确切命令、预期输出。

**类型一致性：** `ReminderRun(items, newLastCheckMs)` / `reminderRun(...)` 签名 Task 1 定义、Task 3 调用一致；`NotificationSpec(textRes, formatArgs, target, assetId)` / `toNotificationSpec()` Task 2 定义、Task 3 调用一致；`PendingDeepLink`（None/AddRecord/MyAsset/AssetInfo(assetId)/Reimbursement）/ `parsePendingDeepLink(shortcutsType, reminderTarget, reminderAssetId)` Task 4 定义、Task 5 调用一致；`MainApp(pendingDeepLink, onConsumePendingDeepLink, viewModel)` Task 5 定义与 MainActivity 调用一致。
