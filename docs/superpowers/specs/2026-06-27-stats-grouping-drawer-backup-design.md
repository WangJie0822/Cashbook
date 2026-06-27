# 统计页日期头按周期自适应 + 首页抽屉返回键修复 + 备份恢复真机验证 设计

- 日期：2026-06-27
- 状态：设计已定稿（节点 1 四维评审完成）
- 范围：`feature:records`（日期头）、`feature:settings`（抽屉）、真机验证（android-cli + 备份 zip）

## 背景与问题（均带实证）

### Issue 1：分类/资产/标签统计页周期切「全年」等仍按天分组、与首页不一致
- 首页 `LauncherContentScreen.DayHeaderItem`（`feature/records/.../screen/LauncherContentScreen.kt:569-631`，日期文案 `:586-593`）按 `DateSelectionTypeEnum` 自适应：按月/日→`27日`；全年→`6月27日`；区间/全部→`2026年6月27日`。
- 三个统计页（分类/标签 `TypedAnalyticsScreen`、资产 `AssetInfoContentScreen`）共用 `RecordDayHeader`（`feature/records/.../view/RecordMonthSummaryHeader.kt:221-240`），**恒渲染 `27日`**、无月/年上下文。
- 底层 `recordDaySeparator`（`feature/records/.../viewmodel/RecordDayGrouping.kt`）首页与统计页**都按日分组**——差异**只在日期头渲染**，不在分组逻辑。
- 结论：「和首页一致」= 让统计页日期头也按周期自适应；**保持按日分组**（用户已确认采「仅日期头标签自适应」）。

### Issue 2：首页抽屉显示时按返回键不收起、无反应
- 抽屉是 `feature/settings/.../screen/LauncherScreen.kt` 的 `ModalNavigationDrawer`（**左侧**导航抽屉，汉堡菜单触发；用户描述「右侧」与代码不符，设备复现时确认）。
- `LauncherScreen.kt:137-139` **已有** `BackHandler(enabled = drawerState.isOpen) { onRequestDismissDrawerSheet() }`，静态读码逻辑自洽，**未定位到确定性 bug**。
- 全仓 `BackHandler` 仅 2 处（`LauncherScreen` 与 `MyAssetScreen.kt:104`，enabled 条件互斥），`MainActivity`/`MainApp` 无竞争 BackHandler。
- **根因待设备实证**，不预设 `LauncherScreen` 即问题点。

### Issue 3/4：真机验证 + 验证新增的备份恢复功能
- 测试数据：`D:\jiewang41\Downloads\Cashbook_Backup_File_20260627164133.zip`。
- **已核验**：该 zip **仅含 `cashbook.db`（21MB）、无 manifest/settings/record_images**（comment `Online,false,cn.wj.android.cashbook,v1.2.0_26062514_online,14`）→ 它是 **legacy db-only 备份**，直接恢复只走旧路径（`BackupRecoveryManagerImpl.kt:762-764` manifest 缺省=版本 1），**不覆盖新增的图片/settings/manifest 备份功能**。

## 目标

1. 统计页（分类/资产/标签）日期头按周期自适应，与首页一致；并顺手修复「可配置月起始日跨自然月时月视图丢月份」（首页+统计页）。
2. 修复首页抽屉返回键不收起（先设备复现根因，再最小修复）。
3. 真机验证三项功能正常 + 端到端验证新增备份恢复功能（含图片/settings/manifest 新格式 round-trip）。

## 非目标（YAGNI）

- 不改 `recordDaySeparator` 底层按日分组逻辑。
- 不给统计页日期头加每日收入/支出小计（用户选「仅标签自适应」）。
- 不构造恶意 zip 验证安全防护（zip-slip/解压炸弹/版本门已有 JVM 单测覆盖）。
- 不做无关重构。

## 设计

### Issue 1 — 日期头按周期自适应（单一真源）

**纯函数**（feature/records top-level `internal fun`，纯 JVM 可单测，仿 `RecordDayGroupingTest` 的 JUnit+Truth）：

```
internal fun recordDayHeaderDateText(
    type: DateSelectionTypeEnum,
    year: Int, month: Int, day: Int,
    dayTypeSuffix: String,                 // 已解析的「(今天)/(昨天)/(前天)」或 ""
    dayLabel: String, monthLabel: String, yearLabel: String,  // 已解析的 日/月/年
    byMonthCrossesNaturalMonth: Boolean,   // BY_MONTH 周期是否跨自然月（monthStartDay≠1）
): String
```

分支（保持与首页现有 `:586-593` 逐字一致，仅新增 BY_MONTH 跨月子分支）：
- `BY_DAY` → `"${day}${dayLabel}${suffix}"`
- `BY_MONTH` → 跨自然月 `"${month}${monthLabel}${day}${dayLabel}${suffix}"`；否则 `"${day}${dayLabel}${suffix}"`
- `BY_YEAR` → `"${month}${monthLabel}${day}${dayLabel}${suffix}"`
- `DATE_RANGE` / `ALL` → `"${year}${yearLabel}${month}${monthLabel}${day}${dayLabel}"`（无 suffix，沿用首页现状）

