# 提醒通知设计：信用卡账单/还款提醒（N1）+ 待报销提醒（N2）

> 创建于 2026-06-26。对应待办 N1（信用卡账单日/还款日通知提醒）+ N2（待报销记录通知提醒）。
> 两者共建一套通知基础设施，本 spec 一次性设计、后续分别实施。
> **v2（2026-06-26）**：纳入节点 1 四维评审 15 条 finding 处置 + 用户对 3 个 High 决策点的拍板（补发机制 / 维持当前账本+可见卡 / 默认关+轻量引导）。

## 1. 背景与目标

- **N1**：为信用卡资产的账单日、还款日添加本地通知提醒，避免用户忘记看账单 / 逾期还款。
- **N2**：为待报销列表添加定期通知提醒，提示用户存在未处理的可报销支出。

**关键既有事实（已 hands-on 核查）**：

- 通知权限已声明：`sync/work/src/main/AndroidManifest.xml:19-20` 含 `POST_NOTIFICATIONS` + `FOREGROUND_SERVICE_DATA_SYNC`（merge 进 app）。
- 已有通知基础设施：`sync/work/.../initializers/SyncWorkHelpers.kt` 定义 3 个渠道（Sync:36 / Notice:81 / Upgrade:114）；`WorkManagerAppUpgradeManager.kt:112` 已用 `noticeNotificationBuilder()` + `nm.notify(NoticeNotificationId, …)` 发提醒，为可复用发送范例。其 PendingIntent 用 `FLAG_CANCEL_CURRENT or FLAG_IMMUTABLE` + ``package` = ApplicationInfo.applicationId` 锁定收件方（`SyncWorkHelpers.kt:135,142`）。
- WorkManager 周期任务范例：`AutoBackupWorker.kt`（HiltWorker + `PeriodicWorkRequestBuilder<DelegatingWorker>`:79 + `setInputData(::class.delegatedData())` + 顶层纯函数 `mapBackupStateToResult`:95）。**周期任务实际在 `InitWorker.doWork():66-103` 内用 `enqueueUniquePeriodicWork(name, ExistingPeriodicWorkPolicy.UPDATE, …)` 注册并受 `appSettingsModel` 开关 gate**（非在 `Sync.initialize()`）。
- `Sync.initialize()`（`SyncInitializer.kt:32-41`）仅 enqueue 一次性 `InitWorker`；唯一调用方 `CashbookApplication.kt:106`。
- 信用卡字段：`AssetTable.billingDate`/`repaymentDate`（`core/database/.../table/AssetTable.kt:59-60`，**非空 String**，如 `"15"`，信用卡专用）。
- 信用卡判别：`AssetClassificationEnum.isCreditCard`（`core/model/.../enums/AssetClassificationEnum.kt:83`），`AssetModel.classification.isCreditCard` 可用；`AssetModel` 自带 `billingDate`/`repaymentDate`。
- 当前账本**可见**资产：`AssetRepository.currentVisibleAssetListData: Flow<List<AssetModel>>`（`AssetRepositoryImpl.kt:51-54` = `getVisibleAssetsByBookId(currentBookId)`，随当前账本变化）。**隐藏资产在 `currentInvisibleAssetListData`:79 另一流**——本设计 N1 仅用可见流（见 §9 已知限制）。
- 待报销数据：`RecordRepository.getReimbursableUnrelatedRecordList()`（core:data，**无参**，`RecordRepositoryImpl.kt:429` 内部 `recordSettingsData.first().currentBookId` 自取当前账本，NOT EXISTS + `reimbursed=0` 过滤）；Worker 无需显式拿 currentBookId。
- 设置链路（每加一个字段贯穿 **4 个构造站点**）：`app_settings.proto` → `AppSettingsModel`（`core/model`）→ 构造站点 ①`CombineProtoDataSource.kt:70`（读映射 + writer）②`FakeSettingRepository.kt:36`（`core/testing`）③`FakeCombineProtoDataSource.kt:45`（`core/data` test 数据源）；接口实现 2 处：`SettingRepositoryImpl.kt:59`（setter 委托 `combineProtoDataSource.updateXxx`）+ `FakeSettingRepository.kt:33`。
- `sync/work/build.gradle.kts:39` 依赖含 `core:data`，**不含 `core:domain`** → N2 走 core:data，无需新增 domain 依赖。
- 月起始日 D：`RecordSettingsModel.monthStartDay`（`record_settings.proto`，spec1 已实现，范围 1–28，默认 1）；经 `SettingRepository.recordSettingsModel`:42 第二次 `.first()` 读取。
- **安全门**：`app_settings.proto:5 needSecurityVerificationWhenLaunch`；`MainApp.kt:378` `if (needVerity)` 为真时只渲染 `Verification`(:380)、**不 compose `CashbookNavHost`**(:392)；现有 App Shortcut 深链在 `MainApp.kt:227-231` 的 `LaunchedEffect` 内、受 `if (!needRequestProtocol && !needVerity)`(:230) 门控导航（`SHORTCUTS_TYPE_ADD`/`SHORTCUTS_TYPE_ASSET`，常量 `MainApp.kt:62-63`）。MainActivity 为 exported launcher（`AndroidManifest.xml`，`launchMode=singleTask`，仅 MAIN/LAUNCHER intent-filter，**无 VIEW/deeplink**）。

