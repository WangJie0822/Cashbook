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

package cn.wj.android.cashbook.feature.records.screen

import androidx.activity.ComponentActivity
import androidx.compose.material3.SnackbarResult
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import cn.wj.android.cashbook.core.design.theme.CashbookTheme
import cn.wj.android.cashbook.core.model.entity.DateSelectionEntity
import cn.wj.android.cashbook.core.testing.util.captureMultiDevice
import cn.wj.android.cashbook.core.testing.util.captureMultiTheme
import cn.wj.android.cashbook.feature.records.viewmodel.LauncherContentUiState
import dagger.hilt.android.testing.HiltTestApplication
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.LooperMode
import java.time.YearMonth

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = HiltTestApplication::class, qualifiers = "480dpi")
@LooperMode(LooperMode.Mode.PAUSED)
class LauncherContentScreenScreenshotTests {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val successUiState = LauncherContentUiState.Success(
        topBgUri = "",
        totalIncome = "3,000.00",
        totalExpand = "2,000.00",
        totalBalance = "1,000.00",
    )

    @Test
    fun launcherContentScreen_loading_multipleThemes() {
        composeTestRule.captureMultiTheme(
            name = "LauncherContentScreen",
            overrideFileName = "LauncherContentScreen_loading",
        ) {
            LauncherContentScreen(
                shouldDisplayDeleteFailedBookmark = 0,
                onRequestDismissBookmark = {},
                viewRecord = null,
                recordDetailSheetContent = {},
                dateSelection = DateSelectionEntity.ByMonth(YearMonth.now()),
                showDatePopup = false,
                onMenuClick = {},
                onDateClick = {},
                onDateSelected = {},
                onDismissDatePopup = {},
                onSearchClick = {},
                onCalendarClick = {},
                onAnalyticsClick = {},
                onAddClick = {},
                uiState = LauncherContentUiState.Loading,
                onRecordItemClick = {},
                onRequestDismissSheet = {},
                onShowSnackbar = { _, _ -> SnackbarResult.Dismissed },
            )
        }
    }

    @Test
    fun launcherContentScreen_success_multipleThemes() {
        composeTestRule.captureMultiTheme(name = "LauncherContentScreen") {
            LauncherContentScreen(
                shouldDisplayDeleteFailedBookmark = 0,
                onRequestDismissBookmark = {},
                viewRecord = null,
                recordDetailSheetContent = {},
                dateSelection = DateSelectionEntity.ByMonth(YearMonth.now()),
                showDatePopup = false,
                onMenuClick = {},
                onDateClick = {},
                onDateSelected = {},
                onDismissDatePopup = {},
                onSearchClick = {},
                onCalendarClick = {},
                onAnalyticsClick = {},
                onAddClick = {},
                uiState = successUiState,
                onRecordItemClick = {},
                onRequestDismissSheet = {},
                onShowSnackbar = { _, _ -> SnackbarResult.Dismissed },
            )
        }
    }

    @Test
    fun launcherContentScreen_success_multipleDevices() {
        composeTestRule.captureMultiDevice(screenshotName = "LauncherContentScreen") {
            CashbookTheme {
                LauncherContentScreen(
                    shouldDisplayDeleteFailedBookmark = 0,
                    onRequestDismissBookmark = {},
                    viewRecord = null,
                    recordDetailSheetContent = {},
                    dateSelection = DateSelectionEntity.ByMonth(YearMonth.now()),
                    showDatePopup = false,
                    onMenuClick = {},
                    onDateClick = {},
                    onDateSelected = {},
                    onDismissDatePopup = {},
                    onSearchClick = {},
                    onCalendarClick = {},
                    onAnalyticsClick = {},
                    onAddClick = {},
                    uiState = successUiState,
                    onRecordItemClick = {},
                    onRequestDismissSheet = {},
                    onShowSnackbar = { _, _ -> SnackbarResult.Dismissed },
                )
            }
        }
    }

    @Test
    fun launcherContentScreen_loading_multipleDevices() {
        composeTestRule.captureMultiDevice(screenshotName = "LauncherContentScreen_loading") {
            CashbookTheme {
                LauncherContentScreen(
                    shouldDisplayDeleteFailedBookmark = 0,
                    onRequestDismissBookmark = {},
                    viewRecord = null,
                    recordDetailSheetContent = {},
                    dateSelection = DateSelectionEntity.ByMonth(YearMonth.now()),
                    showDatePopup = false,
                    onMenuClick = {},
                    onDateClick = {},
                    onDateSelected = {},
                    onDismissDatePopup = {},
                    onSearchClick = {},
                    onCalendarClick = {},
                    onAnalyticsClick = {},
                    onAddClick = {},
                    uiState = LauncherContentUiState.Loading,
                    onRecordItemClick = {},
                    onRequestDismissSheet = {},
                    onShowSnackbar = { _, _ -> SnackbarResult.Dismissed },
                )
            }
        }
    }
}
