# Cashbook 设计改进方案

## 概述

对 Cashbook 项目进行 5 项架构与代码质量改进，分两批实施。

**第一批（低风险快速见效）**：
1. 间距 Design Token（CbSpacing）
2. BackHandler 替换为官方实现
3. contentDescription 无障碍补全

**第二批（较大改动）**：
4. 导航路由迁移 type-safe navigation
5. ProgressDialogManager CompositionLocal 注入重构

---

## 1. CbSpacing 间距 Token

### 背景

间距值（8.dp、16.dp 等）硬编码在各组件中，共 272 处，无集中定义。主题系统已有颜色、排版、背景 Token，唯独缺少间距。

### 设计

**新增文件**：`core/design/src/main/kotlin/cn/wj/android/cashbook/core/design/theme/Spacing.kt`

```kotlin
@Immutable
data class CbSpacing(
    val extraSmall: Dp = 4.dp,
    val small: Dp = 8.dp,
    val medium: Dp = 16.dp,
    val large: Dp = 24.dp,
    val extraLarge: Dp = 32.dp,
)

val LocalSpacing = staticCompositionLocalOf { CbSpacing() }
```

**集成**：在 `Theme.kt` 的 `CompositionLocalProvider` 中添加 `LocalSpacing provides CbSpacing()`。

**使用方式**：
```kotlin
val spacing = LocalSpacing.current
Modifier.padding(spacing.medium)  // 替代 Modifier.padding(16.dp)
```

### 替换范围

本轮仅替换 `core/design` 和 `core/ui` 中的硬编码间距值（约 50 处）。feature 模块留给后续逐步迁移。

### 值映射

| Token | 值 | 典型场景 |
|-------|-----|---------|
| extraSmall | 4.dp | 紧凑间距 |
| small | 8.dp | 列表项间距、图标与文字间距 |
| medium | 16.dp | 卡片内边距、区块间距 |
| large | 24.dp | 区块分隔 |
| extraLarge | 32.dp | 页面级间距 |

对于不在上述范围内的特殊值（如 2.dp、6.dp、56.dp、70.dp、200.dp），保持硬编码不变，这些属于组件特定尺寸而非通用间距。

---

## 2. BackHandler 替换为官方实现

### 背景

`core/ui` 中自定义了 `BackPressHandler`，使用 `OnBackPressedDispatcher` + `LocalBackPressedDispatcher` CompositionLocal。Compose 已提供官方 `androidx.activity.compose.BackHandler`，功能完全覆盖且自带 `enabled` 参数。

当前仅 2 处使用自定义实现，无隐藏依赖。

### 设计

**删除**：
- `core/ui/src/main/kotlin/cn/wj/android/cashbook/core/ui/BackHandler.kt`（整个文件）

**修改 `ActivityUiState.kt`**：
- `ProvideLocalState` 移除 `onBackPressedDispatcher: OnBackPressedDispatcher` 参数
- 移除 `LocalBackPressedDispatcher provides onBackPressedDispatcher`

**修改调用方**：
- `MainActivity.kt`：移除 `onBackPressedDispatcher = this.onBackPressedDispatcher`
- `MarkdownActivity.kt`：移除 `onBackPressedDispatcher = this.onBackPressedDispatcher`

**替换使用处**：

`LauncherScreen.kt`（原代码）：
```kotlin
if (drawerState.isOpen) {
    BackPressHandler {
        onRequestDismissDrawerSheet()
    }
}
```
替换为：
```kotlin
BackHandler(enabled = drawerState.isOpen) {
    onRequestDismissDrawerSheet()
}
```

`MyAssetScreen.kt`（原代码）：
```kotlin
if (showMoreDialog) {
    BackPressHandler {
        viewModel.dismissShowMoreDialog()
    }
}
```
替换为：
```kotlin
BackHandler(enabled = showMoreDialog) {
    viewModel.dismissShowMoreDialog()
}
```

**文档**：更新 `docs/ui-design-specification.md` 中 `LocalBackPressedDispatcher` 的引用。

### 影响分析

- 功能行为完全一致，官方 `BackHandler` 内部也使用 `OnBackPressedCallback`
- `enabled` 参数替代外层 `if` 判断，更简洁
- `ProvideLocalState` 签名变更，需同步修改 `MainActivity` 和 `MarkdownActivity` 调用处

---

## 3. contentDescription 无障碍补全

### 背景

79/82 处 contentDescription 为 null（96% 缺失），包括核心组件 TopAppBar、TextField、Calculator 中的可交互图标。

### 设计原则