## 2. 需求决策（用户已拍板）

| 维度 | N1 信用卡 | N2 待报销 |
|---|---|---|
| 触发时机 | 账单日 / 还款日**当天** | **每月 D 号**（D = `monthStartDay`，默认 1） |
| 触发条件 | 当前账本**可见**信用卡，今日 day-of-month == billingDate / repaymentDate | 当前账本待报销笔数 > 0 |
| 账本/可见范围 | **仅当前账本 + 仅可见卡**（已知限制见 §9） | 仅当前账本 |
| 开关 | 全局 bool（默认 **关**） | 全局 bool（默认 **关**） |
| 提醒时刻 | 每日 **10:00** | 每月 D 号 10:00 |
| 漏发处理 | **补发机制**（见 §5.3） | **补发机制**（见 §5.3） |
| 可发现性 | 默认关 + 持卡用户轻量一次性引导（见 §7） | 默认关 + 有待报销用户轻量一次性引导 |

## 3. 架构与模块改动

无新建 feature 模块（除设置开关外无新界面）。

| 层 | 改动 |
|---|---|
| `core:datastore-proto` | `app_settings.proto` 加 `bool creditCardReminderEnable = 26;`、`bool reimbursementReminderEnable = 27;`、`sint64 lastReminderCheckMs = 28;`（补发用） |
| `core:model` | `AppSettingsModel` 加 3 字段（末位，**均给 Kotlin 默认值** `= false` / `= 0L`） |
| `core:datastore` | `CombineProtoDataSource` 读映射 3 字段 + 3 个 writer |
| `core:data` | `SettingRepository`/`Impl` 加 3 个 `updateXxx`；新增**纯函数**判定提醒；引导状态读写 |
| `core:testing` | `FakeSettingRepository`（含接口实现）同步 3 字段 + 默认值 |
| **`core:data` test** | **`FakeCombineProtoDataSource` 同步 `AppSettingsModel` 构造 + 3 个 `updateXxx`**（节点 1 finding#4，漏改即 `:core:data:testDebugUnitTest` 编译失败） |
| `sync/work` | 新增 `DailyReminderWorker`（HiltWorker，经 `DelegatingWorker` 委派）+ `ReminderNotificationChannel` helper + 深链 PendingIntent；周期任务注册**对齐 `InitWorker.doWork()` 既有模式**；worker 名常量入 `SyncInitializer.kt` |
| `app` | `MainActivity` `onCreate`/`onNewIntent` 解析提醒深链 Intent extra → 经 `MainApp.kt:227` 受 `needVerity` 门控的 `LaunchedEffect` 导航（复用 shortcutsType 机制，新增提醒目标类型常量）；**禁新增导出 `<deepLink>` intent-filter** |
| `feature:settings` | 设置页「提醒」分组 2 个 `Switch` + monthStartDay 耦合说明文案；首次开启请求 `POST_NOTIFICATIONS` |
| 引导入口 | 资产页（持信用卡）/ 待报销页（有记录）一次性轻量提示（引导已展示标记存 DataStore） |

## 4. 数据模型

`app_settings.proto`（接续字段号 25）：

```proto
bool creditCardReminderEnable = 26;       // 信用卡账单/还款提醒开关
bool reimbursementReminderEnable = 27;    // 待报销提醒开关
sint64 lastReminderCheckMs = 28;          // 上次提醒检查日期(epoch ms)，0=从未；用于补发
```

