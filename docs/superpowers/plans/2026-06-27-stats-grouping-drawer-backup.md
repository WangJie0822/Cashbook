# 统计页日期头按周期自适应 + 首页抽屉返回键修复 + 备份恢复真机验证 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让分类/资产/标签统计页的日期头按周期（全年/全部/跨自然月的按月）显示月/年上下文与首页一致；修复首页抽屉返回键不收起；真机验证三项 + 端到端验证新增备份恢复功能。

**Architecture:** 抽单一真源纯函数 `recordDayHeaderDateText`（feature:records，纯 JVM 可单测），首页 `DayHeaderItem` 与统计页 `RecordDayHeader` 共用；保持底层 `recordDaySeparator` 按日分组不变。抽屉先设备复现根因再最小修复、修复仍经 VM 意图源。备份验证用「恢复 db-only 旧备份 → app 自产新格式备份 → 恢复新备份」闭环。

**Tech Stack:** Kotlin, Jetpack Compose, Hilt, JUnit + Truth（纯 JVM 单测）, Roborazzi（截图，CI 管理基线）, Robolectric（Compose 交互测试）, android-cli + adb（真机验证）。

## Global Constraints

- 设计系统封装强制：禁止直接用 Material3 组件，必须用 `core/design` 的 `CbXxx`（本计划日期头仅用 `Text`/`Column`/`CbHorizontalDivider`，不引入新禁用组件）。
- Roborazzi 截图基线由 CI 管理：本地**只提交代码、不录基线、不据本机 verify 判失败**；PR 由 `recordRoborazziDevDebug` auto-commit 重录。
- 改 Composable/ViewModel 签名必须同步该模块 `src/test` 的 `*ScreenshotTests`/`*ViewModelTest`（否则整模块 `testDebugUnitTest` 编译失败）；本计划给屏级新参数加默认值以避免测试调用点编译破坏。
- 纯函数禁调 `stringResource`（@Composable API）；本地化在 @Composable 薄壳解析后作字符串传入纯函数。
- TDD、频繁提交、最小范围提交；提交信息格式 `[类型|模块|功能][影响范围]说明` + `Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>`。
- 项目仓库 git 跟踪文件**不写 vault `[[...]]` 引用**。
- 功能开发在 git worktree 隔离进行（执行阶段经 using-git-worktrees 建立）。
- JVM 库模块测试任务 `:test`；Android 库模块（feature:*）`:testDebugUnitTest`。
- 真机 journey 报告**强制脱敏**；DB dump/zip/图片只入 gitignored `docs/testing/reports/evidence/`。

---

### Task 1: 纯函数 `recordDayHeaderDateText` + 单元测试

**Files:**
- Modify: `feature/records/src/main/kotlin/cn/wj/android/cashbook/feature/records/viewmodel/RecordDayGrouping.kt`（追加 top-level `internal fun`）
- Test: `feature/records/src/test/kotlin/cn/wj/android/cashbook/feature/records/viewmodel/RecordDayGroupingTest.kt`（追加用例）

**Interfaces:**
- Produces: `internal fun recordDayHeaderDateText(type: DateSelectionTypeEnum, year: Int, month: Int, day: Int, dayTypeSuffix: String, dayLabel: String, monthLabel: String, yearLabel: String, byMonthCrossesNaturalMonth: Boolean): String`

- [ ] **Step 1: 写失败测试**（追加到 `RecordDayGroupingTest.kt` 类体内）

