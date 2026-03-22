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
import cn.wj.android.cashbook.core.model.model.Selectable
import cn.wj.android.cashbook.core.testing.data.createBooksModel
import cn.wj.android.cashbook.core.testing.util.captureMultiDevice
import cn.wj.android.cashbook.core.testing.util.captureMultiTheme
import cn.wj.android.cashbook.core.ui.DialogState
import cn.wj.android.cashbook.feature.books.viewmodel.MyBooksUiState
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
class MyBooksScreenScreenshotTests {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val booksList = listOf(
        Selectable(
            createBooksModel(id = 1L, name = "默认账本", description = "日常记账"),
            selected = true,
        ),
        Selectable(
            createBooksModel(id = 2L, name = "旅行账本", description = ""),
            selected = false,
        ),
    )

    @Test
    fun myBooksScreen_loading_multipleThemes() {
        composeTestRule.captureMultiTheme(
            name = "MyBooksScreen",
            overrideFileName = "MyBooksScreen_loading",
        ) {
            MyBooksScreen(
                uiState = MyBooksUiState.Loading,
                onBookSelected = {},
                onEditBookClick = {},
                onDeleteBookClick = {},
                dialogState = DialogState.Dismiss,
                onConfirmDelete = {},
                onDismissDialog = {},
                onBackClick = {},
            )
        }
    }

    @Test
    fun myBooksScreen_withBooks_multipleThemes() {
        composeTestRule.captureMultiTheme(name = "MyBooksScreen") {
            MyBooksScreen(
                uiState = MyBooksUiState.Success(booksList),
                onBookSelected = {},
                onEditBookClick = {},
                onDeleteBookClick = {},
                dialogState = DialogState.Dismiss,
                onConfirmDelete = {},
                onDismissDialog = {},
                onBackClick = {},
            )
        }
    }

    @Test
    fun myBooksScreen_withBooks_multipleDevices() {
        composeTestRule.captureMultiDevice(screenshotName = "MyBooksScreen") {
            CashbookTheme {
                MyBooksScreen(
                    uiState = MyBooksUiState.Success(booksList),
                    onBookSelected = {},
                    onEditBookClick = {},
                    onDeleteBookClick = {},
                    dialogState = DialogState.Dismiss,
                    onConfirmDelete = {},
                    onDismissDialog = {},
                    onBackClick = {},
                )
            }
        }
    }

    @Test
    fun myBooksScreen_loading_multipleDevices() {
        composeTestRule.captureMultiDevice(screenshotName = "MyBooksScreen_loading") {
            CashbookTheme {
                MyBooksScreen(
                    uiState = MyBooksUiState.Loading,
                    onBookSelected = {},
                    onEditBookClick = {},
                    onDeleteBookClick = {},
                    dialogState = DialogState.Dismiss,
                    onConfirmDelete = {},
                    onDismissDialog = {},
                    onBackClick = {},
                )
            }
        }
    }
}
