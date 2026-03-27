# Cashbook 设计改进实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 对 Cashbook 项目进行 5 项设计改进：间距 Token、BackHandler 替换、contentDescription 补全、type-safe navigation 迁移、ProgressDialogManager 重构。

**Architecture:** 分两批实施。第一批（Task 1-3）无依赖可并行：CbSpacing Token 定义并替换 core 模块、BackHandler 替换为官方实现、contentDescription 无障碍补全。第二批（Task 4-5）按序执行：导航路由迁移 type-safe navigation、ProgressDialogManager CompositionLocal 注入。

**Tech Stack:** Kotlin, Jetpack Compose, Navigation Compose 2.9.0, kotlinx-serialization, Hilt, Material3

---

## 命名冲突说明

现有项目中 Screen composable 使用 `XxxRoute` 命名（如 `EditRecordRoute`、`AboutUsRoute`）。为避免与 `@Serializable` 路由类命名冲突，路由类统一**不使用 `Route` 后缀**：

| 路由类（新） | Screen Composable（已有） |
|-------------|--------------------------|
| `SettingsLauncher` | `LauncherRoute` |
| `AboutUs` | `AboutUsRoute` |
| `EditRecord` | `EditRecordRoute` |
| `AssetInfo` | `AssetInfoRoute` |

---

## 第一批：低风险快速见效（可并行）

### Task 1: CbSpacing 间距 Token

**Files:**
- Create: `core/design/src/main/kotlin/cn/wj/android/cashbook/core/design/theme/Spacing.kt`
- Modify: `core/design/src/main/kotlin/cn/wj/android/cashbook/core/design/theme/Theme.kt:228-232`

- [ ] **Step 1: 创建 Spacing.kt**

```kotlin
/*
 * Copyright 2021 The Cashbook Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.wj.android.cashbook.core.design.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 间距 Design Token
 *
 * > 通过 [LocalSpacing] 在 Compose 树中提供
 */
@Immutable
data class CbSpacing(
    /** 紧凑间距 */
    val extraSmall: Dp = 4.dp,
    /** 列表项间距、图标与文字间距 */
    val small: Dp = 8.dp,
    /** 卡片内边距、区块间距 */
    val medium: Dp = 16.dp,
    /** 区块分隔 */
    val large: Dp = 24.dp,
    /** 页面级间距 */
    val extraLarge: Dp = 32.dp,
)

val LocalSpacing = staticCompositionLocalOf { CbSpacing() }
```

- [ ] **Step 2: 集成到 Theme.kt**

在 `core/design/src/main/kotlin/cn/wj/android/cashbook/core/design/theme/Theme.kt` 的 `CompositionLocalProvider`（第228行）中添加 `LocalSpacing`：

```kotlin
// 第228-232行，替换为：
CompositionLocalProvider(
    LocalExtendedColors provides extendedColors,
    LocalGradientColors provides gradientColors,
    LocalBackgroundTheme provides backgroundTheme,
    LocalTintTheme provides tintTheme,
    LocalSpacing provides CbSpacing(),
) {
```

- [ ] **Step 3: 替换 core/design 组件中的硬编码间距**

逐文件审查 `core/design/src/main/kotlin/cn/wj/android/cashbook/core/design/component/` 下所有文件，将符合映射的硬编码间距值替换为 Token。替换规则：

- `4.dp` → `LocalSpacing.current.extraSmall`（仅用于 padding/spacing 场景）
- `8.dp` → `LocalSpacing.current.small`
- `16.dp` → `LocalSpacing.current.medium`
- `24.dp` → `LocalSpacing.current.large`
- `32.dp` → `LocalSpacing.current.extraLarge`

**注意**：
- 组件特定尺寸（如 `minHeight = 88.dp`、`size = 200.dp`）不替换
- 非 padding/spacing 用途（如 `cornerRadius`、`elevation`）不替换
- 通过 `val spacing = LocalSpacing.current` 在 Composable 顶部获取引用，避免重复调用

- [ ] **Step 4: 替换 core/ui 组件中的硬编码间距**

同样审查 `core/ui/src/main/kotlin/cn/wj/android/cashbook/core/ui/` 下的文件，按相同规则替换。

特别注意 `DialogState.kt` 中 `ProgressDialog()` 的间距（第166-176行）：
```kotlin
// 原：Modifier.padding(horizontal = 16.dp, vertical = 32.dp)
// 改为：Modifier.padding(horizontal = spacing.medium, vertical = spacing.extraLarge)
// 原：Modifier.padding(top = 32.dp)
// 改为：Modifier.padding(top = spacing.extraLarge)
```

- [ ] **Step 5: 运行格式检查**

```bash
./gradlew spotlessApply --init-script gradle/init.gradle.kts --no-configuration-cache
```

- [ ] **Step 6: 构建验证**

```bash
./gradlew :core:design:assembleDebug :core:ui:assembleDebug
```

- [ ] **Step 7: 提交**

```bash
git add core/design/src/main/kotlin/cn/wj/android/cashbook/core/design/theme/Spacing.kt
git add core/design/src/main/kotlin/cn/wj/android/cashbook/core/design/theme/Theme.kt
git add core/design/src/main/kotlin/cn/wj/android/cashbook/core/design/component/
git add core/ui/src/main/kotlin/cn/wj/android/cashbook/core/ui/
git commit -m "[refactor|design|间距Token][公共]新增CbSpacing间距Token，替换core模块硬编码间距值"
```

---

### Task 2: BackHandler 替换为官方实现

