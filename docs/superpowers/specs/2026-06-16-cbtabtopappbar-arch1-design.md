# CbTabTopAppBar 下沉封装设计（ARCH-1 防 BUG-1 同类回归）

- 日期：2026-06-16
- 类型：core/design 组件抽取 + 调用方重构（渲染等价）
- 关联：BUG-1（记账/我的分类支出分类不渲染，main `20a0e502` 修复）、CLAUDE.md 防回归规则（CbTabRow 在 CbTopAppBar title 须 fillMaxWidth）

## 1. 背景与目标

BUG-1 根因：`CbTabRow(Modifier.fillMaxSize())` 置于 `CbTopAppBar` 的 `title` 槽 → Material3 `TopAppBar` 按 title 撑满全屏高 → `CbScaffold` body 区塌陷为 0 高 → 该屏内容（分类网格）完全不渲染。修复为 `fillMaxSize → fillMaxWidth`（2 行）。

该误用 lint 不覆盖（Compose modifier 链语义检测难度高），当前仅靠 CLAUDE.md 规则 + 回归测试防回归。**ARCH-1 目标**：把"CbTopAppBar + title 内 fillMaxWidth 的 CbTabRow"固化为 core/design 组件 `CbTabTopAppBar`，`fillMaxWidth` 写死在组件内、不向调用方暴露 modifier，从 API 层使新代码自然走正确路径。

## 2. 范围

全项目"CbTabRow 放在 CbTopAppBar title"模式经核验**只有 2 处**：

- `feature/types/.../MyCategoriesScreen.kt:916`（MyCategoriesTopBar）
- `feature/records/.../EditRecordScreen.kt:1035`（EditRecordTopBar）

两者除 `indicatorColor`（EditRecord = `selectedTab.typeColor` 随 tab 变 / MyCategories = 固定 `onTertiaryContainer`）与 tab 文本颜色、uiState 守卫类型外**完全同构**。

**不纳入**：`AnalyticsScreen.kt:507,717` 的 CbTabRow（在 body 区，非 topbar title，fillMaxWidth 正常用法）、`core/ui/DateSelectionPopup.kt:187`（dropdown 内）。

## 3. 设计