```kotlin
@Test
fun dayHeaderText_byDay_showsDayOnly() {
    val r = recordDayHeaderDateText(
        type = cn.wj.android.cashbook.core.model.enums.DateSelectionTypeEnum.BY_DAY,
        year = 2026, month = 6, day = 27, dayTypeSuffix = "(今天)",
        dayLabel = "日", monthLabel = "月", yearLabel = "年", byMonthCrossesNaturalMonth = false,
    )
    assertThat(r).isEqualTo("27日(今天)")
}

@Test
fun dayHeaderText_byMonth_notCrossing_showsDayOnly() {
    val r = recordDayHeaderDateText(
        type = cn.wj.android.cashbook.core.model.enums.DateSelectionTypeEnum.BY_MONTH,
        year = 2026, month = 6, day = 27, dayTypeSuffix = "",
        dayLabel = "日", monthLabel = "月", yearLabel = "年", byMonthCrossesNaturalMonth = false,
    )
    assertThat(r).isEqualTo("27日")
}

@Test
fun dayHeaderText_byMonth_crossing_showsMonthDay() {
    val r = recordDayHeaderDateText(
        type = cn.wj.android.cashbook.core.model.enums.DateSelectionTypeEnum.BY_MONTH,
        year = 2026, month = 6, day = 27, dayTypeSuffix = "",
        dayLabel = "日", monthLabel = "月", yearLabel = "年", byMonthCrossesNaturalMonth = true,
    )
    assertThat(r).isEqualTo("6月27日")
}

@Test
fun dayHeaderText_byYear_showsMonthDay_withSuffix() {
    val r = recordDayHeaderDateText(
        type = cn.wj.android.cashbook.core.model.enums.DateSelectionTypeEnum.BY_YEAR,
        year = 2026, month = 6, day = 27, dayTypeSuffix = "(今天)",
        dayLabel = "日", monthLabel = "月", yearLabel = "年", byMonthCrossesNaturalMonth = false,
    )
    assertThat(r).isEqualTo("6月27日(今天)")
}

@Test
fun dayHeaderText_dateRange_showsYearMonthDay_noSuffix() {
    val r = recordDayHeaderDateText(
        type = cn.wj.android.cashbook.core.model.enums.DateSelectionTypeEnum.DATE_RANGE,
        year = 2026, month = 6, day = 27, dayTypeSuffix = "(今天)",
        dayLabel = "日", monthLabel = "月", yearLabel = "年", byMonthCrossesNaturalMonth = false,
    )
    assertThat(r).isEqualTo("2026年6月27日")
}

@Test
fun dayHeaderText_all_showsYearMonthDay_noSuffix() {
    val r = recordDayHeaderDateText(
        type = cn.wj.android.cashbook.core.model.enums.DateSelectionTypeEnum.ALL,
        year = 2026, month = 6, day = 27, dayTypeSuffix = "(今天)",
        dayLabel = "日", monthLabel = "月", yearLabel = "年", byMonthCrossesNaturalMonth = false,
    )
    assertThat(r).isEqualTo("2026年6月27日")
}
```

- [ ] **Step 2: 跑测试确认失败**

Run: `./gradlew :feature:records:testDebugUnitTest --tests "*RecordDayGroupingTest" --no-daemon --console=plain`
Expected: 编译失败 / `Unresolved reference: recordDayHeaderDateText`

- [ ] **Step 3: 实现纯函数**（追加到 `RecordDayGrouping.kt` 末尾；文件已 `package cn.wj.android.cashbook.feature.records.viewmodel`）

