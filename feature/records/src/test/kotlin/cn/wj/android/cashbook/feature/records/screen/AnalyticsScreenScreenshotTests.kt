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
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import cn.wj.android.cashbook.core.design.theme.CashbookTheme
import cn.wj.android.cashbook.core.model.entity.AnalyticsRecordBarEntity
import cn.wj.android.cashbook.core.model.entity.AnalyticsRecordPieEntity
import cn.wj.android.cashbook.core.model.entity.DateSelectionEntity
import cn.wj.android.cashbook.core.model.enums.AnalyticsBarGranularity
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.testing.util.captureMultiDevice
import cn.wj.android.cashbook.core.testing.util.captureMultiTheme
import cn.wj.android.cashbook.feature.records.viewmodel.AnalyticsUiState
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
class AnalyticsScreenScreenshotTests {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val successUiState = AnalyticsUiState.Success(
        granularity = AnalyticsBarGranularity.DAY,
        titleText = "2024年1月",
        totalIncome = "5,000.00",
        totalExpenditure = "3,000.00",
        totalBalance = "2,000.00",
        noData = false,
        barDataList = listOf(
            AnalyticsRecordBarEntity(
                date = "01",
                expenditure = 100_00L,
                income = 200_00L,
                balance = 100_00L,
                granularity = AnalyticsBarGranularity.DAY,
            ),
            AnalyticsRecordBarEntity(
                date = "02",
                expenditure = 150_00L,
                income = 50_00L,
                balance = -100_00L,
                granularity = AnalyticsBarGranularity.DAY,
            ),
        ),
        expenditurePieDataList = listOf(
            AnalyticsRecordPieEntity(
                typeId = 1L,
                typeName = "餐饮",
                typeIconResName = "vector_type_food",
                typeCategory = RecordTypeCategoryEnum.EXPENDITURE,
                totalAmount = 200_00L,
                percent = 0.6f,
            ),
            AnalyticsRecordPieEntity(
                typeId = 2L,
                typeName = "交通",
                typeIconResName = "vector_type_traffic",
                typeCategory = RecordTypeCategoryEnum.EXPENDITURE,
                totalAmount = 100_00L,
                percent = 0.4f,
            ),
        ),
        incomePieDataList = listOf(
            AnalyticsRecordPieEntity(
                typeId = 3L,
                typeName = "工资",
                typeIconResName = "vector_type_salary",
                typeCategory = RecordTypeCategoryEnum.INCOME,
                totalAmount = 500_00L,
                percent = 1.0f,
            ),
        ),
        transferPieDataList = emptyList(),
    )

    private val noDataUiState = AnalyticsUiState.Success(
        granularity = AnalyticsBarGranularity.DAY,
        titleText = "2024年1月",
        totalIncome = "0.00",
        totalExpenditure = "0.00",
        totalBalance = "0.00",
        noData = true,
        barDataList = emptyList(),
        expenditurePieDataList = emptyList(),
        incomePieDataList = emptyList(),
        transferPieDataList = emptyList(),
    )

    @Test
    fun analyticsScreen_loading_multipleThemes() {
        composeTestRule.captureMultiTheme(
            name = "AnalyticsScreen",
            overrideFileName = "AnalyticsScreen_loading",
        ) {
            AnalyticsScreen(
                dateSelection = DateSelectionEntity.ByMonth(YearMonth.now()),
                showDatePopup = false,
                onDateClick = {},
                onDateSelected = {},
                onDismissDatePopup = {},
                sheetData = null,
                onRequestShowBottomSheet = {},
                onRequestDismissBottomSheet = {},
                uiState = AnalyticsUiState.Loading,
                onRequestNaviToTypeAnalytics = { _, _ -> },
                onRequestPopBackStack = {},
            )
        }
    }

    @Test
    fun analyticsScreen_success_multipleThemes() {
        composeTestRule.captureMultiTheme(name = "AnalyticsScreen") {
            AnalyticsScreen(
                dateSelection = DateSelectionEntity.ByMonth(YearMonth.now()),
                showDatePopup = false,
                onDateClick = {},
                onDateSelected = {},
                onDismissDatePopup = {},
                sheetData = null,
                onRequestShowBottomSheet = {},
                onRequestDismissBottomSheet = {},
                uiState = successUiState,
                onRequestNaviToTypeAnalytics = { _, _ -> },
                onRequestPopBackStack = {},
            )
        }
    }

    @Test
    fun analyticsScreen_noData_multipleThemes() {
        composeTestRule.captureMultiTheme(
            name = "AnalyticsScreen",
            overrideFileName = "AnalyticsScreen_noData",
        ) {
            AnalyticsScreen(
                dateSelection = DateSelectionEntity.ByMonth(YearMonth.now()),
                showDatePopup = false,
                onDateClick = {},
                onDateSelected = {},
                onDismissDatePopup = {},
                sheetData = null,
                onRequestShowBottomSheet = {},
                onRequestDismissBottomSheet = {},
                uiState = noDataUiState,
                onRequestNaviToTypeAnalytics = { _, _ -> },
                onRequestPopBackStack = {},
            )
        }
    }

    @Test
    fun analyticsScreen_success_multipleDevices() {
        composeTestRule.captureMultiDevice(screenshotName = "AnalyticsScreen") {
            CashbookTheme {
                AnalyticsScreen(
                    dateSelection = DateSelectionEntity.ByMonth(YearMonth.now()),
                    showDatePopup = false,
                    onDateClick = {},
                    onDateSelected = {},
                    onDismissDatePopup = {},
                    sheetData = null,
                    onRequestShowBottomSheet = {},
                    onRequestDismissBottomSheet = {},
                    uiState = successUiState,
                    onRequestNaviToTypeAnalytics = { _, _ -> },
                    onRequestPopBackStack = {},
                )
            }
        }
    }

    @Test
    fun analyticsScreen_loading_multipleDevices() {
        composeTestRule.captureMultiDevice(screenshotName = "AnalyticsScreen_loading") {
            CashbookTheme {
                AnalyticsScreen(
                    dateSelection = DateSelectionEntity.ByMonth(YearMonth.now()),
                    showDatePopup = false,
                    onDateClick = {},
                    onDateSelected = {},
                    onDismissDatePopup = {},
                    sheetData = null,
                    onRequestShowBottomSheet = {},
                    onRequestDismissBottomSheet = {},
                    uiState = AnalyticsUiState.Loading,
                    onRequestNaviToTypeAnalytics = { _, _ -> },
                    onRequestPopBackStack = {},
                )
            }
        }
    }
}
