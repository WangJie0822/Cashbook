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
import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import cn.wj.android.cashbook.core.design.theme.CashbookTheme
import cn.wj.android.cashbook.core.model.enums.ImageQualityEnum
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.testing.util.captureMultiDevice
import cn.wj.android.cashbook.core.testing.util.captureMultiTheme
import cn.wj.android.cashbook.core.ui.DialogState
import cn.wj.android.cashbook.feature.records.enums.EditRecordBookmarkEnum
import cn.wj.android.cashbook.feature.records.enums.EditRecordBottomSheetEnum
import cn.wj.android.cashbook.feature.records.viewmodel.EditRecordUiState
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
class EditRecordScreenScreenshotTests {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val expenditureUiState = EditRecordUiState.Success(
        amountText = "100.00",
        chargesText = "",
        concessionsText = "",
        remarkText = "午餐",
        selectedAssetId = -1L,
        assetText = "现金",
        relatedAssetText = "",
        dateTimeText = "2024-01-01 12:00",
        reimbursable = false,
        selectedTypeId = 1L,
        needRelated = false,
        relatedCount = 0,
        relatedAmount = "",
        imageQuality = ImageQualityEnum.ORIGINAL,
    )

    private val incomeUiState = EditRecordUiState.Success(
        amountText = "5,000.00",
        chargesText = "",
        concessionsText = "",
        remarkText = "工资",
        selectedAssetId = -1L,
        assetText = "银行卡",
        relatedAssetText = "",
        dateTimeText = "2024-01-15 10:00",
        reimbursable = false,
        selectedTypeId = 2L,
        needRelated = false,
        relatedCount = 0,
        relatedAmount = "",
        imageQuality = ImageQualityEnum.ORIGINAL,
    )

    @Test
    fun editRecordScreen_loading_multipleThemes() {
        composeTestRule.captureMultiTheme(
            name = "EditRecordScreen",
            overrideFileName = "EditRecordScreen_loading",
        ) {
            EditRecordScreen(
                uiState = EditRecordUiState.Loading,
                shouldDisplayBookmark = EditRecordBookmarkEnum.NONE,
                onRequestDismissBookmark = {},
                dialogState = DialogState.Dismiss,
                onRequestDismissDialog = {},
                onRecordTimeClick = {},
                onDateSelected = {},
                onTimeSelected = {},
                selectedTypeCategory = RecordTypeCategoryEnum.EXPENDITURE,
                onTypeCategorySelect = {},
                bottomSheetType = EditRecordBottomSheetEnum.NONE,
                onRequestDismissBottomSheet = {},
                onAmountClick = {},
                onAmountChange = {},
                onChargesClick = {},
                onChargesChange = {},
                onConcessionsClick = {},
                onRelatedRecordClick = {},
                onConcessionsChange = {},
                onImageItemClick = { _, _ -> },
                onImageListSave = {},
                typeListContent = {},
                onRemarkChange = {},
                onAssetClick = {},
                onRelatedAssetClick = {},
                selectAssetBottomSheetContent = {},
                selectRelatedAssetBottomSheetContent = {},
                tagText = "",
                imageList = emptyList(),
                onTagClick = {},
                onImageClick = {},
                selectTagBottomSheetContent = {},
                onReimbursableClick = {},
                onSaveClick = {},
                onBackClick = {},
            )
        }
    }

    @Test
    fun editRecordScreen_expenditure_multipleThemes() {
        composeTestRule.captureMultiTheme(
            name = "EditRecordScreen",
            overrideFileName = "EditRecordScreen_expenditure",
        ) {
            EditRecordScreen(
                uiState = expenditureUiState,
                shouldDisplayBookmark = EditRecordBookmarkEnum.NONE,
                onRequestDismissBookmark = {},
                dialogState = DialogState.Dismiss,
                onRequestDismissDialog = {},
                onRecordTimeClick = {},
                onDateSelected = {},
                onTimeSelected = {},
                selectedTypeCategory = RecordTypeCategoryEnum.EXPENDITURE,
                onTypeCategorySelect = {},
                bottomSheetType = EditRecordBottomSheetEnum.NONE,
                onRequestDismissBottomSheet = {},
                onAmountClick = {},
                onAmountChange = {},
                onChargesClick = {},
                onChargesChange = {},
                onConcessionsClick = {},
                onRelatedRecordClick = {},
                onConcessionsChange = {},
                onImageItemClick = { _, _ -> },
                onImageListSave = {},
                typeListContent = { Text(text = "类型列表") },
                onRemarkChange = {},
                onAssetClick = {},
                onRelatedAssetClick = {},
                selectAssetBottomSheetContent = {},
                selectRelatedAssetBottomSheetContent = {},
                tagText = "",
                imageList = emptyList(),
                onTagClick = {},
                onImageClick = {},
                selectTagBottomSheetContent = {},
                onReimbursableClick = {},
                onSaveClick = {},
                onBackClick = {},
            )
        }
    }