在文件顶部 import 区加：
```kotlin
import cn.wj.android.cashbook.core.model.enums.DateSelectionTypeEnum
```
追加函数：
```kotlin
/**
 * 记录日期头文案（首页 [LauncherListItem.DayHeader] 渲染与分类/资产/标签统计页共用，单一真源）。
 *
 * 按周期类型决定是否带月/年上下文；BY_MONTH 在月起始日≠1（周期跨自然月）时也带月份。
 * 纯函数：所有本地化字符串（[dayLabel]/[monthLabel]/[yearLabel]/[dayTypeSuffix]）由 @Composable
 * 调用方解析后传入，故本函数不依赖 Compose、可纯 JVM 单测。
 *
 * 沿用首页既有口径：BY_YEAR 带 [dayTypeSuffix]，DATE_RANGE/ALL 不带（本次不统一，见 spec F8）。
 *
 * @param dayTypeSuffix 已解析的「(今天)/(昨天)/(前天)」或空串
 * @param byMonthCrossesNaturalMonth BY_MONTH 周期是否跨自然月（= 归一化后 monthStartDay≠1）
 */
internal fun recordDayHeaderDateText(
    type: DateSelectionTypeEnum,
    year: Int,
    month: Int,
    day: Int,
    dayTypeSuffix: String,
    dayLabel: String,
    monthLabel: String,
    yearLabel: String,
    byMonthCrossesNaturalMonth: Boolean,
): String = when (type) {
    DateSelectionTypeEnum.BY_DAY ->
        "$day$dayLabel$dayTypeSuffix"

    DateSelectionTypeEnum.BY_MONTH ->
        if (byMonthCrossesNaturalMonth) {
            "$month$monthLabel$day$dayLabel$dayTypeSuffix"
        } else {
            "$day$dayLabel$dayTypeSuffix"
        }

    DateSelectionTypeEnum.BY_YEAR ->
        "$month$monthLabel$day$dayLabel$dayTypeSuffix"

    DateSelectionTypeEnum.DATE_RANGE, DateSelectionTypeEnum.ALL ->
        "$year$yearLabel$month$monthLabel$day$dayLabel"
}
```

- [ ] **Step 4: 跑测试确认通过**

Run: `./gradlew :feature:records:testDebugUnitTest --tests "*RecordDayGroupingTest" --no-daemon --console=plain`
Expected: BUILD SUCCESSFUL（只信 `grep -E '^BUILD (SUCCESSFUL|FAILED)'`）

- [ ] **Step 5: 提交**

```bash
git add feature/records/src/main/kotlin/cn/wj/android/cashbook/feature/records/viewmodel/RecordDayGrouping.kt feature/records/src/test/kotlin/cn/wj/android/cashbook/feature/records/viewmodel/RecordDayGroupingTest.kt
git commit -m "[feat|feature:records|统计日期头][公共]新增 recordDayHeaderDateText 纯函数+单测（日期头按周期自适应单一真源）"
```

---

### Task 2: 统计页（分类/标签/资产）日期头用纯函数

**Files:**
- Modify: `feature/records/src/main/kotlin/cn/wj/android/cashbook/feature/records/view/RecordMonthSummaryHeader.kt`（`RecordDayHeader` 加参 + 调纯函数）
- Modify: `feature/records/src/main/kotlin/cn/wj/android/cashbook/feature/records/viewmodel/TypedAnalyticsViewModel.kt`（暴露 `monthStartDay`）
- Modify: `feature/records/src/main/kotlin/cn/wj/android/cashbook/feature/records/viewmodel/AssetInfoContentViewModel.kt`（暴露 `monthStartDay`）
- Modify: `feature/records/src/main/kotlin/cn/wj/android/cashbook/feature/records/screen/TypedAnalyticsScreen.kt`（传 type + flag）
- Modify: `feature/records/src/main/kotlin/cn/wj/android/cashbook/feature/records/screen/AssetInfoContentScreen.kt`（传 type + flag）

**Interfaces:**
- Consumes: `recordDayHeaderDateText(...)`（Task 1）
- Produces: `RecordDayHeader(item, dateSelectionType: DateSelectionTypeEnum, byMonthCrossesNaturalMonth: Boolean)`；`TypedAnalyticsViewModel.monthStartDay: StateFlow<Int>`；`AssetInfoContentViewModel.monthStartDay: StateFlow<Int>`

- [ ] **Step 1: 改 `RecordDayHeader` 签名 + 调纯函数**（`RecordMonthSummaryHeader.kt`）