- 可交互组件（IconButton 内的 Icon）必须提供 contentDescription
- 纯装饰性图标保持 `null`

### 核心组件修复

**`TopAppBar.kt`**：给 `CbTopAppBar` 新增可选参数：
```kotlin
@Composable
fun CbTopAppBar(
    // ...existing params
    navigationIconContentDescription: String? = null,
)
```
调用方按需传入，默认值 null 兼容现有调用。

**`TextField.kt`**：密码可见性切换按钮，根据状态提供描述：
- 密码隐藏时：stringResource(R.string.cd_show_password)
- 密码可见时：stringResource(R.string.cd_hide_password)

**`Calculator.kt`**：退格按钮添加 contentDescription。

### 字符串资源

在各模块的 `res/values/strings.xml` 中新增 `cd_` 前缀的无障碍字符串，命名规范参考已有的 `cd_search`、`cd_calendar`、`cd_analytics`。

常用字符串统一放在 `core/ui/src/main/res/values/strings_content_description.xml`（已有 `cd_search` 等定义的同一文件）：
- `cd_navigate_back` = "返回"
- `cd_more_options` = "更多选项"
- `cd_close` = "关闭"
- `cd_delete` = "删除"
- `cd_edit` = "编辑"
- `cd_show_password` = "显示密码"
- `cd_hide_password` = "隐藏密码"
- `cd_backspace` = "删除"

### 修复范围

26 个文件 79 处，逐一审查：可交互的补全，纯装饰的保留 null。

不添加自动化 Lint 规则，项目规模通过 code review 即可。

---

## 4. 导航路由迁移 type-safe navigation

### 背景

当前 6 个 feature 模块使用 `ROUTE_XXX` 字符串常量 + `navArgument` 手动解析参数 + `.replace("{key}", value)` 拼接导航。Navigation Compose 2.8.0+ 支持 `@Serializable` 路由类，项目当前版本 2.9.0。

### 设计

**Route 类定义**：各 feature 模块 `navigation/` 包内，替代 `ROUTE_` 常量。

无参数路由使用 `@Serializable object`，有参数路由使用 `@Serializable data class`。

**完整路由类清单**：

Settings 模块：
```kotlin
@Serializable object SettingsLauncherRoute
@Serializable object AboutUsRoute
@Serializable object SettingRoute
@Serializable object BackupAndRecoveryRoute
```

Records 模块：
```kotlin
@Serializable data class EditRecordRoute(val recordId: Long = -1L, val assetId: Long = -1L)
@Serializable object AnalyticsRoute
@Serializable data class TypedAnalyticsRoute(val tagId: Long = -1L, val typeId: Long = -1L, val date: String = "")
@Serializable object SelectRelatedRecordRoute
@Serializable object RecordCalendarRoute
@Serializable object RecordSearchRoute
```

Assets 模块：
```kotlin
@Serializable object MyAssetRoute
@Serializable object InvisibleAssetRoute
@Serializable data class AssetInfoRoute(val assetId: Long = -1L)
@Serializable data class EditAssetRoute(val assetId: Long = -1L)
```

Books 模块：
```kotlin
@Serializable object MyBooksRoute
@Serializable data class EditBookRoute(val bookId: Long = -1L)
```

Tags 模块：
```kotlin
@Serializable object MyTagsRoute
```

Types 模块：
```kotlin
@Serializable object MyCategoriesRoute
```

### 路由注册改造

```kotlin
// 前
composable(
    route = ROUTE_ASSET_INFO,
    arguments = listOf(navArgument(ROUTE_KEY_ASSET_ID) { type = NavType.LongType; defaultValue = -1L }),
) { val assetId = it.arguments?.getLong(ROUTE_KEY_ASSET_ID) ?: -1L; ... }

// 后
composable<AssetInfoRoute> { backStackEntry ->
    val route = backStackEntry.toRoute<AssetInfoRoute>()
    AssetInfoScreen(assetId = route.assetId, ...)
}
```

### 导航调用改造

```kotlin
// 前
fun NavController.naviToAssetInfo(assetId: Long) {
    navigate(ROUTE_ASSET_INFO.replace("{$ROUTE_KEY_ASSET_ID}", assetId.toString()))
}

// 后
fun NavController.naviToAssetInfo(assetId: Long) {
    navigate(AssetInfoRoute(assetId = assetId))
}
```

### NavHost startDestination

```kotlin
// 前
NavHost(navController = navController, startDestination = ROUTE_SETTINGS_LAUNCHER)
// 后
NavHost(navController = navController, startDestination = SettingsLauncherRoute)
```

### 依赖