**Files:**
- Delete: `core/ui/src/main/kotlin/cn/wj/android/cashbook/core/ui/BackHandler.kt`
- Modify: `app/src/main/kotlin/cn/wj/android/cashbook/ui/ActivityUiState.kt:42-54`
- Modify: `app/src/main/kotlin/cn/wj/android/cashbook/ui/MainActivity.kt:105-110`
- Modify: `app/src/main/kotlin/cn/wj/android/cashbook/ui/MarkdownActivity.kt:90-102`
- Modify: `feature/settings/src/main/kotlin/cn/wj/android/cashbook/feature/settings/screen/LauncherScreen.kt:45,164-168`
- Modify: `feature/assets/src/main/kotlin/cn/wj/android/cashbook/feature/assets/screen/MyAssetScreen.kt:71,102-106`
- Modify: `docs/ui-design-specification.md:1559`

- [ ] **Step 1: 替换 LauncherScreen.kt 中的 BackPressHandler**

在 `feature/settings/src/main/kotlin/cn/wj/android/cashbook/feature/settings/screen/LauncherScreen.kt` 中：

1. 删除 import：`import cn.wj.android.cashbook.core.ui.BackPressHandler`（第45行）
2. 添加 import：`import androidx.activity.compose.BackHandler`
3. 替换第164-168行：

```kotlin
// 原：
if (drawerState.isOpen) {
    BackPressHandler {
        onRequestDismissDrawerSheet()
    }
}
// 改为：
BackHandler(enabled = drawerState.isOpen) {
    onRequestDismissDrawerSheet()
}
```

- [ ] **Step 2: 替换 MyAssetScreen.kt 中的 BackPressHandler**

在 `feature/assets/src/main/kotlin/cn/wj/android/cashbook/feature/assets/screen/MyAssetScreen.kt` 中：

1. 删除 import：`import cn.wj.android.cashbook.core.ui.BackPressHandler`（第71行）
2. 添加 import：`import androidx.activity.compose.BackHandler`
3. 替换第102-106行：

```kotlin
// 原：
if (showMoreDialog) {
    BackPressHandler {
        viewModel.dismissShowMoreDialog()
    }
}
// 改为：
BackHandler(enabled = showMoreDialog) {
    viewModel.dismissShowMoreDialog()
}
```

- [ ] **Step 3: 修改 ActivityUiState.kt 移除 LocalBackPressedDispatcher**

在 `app/src/main/kotlin/cn/wj/android/cashbook/ui/ActivityUiState.kt` 中：

1. 删除 import：`import androidx.activity.OnBackPressedDispatcher`（第19行）
2. 删除 import：`import cn.wj.android.cashbook.core.ui.LocalBackPressedDispatcher`（第29行）
3. 修改 `ProvideLocalState` 函数（第42-54行）：

```kotlin
@Composable
internal fun ProvideLocalState(
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalDefaultEmptyImagePainter provides painterResource(id = R.drawable.vector_no_data_200),
        LocalDefaultLoadingHint provides stringResource(id = R.string.data_in_loading),
        LocalProgressDialogHint provides stringResource(id = R.string.progress_loading_default),
        content = content,
    )
}
```

- [ ] **Step 4: 修改 MainActivity.kt 移除 onBackPressedDispatcher 参数**

在 `app/src/main/kotlin/cn/wj/android/cashbook/ui/MainActivity.kt` 中，将第105-108行：

```kotlin
// 原：
ProvideLocalState(
    onBackPressedDispatcher = this.onBackPressedDispatcher,
) {
// 改为：
ProvideLocalState {
```

- [ ] **Step 5: 修改 MarkdownActivity.kt 移除 onBackPressedDispatcher 参数**

在 `app/src/main/kotlin/cn/wj/android/cashbook/ui/MarkdownActivity.kt` 中，将第90-92行：

```kotlin
// 原：
ProvideLocalState(
    onBackPressedDispatcher = this.onBackPressedDispatcher,
) {
// 改为：
ProvideLocalState {
```

- [ ] **Step 6: 删除 BackHandler.kt**

```bash
git rm core/ui/src/main/kotlin/cn/wj/android/cashbook/core/ui/BackHandler.kt
```

- [ ] **Step 7: 更新 ui-design-specification.md**

在 `docs/ui-design-specification.md` 第1559行，将 `LocalBackPressedDispatcher` 相关行替换或删除，注明已迁移到官方 `BackHandler`。

- [ ] **Step 8: 运行格式检查 + 构建验证**

```bash
./gradlew spotlessApply --init-script gradle/init.gradle.kts --no-configuration-cache
./gradlew :app:assembleOnlineDebug :feature:settings:assembleDebug :feature:assets:assembleDebug
```

- [ ] **Step 9: 提交**

```bash
git add -u
git add docs/ui-design-specification.md
git commit -m "[refactor|UI|返回处理][公共]替换自定义BackPressHandler为官方BackHandler，清理LocalBackPressedDispatcher"
```

---

### Task 3: contentDescription 无障碍补全

**Files:**
- Modify: `core/ui/src/main/res/values/strings_content_description.xml`
- Modify: `core/design/src/main/kotlin/cn/wj/android/cashbook/core/design/component/TopAppBar.kt:49-74`
- Modify: `core/design/src/main/kotlin/cn/wj/android/cashbook/core/design/component/TextField.kt:180-186`
- Modify: `core/design/src/main/kotlin/cn/wj/android/cashbook/core/design/component/Calculator.kt:178`
- Modify: 26 个 feature 文件中的可交互图标（见下方清单）

- [ ] **Step 1: 新增无障碍字符串资源**

在 `core/ui/src/main/res/values/strings_content_description.xml` 中添加：

