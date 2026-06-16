# CbTabTopAppBar 下沉封装（ARCH-1）Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 把"CbTopAppBar + title 内 fillMaxWidth 的 CbTabRow"固化为 core/design 组件 `CbTabTopAppBar`（fillMaxWidth 写死、不暴露 modifier），改 EditRecordScreen / MyCategoriesScreen 两处调用，渲染等价，从 API 层防 BUG-1 同类回归。

**Architecture:** 最小外壳封装——`CbTabTopAppBar` 内部固化 `CbTopAppBar(title = { CbTabRow(Modifier.fillMaxWidth(), …) })`，差异项 `indicatorColor` 必填参数化、tab 由 `tabs` slot 提供（core/design 不承载 tab 语义）。两处调用方从"title 内 if(Success)"等价改写为"外层 if/else（else 走空 title CbTopAppBar）"。等价性由 Roborazzi 截图 0 diff（像素级）守护。

**Tech Stack:** Kotlin, Jetpack Compose, Material3, Roborazzi（Robolectric 截图测试）, Gradle。

**关键背景：** BUG-1（main `20a0e502`）= CbTabRow(fillMaxSize) 置于 CbTopAppBar title → TopAppBar 撑满全屏高 → CbScaffold body 塌陷 0 高 → 内容不渲染。本计划是其结构性防回归。spec：`docs/superpowers/specs/2026-06-16-cbtabtopappbar-arch1-design.md`（经节点1 team-review 四维评审修正）。

**通用约束（每个 Task 适用）：**
- Gradle 命令统一加 `--offline --no-daemon --console=plain`（本机暖缓存，见 CLAUDE.local.md）
- 构建前若距上次 >十几分钟，先查内存（`Get-CimInstance Win32_OperatingSystem` 可用 <1000MB 中止）
- BUILD 结果只信 `grep -E '^BUILD (SUCCESSFUL|FAILED)'`，不信 bash exit code
- 截图测试为 Robolectric，**本机可跑**（无需真机）

---

### Task 1: 补齐 EditRecordScreen 截图基线作 golden（pre-existing 缺失修复）

**背景：** `EditRecordScreenScreenshotTests.kt` 测试代码已入库（`a50d182a`）但基线 PNG 从未 record/提交（git+磁盘双缺）。Task 3 要用 `verifyRoborazziDebug 0 diff` 证 EditRecordTopBar 重构等价，必须先对**当前未改代码**录制 golden 基线。当前 HEAD 已含 BUG-1 修复（fillMaxWidth），渲染正常。

**Files:**
- Create（record 生成）: `feature/records/src/test/screenshots/EditRecordScreen/*.png` + `feature/records/src/test/screenshots/EditRecordScreen*.png`

- [ ] **Step 1: 录制 EditRecordScreen 基线（仅该测试类）**

Run:
```bash
./gradlew :feature:records:recordRoborazziDebug --tests "cn.wj.android.cashbook.feature.records.screen.EditRecordScreenScreenshotTests" --offline --no-daemon --console=plain 2>&1 | grep -E "BUILD (SUCCESSFUL|FAILED)|FAILED|error:"
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 2: 确认生成的基线文件清单**

Run:
```bash
ls feature/records/src/test/screenshots/ | grep -i editrecord; echo "---"; ls feature/records/src/test/screenshots/EditRecordScreen/ 2>/dev/null
```
Expected: 生成 EditRecordScreen 扁平 device 基线（`EditRecordScreen_phone/tablet/foldable.png`、`EditRecordScreen_loading_*`）+ `EditRecordScreen/` 子目录 theme 基线（expenditure/income × dark/light × dynamic/notDynamic）。应有十余张。

- [ ] **Step 3: 视觉核验基线非塌陷（防 BUG-1 残留 / 录到坏基线）**

用 Read 工具查看 `feature/records/src/test/screenshots/EditRecordScreen_phone.png`（device 变体）：必须看到顶部 topbar 有 tab（支出/收入/转账）+ 下方 body 表单区正常排布（金额/备注/类型列表），**非空白塌陷、tab 不浮在屏幕中部**。若塌陷 → 当前代码有问题，停止并排查（不应发生，BUG-1 已修）。

- [ ] **Step 4: 提交 golden 基线**

```bash
git add feature/records/src/test/screenshots/EditRecordScreen feature/records/src/test/screenshots/EditRecordScreen_*.png
git commit -m "$(cat <<'EOF'
[test|feature|records][公共]补齐 EditRecordScreen 截图测试基线（ARCH-1 golden）