确认 `kotlinx-serialization` 插件在各 feature 模块的 `build.gradle.kts` 中已启用。项目网络层已依赖此插件，但需检查 feature 模块是否也已应用。

### 保留不变

- `NavController` 扩展函数命名（`naviToXxx`）保持一致
- `LocalNavController` 和 `popBackStackSafety()` 不变
- composable 的 lambda content 参数模式不变

### 删除清理

- 所有 `ROUTE_` 字符串常量
- 所有 `ROUTE_KEY_` 参数键常量
- 所有 `navArgument()` 调用
- 所有 `.replace("{key}", value)` 拼接

---

## 5. ProgressDialogManager CompositionLocal 注入重构

### 背景

`ProgressDialogManager` 使用 `object` 单例 + `mutableStateOf` 管理全局进度弹窗状态。3 个 ViewModel 直接引用单例，存在并发风险和测试困难（测试需 Robolectric 提供 Compose 环境）。

### 设计

**抽取接口**：
```kotlin
interface ProgressDialogController {
    val dialogState: DialogState
    fun show(hint: String? = null, cancelable: Boolean = true, onDismiss: () -> Unit = {})
    fun dismiss()
}
```

**默认实现**：
```kotlin
class DefaultProgressDialogController : ProgressDialogController {
    override var dialogState: DialogState by mutableStateOf(DialogState.Dismiss)
        private set

    override fun dismiss() { dialogState = DialogState.Dismiss }

    override fun show(hint: String?, cancelable: Boolean, onDismiss: () -> Unit) {
        dialogState = DialogState.Shown(
            ProgressDialogState(hint = hint, cancelable = cancelable, onDismiss = onDismiss)
        )
    }
}
```

**CompositionLocal 注入**：
```kotlin
val LocalProgressDialogController = staticCompositionLocalOf<ProgressDialogController> {
    error("No ProgressDialogController provided")
}
```

在 `ActivityUiState.kt` 的 `ProvideLocalState` 中提供实例（内部 `remember { DefaultProgressDialogController() }`）。

### ViewModel 层改造

ViewModel 无法直接访问 CompositionLocal，`runCatchWithProgress` 改为接收 controller 参数：

```kotlin
suspend inline fun <R> runCatchWithProgress(
    controller: ProgressDialogController,
    hint: String? = null,
    cancelable: Boolean = true,
    noinline onDismiss: () -> Unit = {},
    minInterval: Long = 550L,
    timeout: Long = -1L,
    noinline block: suspend CoroutineScope.() -> R,
): Result<R>
```

Screen composable 中获取 controller 并传递给 ViewModel：
```kotlin
val controller = LocalProgressDialogController.current
// ViewModel 方法接收 controller 参数
viewModel.doBackup(controller)
```

### 受影响的 ViewModel

| ViewModel | 当前用法 | 改造方式 |
|-----------|---------|---------|
| BackupAndRecoveryViewModel | 直接调用 show()/dismiss() | 方法接收 controller 参数 |
| AnalyticsViewModel | show()/dismiss() + runCatchWithProgress | 同上 |
| ConfirmDeleteRecordDialogViewModel | runCatchWithProgress | 同上 |

### ProgressDialog composable 改造

```kotlin
@Composable
fun ProgressDialog() {
    val controller = LocalProgressDialogController.current
    ((controller.dialogState as? DialogState.Shown<*>)?.data as? ProgressDialogState)?.let { state ->
        // ...UI 不变
    }
}
```

### 删除

- `object ProgressDialogManager` 整体删除

### 测试改进

测试中可直接构造 mock 实现，不再依赖 Robolectric：
```kotlin
class FakeProgressDialogController : ProgressDialogController {
    override var dialogState: DialogState = DialogState.Dismiss
    override fun show(...) { /* no-op or record */ }
    override fun dismiss() { /* no-op or record */ }
}
```

---

## 实施顺序

### 第一批（无依赖关系，可并行）

1. **CbSpacing**：新增 Spacing.kt → 集成到 Theme → 替换 core 模块硬编码值
2. **BackHandler**：删除自定义实现 → 替换 2 处使用 → 清理 ProvideLocalState
3. **contentDescription**：新增字符串资源 → 修复核心组件 → 逐文件补全 feature 模块

### 第二批（有内部依赖，按序执行）

4. **导航路由**：定义 Route 类 → 改造路由注册 → 改造导航调用 → 改造 NavHost → 清理旧常量
5. **ProgressDialogManager**：抽取接口 → 实现 DefaultController → 添加 CompositionLocal → 改造 runCatchWithProgress → 改造 ViewModel → 改造 ProgressDialog → 删除单例
