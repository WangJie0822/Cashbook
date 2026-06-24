# 统计页周期选择弹窗 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在资产详情、分类统计、标签统计三屏的周期汇总卡片上增加日期选择弹窗，支持 全部/年/月/时间段/按日 切换。

**Architecture:** 全部改动集中在 `feature:records` 单模块（`core/ui` 的 `DateSelectionPopup`、`core/model` 的 `DateSelectionEntity` 只读复用）。三屏共享卡片 `RecordMonthSummaryHeader` 内嵌 `DateSelectionPopup`（一处改覆盖三屏）；两个 ViewModel 加 `updateDateSelection` setter + VM 持有的 `showDatePopup` 弹窗态；数据管线（`recordList`/`summary` 已走 `selection.toDateRange(d)`）零改动。

**Tech Stack:** Kotlin, Jetpack Compose, Hilt, Paging3, Coroutines/Flow, Roborazzi（截图测试）, JUnit4 + Truth。

设计文档：`docs/superpowers/specs/2026-06-24-stats-date-selection-popup-design.md`

## Global Constraints

- `feature:records` 模块测试源集**整体编译**：任一 Composable/ViewModel 签名变更必须同步更新 `src/test` 下所有调用处，否则 `:feature:records:testDebugUnitTest` 整模块编译失败。
- UI 禁用裸 Material3 组件，必须用 `core/design` 封装（`CbCard`/`CbIconButton`/`CbIcons`/`CbScaffold`/`CbTopAppBar` 等）；`Icon(...)` 与 `MaterialTheme.colorScheme/.typography` 属性访问允许。
- Android 库模块用 `:testDebugUnitTest`（非 `:test`）。
- 不改 `DateSelectionPopup`（`core/ui`）与 `DateSelectionEntity`（`core/model`）。
- 不改 `AssetInfoContent`/`AssetInfoContentRoute` 跨模块 public 签名（`app/MainApp` 注入零影响）。
- 提交遵循 `[类型|模块|功能][影响范围]说明`，原子化。

---

### Task 1: 两个 ViewModel 新增 `updateDateSelection` + `showDatePopup` 弹窗态

**Files:**
- Modify: `feature/records/src/main/kotlin/cn/wj/android/cashbook/feature/records/viewmodel/AssetInfoContentViewModel.kt`
- Modify: `feature/records/src/main/kotlin/cn/wj/android/cashbook/feature/records/viewmodel/TypedAnalyticsViewModel.kt`
- Test: `feature/records/src/test/kotlin/cn/wj/android/cashbook/feature/records/viewmodel/AssetInfoContentViewModelTest.kt`
- Test: `feature/records/src/test/kotlin/cn/wj/android/cashbook/feature/records/viewmodel/TypedAnalyticsViewModelTest.kt`

**Interfaces:**
- Produces（Task 2 Route 消费）：两个 VM 各新增
  - `val showDatePopup: Boolean`（Compose `mutableStateOf`，`private set`）
  - `fun displayDatePopup()`、`fun dismissDatePopup()`
  - `fun updateDateSelection(selection: DateSelectionEntity)`
- 既有 `updateMonth(yearMonth: YearMonth)` 保留不变（供前后翻月箭头）。

- [ ] **Step 1: 写 `AssetInfoContentViewModelTest` 失败测试**

在 `AssetInfoContentViewModelTest.kt` 的 `updateAssetId 基础行为测试` 区块后追加（与现有 `buildViewModel()` 风格一致）：

