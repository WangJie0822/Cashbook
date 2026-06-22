# M1 抽屉回调聚合为 LauncherDrawerActions 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 把首页抽屉 7 个点击回调从 4 层透传收敛为单个 `LauncherDrawerActions` 数据类，功能等价。

**Architecture:** 在 `LauncherScreen.kt` 顶层新增 `internal data class LauncherDrawerActions`（7 个 `onXxxClick` 字段）。全链路 5 层（MainApp → settingsLauncherScreen → LauncherRoute → LauncherScreen → LauncherSheet）各层 7 参数收敛为 1 个 `actions` 参数。dismiss 抽屉副作用在 `LauncherRoute` 内 `map` 包装（保留「先 navi 再 dismiss」语义）。这是不可分割的 Kotlin 编译单元 —— 签名变更跨 5 层 + 截图测试必须同步改，故单 Task 单 commit。

**Tech Stack:** Kotlin、Jetpack Compose、Material3 ModalNavigationDrawer、Roborazzi 截图测试、Hilt Navigation。

**等价判据（核心验收）:** `:feature:settings:verifyRoborazziDebug` **0 diff**（LauncherScreen 4 屏 14 张已入库基线守护）。

---

### Task 1: 全链路签名聚合（单编译单元单 commit）

**Files:**
- Modify: `feature/settings/src/main/kotlin/cn/wj/android/cashbook/feature/settings/screen/LauncherScreen.kt`（新增 data class + 改 `LauncherRoute`/`LauncherScreen`/`LauncherSheet` 3 个函数）
- Modify: `feature/settings/src/main/kotlin/cn/wj/android/cashbook/feature/settings/navigation/SettingsNavigation.kt:73-95`（改 `settingsLauncherScreen`）
- Modify: `app/src/main/kotlin/cn/wj/android/cashbook/ui/MainApp.kt:414-440`（改调用方）
- Test: `feature/settings/src/test/kotlin/cn/wj/android/cashbook/feature/settings/screen/LauncherScreenScreenshotTests.kt`（改 4 个 @Test）

---

- [ ] **Step 0: 确认 baseline 回归网就位（改代码前 verify 必须绿）**

Run:
```bash
cd "D:/Work/Workspace/Owner/Cashbook" && ./gradlew :feature:settings:verifyRoborazziDebug --offline --no-daemon --console=plain
```
Expected: `BUILD SUCCESSFUL`（当前未改代码，LauncherScreen 14 张基线与当前渲染一致）。若 FAIL，先停下排查 pre-existing drift，不要继续改代码。

---

- [ ] **Step 1: 在 `LauncherScreen.kt` 顶层新增 `LauncherDrawerActions` 数据类**

在 `LauncherScreen.kt` 中 `LauncherRoute` 函数（`@Composable internal fun LauncherRoute(`，约 line 62 的 KDoc 之前）前插入：

```kotlin
/**
 * 首页抽屉菜单点击回调聚合
 *
 * @param onMyAssetClick 我的资产点击回调
 * @param onMyBookClick 我的账本点击回调
 * @param onMyCategoryClick 我的分类点击回调
 * @param onMyTagClick 我的标签点击回调
 * @param onReimbursementClick 待报销点击回调
 * @param onSettingClick 设置点击回调
 * @param onAboutUsClick 关于我们点击回调
 */
data class LauncherDrawerActions(
    val onMyAssetClick: () -> Unit,
    val onMyBookClick: () -> Unit,
    val onMyCategoryClick: () -> Unit,
    val onMyTagClick: () -> Unit,
    val onReimbursementClick: () -> Unit,
    val onSettingClick: () -> Unit,
    val onAboutUsClick: () -> Unit,
)
```

> **勘误（实施期实证 2026-06-22）**：必须 `public`（无修饰符），不能 `internal`。`settingsLauncherScreen` 是 public 跨模块 API（app/MainApp.kt 调用并构造 `LauncherDrawerActions`），public 函数不能暴露 internal 参数类型，否则 `:feature:settings:compileDebugKotlin` 失败。详见 spec 4.1 勘误。

---

