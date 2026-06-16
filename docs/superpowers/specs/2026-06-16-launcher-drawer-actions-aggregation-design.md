# M1 抽屉回调聚合为 LauncherDrawerActions — 设计文档

- 日期：2026-06-16
- 类型：架构重构（功能等价）
- 来源：报销管理界面 #1 的 follow-up backlog（M1 抽屉回调 4 层透传聚合）
- 模块：feature/settings（主）、app（调用方）

## 1. 背景与目标

首页左侧抽屉（`LauncherSheet`）含 7 个菜单项（我的账本/我的资产/我的分类/我的标签/待报销/设置/关于我们），各对应一个点击回调。这 7 个回调从 app 导航宿主一路透传到叶子 Composable，跨 **4 层**、共 28 处参数声明，且中途经历一次命名翻译（`onRequestNaviToXxx` ↔ `onXxxClick`）。报销界面 #1 新增「待报销」项时，这条透传链每层都要手工加一个参数，暴露了样板代码膨胀问题。

**目标**：把 7 个回调收敛为单个 `LauncherDrawerActions` 数据类，全链路（1→5 层）只透传一个参数。

**功能等价**：不改 UI、行为、视觉、可观察副作用（含 dismiss 抽屉的时机与顺序）。等价性以已入库的 `LauncherScreen` Roborazzi 截图基线 `verifyRoborazzi` **0 diff** 为客观判据。

## 2. 非目标（YAGNI）

- 不改 `NavExtensions` 的 `naviToXxx` 命名（超出 M1 scope，影响其他调用方）。
- 不把数据类下沉到 `core/ui`（无第二个抽屉复用，违反 YAGNI）。
- 不改 `LauncherViewModel` / `LauncherUiState` / 抽屉以外的任何逻辑。

## 3. 当前结构（实证）

| 层 | 文件:行 | 函数 | 回调命名 |
|---|---|---|---|
| 5（根/宿主） | `app/.../ui/MainApp.kt:414` | `settingsLauncherScreen(...)` | `onRequestNaviToXxx = navController::naviToXxx` |
| 4 | `feature/settings/.../navigation/SettingsNavigation.kt:73` | `NavGraphBuilder.settingsLauncherScreen` | `onRequestNaviToXxx` |
| 3 | `feature/settings/.../screen/LauncherScreen.kt:63` | `LauncherRoute` | `onRequestNaviToXxx`（接收后给每个加 `dismissDrawerSheet()` 包装，line 83-110） |
| 2 | `feature/settings/.../screen/LauncherScreen.kt:134` | `LauncherScreen` | `onXxxClick` |
| 1（叶子） | `feature/settings/.../screen/LauncherScreen.kt:217` | `LauncherSheet` | `onXxxClick` → `NavigationDrawerItem.onClick` |

7 个回调：`onMyAssetClick` / `onMyBookClick` / `onMyCategoryClick` / `onMyTagClick` / `onReimbursementClick` / `onSettingClick` / `onAboutUsClick`。

**dismiss 副作用现状**（`LauncherRoute` line 83-110）：每个上游 `onRequestNaviToXxx` 被包成 `{ onRequestNaviToXxx.invoke(); viewModel.dismissDrawerSheet() }` —— **先 navi 再 dismiss**。

## 4. 设计

### 4.1 数据类

`LauncherScreen.kt` 顶层新增（`internal`，与现有 Composable 一致）：

```kotlin
internal data class LauncherDrawerActions(
    val onMyAssetClick: () -> Unit,
    val onMyBookClick: () -> Unit,
    val onMyCategoryClick: () -> Unit,
    val onMyTagClick: () -> Unit,
    val onReimbursementClick: () -> Unit,
    val onSettingClick: () -> Unit,
    val onAboutUsClick: () -> Unit,
)
```

字段命名采用 `onXxxClick`（跟随叶子节点 `LauncherSheet`/`LauncherScreen` 现有命名 + Composable 社区惯例：actions 抽象为 UI 事件 click，调用方实现恰为 nav 仅是偶然）。

### 4.2 dismiss 包装位置：Route 内 map

`LauncherRoute` 接收上游纯 nav `actions` 后，内部派生一份 `dismissActions`，每个 lambda 包成 `{ it(); viewModel.dismissDrawerSheet() }`（保留**先 navi 再 dismiss** 顺序），传给 `LauncherScreen`。上游（SettingsNavigation/MainApp）不感知 dismiss 逻辑——职责正确（抽屉开关是 Launcher 内部事务）。