> **F3 约束**：纯函数**不调 `stringResource`**（@Composable API）。`stringResource` 与 `dayTypeSuffix`、`byMonthCrossesNaturalMonth` 的解析/计算放 `@Composable` 薄壳，结果作为字符串/布尔传入纯函数。

**接线**：
- `RecordDayHeader` 新增 `dateSelectionType: DateSelectionTypeEnum` + `byMonthCrossesNaturalMonth: Boolean` 参数，调用纯函数；`TypedAnalyticsScreen`/`AssetInfoContentScreen` 传 `dateSelection.type` + 计算的跨月标志。
- 首页 `DayHeaderItem` 也改用同一纯函数（单一真源；BY_DAY/BY_MONTH 非跨月分支输出与现状逐字一致 → 首页基线 0 diff）。
- **仅共享日期文案**，首页 `DayHeaderItem`（Row + 日收支行）与统计页 `RecordDayHeader`（Column + Divider）**布局各自保留**。

**F7 跨自然月**：`byMonthCrossesNaturalMonth = (dateSelection is ByMonth) && normalizeMonthStartDay(monthStartDay) != 1`。各屏 VM 已持有 `monthStartDay`（`LauncherContentViewModel` `_monthStartDay`、`TypedAnalyticsViewModel:92`、`AssetInfoContentViewModel:85`）；plumb 到对应屏（具体暴露方式由 plan 定，倾向 VM 暴露派生布尔或 `monthStartDay`，避免屏内重复逻辑）。

**F8（保留现状+注明）**：BY_YEAR 带 suffix、DATE_RANGE/ALL 不带——单一真源后首页与统计页天然一致，KDoc 注明此为沿用首页既有口径，本次不统一。

**测试**：
- 纯函数单测：5 种 type × dayType(0/-1/-2/1) × BY_MONTH 跨月/不跨月，断言文案与 suffix placement。
- 截图：**8 张 fixedPeriod 基线必变**（`AssetInfoContentScreen`+`TypedAnalyticsScreen` 各 4 张，用例 `ByYear(2024)`，`27日`→`1月15日`）。按 CLAUDE.md「Roborazzi 基线 CI 管理」——本地只提交代码、不录不判，PR 由 `recordRoborazziDevDebug` 重录。可补一条统计页 BY_MONTH 跨月截图用例。

### Issue 2 — 首页抽屉返回键（根因先实证）

**步骤**：
1. 模拟器 systematic-debugging **先复现**（开抽屉→`keyevent KEYCODE_BACK` + 手势返回→观察是否收起 + logcat），定位根因；确认「右侧抽屉」指代码里的左侧导航抽屉。
2. 根因确认后施**最小修复**。候选方向（复现后定，**不预设**）：
   - `BackHandler(enabled = …)` 的 enabled 用**意图源** `shouldDisplayDrawerSheet`（或 `drawerState.isOpen || drawerState.targetValue == DrawerValue.Open`），规避 `drawerState.isOpen` 读 settled `currentValue` 在开动画窗口为 Closed 致 BackHandler 被禁用的可能【推测，待实证】；
   - 或定位到状态机/Material3 内置 predictive-back 竞争后对症修。
3. **硬约束（F4）**：修复**必须仍经** `onRequestDismissDrawerSheet()`（VM 为唯一意图源），**禁止**在 UI 里直接 `drawerState.close()` 而不回写 `shouldDisplayDrawerSheet`——否则 VM 仍为 true、`LaunchedEffect(shouldDisplayDrawerSheet)` 因 key 未变不重发，导致「菜单再开抽屉失效」新回归。
4. 把最小修复限制在 `LauncherScreen` 函数体内，**避免签名漂移**（否则连带 4 处 `LauncherScreenScreenshotTests` + `SettingsNavigation` + app）。

**测试（F6）**：补 Robolectric Compose 交互回归测试：渲染/打开抽屉→back→断言触发 `onRequestDismissDrawerSheet`/抽屉收起，且**关闭后再 `displayDrawerSheet` 能重新打开**（直击 F4 脱同步路径）。

### Issue 3/4 — 真机验证（F1 闭环 + F2 脱敏）

**环境**：android-cli 启模拟器（`Medium_Phone`，API34 `bp_api34` 备选，防本机 boot 挂死）；装**非 Dev** Debug 包（如 `assembleOnlineDebug`/`OfflineDebug`，使恢复缓存自动清 `BackupRecoveryManagerImpl.kt:844`）。

**备份恢复验证（F1 闭环，关键）**：
1. `adb push` zip 到可授子目录 → app 备份恢复入口（`OpenDocumentTree` SAF 选目录，半手动 tap）恢复 → 验 **legacy db-only 旧路径**。
2. 启动 → 触发 BLOB→file 图片 backfill（`recordRepository.backfillImagesToFiles()`）+ finalAmount 净自付重算。
3. 验图片可显示（双轨读 File 优先/bytes 回退）。
4. **app 内自身再产一个新格式备份**（含 manifest+settings.json+record_images/）。
5. 恢复该新备份 → **端到端验证新增备份恢复功能 round-trip**。
   > 若 db 无图片记录，新备份无 record_images/，则该项仅验 manifest+settings；报告如实标注。
   > 若被迫用 `run-as` 直推 db 绕过恢复代码路径，须在报告标注「未经恢复代码路径、不算备份功能验证」。

