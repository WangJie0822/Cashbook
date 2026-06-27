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

package cn.wj.android.cashbook.feature.settings.screen

import androidx.activity.ComponentActivity
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import cn.wj.android.cashbook.core.design.theme.CashbookTheme
import cn.wj.android.cashbook.feature.settings.navigation.LauncherDrawerActions
import cn.wj.android.cashbook.feature.settings.viewmodel.LauncherUiState
import dagger.hilt.android.testing.HiltTestApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * [LauncherScreen] 抽屉返回键交互回归测试。
 *
 * 守护「抽屉显示时按返回收起抽屉」契约，直击手势/滑动打开导致的双真源 desync：
 * 当 drawerState 已 Open 而 shouldDisplayDrawerSheet 仍为 false 时（手势打开路径），
 * 按系统返回必须真正关闭抽屉（修复前 onRequestDismissDrawerSheet 把 false 设 false、
 * LaunchedEffect 不重触发、抽屉关不掉 → 本测试红）。
 *
 * 本机 Robolectric 需 android-all-instrumented 缓存；缺失则随 CI 运行（与同模块截图测试一致）。
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = HiltTestApplication::class, qualifiers = "480dpi")
class LauncherScreenBackHandlerTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun noopActions() = LauncherDrawerActions(
        onMyAssetClick = {},
        onMyBookClick = {},
        onMyCategoryClick = {},
        onMyTagClick = {},
        onReimbursementClick = {},
        onBudgetClick = {},
        onSettingClick = {},
        onAboutUsClick = {},
    )

    @Test
    fun back_closesDrawer_evenWhenOpenedByGestureWithIntentStateFalse() {
        var dismissCount = 0
        lateinit var drawerState: DrawerState
        lateinit var scope: CoroutineScope
        composeTestRule.setContent {
            drawerState = rememberDrawerState(DrawerValue.Closed)
            scope = rememberCoroutineScope()
            CashbookTheme {
                LauncherScreen(
                    shouldDisplayDrawerSheet = false,
                    onRequestDisplayDrawerSheet = {},
                    onRequestDismissDrawerSheet = { dismissCount++ },
                    uiState = LauncherUiState.Success(currentBookName = "默认账本"),
                    actions = noopActions(),
                    content = {},
                    drawerState = drawerState,
                )
            }
        }

        // 模拟手势/滑动打开抽屉：drawerState 变 Open，但 shouldDisplayDrawerSheet 仍为 false（desync 态）。
        scope.launch { drawerState.open() }
        composeTestRule.waitForIdle()
        assertTrue("前置条件：抽屉应已打开", drawerState.isOpen)

        // 按系统返回键
        composeTestRule.runOnUiThread {
            composeTestRule.activity.onBackPressedDispatcher.onBackPressed()
        }
        composeTestRule.waitForIdle()

        // 修复后：抽屉真正收起 + 意图源回写（dismiss 回调触发）
        assertTrue("返回应触发 dismiss 回调（VM 意图源同步）", dismissCount >= 1)
        assertEquals("返回后抽屉应收起", DrawerValue.Closed, drawerState.currentValue)
    }
}
