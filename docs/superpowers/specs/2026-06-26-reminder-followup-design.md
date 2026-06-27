# 提醒通知收尾 backlog 设计（深链一次性消费 + doWork 编排纯函数化 + 真机验证）

> 创建于 2026-06-26。承接已合入的 N1/N2 提醒通知功能（`2026-06-26-reminder-notifications-design.md`）的遗留 backlog。

## 背景

N1（信用卡账单/还款提醒）+ N2（待报销提醒）功能已完成。本设计处理其遗留 backlog 中的三项：

- **Item 1 深链一次性消费**：当前深链导航存在「消费后从不复位 → uiState 再发射时带陈旧值重触发、把用户弹回目标页」的 bug；老的 `shortcutsType`（应用快捷方式）是同款 bug，新的 `reminderTarget` 继承了它。统一为一次性消费。
- **Item 2 doWork 编排纯函数化**：`DailyReminderWorker.doWork()` 的编排逻辑（开关早退、补发区间、逐日判定、checkpoint 推进、通知映射）无测试覆盖；纯函数 `computeReminders`/`reminderCheckDates` 已各有测试，缺的是「编排薄壳」本身。
- **Item 3 真机 journey 黑盒验证**：本机无物理设备，需模拟器端到端验证 N1/N2 + Item 1 修复后的深链行为。

> Item 4（4 处裸 `ProgressIndicator` 迁封装）经 grep 核验已在 `a5752026` 全部完成（core/design 之外零生产代码裸用），**不在本设计范围**。

## 问题实证

### Item 1 bug 定位

- `MainActivity.kt:53-59`：`shortcutsType` / `reminderTarget` / `reminderAssetId` 各为 `mutableState`，`onCreate`(66-69) 与 `onNewIntent`(131-133) 均从 `intent` 读取 extra 赋值。
- `MainApp.kt:234-255`（shortcuts）与 `MainApp.kt:258-278`（reminder）：两个 `LaunchedEffect`，key 含 `uiState`。
- 缺陷：导航后**不复位**触发值。唯一守卫是「已在目标路由则不重复导航」（`hasRoute<AssetInfo>() != true`）。用户从落地页导航**离开**后，只要 `uiState` 再发射新 `Success`（needVerity 切换 / 配置变更 / 进程恢复），effect 带**同一**值重跑 → 强制弹回目标页。
- 配置变更/进程恢复二次触发向量：`onCreate` 每次从 `getIntent()` 重读 extra，旋转重建后 extra 仍在 → 再触发。故「一次性消费」需同时处理「内存态复位」与「intent extra 清除」两层。

### Item 2 缺口定位

- `DailyReminderWorker.doWork()`（`DailyReminderWorker.kt:75-116`）：开关全关早退、`reminderCheckDates` 区间、条件查信用卡/待报销 size、逐日 `computeReminders` → `notify`、`updateLastReminderCheckMs`。整段编排无 doWork 级测试。
- `notify`(118-144)：`ReminderItem` → 文案 string-res + 深链目标 + assetId 的映射也无测试。
- 同模块先例 `AutoBackupWorkerTest`（`AutoBackupWorkerTest.kt:24-60`）：把 worker 决策抽 top-level 纯函数 `mapBackupStateToResult` 纯 JVM 测，**不引 Robolectric**。`sync:work` 现仅 junit+truth 依赖。本设计沿用此先例。

## Item 1 设计：深链一次性消费（统一 shortcuts + reminder）

### 数据模型（app 模块新增）

```kotlin
sealed interface PendingDeepLink {
    data object None : PendingDeepLink
    data object AddRecord : PendingDeepLink                       // 快捷方式 记一笔
    data object MyAsset : PendingDeepLink                         // 快捷方式 我的资产
    data class AssetInfo(val assetId: Long) : PendingDeepLink     // 提醒 信用卡详情
    data object Reimbursement : PendingDeepLink                   // 提醒 待报销
}

/** 纯函数：合并解析 intent 三个 extra 为单一深链意图。优先级：reminder 高于 shortcuts（实施时确认互斥）。 */
internal fun parsePendingDeepLink(
    shortcutsType: Int,
    reminderTarget: Int,
    reminderAssetId: Long,
): PendingDeepLink
```

`PendingDeepLink` 与 `parsePendingDeepLink` 置于 app 模块（与 `MainActivity`/`MainApp` 同层，依赖 feature 路由）。

### MainActivity 改造