```xml
<?xml version="1.0" encoding="utf-8"?>
<!--
  ~ Copyright 2021 The Cashbook Open Source Project
  ~
  ~ Licensed under the Apache License, Version 2.0 (the "License");
  ~ you may not use this file except in compliance with the License.
  ~ You may obtain a copy of the License at
  ~
  ~     https://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing, software
  ~ distributed under the License is distributed on an "AS IS" BASIS,
  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~ See the License for the specific language governing permissions and
  ~ limitations under the License.
  -->

<resources>
    <string name="cd_search">搜索</string>
    <string name="cd_calendar">日历</string>
    <string name="cd_analytics">分析</string>
    <string name="cd_navigate_back">返回</string>
    <string name="cd_more_options">更多选项</string>
    <string name="cd_close">关闭</string>
    <string name="cd_delete">删除</string>
    <string name="cd_edit">编辑</string>
    <string name="cd_show_password">显示密码</string>
    <string name="cd_hide_password">隐藏密码</string>
    <string name="cd_backspace">退格</string>
    <string name="cd_add">添加</string>
    <string name="cd_confirm">确认</string>
    <string name="cd_expand">展开</string>
    <string name="cd_collapse">收起</string>
    <string name="cd_sort">排序</string>
    <string name="cd_fingerprint">指纹识别</string>
    <string name="cd_menu">菜单</string>
    <string name="cd_settings">设置</string>
    <string name="cd_share">分享</string>
    <string name="cd_copy">复制</string>
    <string name="cd_visibility_toggle">切换可见性</string>
    <string name="cd_select_date">选择日期</string>
    <string name="cd_previous">上一个</string>
    <string name="cd_next">下一个</string>
    <string name="cd_clear">清除</string>
    <string name="cd_drag_handle">拖动排序</string>
</resources>
```

- [ ] **Step 2: 修复 TopAppBar.kt — 返回按钮**

在 `core/design/src/main/kotlin/cn/wj/android/cashbook/core/design/component/TopAppBar.kt` 中：

1. 添加 import：
```kotlin
import androidx.compose.ui.res.stringResource
import cn.wj.android.cashbook.core.ui.R as UiR
```