EditRecordScreenScreenshotTests（a50d182a 加测试）基线 pre-existing 缺失，此前从未
record/入库。本次对当前 HEAD（BUG-1 已修 fillMaxWidth）录制基线作 golden，供 ARCH-1
改 EditRecordTopBar 后 verify 0 diff 证等价。已 Read 确认非塌陷。

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

### Task 2: 新增 CbTabTopAppBar 组件 + core/design 截图测试

**Files:**
- Modify: `core/design/src/main/kotlin/cn/wj/android/cashbook/core/design/component/TopAppBar.kt`（追加 `CbTabTopAppBar`）
- Create: `core/design/src/test/kotlin/cn/wj/android/cashbook/core/design/CbTabTopAppBarScreenshotTests.kt`
- Create（record 生成）: `core/design/src/test/screenshots/CbTabTopAppBar/*.png`

- [ ] **Step 1: 实现 CbTabTopAppBar 组件**

在 `TopAppBar.kt` 末尾（`CommonTopBarPreview` 之前或之后均可，放第二个 CbTopAppBar 重载之后）追加：

```kotlin
/**
 * 顶部标题栏 + title 槽内 Tab 行的封装。
 *
 * 固化 [CbTopAppBar] 的 title 槽放一个 `Modifier.fillMaxWidth()` 的 [CbTabRow]——
 * fillMaxWidth 写死、不暴露给调用方，从 API 层杜绝 fillMaxSize 误用（BUG-1：fillMaxSize
 * 含 fillMaxHeight 会使 TopAppBar 撑满全屏高、CbScaffold body 塌陷 0 高、内容不渲染）。
 *
 * @param selectedTabIndex 选中 tab 下标
 * @param indicatorColor 指示器颜色（必填，无默认——各调用方用色不同）
 * @param onBackClick 返回事件
 * @param actions 标题菜单
 * @param tabs tab 内容槽，调用方放 [CbTab]
 */
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
    CbTopAppBar(
        onBackClick = onBackClick,
        modifier = modifier,
        actions = actions,
        title = {
            CbTabRow(
                modifier = Modifier.fillMaxWidth(),
                selectedTabIndex = selectedTabIndex,
                containerColor = Color.Unspecified,
                contentColor = Color.Unspecified,
                indicator = { tabPositions ->
                    if (selectedTabIndex < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                            color = indicatorColor,
                        )
                    }
                },
                divider = {},
                tabs = tabs,
            )
        },
    )
}
```

同时在 `TopAppBar.kt` 顶部 import 区补充（若缺）：
```kotlin
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
```
（`ExperimentalMaterial3Api`/`Composable`/`Modifier`/`Color`/`RowScope` 已有；`CbTabRow` 同包无需 import）

- [ ] **Step 2: 编译 core/design 验证组件可编译**

Run:
```bash
./gradlew :core:design:compileDebugKotlin --offline --no-daemon --console=plain 2>&1 | grep -E "BUILD (SUCCESSFUL|FAILED)|error:|e: "
```
Expected: `BUILD SUCCESSFUL`（若报未解析符号，按错误补 import）

- [ ] **Step 3: 写截图测试**

Create `core/design/src/test/kotlin/cn/wj/android/cashbook/core/design/CbTabTopAppBarScreenshotTests.kt`：

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

package cn.wj.android.cashbook.core.design