**功能验证**：
- Issue 1：进分类/资产/标签统计 → 周期切全年/全部 → 日期头显示带月/年上下文；若设了非自然月起始日，月视图头带月份。
- Issue 2：首页开抽屉 → keyevent + 手势 back → 抽屉收起（F9：报告标注导航模式）。

**F2 脱敏（强制）**：journey 报告**强制脱敏**——不贴真实金额/资产名/备注/收据图、不贴 DB 原始行、截图打码；DB dump/zip/图片**只放** gitignored `docs/testing/reports/evidence/`（已确认 `.gitignore:111`）；验完 `adb shell pm clear` + 重置/删除 AVD，使真实备份不残留。报告 `.md` 是 git 跟踪文件、随公开仓库分发。

**产物**：脱敏 journey 报告 `docs/testing/reports/2026-06-27-stats-grouping-drawer-backup-journey.md`。

## 节点 1 四维评审 finding 与处置

| # | 维度 | 级别 | finding | 处置 |
|---|---|---|---|---|
| F1 | feas+reverse | High（验证范围） | zip 仅 db、无新格式 entry，恢复只走 legacy v1 | 验证改自产自验闭环（恢复旧→app 产新→恢复新） |
| F2 | security | High（流程） | 公开仓库恢复真实财务备份 + git 跟踪报告→PII 泄露 | 报告强制脱敏 + 产物入 gitignored evidence + 非 Dev flavor + pm clear/重置 AVD |
| F4 | reverse+impact | High | Issue 2 根因未确立；修复绕过 VM 会破坏菜单开抽屉 | 根因先实证、修复仍经 VM、不预设问题点 |
| F3 | feas+reverse | Medium | 纯函数不能调 stringResource；恢复入口是 SAF 选目录、API30+ 不可授外存写 | 纯函数收预解析串、本地化放薄壳；journey 接受半手动 SAF |
| F5 | impact | Medium | 改动 1 致 8 张截图基线变；资产详情页同步变更 | intended（资产是点名三页之一）；CI 重录、PR 标注 |
| F6 | impact | Medium | Issue 2 修复无自动化回归守护 | 补 Robolectric 交互测试 |
| F7 | reverse | Medium | 月起始日≠1 时 ByMonth 跨自然月、月视图丢月份 | 用户选「顺手一起修」首页+统计页 |
| F8 | reverse | Low | BY_YEAR 带 suffix、RANGE/ALL 不带 | 保留现状、KDoc 注明 |
| F9 | reverse | Low | keyevent 与 predictive-back/手势返回派发不同 | journey 测 keyevent+手势、标导航模式 |
| F10 | security | Low | happy-path 恢复不验恶意输入防护 | 不当安全验证、不重复造恶意 zip |
| — | security | 无 finding | Issue 2 修复不绕过 app 锁门（needVerity 时抽屉子树不在 composition） | 确认安全 |

## 执行阶段（产物 + 依赖，不计工时）

- **Phase 1（Issue 1）**：纯函数 + 单测（TDD）→ `RecordDayHeader` 接线 + 两统计页传参 → 首页 `DayHeaderItem` 改用纯函数 → 跨月标志 plumbing。单会话产物：feature:records 编译通过 + 纯函数单测绿 + `:feature:records:testDebugUnitTest` 编译通过。
- **Phase 2（Issue 2）**：模拟器复现根因（依赖模拟器就绪）→ 最小修复 → Robolectric 交互回归测试。
- **Phase 3（节点 2 full-review）**：对 git diff 全维度审查（diff 规模决定满 5 Phase / 降级两维快审；纯函数+小修复预计 <50 行可降级，但含交互/状态机改动从严）。
- **Phase 4（真机 journey）**：恢复闭环验证 + Issue 1/2 功能验证 → 脱敏报告。
- **Phase 5（finishing）**：合入。

## 已知局限

- DATE_RANGE/ALL 不显示「今天/昨天/前天」后缀（沿用首页现状，F8）。
- 真机验证若 db 无图片记录，新格式备份的 record_images/ round-trip 无法覆盖（报告如实标注）。
- 备份恢复的恶意输入防护由 JVM 单测覆盖，本次真机 happy-path 不验（F10）。

## 验收标准

- 分类/资产/标签统计页周期切全年/全部，日期头带月/年上下文；月起始日≠1 时月视图头带月份。
- 首页开抽屉按返回（keyevent + 手势）抽屉收起；关闭后菜单可重新打开。
- 备份恢复：legacy db-only 恢复 ✓ + 新格式 round-trip ✓（或如实标注未覆盖项）。
- 纯函数单测 + Issue 2 交互测试绿；feature:records / feature:settings `testDebugUnitTest` 通过。
- 节点 2 full-review 无 Critical/High 未决。
- journey 报告已脱敏、evidence 入 gitignored 目录。