```kotlin
    @Test
    fun when_updateDateSelection_byYear_then_dateSelection_updates() {
        val viewModel = buildViewModel()
        viewModel.updateDateSelection(
            cn.wj.android.cashbook.core.model.entity.DateSelectionEntity.ByYear(2024),
        )
        assertThat(viewModel.dateSelection.value)
            .isEqualTo(cn.wj.android.cashbook.core.model.entity.DateSelectionEntity.ByYear(2024))
    }

    @Test
    fun when_updateDateSelection_all_then_dateSelection_is_all() {
        val viewModel = buildViewModel()
        viewModel.updateDateSelection(cn.wj.android.cashbook.core.model.entity.DateSelectionEntity.All)
        assertThat(viewModel.dateSelection.value)
            .isEqualTo(cn.wj.android.cashbook.core.model.entity.DateSelectionEntity.All)
    }

    @Test
    fun when_display_and_dismiss_date_popup_then_flag_toggles() {
        val viewModel = buildViewModel()
        assertThat(viewModel.showDatePopup).isFalse()
        viewModel.displayDatePopup()
        assertThat(viewModel.showDatePopup).isTrue()
        viewModel.dismissDatePopup()
        assertThat(viewModel.showDatePopup).isFalse()
    }
```

- [ ] **Step 2: 写 `TypedAnalyticsViewModelTest` 失败测试**

在 `TypedAnalyticsViewModelTest.kt` 的 `when_updateData_repeated_with_same_args_after_updateMonth_then_month_preserved` 测试后追加：

```kotlin
    @Test
    fun when_updateDateSelection_byYear_then_dateSelection_updates() = runTest {
        viewModel.updateData(tagId = -1L, typeId = 1L, date = "2024-06")
        viewModel.updateDateSelection(DateSelectionEntity.ByYear(2024))
        assertThat(viewModel.dateSelection.value).isEqualTo(DateSelectionEntity.ByYear(2024))
    }

    @Test
    fun when_updateData_repeated_same_args_after_updateDateSelection_then_selection_preserved() = runTest {
        // Route 重组重复 updateData（相同入口 key）不应把弹窗切换的周期重置回入口周期
        viewModel.updateData(tagId = -1L, typeId = 1L, date = "2024-06")
        viewModel.updateDateSelection(DateSelectionEntity.ByYear(2024))
        viewModel.updateData(tagId = -1L, typeId = 1L, date = "2024-06")
        assertThat(viewModel.dateSelection.value).isEqualTo(DateSelectionEntity.ByYear(2024))
    }

    @Test
    fun when_display_and_dismiss_date_popup_then_flag_toggles() {
        assertThat(viewModel.showDatePopup).isFalse()
        viewModel.displayDatePopup()
        assertThat(viewModel.showDatePopup).isTrue()
        viewModel.dismissDatePopup()
        assertThat(viewModel.showDatePopup).isFalse()
    }
```

- [ ] **Step 3: 运行测试，确认失败**

Run: `./gradlew :feature:records:testDebugUnitTest --tests "*AssetInfoContentViewModelTest" --tests "*TypedAnalyticsViewModelTest"`
Expected: 编译失败 / FAIL（`updateDateSelection`、`showDatePopup`、`displayDatePopup`、`dismissDatePopup` 未定义）

- [ ] **Step 4: 实现 `AssetInfoContentViewModel`**

在 import 区加（该文件当前无 compose runtime import）：

```kotlin
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
```

在 `dateSelection` 声明（`val dateSelection: StateFlow<DateSelectionEntity> = _dateSelection`）之后追加：

```kotlin
    /** 日期选择 Popup 是否展开（VM 持有，对齐 AnalyticsViewModel 范式） */
    var showDatePopup by mutableStateOf(false)
        private set
```

在 `updateMonth(...)` 方法之后追加：

```kotlin
    /** 显示日期选择 Popup */
    fun displayDatePopup() {
        showDatePopup = true
    }

    /** 隐藏日期选择 Popup */
    fun dismissDatePopup() {
        showDatePopup = false
    }

    /** 更新日期选择（全部/年/月/时间段/按日） */
    fun updateDateSelection(selection: DateSelectionEntity) {
        _dateSelection.tryEmit(selection)
    }
```

- [ ] **Step 5: 实现 `TypedAnalyticsViewModel`**

该文件已 import `mutableStateOf`/`getValue`/`setValue`。在 `dateSelection` 声明（`val dateSelection: StateFlow<DateSelectionEntity> = _dateSelection`）之后追加：