2. 修改带 `onBackClick` 的 `CbTopAppBar` 重载（第49-74行），添加 `navigationIconContentDescription` 参数：

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CbTopAppBar(
    title: @Composable () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    navigationIconContentDescription: String? = stringResource(id = UiR.string.cd_navigate_back),
    actions: @Composable RowScope.() -> Unit = {},
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
    colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    CbTopAppBar(
        title = title,
        modifier = modifier,
        navigationIcon = {
            CbIconButton(onClick = onBackClick) {
                Icon(
                    imageVector = CbIcons.ArrowBack,
                    contentDescription = navigationIconContentDescription,
                )
            }
        },
        actions = actions,
        windowInsets = windowInsets,
        colors = colors,
        scrollBehavior = scrollBehavior,
    )
}
```

- [ ] **Step 3: 修复 TextField.kt — 密码可见性切换**

在 `core/design/src/main/kotlin/cn/wj/android/cashbook/core/design/component/TextField.kt` 中：

1. 添加 import：
```kotlin
import androidx.compose.ui.res.stringResource
import cn.wj.android.cashbook.core.ui.R as UiR
```

2. 修改第182-185行：

```kotlin
// 原：
Icon(
    imageVector = if (visible) CbIcons.VisibilityOff else CbIcons.Visibility,
    contentDescription = null,
)
// 改为：
Icon(
    imageVector = if (visible) CbIcons.VisibilityOff else CbIcons.Visibility,
    contentDescription = stringResource(
        id = if (visible) UiR.string.cd_hide_password else UiR.string.cd_show_password,
    ),
)
```

- [ ] **Step 4: 修复 Calculator.kt — 退格按钮**

在 `core/design/src/main/kotlin/cn/wj/android/cashbook/core/design/component/Calculator.kt` 中：

1. 添加 import：
```kotlin
import androidx.compose.ui.res.stringResource
import cn.wj.android.cashbook.core.ui.R as UiR
```

2. 在退格按钮的 Icon（第178行）添加 contentDescription：

```kotlin
// 原：contentDescription = null
// 改为：contentDescription = stringResource(id = UiR.string.cd_backspace)
```

- [ ] **Step 5: 逐文件补全 feature 模块的 contentDescription**

逐一审查以下 26 个文件中的 `contentDescription = null`，对可交互组件（IconButton 内的 Icon）补全描述，纯装饰图标保持 null：

**审查清单**（每个文件需逐一读取、判断交互性、修改）：

core 模块：
- `core/design/component/Empty.kt` — 纯装饰图片，保持 null
- `core/ui/component/TypeIcon.kt` — 类型图标，一般是装饰性，保持 null

app 模块：
- `app/ui/MainApp.kt:708` — 指纹识别按钮，改为 `cd_fingerprint`

feature/assets（5 个文件）：
- `AssetListItem.kt` — 审查 3 处
- `AssetInfoScreen.kt` — 审查 6 处
- `EditAssetScreen.kt` — 审查 6 处
- `MyAssetScreen.kt` — 审查 5 处

feature/books（2 个文件）：
- `EditBookScreen.kt` — 审查 3 处
- `MyBooksScreen.kt` — 审查 4 处

feature/records（7 个文件）：
- `ImagePreviewDialog.kt` — 审查 2 处
- `AnalyticsScreen.kt` — 审查 3 处
- `CalendarScreen.kt` — 审查 1 处
- `EditRecordScreen.kt` — 审查 5 处
- `LauncherContentScreen.kt` — 审查 4 处
- `SearchScreen.kt` — 审查 2 处
- `SelectRelatedRecordScreen.kt` — 审查 1 处
- `RecordDetailsSheet.kt` — 审查 2 处

feature/settings（4 个文件）：
- `AboutUsScreen.kt` — 审查 3 处
- `BackupAndRecoveryScreen.kt` — 审查 1 处
- `LauncherScreen.kt` — 审查 6 处
- `SettingScreen.kt` — 审查 5 处

feature/tags（1 个文件）：
- `MyTagsScreen.kt` — 审查 1 处

feature/types（1 个文件）：
- `MyCategoriesScreen.kt` — 审查 8 处

**判断原则**：
- IconButton 包裹的 Icon → 必须添加描述
- 独立的 Icon（无 onClick）→ 纯装饰，保持 null
- 如果图标含义可从上下文推断（如列表项行尾的箭头），可保持 null

- [ ] **Step 6: 运行格式检查 + 构建验证**

```bash
./gradlew spotlessApply --init-script gradle/init.gradle.kts --no-configuration-cache
./gradlew :app:assembleOnlineDebug
```

- [ ] **Step 7: 提交**

```bash
git add core/ui/src/main/res/values/strings_content_description.xml
git add core/design/src/main/kotlin/cn/wj/android/cashbook/core/design/component/
git add feature/ app/
git commit -m "[fix|UI|无障碍][公共]补全可交互图标的contentDescription，新增无障碍字符串资源"
```

---

## 第二批：较大改动（按序执行）

### Task 4: 导航路由迁移 type-safe navigation

**Files:**
- Modify: `build-logic/convention/src/main/kotlin/AndroidFeatureConventionPlugin.kt:31-34`
- Modify: `feature/settings/src/main/kotlin/.../navigation/SettingsNavigation.kt`（全文重写）
- Modify: `feature/records/src/main/kotlin/.../navigation/RecordNavigation.kt`（全文重写）
- Modify: `feature/assets/src/main/kotlin/.../navigation/AssetNavigation.kt`（全文重写）
- Modify: `feature/books/src/main/kotlin/.../navigation/BooksNavigation.kt`（全文重写）
- Modify: `feature/tags/src/main/kotlin/.../navigation/TagNavigation.kt`（全文重写）
- Modify: `feature/types/src/main/kotlin/.../navigation/TypesNavigation.kt`（全文重写）
- Modify: `app/src/main/kotlin/cn/wj/android/cashbook/ui/MainApp.kt`（startDestination 和 import）

- [ ] **Step 1: 添加 serialization 插件到 Feature Convention Plugin**

在 `build-logic/convention/src/main/kotlin/AndroidFeatureConventionPlugin.kt` 中：

1. 在 `pluginManager.apply` 块（第31-34行）添加 serialization 插件：

```kotlin
pluginManager.apply {
    apply(ProjectSetting.Plugin.PLUGIN_CASHBOOK_LIBRARY)
    apply(ProjectSetting.Plugin.PLUGIN_CASHBOOK_HILT)
    apply("org.jetbrains.kotlin.plugin.serialization")
}
```

2. 在 `dependencies` 块（第43-52行）添加 serialization 依赖：

```kotlin
dependencies {
    "implementation"(project(":core:design"))
    "implementation"(project(":core:ui"))

    "implementation"(libs.findLibrary("androidx-hilt-navigation-compose").get())
    "implementation"(libs.findLibrary("androidx-lifecycle-runtime-compose").get())
    "implementation"(libs.findLibrary("androidx-lifecycle-viewmodel-compose").get())
    "implementation"(libs.findLibrary("kotlinx-serialization-json").get())

    "androidTestImplementation"(libs.findLibrary("androidx-lifecycle-runtime-testing").get())
}
```

- [ ] **Step 2: 构建验证 — 确认插件生效**

```bash
./gradlew :feature:settings:assembleDebug
```

- [ ] **Step 3: 迁移 SettingsNavigation.kt**

将 `feature/settings/src/main/kotlin/cn/wj/android/cashbook/feature/settings/navigation/SettingsNavigation.kt` 替换为：

```kotlin
/*
 * Copyright 2021 The Cashbook Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * ...（保留完整 license header）
 */

package cn.wj.android.cashbook.feature.settings.navigation

import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import cn.wj.android.cashbook.feature.settings.screen.AboutUsRoute
import cn.wj.android.cashbook.feature.settings.screen.BackupAndRecoveryRoute
import cn.wj.android.cashbook.feature.settings.screen.LauncherRoute
import cn.wj.android.cashbook.feature.settings.screen.SettingRoute
import kotlinx.serialization.Serializable

/** 设置 - 启动页 */
@Serializable
object SettingsLauncher

/** 设置 - 关于我们 */
@Serializable
object AboutUs

/** 设置 - 设置 */
@Serializable
object Setting

/** 设置 - 备份与恢复 */
@Serializable
object BackupAndRecovery

/** 跳转到关于我们 */
fun NavController.naviToAboutUs() {
    this.navigate(AboutUs)
}

/** 跳转到设置 */
fun NavController.naviToSetting() {
    this.navigate(Setting)
}

/** 跳转备份恢复界面 */
fun NavController.naviToBackupAndRecovery() {
    this.navigate(BackupAndRecovery)
}

