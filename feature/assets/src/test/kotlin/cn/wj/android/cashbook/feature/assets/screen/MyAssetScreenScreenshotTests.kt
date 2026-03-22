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
import cn.wj.android.cashbook.core.model.enums.ClassificationTypeEnum
import cn.wj.android.cashbook.core.model.model.AssetTypeViewsModel
import cn.wj.android.cashbook.core.testing.data.createAssetModel
import cn.wj.android.cashbook.core.testing.util.captureMultiDevice
import cn.wj.android.cashbook.core.testing.util.captureMultiTheme
import cn.wj.android.cashbook.feature.assets.viewmodel.MyAssetUiState
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
class MyAssetScreenScreenshotTests {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val successUiState = MyAssetUiState.Success(
        topUpInTotal = false,
        totalAsset = "10,000.00",
        totalLiabilities = "2,000.00",
        netAsset = "8,000.00",
    )

    private val assetTypedListData = listOf(
        AssetTypeViewsModel(
            type = ClassificationTypeEnum.CAPITAL_ACCOUNT,
            nameResId = 0,
            totalAmount = 1000000L,
            assetList = listOf(
                createAssetModel(
                    id = 1L,
                    name = "现金",
                    balance = 500000L,
                    classification = AssetClassificationEnum.CASH,
                ),
                createAssetModel(
                    id = 2L,
                    name = "支付宝",
                    balance = 500000L,
                    classification = AssetClassificationEnum.ALIPAY,
                ),
            ),
        ),
    )

    @Test
    fun myAssetScreen_loading_multipleThemes() {
        composeTestRule.captureMultiTheme(
            name = "MyAssetScreen",
            overrideFileName = "MyAssetScreen_loading",
        ) {
            MyAssetScreen(
                uiState = MyAssetUiState.Loading,
                showMoreDialog = false,
                onRequestDisplayShowMoreDialog = {},
                onRequestDismissShowMoreDialog = {},
                assetTypedListData = emptyList(),
                onTopUpInTotalChange = {},
                onAssetItemClick = {},
                onAddAssetClick = {},
                onInvisibleAssetClick = {},
                onBackClick = {},
            )
        }
    }

    @Test
    fun myAssetScreen_withAssets_multipleThemes() {
        composeTestRule.captureMultiTheme(name = "MyAssetScreen") {
            MyAssetScreen(
                uiState = successUiState,
                showMoreDialog = false,
                onRequestDisplayShowMoreDialog = {},
                onRequestDismissShowMoreDialog = {},
                assetTypedListData = assetTypedListData,
                onTopUpInTotalChange = {},
                onAssetItemClick = {},
                onAddAssetClick = {},
                onInvisibleAssetClick = {},
                onBackClick = {},
            )
        }
    }

    @Test
    fun myAssetScreen_empty_multipleThemes() {
        composeTestRule.captureMultiTheme(
            name = "MyAssetScreen",
            overrideFileName = "MyAssetScreen_empty",
        ) {
            MyAssetScreen(
                uiState = successUiState,
                showMoreDialog = false,
                onRequestDisplayShowMoreDialog = {},
                onRequestDismissShowMoreDialog = {},
                assetTypedListData = emptyList(),
                onTopUpInTotalChange = {},
                onAssetItemClick = {},
                onAddAssetClick = {},
                onInvisibleAssetClick = {},
                onBackClick = {},
            )
        }
    }

    @Test
    fun myAssetScreen_withAssets_multipleDevices() {
        composeTestRule.captureMultiDevice(screenshotName = "MyAssetScreen") {
            CashbookTheme {
                MyAssetScreen(
                    uiState = successUiState,
                    showMoreDialog = false,
                    onRequestDisplayShowMoreDialog = {},
                    onRequestDismissShowMoreDialog = {},
                    assetTypedListData = assetTypedListData,
                    onTopUpInTotalChange = {},
                    onAssetItemClick = {},
                    onAddAssetClick = {},
                    onInvisibleAssetClick = {},
                    onBackClick = {},
                )
            }
        }
    }

    @Test
    fun myAssetScreen_empty_multipleDevices() {
        composeTestRule.captureMultiDevice(screenshotName = "MyAssetScreen_empty") {
            CashbookTheme {
                MyAssetScreen(
                    uiState = successUiState,
                    showMoreDialog = false,
                    onRequestDisplayShowMoreDialog = {},
                    onRequestDismissShowMoreDialog = {},
                    assetTypedListData = emptyList(),
                    onTopUpInTotalChange = {},
                    onAssetItemClick = {},
                    onAddAssetClick = {},
                    onInvisibleAssetClick = {},
                    onBackClick = {},
                )
            }
        }
    }
}
