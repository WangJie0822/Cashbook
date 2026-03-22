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
import cn.wj.android.cashbook.core.model.entity.RecordDayEntity
import cn.wj.android.cashbook.core.model.entity.RecordViewsEntity
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.testing.util.captureMultiDevice
import cn.wj.android.cashbook.core.testing.util.captureMultiTheme
import cn.wj.android.cashbook.core.ui.DialogState
import cn.wj.android.cashbook.feature.records.viewmodel.CalendarUiState
import dagger.hilt.android.testing.HiltTestApplication
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.LooperMode
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = HiltTestApplication::class, qualifiers = "480dpi")
@LooperMode(LooperMode.Mode.PAUSED)
class CalendarScreenScreenshotTests {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val today = LocalDate.of(2024, 1, 15)

    private val successUiState = CalendarUiState.Success(
        monthIncome = "5,000.00",
        monthExpand = "3,000.00",
        monthBalance = "2,000.00",
        schemas = mapOf(
            LocalDate.of(2024, 1, 1) to RecordDayEntity(
                day = 1,
                dayType = 1,
                dayIncome = 500_00L,
                dayExpand = 200_00L,
            ),
            LocalDate.of(2024, 1, 15) to RecordDayEntity(
                day = 15,
                dayType = 0,
                dayIncome = 0L,
                dayExpand = 100_00L,
            ),
        ),
        recordList = listOf(
            RecordViewsEntity(
                id = 1L,
                typeId = 1L,
                typeCategory = RecordTypeCategoryEnum.EXPENDITURE,
                typeName = "餐饮",
                typeIconResName = "vector_type_food",
                assetId = null,
                assetName = null,
                assetIconResId = null,
                relatedAssetId = null,
                relatedAssetName = null,
                relatedAssetIconResId = null,
                amount = 50_00L,
                finalAmount = 50_00L,
                charges = 0L,
                concessions = 0L,
                remark = "午餐",
                reimbursable = false,
                relatedTags = emptyList(),
                relatedImage = emptyList(),
                relatedRecord = emptyList(),
                relatedAmount = 0L,
                recordTime = 1705276800000L,
            ),
        ),
    )

    @Test
    fun calendarScreen_loading_multipleThemes() {
        composeTestRule.captureMultiTheme(
            name = "CalendarScreen",
            overrideFileName = "CalendarScreen_loading",
        ) {
            CalendarScreen(
                shouldDisplayDeleteFailedBookmark = 0,
                onRequestDismissBookmark = {},
                selectedDate = today,
                onDateClick = {},
                uiState = CalendarUiState.Loading,
                dialogState = DialogState.Dismiss,
                onRequestDismissDialog = {},
                onDateSelected = {},
                onRecordItemClick = {},
                recordDetailSheetContent = { _ -> },
                sheetViewData = null,
                onRequestDismissSheet = {},
                onBackClick = {},
                onShowSnackbar = { _, _ -> SnackbarResult.Dismissed },
            )
        }
    }

    @Test
    fun calendarScreen_success_multipleThemes() {
        composeTestRule.captureMultiTheme(name = "CalendarScreen") {
            CalendarScreen(
                shouldDisplayDeleteFailedBookmark = 0,
                onRequestDismissBookmark = {},
                selectedDate = today,
                onDateClick = {},
                uiState = successUiState,
                dialogState = DialogState.Dismiss,
                onRequestDismissDialog = {},
                onDateSelected = {},
                onRecordItemClick = {},
                recordDetailSheetContent = { _ -> },
                sheetViewData = null,
                onRequestDismissSheet = {},
                onBackClick = {},
                onShowSnackbar = { _, _ -> SnackbarResult.Dismissed },
            )
        }
    }

    @Test
    fun calendarScreen_success_multipleDevices() {
        composeTestRule.captureMultiDevice(screenshotName = "CalendarScreen") {
            CashbookTheme {
                CalendarScreen(
                    shouldDisplayDeleteFailedBookmark = 0,
                    onRequestDismissBookmark = {},
                    selectedDate = today,
                    onDateClick = {},
                    uiState = successUiState,
                    dialogState = DialogState.Dismiss,
                    onRequestDismissDialog = {},
                    onDateSelected = {},
                    onRecordItemClick = {},
                    recordDetailSheetContent = { _ -> },
                    sheetViewData = null,
                    onRequestDismissSheet = {},
                    onBackClick = {},
                    onShowSnackbar = { _, _ -> SnackbarResult.Dismissed },
                )
            }
        }
    }

    @Test
    fun calendarScreen_loading_multipleDevices() {
        composeTestRule.captureMultiDevice(screenshotName = "CalendarScreen_loading") {
            CashbookTheme {
                CalendarScreen(
                    shouldDisplayDeleteFailedBookmark = 0,
                    onRequestDismissBookmark = {},
                    selectedDate = today,
                    onDateClick = {},
                    uiState = CalendarUiState.Loading,
                    dialogState = DialogState.Dismiss,
                    onRequestDismissDialog = {},
                    onDateSelected = {},
                    onRecordItemClick = {},
                    recordDetailSheetContent = { _ -> },
                    sheetViewData = null,
                    onRequestDismissSheet = {},
                    onBackClick = {},
                    onShowSnackbar = { _, _ -> SnackbarResult.Dismissed },
                )
            }
        }
    }
}