fun NavGraphBuilder.settingsLauncherScreen(
    onRequestNaviToMyAsset: () -> Unit,
    onRequestNaviToMyBooks: () -> Unit,
    onRequestNaviToMyCategory: () -> Unit,
    onRequestNaviToMyTags: () -> Unit,
    onRequestNaviToSetting: () -> Unit,
    onRequestNaviToAboutUs: () -> Unit,
    content: @Composable (() -> Unit) -> Unit,
) {
    composable<SettingsLauncher> {
        LauncherRoute(
            onRequestNaviToMyAsset = onRequestNaviToMyAsset,
            onRequestNaviToMyBooks = onRequestNaviToMyBooks,
            onRequestNaviToMyCategory = onRequestNaviToMyCategory,
            onRequestNaviToMyTags = onRequestNaviToMyTags,
            onRequestNaviToSetting = onRequestNaviToSetting,
            onRequestNaviToAboutUs = onRequestNaviToAboutUs,
            content = content,
        )
    }
}

fun NavGraphBuilder.aboutUsScreen(
    onRequestNaviToChangelog: () -> Unit,
    onRequestNaviToPrivacyPolicy: () -> Unit,
    onRequestPopBackStack: () -> Unit,
) {
    composable<AboutUs> {
        AboutUsRoute(
            onRequestNaviToChangelog = onRequestNaviToChangelog,
            onRequestNaviToPrivacyPolicy = onRequestNaviToPrivacyPolicy,
            onRequestPopBackStack = onRequestPopBackStack,
        )
    }
}

fun NavGraphBuilder.settingScreen(
    onRequestNaviToBackupAndRecovery: () -> Unit,
    onRequestPopBackStack: () -> Unit,
    onShowSnackbar: suspend (String, String?) -> SnackbarResult,
) {
    composable<Setting> {
        SettingRoute(
            onRequestPopBackStack = onRequestPopBackStack,
            onRequestNaviToBackupAndRecovery = onRequestNaviToBackupAndRecovery,
            onShowSnackbar = onShowSnackbar,
        )
    }
}

fun NavGraphBuilder.backupAndRecoveryScreen(
    onRequestPopBackStack: () -> Unit,
    onShowSnackbar: suspend (String, String?) -> SnackbarResult,
) {
    composable<BackupAndRecovery> {
        BackupAndRecoveryRoute(
            onRequestPopBackStack = onRequestPopBackStack,
            onShowSnackbar = onShowSnackbar,
        )
    }
}
```

- [ ] **Step 4: 迁移 RecordNavigation.kt**

将 `feature/records/src/main/kotlin/cn/wj/android/cashbook/feature/records/navigation/RecordNavigation.kt` 重写。关键变更：

```kotlin
// 路由类定义（替换所有 ROUTE_ 常量）
@Serializable
data class EditRecord(val recordId: Long = -1L, val assetId: Long = -1L)

@Serializable
object Analytics

@Serializable
data class TypedAnalytics(val tagId: Long = -1L, val typeId: Long = -1L, val date: String = "")

@Serializable
object SelectRelatedRecord

@Serializable
object RecordCalendar

@Serializable
object RecordSearch
```

导航函数改造示例：
```kotlin
fun NavController.naviToEditRecord(recordId: Long = -1L, assetId: Long = -1L) {
    this.navigate(EditRecord(recordId = recordId, assetId = assetId))
}

fun NavController.naviToTypedAnalytics(
    tagId: Long = -1L,
    typeId: Long = -1L,
    date: String? = null,
) {
    this.navigate(TypedAnalytics(tagId = tagId, typeId = typeId, date = date.orEmpty()))
}
```

NavGraphBuilder 扩展改造示例：
```kotlin
fun NavGraphBuilder.editRecordScreen(
    typeListContent: @Composable (RecordTypeCategoryEnum, Long, (Long) -> Unit) -> Unit,
    assetBottomSheetContent: @Composable (Long, Long, Boolean, (Long) -> Unit) -> Unit,
    tagBottomSheetContent: @Composable (List<Long>, (List<Long>) -> Unit, () -> Unit) -> Unit,
    onRequestNaviToSelectRelatedRecord: () -> Unit,
    onRequestPopBackStack: () -> Unit,
) {
    composable<EditRecord> { backStackEntry ->
        val route = backStackEntry.toRoute<EditRecord>()
        EditRecordRoute(
            recordId = route.recordId,
            assetId = route.assetId,
            typeListContent = typeListContent,
            assetBottomSheetContent = assetBottomSheetContent,
            tagBottomSheetContent = tagBottomSheetContent,
            onRequestNaviToSelectRelatedRecord = onRequestNaviToSelectRelatedRecord,
            onRequestPopBackStack = onRequestPopBackStack,
        )
    }
}