import androidx.activity.ComponentActivity
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import cn.wj.android.cashbook.core.design.component.CbTab
import cn.wj.android.cashbook.core.design.component.CbTabTopAppBar
import cn.wj.android.cashbook.core.testing.util.captureMultiTheme
import dagger.hilt.android.testing.HiltTestApplication
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = HiltTestApplication::class, qualifiers = "480dpi")
@LooperMode(LooperMode.Mode.PAUSED)
@OptIn(ExperimentalMaterial3Api::class)
class CbTabTopAppBarScreenshotTests {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun cbTabTopAppBar_multipleThemes() {
        composeTestRule.captureMultiTheme(name = "CbTabTopAppBar") {
            CbTabTopAppBar(
                selectedTabIndex = 0,
                indicatorColor = MaterialTheme.colorScheme.primary,
                onBackClick = {},
            ) {
                listOf("支出", "收入", "转账").forEachIndexed { index, text ->
                    CbTab(
                        selected = index == 0,
                        onClick = {},
                        text = { Text(text = text) },
                    )
                }
            }
        }
    }
}
```

- [ ] **Step 4: 录制新组件基线**

Run:
```bash
./gradlew :core:design:recordRoborazziDebug --tests "cn.wj.android.cashbook.core.design.CbTabTopAppBarScreenshotTests" --offline --no-daemon --console=plain 2>&1 | grep -E "BUILD (SUCCESSFUL|FAILED)|FAILED|error:"
```
Expected: `BUILD SUCCESSFUL`，生成 `core/design/src/test/screenshots/CbTabTopAppBar/CbTabTopAppBar_{dark,light}_defaultTheme_{dynamic,notDynamic}.png`（4 张）

- [ ] **Step 5: 视觉核验新组件基线（防退化为空壳 topbar）**

用 Read 查看 `core/design/src/test/screenshots/CbTabTopAppBar/CbTabTopAppBar_light_defaultTheme_notDynamic.png`：必须看到一行 3 个 tab（支出/收入/转账）+ 第 0 个下方有 indicator 下划线 + 左侧返回箭头。**若只有空 topbar 无 tab → 测试构造有误，修正重录。**

- [ ] **Step 6: verify 自洽 + spotless**

Run:
```bash
./gradlew :core:design:verifyRoborazziDebug --tests "cn.wj.android.cashbook.core.design.CbTabTopAppBarScreenshotTests" --offline --no-daemon --console=plain 2>&1 | grep -E "BUILD (SUCCESSFUL|FAILED)"
./gradlew :core:design:spotlessApply --init-script gradle/init.gradle.kts --no-configuration-cache --offline --no-daemon --console=plain 2>&1 | grep -E "BUILD (SUCCESSFUL|FAILED)"
```
Expected: 两条均 `BUILD SUCCESSFUL`

- [ ] **Step 7: 提交**

```bash
git add core/design/src/main/kotlin/cn/wj/android/cashbook/core/design/component/TopAppBar.kt core/design/src/test/kotlin/cn/wj/android/cashbook/core/design/CbTabTopAppBarScreenshotTests.kt core/design/src/test/screenshots/CbTabTopAppBar
git commit -m "$(cat <<'EOF'
[feat|core|design][公共]新增 CbTabTopAppBar——固化 topbar title 内 fillMaxWidth 的 CbTabRow

最小外壳封装 CbTopAppBar + title 内 Modifier.fillMaxWidth() 的 CbTabRow，fillMaxWidth
写死不暴露 modifier，从 API 层防 BUG-1（fillMaxSize 致 Scaffold body 塌陷）。indicatorColor
必填、tab 由 slot 提供。附截图测试 + 基线（含真实 3 tab + 选中 indicator）。

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

### Task 3: EditRecordScreen 接入 CbTabTopAppBar

**Files:**
- Modify: `feature/records/src/main/kotlin/cn/wj/android/cashbook/feature/records/screen/EditRecordScreen.kt`（`EditRecordTopBar`，1025-1061）

- [ ] **Step 1: 改写 EditRecordTopBar**

将 `EditRecordTopBar`（当前 1025-1061）函数体替换为：