- 3 个 `mutableState`（`shortcutsType`/`reminderTarget`/`reminderAssetId`）合并为 1 个 `pendingDeepLink: PendingDeepLink by mutableStateOf(None)`。
- `onCreate` / `onNewIntent`：`pendingDeepLink = parsePendingDeepLink(intent.getIntExtra(SHORTCUTS_TYPE, -1), intent.getIntExtra(EXTRA_REMINDER_TARGET, REMINDER_TARGET_NONE), intent.getLongExtra(EXTRA_REMINDER_ASSET_ID, -1L))`。
- 向 `MainApp` 传 `pendingDeepLink` + `onConsumePendingDeepLink: () -> Unit`。
- `onConsumePendingDeepLink` = 复位 `pendingDeepLink = None` **且** `intent.removeExtra(SHORTCUTS_TYPE / EXTRA_REMINDER_TARGET / EXTRA_REMINDER_ASSET_ID)`（防配置变更/进程恢复后 `onCreate` 重读重触发）。

### MainApp 改造

- 签名从 `(shortcutsType: Int, reminderTarget: Int, reminderAssetId: Long)` 改为 `(pendingDeepLink: PendingDeepLink, onConsumePendingDeepLink: () -> Unit)`。唯一调用方 `MainActivity.kt:119`，无 Preview/截图测试引用。
- 两个 `LaunchedEffect` 合并为一个 `LaunchedEffect(pendingDeepLink, uiState)`：`Success && !needRequestProtocol && !needVerity` 时 `when(pendingDeepLink)` 导航（AddRecord→naviToEditRecord / MyAsset→naviToMyAsset / AssetInfo→naviToAssetInfo(assetId) / Reimbursement→naviToReimbursement），**导航后调 `onConsumePendingDeepLink()`**；`None` 分支 no-op。
- **门控延后消费语义**：`needVerity=true` 时既不导航也不消费 → 验证通过后 `uiState` 重发 → effect 再跑 → 导航并消费。门控天然把消费延后到验证通过，顺带修了「安全门未过时深链丢失」的潜在问题。
- `hasRoute` 防重导航守卫保留（消费一次后冗余但无害，防御 effect 在同一 emission 内多次求值）。

### 影响面

- 改动文件：`MainActivity.kt`、`MainApp.kt` + 新增 `PendingDeepLink.kt`（app 模块）。
- 签名变更仅 `MainApp` 单一调用方。常量复用 core:common 既有 `SHORTCUTS_TYPE` / `SHORTCUTS_TYPE_ADD` / `SHORTCUTS_TYPE_ASSET` / `EXTRA_REMINDER_TARGET` / `EXTRA_REMINDER_ASSET_ID` / `REMINDER_TARGET_*`。
- 无 Proto / Room / 接口跨模块变更。

## Item 2 设计：doWork 编排纯函数化（行为保持）

抽两个 top-level `internal` 纯函数（`sync:work`），`doWork` 退化为 I/O 薄壳。**严格行为保持**，不改任何判定语义。

### reminderRun — 编排决策

```kotlin
internal data class ReminderRun(
    val items: List<ReminderItem>,   // 待投递通知，按日期+顺序
    val newLastCheckMs: Long?,       // 需持久化的 checkpoint，null=不推进
)

internal fun reminderRun(
    lastReminderCheckMs: Long,
    todayMs: Long,
    zone: ZoneId,
    creditCardEnable: Boolean,
    reimbursementEnable: Boolean,
    creditCards: List<CreditCardReminderInfo>,
    monthStartDay: Int,
    reimbursableCount: Int,
): ReminderRun
```

判定（逐一对齐现行 `doWork`）：
- 两开关全关 → `ReminderRun(emptyList, null)`（不推进 checkpoint，与现行 line 78-81 一致）。
- `dates = reminderCheckDates(...)` 为空 → `ReminderRun(emptyList, null)`（不推进，与现行 line 84-85 一致）。
- 否则 `items = dates.flatMap { computeReminders(date, ...) }`，`newLastCheckMs = todayMs 当日 startOfDay`（推进，与现行 line 101-114 一致；注意：dates 非空但当日无提醒时 items 为空、checkpoint **仍推进**）。

### ReminderItem.toNotificationSpec — 文案/深链目标映射

```kotlin
internal data class NotificationSpec(
    @StringRes val textRes: Int,
    val formatArgs: List<Any>,   // assetName 或 count
    val target: Int,             // REMINDER_TARGET_ASSET / REMINDER_TARGET_REIMBURSEMENT
    val assetId: Long,
)

internal fun ReminderItem.toNotificationSpec(): NotificationSpec
```

