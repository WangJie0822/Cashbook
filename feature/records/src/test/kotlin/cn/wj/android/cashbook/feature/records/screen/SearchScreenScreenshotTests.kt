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
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import cn.wj.android.cashbook.core.design.theme.CashbookTheme
import cn.wj.android.cashbook.core.model.entity.RecordViewsEntity
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.testing.util.captureMultiDevice
import cn.wj.android.cashbook.core.testing.util.captureMultiTheme
import dagger.hilt.android.testing.HiltTestApplication
import kotlinx.coroutines.flow.flowOf
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
class SearchScreenScreenshotTests {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val sampleRecords = listOf(
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
    )

    @Test
    fun searchScreen_empty_multipleThemes() {
        composeTestRule.captureMultiTheme(
            name = "SearchScreen",
            overrideFileName = "SearchScreen_empty",
        ) {
            val recordList = flowOf(PagingData.from(emptyList<RecordViewsEntity>()))
                .collectAsLazyPagingItems()
            SearchScreen(
                viewRecord = null,
                onRequestShowRecordDetailSheet = {},
                onRequestDismissBottomSheet = {},
                searchHistoryList = listOf("午餐", "交通"),
                onRequestClearSearchHistory = {},
                recordList = recordList,
                onKeywordChange = {},
                onRequestNaviToEditRecord = {},
                onRequestNaviToAssetInfo = {},
                onRequestPopBackStack = {},
            )
        }
    }

    @Test
    fun searchScreen_withResults_multipleThemes() {
        composeTestRule.captureMultiTheme(name = "SearchScreen") {
            val recordList = flowOf(PagingData.from(sampleRecords))
                .collectAsLazyPagingItems()
            SearchScreen(
                viewRecord = null,
                onRequestShowRecordDetailSheet = {},
                onRequestDismissBottomSheet = {},
                searchHistoryList = emptyList(),
                onRequestClearSearchHistory = {},
                recordList = recordList,
                onKeywordChange = {},
                onRequestNaviToEditRecord = {},
                onRequestNaviToAssetInfo = {},
                onRequestPopBackStack = {},
            )
        }
    }

    @Test
    fun searchScreen_withResults_multipleDevices() {
        composeTestRule.captureMultiDevice(screenshotName = "SearchScreen") {
            CashbookTheme {
                val recordList = flowOf(PagingData.from(sampleRecords))
                    .collectAsLazyPagingItems()
                SearchScreen(
                    viewRecord = null,
                    onRequestShowRecordDetailSheet = {},
                    onRequestDismissBottomSheet = {},
                    searchHistoryList = emptyList(),
                    onRequestClearSearchHistory = {},
                    recordList = recordList,
                    onKeywordChange = {},
                    onRequestNaviToEditRecord = {},
                    onRequestNaviToAssetInfo = {},
                    onRequestPopBackStack = {},
                )
            }
        }
    }

    @Test
    fun searchScreen_empty_multipleDevices() {
        composeTestRule.captureMultiDevice(screenshotName = "SearchScreen_empty") {
            CashbookTheme {
                val recordList = flowOf(PagingData.from(emptyList<RecordViewsEntity>()))
                    .collectAsLazyPagingItems()
                SearchScreen(
                    viewRecord = null,
                    onRequestShowRecordDetailSheet = {},
                    onRequestDismissBottomSheet = {},
                    searchHistoryList = listOf("午餐", "交通"),
                    onRequestClearSearchHistory = {},
                    recordList = recordList,
                    onKeywordChange = {},
                    onRequestNaviToEditRecord = {},
                    onRequestNaviToAssetInfo = {},
                    onRequestPopBackStack = {},
                )
            }
        }
    }
}
