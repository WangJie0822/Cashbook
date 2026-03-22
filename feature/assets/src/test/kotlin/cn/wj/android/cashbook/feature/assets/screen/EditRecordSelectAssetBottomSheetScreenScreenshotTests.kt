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
import cn.wj.android.cashbook.core.testing.data.createAssetModel
import cn.wj.android.cashbook.core.testing.util.captureMultiDevice
import cn.wj.android.cashbook.core.testing.util.captureMultiTheme
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
class EditRecordSelectAssetBottomSheetScreenScreenshotTests {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val assetList = listOf(
        createAssetModel(
            id = 1L,
            name = "现金",
            balance = 500000L,
            classification = AssetClassificationEnum.CASH,
        ),
        createAssetModel(
            id = 2L,
            name = "支付宝",
            balance = 1000000L,
            classification = AssetClassificationEnum.ALIPAY,
        ),
        createAssetModel(
            id = 3L,
            name = "微信钱包",
            balance = 300000L,
            classification = AssetClassificationEnum.WECHAT,
        ),
    )

    @Test
    fun editRecordSelectAssetBottomSheetScreen_withAssets_multipleThemes() {
        composeTestRule.captureMultiTheme(name = "EditRecordSelectAssetBottomSheetScreen") {
            EditRecordSelectAssetBottomSheetScreen(
                assetList = assetList,
                onAssetChange = {},
                onAddAssetClick = {},
            )
        }
    }

    @Test
    fun editRecordSelectAssetBottomSheetScreen_empty_multipleThemes() {
        composeTestRule.captureMultiTheme(
            name = "EditRecordSelectAssetBottomSheetScreen",
            overrideFileName = "EditRecordSelectAssetBottomSheetScreen_empty",
        ) {
            EditRecordSelectAssetBottomSheetScreen(
                assetList = emptyList(),
                onAssetChange = {},
                onAddAssetClick = {},
            )
        }
    }

    @Test
    fun editRecordSelectAssetBottomSheetScreen_withAssets_multipleDevices() {
        composeTestRule.captureMultiDevice(
            screenshotName = "EditRecordSelectAssetBottomSheetScreen",
        ) {
            CashbookTheme {
                EditRecordSelectAssetBottomSheetScreen(
                    assetList = assetList,
                    onAssetChange = {},
                    onAddAssetClick = {},
                )
            }
        }
    }

    @Test
    fun editRecordSelectAssetBottomSheetScreen_empty_multipleDevices() {
        composeTestRule.captureMultiDevice(
            screenshotName = "EditRecordSelectAssetBottomSheetScreen_empty",
        ) {
            CashbookTheme {
                EditRecordSelectAssetBottomSheetScreen(
                    assetList = emptyList(),
                    onAssetChange = {},
                    onAddAssetClick = {},
                )
            }
        }
    }
}