```kotlin
    /** 日期选择 Popup 是否展开（VM 持有，对齐 AnalyticsViewModel 范式） */
    var showDatePopup by mutableStateOf(false)
        private set
```

在 `updateMonth(...)` 方法之后追加：

```kotlin
    /** 显示日期选择 Popup */
    fun displayDatePopup() {
        showDatePopup = true
    }

    /** 隐藏日期选择 Popup */
    fun dismissDatePopup() {
        showDatePopup = false
    }

    /** 更新日期选择（全部/年/月/时间段/按日） */
    fun updateDateSelection(selection: DateSelectionEntity) {
        _dateSelection.tryEmit(selection)
    }
```

- [ ] **Step 6: 运行测试，确认通过**

Run: `./gradlew :feature:records:testDebugUnitTest --tests "*AssetInfoContentViewModelTest" --tests "*TypedAnalyticsViewModelTest"`
Expected: PASS

- [ ] **Step 7: 提交**

```bash
git add feature/records/src/main/kotlin/cn/wj/android/cashbook/feature/records/viewmodel/AssetInfoContentViewModel.kt \
        feature/records/src/main/kotlin/cn/wj/android/cashbook/feature/records/viewmodel/TypedAnalyticsViewModel.kt \
        feature/records/src/test/kotlin/cn/wj/android/cashbook/feature/records/viewmodel/AssetInfoContentViewModelTest.kt \
        feature/records/src/test/kotlin/cn/wj/android/cashbook/feature/records/viewmodel/TypedAnalyticsViewModelTest.kt
git commit -m "[feat|feature|统计][公共]资产/分类/标签统计VM加updateDateSelection+showDatePopup弹窗态"
```

---

### Task 2: 泛化 `RecordMonthSummaryHeader` 内嵌弹窗 + 两屏 Route/Screen 接线 + 截图基线

**Files:**
- Modify: `feature/records/src/main/kotlin/cn/wj/android/cashbook/feature/records/view/RecordMonthSummaryHeader.kt`
- Modify: `feature/records/src/main/kotlin/cn/wj/android/cashbook/feature/records/screen/AssetInfoContentScreen.kt`
- Modify: `feature/records/src/main/kotlin/cn/wj/android/cashbook/feature/records/screen/TypedAnalyticsScreen.kt`
- Test: `feature/records/src/test/kotlin/cn/wj/android/cashbook/feature/records/screen/TypedAnalyticsScreenScreenshotTests.kt`
- Test: `feature/records/src/test/kotlin/cn/wj/android/cashbook/feature/records/screen/AssetInfoContentScreenScreenshotTests.kt`

**Interfaces:**
- Consumes（Task 1）：`viewModel.showDatePopup`、`displayDatePopup()`、`dismissDatePopup()`、`updateDateSelection(DateSelectionEntity)`。
- Produces：`RecordMonthSummaryHeader` 新签名
  ```
  RecordMonthSummaryHeader(
      dateSelection: DateSelectionEntity,
      summary: AssetMonthSummaryModel,
      showTransferHint: Boolean,
      showDatePopup: Boolean = false,
      onDateClick: () -> Unit = {},
      onDismissDatePopup: () -> Unit = {},
      onDateSelected: (DateSelectionEntity) -> Unit = {},
      onPreviousMonth: () -> Unit,
      onNextMonth: () -> Unit,
  )
  ```
  （移除 `periodText`、`monthSwitchable`；内部派生 `monthSwitchable = dateSelection is ByMonth`、`periodText = dateSelection.getDisplayText()`）

- [ ] **Step 1: 改 `RecordMonthSummaryHeader` 的 import**

在 import 区追加：

```kotlin
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import cn.wj.android.cashbook.core.design.theme.rememberHapticOnClick
import cn.wj.android.cashbook.core.model.entity.DateSelectionEntity
import cn.wj.android.cashbook.core.ui.component.DateSelectionPopup
```

