# UI 截图测试补充 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 Cashbook 项目建立全面的 UI 截图测试（Roborazzi），覆盖 core/design 组件、core/ui 组件和全部 feature Screen。

**Architecture:** 分三层推进：(1) 基础设施配置（Roborazzi 插件 + 可见性修改），(2) 组件级截图测试（core/design + core/ui），(3) 页面级截图测试（feature/*）。所有测试类遵循现有 `BackgroundScreenshotTests` 模板，组件测试使用 `captureMultiTheme()`，Screen 测试额外使用 `captureMultiDevice()`。

**Tech Stack:** Roborazzi v1.46.1 + Robolectric + Compose Testing + Accompanist TestHarness

**Spec:** `docs/superpowers/specs/2026-03-22-ui-screenshot-tests-design.md`

---

## 文件结构总览

### 基础设施（修改）
- `feature/records/build.gradle.kts` — 添加 Roborazzi 插件
- `feature/settings/build.gradle.kts` — 同上
- `feature/assets/build.gradle.kts` — 同上
- `feature/tags/build.gradle.kts` — 同上
- `feature/books/build.gradle.kts` — 同上
- `feature/types/build.gradle.kts` — 同上
- `core/ui/build.gradle.kts` — 同上
- `feature/records/.../screen/AnalyticsScreen.kt` — private→internal
- `feature/records/.../screen/SearchScreen.kt` — private→internal
- `feature/records/.../screen/TypedAnalyticsScreen.kt` — private→internal
- `feature/assets/.../screen/InvisibleAssetScreen.kt` — private→internal

### Core Design 组件测试（新建，22 个文件）
目录：`core/design/src/test/kotlin/cn/wj/android/cashbook/core/design/`

### Core UI 组件测试（新建，3 个文件）
目录：`core/ui/src/test/kotlin/cn/wj/android/cashbook/core/ui/`

### Feature Screen 截图测试（新建，24 个文件）
目录：各 `feature/*/src/test/kotlin/cn/wj/android/cashbook/feature/*/screen/`

---

## 通用模板

所有截图测试类共享以下结构（后续 Task 中不再重复 license header 和注解）：

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

// package 声明...
// import 声明...

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = HiltTestApplication::class, qualifiers = "480dpi")
@LooperMode(LooperMode.Mode.PAUSED)
class XxxScreenshotTests {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    // @Test 方法...
}
```

**通用 import（所有测试类都需要）：**
```kotlin
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import cn.wj.android.cashbook.core.testing.util.captureMultiTheme
import dagger.hilt.android.testing.HiltTestApplication
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.LooperMode
```

**Screen 测试额外需要：**
```kotlin
import cn.wj.android.cashbook.core.testing.util.captureMultiDevice
```

---

## Task 1: 基础设施 — Roborazzi 插件配置

**Files:**
- Modify: `feature/records/build.gradle.kts`
- Modify: `feature/settings/build.gradle.kts`
- Modify: `feature/assets/build.gradle.kts`
- Modify: `feature/tags/build.gradle.kts`
- Modify: `feature/books/build.gradle.kts`
- Modify: `feature/types/build.gradle.kts`
- Modify: `core/ui/build.gradle.kts`

- [ ] **Step 1: 在 7 个模块的 build.gradle.kts 中添加 Roborazzi 插件**

在每个文件的 `plugins {}` 块末尾添加：
```kotlin
alias(libs.plugins.takahirom.roborazzi)
```

参考 `core/design/build.gradle.kts` 中已有的配置模式。

- [ ] **Step 2: 验证构建不报错**

```bash
./gradlew :feature:records:assembleOnlineDebug :feature:settings:assembleOnlineDebug :feature:assets:assembleOnlineDebug :feature:tags:assembleOnlineDebug :feature:books:assembleOnlineDebug :feature:types:assembleOnlineDebug :core:ui:assembleDebug --dry-run
```

Expected: BUILD SUCCESSFUL（dry-run 仅检查任务图解析）

- [ ] **Step 3: Commit**

```bash
git add feature/records/build.gradle.kts feature/settings/build.gradle.kts feature/assets/build.gradle.kts feature/tags/build.gradle.kts feature/books/build.gradle.kts feature/types/build.gradle.kts core/ui/build.gradle.kts
git commit -m "[build|all|UI测试][公共]为 feature 模块和 core/ui 添加 Roborazzi 插件"
```

---

## Task 2: 基础设施 — Screen 函数可见性修改

**Files:**
- Modify: `feature/records/src/main/kotlin/cn/wj/android/cashbook/feature/records/screen/AnalyticsScreen.kt:123`
- Modify: `feature/records/src/main/kotlin/cn/wj/android/cashbook/feature/records/screen/SearchScreen.kt:88`
- Modify: `feature/records/src/main/kotlin/cn/wj/android/cashbook/feature/records/screen/TypedAnalyticsScreen.kt:90`
- Modify: `feature/assets/src/main/kotlin/cn/wj/android/cashbook/feature/assets/screen/InvisibleAssetScreen.kt:63`

- [ ] **Step 1: 将 4 个 private fun 改为 internal fun**

在每个文件中，将 `private fun XxxScreen(` 改为 `internal fun XxxScreen(`：

- `AnalyticsScreen.kt:123` — `private fun AnalyticsScreen(` → `internal fun AnalyticsScreen(`
- `SearchScreen.kt:88` — `private fun SearchScreen(` → `internal fun SearchScreen(`
- `TypedAnalyticsScreen.kt:90` — `private fun TypedAnalyticsScreen(` → `internal fun TypedAnalyticsScreen(`
- `InvisibleAssetScreen.kt:63` — `private fun InvisibleAssetScreen(` → `internal fun InvisibleAssetScreen(`

- [ ] **Step 2: 运行 spotlessApply 确保格式正确**

```bash
./gradlew spotlessApply --init-script gradle/init.gradle.kts --no-configuration-cache
```

- [ ] **Step 3: Commit**

```bash
git add feature/records/src/main/kotlin/cn/wj/android/cashbook/feature/records/screen/AnalyticsScreen.kt feature/records/src/main/kotlin/cn/wj/android/cashbook/feature/records/screen/SearchScreen.kt feature/records/src/main/kotlin/cn/wj/android/cashbook/feature/records/screen/TypedAnalyticsScreen.kt feature/assets/src/main/kotlin/cn/wj/android/cashbook/feature/assets/screen/InvisibleAssetScreen.kt
git commit -m "[refactor|records,assets|UI测试][公共]将 4 个 private Screen 函数改为 internal 以支持截图测试"
```

---

## Task 3: Core Design — 简单包装组件截图测试（8 个）

**Files:**
- Create: `core/design/src/test/kotlin/cn/wj/android/cashbook/core/design/CardScreenshotTests.kt`
- Create: `core/design/src/test/kotlin/cn/wj/android/cashbook/core/design/TextButtonScreenshotTests.kt`
- Create: `core/design/src/test/kotlin/cn/wj/android/cashbook/core/design/IconButtonScreenshotTests.kt`
- Create: `core/design/src/test/kotlin/cn/wj/android/cashbook/core/design/FloatingActionButtonScreenshotTests.kt`
- Create: `core/design/src/test/kotlin/cn/wj/android/cashbook/core/design/DividerScreenshotTests.kt`
- Create: `core/design/src/test/kotlin/cn/wj/android/cashbook/core/design/ListItemScreenshotTests.kt`
- Create: `core/design/src/test/kotlin/cn/wj/android/cashbook/core/design/FooterScreenshotTests.kt`
- Create: `core/design/src/test/kotlin/cn/wj/android/cashbook/core/design/LoadingScreenshotTests.kt`

- [ ] **Step 1: 创建 CardScreenshotTests.kt**

```kotlin
// license header + package cn.wj.android.cashbook.core.design + 通用 import
import androidx.compose.material3.Text
import cn.wj.android.cashbook.core.design.component.CbCard
import cn.wj.android.cashbook.core.design.component.CbElevatedCard

// 通用注解
class CardScreenshotTests {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun cbCard_multipleThemes() {
        composeTestRule.captureMultiTheme(name = "Card") {
            CbCard {
                Text(text = "Card content", modifier = Modifier.padding(16.dp))
            }
        }
    }

    @Test
    fun cbCard_clickable_multipleThemes() {
        composeTestRule.captureMultiTheme(name = "Card", overrideFileName = "Card_clickable") {
            CbCard(onClick = {}) {
                Text(text = "Clickable card", modifier = Modifier.padding(16.dp))
            }
        }
    }

    @Test
    fun cbElevatedCard_multipleThemes() {
        composeTestRule.captureMultiTheme(name = "Card", overrideFileName = "ElevatedCard") {
            CbElevatedCard {
                Text(text = "Elevated card", modifier = Modifier.padding(16.dp))
            }
        }
    }
}
```

- [ ] **Step 2: 创建 TextButtonScreenshotTests.kt**

```kotlin
import androidx.compose.material3.Text
import cn.wj.android.cashbook.core.design.component.CbTextButton

class TextButtonScreenshotTests {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun cbTextButton_enabled_multipleThemes() {
        composeTestRule.captureMultiTheme(name = "TextButton") {
            CbTextButton(onClick = {}) {
                Text(text = "确认")
            }
        }
    }

    @Test
    fun cbTextButton_disabled_multipleThemes() {
        composeTestRule.captureMultiTheme(name = "TextButton", overrideFileName = "TextButton_disabled") {
            CbTextButton(onClick = {}, enabled = false) {
                Text(text = "确认")
            }
        }
    }
}
```

- [ ] **Step 3: 创建 IconButtonScreenshotTests.kt**

```kotlin
import androidx.compose.material3.Icon
import cn.wj.android.cashbook.core.design.component.CbIconButton
import cn.wj.android.cashbook.core.design.icon.CbIcons

class IconButtonScreenshotTests {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun cbIconButton_enabled_multipleThemes() {
        composeTestRule.captureMultiTheme(name = "IconButton") {
            CbIconButton(onClick = {}) {
                Icon(imageVector = CbIcons.Settings, contentDescription = null)
            }
        }
    }

    @Test
    fun cbIconButton_disabled_multipleThemes() {
        composeTestRule.captureMultiTheme(name = "IconButton", overrideFileName = "IconButton_disabled") {
            CbIconButton(onClick = {}, enabled = false) {
                Icon(imageVector = CbIcons.Settings, contentDescription = null)
            }
        }
    }
}
```

- [ ] **Step 4: 创建 FloatingActionButtonScreenshotTests.kt**

```kotlin
import androidx.compose.material3.Icon
import cn.wj.android.cashbook.core.design.component.CbFloatingActionButton
import cn.wj.android.cashbook.core.design.component.CbSmallFloatingActionButton
import cn.wj.android.cashbook.core.design.icon.CbIcons

class FloatingActionButtonScreenshotTests {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun cbFab_multipleThemes() {
        composeTestRule.captureMultiTheme(name = "FloatingActionButton") {
            CbFloatingActionButton(onClick = {}) {
                Icon(imageVector = CbIcons.Add, contentDescription = null)
            }
        }
    }

    @Test
    fun cbSmallFab_multipleThemes() {
        composeTestRule.captureMultiTheme(name = "FloatingActionButton", overrideFileName = "SmallFab") {
            CbSmallFloatingActionButton(onClick = {}) {
                Icon(imageVector = CbIcons.Add, contentDescription = null)
            }
        }
    }
}
```

- [ ] **Step 5: 创建 DividerScreenshotTests.kt**

```kotlin
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import cn.wj.android.cashbook.core.design.component.CbHorizontalDivider
import cn.wj.android.cashbook.core.design.component.CbVerticalDivider

class DividerScreenshotTests {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun cbHorizontalDivider_multipleThemes() {
        composeTestRule.captureMultiTheme(name = "Divider") {
            Column(modifier = Modifier.width(200.dp)) {
                Text(text = "上方内容")
                CbHorizontalDivider()
                Text(text = "下方内容")
            }
        }
    }

    @Test
    fun cbVerticalDivider_multipleThemes() {
        composeTestRule.captureMultiTheme(name = "Divider", overrideFileName = "VerticalDivider") {
            Row(modifier = Modifier.height(100.dp)) {
                Text(text = "左侧")
                CbVerticalDivider()
                Text(text = "右侧")
            }
        }
    }
}
```

- [ ] **Step 6: 创建 ListItemScreenshotTests.kt**

```kotlin
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import cn.wj.android.cashbook.core.design.component.CbListItem
import cn.wj.android.cashbook.core.design.icon.CbIcons

class ListItemScreenshotTests {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun cbListItem_headlineOnly_multipleThemes() {
        composeTestRule.captureMultiTheme(name = "ListItem") {
            CbListItem(
                headlineContent = { Text("标题文字") },
            )
        }
    }

    @Test
    fun cbListItem_allContent_multipleThemes() {
        composeTestRule.captureMultiTheme(name = "ListItem", overrideFileName = "ListItem_full") {
            CbListItem(
                headlineContent = { Text("标题文字") },
                overlineContent = { Text("上方文字") },
                supportingContent = { Text("辅助文字") },
                leadingContent = { Icon(imageVector = CbIcons.Settings, contentDescription = null) },
                trailingContent = { Text("尾部") },
            )
        }
    }
}
```

- [ ] **Step 7: 创建 FooterScreenshotTests.kt 和 LoadingScreenshotTests.kt**

**FooterScreenshotTests.kt:**
```kotlin
import cn.wj.android.cashbook.core.design.component.Footer

class FooterScreenshotTests {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun footer_multipleThemes() {
        composeTestRule.captureMultiTheme(name = "Footer") {
            Footer(hintText = "已经到底啦")
        }
    }
}
```

**LoadingScreenshotTests.kt:**
```kotlin
import cn.wj.android.cashbook.core.design.component.Loading

class LoadingScreenshotTests {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun loading_default_multipleThemes() {
        composeTestRule.captureMultiTheme(name = "Loading") {
            Loading()
        }
    }

    @Test
    fun loading_customHint_multipleThemes() {
        composeTestRule.captureMultiTheme(name = "Loading", overrideFileName = "Loading_customHint") {
            Loading(hintText = "正在加载数据...")
        }
    }
}
```

- [ ] **Step 8: 运行截图录制验证**

```bash
./gradlew :core:design:recordRoborazziDebug
```

Expected: BUILD SUCCESSFUL，`core/design/src/test/screenshots/` 下生成新截图

- [ ] **Step 9: Commit**

```bash
git add core/design/src/test/
git commit -m "[test|core/design|UI测试][公共]新增 Card、TextButton、IconButton、FAB、Divider、ListItem、Footer、Loading 组件截图测试"
```

---

## Task 4: Core Design — 布局容器组件截图测试（4 个）

**Files:**
- Create: `core/design/src/test/kotlin/cn/wj/android/cashbook/core/design/ScaffoldScreenshotTests.kt`
- Create: `core/design/src/test/kotlin/cn/wj/android/cashbook/core/design/TabRowScreenshotTests.kt`
- Create: `core/design/src/test/kotlin/cn/wj/android/cashbook/core/design/TopAppBarScreenshotTests.kt`
- Create: `core/design/src/test/kotlin/cn/wj/android/cashbook/core/design/VerticalGridScreenshotTests.kt`

- [ ] **Step 1: 创建 ScaffoldScreenshotTests.kt**

```kotlin
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import cn.wj.android.cashbook.core.design.component.CbFloatingActionButton
import cn.wj.android.cashbook.core.design.component.CbScaffold
import cn.wj.android.cashbook.core.design.component.CbTopAppBar
import cn.wj.android.cashbook.core.design.icon.CbIcons

class ScaffoldScreenshotTests {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun cbScaffold_withTopBarAndFab_multipleThemes() {
        composeTestRule.captureMultiTheme(name = "Scaffold") {
            CbScaffold(
                topBar = {
                    CbTopAppBar(
                        title = { Text("页面标题") },
                        onBackClick = {},
                    )
                },
                floatingActionButton = {
                    CbFloatingActionButton(onClick = {}) {
                        Icon(imageVector = CbIcons.Add, contentDescription = null)
                    }
                },
            ) { paddingValues ->
                Text(
                    text = "页面内容",
                    modifier = Modifier.padding(paddingValues),
                )
            }
        }
    }
}
```

- [ ] **Step 2: 创建 TabRowScreenshotTests.kt**

```kotlin
import androidx.compose.material3.Text
import cn.wj.android.cashbook.core.design.component.CbTab
import cn.wj.android.cashbook.core.design.component.CbTabRow

class TabRowScreenshotTests {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun cbTabRow_firstSelected_multipleThemes() {
        composeTestRule.captureMultiTheme(name = "TabRow") {
            CbTabRow(selectedTabIndex = 0) {
                CbTab(selected = true, onClick = {}, text = { Text("支出") })
                CbTab(selected = false, onClick = {}, text = { Text("收入") })
                CbTab(selected = false, onClick = {}, text = { Text("转账") })
            }
        }
    }

    @Test
    fun cbTabRow_secondSelected_multipleThemes() {
        composeTestRule.captureMultiTheme(name = "TabRow", overrideFileName = "TabRow_secondSelected") {
            CbTabRow(selectedTabIndex = 1) {
                CbTab(selected = false, onClick = {}, text = { Text("支出") })
                CbTab(selected = true, onClick = {}, text = { Text("收入") })
                CbTab(selected = false, onClick = {}, text = { Text("转账") })
            }
        }
    }
}
```

- [ ] **Step 3: 创建 TopAppBarScreenshotTests.kt**

```kotlin
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import cn.wj.android.cashbook.core.design.component.CbIconButton
import cn.wj.android.cashbook.core.design.component.CbTopAppBar
import cn.wj.android.cashbook.core.design.icon.CbIcons

class TopAppBarScreenshotTests {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun cbTopAppBar_withBackButton_multipleThemes() {
        composeTestRule.captureMultiTheme(name = "TopAppBar") {
            CbTopAppBar(
                title = { Text("设置") },
                onBackClick = {},
            )
        }
    }

    @Test
    fun cbTopAppBar_withActions_multipleThemes() {
        composeTestRule.captureMultiTheme(name = "TopAppBar", overrideFileName = "TopAppBar_actions") {
            CbTopAppBar(
                title = { Text("记录") },
                onBackClick = {},
                actions = {
                    CbIconButton(onClick = {}) {
                        Icon(imageVector = CbIcons.Search, contentDescription = null)
                    }
                },
            )
        }
    }

    @Test
    fun cbTopAppBar_noNavigationIcon_multipleThemes() {
        composeTestRule.captureMultiTheme(name = "TopAppBar", overrideFileName = "TopAppBar_noNav") {
            CbTopAppBar(
                title = { Text("首页") },
            )
        }
    }
}
```

- [ ] **Step 4: 创建 VerticalGridScreenshotTests.kt**

```kotlin
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import cn.wj.android.cashbook.core.design.component.CbVerticalGrid

class VerticalGridScreenshotTests {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun cbVerticalGrid_2columns_multipleThemes() {
        composeTestRule.captureMultiTheme(name = "VerticalGrid") {
            CbVerticalGrid(
                columns = 2,
                items = listOf("项目1", "项目2", "项目3", "项目4"),
            ) { item ->
                Text(text = item, modifier = Modifier.padding(8.dp))
            }
        }
    }

    @Test
    fun cbVerticalGrid_3columns_multipleThemes() {
        composeTestRule.captureMultiTheme(name = "VerticalGrid", overrideFileName = "VerticalGrid_3col") {
            CbVerticalGrid(
                columns = 3,
                items = listOf("A", "B", "C", "D", "E", "F"),
            ) { item ->
                Text(text = item, modifier = Modifier.padding(8.dp))
            }
        }
    }
}
```

- [ ] **Step 5: 运行截图录制验证**

```bash
./gradlew :core:design:recordRoborazziDebug
```

- [ ] **Step 6: Commit**

```bash
git add core/design/src/test/
git commit -m "[test|core/design|UI测试][公共]新增 Scaffold、TabRow、TopAppBar、VerticalGrid 组件截图测试"
```

---

## Task 5: Core Design — 输入组件截图测试（2 个）

**Files:**
- Create: `core/design/src/test/kotlin/cn/wj/android/cashbook/core/design/TextFieldScreenshotTests.kt`
- Create: `core/design/src/test/kotlin/cn/wj/android/cashbook/core/design/EmptyScreenshotTests.kt`

- [ ] **Step 1: 创建 TextFieldScreenshotTests.kt**

先阅读 `core/design/src/main/kotlin/cn/wj/android/cashbook/core/design/component/TextField.kt` 和 `TextFieldState.kt` 了解 TextFieldState 的构造方式。

```kotlin
import androidx.compose.material3.Text
import cn.wj.android.cashbook.core.design.component.CbTextField
import cn.wj.android.cashbook.core.design.component.CbOutlinedTextField
import cn.wj.android.cashbook.core.design.component.CbPasswordTextField
import cn.wj.android.cashbook.core.design.component.TextFieldState

class TextFieldScreenshotTests {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun cbTextField_empty_multipleThemes() {
        composeTestRule.captureMultiTheme(name = "TextField") {
            CbTextField(
                textFieldState = TextFieldState(defaultText = ""),
                label = { Text("标签") },
                placeholder = { Text("请输入") },
            )
        }
    }

    @Test
    fun cbTextField_withText_multipleThemes() {
        composeTestRule.captureMultiTheme(name = "TextField", overrideFileName = "TextField_withText") {
            CbTextField(
                textFieldState = TextFieldState(defaultText = "已输入内容"),
                label = { Text("标签") },
            )
        }
    }

    @Test
    fun cbTextField_error_multipleThemes() {
        // TextFieldState 通过 validator + requestErrors() 控制错误状态
        val errorState = TextFieldState(
            defaultText = "",
            validator = { false },
            errorFor = { "必填项" },
        ).apply { requestErrors() }
        composeTestRule.captureMultiTheme(name = "TextField", overrideFileName = "TextField_error") {
            CbTextField(
                textFieldState = errorState,
                label = { Text("标签") },
            )
        }
    }

    @Test
    fun cbOutlinedTextField_multipleThemes() {
        composeTestRule.captureMultiTheme(name = "TextField", overrideFileName = "OutlinedTextField") {
            CbOutlinedTextField(
                textFieldState = TextFieldState(defaultText = "内容"),
                label = { Text("备注") },
            )
        }
    }

    @Test
    fun cbPasswordTextField_multipleThemes() {
        composeTestRule.captureMultiTheme(name = "TextField", overrideFileName = "PasswordTextField") {
            CbPasswordTextField(
                textFieldState = TextFieldState(defaultText = "123456"),
                label = { Text("密码") },
            )
        }
    }
}

- [ ] **Step 2: 创建 EmptyScreenshotTests.kt**

```kotlin
import androidx.compose.material3.Text
import cn.wj.android.cashbook.core.design.component.CbTextButton
import cn.wj.android.cashbook.core.design.component.Empty

class EmptyScreenshotTests {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun empty_withHint_multipleThemes() {
        composeTestRule.captureMultiTheme(name = "Empty") {
            Empty(hintText = "暂无数据")
        }
    }

    @Test
    fun empty_withButton_multipleThemes() {
        composeTestRule.captureMultiTheme(name = "Empty", overrideFileName = "Empty_withButton") {
            Empty(hintText = "暂无数据") {
                CbTextButton(onClick = {}) {
                    Text(text = "去添加")
                }
            }
        }
    }
}
```

- [ ] **Step 3: 运行截图录制验证**

```bash
./gradlew :core:design:recordRoborazziDebug
```

- [ ] **Step 4: Commit**

```bash
git add core/design/src/test/
git commit -m "[test|core/design|UI测试][公共]新增 TextField、Empty 组件截图测试"
```

---

## Task 6: Core Design — 自绘制图表组件截图测试（2 个）

**Files:**
- Create: `core/design/src/test/kotlin/cn/wj/android/cashbook/core/design/PieChartScreenshotTests.kt`
- Create: `core/design/src/test/kotlin/cn/wj/android/cashbook/core/design/LineChartScreenshotTests.kt`

- [ ] **Step 1: 创建 PieChartScreenshotTests.kt**

先阅读 `core/design/src/main/kotlin/cn/wj/android/cashbook/core/design/component/PieChart.kt` 确认 `PieSlice` 和 `CbPieChart` 的 API。

```kotlin
import androidx.compose.foundation.layout.size
import androidx.compose.ui.graphics.Color
import cn.wj.android.cashbook.core.design.component.CbPieChart
import cn.wj.android.cashbook.core.design.component.PieSlice

class PieChartScreenshotTests {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val testSlices = listOf(
        PieSlice(label = "餐饮", value = 3500f, color = Color(0xFFE57373)),
        PieSlice(label = "交通", value = 1200f, color = Color(0xFF81C784)),
        PieSlice(label = "购物", value = 2800f, color = Color(0xFF64B5F6)),
        PieSlice(label = "其他", value = 500f, color = Color(0xFFFFB74D)),
    )

    @Test
    fun cbPieChart_withData_multipleThemes() {
        composeTestRule.captureMultiTheme(name = "PieChart") {
            CbPieChart(
                slices = testSlices,
                centerText = "¥8,000",
                modifier = Modifier.size(200.dp),
            )
        }
    }

    @Test
    fun cbPieChart_empty_multipleThemes() {
        composeTestRule.captureMultiTheme(name = "PieChart", overrideFileName = "PieChart_empty") {
            CbPieChart(
                slices = emptyList(),
                centerText = "¥0",
                modifier = Modifier.size(200.dp),
            )
        }
    }

    @Test
    fun cbPieChart_selectedSlice_multipleThemes() {
        composeTestRule.captureMultiTheme(name = "PieChart", overrideFileName = "PieChart_selected") {
            CbPieChart(
                slices = testSlices,
                centerText = "¥3,500",
                selectedIndex = 0,
                modifier = Modifier.size(200.dp),
            )
        }
    }
}
```

- [ ] **Step 2: 创建 LineChartScreenshotTests.kt**

先阅读 `core/design/src/main/kotlin/cn/wj/android/cashbook/core/design/component/LineChart.kt` 确认 `LineDataSet`、`LineEntry` 和 `CbLineChart` 的 API。

```kotlin
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.graphics.Color
import cn.wj.android.cashbook.core.design.component.CbLineChart
import cn.wj.android.cashbook.core.design.component.LineDataSet
import cn.wj.android.cashbook.core.design.component.LineEntry

class LineChartScreenshotTests {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val singleDataSet = listOf(
        LineDataSet(
            label = "支出",
            entries = listOf(
                LineEntry(x = 1f, y = 3500f, label = "1月"),
                LineEntry(x = 2f, y = 2800f, label = "2月"),
                LineEntry(x = 3f, y = 4200f, label = "3月"),
                LineEntry(x = 4f, y = 3100f, label = "4月"),
            ),
            color = Color(0xFFE57373),
        ),
    )

    private val multipleDataSets = singleDataSet + listOf(
        LineDataSet(
            label = "收入",
            entries = listOf(
                LineEntry(x = 1f, y = 5000f, label = "1月"),
                LineEntry(x = 2f, y = 5200f, label = "2月"),
                LineEntry(x = 3f, y = 4800f, label = "3月"),
                LineEntry(x = 4f, y = 5500f, label = "4月"),
            ),
            color = Color(0xFF81C784),
        ),
    )

    @Test
    fun cbLineChart_singleDataSet_multipleThemes() {
        composeTestRule.captureMultiTheme(name = "LineChart") {
            CbLineChart(
                dataSets = singleDataSet,
                modifier = Modifier.fillMaxWidth().height(200.dp),
            )
        }
    }

    @Test
    fun cbLineChart_multipleDataSets_multipleThemes() {
        composeTestRule.captureMultiTheme(name = "LineChart", overrideFileName = "LineChart_multi") {
            CbLineChart(
                dataSets = multipleDataSets,
                modifier = Modifier.fillMaxWidth().height(200.dp),
            )
        }
    }

    @Test
    fun cbLineChart_empty_multipleThemes() {
        composeTestRule.captureMultiTheme(name = "LineChart", overrideFileName = "LineChart_empty") {
            CbLineChart(
                dataSets = emptyList(),
                modifier = Modifier.fillMaxWidth().height(200.dp),
            )
        }
    }

    @Test
    fun cbLineChart_withZeroLine_multipleThemes() {
        composeTestRule.captureMultiTheme(name = "LineChart", overrideFileName = "LineChart_zeroLine") {
            CbLineChart(
                dataSets = singleDataSet,
                showZeroLine = true,
                modifier = Modifier.fillMaxWidth().height(200.dp),
            )
        }
    }
}
```

- [ ] **Step 3: 运行截图录制验证**

```bash
./gradlew :core:design:recordRoborazziDebug
```

- [ ] **Step 4: Commit**

```bash
git add core/design/src/test/
git commit -m "[test|core/design|UI测试][公共]新增 PieChart、LineChart 自绘图表组件截图测试"
```

---

## Task 7: Core Design — 交互组件截图测试（3 个）

**Files:**
- Create: `core/design/src/test/kotlin/cn/wj/android/cashbook/core/design/CalculatorScreenshotTests.kt`
- Create: `core/design/src/test/kotlin/cn/wj/android/cashbook/core/design/CalendarViewScreenshotTests.kt`
- Create: `core/design/src/test/kotlin/cn/wj/android/cashbook/core/design/WheelPickerScreenshotTests.kt`

- [ ] **Step 1: 创建 CalculatorScreenshotTests.kt**

先阅读 `Calculator.kt` 确认 API 签名。

```kotlin
import androidx.compose.material3.MaterialTheme
import cn.wj.android.cashbook.core.design.component.Calculator

class CalculatorScreenshotTests {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun calculator_default_multipleThemes() {
        composeTestRule.captureMultiTheme(name = "Calculator") {
            Calculator(
                defaultText = "",
                primaryColor = MaterialTheme.colorScheme.primary,
                onConfirmClick = {},
            )
        }
    }

    @Test
    fun calculator_withInput_multipleThemes() {
        composeTestRule.captureMultiTheme(name = "Calculator", overrideFileName = "Calculator_withInput") {
            Calculator(
                defaultText = "128.50",
                primaryColor = MaterialTheme.colorScheme.primary,
                onConfirmClick = {},
            )
        }
    }
}
```

- [ ] **Step 2: 创建 CalendarViewScreenshotTests.kt**

先阅读 `CalendarView.kt` 确认 API 签名。

```kotlin
import java.time.LocalDate
import cn.wj.android.cashbook.core.design.component.CalendarView

class CalendarViewScreenshotTests {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun calendarView_default_multipleThemes() {
        composeTestRule.captureMultiTheme(name = "CalendarView") {
            CalendarView(
                onDateSelected = {},
                selectDate = LocalDate.of(2024, 1, 15),
            )
        }
    }

    @Test
    fun calendarView_selectedDate_multipleThemes() {
        composeTestRule.captureMultiTheme(name = "CalendarView", overrideFileName = "CalendarView_selected") {
            CalendarView(
                onDateSelected = {},
                selectDate = LocalDate.of(2024, 6, 20),
            )
        }
    }
}
```

- [ ] **Step 3: 创建 WheelPickerScreenshotTests.kt**

先阅读 `WheelPicker.kt` 确认 API 签名。

```kotlin
import cn.wj.android.cashbook.core.design.component.CbWheelPicker

class WheelPickerScreenshotTests {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun cbWheelPicker_default_multipleThemes() {
        composeTestRule.captureMultiTheme(name = "WheelPicker") {
            CbWheelPicker(
                items = listOf("2022", "2023", "2024", "2025", "2026"),
                selectedIndex = 2,
                onItemSelected = {},
            )
        }
    }

    @Test
    fun cbWheelPicker_moreVisible_multipleThemes() {
        composeTestRule.captureMultiTheme(name = "WheelPicker", overrideFileName = "WheelPicker_5visible") {
            CbWheelPicker(
                items = listOf("1月", "2月", "3月", "4月", "5月", "6月", "7月", "8月", "9月", "10月", "11月", "12月"),
                selectedIndex = 5,
                onItemSelected = {},
                visibleItemCount = 5,
            )
        }
    }
}
```

- [ ] **Step 4: 运行截图录制验证**

```bash
./gradlew :core:design:recordRoborazziDebug
```

- [ ] **Step 5: Commit**

```bash
git add core/design/src/test/
git commit -m "[test|core/design|UI测试][公共]新增 Calculator、CalendarView、WheelPicker 交互组件截图测试"
```

---

## Task 8: Core Design — Dialog/Sheet 组件截图测试（3 个）

**Files:**
- Create: `core/design/src/test/kotlin/cn/wj/android/cashbook/core/design/AlertDialogScreenshotTests.kt`
- Create: `core/design/src/test/kotlin/cn/wj/android/cashbook/core/design/DateRangePickerScreenshotTests.kt`
- Create: `core/design/src/test/kotlin/cn/wj/android/cashbook/core/design/ModalBottomSheetScreenshotTests.kt`

- [ ] **Step 1: 阅读源码确认 Dialog 截图方式**

Dialog 和 BottomSheet 在 Robolectric 环境下使用独立 window 渲染，`captureMultiTheme` 默认截取 root 可能无法捕获 Dialog 内容。

先阅读以下文件确认组件行为：
- `core/design/src/main/kotlin/cn/wj/android/cashbook/core/design/component/AlertDialog.kt`
- `core/design/src/main/kotlin/cn/wj/android/cashbook/core/design/component/DateTimePicker.kt`
- `core/design/src/main/kotlin/cn/wj/android/cashbook/core/design/component/ModalBottomSheet.kt`

如果 Dialog 在 Robolectric 下无法通过 `onRoot()` 截图，需要：
1. 使用 `CbBaseAlterDialog` 的 content lambda 方式，直接渲染内容部分而非完整 Dialog
2. 或使用 `onAllNodes(isDialog())` 定位 Dialog 节点

**如果 Dialog 截图有问题，采用备选方案：直接测试 Dialog 内容区域而非完整 Dialog。**

- [ ] **Step 2: 创建 AlertDialogScreenshotTests.kt**

```kotlin
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import cn.wj.android.cashbook.core.design.component.CbAlertDialog
import cn.wj.android.cashbook.core.design.component.CbTextButton
import cn.wj.android.cashbook.core.design.icon.CbIcons

class AlertDialogScreenshotTests {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun cbAlertDialog_titleAndText_multipleThemes() {
        composeTestRule.captureMultiTheme(name = "AlertDialog") {
            CbAlertDialog(
                onDismissRequest = {},
                title = { Text("确认删除") },
                text = { Text("删除后无法恢复，是否继续？") },
                confirmButton = {
                    CbTextButton(onClick = {}) { Text("删除") }
                },
                dismissButton = {
                    CbTextButton(onClick = {}) { Text("取消") }
                },
            )
        }
    }

    @Test
    fun cbAlertDialog_withIcon_multipleThemes() {
        composeTestRule.captureMultiTheme(name = "AlertDialog", overrideFileName = "AlertDialog_icon") {
            CbAlertDialog(
                onDismissRequest = {},
                icon = { Icon(imageVector = CbIcons.Info, contentDescription = null) },
                title = { Text("警告") },
                text = { Text("此操作不可撤销") },
                confirmButton = {
                    CbTextButton(onClick = {}) { Text("确认") }
                },
            )
        }
    }

    @Test
    fun cbAlertDialog_confirmOnly_multipleThemes() {
        composeTestRule.captureMultiTheme(name = "AlertDialog", overrideFileName = "AlertDialog_confirmOnly") {
            CbAlertDialog(
                onDismissRequest = {},
                title = { Text("提示") },
                text = { Text("操作已完成") },
                confirmButton = {
                    CbTextButton(onClick = {}) { Text("知道了") }
                },
            )
        }
    }
}
```

注意：`CbIcons.Info` 已确认存在于 `CbIcons` 对象中。

- [ ] **Step 3: 创建 DateRangePickerScreenshotTests.kt 和 ModalBottomSheetScreenshotTests.kt**

这两个组件涉及复杂的 window 管理，需要先运行一个简单测试验证 Robolectric 下的行为。如果直接截图不可行，使用以下替代方案：

**DateRangePickerScreenshotTests.kt** — 如果 `DateRangePickerDialog` 截图有问题，跳过该组件或仅截取内部 content。

**ModalBottomSheetScreenshotTests.kt:**
```kotlin
import androidx.compose.material3.Text
import cn.wj.android.cashbook.core.design.component.CbListItem
import cn.wj.android.cashbook.core.design.component.CbModalBottomSheet

class ModalBottomSheetScreenshotTests {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun cbModalBottomSheet_withContent_multipleThemes() {
        composeTestRule.captureMultiTheme(name = "ModalBottomSheet") {
            CbModalBottomSheet(onDismissRequest = {}) {
                CbListItem(headlineContent = { Text("选项一") })
                CbListItem(headlineContent = { Text("选项二") })
                CbListItem(headlineContent = { Text("选项三") })
            }
        }
    }
}
```

- [ ] **Step 4: 运行截图录制，排查 Dialog/Sheet 截图问题**

```bash
./gradlew :core:design:recordRoborazziDebug --tests "*AlertDialog*" --tests "*ModalBottomSheet*" --tests "*DateRangePicker*"
```

如果某些测试失败，根据错误信息调整截图策略（可能需要使用 `onAllNodes(isDialog())` 或直接渲染内容部分）。

- [ ] **Step 5: Commit**

```bash
git add core/design/src/test/
git commit -m "[test|core/design|UI测试][公共]新增 AlertDialog、DateRangePicker、ModalBottomSheet 组件截图测试"
```

---

## Task 9: Core UI 组件截图测试（3 个）

**Files:**
- Create: `core/ui/src/test/kotlin/cn/wj/android/cashbook/core/ui/TypeIconScreenshotTests.kt`
- Create: `core/ui/src/test/kotlin/cn/wj/android/cashbook/core/ui/DateSelectionPopupScreenshotTests.kt`
- Create: `core/ui/src/test/kotlin/cn/wj/android/cashbook/core/ui/SelectDateDialogScreenshotTests.kt`

- [ ] **Step 1: 阅读源码**

阅读以下文件确认 API：
- `core/ui/src/main/kotlin/cn/wj/android/cashbook/core/ui/component/TypeIcon.kt`
- `core/ui/src/main/kotlin/cn/wj/android/cashbook/core/ui/component/DateSelectionPopup.kt`
- `core/ui/src/main/kotlin/cn/wj/android/cashbook/core/ui/component/SelectDateDialog.kt`

还需要阅读 `DateSelectionEntity` 定义以了解如何构造不同选择状态。

- [ ] **Step 2: 创建 TypeIconScreenshotTests.kt**

```kotlin
package cn.wj.android.cashbook.core.ui

// 通用 import + 额外：
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.res.painterResource
import cn.wj.android.cashbook.core.ui.component.TypeIcon

class TypeIconScreenshotTests {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun typeIcon_default_multipleThemes() {
        composeTestRule.captureMultiTheme(name = "TypeIcon") {
            // 使用一个可用的 drawable 资源，需确认具体资源 ID
            TypeIcon(
                painter = painterResource(id = cn.wj.android.cashbook.core.design.R.drawable.xxx),
            )
        }
    }

    @Test
    fun typeIcon_customColor_multipleThemes() {
        composeTestRule.captureMultiTheme(name = "TypeIcon", overrideFileName = "TypeIcon_customColor") {
            TypeIcon(
                painter = painterResource(id = cn.wj.android.cashbook.core.design.R.drawable.xxx),
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            )
        }
    }

    @Test
    fun typeIcon_showMore_multipleThemes() {
        composeTestRule.captureMultiTheme(name = "TypeIcon", overrideFileName = "TypeIcon_showMore") {
            TypeIcon(
                painter = painterResource(id = cn.wj.android.cashbook.core.design.R.drawable.xxx),
                showMore = true,
            )
        }
    }
}
```

注意：`painter` 参数需要使用实际项目中可用的 drawable 资源。先在 `core/design/src/main/res/drawable*/` 下查找可用的类型图标资源。

- [ ] **Step 3: 创建 DateSelectionPopupScreenshotTests.kt**

```kotlin
package cn.wj.android.cashbook.core.ui

