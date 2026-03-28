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
import cn.wj.android.cashbook.core.model.entity.RecordViewsEntity
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.testing.util.captureMultiDevice
import cn.wj.android.cashbook.core.testing.util.captureMultiTheme
import cn.wj.android.cashbook.feature.records.viewmodel.SelectRelatedRecordUiState
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
class SelectRelatedRecordScreenScreenshotTests {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val sampleRecord = RecordViewsEntity(
        id = 1L,
        typeId = 1L,
        typeCategory = RecordTypeCategoryEnum.EXPENDITURE,
        typeName = "餐饮",
        typeIconResName = "vector_type_three_meals_24",
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
    )

    private val successWithRecords = SelectRelatedRecordUiState.Success(
        relatedRecordList = listOf(sampleRecord),
        recordList = listOf(
            sampleRecord.copy(id = 2L, typeName = "交通", amount = 30_00L, finalAmount = 30_00L),
        ),
    )

    private val successEmpty = SelectRelatedRecordUiState.Success(
        relatedRecordList = emptyList(),
        recordList = listOf(sampleRecord),
    )

    @Test
    fun selectRelatedRecordScreen_loading_multipleThemes() {
        composeTestRule.captureMultiTheme(
            name = "SelectRelatedRecordScreen",
            overrideFileName = "SelectRelatedRecordScreen_loading",
        ) {
            SelectRelatedRecordScreen(
                uiState = SelectRelatedRecordUiState.Loading,
                onKeywordChanged = {},
                onRelatedRecordItemClick = {},
                onRecordItemClick = {},
                onRequestSaveRelatedRecord = {},
                onRequestPopBackStack = {},
            )
        }
    }

    @Test
    fun selectRelatedRecordScreen_success_multipleThemes() {
        composeTestRule.captureMultiTheme(name = "SelectRelatedRecordScreen") {
            SelectRelatedRecordScreen(
                uiState = successWithRecords,
                onKeywordChanged = {},
                onRelatedRecordItemClick = {},
                onRecordItemClick = {},
                onRequestSaveRelatedRecord = {},
                onRequestPopBackStack = {},
            )
        }
    }

    @Test
    fun selectRelatedRecordScreen_empty_multipleThemes() {
        composeTestRule.captureMultiTheme(
            name = "SelectRelatedRecordScreen",
            overrideFileName = "SelectRelatedRecordScreen_empty",
        ) {
            SelectRelatedRecordScreen(
                uiState = successEmpty,
                onKeywordChanged = {},
                onRelatedRecordItemClick = {},
                onRecordItemClick = {},
                onRequestSaveRelatedRecord = {},
                onRequestPopBackStack = {},
            )
        }
    }

    @Test
    fun selectRelatedRecordScreen_success_multipleDevices() {
        composeTestRule.captureMultiDevice(screenshotName = "SelectRelatedRecordScreen") {
            CashbookTheme {
                SelectRelatedRecordScreen(
                    uiState = successWithRecords,
                    onKeywordChanged = {},
                    onRelatedRecordItemClick = {},
                    onRecordItemClick = {},
                    onRequestSaveRelatedRecord = {},
                    onRequestPopBackStack = {},
                )
            }
        }
    }

    @Test
    fun selectRelatedRecordScreen_loading_multipleDevices() {
        composeTestRule.captureMultiDevice(
            screenshotName = "SelectRelatedRecordScreen_loading",
        ) {
            CashbookTheme {
                SelectRelatedRecordScreen(
                    uiState = SelectRelatedRecordUiState.Loading,
                    onKeywordChanged = {},
                    onRelatedRecordItemClick = {},
                    onRecordItemClick = {},
                    onRequestSaveRelatedRecord = {},
                    onRequestPopBackStack = {},
                )
            }
        }
    }
}
