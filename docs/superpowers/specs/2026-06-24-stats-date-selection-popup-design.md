# 设计：资产/分类/标签统计页周期选择弹窗

- 日期：2026-06-24
- 状态：已批准（待写实施计划）
- 模块：`feature:records`（`core/ui`、`core/model` 只读复用）

## 1. 需求

在 **资产详情、分类统计、标签统计** 三个统计界面的「月份选择卡片」上增加日期选择弹窗：点击卡片周期区域，弹出与**首页标题时间选择同款**的弹窗，支持 全部 / 按年 / 按月 / 时间段 / 按日 五种筛选（复用首页同一组件，不裁剪 Tab）。

## 2. 现状（事实，带 file:line）

- 可复用弹窗 `core/ui/.../component/DateSelectionPopup.kt:72`：`DropdownMenu` 实现，5 Tab（`DateSelectionTypeEnum.entries`，line 93：BY_DAY/BY_MONTH/BY_YEAR/DATE_RANGE/ALL），通过 `onDateSelected(DateSelectionEntity)` 实时回调，点外部 dismiss。5 Tab 来自枚举与 `stringResource`，无业务硬编码，可原样复用。
- 数据模型 `core/model/.../entity/DateSelectionEntity.kt:27`：sealed class，`toDateRange(monthStartDay)`（line 55-87）对全 5 模式返回 `[start,end)` 毫秒半开区间（All=`0L to Long.MAX_VALUE`），`getDisplayText()`（line 90）。
- 共享周期卡片 `feature/records/.../view/RecordMonthSummaryHeader.kt:54`：当前签名 `(periodText, monthSwitchable, summary, showTransferHint, onPreviousMonth, onNextMonth)`。`monthSwitchable` 同时控制「前后翻月箭头显隐」与「汇总标签月口径/中性口径」（line 122-138：`if (monthSwitchable) R.string.month_income else R.string.summary_income`）。**全仓仅被 2 处生产调用**：`AssetInfoContentScreen.kt:107`（硬编码 `monthSwitchable=true`）、`TypedAnalyticsScreen.kt:164`（`monthSwitchable = dateSelection is ByMonth`）。
- 数据管线**已支持全 5 模式**：`AssetInfoContentViewModel.kt:84/115`、`TypedAnalyticsViewModel.kt:94` 的 `recordList`/`summary` 只把 `selection` 喂给 `selection.toDateRange(d)` 取 `(start,end)`，**不对子类型分支**。当前仅有 setter `updateMonth(YearMonth)`，UI 仅暴露前后翻月箭头。结论：本特性数据层零改动，缺的是 UI 触发入口 + 泛化 setter。
- 既有同款范式参照：`AnalyticsScreen.kt:143-188`（图表页）= `CbScaffold` → `CbTopAppBar` title 槽 `Box{ Row(clickable=onDateClick){周期文本+ArrowDropDown}; DateSelectionPopup(...) }`，`showDatePopup` 由 `AnalyticsViewModel.kt:85` 持有（`displayDatePopup`/`dismissDatePopup`/`updateDateSelection`）。首页 `LauncherContentScreen.kt:358` 同构。

## 3. 关键设计决策

### 3.1 弹窗放在卡片上，而非 TopAppBar
项目内 3 处既有 `DateSelectionPopup` 用法都锚定在固定 TopAppBar 的 title 槽——但这能成立是因为那些屏的 title 本就是日期。本特性两屏不同：
- 资产详情 topbar 在 `feature:assets`（`AssetInfoScreen.kt:179` title=资产名，已有 编辑/删除/更多 三个 action），跨模块且 title 被占。
- 分类/标签 topbar title=分类/标签名（`TypedAnalyticsScreen.kt:124`）。

且需求明确要求弹窗加在「月份选择卡片」上。故**弹窗内嵌进共享卡片 `RecordMonthSummaryHeader`**，一处改动覆盖三屏。

### 3.2 弹窗展开态由 ViewModel 持有
对齐 `AnalyticsViewModel`/`LauncherContentViewModel` 既有范式（`showDatePopup` + `displayDatePopup`/`dismissDatePopup`），不用 Route 本地 `remember`。好处：与既有一致、便于截图测试构造展开态、抗重组。

### 3.3 保留 5 Tab（含按日）
复用首页同款弹窗，不参数化裁剪。按日模式下卡片走中性标签（`summary_income`）、列表单日仍渲一个 DayHeader——与首页 ByDay 行为一致，可接受。

### 3.4 `RecordMonthSummaryHeader` 由 `dateSelection` 统一派生
`periodText: String` → `dateSelection: DateSelectionEntity`（内部派生显示文本 + `dateSelection is ByMonth` 派生月份可切换性），**移除 `monthSwitchable` 参数**（消除冗余）。资产页随之从「恒月口径」泛化为「按 selection 派生」，与分类/标签一致。

## 4. 设计详情

