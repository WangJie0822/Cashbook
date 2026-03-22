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

package cn.wj.android.cashbook.feature.assets.screen

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import cn.wj.android.cashbook.core.design.theme.CashbookTheme
import cn.wj.android.cashbook.core.model.enums.AssetClassificationEnum
import cn.wj.android.cashbook.core.testing.util.captureMultiDevice
import cn.wj.android.cashbook.core.testing.util.captureMultiTheme
import cn.wj.android.cashbook.core.ui.DialogState
import cn.wj.android.cashbook.feature.assets.enums.EditAssetBottomSheetEnum
import cn.wj.android.cashbook.feature.assets.viewmodel.EditAssetUiState
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
class EditAssetScreenScreenshotTests {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val newAssetUiState = EditAssetUiState.Success(
        typeEnable = true,
        isCreditCard = false,
        classification = AssetClassificationEnum.CASH,
        assetName = "",
        totalAmount = "",
        balance = "",
        openBank = "",
        cardNo = "",
        remark = "",
        billingDate = "",
        repaymentDate = "",
        invisible = false,
    )

    private val editAssetUiState = EditAssetUiState.Success(
        typeEnable = false,
        isCreditCard = true,
        classification = AssetClassificationEnum.CREDIT_CARD,
        assetName = "中国银行信用卡",
        totalAmount = "50,000.00",
        balance = "12,345.67",
        openBank = "中国银行",
        cardNo = "6222 **** **** 1234",
        remark = "主要消费卡",
        billingDate = "15",
        repaymentDate = "5",
        invisible = false,
    )

    @Test
    fun editAssetScreen_loading_multipleThemes() {
        composeTestRule.captureMultiTheme(
            name = "EditAssetScreen",
            overrideFileName = "EditAssetScreen_loading",
        ) {
            EditAssetScreen(
                isCreate = true,
                uiState = EditAssetUiState.Loading,
                onSelectClassificationClick = {},
                onClassificationChange = { _, _ -> },
                onBillingDateClick = {},
                onRepaymentDateClick = {},
                onInvisibleChange = {},
                bottomSheet = EditAssetBottomSheetEnum.DISMISS,
                onBottomSheetDismiss = {},
                dialogState = DialogState.Dismiss,
                onDialogDismiss = {},
                onDaySelect = {},
                onSaveClick = { _, _, _, _, _, _, _ -> },
                onBackClick = {},
            )
        }
    }

    @Test
    fun editAssetScreen_newAsset_multipleThemes() {
        composeTestRule.captureMultiTheme(name = "EditAssetScreen") {
            EditAssetScreen(
                isCreate = true,
                uiState = newAssetUiState,
                onSelectClassificationClick = {},
                onClassificationChange = { _, _ -> },
                onBillingDateClick = {},
                onRepaymentDateClick = {},
                onInvisibleChange = {},
                bottomSheet = EditAssetBottomSheetEnum.DISMISS,
                onBottomSheetDismiss = {},
                dialogState = DialogState.Dismiss,
                onDialogDismiss = {},
                onDaySelect = {},
                onSaveClick = { _, _, _, _, _, _, _ -> },
                onBackClick = {},
            )
        }
    }

    @Test
    fun editAssetScreen_editExisting_multipleThemes() {
        composeTestRule.captureMultiTheme(
            name = "EditAssetScreen",
            overrideFileName = "EditAssetScreen_edit",
        ) {
            EditAssetScreen(
                isCreate = false,
                uiState = editAssetUiState,
                onSelectClassificationClick = {},
                onClassificationChange = { _, _ -> },
                onBillingDateClick = {},
                onRepaymentDateClick = {},
                onInvisibleChange = {},
                bottomSheet = EditAssetBottomSheetEnum.DISMISS,
                onBottomSheetDismiss = {},
                dialogState = DialogState.Dismiss,
                onDialogDismiss = {},
                onDaySelect = {},
                onSaveClick = { _, _, _, _, _, _, _ -> },
                onBackClick = {},
            )
        }
    }

    @Test
    fun editAssetScreen_newAsset_multipleDevices() {
        composeTestRule.captureMultiDevice(screenshotName = "EditAssetScreen") {
            CashbookTheme {
                EditAssetScreen(
                    isCreate = true,
                    uiState = newAssetUiState,
                    onSelectClassificationClick = {},
                    onClassificationChange = { _, _ -> },
                    onBillingDateClick = {},
                    onRepaymentDateClick = {},
                    onInvisibleChange = {},
                    bottomSheet = EditAssetBottomSheetEnum.DISMISS,
                    onBottomSheetDismiss = {},
                    dialogState = DialogState.Dismiss,
                    onDialogDismiss = {},
                    onDaySelect = {},
                    onSaveClick = { _, _, _, _, _, _, _ -> },
                    onBackClick = {},
                )
            }
        }
    }

    @Test
    fun editAssetScreen_editExisting_multipleDevices() {
        composeTestRule.captureMultiDevice(screenshotName = "EditAssetScreen_edit") {
            CashbookTheme {
                EditAssetScreen(
                    isCreate = false,
                    uiState = editAssetUiState,
                    onSelectClassificationClick = {},
                    onClassificationChange = { _, _ -> },
                    onBillingDateClick = {},
                    onRepaymentDateClick = {},
                    onInvisibleChange = {},
                    bottomSheet = EditAssetBottomSheetEnum.DISMISS,
                    onBottomSheetDismiss = {},
                    dialogState = DialogState.Dismiss,
                    onDialogDismiss = {},
                    onDaySelect = {},
                    onSaveClick = { _, _, _, _, _, _, _ -> },
                    onBackClick = {},
                )
            }
        }
    }
}
