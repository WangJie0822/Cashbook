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
import cn.wj.android.cashbook.core.testing.util.captureMultiDevice
import cn.wj.android.cashbook.core.testing.util.captureMultiTheme
import cn.wj.android.cashbook.core.ui.DialogState
import cn.wj.android.cashbook.feature.assets.enums.AssetInfoBookmarkEnum
import cn.wj.android.cashbook.feature.assets.viewmodel.AssetInfoUiState
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
class AssetInfoScreenScreenshotTests {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val successUiState = AssetInfoUiState.Success(
        assetName = "中国银行信用卡",
        isCreditCard = true,
        balance = "12,345.67",
        totalAmount = "50,000.00",
        billingDate = "15",
        repaymentDate = "5",
        openBank = "中国银行",
        cardNo = "6222 **** **** 1234",
        remark = "主要消费卡",
    )

    @Test
    fun assetInfoScreen_loading_multipleThemes() {
        composeTestRule.captureMultiTheme(
            name = "AssetInfoScreen",
            overrideFileName = "AssetInfoScreen_loading",
        ) {
            AssetInfoScreen(
                bookmark = AssetInfoBookmarkEnum.DISMISS,
                onRequestDisplayBookmark = {},
                onRequestDismissBookmark = {},
                dialogState = DialogState.Dismiss,
                onRequestShowMoreDialog = {},
                onRequestDismissDialog = {},
                uiState = AssetInfoUiState.Loading,
                viewRecord = null,
                assetRecordListContent = { _ -> },
                recordDetailSheetContent = {},
                onEditAssetClick = {},
                onDeleteAssetClick = {},
                onConfirmDeleteAsset = {},
                onRequestDismissBottomSheet = {},
                onAddRecordClick = {},
                onBackClick = {},
            )
        }
    }

    @Test
    fun assetInfoScreen_success_multipleThemes() {
        composeTestRule.captureMultiTheme(name = "AssetInfoScreen") {
            AssetInfoScreen(
                bookmark = AssetInfoBookmarkEnum.DISMISS,
                onRequestDisplayBookmark = {},
                onRequestDismissBookmark = {},
                dialogState = DialogState.Dismiss,
                onRequestShowMoreDialog = {},
                onRequestDismissDialog = {},
                uiState = successUiState,
                viewRecord = null,
                assetRecordListContent = { topContent -> topContent() },
                recordDetailSheetContent = {},
                onEditAssetClick = {},
                onDeleteAssetClick = {},
                onConfirmDeleteAsset = {},
                onRequestDismissBottomSheet = {},
                onAddRecordClick = {},
                onBackClick = {},
            )
        }
    }

    @Test
    fun assetInfoScreen_success_multipleDevices() {
        composeTestRule.captureMultiDevice(screenshotName = "AssetInfoScreen") {
            CashbookTheme {
                AssetInfoScreen(
                    bookmark = AssetInfoBookmarkEnum.DISMISS,
                    onRequestDisplayBookmark = {},
                    onRequestDismissBookmark = {},
                    dialogState = DialogState.Dismiss,
                    onRequestShowMoreDialog = {},
                    onRequestDismissDialog = {},
                    uiState = successUiState,
                    viewRecord = null,
                    assetRecordListContent = { topContent -> topContent() },
                    recordDetailSheetContent = {},
                    onEditAssetClick = {},
                    onDeleteAssetClick = {},
                    onConfirmDeleteAsset = {},
                    onRequestDismissBottomSheet = {},
                    onAddRecordClick = {},
                    onBackClick = {},
                )
            }
        }
    }

    @Test
    fun assetInfoScreen_loading_multipleDevices() {
        composeTestRule.captureMultiDevice(screenshotName = "AssetInfoScreen_loading") {
            CashbookTheme {
                AssetInfoScreen(
                    bookmark = AssetInfoBookmarkEnum.DISMISS,
                    onRequestDisplayBookmark = {},
                    onRequestDismissBookmark = {},
                    dialogState = DialogState.Dismiss,
                    onRequestShowMoreDialog = {},
                    onRequestDismissDialog = {},
                    uiState = AssetInfoUiState.Loading,
                    viewRecord = null,
                    assetRecordListContent = { _ -> },
                    recordDetailSheetContent = {},
                    onEditAssetClick = {},
                    onDeleteAssetClick = {},
                    onConfirmDeleteAsset = {},
                    onRequestDismissBottomSheet = {},
                    onAddRecordClick = {},
                    onBackClick = {},
                )
            }
        }
    }
}