（`Arrangement`、`CbIcons`、`CbCard`、`CbIconButton`、`MaterialTheme`、`Modifier`、`Alignment`、`Text`、`Icon`、`stringResource`、`TextAlign`、`dp` 已 import。）

- [ ] **Step 2: 替换 `RecordMonthSummaryHeader` 签名与周期行实现**

把现有 `RecordMonthSummaryHeader(...)` 的签名与 KDoc 改为（注意删 `periodText`/`monthSwitchable`，加 `dateSelection` + 4 弹窗参）：

```kotlin
/**
 * 月份切换器 / 固定周期文字 + 收入/支出/结余 3 列汇总卡。
 * 供资产详情、分类统计、标签统计共用。点击周期文本打开日期选择 [DateSelectionPopup]。
 *
 * @param dateSelection 当前周期选择（内部派生显示文本与月份可切换性）
 * @param summary 收支结余汇总
 * @param showTransferHint true 时以提示文案代替 3 列（转账类型不计入收支）
 * @param showDatePopup 日期选择 Popup 是否展开
 * @param onDateClick 点击周期区域（打开 Popup）回调
 * @param onDismissDatePopup 关闭 Popup 回调
 * @param onDateSelected 日期选择回调
 * @param onPreviousMonth 切换到上一月回调（仅 ByMonth 态箭头使用）
 * @param onNextMonth 切换到下一月回调（仅 ByMonth 态箭头使用）
 */
@Composable
internal fun RecordMonthSummaryHeader(
    dateSelection: DateSelectionEntity,
    summary: AssetMonthSummaryModel,
    showTransferHint: Boolean,
    showDatePopup: Boolean = false,
    onDateClick: () -> Unit = {},
    onDismissDatePopup: () -> Unit = {},
    onDateSelected: (DateSelectionEntity) -> Unit = {},
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
) {
    val monthSwitchable = dateSelection is DateSelectionEntity.ByMonth
    val periodText = dateSelection.getDisplayText()
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
            // 周期行 + 内嵌日期选择 Popup（Popup 锚定在本 Box）
            Box {
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
                        Row(
                            modifier = Modifier.clickable(
                                onClick = rememberHapticOnClick(onClick = onDateClick),
                            ),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = periodText,
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Icon(
                                imageVector = CbIcons.ArrowDropDown,
                                contentDescription = null,
                            )
                        }
                        CbIconButton(onClick = onNextMonth) {
                            Icon(
                                imageVector = CbIcons.KeyboardArrowRight,
                                contentDescription = stringResource(id = R.string.cd_next),
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(onClick = rememberHapticOnClick(onClick = onDateClick)),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = periodText,
                                style = MaterialTheme.typography.titleMedium,
                                textAlign = TextAlign.Center,
                            )
                            Icon(
                                imageVector = CbIcons.ArrowDropDown,
                                contentDescription = null,
                            )
                        }
                    }
                }
                DateSelectionPopup(
                    expanded = showDatePopup,
                    onDismissRequest = onDismissDatePopup,
                    currentSelection = dateSelection,
                    onDateSelected = onDateSelected,
                )
            }

            if (showTransferHint) {
```

> 下方 `if (showTransferHint) { ... } else { ... }` 汇总区块**原样保留不动**（仍引用本地派生的 `monthSwitchable`，`R.string.month_income` vs `summary_income` 语义不变）。仅替换到上面 `if (showTransferHint) {` 这一行为止的周期行部分。

- [ ] **Step 3: 改 `AssetInfoContentScreen` —— Screen 签名 + header 调用 + Route 接线**

`AssetInfoContentScreen` composable 签名加 4 个弹窗参（默认值），位置在 `recordList` 之后、`onPreviousMonth` 之前：

```kotlin
internal fun AssetInfoContentScreen(
    topContent: @Composable () -> Unit,
    dateSelection: DateSelectionEntity,
    summary: AssetMonthSummaryModel,
    recordList: LazyPagingItems<LauncherListItem>,
    showDatePopup: Boolean = false,
    onDateClick: () -> Unit = {},
    onDismissDatePopup: () -> Unit = {},
    onDateSelected: (DateSelectionEntity) -> Unit = {},
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onRecordItemClick: (RecordViewsEntity) -> Unit,
) {
```

