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

package cn.wj.android.cashbook.feature.books.screen

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import cn.wj.android.cashbook.core.design.theme.CashbookTheme
import cn.wj.android.cashbook.core.testing.data.createBooksModel
import cn.wj.android.cashbook.core.testing.util.captureMultiDevice
import cn.wj.android.cashbook.core.testing.util.captureMultiTheme
import cn.wj.android.cashbook.feature.books.enums.EditBookBookmarkEnum
import cn.wj.android.cashbook.feature.books.viewmodel.EditBookUiState
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
class EditBookScreenScreenshotTests {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val successUiState = EditBookUiState.Success(
        data = createBooksModel(id = 1L, name = "测试账本", description = "账本描述"),
    )

    @Test
    fun editBookScreen_loading_multipleThemes() {
        composeTestRule.captureMultiTheme(
            name = "EditBookScreen",
            overrideFileName = "EditBookScreen_loading",
        ) {
            EditBookScreen(
                shouldDisplayBookmark = EditBookBookmarkEnum.NONE,
                onDismissBookmark = {},
                uiState = EditBookUiState.Loading,
                onSaveClick = { _, _, _ -> },
                onBackClick = {},
            )
        }
    }

    @Test
    fun editBookScreen_success_multipleThemes() {
        composeTestRule.captureMultiTheme(name = "EditBookScreen") {
            EditBookScreen(
                shouldDisplayBookmark = EditBookBookmarkEnum.NONE,
                onDismissBookmark = {},
                uiState = successUiState,
                onSaveClick = { _, _, _ -> },
                onBackClick = {},
            )
        }
    }

    @Test
    fun editBookScreen_success_multipleDevices() {
        composeTestRule.captureMultiDevice(screenshotName = "EditBookScreen") {
            CashbookTheme {
                EditBookScreen(
                    shouldDisplayBookmark = EditBookBookmarkEnum.NONE,
                    onDismissBookmark = {},
                    uiState = successUiState,
                    onSaveClick = { _, _, _ -> },
                    onBackClick = {},
                )
            }
        }
    }

    @Test
    fun editBookScreen_loading_multipleDevices() {
        composeTestRule.captureMultiDevice(screenshotName = "EditBookScreen_loading") {
            CashbookTheme {
                EditBookScreen(
                    shouldDisplayBookmark = EditBookBookmarkEnum.NONE,
                    onDismissBookmark = {},
                    uiState = EditBookUiState.Loading,
                    onSaveClick = { _, _, _ -> },
                    onBackClick = {},
                )
            }
        }
    }
}