```kotlin
internal fun EditRecordTopBar(
    uiState: EditRecordUiState,
    selectedTab: RecordTypeCategoryEnum,
    onTabSelected: (RecordTypeCategoryEnum) -> Unit,
    onBackClick: () -> Unit,
) {
    if (uiState is EditRecordUiState.Success) {
        CbTabTopAppBar(
            selectedTabIndex = selectedTab.ordinal,
            indicatorColor = selectedTab.typeColor,
            onBackClick = onBackClick,
        ) {
            RecordTypeCategoryEnum.entries.forEach { enum ->
                CbTab(
                    selected = selectedTab == enum,
                    onClick = { onTabSelected(enum) },
                    text = { Text(text = enum.text) },
                    selectedContentColor = selectedTab.typeColor,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    } else {
        CbTopAppBar(title = {}, onBackClick = onBackClick)
    }
}
```

import 调整：
- 增 `import cn.wj.android.cashbook.core.design.component.CbTabTopAppBar`
- `CbTopAppBar`/`CbTab`/`Text`/`MaterialTheme` 已有
- `CbTabRow`、`TabRowDefaults`、`tabIndicatorOffset`、`Color`（若 `Color.Unspecified` 仅此处用）若变成未使用，由 Step 3 spotlessApply 自动移除；`fillMaxWidth` 在本文件他处仍用，保留

- [ ] **Step 2: 编译 feature:records**

Run:
```bash
./gradlew :feature:records:compileDebugKotlin --offline --no-daemon --console=plain 2>&1 | grep -E "BUILD (SUCCESSFUL|FAILED)|error:|e: "
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: spotlessApply（自动清理 unused import）**

Run:
```bash
./gradlew :feature:records:spotlessApply --init-script gradle/init.gradle.kts --no-configuration-cache --offline --no-daemon --console=plain 2>&1 | grep -E "BUILD (SUCCESSFUL|FAILED)"
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: verify 截图 0 diff（等价性硬证据，对 Task 1 golden）**

Run:
```bash
./gradlew :feature:records:verifyRoborazziDebug --tests "cn.wj.android.cashbook.feature.records.screen.EditRecordScreenScreenshotTests" --offline --no-daemon --console=plain 2>&1 | grep -E "BUILD (SUCCESSFUL|FAILED)|Roborazzi|diff"
```
Expected: `BUILD SUCCESSFUL`（0 diff）。**若 FAILED 有 diff** → 重构非等价，查 `feature/records/build/outputs/roborazzi/*_compare.png` 定位差异，修正 EditRecordTopBar（多半是颜色/参数抄错）直到 0 diff。

- [ ] **Step 5: 跑 BUG-1 回归测试（守 body 不塌陷）**

Run:
```bash
./gradlew :feature:records:testDebugUnitTest --tests "cn.wj.android.cashbook.feature.records.screen.EditRecordScreenRenderRegressionTest" --offline --no-daemon --console=plain 2>&1 | grep -E "BUILD (SUCCESSFUL|FAILED)|FAILED"
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 6: 提交**

```bash
git add feature/records/src/main/kotlin/cn/wj/android/cashbook/feature/records/screen/EditRecordScreen.kt
git commit -m "$(cat <<'EOF'
[refactor|feature|records][公共]EditRecordTopBar 接入 CbTabTopAppBar

把 title 内手写 fillMaxWidth CbTabRow 替换为 CbTabTopAppBar（fillMaxWidth 封装内写死），
非 Success 态等价改走 else 空 title CbTopAppBar。indicatorColor=selectedTab.typeColor。
verifyRoborazzi 0 diff 证渲染等价 + RenderRegressionTest 守 body 不塌陷。

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

### Task 4: MyCategoriesScreen 接入 CbTabTopAppBar

**Files:**
- Modify: `feature/types/src/main/kotlin/cn/wj/android/cashbook/feature/types/screen/MyCategoriesScreen.kt`（`MyCategoriesTopBar`，904-942）

- [ ] **Step 1: 改写 MyCategoriesTopBar**

将 `MyCategoriesTopBar`（当前 906-942，保留其上方 `@OptIn(ExperimentalMaterial3Api::class)` 与 `@Composable`）函数体替换为：

