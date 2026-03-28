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
import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import cn.wj.android.cashbook.core.design.theme.CashbookTheme
import cn.wj.android.cashbook.core.testing.util.captureMultiDevice
import cn.wj.android.cashbook.core.testing.util.captureMultiTheme
import cn.wj.android.cashbook.feature.settings.viewmodel.LauncherUiState
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
class LauncherScreenScreenshotTests {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun launcherScreen_loading_multipleThemes() {
        composeTestRule.captureMultiTheme(
            name = "LauncherScreen",
            overrideFileName = "LauncherScreen_loading",
        ) {
            LauncherScreen(
                shouldDisplayDrawerSheet = false,
                onRequestDisplayDrawerSheet = {},
                onRequestDismissDrawerSheet = {},
                uiState = LauncherUiState.Loading,
                onMyAssetClick = {},
                onMyBookClick = {},
                onMyCategoryClick = {},
                onMyTagClick = {},
                onSettingClick = {},
                onAboutUsClick = {},
                content = { Text(text = "内容区域") },
            )
        }
    }

    @Test
    fun launcherScreen_success_multipleThemes() {
        composeTestRule.captureMultiTheme(name = "LauncherScreen") {
            LauncherScreen(
                shouldDisplayDrawerSheet = false,
                onRequestDisplayDrawerSheet = {},
                onRequestDismissDrawerSheet = {},
                uiState = LauncherUiState.Success(currentBookName = "默认账本"),
                onMyAssetClick = {},
                onMyBookClick = {},
                onMyCategoryClick = {},
                onMyTagClick = {},
                onSettingClick = {},
                onAboutUsClick = {},
                content = { Text(text = "内容区域") },
            )
        }
    }

    @Test
    fun launcherScreen_success_multipleDevices() {
        composeTestRule.captureMultiDevice(screenshotName = "LauncherScreen") {
            CashbookTheme {
                LauncherScreen(
                    shouldDisplayDrawerSheet = false,
                    onRequestDisplayDrawerSheet = {},
                    onRequestDismissDrawerSheet = {},
                    uiState = LauncherUiState.Success(currentBookName = "默认账本"),
                    onMyAssetClick = {},
                    onMyBookClick = {},
                    onMyCategoryClick = {},
                    onMyTagClick = {},
                    onSettingClick = {},
                    onAboutUsClick = {},
                    content = { Text(text = "内容区域") },
                )
            }
        }
    }

    @Test
    fun launcherScreen_loading_multipleDevices() {
        composeTestRule.captureMultiDevice(screenshotName = "LauncherScreen_loading") {
            CashbookTheme {
                LauncherScreen(
                    shouldDisplayDrawerSheet = false,
                    onRequestDisplayDrawerSheet = {},
                    onRequestDismissDrawerSheet = {},
                    uiState = LauncherUiState.Loading,
                    onMyAssetClick = {},
                    onMyBookClick = {},
                    onMyCategoryClick = {},
                    onMyTagClick = {},
                    onSettingClick = {},
                    onAboutUsClick = {},
                    content = { Text(text = "内容区域") },
                )
            }
        }
    }
}