- proto3 标量默认（bool=false / sint64=0）→ 存量用户升级后默认关、lastCheck=0，向后兼容，**无 Room migration**（Proto DataStore 非 Room）。
- `AppSettingsModel` 新增 3 字段置末位 **且写 Kotlin 默认值**（`= false` / `= 0L`）→ 3 个命名参数构造站点可不改即编译；但 `CombineProtoDataSource.kt:70` 读映射**必须**新增 3 行 `xxx = it.xxx`，否则 proto 值永不传播（静默 bug，非编译错）。引导已展示标记另存（DataStore，键值），不入 AppSettings。

## 5. 调度设计（方案 A：单一每日 Worker + 补发）

### 5.1 注册（对齐 InitWorker 既有模式）
在 `InitWorker.doWork()`（现有周期任务集中注册处）追加：

```
WorkManager.enqueueUniquePeriodicWork(
    ReminderWorkName,
    ExistingPeriodicWorkPolicy.UPDATE,   // 与现有 Sync/AutoBackup 一致；UPDATE 重算 initialDelay 仍指向下一个 10:00，且让存量用户能拿到后续 schedule 修复
    DailyReminderWorker.startUpPeriodicReminderWork(),  // PeriodicWorkRequestBuilder<DelegatingWorker>(1天) + initialDelay 对齐下一个 10:00 + setInputData(DailyReminderWorker::class.delegatedData())
)
```

- 始终注册（不随开关增删 Worker）；两开关全关时 Worker 内直接 `success()` 空转。
- 另在注册时 enqueue 一次性 `OneTimeWorkRequest<DelegatingWorker>`（同 worker）立即跑一次**启动补查**（见 §5.3），消"app 启动时补发遗漏"。
- 选 `UPDATE`（非 spec v1 的 KEEP）：与现有约定一致，且每次启动重算的 initialDelay 仍对齐"下一个 10:00"不会丢日，同时修复了 v1 的"KEEP 致存量拿不到 schedule 修复"风险（finding#10）。

### 5.2 Worker 逻辑（薄壳 + 纯函数）
`DailyReminderWorker.doWork()`：

1. 读 `appSettingsModel.first()`（3 字段）+ `recordSettingsModel.first().monthStartDay`；两开关全关 → 直接 `success()`。
2. N1 开 → `currentVisibleAssetListData.first()` 过滤 `isCreditCard`；N2 开 → 需要时 `getReimbursableUnrelatedRecordList()` 取笔数。
3. 计算补查区间（§5.3）；对区间内每个逻辑日期调纯函数 `computeReminders(...)`。
4. 对每个 `ReminderItem` 发通知；更新 `lastReminderCheckMs = today`；`success()`。

### 5.3 补发机制（finding#1，用户决策）
- `lastReminderCheckMs` 记录上次成功检查日期。Worker/启动补查时：
  - `from = max(today - 上限N天, lastCheck.date + 1)`，`to = today`（首次 lastCheck=0 → from=today，**不补历史**避免首启轰炸）。
  - **补查上限 N=7 天**（设备长期关机后只补最近 7 天，更早丢弃，防通知轰炸）。
  - 对 `[from..to]` 每个 date 调 `computeReminders(date, …)`，发该日命中的提醒。
- 同一逻辑日期的提醒仅在该日被处理一次（区间逐日 + 更新 lastCheck 后不重叠），避免重复发。
- N2 每月 D 号同理：若 D 号当天 Worker 漏跑，下次（≤7 天内）补查区间覆盖到 D 号则补发。

### 5.4 提醒判定纯函数（呼应项目惯例 + `mapBackupStateToResult` 范例）
`computeReminders(date, creditCards, monthStartDay, settings, reimbursableCount): List<ReminderItem>` —— top-level `internal fun` 纯函数（入参全值类型，无 Android/IO 依赖），纯 JVM 单测。
- N1：对每张卡 `billingDate`/`repaymentDate` **`toIntOrNull()` + 范围校验**（finding#8），单卡解析失败/越界**跳过该卡**，不中断整批；用 `java.time` 判断 `date.dayOfMonth == day`。
- 月末语义（finding#9）：日期选择器 UI 仅可选 1–30（`EditAssetScreen.kt:521`，"31" 不可达），但导入/恢复数据仍可能含越界值——纯函数对"当月无该日"（如 "30" 在 2 月）的语义为**跳过**（不顺延），单测覆盖 29/30 在 2 月/闰年。
- N2：`date.dayOfMonth == monthStartDay` 且 `reimbursableCount > 0`。