### 3.1 组件 API（新增于 `core/design/component/TopAppBar.kt`）

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CbTabTopAppBar(
    selectedTabIndex: Int,
    indicatorColor: Color,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
    tabs: @Composable () -> Unit,
) {
    CbTopAppBar(onBackClick = onBackClick, modifier = modifier, actions = actions, title = {
        CbTabRow(
            modifier = Modifier.fillMaxWidth(),   // 防回归核心：写死
            selectedTabIndex = selectedTabIndex,
            containerColor = Color.Unspecified,
            contentColor = Color.Unspecified,
            indicator = { tp ->
                if (selectedTabIndex < tp.size) {
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tp[selectedTabIndex]),
                        color = indicatorColor,
                    )
                }
            },
            divider = {},
            tabs = tabs,
        )
    })
}
```

- 固化项（两处共用）：`containerColor/contentColor = Unspecified`、`divider = {}`、indicator 用 `SecondaryIndicator + tabIndicatorOffset`
- 差异项参数化：`indicatorColor`（**必填、无默认值**——两处用色不同且都非 primary，不设默认避免未来第 3 处忘传静默落 primary，team-review R-5/I-Medium）
- tab 本身（含 per-tab `selectedContentColor`/`text`）留在 `tabs` slot 由调用方控制
- `actions` 透传（当前两处未用，默认 `{}`，与 CbTopAppBar 对齐、低成本前瞻）
- indicator 加 `selectedTabIndex < tp.size` 越界守卫：**非逐字等价**——原两处无守卫（越界抛 IndexOutOfBounds），封装内静默不画。RecordTypeCategoryEnum 恒 3 项、`ordinal ∈ {0,1,2}` 恒 `< 3`，**正常路径输出与原完全一致**（截图 0 diff 可证）；守卫仅在不可达越界态生效，是有意的防御性收敛，见 §6（team-review F-Low/R-3/I-High）

### 3.2 调用方改动（保持渲染等价）

原代码：`CbTopAppBar(title = { if (Success) { CbTabRow... } })`，非 Success 时 title 为空。等价改写为外层 if/else：

```kotlin
if (uiState is XxxUiState.Success) {
    CbTabTopAppBar(
        selectedTabIndex = selectedTab.ordinal,
        onBackClick = onBackClick,
        indicatorColor = /* EditRecord: selectedTab.typeColor; MyCategories: onTertiaryContainer */,
    ) {
        RecordTypeCategoryEnum.entries.forEach { enum ->
            CbTab(/* 原样搬入，selected/onClick/text/颜色不变 */)
        }
    }
} else {
    CbTopAppBar(title = {}, onBackClick = onBackClick)   // 等价于原"空 title"
}
```

## 4. 渲染等价性与测试策略

| 测试 | 作用 |
|---|---|
| `MyCategoriesScreenScreenshotTests`（Success/Loading/empty 三态，含 topbar）`:feature:types:verifyRoborazziDebug` **0 diff** | MyCategories 侧重构等价的像素级硬证据；Loading 态验证 else 分支等价。基线已入库（`240268ef`） |
| `EditRecordScreenScreenshotTests`（expenditure/income Success + loading 态，整屏含 topbar）`:feature:records:verifyRoborazziDebug` **0 diff** | EditRecord 侧 topbar 等价的像素级证据。**⚠️ 基线 pre-existing 缺失**（`a50d182a` 加测试时未带基线，git+磁盘双缺）——实施须**先对当前未改代码 `recordRoborazziDebug` 生成 EditRecordScreen 基线、提交作 golden**，改 EditRecordTopBar 后再 verify 0 diff（team-review F-High + 本会话核验深化） |
| `EditRecordScreenRenderRegressionTest` + `MyCategoriesScreenRenderRegressionTest`（`assertIsDisplayed`） | BUG-1 回归：**仅守 body 不塌陷**（核验 `EditRecordScreenRenderRegressionTest.kt:120` 断 "类型列表"、`MyCategoriesScreenRenderRegressionTest.kt:99` 断 "餐饮"），**不守 topbar tab**。topbar 等价由上面两行截图网负责 |
| 新增 `CbTabTopAppBar` core/design 截图测试（沿用 `captureMultiTheme`） | 锁定新组件自身渲染基线，须构造 **≥2 tab + 选中态 + indicator 可见** 的真实 tab 态，避免退化为空壳 topbar（team-review F-提示/I-Low） |

注：原 spec 误称"EditRecord topbar 无截图覆盖、靠 RenderRegressionTest 守护"——经 team-review + 核验更正：EditRecordScreenScreenshotTests **覆盖** topbar 但**基线缺失需先补**；两个 RenderRegressionTest 只守 body 塌陷不守 topbar tab。两侧 topbar 等价最终都由截图 0 diff（像素级）守护。

## 5. 防回归定位（诚实说明）

**软约束**：提供正确便利组件 + CLAUDE.md 规则引导，使新代码自然走 `fillMaxWidth`。**不强制**——未来仍可有人手写 `CbTopAppBar + CbTabRow(fillMaxSize)`。lint 硬检测 Compose modifier 链语义可行性低，不在本次范围。

**为何不选「零改动」（保持 2 处现状 + CLAUDE.md 规则 + 现有 RenderRegressionTest）**（team-review R-4 拷问）：零改动成本最低、零回归风险，但 BUG-1 防线仅剩"文档规则 + 守 body 的回归测试"，新代码仍需人工记得 fillMaxWidth。本次取舍：用户已决策做 ARCH-1（连做 backlog），抽组件换来"API 层默认正确 + 顺带补齐 EditRecord 截图网（修复 pre-existing 基线缺失）+ 新组件像素级基线"；代价是 2 处等价重构 + 其回归验证。鉴于改动可被截图 0 diff 完全守护、回滚简单（team-review R-6），净收益成立。

## 6. 非目标与等价性边界

- 不做 lint 规则
- 不改 AnalyticsScreen / DateSelectionPopup 的 CbTabRow
- 不引入 tab 数据模型（最小外壳，tab 由 slot 提供，core/design 不承载 tab 语义）
- **等价性边界（诚实标注，team-review F-Low/R-3/I-High）**：本次是"正常路径渲染等价"（截图 0 diff 守护），**非逐字行为等价**——唯一有意的语义变更是 indicator 加越界守卫，把"越界抛 IndexOutOfBounds"收敛为"越界静默不画"。当前 RecordTypeCategoryEnum 恒 3 项使该差异不可达、正常输出完全一致。除此之外不改变任何现有渲染输出。

## 7. 验收

1. `:core:design` 编译通过 + 新组件 `CbTabTopAppBar` 截图测试基线生成（含真实 tab 态：≥2 tab + 选中 + indicator 可见）
2. `:feature:types:verifyRoborazziDebug` 0 diff（含 MyCategoriesScreen Success/Loading/empty 三态）
3. `:feature:records:verifyRoborazziDebug` 0 diff（含 EditRecordScreen expenditure/income/loading 态）——**前置**：实施时先对未改代码 record 并提交 EditRecordScreen 基线作 golden
4. `:feature:types` + `:feature:records` 的 RenderRegressionTest 通过（守 BUG-1 body 不塌陷）
5. spotless 通过（新文件含 Apache License header）
6. 节点2 评审无 Critical/High