文件顶部 import 区加：
```kotlin
import cn.wj.android.cashbook.core.model.enums.DateSelectionTypeEnum
import cn.wj.android.cashbook.feature.records.viewmodel.recordDayHeaderDateText
```
将现有 `RecordDayHeader`（`:221-240`）整体替换为：
```kotlin
/**
 * 记录列表按日分组头（资产/分类/标签统计共用）。
 * 日期文案按 [dateSelectionType] 自适应（与首页 DayHeaderItem 单一真源，见 [recordDayHeaderDateText]）。
 *
 * @param dateSelectionType 当前周期类型
 * @param byMonthCrossesNaturalMonth BY_MONTH 周期是否跨自然月（monthStartDay≠1）
 */
@Composable
internal fun RecordDayHeader(
    item: LauncherListItem.DayHeader,
    dateSelectionType: DateSelectionTypeEnum,
    byMonthCrossesNaturalMonth: Boolean,
) {
    val dayTypeSuffix = when (item.dayType) {
        0 -> stringResource(id = R.string.today_with_brackets)
        -1 -> stringResource(id = R.string.yesterday_with_brackets)
        -2 -> stringResource(id = R.string.before_yesterday_with_brackets)
        else -> ""
    }
    val dateArray = item.dateStr.split("-")
    val year = dateArray.getOrNull(0)?.toIntOrNull() ?: 0
    val month = dateArray.getOrNull(1)?.toIntOrNull() ?: 0
    val dateText = recordDayHeaderDateText(
        type = dateSelectionType,
        year = year,
        month = month,
        day = item.day,
        dayTypeSuffix = dayTypeSuffix,
        dayLabel = stringResource(id = R.string.day),
        monthLabel = stringResource(id = R.string.month),
        yearLabel = stringResource(id = R.string.year),
        byMonthCrossesNaturalMonth = byMonthCrossesNaturalMonth,
    )
    Column {
        Text(
            text = dateText,
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

- [ ] **Step 2: VM 暴露 `monthStartDay`**

`TypedAnalyticsViewModel.kt`：在 `_monthStartDay`（`:92-94`）声明之后追加：
```kotlin
/** 月起始日（归一化后）供 UI 计算日期头是否跨自然月 */
val monthStartDay: StateFlow<Int> = _monthStartDay
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), 1)
```
`AssetInfoContentViewModel.kt`：在 `_monthStartDay`（`:85-87`）声明之后追加同样的 `monthStartDay`（imports `stateIn`/`SharingStarted`/`StateFlow` 两文件均已存在）。

- [ ] **Step 3: 统计屏传 type + flag**

`TypedAnalyticsScreen.kt`：
- 在 `TypedAnalyticsScreen` 签名加默认参数：`byMonthCrossesNaturalMonth: Boolean = false,`
- `RecordDayHeader(item = item)` 调用处（`:201`）改为：
```kotlin
RecordDayHeader(
    item = item,
    dateSelectionType = dateSelection.type,
    byMonthCrossesNaturalMonth = byMonthCrossesNaturalMonth,
)
```
- `TypedAnalyticsRoute` 内收集并下传：在 `val summary by ...`（`:77`）后加
```kotlin
val monthStartDay by viewModel.monthStartDay.collectAsStateWithLifecycle()
```
并在 `TypedAnalyticsScreen(...)` 调用（`:81-99`）补 `byMonthCrossesNaturalMonth = monthStartDay != 1,`
- 顶部 import 加 `import cn.wj.android.cashbook.core.model.entity.DateSelectionEntity`（已存在）；`dateSelection.type` 来自 `DateSelectionEntity.type`，无需额外 import。

`AssetInfoContentScreen.kt`：
- `AssetInfoContentScreen` 签名加：`byMonthCrossesNaturalMonth: Boolean = false,`
- `RecordDayHeader(item = item)`（`:152`）改为带 `dateSelectionType = dateSelection.type, byMonthCrossesNaturalMonth = byMonthCrossesNaturalMonth`
- `AssetInfoContentRoute` 在 `val summary by ...`（`:64`）后加 `val monthStartDay by viewModel.monthStartDay.collectAsStateWithLifecycle()`，并在 `AssetInfoContentScreen(...)` 调用补 `byMonthCrossesNaturalMonth = monthStartDay != 1,`

- [ ] **Step 4: 加 BY_MONTH 跨月截图用例（守护 F7，基线 CI 录）**

在 `TypedAnalyticsScreenScreenshotTests.kt` 仿 `typedAnalyticsScreen_fixedPeriod_multipleThemes`（`:138`）增一例：`dateSelection = DateSelectionEntity.ByMonth(YearMonth.of(2024,1))` + `byMonthCrossesNaturalMonth = true`，`overrideFileName = "TypedAnalyticsScreen_byMonthCrossing"`。

- [ ] **Step 5: 编译 + 测试编译验证**

Run: `./gradlew :feature:records:testDebugUnitTest --no-daemon --console=plain`
Expected: BUILD SUCCESSFUL（截图本机变化勿判失败，基线 CI 录）

- [ ] **Step 6: 提交**

```bash
git add feature/records/src/main/kotlin/cn/wj/android/cashbook/feature/records/view/RecordMonthSummaryHeader.kt feature/records/src/main/kotlin/cn/wj/android/cashbook/feature/records/viewmodel/TypedAnalyticsViewModel.kt feature/records/src/main/kotlin/cn/wj/android/cashbook/feature/records/viewmodel/AssetInfoContentViewModel.kt feature/records/src/main/kotlin/cn/wj/android/cashbook/feature/records/screen/TypedAnalyticsScreen.kt feature/records/src/main/kotlin/cn/wj/android/cashbook/feature/records/screen/AssetInfoContentScreen.kt feature/records/src/test/kotlin/cn/wj/android/cashbook/feature/records/screen/TypedAnalyticsScreenScreenshotTests.kt
git commit -m "[fix|feature:records|统计日期头][公共]分类/标签/资产统计页日期头按周期自适应（全年/全部/跨自然月按月带月年上下文）"
```

---

### Task 3: 首页 `DayHeaderItem` 改用纯函数（单一真源，基线 0 diff）

**Files:**
- Modify: `feature/records/src/main/kotlin/cn/wj/android/cashbook/feature/records/screen/LauncherContentScreen.kt`（`DayHeaderItem`/`FrontLayerContent`/`LauncherContentScreen` 加 flag；`DayHeaderItem` 改用纯函数）
- Modify: `feature/records/src/main/kotlin/cn/wj/android/cashbook/feature/records/viewmodel/LauncherContentViewModel.kt`（暴露 `monthStartDay`）

**Interfaces:**
- Consumes: `recordDayHeaderDateText(...)`（Task 1）
- Produces: `LauncherContentViewModel.monthStartDay: StateFlow<Int>`

- [ ] **Step 1: VM 暴露 `monthStartDay`**

`LauncherContentViewModel.kt`：在 `_monthStartDay`（`:141-143`）后追加：
```kotlin
/** 月起始日（归一化后）供 UI 计算日期头是否跨自然月 */
val monthStartDay: StateFlow<Int> = _monthStartDay
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 1)
```

- [ ] **Step 2: `DayHeaderItem` 改用纯函数**（`LauncherContentScreen.kt`）

顶部 import 加：
```kotlin
import cn.wj.android.cashbook.feature.records.viewmodel.recordDayHeaderDateText
```
`DayHeaderItem` 签名加参数 `byMonthCrossesNaturalMonth: Boolean,`（放在 `dateSelectionType` 之后）。
将其内 `val dateText = when (dateSelectionType) { ... }`（`:586-593`）替换为：
```kotlin
val dateText = recordDayHeaderDateText(
    type = dateSelectionType,
    year = year,
    month = month,
    day = day,
    dayTypeSuffix = dayTypeSuffix,
    dayLabel = stringResource(id = R.string.day),
    monthLabel = stringResource(id = R.string.month),
    yearLabel = stringResource(id = R.string.year),
    byMonthCrossesNaturalMonth = byMonthCrossesNaturalMonth,
)
```
（`year`/`month`/`day`/`dayTypeSuffix` 上方已定义；`DateSelectionTypeEnum` 已 import。）

- [ ] **Step 3: 透传 flag 到 DayHeaderItem**

- `FrontLayerContent`（`:481`）签名加 `byMonthCrossesNaturalMonth: Boolean,`；调用 `DayHeaderItem(...)`（`:528`）补 `byMonthCrossesNaturalMonth = byMonthCrossesNaturalMonth,`
- `LauncherContentScreen`（`:172`）签名加默认参数 `byMonthCrossesNaturalMonth: Boolean = false,`；调用 `FrontLayerContent(...)`（`:289`）补 `byMonthCrossesNaturalMonth = byMonthCrossesNaturalMonth,`
- `LauncherContentRoute`（`:124`）：在 `val pagingItems = ...`（`:138`）后加 `val monthStartDay by viewModel.monthStartDay.collectAsStateWithLifecycle()`，并在 `LauncherContentScreen(...)` 调用（`:140`）补 `byMonthCrossesNaturalMonth = monthStartDay != 1,`

- [ ] **Step 4: 编译验证 + 首页基线 0 diff 自检**

Run: `./gradlew :feature:records:testDebugUnitTest --no-daemon --console=plain`
Expected: BUILD SUCCESSFUL。
说明：首页 4 个截图用例均 `ByMonth`+默认设置（flag=false）→ `recordDayHeaderDateText` BY_MONTH 非跨月分支输出 `"${day}日$suffix"` 与原内联逐字一致 → CI verify 首页 `LauncherContentScreen*` 基线应 0 diff（本地不判）。

- [ ] **Step 5: 提交**

```bash
git add feature/records/src/main/kotlin/cn/wj/android/cashbook/feature/records/screen/LauncherContentScreen.kt feature/records/src/main/kotlin/cn/wj/android/cashbook/feature/records/viewmodel/LauncherContentViewModel.kt
git commit -m "[refactor|feature:records|统计日期头][公共]首页 DayHeaderItem 改用 recordDayHeaderDateText 单一真源（行为不变+跨自然月按月）"
```

---

### Task 4: Issue 2 — 模拟器复现 + 根因定位（investigation）

**Files:** 无代码改动；产出根因证据（记入下一任务提交说明 / 临时笔记）。

**前置：** 模拟器就绪（Task 6 的环境步骤可前置到此）。装本 worktree 的非 Dev Debug 包。

- [ ] **Step 1: 内存预检**（CLAUDE.local.md 强制）

Run（PowerShell）：`$os=Get-CimInstance Win32_OperatingSystem; "Avail: {0:N0}MB Used%: {1:N1}" -f ($os.FreePhysicalMemory/1024), ((1-$os.FreePhysicalMemory/$os.TotalVisibleMemorySize)*100)`
可用 <1000MB 或 >90% → 停下问用户。

- [ ] **Step 2: 复现**

开首页抽屉（点汉堡菜单或左缘右滑）→ 分别用 `adb shell input keyevent KEYCODE_BACK` 与手势返回，观察抽屉是否收起。同时 `adb logcat` 捕获。确认「右侧抽屉」实指代码里左侧导航抽屉。

- [ ] **Step 3: 定位根因（不预设 LauncherScreen 即问题点）**

至少核验候选：①`drawerState.isOpen` 读 settled `currentValue` 在开动画窗口为 Closed → BackHandler 禁用（快速「开→立刻返回」透传）；②`shouldDisplayDrawerSheet` 与 `drawerState` 双源 desync 致 `LaunchedEffect` 不重发；③Material3 `ModalNavigationDrawer` 内置 predictive-back 与显式 BackHandler 竞争；④keyevent vs predictive-back/手势返回行为差异。记录确证证据（logcat/复现步骤），判断是否可在 Robolectric 复现。

- [ ] **Step 4: 输出根因结论**

明确：根因 = <实证结论>；选定修复方向；Robolectric 是否可复现（决定 Task 5 测试是否能先红）。**连续 2 轮假设被推翻仍未定位 → 按 CLAUDE.md「排查暂停」格式停下问用户。**

---

### Task 5: Issue 2 — 回归测试 + 最小修复

**Files:**
- Test: `feature/settings/src/test/kotlin/cn/wj/android/cashbook/feature/settings/screen/LauncherScreenBackHandlerTest.kt`（新增 Robolectric Compose 交互测试）
- Modify: `feature/settings/src/main/kotlin/cn/wj/android/cashbook/feature/settings/screen/LauncherScreen.kt`（按 Task 4 根因最小修复）

**Interfaces:**
- Consumes: Task 4 根因结论。

- [ ] **Step 1: 写回归测试（守护"返回收起 + 关闭后可重开"，直击 F4 脱同步路径）**

新建测试，渲染 `LauncherScreen(uiState=Success, shouldDisplayDrawerSheet=true, …)`，用 `androidx.activity.ComponentActivity` + `createAndroidComposeRule` 触发系统返回（`activityRule` / `Espresso.pressBack()` 或 `onBackPressedDispatcher.onBackPressed()`），断言：①返回后触发 `onRequestDismissDrawerSheet`（用回调计数）；②随后 `shouldDisplayDrawerSheet=false` 重组下再 `displayDrawerSheet` 能重新打开（断言 `drawerState`/回调）。
> 测试具体 API 以 Task 4 是否可在 Robolectric 复现为准：可复现则先确保**红**；不可复现（如纯 predictive-back/手势特性）则该测试作"期望行为守护"（可能本就绿），并在 KDoc 注明"真实场景由 Task 6 设备 journey 覆盖"。
> 本机 Robolectric 需 `android-all-instrumented` 缓存（CLAUDE.local.md）；缺则该测试在 CI 跑，本地至少保证编译通过。

- [ ] **Step 2: 跑测试（按根因预期红/绿）**

Run: `./gradlew :feature:settings:testDebugUnitTest --tests "*LauncherScreenBackHandlerTest" --no-daemon --console=plain`

- [ ] **Step 3: 按根因施最小修复**

在 `LauncherScreen` 函数体内最小修复（**禁止改签名**避免连带 4 处截图测试 + Navigation）。候选（按 Task 4 结论选）：
```kotlin
// 候选 A：enabled 改用意图源（规避 drawerState.isOpen 读 settled currentValue 的动画窗口空档）
BackHandler(enabled = shouldDisplayDrawerSheet) {
    onRequestDismissDrawerSheet()
}
```
**硬约束（F4）**：修复**必须仍调 `onRequestDismissDrawerSheet()`**（VM 为唯一意图源）；**禁止**直接 `drawerState.close()` 而不回写 `shouldDisplayDrawerSheet`（否则菜单再开失效）。若根因为 Material3 内置 BackHandler 竞争，则按实证对症（如调整 enabled 条件/移除冗余）。

- [ ] **Step 4: 跑测试确认通过**

Run: `./gradlew :feature:settings:testDebugUnitTest --tests "*LauncherScreenBackHandlerTest" --no-daemon --console=plain`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: 提交**

```bash
git add feature/settings/src/main/kotlin/cn/wj/android/cashbook/feature/settings/screen/LauncherScreen.kt feature/settings/src/test/kotlin/cn/wj/android/cashbook/feature/settings/screen/LauncherScreenBackHandlerTest.kt
git commit -m "[fix|feature:settings|抽屉返回][公共]修复首页抽屉返回键不收起（<根因一句话>）+ Robolectric 交互回归测试"
```

---

### Task 6: 真机 journey 验证（备份恢复闭环 + Issue 1/2）

**Files:**
- Create: `docs/testing/reports/2026-06-27-stats-grouping-drawer-backup-journey.md`（**脱敏**报告）
- evidence（DB dump/zip/截图）只入 `docs/testing/reports/evidence/`（gitignored）

- [ ] **Step 1: 环境就绪**

内存预检（同 Task 4 Step 1）。`android` 启模拟器（`Medium_Phone`，挂死则 `bp_api34`）；装**非 Dev** Debug 包（`assembleOnlineDebug`/`OfflineDebug`，使恢复缓存自动清）。代理/offline 按 CLAUDE.local.md。

- [ ] **Step 2: 备份恢复闭环验证**

①`adb push "D:\jiewang41\Downloads\Cashbook_Backup_File_20260627164133.zip"` 到可授子目录（如 `/sdcard/Download/CashbookBackup/`）。②app 备份恢复入口（`OpenDocumentTree` SAF 选目录，半手动 tap 授权）恢复 → 验 legacy db-only 恢复成功。③启动触发 BLOB→file backfill + finalAmount 净自付重算；验图片可显示（双轨读）。④app 内**再产一个新格式备份**（含 manifest+settings.json+record_images/）。⑤恢复该新备份 → 端到端验新功能 round-trip。
> 若 db 无图片记录 → 新备份无 record_images/，报告如实标注未覆盖项。若被迫 `run-as` 直推 db → 报告标注"未经恢复代码路径、不算备份验证"。

- [ ] **Step 3: Issue 1 功能验证**

进分类/资产/标签统计 → 周期切「全年」「全部」→ 截图确认日期头带月/年上下文（如「6月27日」「2026年6月27日」）。若设非自然月起始日（设置改 monthStartDay≠1）→ 月视图头带月份。

- [ ] **Step 4: Issue 2 功能验证**

首页开抽屉 → keyevent 返回 + 手势返回（模拟器开手势导航/predictive back）→ 确认抽屉收起；再点菜单确认能重新打开。报告标注所用导航模式（F9）。

- [ ] **Step 5: 写脱敏报告 + 清理**

报告**不贴**真实金额/资产名/备注/收据图、不贴 DB 原始行、截图打码；evidence 入 gitignored 目录。验完 `adb shell pm clear <pkg>` + 重置/删 AVD。
```bash
git add docs/testing/reports/2026-06-27-stats-grouping-drawer-backup-journey.md
git commit -m "[test|统计·抽屉·备份|真机验证][公共]模拟器 journey 验证报告（日期头自适应+抽屉返回+备份恢复闭环，脱敏）"
```

---

### Task 7: 节点 2 full-review + finishing

- [ ] **Step 1: 完整链路自检**

`./gradlew :feature:records:testDebugUnitTest :feature:settings:testDebugUnitTest --no-daemon --console=plain` 全绿；`:app:compileOnlineDebugKotlin` 验跨模块 Hilt 图。

- [ ] **Step 2: 节点 2 full-review**

`Skill comprehensive-review:full-review` 对本次 git diff 全维度审查（diff 含交互/状态机改动，从严跑；按 Phase 2→3 checkpoint 推进）。Critical/High 交付前修复或经用户授权放行。

- [ ] **Step 3: finishing**

`Skill superpowers:finishing-a-development-branch` 决定合入方式（PR 让 CI 录截图基线）。

---

## Self-Review（plan vs spec）

- **Spec 覆盖**：Issue 1（Task 1-3）✓；F7 跨自然月（Task 1 纯函数 + Task 2/3 flag）✓；Issue 2 根因先实证（Task 4）+ 修复+回归测试（Task 5）✓；备份闭环 F1（Task 6 Step 2）✓；F2 脱敏（Task 6 Step 5）✓；F5 基线 CI（全程约束）✓；F6 回归测试（Task 5）✓；F8 保留+注明（Task 1 KDoc）✓；F9 导航模式（Task 6 Step 4）✓；节点 2（Task 7）✓。
- **Placeholder 扫描**：Issue 1 全为实代码；Issue 2 修复因根因待实证给"候选 A + 硬约束"而非空泛占位（已显式标注依赖 Task 4 结论），属诚实的实证优先，非 placeholder。
- **类型一致性**：`recordDayHeaderDateText` 签名 Task 1 定义、Task 2/3 调用一致；`monthStartDay: StateFlow<Int>` 三 VM 一致；`byMonthCrossesNaturalMonth: Boolean` 全程一致；`RecordDayHeader` 新签名 Task 2 定义、调用点一致。