### 5.5 待 plan 阶段 PoC 验证
- 【需 plan PoC 验证】WorkManager `PeriodicWorkRequest` 不保证精确时刻/送达（Doze/OEM 省电）；本设计以**补发机制**（§5.3）缓解漏发，PoC 验证补查逻辑在跨午夜漂移/漏跑日的覆盖正确性，而非时刻精度。
- 跨午夜漂移（finding，Medium）：因补查按"逻辑日期区间"而非"仅今天"，跨午夜漂移不会丢日（区间会覆盖）。

## 6. 通知设计

- **新增独立渠道** `ReminderNotificationChannel`（ID 常量入 `SyncWorkHelpers.kt`）。`IMPORTANCE_DEFAULT`，渠道名/描述走 string 资源；**渠道 `setLockscreenVisibility(VISIBILITY_PRIVATE)`**（finding#5）。
- **锁屏隐私**（finding#5）：通知 `setVisibility(VISIBILITY_PRIVATE)` + `setPublicVersion()` 提供脱敏公开版（如"您有一条记账提醒"）；锁屏不直接暴露卡名/笔数。
- 文案（string 资源，**卡名作 `getString(R.string.xxx, cardName)` 实参**，绝不当格式串，防 `%` 致 `IllegalFormatException`，finding#16）：账单「{卡名} 今日账单已出」/ 还款「{卡名} 今日还款，别忘了」/ 待报销「你有 {N} 笔支出待报销」。
- **PendingIntent**（finding#6）：`PendingIntent.getActivity(..., FLAG_IMMUTABLE)` + **显式组件 Intent** `Intent(context, MainActivity::class.java)`（非隐式 action），沿用 `SyncWorkHelpers.kt` 成熟写法。
- **深链导航 + 安全门**（finding#3、#7）：Intent extra 携带「提醒目标类型 + assetId」→ `MainActivity.onCreate`/`onNewIntent`（singleTask 热启走 onNewIntent）解析 → 经 `MainApp.kt:227` 那个**受 `!needRequestProtocol && !needVerity` 门控**的 `LaunchedEffect` 消费导航（复用 shortcutsType 机制，新增提醒类型常量）。N1 → 资产详情（复用/扩展 `SHORTCUTS_TYPE_ASSET`）；N2 → 待报销列表。**禁新增 Navigation Compose 原生 `<deepLink>` intent-filter**（既绕安全门又开放导出攻击面）。深链作**独立交付项**，含冷启/热启两路手测。
- **通知分组**（finding#14）：notificationId 每卡每事件用资产 id 派生、N2 固定 id（避免覆盖/retry 重发）；多条用 `NotificationCompat` group + summary 折叠，防同日通知风暴。

## 7. 设置 UI 与引导

- `feature:settings` 设置页新增「提醒」分组，2 个 `Switch`：信用卡账单/还款提醒、待报销提醒。
  - 待报销开关下方**显式说明 monthStartDay 耦合**（finding#13）：「待报销提醒在每月记账起始日发出」。
- 开启时（Android 13+）经 `rememberLauncherForActivityResult(RequestPermission)` 请求 `POST_NOTIFICATIONS`；拒绝则开关仍可置开（记录意图），系统不展示——不强制、不阻断。`nm.notify` 无权限时静默 no-op、不崩。
- **轻量一次性引导**（finding#11，用户决策）：对持有信用卡资产的用户（资产页）/ 有待报销记录的用户（待报销页）做一次性轻量提示「可开启提醒」，点击跳设置「提醒」分组；引导已展示标记存 DataStore，仅展示一次。
- 若设置页开关经 `SettingViewModel`，须同步 `SettingViewModelTest` + settings 截图基线（CLAUDE.md 强制，finding）。

## 8. 测试策略

| 对象 | 测试 |
|---|---|
| `computeReminders` 纯函数 | 纯 JVM 单测：账单/还款命中与否；**真实可达日期值**（29/30 在 2 月/闰年，非 "31"）；脏值/越界/空/非数字 single-card 跳过；单卡 / 多卡 / 无卡；N2 命中 D / 笔数=0 不发；开关组合 |
| 补发区间逻辑 | 纯 JVM 单测：lastCheck=0 不补历史；漏 1–7 天补查覆盖；超 7 天上限只补最近 7 天；跨午夜/跨月边界 |
| `SettingRepositoryImpl` 3 setter | 复用现有 setter 测试模式（含 `FakeCombineProtoDataSource` 接线） |
| `CombineProtoDataSource` 映射 | 沿用现有 proto 字段映射测试 |
| 设置页开关 + 引导 | feature:settings 截图测试 + ViewModelTest（若 ViewModel 介入） |
| `DailyReminderWorker` | 薄壳，逻辑覆盖靠纯函数；不强求 instrumented |
| 深链导航 | 冷启/热启两路真机/模拟器手测（含开启安全验证后先弹密码/指纹门再落地） |