fun NavGraphBuilder.typedAnalyticsScreen(
    onRequestNaviToEditRecord: (Long) -> Unit,
    onRequestNaviToAssetInfo: (Long) -> Unit,
    onRequestPopBackStack: () -> Unit,
) {
    composable<TypedAnalytics> { backStackEntry ->
        val route = backStackEntry.toRoute<TypedAnalytics>()
        TypedAnalyticsRoute(
            typeId = route.typeId,
            tagId = route.tagId,
            date = route.date,
            onRequestNaviToEditRecord = onRequestNaviToEditRecord,
            onRequestNaviToAssetInfo = onRequestNaviToAssetInfo,
            onRequestPopBackStack = onRequestPopBackStack,
        )
    }
}
```

无参数路由的 composable 不需要 `toRoute` 调用：
```kotlin
fun NavGraphBuilder.analyticsScreen(...) {
    composable<Analytics> {
        AnalyticsRoute(...)
    }
}
```

保留文件底部的 `LauncherContent()`、`AssetInfoContent()`、`RecordDetailSheetContent()` 不变——它们不是导航路由，是普通 Composable 函数。

删除所有 `navArgument` import 和 `NavType` import。添加 `import kotlinx.serialization.Serializable` 和 `import androidx.navigation.toRoute`。

- [ ] **Step 5: 迁移 AssetNavigation.kt**

路由类定义：
```kotlin
@Serializable object MyAsset
@Serializable object InvisibleAsset
@Serializable data class AssetInfo(val assetId: Long = -1L)
@Serializable data class EditAsset(val assetId: Long = -1L)
```

导航函数：
```kotlin
fun NavController.naviToMyAsset() { this.navigate(MyAsset) }
fun NavController.naviToInvisibleAsset() { this.navigate(InvisibleAsset) }
fun NavController.naviToAssetInfo(assetId: Long) { this.navigate(AssetInfo(assetId = assetId)) }
fun NavController.naviToEditAsset(assetId: Long = -1L) { this.navigate(EditAsset(assetId = assetId)) }
```

NavGraphBuilder 扩展中的 `assetInfoScreen` 注意保留 `assetId` 的使用：
```kotlin
fun NavGraphBuilder.assetInfoScreen(...) {
    composable<AssetInfo> { backStackEntry ->
        val route = backStackEntry.toRoute<AssetInfo>()
        val assetId = route.assetId
        AssetInfoRoute(
            assetId = assetId,
            assetRecordListContent = { topContent, onRecordItemClick ->
                assetRecordListContent(assetId, topContent, onRecordItemClick)
            },
            recordDetailSheetContent = recordDetailSheetContent,
            onRequestNaviToEditAsset = { onRequestNaviToEditAsset(assetId) },
            onRequestNaviToAddRecord = { onRequestNaviToAddRecord(assetId) },
            onRequestPopBackStack = onRequestPopBackStack,
        )
    }
}
```

保留 `EditRecordSelectAssetBottomSheetContent()` 不变。

- [ ] **Step 6: 迁移 BooksNavigation.kt**

```kotlin
@Serializable object MyBooks
@Serializable data class EditBook(val bookId: Long = -1L)

fun NavController.naviToMyBooks() { this.navigate(MyBooks) }
fun NavController.naviToEditBook(bookId: Long = -1L) { this.navigate(EditBook(bookId = bookId)) }

fun NavGraphBuilder.myBooksScreen(...) {
    composable<MyBooks> { MyBooksRoute(...) }
}

fun NavGraphBuilder.editBookScreen(...) {
    composable<EditBook> { backStackEntry ->
        val route = backStackEntry.toRoute<EditBook>()
        EditBookRoute(bookId = route.bookId, onRequestPopBackStack = onRequestPopBackStack)
    }
}
```

- [ ] **Step 7: 迁移 TagNavigation.kt**

```kotlin
@Serializable object MyTags

fun NavController.naviToMyTags() { this.navigate(MyTags) }

fun NavGraphBuilder.myTagsScreen(...) {
    composable<MyTags> { MyTagsRoute(...) }
}
```

保留 `EditRecordSelectTagBottomSheetContent()` 不变。

- [ ] **Step 8: 迁移 TypesNavigation.kt**

```kotlin
@Serializable object MyCategories

fun NavController.naviToMyCategories() { this.navigate(MyCategories) }

fun NavGraphBuilder.myCategoriesScreen(...) {
    composable<MyCategories> { MyCategoriesRoute(...) }
}
```

保留 `EditRecordTypeListContent()` 不变。

- [ ] **Step 9: 修改 MainApp.kt — startDestination**

在 `app/src/main/kotlin/cn/wj/android/cashbook/ui/MainApp.kt` 中：

1. 删除旧 import：`import cn.wj.android.cashbook.feature.settings.navigation.ROUTE_SETTINGS_LAUNCHER`（如有）
2. 添加新 import：`import cn.wj.android.cashbook.feature.settings.navigation.SettingsLauncher`
3. 修改 `CashbookNavHost` 中的 `NavHost` 调用：

```kotlin
// 原：startDestination = ROUTE_SETTINGS_LAUNCHER
// 改为：startDestination = SettingsLauncher
```

4. 同步检查 MainApp.kt 中是否有其他对 `ROUTE_EDIT_RECORD`、`ROUTE_MY_ASSET` 等公开常量的引用，统一替换为对应路由类。

- [ ] **Step 10: 运行格式检查 + 全量构建验证**

```bash
./gradlew spotlessApply --init-script gradle/init.gradle.kts --no-configuration-cache
./gradlew :app:assembleOnlineDebug
```

- [ ] **Step 11: 提交**

```bash
git add build-logic/ feature/ app/
git commit -m "[refactor|all|导航][公共]迁移导航路由到type-safe navigation，使用@Serializable路由类替代字符串路由"
```

---

### Task 5: ProgressDialogManager CompositionLocal 注入重构

**Files:**
- Modify: `core/ui/src/main/kotlin/cn/wj/android/cashbook/core/ui/DialogState.kt`（主要重构）
- Modify: `app/src/main/kotlin/cn/wj/android/cashbook/ui/ActivityUiState.kt`
- Modify: `feature/settings/src/main/kotlin/.../viewmodel/BackupAndRecoveryViewModel.kt`
- Modify: `feature/settings/src/main/kotlin/.../screen/BackupAndRecoveryScreen.kt`
- Modify: `feature/records/src/main/kotlin/.../viewmodel/AnalyticsViewModel.kt`
- Modify: `feature/records/src/main/kotlin/.../screen/AnalyticsScreen.kt`
- Modify: `feature/records/src/main/kotlin/.../viewmodel/ConfirmDeleteRecordDialogViewModel.kt`
- Modify: `feature/records/src/main/kotlin/.../dialog/ConfirmDeleteRecordDialog.kt`（或对应 Screen）
- Modify: `feature/assets/src/main/kotlin/.../viewmodel/AssetInfoViewModel.kt`（如有使用）
- Modify: `feature/records/src/test/kotlin/.../viewmodel/ConfirmDeleteRecordDialogViewModelTest.kt`

- [ ] **Step 1: 在 DialogState.kt 中抽取接口和默认实现**

重构 `core/ui/src/main/kotlin/cn/wj/android/cashbook/core/ui/DialogState.kt`：

在 `ProgressDialogState` 之后（约第65行后），替换 `object ProgressDialogManager` 为：

```kotlin
/**
 * 进度弹窗控制器接口
 */