import cn.wj.android.cashbook.core.model.entity.DateSelectionEntity
import cn.wj.android.cashbook.core.ui.component.DateSelectionPopup

class DateSelectionPopupScreenshotTests {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun dateSelectionPopup_byDay_multipleThemes() {
        composeTestRule.captureMultiTheme(name = "DateSelectionPopup") {
            // 需要根据 DateSelectionEntity 的实际子类构造
            DateSelectionPopup(
                expanded = true,
                onDismissRequest = {},
                currentSelection = DateSelectionEntity.ByDay(/* 参数 */),
                onDateSelected = {},
            )
        }
    }

    // 类似地为 ByMonth、ByYear、ByRange、All 各创建一个测试方法
}
```

注意：`DateSelectionEntity` 是密封类，需要先阅读其定义了解所有子类和构造参数。

- [ ] **Step 4: 创建 SelectDateDialogScreenshotTests.kt**

```kotlin
package cn.wj.android.cashbook.core.ui

import java.time.YearMonth
import cn.wj.android.cashbook.core.ui.component.SelectDateDialog

class SelectDateDialogScreenshotTests {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun selectDateDialog_monthSelection_multipleThemes() {
        composeTestRule.captureMultiTheme(name = "SelectDateDialog") {
            SelectDateDialog(
                onDialogDismiss = {},
                currentDate = YearMonth.of(2024, 6),
                onDateSelected = { _, _ -> },
            )
        }
    }