- [ ] **Step 2: 替换 `LauncherRoute` 函数整体（line 50-114）**

把现有 `LauncherRoute` 的 KDoc + 函数体（从 `/**\n * 首页显示` 第一段 KDoc 到函数闭合 `}`，即原 line 50-114）整体替换为：

```kotlin
/**
 * 首页显示
 * - 首页显示主体，提供左侧抽屉菜单、用户隐私协议弹窗、安全校验功能，具体内容显示通过 [content] 参数提供
 *
 * @param actions 抽屉菜单点击回调聚合
 * @param content 显示内容，参数 (打开抽屉) -> [Unit]
 */
@Composable
internal fun LauncherRoute(
    actions: LauncherDrawerActions,
    modifier: Modifier = Modifier,
    viewModel: LauncherViewModel = hiltViewModel(),
    content: @Composable (() -> Unit) -> Unit,
) {
    // 界面 UI 状态数据
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 点击后关闭抽屉：先执行导航再关闭抽屉（与原内联 lambda 语义一致）
    fun wrap(action: () -> Unit): () -> Unit = {
        action()
        viewModel.dismissDrawerSheet()
    }

    LauncherScreen(
        shouldDisplayDrawerSheet = viewModel.shouldDisplayDrawerSheet,
        onRequestDisplayDrawerSheet = viewModel::displayDrawerSheet,
        onRequestDismissDrawerSheet = viewModel::dismissDrawerSheet,
        uiState = uiState,
        actions = LauncherDrawerActions(
            onMyAssetClick = wrap(actions.onMyAssetClick),
            onMyBookClick = wrap(actions.onMyBookClick),
            onMyCategoryClick = wrap(actions.onMyCategoryClick),
            onMyTagClick = wrap(actions.onMyTagClick),
            onReimbursementClick = wrap(actions.onReimbursementClick),
            onSettingClick = wrap(actions.onSettingClick),
            onAboutUsClick = wrap(actions.onAboutUsClick),
        ),
        content = { content { viewModel.displayDrawerSheet() } },
        modifier = modifier,
    )
}
```

> 说明：`wrap` 在每次 recomposition 重建 lambda，与原 line 83-110 内联 lambda 行为完全一致，无性能/行为回归，故不引入 `remember`。

---

- [ ] **Step 3: 改 `LauncherScreen` 函数签名与 `LauncherSheet` 调用（原 line 116-203）**

(a) `LauncherScreen` 的 KDoc 删除 7 行 `@param onXxxClick`，替换为单行 `@param actions 抽屉菜单点击回调聚合`。

(b) 函数签名：删除这 7 个参数
```kotlin
    onMyAssetClick: () -> Unit,
    onMyBookClick: () -> Unit,
    onMyCategoryClick: () -> Unit,
    onMyTagClick: () -> Unit,
    onReimbursementClick: () -> Unit,
    onSettingClick: () -> Unit,
    onAboutUsClick: () -> Unit,
```
替换为单行（放在 `uiState: LauncherUiState,` 之后、`content: @Composable () -> Unit,` 之前，保持原位置）：
```kotlin
    actions: LauncherDrawerActions,
```

(c) 函数体内 `LauncherSheet(...)` 调用（原 line 187-196）替换为：
```kotlin
                        LauncherSheet(
                            currentBookName = uiState.currentBookName,
                            actions = actions,
                        )
```

---

- [ ] **Step 4: 改 `LauncherSheet` 函数签名与 7 处 onClick（原 line 205-296）**

(a) `LauncherSheet` 的 KDoc 删除 7 行 `@param onXxxClick`，替换为单行 `@param actions 抽屉菜单点击回调聚合`。

(b) 函数签名：删除这 7 个参数
```kotlin
    onMyAssetClick: () -> Unit,
    onMyBookClick: () -> Unit,
    onMyCategoryClick: () -> Unit,
    onMyTagClick: () -> Unit,
    onReimbursementClick: () -> Unit,
    onSettingClick: () -> Unit,
    onAboutUsClick: () -> Unit,
```
替换为（放在 `currentBookName: String,` 之后、`modifier: Modifier = Modifier,` 之前）：
```kotlin
    actions: LauncherDrawerActions,
```

