# 提醒通知收尾 backlog 模拟器 journey 验证报告

> 日期：2026-06-27　环境：Medium_Phone 模拟器（API 30 / Android 11，Google Play 镜像，无 root）
> 对象：提醒通知收尾 backlog（Item 1 深链一次性消费 + Item 2 doWork 编排纯函数化）
> 构建：`:app:assembleOnlineDebug`（`app-Online-debug.apk`）

## 方法

- Compose 截图全白 → 全程用 `android layout` JSON dump（`center`/`text`/`interactions`/`resource-id`）+ jq 定位元素。
- 深链通过 `adb am start -n .../MainActivity --ei/--el <extra>` 投递，**等价于通知 PendingIntent 的行为**（`reminderDeepLinkIntent` 构造同样的显式 Intent + extra）。
- 数据库通过 `adb exec-out run-as cat databases/cashbook.db*` 拉取 + 本地 python sqlite3 核验真实存储值。

## 验证结果

### ✅ Item 1 — 深链落地（全部 PendingDeepLink 分支）

| 分支 | 投递 | 结果 |
|---|---|---|
| Reimbursement | `--ei extra_reminder_target 2` | 落地「待报销」屏（「共 0 笔，合计 ¥0.00」「无记录数据」）✅ |
| MyAsset（快捷方式） | `--ei shortcuts_type 1` | 落地「我的资产」屏 ✅ |
| AssetInfo | `--ei extra_reminder_target 1 --el extra_reminder_asset_id 1` | 落地「其它信用卡OCC」资产详情（账单日 27日）✅ |

`parsePendingDeepLink` 三类输入 → 对应路由导航均正确。AddRecord 分支（`shortcuts_type 0`）未单独黑盒，机制同上 + 已被 `PendingDeepLinkTest` 单测覆盖。

### ✅ Item 1 — 一次性消费（核心 bug 修复）

- 深链落地「待报销」→ 返回首页 → **旋转屏幕触发 Activity 重建**（onCreate 重读 `getIntent()`）→ **仍停在首页（月收入/月支出/月结余/2026-06），未弹回待报销** ✅。
- 证实 `onConsumePendingDeepLink` 已清除 intent extra，recreate 重解析得 `None`、不再导航。这正是旧 sticky 状态会弹回的场景（旧实现深链值持于 MainActivity mutableState、消费后从不复位）。

### ✅ Item 2 — DailyReminderWorker.doWork 端到端运行 + N1 通知投递

经 `InitWorker` 的 `_OneTime` 启动补查 work（`ExistingWorkPolicy.REPLACE`，每次启动立即执行；周期 work 带 initialDelay 到 10:00、计划前 force-run 被 WorkManager 拒绝重排，故走补查路径）：

1. 建信用卡「其它信用卡OCC」（DB 核验 `classification=10` OTHER_CREDIT_CARD → `isCreditCard=true`，`billing_date='27'`=今天，`books_id=1`，`invisible=0`）。
2. 设置页开「信用卡账单/还款提醒」开关（state=checked，持久化 proto）。
3. force-stop + 重启 → 补查 `doWork()` 运行（logcat `CASHBOOK-DailyReminderWorker: doWork(), reminder check`）。
4. **投递通知 id `20018`**（= `ReminderNotificationBaseId(20016) + assetId(1)×2`，CreditCardBilling），内容 **`其它信用卡OCC 今日账单已出`**（`R.string.reminder_credit_billing` + 资产名），`mPackageVisibility=-1000`（VISIBILITY_PRIVATE）✅。

此链路端到端跑通**本次重构的 `reminderRun`（区间→逐日 computeReminders→checkpoint）+ `toNotificationSpec`（文案/深链目标映射）** 在真机上的产出，结果与单测一致。N1/N2 功能此前从未真机验证，本次首次确认 N1 通知真实投递。

### ✅ Item 2 — 信用卡过滤正确性（反向佐证）

首次误建的资产经 `信用卡→选招商银行` 存为 `classification=24`（BANK_CARD_ZS，`isCreditCard=false`），doWork 运行但 `reminderRun` 产出**空 items、无通知**——worker 的 `filter { it.classification.isCreditCard }` 正确排除非信用卡资产。

## 未在真机完成（已有等价覆盖）

- **真实通知点击 → 深链落地**：下拉通知栏后 `android layout`/`uiautomator dump` 挂起、模拟器随后冻结 offline，未能点击通知 widget。但该通知的 `contentIntent` = `reminderDeepLinkIntent(REMINDER_TARGET_ASSET, assetId=1)`，与上文 **AssetInfo 深链测试（已 ✅）投递的 Intent 完全相同**，功能等价已验证。
- **安全门（needVerity）门控**：模拟器冻结前未完成应用锁设置。已由节点 2 安全审计独立确认正确（导航+消费全在 `if (!needRequestProtocol && !needVerity)` 内、锁未过 NavHost 不组合 navController 无图可导航）。

## 观察（N1 功能侧，非本次 followup 改动，待确认）

`信用卡→选具体银行（如招商银行）` 创建的资产存为 `BANK_CARD_<bank>` 分类（招商=24），`isCreditCard=false`，故 N1 提醒**不会对其触发**；但 EditAsset 对该资产**显示了账单日/还款日字段**。即「带账单/还款日的银行品牌卡」可能收不到提醒。此为 N1/资产分类模型行为，**非本次 followup（Item 1/2）所改**，建议作为 N1 backlog 进一步确认是否预期。

## 结论

Item 1（深链一次性消费）核心行为变更 + Item 2（doWork 重构）的真机端到端均验证通过，并首次确认 N1 通知真实投递。剩余两项（真实通知点击、安全门）有等价覆盖（同 Intent 已验 / 节点 2 安全审计）。

环境踩坑：① 通知栏下拉态使 `android layout`/`uiautomator dump` 挂起、可致模拟器冻结，避免在 shade 展开态做 layout dump；② Google Play 镜像无 root，无法拨系统时钟或强制周期 worker，须借 `_OneTime` 补查 work 触发 doWork；③ DoKit 浮窗占左上拦截菜单图标，用左缘右滑打开导航抽屉绕过；④ `信用卡→招商银行` 存 BANK_CARD_ZS 分类（建测试信用卡用「其它信用卡」避免银行选择器、确保 isCreditCard=true）。
