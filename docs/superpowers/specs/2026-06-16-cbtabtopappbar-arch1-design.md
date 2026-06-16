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
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    indicatorColor: Color = MaterialTheme.colorScheme.primary,
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
- 差异项参数化：`indicatorColor`
- tab 本身（含 per-tab `selectedContentColor`/`text`）留在 `tabs` slot 由调用方控制
- `actions` 透传（当前两处未用，默认 `{}`，与 CbTopAppBar 对齐、低成本前瞻）
- indicator 加 `selectedTabIndex < tp.size` 越界守卫（正常不越界，输出与原一致；防御性，沿用 CbTabRow 默认 indicator 写法）

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
| `MyCategoriesScreenScreenshotTests`（Success/Loading/empty 三态，含 topbar）`verifyRoborazziDebug` **0 diff** | 重构未改变渲染的硬证据；Loading 态验证 else 分支等价 |
| `EditRecordScreenRenderRegressionTest` + `MyCategoriesScreenRenderRegressionTest`（`assertIsDisplayed`） | BUG-1 回归：tab + body 仍可见不塌陷 |
| 新增 `CbTabTopAppBar` core/design 截图测试（沿用 `captureMultiTheme`） | 锁定新组件自身渲染基线 |

注：EditRecordScreen 的 topbar 无截图覆盖，靠 RenderRegressionTest 守护；MyCategoriesScreen 整屏截图覆盖 topbar，是等价性主证据。

## 5. 防回归定位（诚实说明）

**软约束**：提供正确便利组件 + CLAUDE.md 规则引导，使新代码自然走 `fillMaxWidth`。**不强制**——未来仍可有人手写 `CbTopAppBar + CbTabRow(fillMaxSize)`。lint 硬检测 Compose modifier 链语义可行性低，不在本次范围。

## 6. 非目标

- 不做 lint 规则
- 不改 AnalyticsScreen / DateSelectionPopup 的 CbTabRow
- 不改变任何现有渲染输出（纯等价重构）
- 不引入 tab 数据模型（最小外壳，tab 由 slot 提供，core/design 不承载 tab 语义）

## 7. 验收

1. `:core:design` 编译通过 + 新组件截图测试基线生成
2. `:feature:types:verifyRoborazziDebug` 0 diff（含 MyCategoriesScreen 三态）
3. `:feature:types` + `:feature:records` 的 RenderRegressionTest 通过
4. spotless 通过
5. 节点2 评审无 Critical/High