    @Test
    fun selectDateDialog_yearSelectable_multipleThemes() {
        composeTestRule.captureMultiTheme(name = "SelectDateDialog", overrideFileName = "SelectDateDialog_yearSelectable") {
            SelectDateDialog(
                onDialogDismiss = {},
                currentDate = YearMonth.of(2024, 6),
                yearSelectable = true,
                onDateSelected = { _, _ -> },
            )
        }
    }

    @Test
    fun selectDateDialog_yearSelected_multipleThemes() {
        composeTestRule.captureMultiTheme(name = "SelectDateDialog", overrideFileName = "SelectDateDialog_yearSelected") {
            SelectDateDialog(
                onDialogDismiss = {},
                currentDate = YearMonth.of(2024, 6),
                yearSelectable = true,
                yearSelected = true,
                onDateSelected = { _, _ -> },
            )
        }
    }
}
```

- [ ] **Step 5: 运行截图录制验证**

```bash
./gradlew :core:ui:recordRoborazziDebug
```

- [ ] **Step 6: Commit**

```bash
git add core/ui/src/test/
git commit -m "[test|core/ui|UI测试][公共]新增 TypeIcon、DateSelectionPopup、SelectDateDialog 组件截图测试"
```

---

## Task 10: Feature/Settings — Screen 截图测试（5 个）

**Files:**
- Create: `feature/settings/src/test/kotlin/cn/wj/android/cashbook/feature/settings/screen/LauncherScreenScreenshotTests.kt`
- Create: `feature/settings/src/test/kotlin/cn/wj/android/cashbook/feature/settings/screen/SettingScreenScreenshotTests.kt`
- Create: `feature/settings/src/test/kotlin/cn/wj/android/cashbook/feature/settings/screen/BackupAndRecoveryScreenScreenshotTests.kt`
- Create: `feature/settings/src/test/kotlin/cn/wj/android/cashbook/feature/settings/screen/AboutUsScreenScreenshotTests.kt`
- Create: `feature/settings/src/test/kotlin/cn/wj/android/cashbook/feature/settings/screen/MarkdownScreenScreenshotTests.kt`

- [ ] **Step 1: 阅读 Screen 源码和 UiState 定义**

阅读以下文件，确认内部 Composable 的完整参数签名和 UiState 定义：
- `feature/settings/src/main/kotlin/.../screen/LauncherScreen.kt` (line 129+)
- `feature/settings/src/main/kotlin/.../screen/SettingScreen.kt` (line 167+)
- `feature/settings/src/main/kotlin/.../screen/BackupAndRecoveryScreen.kt` (line 155+)
- `feature/settings/src/main/kotlin/.../screen/AboutUsScreen.kt` (line 130+)
- `feature/settings/src/main/kotlin/.../screen/MarkdownScreen.kt` (line 83+)
- `feature/settings/src/main/kotlin/.../viewmodel/LauncherViewModel.kt` — LauncherUiState
- `feature/settings/src/main/kotlin/.../viewmodel/SettingViewModel.kt` — SettingUiState
- `feature/settings/src/main/kotlin/.../viewmodel/BackupAndRecoveryViewModel.kt` — BackupAndRecoveryUiState
- `feature/settings/src/main/kotlin/.../viewmodel/AboutUsViewModel.kt` — AboutUsUiState

- [ ] **Step 2: 创建每个 Screen 的测试类**

每个测试类的模式：

```kotlin
package cn.wj.android.cashbook.feature.settings.screen

