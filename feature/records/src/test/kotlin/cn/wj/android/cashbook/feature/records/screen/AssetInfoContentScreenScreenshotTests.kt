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
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import cn.wj.android.cashbook.core.design.theme.CashbookTheme
import cn.wj.android.cashbook.core.model.entity.DateSelectionEntity
import cn.wj.android.cashbook.core.model.entity.RecordViewsEntity
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.model.model.AssetMonthSummaryModel
import cn.wj.android.cashbook.core.testing.util.captureMultiDevice
import cn.wj.android.cashbook.core.testing.util.captureMultiTheme
import cn.wj.android.cashbook.feature.records.viewmodel.LauncherListItem
import dagger.hilt.android.testing.HiltTestApplication
import kotlinx.coroutines.flow.flowOf
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
class AssetInfoContentScreenScreenshotTests {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val sampleRecords = listOf(
        RecordViewsEntity(
            id = 1L,
            typeId = 1L,
            typeCategory = RecordTypeCategoryEnum.EXPENDITURE,
            typeName = "餐饮",
            typeIconResName = "vector_type_three_meals_24",
            assetId = 1L,
            assetName = "现金",
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
        RecordViewsEntity(
            id = 2L,
            typeId = 2L,
            typeCategory = RecordTypeCategoryEnum.INCOME,
            typeName = "工资",
            typeIconResName = "vector_type_salary_24",
            assetId = 1L,
            assetName = "现金",
            assetIconResId = null,
            relatedAssetId = null,
            relatedAssetName = null,
            relatedAssetIconResId = null,
            amount = 5000_00L,
            finalAmount = 5000_00L,
            charges = 0L,
            concessions = 0L,
            remark = "月薪",
            reimbursable = false,
            relatedTags = emptyList(),
            relatedImage = emptyList(),
            relatedRecord = emptyList(),
            relatedAmount = 0L,
            recordTime = 1705363200000L,
        ),
    )

    /** 按日分组后的列表项（日期头 + 记录），模拟 ViewModel 经 insertSeparators 生成的结果 */
    private val sampleItems = listOf<LauncherListItem>(
        LauncherListItem.DayHeader(dateStr = "2024-01-15", day = 15, dayType = 1),
        LauncherListItem.Record(sampleRecords[0]),
        LauncherListItem.DayHeader(dateStr = "2024-01-16", day = 16, dayType = 1),
        LauncherListItem.Record(sampleRecords[1]),
    )

    private val sampleSummary = AssetMonthSummaryModel(
        income = 5000_00L,
        expenditure = 50_00L,
        balance = 4950_00L,
    )

    private val sampleDateSelection = DateSelectionEntity.ByMonth(YearMonth.of(2024, 1))

    @Test
    fun assetInfoContentScreen_withRecords_multipleThemes() {
        composeTestRule.captureMultiTheme(name = "AssetInfoContentScreen") {
            val recordList = flowOf(PagingData.from(sampleItems))
                .collectAsLazyPagingItems()
            AssetInfoContentScreen(
                topContent = { Text(text = "资产信息头部") },
                dateSelection = sampleDateSelection,
                summary = sampleSummary,
                recordList = recordList,
                onPreviousMonth = {},
                onNextMonth = {},
                onRecordItemClick = {},
            )
        }
    }

    @Test
    fun assetInfoContentScreen_empty_multipleThemes() {
        composeTestRule.captureMultiTheme(
            name = "AssetInfoContentScreen",
            overrideFileName = "AssetInfoContentScreen_empty",
        ) {
            val recordList = flowOf(PagingData.from(emptyList<LauncherListItem>()))
                .collectAsLazyPagingItems()
            AssetInfoContentScreen(
                topContent = { Text(text = "资产信息头部") },
                dateSelection = sampleDateSelection,
                summary = AssetMonthSummaryModel(0L, 0L, 0L),
                recordList = recordList,
                onPreviousMonth = {},
                onNextMonth = {},
                onRecordItemClick = {},
            )
        }
    }

    @Test
    fun assetInfoContentScreen_withRecords_multipleDevices() {
        composeTestRule.captureMultiDevice(screenshotName = "AssetInfoContentScreen") {
            CashbookTheme {
                val recordList = flowOf(PagingData.from(sampleItems))
                    .collectAsLazyPagingItems()
                AssetInfoContentScreen(
                    topContent = { Text(text = "资产信息头部") },
                    dateSelection = sampleDateSelection,
                    summary = sampleSummary,
                    recordList = recordList,
                    onPreviousMonth = {},
                    onNextMonth = {},
                    onRecordItemClick = {},
                )
            }
        }
    }

    @Test
    fun assetInfoContentScreen_empty_multipleDevices() {
        composeTestRule.captureMultiDevice(screenshotName = "AssetInfoContentScreen_empty") {
            CashbookTheme {
                val recordList = flowOf(PagingData.from(emptyList<LauncherListItem>()))
                    .collectAsLazyPagingItems()
                AssetInfoContentScreen(
                    topContent = { Text(text = "资产信息头部") },
                    dateSelection = sampleDateSelection,
                    summary = AssetMonthSummaryModel(0L, 0L, 0L),
                    recordList = recordList,
                    onPreviousMonth = {},
                    onNextMonth = {},
                    onRecordItemClick = {},
                )
            }
        }
    }

    @Test
    fun assetInfoContentScreen_fixedPeriod_multipleThemes() {
        composeTestRule.captureMultiTheme(
            name = "AssetInfoContentScreen",
            overrideFileName = "AssetInfoContentScreen_fixedPeriod",
        ) {
            val recordList = flowOf(PagingData.from(sampleItems))
                .collectAsLazyPagingItems()
            AssetInfoContentScreen(
                topContent = { Text(text = "资产信息头部") },
                dateSelection = DateSelectionEntity.ByYear(2024),
                summary = sampleSummary,
                recordList = recordList,
                onPreviousMonth = {},
                onNextMonth = {},
                onRecordItemClick = {},
            )
        }
    }
}