interface ProgressDialogController {
    /** 弹窗状态 */
    val dialogState: DialogState

    /** 隐藏弹窗 */
    fun dismiss()

    /**
     * 显示弹窗
     *
     * @param hint 提示文本
     * @param cancelable 是否可取消
     * @param onDismiss 隐藏回调
     */
    fun show(
        hint: String? = null,
        cancelable: Boolean = true,
        onDismiss: () -> Unit = {},
    )
}

/**
 * 进度弹窗控制器默认实现
 */
class DefaultProgressDialogController : ProgressDialogController {

    override var dialogState: DialogState by mutableStateOf(DialogState.Dismiss)
        private set

    override fun dismiss() {
        dialogState = DialogState.Dismiss
    }

    override fun show(
        hint: String?,
        cancelable: Boolean,
        onDismiss: () -> Unit,
    ) {
        dialogState = DialogState.Shown(
            ProgressDialogState(
                hint = hint,
                cancelable = cancelable,
                onDismiss = onDismiss,
            ),
        )
    }
}

/**
 * 通过 CompositionLocal 提供进度弹窗控制器
 */
val LocalProgressDialogController = staticCompositionLocalOf<ProgressDialogController> {
    error("No ProgressDialogController provided")
}
```

- [ ] **Step 2: 修改 runCatchWithProgress 接收 controller 参数**

在同文件中修改 `runCatchWithProgress`（约第106-131行）：

```kotlin
/** 显示提示文本为[hint]，能否取消[cancelable]的进度弹窗，并执行[block]逻辑，最低显示[minInterval]ms，最高显示[timeout]ms，逻辑执行完成、异常或超时返回结果，并执行回调[onDismiss] */
suspend inline fun <R> runCatchWithProgress(
    controller: ProgressDialogController,
    hint: String? = null,
    cancelable: Boolean = true,
    noinline onDismiss: () -> Unit = {},
    minInterval: Long = 550L,
    timeout: Long = -1L,
    noinline block: suspend CoroutineScope.() -> R,
): Result<R> {
    val result: Result<R>
    val ms = measureTimeMillis {
        controller.show(hint, cancelable, onDismiss)
        result = runCatching {
            val timeMillis = if (timeout < 0L) {
                Long.MAX_VALUE
            } else {
                timeout
            }
            withTimeout(timeMillis, block)
        }
    }
    if (ms < minInterval) {
        delay(minInterval - ms)
    }
    controller.dismiss()
    return result
}
```

- [ ] **Step 3: 修改 ProgressDialog composable**

在同文件中修改 `ProgressDialog()`（约第151-183行）：

```kotlin
@Composable
fun ProgressDialog() {
    val controller = LocalProgressDialogController.current
    ((controller.dialogState as? DialogState.Shown<*>)?.data as? ProgressDialogState)?.let { state ->
        Dialog(
            onDismissRequest = {
                controller.dismiss()
                state.onDismiss()
            },
            properties = DialogProperties(
                dismissOnBackPress = state.cancelable,
                dismissOnClickOutside = state.cancelable,
            ),
            content = {
                CbCard {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        LinearProgressIndicator()
                        Text(
                            text = if (state.hint.isNullOrBlank()) {
                                LocalProgressDialogHint.current
                            } else {
                                state.hint
                            },
                            modifier = Modifier.padding(top = 32.dp),
                        )
                    }
                }
            },
        )
    }
}
```

- [ ] **Step 4: 删除 object ProgressDialogManager**

确认所有引用已迁移后，删除 `object ProgressDialogManager { ... }` 代码块（原第72-103行）。

- [ ] **Step 5: 在 ActivityUiState.kt 中提供 controller 实例**

在 `app/src/main/kotlin/cn/wj/android/cashbook/ui/ActivityUiState.kt` 的 `ProvideLocalState` 中：

1. 添加 import：
```kotlin
import androidx.compose.runtime.remember
import cn.wj.android.cashbook.core.ui.DefaultProgressDialogController
import cn.wj.android.cashbook.core.ui.LocalProgressDialogController
```

2. 修改函数：

```kotlin
@Composable
internal fun ProvideLocalState(
    content: @Composable () -> Unit,
) {
    val progressDialogController = remember { DefaultProgressDialogController() }
    CompositionLocalProvider(
        LocalDefaultEmptyImagePainter provides painterResource(id = R.drawable.vector_no_data_200),
        LocalDefaultLoadingHint provides stringResource(id = R.string.data_in_loading),
        LocalProgressDialogHint provides stringResource(id = R.string.progress_loading_default),
        LocalProgressDialogController provides progressDialogController,
        content = content,
    )
}
```

- [ ] **Step 6: 改造 BackupAndRecoveryViewModel**

读取 `feature/settings/src/main/kotlin/cn/wj/android/cashbook/feature/settings/viewmodel/BackupAndRecoveryViewModel.kt`，找到所有 `ProgressDialogManager.show()` 和 `ProgressDialogManager.dismiss()` 调用。

将直接引用 `ProgressDialogManager` 的方法改为接收 `ProgressDialogController` 参数。例如：

```kotlin
// 原（在 ViewModel 内部直接调用）：
ProgressDialogManager.show(cancelable = false)
// ...执行逻辑...
ProgressDialogManager.dismiss()