### 4.1 共享卡片 `RecordMonthSummaryHeader`（一处改，覆盖三屏）
- 签名：移除 `periodText`、`monthSwitchable`；新增 `dateSelection: DateSelectionEntity`；新增弹窗参 `showDatePopup: Boolean = false`、`onDateClick: () -> Unit = {}`、`onDismissDatePopup: () -> Unit = {}`、`onDateSelected: (DateSelectionEntity) -> Unit = {}`（给默认值降低测试改动面）。
- 内部：`val monthSwitchable = dateSelection is DateSelectionEntity.ByMonth`；`val periodText = dateSelection.getDisplayText()`。
- 周期行：周期文本 + `CbIcons.ArrowDropDown` caret 组成**整行可点击区域**（热区覆盖整行，含 All 态「全部」）→ `onDateClick`。在该区域所在 `Box` 内宿主 `DateSelectionPopup(expanded=showDatePopup, onDismissRequest=onDismissDatePopup, currentSelection=dateSelection, onDateSelected=onDateSelected)`。
- ByMonth 模式：保留左右翻月箭头（`onPreviousMonth`/`onNextMonth`），中间文本+caret 开全选择器；非 ByMonth：无箭头，居中文本+caret 整行可点。
- DateRange 长文本（最长 21 字符）在非月态居中 `fillMaxWidth` 分支可容纳，不塌陷。

### 4.2 ViewModel（`AssetInfoContentViewModel`、`TypedAnalyticsViewModel` 各加）
- `var showDatePopup by mutableStateOf(false)` + `displayDatePopup()` / `dismissDatePopup()`。
- `fun updateDateSelection(selection: DateSelectionEntity) { _dateSelection.tryEmit(selection) }`（保留 `updateMonth` 供箭头翻月）。

### 4.3 Screen / Route 接线
- `AssetInfoContentScreen`、`TypedAnalyticsScreen` 增 4 个弹窗参（默认值），透传给 `RecordMonthSummaryHeader`。
- `TypedAnalyticsScreen` 移除冗余 `monthSwitchable` 参（其 Route 不再计算、6 处截图调用去掉该实参）。
- Route 接线：`onDateClick → displayDatePopup`、`onDismissDatePopup → dismissDatePopup`、`onDateSelected → updateDateSelection`；`onPreviousMonth/onNextMonth → updateMonth(currentMonth±1)`（`currentMonth = (dateSelection as? ByMonth)?.yearMonth ?: YearMonth.now()`，仅 ByMonth 态箭头可见，自洽）。
- **不动** `AssetInfoContent`/`AssetInfoContentRoute` 跨模块 public 签名（弹窗态收在 Route+VM 内，`app/MainApp` 注入零影响）。

## 5. 风险与缓解

| 风险 | 处置 |
|---|---|
| `DropdownMenu` 锚在 `LazyColumn` item 内，弹窗展开时滚动列表可能错位/锚点回收（与既有 TopAppBar 锚定不同，未实测） | 卡片是列表第 2 项（近顶），主交互路径「点开→选→关」不涉滚动。列为**显式验证项**：展开态 Roborazzi 截图 + 真机手动验证「展开后滚动列表」。失败兜底=把弹窗内容改 `CbModalBottomSheet` 呈现（screen 级锚定，免滚动问题）。 |
| 资产页 `monthSwitchable` 由硬编码 `true` 改派生，新增「固定周期态中性标签」交互无测试守护 | 补 AssetInfo `ByYear` 固定态截图基线 + VM 测试；确认资产页 `showTransferHint` 仍恒 `false`（不显转账提示，与现状一致）。 |
| 签名变更连锁致 `feature:records:testDebugUnitTest` 整模块编译失败 | 改动须同批次更新 **10 处截图调用**（AssetInfo 4 吃默认值 / TypedAnalytics 6 去 `monthSwitchable` 实参）+ 重录基线。 |

## 6. 测试计划

- **截图**：更新现有 10 处调用；新增弹窗展开态变体（验弹窗 UI+锚点）；新增 AssetInfo `ByYear` 固定态变体（验中性标签派生）；重录 Roborazzi 基线。
- **ViewModel**：两 VM 加 `updateDateSelection`（emit `ByYear`/`All`/`DateRange` → `dateSelection` 更新 + `summary`/`recordList` 按新区间重查）测试；TypedAnalytics 补「进入后切周期 → 重复 `updateData`（相同入口 key）不重置」连续性断言（扩展现有「翻月保持」用例）。
- **回归**：`DateSelectionPopupScreenshotTests` 不受影响（`DateSelectionPopup` 签名不变）。

## 7. 范围外 / Backlog

- 范围外：`AnalyticsScreen`（图表页，已有自己的日期选择）；monthStartDay 默认进入语义不变。
- Backlog（评审 Low，非本特性引入）：
  - `toDateRange` 的 `ByYear` 对 `year+1` 无溢出保护——本特性 `YearPicker` 受限 `2000..currentYear+1`、闭环不可达，记防御性收口待办。
  - 汇总 UseCase 对 All 区间 `pageSize=Int.MAX_VALUE` 全量 load 内存求和（`RecordRepositoryImpl.kt:254`）——既有结构、首页已可触发，记 DB 端 SUM 聚合优化待办。

## 8. 受影响文件

改：
- `feature/records/.../view/RecordMonthSummaryHeader.kt`
- `feature/records/.../screen/AssetInfoContentScreen.kt`、`TypedAnalyticsScreen.kt`
- `feature/records/.../viewmodel/AssetInfoContentViewModel.kt`、`TypedAnalyticsViewModel.kt`
- 测试：`AssetInfoContentScreenScreenshotTests.kt`、`TypedAnalyticsScreenScreenshotTests.kt`、`AssetInfoContentViewModelTest.kt`、`TypedAnalyticsViewModelTest.kt` + Roborazzi 基线

只读复用（不改）：
- `core/ui/.../component/DateSelectionPopup.kt`、`core/model/.../entity/DateSelectionEntity.kt`

边界（确认不改 public 签名）：
- `feature/records/.../navigation/RecordNavigation.kt`（`AssetInfoContent` public 包装）、`app/.../MainApp.kt` 注入处