把 `RecordMonthSummaryHeader(...)` 调用（原 `periodText = dateSelection.getDisplayText(), monthSwitchable = true, ...`）替换为：

```kotlin
                RecordMonthSummaryHeader(
                    dateSelection = dateSelection,
                    summary = summary,
                    showTransferHint = false,
                    showDatePopup = showDatePopup,
                    onDateClick = onDateClick,
                    onDismissDatePopup = onDismissDatePopup,
                    onDateSelected = onDateSelected,
                    onPreviousMonth = onPreviousMonth,
                    onNextMonth = onNextMonth,
                )
```

在 `AssetInfoContentRoute` 内的 `AssetInfoContentScreen(...)` 调用加弹窗接线（`topContent`/`dateSelection`/`summary`/`recordList` 之后）：

```kotlin
    AssetInfoContentScreen(
        topContent = topContent,
        dateSelection = dateSelection,
        summary = summary,
        recordList = recordList,
        showDatePopup = viewModel.showDatePopup,
        onDateClick = viewModel::displayDatePopup,
        onDismissDatePopup = viewModel::dismissDatePopup,
        onDateSelected = viewModel::updateDateSelection,
        onPreviousMonth = { viewModel.updateMonth(currentMonth.minusMonths(1)) },
        onNextMonth = { viewModel.updateMonth(currentMonth.plusMonths(1)) },
        onRecordItemClick = onRecordItemClick,
    )
```

- [ ] **Step 4: 改 `TypedAnalyticsScreen` —— 删 `monthSwitchable` 参 + 加弹窗参 + header 调用 + Route 接线**

`TypedAnalyticsScreen` composable 签名：**删除** `monthSwitchable: Boolean,` 这一行，并在 `summary` 之后加 4 个弹窗参：

```kotlin
    dateSelection: DateSelectionEntity,
    summary: AssetMonthSummaryModel,
    showDatePopup: Boolean = false,
    onDateClick: () -> Unit = {},
    onDismissDatePopup: () -> Unit = {},
    onDateSelected: (DateSelectionEntity) -> Unit = {},
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
```

把 `RecordMonthSummaryHeader(...)` 调用（原带 `periodText`/`monthSwitchable`）替换为：

```kotlin
                                    RecordMonthSummaryHeader(
                                        dateSelection = dateSelection,
                                        summary = summary,
                                        showTransferHint = uiState.isTransferType,
                                        showDatePopup = showDatePopup,
                                        onDateClick = onDateClick,
                                        onDismissDatePopup = onDismissDatePopup,
                                        onDateSelected = onDateSelected,
                                        onPreviousMonth = onPreviousMonth,
                                        onNextMonth = onNextMonth,
                                    )
```

在 `TypedAnalyticsRoute` 内：**删除** `val monthSwitchable = dateSelection is DateSelectionEntity.ByMonth` 这一行；`TypedAnalyticsScreen(...)` 调用**删除** `monthSwitchable = monthSwitchable,` 实参，并加弹窗接线：

```kotlin
        dateSelection = dateSelection,
        summary = summary,
        showDatePopup = viewModel.showDatePopup,
        onDateClick = viewModel::displayDatePopup,
        onDismissDatePopup = viewModel::dismissDatePopup,
        onDateSelected = viewModel::updateDateSelection,
        onPreviousMonth = { viewModel.updateMonth(currentMonth.minusMonths(1)) },
        onNextMonth = { viewModel.updateMonth(currentMonth.plusMonths(1)) },
```

- [ ] **Step 5: 改 `TypedAnalyticsScreenScreenshotTests` 的 6 处调用**