// 通用注解 + import
// 额外 import: captureMultiDevice、对应的 UiState、DialogState 等

class XxxScreenScreenshotTests {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun xxxScreen_loading_multipleThemes() {
        composeTestRule.captureMultiTheme(name = "XxxScreen") {
            XxxScreen(
                uiState = XxxUiState.Loading,
                // 其他必需参数传默认空值或 {}
            )
        }
    }

    @Test
    fun xxxScreen_success_multipleThemes() {
        composeTestRule.captureMultiTheme(name = "XxxScreen", overrideFileName = "XxxScreen_success") {
            XxxScreen(
                uiState = XxxUiState.Success(/* 构造测试数据 */),
                // 其他参数...
            )
        }
    }

    @Test
    fun xxxScreen_success_multipleDevices() {
        composeTestRule.captureMultiDevice(screenshotName = "XxxScreen") {
            // 需要包裹在 CashbookTheme 中
            CashbookTheme {
                XxxScreen(
                    uiState = XxxUiState.Success(/* 构造测试数据 */),
                    // 其他参数...
                )
            }
        }
    }
}
```

**关键实现细节：**

- `LauncherScreenScreenshotTests` — LauncherUiState 只有 Loading 和 Success(currentBookName)，参数较少
- `SettingScreenScreenshotTests` — 参数最多（30+ 个 callback），全部传 `{}`；需要构造不同 dialogState 变体
- `BackupAndRecoveryScreenScreenshotTests` — 需要构造 WebDAV 配置/未配置两种状态
- `AboutUsScreenScreenshotTests` — 较简单，Loading + Success
- `MarkdownScreenScreenshotTests` — 接收 String 参数，非 sealed UiState

- [ ] **Step 3: 运行截图录制验证**

```bash
./gradlew :feature:settings:recordRoborazziDebug
```

- [ ] **Step 4: Commit**

```bash
git add feature/settings/src/test/
git commit -m "[test|feature/settings|UI测试][公共]新增设置模块 5 个 Screen 截图测试"
```

---

## Task 11: Feature/Assets — Screen 截图测试（5 个）

**Files:**
- Create: `feature/assets/src/test/kotlin/cn/wj/android/cashbook/feature/assets/screen/MyAssetScreenScreenshotTests.kt`
- Create: `feature/assets/src/test/kotlin/cn/wj/android/cashbook/feature/assets/screen/EditAssetScreenScreenshotTests.kt`
- Create: `feature/assets/src/test/kotlin/cn/wj/android/cashbook/feature/assets/screen/AssetInfoScreenScreenshotTests.kt`
- Create: `feature/assets/src/test/kotlin/cn/wj/android/cashbook/feature/assets/screen/EditRecordSelectAssetBottomSheetScreenScreenshotTests.kt`
- Create: `feature/assets/src/test/kotlin/cn/wj/android/cashbook/feature/assets/screen/InvisibleAssetScreenScreenshotTests.kt`

- [ ] **Step 1: 阅读 Screen 源码和 UiState 定义**

阅读以下文件：
- 5 个 Screen 文件的内部 Composable 签名
- `MyAssetViewModel.kt` — MyAssetUiState (Loading, Success(topUpInTotal, totalAsset, totalLiabilities, netAsset))
- `EditAssetViewModel.kt` — EditAssetUiState (Loading, Success)
- `AssetInfoViewModel.kt` — AssetInfoUiState (Loading, Success)
- `InvisibleAssetViewModel.kt` — 无 UiState，使用 assetTypedListData
- `EditRecordSelectAssetBottomSheetScreen.kt` — 确认参数

还需阅读 `AssetTypeViewsModel` 的定义以构造列表数据。

- [ ] **Step 2: 创建测试类**

遵循 Task 10 的模式。特殊点：

- **MyAssetScreen** — 需要构造 `assetTypedListData: List<AssetTypeViewsModel>` 参数
- **InvisibleAssetScreen** — 无 UiState，直接传入参数
- **EditRecordSelectAssetBottomSheetScreen** — BottomSheet 内容渲染

- [ ] **Step 3: 运行截图录制验证**

```bash
./gradlew :feature:assets:recordRoborazziDebug
```

- [ ] **Step 4: Commit**

```bash
git add feature/assets/src/test/
git commit -m "[test|feature/assets|UI测试][公共]新增资产模块 5 个 Screen 截图测试"
```

---

## Task 12: Feature/Records — Screen 截图测试（8 个）

**Files:**
- Create: `feature/records/src/test/kotlin/cn/wj/android/cashbook/feature/records/screen/LauncherContentScreenScreenshotTests.kt`
- Create: `feature/records/src/test/kotlin/cn/wj/android/cashbook/feature/records/screen/EditRecordScreenScreenshotTests.kt`
- Create: `feature/records/src/test/kotlin/cn/wj/android/cashbook/feature/records/screen/AnalyticsScreenScreenshotTests.kt`
- Create: `feature/records/src/test/kotlin/cn/wj/android/cashbook/feature/records/screen/CalendarScreenScreenshotTests.kt`
- Create: `feature/records/src/test/kotlin/cn/wj/android/cashbook/feature/records/screen/SearchScreenScreenshotTests.kt`
- Create: `feature/records/src/test/kotlin/cn/wj/android/cashbook/feature/records/screen/SelectRelatedRecordScreenScreenshotTests.kt`
- Create: `feature/records/src/test/kotlin/cn/wj/android/cashbook/feature/records/screen/TypedAnalyticsScreenScreenshotTests.kt`
- Create: `feature/records/src/test/kotlin/cn/wj/android/cashbook/feature/records/screen/AssetInfoContentScreenScreenshotTests.kt`

- [ ] **Step 1: 阅读 Screen 源码和 UiState 定义**

阅读以下文件：
- 8 个 Screen 文件的内部 Composable 签名
- 对应的 ViewModel 文件中的 UiState 定义：
  - LauncherContentUiState (Loading, Success(topBgUri, totalIncome, totalExpand, totalBalance))
  - EditRecordUiState (Loading, Success(amountText, chargesText, ...))
  - AnalyticsUiState (Loading, Success(granularity, titleText, ...))
  - CalendarUiState (Loading, Success(monthIncome, monthExpand, monthBalance, schemas, recordList))
  - TypedAnalyticsUiState (Loading, Success(isType, titleText, subTitleText))
  - SelectRelatedRecordUiState (Loading, Success(relatedRecordList, recordList))

还需阅读：
- `DateSelectionEntity` 密封类定义
- `AnalyticsRecordBarEntity` 和 `AnalyticsRecordPieEntity` 定义
- `RecordDayEntity` 和 `RecordViewsEntity` 定义
- `LauncherListItem` 定义

- [ ] **Step 2: 创建测试类**

特殊处理：

- **LauncherContentScreen** — 需要 `LazyPagingItems<LauncherListItem>`，使用：
  ```kotlin
  val pagingItems = flowOf(PagingData.from(testList)).collectAsLazyPagingItems()
  ```
  还需要构造 `BackdropScaffoldState` 和 `WindowAdaptiveInfo`

- **SearchScreen** — 需要 `LazyPagingItems<RecordViewsEntity>`，同上模式构造
- **AnalyticsScreen** — 需要构造 `AnalyticsRecordBarEntity` 和 `AnalyticsRecordPieEntity` 列表
- **CalendarScreen** — 需要构造 `schemas: Map<LocalDate, RecordDayEntity>` 和日期选择状态
- **EditRecordScreen** — 参数较多，callback 全部传 `{}`

- [ ] **Step 3: 运行截图录制验证**

```bash
./gradlew :feature:records:recordRoborazziDebug
```

- [ ] **Step 4: Commit**

```bash
git add feature/records/src/test/
git commit -m "[test|feature/records|UI测试][公共]新增记录模块 8 个 Screen 截图测试"
```

---

## Task 13: Feature/Tags — Screen 截图测试（2 个）

**Files:**
- Create: `feature/tags/src/test/kotlin/cn/wj/android/cashbook/feature/tags/screen/MyTagsScreenScreenshotTests.kt`
- Create: `feature/tags/src/test/kotlin/cn/wj/android/cashbook/feature/tags/screen/EditRecordSelectTagBottomSheetScreenScreenshotTests.kt`

- [ ] **Step 1: 阅读 Screen 源码**

阅读以下文件确认内部 Composable 参数：
- `feature/tags/src/main/kotlin/.../screen/MyTagsScreen.kt` (line 106+)
- `feature/tags/src/main/kotlin/.../screen/EditRecordSelectTagBottomSheetScreen.kt` (line 98+)
- `feature/tags/src/main/kotlin/.../viewmodel/MyTagsViewModel.kt` — 确认数据流

- [ ] **Step 2: 创建测试类**

使用 `createTagModel()` 构造测试数据。

- [ ] **Step 3: 运行截图录制验证**

```bash
./gradlew :feature:tags:recordRoborazziDebug
```

- [ ] **Step 4: Commit**

```bash
git add feature/tags/src/test/
git commit -m "[test|feature/tags|UI测试][公共]新增标签模块 2 个 Screen 截图测试"
```

---

## Task 14: Feature/Books — Screen 截图测试（2 个）

**Files:**
- Create: `feature/books/src/test/kotlin/cn/wj/android/cashbook/feature/books/screen/MyBooksScreenScreenshotTests.kt`
- Create: `feature/books/src/test/kotlin/cn/wj/android/cashbook/feature/books/screen/EditBookScreenScreenshotTests.kt`

- [ ] **Step 1: 阅读 Screen 源码和 UiState 定义**

- `MyBooksScreen.kt` (line 102+)
- `EditBookScreen.kt` (line 106+)
- MyBooksUiState (Loading, Success(booksList: List<Selectable<BooksModel>>))
- EditBookUiState (Loading, Success(data: BooksModel))

使用 `createBooksModel()` 构造测试数据。需要阅读 `Selectable` 类型定义。

- [ ] **Step 2: 创建测试类**

- [ ] **Step 3: 运行截图录制验证**

```bash
./gradlew :feature:books:recordRoborazziDebug
```

- [ ] **Step 4: Commit**

```bash
git add feature/books/src/test/
git commit -m "[test|feature/books|UI测试][公共]新增账本模块 2 个 Screen 截图测试"
```

---

## Task 15: Feature/Types — Screen 截图测试（2 个）

**Files:**
- Create: `feature/types/src/test/kotlin/cn/wj/android/cashbook/feature/types/screen/MyCategoriesScreenScreenshotTests.kt`
- Create: `feature/types/src/test/kotlin/cn/wj/android/cashbook/feature/types/screen/EditRecordTypeListScreenScreenshotTests.kt`

- [ ] **Step 1: 阅读 Screen 源码和 UiState 定义**

- `MyCategoriesScreen.kt` (line 128+)
- `EditRecordTypeListScreen.kt` (line 100+)
- MyCategoriesUiState (Loading, Success(selectedTab: RecordTypeCategoryEnum, typeList: List<ExpandableRecordTypeModel>))
- EditRecordTypeListViewModel — 无 sealed UiState，确认参数

使用 `createRecordTypeModel()` 构造测试数据。需阅读 `ExpandableRecordTypeModel` 定义。

- [ ] **Step 2: 创建测试类**

- [ ] **Step 3: 运行截图录制验证**

```bash
./gradlew :feature:types:recordRoborazziDebug
```

- [ ] **Step 4: Commit**

```bash
git add feature/types/src/test/
git commit -m "[test|feature/types|UI测试][公共]新增分类模块 2 个 Screen 截图测试"
```

---

## Task 16: 全量录制与验证

- [ ] **Step 1: 运行 spotlessApply 确保所有新文件格式正确**

```bash
./gradlew spotlessApply --init-script gradle/init.gradle.kts --no-configuration-cache
```

- [ ] **Step 2: 全量录制基准截图**

```bash
./gradlew recordRoborazziDebug
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 全量验证截图**

```bash
./gradlew verifyRoborazziDebug
```

Expected: BUILD SUCCESSFUL（所有截图与基准一致）

- [ ] **Step 4: 检查截图文件数量**

预期每个 `captureMultiTheme` 生成 4 张截图，每个 `captureMultiDevice` 生成 3 张截图。总截图数约为：
- 组件测试：~50 方法 × 4 = ~200 张
- Screen 测试：~80 方法 × (4+3) = ~560 张（multiTheme + multiDevice 分别在不同测试方法中）
- 总计约 ~760 张截图

- [ ] **Step 5: 如果有格式问题，修复并 Commit**

```bash
git add -A
git commit -m "[test|all|UI测试][公共]修复截图测试格式问题并更新基准截图"
```