## 9. 非目标 / YAGNI / 已知限制

**非目标**：per-资产提醒开关；提醒提前 N 天（仅当天）；通知点击后复杂二级交互。

**AlarmManager**（finding#1 备选）：用户选补发机制替代，**不引入 AlarmManager**（避免 `SCHEDULE_EXACT_ALARM` 受限权限 + 重启重注册）。

**已知限制（用户拍板维持，spec 显式记录）**：
- **N1 仅扫描当前账本 + 仅可见信用卡**（finding#2/#12）。多账本用户切到别的账本、或隐藏了信用卡时，该卡 N1 提醒不触发。理由：与待报销/统计页"当前账本"语义一致、查询最简。代价：信用卡真实还款义务的提醒可靠性受"当前账本 + 可见性"UI 状态影响——多账本/隐藏卡用户需知悉此限制。
- WorkManager 为尽力而为调度，即便有补发机制，极端情况下（设备连续 >7 天关机）仍可能漏发；"当天/当月"为尽力而为、不作硬保证。

## 10. 风险与缓解

| 风险 | 缓解 |
|---|---|
| WorkManager 漏触发/Doze/OEM 省电 | 补发机制（§5.3）+ 启动补查；plan PoC 验证补查覆盖 |
| 跨午夜漂移改变检查日 | 补查按逻辑日期区间，不丢日 |
| 月末日期无对应日 | 纯函数 `java.time` + per-card 校验，语义=跳过；单测覆盖 |
| 脏值（导入/恢复/旧数据）致整批不发 | per-card `toIntOrNull` + 范围校验，单卡失败跳过 |
| 深链绕过安全验证 / 导出攻击面 | 复用受 needVerity 门控的 shortcutsType 模式，禁导出 deepLink |
| 锁屏泄露金融隐私 | VISIBILITY_PRIVATE + 脱敏 publicVersion |
| PendingIntent 崩溃/劫持 | FLAG_IMMUTABLE + 显式组件 Intent |
| AppSettingsModel 加字段破坏构造点 | 字段末位 + Kotlin 默认值；编译期全量检查 4 构造站点（含 FakeCombineProtoDataSource） |
| 同日通知风暴 | group + summary 折叠 |
| 功能默认关无人发现 | 轻量一次性引导 |

## 11. 实施 Phase 划分（产物导向，不标工时）

- **Phase 1 — 数据层开关 + 补发字段**：proto 3 字段（26/27/28）+ AppSettingsModel（Kotlin 默认值）+ CombineProtoDataSource 映射/writer + SettingRepository/Impl 3 setter + **FakeSettingRepository + FakeCombineProtoDataSource** 同步。单会话产物，含 setter/映射单测。
- **Phase 2 — 提醒判定 + 补发纯函数**：`computeReminders` + `ReminderItem` + 补发区间计算（top-level internal fun）+ 纯 JVM 单测（日期边界/脏值/补发区间全覆盖）。位置：`core:data` 或 `sync/work`（plan 定，倾向便于纯 JVM 测处）。
- **Phase 3 — Worker + 通知 + 调度**：`DailyReminderWorker`（DelegatingWorker 委派）+ `ReminderNotificationChannel`（VISIBILITY_PRIVATE）helper + `InitWorker` 注册（UPDATE）+ 启动补查 OneTime + string 资源 + 通知 group/summary。
- **Phase 4 — 深链导航**（独立交付项）：MainActivity onCreate/onNewIntent 解析 + 提醒类型常量 + 经门控 LaunchedEffect 导航 + PendingIntent（FLAG_IMMUTABLE+显式 Intent）。冷/热启手测 + 安全门验证。
- **Phase 5 — 设置 UI + 权限 + 引导**：feature:settings「提醒」分组 2 开关 + monthStartDay 耦合说明 + POST_NOTIFICATIONS 请求 + 截图基线 + ViewModelTest；持卡/有待报销用户轻量一次性引导。
- **Phase 6 — 验收**：跨模块编译（`:app:compileOnlineDebugKotlin`）+ 全量相关单测 + spotless + lint + 节点 2 full-review；模拟器 journey 黑盒（开关→改信用卡日期为今日→触发 Worker→验通知 + 深链落地 + 安全门）。

> 依赖：Phase 1 → 2 → 3；Phase 4 依赖 Phase 3 通知；Phase 5 依赖 Phase 1 开关；Phase 6 收口。