6 处 `TypedAnalyticsScreen(...)` 调用（约 line 98/121/147/173/197/222）**删除各自的 `monthSwitchable = true,` / `monthSwitchable = false,` 实参**。其中 `typedAnalyticsScreen_fixedPeriod_multipleThemes`（原 `dateSelection = DateSelectionEntity.ByYear(2024), monthSwitchable = false`）删 `monthSwitchable` 后保留 `dateSelection = DateSelectionEntity.ByYear(2024)`（固定态由 header 内部派生）。新增的 4 个弹窗参吃默认值，无需在测试中显式传。

> `AssetInfoContentScreenScreenshotTests` 的 4 处调用**无需改**（原本不传 `monthSwitchable`，弹窗参走默认值，`dateSelection` 已传）。

- [ ] **Step 6: 编译 + 运行 feature:records 单元测试，确认绿**

Run: `./gradlew :feature:records:testDebugUnitTest`
Expected: BUILD SUCCESSFUL（全模块编译通过、测试 PASS；Roborazzi 此步仅 capture 不 verify）

- [ ] **Step 7: 新增「资产固定周期态」截图用例（守资产中性标签派生）**

在 `AssetInfoContentScreenScreenshotTests.kt` 追加（复用现有 `sampleItems`/`sampleSummary`）：

```kotlin
    @Test
    fun assetInfoContentScreen_fixedPeriod_multipleThemes() {
        composeTestRule.captureMultiTheme(
            name = "AssetInfoContentScreen",
            overrideFileName = "AssetInfoContentScreen_fixedPeriod",
        ) {
            val recordList = flowOf(PagingData.from(sampleItems))
                .collectAsLazyPagingItems()
            AssetInfoContentScreen(
                topContent = { Text(text = "资产信息头部") },
                dateSelection = DateSelectionEntity.ByYear(2024),
                summary = sampleSummary,
                recordList = recordList,
                onPreviousMonth = {},
                onNextMonth = {},
                onRecordItemClick = {},
            )
        }
    }
```

- [ ] **Step 8: 录制并校验 Roborazzi 基线**

> 卡片新增了 `ArrowDropDown` caret，所有现有 `AssetInfoContentScreen*` / `TypedAnalyticsScreen*` 基线视觉变化，需重录；并新增 `AssetInfoContentScreen_fixedPeriod`。先确认本机内存（CLAUDE.local.md「Gradle/JVM 高内存命令」），再跑。

Run（先确认实际任务名）：`./gradlew :feature:records:tasks --all | grep -i roborazzi`
Run（录制）：`./gradlew :feature:records:recordRoborazziOnlineDebug`
Run（校验）：`./gradlew :feature:records:verifyRoborazziOnlineDebug`
Expected: record 后 verify BUILD SUCCESSFUL。人工抽查新基线 PNG：周期行有下拉 caret、`_fixedPeriod` 显示「收入/支出/结余」中性标签且无翻月箭头。

- [ ] **Step 9: Spotless 格式化**

Run: `./gradlew :feature:records:spotlessApply --init-script gradle/init.gradle.kts --no-configuration-cache`
Expected: BUILD SUCCESSFUL

- [ ] **Step 10: 提交**

```bash
git add feature/records/src/main/kotlin/cn/wj/android/cashbook/feature/records/view/RecordMonthSummaryHeader.kt \
        feature/records/src/main/kotlin/cn/wj/android/cashbook/feature/records/screen/AssetInfoContentScreen.kt \
        feature/records/src/main/kotlin/cn/wj/android/cashbook/feature/records/screen/TypedAnalyticsScreen.kt \
        feature/records/src/test/kotlin/cn/wj/android/cashbook/feature/records/screen/TypedAnalyticsScreenScreenshotTests.kt \
        feature/records/src/test/kotlin/cn/wj/android/cashbook/feature/records/screen/AssetInfoContentScreenScreenshotTests.kt \
        feature/records/src/test/screenshots
git commit -m "[feat|feature|统计][公共]资产/分类/标签周期卡片内嵌日期选择弹窗+泛化RecordMonthSummaryHeader"
```