```kotlin
internal fun MyCategoriesTopBar(
    uiState: MyCategoriesUiState,
    onTabSelected: (RecordTypeCategoryEnum) -> Unit,
    onBackClick: () -> Unit,
) {
    if (uiState is MyCategoriesUiState.Success) {
        val selectedTab = uiState.selectedTab
        CbTabTopAppBar(
            selectedTabIndex = selectedTab.ordinal,
            indicatorColor = MaterialTheme.colorScheme.onTertiaryContainer,
            onBackClick = onBackClick,
        ) {
            RecordTypeCategoryEnum.entries.forEach { enum ->
                CbTab(
                    selected = selectedTab == enum,
                    onClick = { onTabSelected(enum) },
                    text = { Text(text = enum.text) },
                    selectedContentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    unselectedContentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
        }
    } else {
        CbTopAppBar(title = {}, onBackClick = onBackClick)
    }
}
```

import 调整：增 `import cn.wj.android.cashbook.core.design.component.CbTabTopAppBar`；`CbTabRow`/`TabRowDefaults`/`tabIndicatorOffset`/`Color` 若变未使用由 Step 3 spotlessApply 移除。

- [ ] **Step 2: 编译 feature:types**

Run:
```bash
./gradlew :feature:types:compileDebugKotlin --offline --no-daemon --console=plain 2>&1 | grep -E "BUILD (SUCCESSFUL|FAILED)|error:|e: "
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: spotlessApply**

Run:
```bash
./gradlew :feature:types:spotlessApply --init-script gradle/init.gradle.kts --no-configuration-cache --offline --no-daemon --console=plain 2>&1 | grep -E "BUILD (SUCCESSFUL|FAILED)"
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: verify 截图 0 diff（对已入库 240268ef 基线）**

Run:
```bash
./gradlew :feature:types:verifyRoborazziDebug --tests "cn.wj.android.cashbook.feature.types.screen.MyCategoriesScreenScreenshotTests" --offline --no-daemon --console=plain 2>&1 | grep -E "BUILD (SUCCESSFUL|FAILED)|Roborazzi|diff"
```
Expected: `BUILD SUCCESSFUL`（0 diff，含 Success/Loading/empty 三态——Loading 态验证 else 分支等价）。FAILED 处理同 Task 3 Step 4。

- [ ] **Step 5: 跑 BUG-1 回归测试**

Run:
```bash
./gradlew :feature:types:testDebugUnitTest --tests "cn.wj.android.cashbook.feature.types.screen.MyCategoriesScreenRenderRegressionTest" --offline --no-daemon --console=plain 2>&1 | grep -E "BUILD (SUCCESSFUL|FAILED)|FAILED"
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 6: 提交**

```bash
git add feature/types/src/main/kotlin/cn/wj/android/cashbook/feature/types/screen/MyCategoriesScreen.kt
git commit -m "$(cat <<'EOF'
[refactor|feature|types][公共]MyCategoriesTopBar 接入 CbTabTopAppBar

把 title 内手写 fillMaxWidth CbTabRow 替换为 CbTabTopAppBar，非 Success 态等价改走 else
空 title CbTopAppBar。indicatorColor=onTertiaryContainer。verifyRoborazzi 0 diff（三态
含 Loading）证等价 + RenderRegressionTest 守 body 不塌陷。

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

### 完成后整体验收

- [ ] `:core:design:verifyRoborazziDebug`（CbTabTopAppBar）、`:feature:records:verifyRoborazziDebug`（EditRecordScreen）、`:feature:types:verifyRoborazziDebug`（MyCategoriesScreen）全 0 diff
- [ ] 两个 RenderRegressionTest 绿
- [ ] 4 笔 commit 原子、git status 干净
- [ ] 节点2 `comprehensive-review:full-review`（或降级两维快审，本次纯 UI 重构 + 截图等价，可两维）

**遗留 backlog（不在本计划）：** feature/records 其余 Screen 截图测试（`a50d182a` 8 个）基线同样 pre-existing 缺失，需单独立项补齐。