    @Test
    fun editRecordScreen_income_multipleThemes() {
        composeTestRule.captureMultiTheme(
            name = "EditRecordScreen",
            overrideFileName = "EditRecordScreen_income",
        ) {
            EditRecordScreen(
                uiState = incomeUiState,
                shouldDisplayBookmark = EditRecordBookmarkEnum.NONE,
                onRequestDismissBookmark = {},
                dialogState = DialogState.Dismiss,
                onRequestDismissDialog = {},
                onRecordTimeClick = {},
                onDateSelected = {},
                onTimeSelected = {},
                selectedTypeCategory = RecordTypeCategoryEnum.INCOME,
                onTypeCategorySelect = {},
                bottomSheetType = EditRecordBottomSheetEnum.NONE,
                onRequestDismissBottomSheet = {},
                onAmountClick = {},
                onAmountChange = {},
                onChargesClick = {},
                onChargesChange = {},
                onConcessionsClick = {},
                onRelatedRecordClick = {},
                onConcessionsChange = {},
                onImageItemClick = { _, _ -> },
                onImageListSave = {},
                typeListContent = { Text(text = "类型列表") },
                onRemarkChange = {},
                onAssetClick = {},
                onRelatedAssetClick = {},
                selectAssetBottomSheetContent = {},
                selectRelatedAssetBottomSheetContent = {},
                tagText = "日常",
                imageList = emptyList(),
                onTagClick = {},
                onImageClick = {},
                selectTagBottomSheetContent = {},
                onReimbursableClick = {},
                onSaveClick = {},
                onBackClick = {},
            )
        }
    }

    @Test
    fun editRecordScreen_expenditure_multipleDevices() {
        composeTestRule.captureMultiDevice(screenshotName = "EditRecordScreen") {
            CashbookTheme {
                EditRecordScreen(
                    uiState = expenditureUiState,
                    shouldDisplayBookmark = EditRecordBookmarkEnum.NONE,
                    onRequestDismissBookmark = {},
                    dialogState = DialogState.Dismiss,
                    onRequestDismissDialog = {},
                    onRecordTimeClick = {},
                    onDateSelected = {},
                    onTimeSelected = {},
                    selectedTypeCategory = RecordTypeCategoryEnum.EXPENDITURE,
                    onTypeCategorySelect = {},
                    bottomSheetType = EditRecordBottomSheetEnum.NONE,
                    onRequestDismissBottomSheet = {},
                    onAmountClick = {},
                    onAmountChange = {},
                    onChargesClick = {},
                    onChargesChange = {},
                    onConcessionsClick = {},
                    onRelatedRecordClick = {},
                    onConcessionsChange = {},
                    onImageItemClick = { _, _ -> },
                    onImageListSave = {},
                    typeListContent = { Text(text = "类型列表") },
                    onRemarkChange = {},
                    onAssetClick = {},
                    onRelatedAssetClick = {},
                    selectAssetBottomSheetContent = {},
                    selectRelatedAssetBottomSheetContent = {},
                    tagText = "",
                    imageList = emptyList(),
                    onTagClick = {},
                    onImageClick = {},
                    selectTagBottomSheetContent = {},
                    onReimbursableClick = {},
                    onSaveClick = {},
                    onBackClick = {},
                )
            }
        }
    }

    @Test
    fun editRecordScreen_loading_multipleDevices() {
        composeTestRule.captureMultiDevice(screenshotName = "EditRecordScreen_loading") {
            CashbookTheme {
                EditRecordScreen(
                    uiState = EditRecordUiState.Loading,
                    shouldDisplayBookmark = EditRecordBookmarkEnum.NONE,
                    onRequestDismissBookmark = {},
                    dialogState = DialogState.Dismiss,
                    onRequestDismissDialog = {},
                    onRecordTimeClick = {},
                    onDateSelected = {},
                    onTimeSelected = {},
                    selectedTypeCategory = RecordTypeCategoryEnum.EXPENDITURE,
                    onTypeCategorySelect = {},
                    bottomSheetType = EditRecordBottomSheetEnum.NONE,
                    onRequestDismissBottomSheet = {},
                    onAmountClick = {},
                    onAmountChange = {},
                    onChargesClick = {},
                    onChargesChange = {},
                    onConcessionsClick = {},
                    onRelatedRecordClick = {},
                    onConcessionsChange = {},
                    onImageItemClick = { _, _ -> },
                    onImageListSave = {},
                    typeListContent = {},
                    onRemarkChange = {},
                    onAssetClick = {},
                    onRelatedAssetClick = {},
                    selectAssetBottomSheetContent = {},
                    selectRelatedAssetBottomSheetContent = {},
                    tagText = "",
                    imageList = emptyList(),
                    onTagClick = {},
                    onImageClick = {},
                    selectTagBottomSheetContent = {},
                    onReimbursableClick = {},
                    onSaveClick = {},
                    onBackClick = {},
                )
            }
        }
    }
}