(c) 函数体内 5 个 `NavigationDrawerItem` 的 `onClick` 改为 `actions.` 前缀：
- 我的账本：`onClick = onMyBookClick` → `onClick = actions.onMyBookClick`
- 我的资产：`onClick = onMyAssetClick` → `onClick = actions.onMyAssetClick`
- 我的分类：`onClick = onMyCategoryClick` → `onClick = actions.onMyCategoryClick`
- 我的标签：`onClick = onMyTagClick` → `onClick = actions.onMyTagClick`
- 待报销：`onClick = onReimbursementClick` → `onClick = actions.onReimbursementClick`
- 设置：`onClick = onSettingClick` → `onClick = actions.onSettingClick`
- 关于我们：`onClick = onAboutUsClick` → `onClick = actions.onAboutUsClick`

---

- [ ] **Step 5: 改 `SettingsNavigation.kt` 的 `settingsLauncherScreen`（line 73-95）**

(a) 文件顶部 import 区新增（与现有 import 字母序就近放置）：
```kotlin
import cn.wj.android.cashbook.feature.settings.screen.LauncherDrawerActions
```

(b) 把 `settingsLauncherScreen` 的 KDoc（line 61-72，6 个 `@param onRequestNaviToXxx`）+ 函数签名 + 函数体（line 73-95）整体替换为：
```kotlin
/**
 * 首页显示
 * - 首页显示主体，提供左侧抽屉菜单、用户隐私协议弹窗、安全校验功能，具体内容显示通过 [content] 参数提供
 *
 * @param actions 抽屉菜单点击回调聚合
 * @param content 显示内容，参数 (打开抽屉) -> [Unit]
 */
fun NavGraphBuilder.settingsLauncherScreen(
    actions: LauncherDrawerActions,
    content: @Composable (() -> Unit) -> Unit,
) {
    composable<SettingsLauncher> {
        LauncherRoute(
            actions = actions,
            content = content,
        )
    }
}
```

---

- [ ] **Step 6: 改 `MainApp.kt` 调用方（line 414-421）**

(a) 文件顶部 import 区新增：
```kotlin
import cn.wj.android.cashbook.feature.settings.screen.LauncherDrawerActions
```

(b) 把 `settingsLauncherScreen(` 调用的 7 个 `onRequestNaviToXxx = ...` 参数（line 415-421）替换为单个 `actions = ...`：
```kotlin
        settingsLauncherScreen(
            actions = LauncherDrawerActions(
                onMyAssetClick = navController::naviToMyAsset,
                onMyBookClick = navController::naviToMyBooks,
                onMyCategoryClick = navController::naviToMyCategories,
                onMyTagClick = navController::naviToMyTags,
                onReimbursementClick = navController::naviToReimbursement,
                onSettingClick = navController::naviToSetting,
                onAboutUsClick = navController::naviToAboutUs,
            ),
            content = { onRequestOpenDrawer ->
```
（`content = { onRequestOpenDrawer ->` 及其后整段 `LauncherContent(...)` 不变。）

---

- [ ] **Step 7: 改 `LauncherScreenScreenshotTests.kt` 4 个 @Test（line 50/70/91/113）**

4 个 @Test 中每处 `LauncherScreen(...)` 调用的 7 行 `onXxxClick = {},` 替换为单个 `actions` 块。即每处把：
```kotlin
                onMyAssetClick = {},
                onMyBookClick = {},
                onMyCategoryClick = {},
                onMyTagClick = {},
                onReimbursementClick = {},
                onSettingClick = {},
                onAboutUsClick = {},
```
替换为：
```kotlin
                actions = LauncherDrawerActions(
                    onMyAssetClick = {},
                    onMyBookClick = {},
                    onMyCategoryClick = {},
                    onMyTagClick = {},
                    onReimbursementClick = {},
                    onSettingClick = {},
                    onAboutUsClick = {},
                ),
```
（缩进按各处对齐；`multipleDevices` 两处在 `CashbookTheme {}` 内缩进更深，相应多缩进。测试与 SUT 同包 `cn.wj.android.cashbook.feature.settings.screen`，无需 import。）