伪代码：

```kotlin
internal fun LauncherRoute(
    actions: LauncherDrawerActions,
    modifier: Modifier = Modifier,
    viewModel: LauncherViewModel = hiltViewModel(),
    content: @Composable (() -> Unit) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    fun wrap(action: () -> Unit): () -> Unit = {
        action()
        viewModel.dismissDrawerSheet()
    }
    val dismissActions = LauncherDrawerActions(
        onMyAssetClick = wrap(actions.onMyAssetClick),
        onMyBookClick = wrap(actions.onMyBookClick),
        onMyCategoryClick = wrap(actions.onMyCategoryClick),
        onMyTagClick = wrap(actions.onMyTagClick),
        onReimbursementClick = wrap(actions.onReimbursementClick),
        onSettingClick = wrap(actions.onSettingClick),
        onAboutUsClick = wrap(actions.onAboutUsClick),
    )
    LauncherScreen(
        shouldDisplayDrawerSheet = viewModel.shouldDisplayDrawerSheet,
        onRequestDisplayDrawerSheet = viewModel::displayDrawerSheet,
        onRequestDismissDrawerSheet = viewModel::dismissDrawerSheet,
        uiState = uiState,
        actions = dismissActions,
        content = { content { viewModel.displayDrawerSheet() } },
        modifier = modifier,
    )
}
```

> 注：`wrap` 在 composable 内每次 recomposition 重建 lambda。现状每个 trailing lambda 同样每次重建（line 83-110 内联 lambda），因此**无行为/性能回归**。此处不引入 `remember`，保持与现状等价（避免引入新行为）。

### 4.3 各层签名

- **MainApp.kt:414**：`settingsLauncherScreen(actions = LauncherDrawerActions(onMyAssetClick = navController::naviToMyAsset, ...), content = {...})`
- **SettingsNavigation.kt:73**：`settingsLauncherScreen(actions: LauncherDrawerActions, content: ...)` → 体内 `LauncherRoute(actions = actions, content = content)`
- **LauncherRoute**：见 4.2
- **LauncherScreen.kt:134**：`actions: LauncherDrawerActions` 替换 7 个 `onXxxClick`；体内 `LauncherSheet(... actions = actions ...)`
- **LauncherSheet.kt:217**：`actions: LauncherDrawerActions` 替换 7 个 `onXxxClick`；`NavigationDrawerItem.onClick = actions.onMyBookClick` 等

KDoc 同步：删除各函数 7 个 `@param onXxxClick`，改为 `@param actions 抽屉菜单点击回调聚合`。

## 5. 测试影响

- **`LauncherScreenScreenshotTests.kt`**（4 个 @Test，line 50/70/91/113）：每处 7 个 `onXxxClick = {}` → 1 个 `actions = LauncherDrawerActions(onMyAssetClick = {}, ...)`。**4 屏截图基线 `verifyRoborazzi` 必须 0 diff**（无任何 UI 改动）。
- **`LauncherViewModelTest.kt`**：ViewModel 未改，预期零影响（执行时确认编译+通过）。
- 模块测试源集整体编译：feature/settings 任一测试文件签名不匹配会致整模块 `testDebugUnitTest` 编译失败（CLAUDE.md 既往踩坑），故 screenshot 测试必须同步改。

## 6. 验收标准

1. `:feature:settings:testDebugUnitTest` 全绿
2. `:feature:settings:verifyRoborazziDebug` **0 diff**（4 屏 LauncherScreen 基线守护等价性）
3. `:app:assembleOnlineDebug` 编译通过（MainApp 调用方接入验证）
4. `:feature:settings:spotlessCheck` 通过

## 7. 风险与回滚

- 风险极低：纯参数聚合，无逻辑分支变更，无新错误路径。最大风险是 dismiss 顺序漂移 —— 由 4.2「先 navi 再 dismiss」固定 + 截图基线（抽屉默认关闭态，dismiss 行为不在截图内）+ 编译期类型检查共同约束。
- 回滚：单一 commit，`git revert` 即可。
- TRANSFER/金额/数据层：不涉及。

## 8. 实施阶段（产物划分，不计工时）

单会话可完成（controller 串行 TDD）：
- Phase 1：新增数据类 + 改 4 层 Composable 签名（LauncherSheet→Screen→Route→SettingsNavigation→MainApp）+ 同步 LauncherScreenScreenshotTests
- Phase 2：`testDebugUnitTest` + `verifyRoborazziDebug` 0 diff + `assembleOnlineDebug` + spotless 全验收