---

### Task 3: 全模块回归 + 锚点滚动手动验证

**Files:**
- 无代码改动（验证 + 文档化风险项）

**Interfaces:**
- Consumes：Task 1/2 全部产物。

- [ ] **Step 1: 全模块单元测试 + 截图校验**

Run: `./gradlew :feature:records:testDebugUnitTest :feature:records:verifyRoborazziOnlineDebug`
Expected: BUILD SUCCESSFUL（确认无遗漏的签名失配、基线全绿）

- [ ] **Step 2: Lint（确认 Design lint 无新违规，如裸 Material3 误用）**

Run: `./gradlew :feature:records:lintDevRelease -Dlint.baselines.continue=true`
Expected: BUILD SUCCESSFUL（无新增 `Design` error）

- [ ] **Step 3: 输出锚点滚动手动验证清单（交用户在真机/模拟器执行）**

> `DropdownMenu` 锚在 `LazyColumn` item 内属本特性唯一未自动化覆盖的风险（spec 第 5 节）。截图测试不能验证「展开后滚动」的真机行为，需人工：
>
> 对 **资产详情 / 分类统计 / 标签统计** 三屏各验证：
> 1. 点击周期卡片 → 弹窗在卡片下方正确锚定弹出；
> 2. 切到 全部 / 按年 / 时间段 → 卡片周期文本与标签正确刷新（中性「收入/支出/结余」）、列表与汇总按新区间刷新；
> 3. 切回 按月 → 前后翻月箭头出现且可翻月；
> 4. **弹窗展开时上下滚动记录列表** → 观察弹窗是否错位/消失。
>
> 若第 4 步表现可接受（弹窗随点击外部关闭、不残留错位）→ 完成。
> 若明显错位/悬浮脱节 → 触发兜底：把 `RecordMonthSummaryHeader` 内 `DateSelectionPopup` 改用 `CbModalBottomSheet` 呈现（screen 级锚定，免滚动问题），其余接线不变。

- [ ] **Step 4: 交付前确认**

确认 Task 1/2 已提交、`git status` 干净、三屏功能与回归清单通过。涉及 spec 第 7 节 backlog 两项（`toDateRange` ByYear 溢出防御 / All 区间 DB 端 SUM）**不在本特性范围**，保持记录即可。

---

## Self-Review

**Spec coverage（对照 spec 各节）：**
- §4.1 共享卡片泛化 → Task 2 Step 1-2 ✓
- §4.2 两 VM `updateDateSelection`+`showDatePopup` → Task 1 ✓
- §4.3 Screen/Route 接线 + TypedAnalytics 删 `monthSwitchable` + 不动 public 签名 → Task 2 Step 3-4 ✓
- §5 风险（锚点滚动验证 + 兜底）→ Task 3 Step 3 ✓；（资产固定态测试）→ Task 2 Step 7 ✓；（10 处截图连锁）→ Task 2 Step 5/8 ✓
- §6 测试（VM setter/连续性 + 截图更新/新增 + 基线）→ Task 1 Step 1-2、Task 2 Step 5/7/8 ✓
- §7 backlog 显式标范围外 → Task 3 Step 4 ✓

**Placeholder scan：** 无 TBD/TODO；所有 code step 含完整代码；命令含预期输出。Roborazzi 任务名因环境差异先 `tasks --all | grep` 确认（已显式给出探测命令，非占位）。

**Type consistency：** `updateDateSelection(selection: DateSelectionEntity)`、`showDatePopup: Boolean`、`displayDatePopup()`/`dismissDatePopup()` 在 Task 1 定义、Task 2 Route 按同名消费一致；`RecordMonthSummaryHeader` 新签名（删 `periodText`/`monthSwitchable`、加 `dateSelection`+4 弹窗参）在 Task 2 Step 2 定义、Step 3/4 两屏按同签名调用一致；`DateSelectionEntity.ByYear`/`All`/`ByMonth` 与 `getDisplayText()` 均为既有 API。