---

- [ ] **Step 8: 编译 + 单测验证签名一致性（含截图测试整源集编译）**

Run:
```bash
cd "D:/Work/Workspace/Owner/Cashbook" && ./gradlew :feature:settings:testDebugUnitTest --offline --no-daemon --console=plain
```
Expected: `BUILD SUCCESSFUL`。若编译失败（签名漂移/缺 import），按报错定位修复后重跑。

---

- [ ] **Step 9: 等价验收 —— verifyRoborazzi 0 diff（核心判据）**

Run:
```bash
cd "D:/Work/Workspace/Owner/Cashbook" && ./gradlew :feature:settings:verifyRoborazziDebug --offline --no-daemon --console=plain
```
Expected: `BUILD SUCCESSFUL` 且无 diff。**0 diff 即证明重构功能等价**（UI 无任何变化）。若出现 diff，说明引入了非预期渲染变化，停下排查，不得 record 覆盖掩盖。

---

- [ ] **Step 10: app 调用方接入编译验证**

Run（compileOnlineDebugKotlin 比 assembleOnlineDebug 轻，足以验证 MainApp 调用签名正确）:
```bash
cd "D:/Work/Workspace/Owner/Cashbook" && ./gradlew :app:compileOnlineDebugKotlin --offline --no-daemon --console=plain
```
Expected: `BUILD SUCCESSFUL`。

---

- [ ] **Step 11: spotless 格式校验**

Run:
```bash
cd "D:/Work/Workspace/Owner/Cashbook" && ./gradlew :feature:settings:spotlessCheck :app:spotlessCheck --init-script gradle/init.gradle.kts --no-configuration-cache --offline --no-daemon --console=plain
```
Expected: `BUILD SUCCESSFUL`。若 FAIL，跑对应 `spotlessApply`（同 flag）后重跑 check。

---

- [ ] **Step 12: 提交**

```bash
cd "D:/Work/Workspace/Owner/Cashbook" && git add feature/settings/src/main/kotlin/cn/wj/android/cashbook/feature/settings/screen/LauncherScreen.kt feature/settings/src/main/kotlin/cn/wj/android/cashbook/feature/settings/navigation/SettingsNavigation.kt app/src/main/kotlin/cn/wj/android/cashbook/ui/MainApp.kt feature/settings/src/test/kotlin/cn/wj/android/cashbook/feature/settings/screen/LauncherScreenScreenshotTests.kt && git commit -F - <<'COMMIT'
[refactor|feature|settings][公共]抽屉回调聚合为 LauncherDrawerActions

M1：首页抽屉 7 个点击回调 4 层透传收敛为单个 LauncherDrawerActions 数据类，全链路 MainApp→settingsLauncherScreen→LauncherRoute→LauncherScreen→LauncherSheet 各层 7 参数→1 参数。dismiss 副作用在 Route 内 wrap 包装（保留先 navi 再 dismiss）。功能等价：verifyRoborazziDebug 0 diff。
COMMIT
```

---

## 验收清单（全 Task 完成判据）

1. `:feature:settings:testDebugUnitTest` `BUILD SUCCESSFUL`
2. `:feature:settings:verifyRoborazziDebug` `BUILD SUCCESSFUL` 且 **0 diff**
3. `:app:compileOnlineDebugKotlin` `BUILD SUCCESSFUL`
4. `:feature:settings:spotlessCheck` + `:app:spotlessCheck` `BUILD SUCCESSFUL`
5. 单一 commit，可 `git revert` 回滚

## 备注

- **本机约束**：每个 Gradle 命令前查内存（CLAUDE.local.md，可用 <1000MB 中止）；`--no-daemon` 跑完即退避免 JVM 累积；后台跑只信 `grep -E '^BUILD (SUCCESSFUL|FAILED)'` 判定，不信 exit code。
- **节点 2 评审**：本重构含生产代码（Composable 签名/调用链）变更，git diff 跨 app + feature/settings，完成后走 `comprehensive-review:full-review`（改动规模 < ~50 行且不涉安全/接口对外/破坏性，可降级两维快审）。