映射（对齐现行 `notify` line 122-143）：
- `CreditCardBilling` → `reminder_credit_billing` + [assetName] + `REMINDER_TARGET_ASSET` + assetId
- `CreditCardRepayment` → `reminder_credit_repayment` + [assetName] + `REMINDER_TARGET_ASSET` + assetId
- `Reimbursement` → `reminder_reimbursement` + [count] + `REMINDER_TARGET_REIMBURSEMENT` + -1L

### doWork 薄壳

```
val settings = settingRepository.appSettingsModel.first()
if (两开关全关) return success()        // 跳查 repo 的微优化保留
val zone/todayMs/monthStartDay
val creditCards = if (creditCardEnable) query else emptyList()
val reimbursableCount = if (reimbursementEnable) query else 0
val run = reminderRun(settings.lastReminderCheckMs, todayMs, zone, ...flags, creditCards, monthStartDay, count)
run.items.forEach { notify(it) }
run.newLastCheckMs?.let { settingRepository.updateLastReminderCheckMs(it) }
success()
```

`notify` 用 `item.toNotificationSpec()` 取值；`getString(spec.textRes, *args)` / `reminderDeepLinkIntent(ctx, spec.target, spec.assetId)` / `nm.notify` 留壳内（I/O，不测）。

> 注：「两开关全关 → 跳查」既由壳内 early-return 保留（微优化），又被 `reminderRun` 独立正确处理（传 false flags → 返回 empty/null）；两者无冲突，`reminderRun` 全分支可测，壳只是短路省查询。

## Item 3 设计：真机 journey 黑盒验证（模拟器）

实施完 1+2 后执行（纯验证，无代码改动）：

1. 先查内存压力（>90% 或可用 <1000MB → 中止问用户；关 Android Studio 腾内存）。启 Medium_Phone 模拟器。
2. 开提醒开关（设置页两开关）+ 改某信用卡资产账单日/还款日为「今天」+ 造一笔待报销支出。
3. 手动触发 `DailyReminderWorker`（`adb` WorkManager 测试触发 / 或临时改触发时机）。
4. 验证：
   - N1 账单/还款通知 + N2 待报销通知投递、文案正确。
   - 点击通知深链落地正确页（信用卡详情 / 待报销列表）。
   - **安全门门控**：开应用锁（needVerity）后点深链 → 验证通过前不跳转、通过后落地。
   - **一次性消费**：落地后返回、再切 uiState（如锁屏解锁），**不被弹回**目标页。

> 受黑盒工具限制（Compose 截图全白、icon 无 text）的项按既往 android-cli 方法论（`android layout` JSON dump + jq）处理。

## 测试矩阵

| 项 | 测试 | 层 |
|---|---|---|
| Item 1 | `parsePendingDeepLink` 全分支（add / asset / reminder-asset / reimbursement / none / 优先级互斥） | app `testOnlineDebugUnitTest`（纯 JVM） |
| Item 1 | 消费一次性行为（导航后复位、门控延后消费、弹回防护） | Item 3 journey（UI 层，模拟器） |
| Item 2 | `reminderRun` 全分支（两开关 4 组合 / dates 空 / dates 多日补发 / checkpoint 推进与否 / dates 非空但 items 空仍推进） | `sync:work:testDebugUnitTest`（纯 JVM） |
| Item 2 | `toNotificationSpec` 三类 `ReminderItem` 映射 | `sync:work:testDebugUnitTest`（纯 JVM） |
| 回归 | `:app:compileOnlineDebugKotlin`（跨模块 Hilt 全图）+ `sync:work:testDebugUnitTest` + `app:testOnlineDebugUnitTest` + spotlessCheck + 相关 lint | — |

## 评审节点

- **节点 1**（方案后）：Item 1 改导航行为，已走本 brainstorming；本 spec 落盘后按需四维 team-review。
- **节点 2**（开发完）：`comprehensive-review:full-review` 对本次 git diff（跨 app + sync:work，含导航行为变更）跑核心维度。

## 非目标 / 范围外

- 不改 N1/N2 的判定语义（`computeReminders`/`reminderCheckDates` 不动）。
- 不改 Proto / Room schema / 跨模块接口。
- 不处理 N1 多账本/隐藏卡限制（spec §9 已知限制，保持）。
- Item 4 已完成，不在范围。

## 实施顺序

Item 2（行为保持重构 + 纯 JVM 测，风险最低）→ Item 1（导航行为变更 + 纯函数测）→ 节点 2 full-review → Item 3 模拟器 journey 验证。