// 改为（方法接收 controller 参数）：
fun backup(controller: ProgressDialogController) {
    viewModelScope.launch {
        controller.show(cancelable = false)
        // ...执行逻辑...
        controller.dismiss()
    }
}
```

或更好地使用 `runCatchWithProgress`：
```kotlin
fun backup(controller: ProgressDialogController) {
    viewModelScope.launch {
        runCatchWithProgress(controller, cancelable = false) {
            // ...执行逻辑...
        }
    }
}
```

删除 `import cn.wj.android.cashbook.core.ui.ProgressDialogManager`，添加 `import cn.wj.android.cashbook.core.ui.ProgressDialogController`。

- [ ] **Step 7: 改造对应 Screen composable 传递 controller**

在 `BackupAndRecoveryScreen.kt`（或 `BackupAndRecoveryRoute`）中：

```kotlin
val controller = LocalProgressDialogController.current
// 传递给 ViewModel 方法
viewModel.backup(controller)
```

- [ ] **Step 8: 改造 AnalyticsViewModel**

同 Step 6 模式。找到所有 `ProgressDialogManager.show()`、`ProgressDialogManager.dismiss()` 和 `runCatchWithProgress` 调用，改为接收 `controller` 参数。

- [ ] **Step 9: 改造 AnalyticsScreen composable**

同 Step 7 模式，通过 `LocalProgressDialogController.current` 获取 controller 并传递。

- [ ] **Step 10: 改造 ConfirmDeleteRecordDialogViewModel**

```kotlin
// 原：
fun onDeleteRecordConfirm(recordId: Long, hintText: String, onResult: (ResultModel) -> Unit) {
    viewModelScope.launch {
        runCatchWithProgress(hint = hintText, cancelable = false) {
            deleteRecordUseCase(recordId)
            onResult.invoke(ResultModel.success())
        }
    }
}

// 改为：
fun onDeleteRecordConfirm(
    controller: ProgressDialogController,
    recordId: Long,
    hintText: String,
    onResult: (ResultModel) -> Unit,
) {
    viewModelScope.launch {
        runCatchWithProgress(controller, hint = hintText, cancelable = false) {
            deleteRecordUseCase(recordId)
            onResult.invoke(ResultModel.success())
        }
    }
}
```

- [ ] **Step 11: 改造 ConfirmDeleteRecordDialog 对应 UI**

通过 `LocalProgressDialogController.current` 获取 controller 并传递给 ViewModel 方法。

- [ ] **Step 12: 检查 AssetInfoViewModel**

读取 `feature/assets/src/main/kotlin/.../viewmodel/AssetInfoViewModel.kt`，确认其是否实际使用了 `ProgressDialogManager` 或 `runCatchWithProgress`（之前探索发现有 import 但可能未实际调用）。如有使用，同样改造。

- [ ] **Step 13: 更新测试**

在 `feature/records/src/test/kotlin/.../viewmodel/ConfirmDeleteRecordDialogViewModelTest.kt` 中：

创建 `FakeProgressDialogController`：
```kotlin
class FakeProgressDialogController : ProgressDialogController {
    override var dialogState: DialogState = DialogState.Dismiss
    var showCallCount = 0
    var dismissCallCount = 0

    override fun show(hint: String?, cancelable: Boolean, onDismiss: () -> Unit) {
        showCallCount++
        dialogState = DialogState.Shown(ProgressDialogState(hint, cancelable, onDismiss))
    }

    override fun dismiss() {
        dismissCallCount++
        dialogState = DialogState.Dismiss
    }
}
```

更新测试调用：将 `viewModel.onDeleteRecordConfirm(recordId, hintText, onResult)` 改为 `viewModel.onDeleteRecordConfirm(fakeController, recordId, hintText, onResult)`。

测试不再需要 Robolectric 提供 Compose `mutableStateOf` 环境（如果 `FakeProgressDialogController` 使用普通属性）。

- [ ] **Step 14: 运行格式检查 + 全量构建 + 测试**

```bash
./gradlew spotlessApply --init-script gradle/init.gradle.kts --no-configuration-cache
./gradlew :app:assembleOnlineDebug
./gradlew testOnlineDebugUnitTest
```

- [ ] **Step 15: 提交**

```bash
git add core/ui/ app/ feature/
git commit -m "[refactor|UI|进度弹窗][公共]重构ProgressDialogManager为CompositionLocal注入，提升可测试性"
```

---

## 验证清单

每个 Task 完成后，运行以下验证：

```bash
# 格式检查
./gradlew spotlessApply --init-script gradle/init.gradle.kts --no-configuration-cache

# 全量构建（至少 OnlineDebug）
./gradlew :app:assembleOnlineDebug

# 单元测试
./gradlew testOnlineDebugUnitTest
```

全部 5 个 Task 完成后的最终验证：

```bash
# 全 flavor 构建
./gradlew :app:assemble

# Lint 检查
./gradlew :app:lintOnlineRelease :app:lintOfflineRelease :app:lintDevRelease :lint:lint -Dlint.baselines.continue=true
```
