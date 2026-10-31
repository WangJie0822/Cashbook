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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import cn.wj.android.cashbook.core.design.theme.CashbookTheme
import cn.wj.android.cashbook.core.model.entity.RecordViewsEntity
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.model.model.RecordCategoryNet
import cn.wj.android.cashbook.core.model.model.RecordFilter
import cn.wj.android.cashbook.core.model.model.RecordFilterSummary
import cn.wj.android.cashbook.core.model.model.toRequest
import cn.wj.android.cashbook.core.model.transfer.asEntity
import cn.wj.android.cashbook.core.testing.data.createRecordModel
import cn.wj.android.cashbook.core.testing.data.createRecordTypeModel
import cn.wj.android.cashbook.core.testing.data.createRecordViewsModel
import cn.wj.android.cashbook.core.testing.data.createTagModel
import cn.wj.android.cashbook.core.testing.util.captureMultiDevice
import cn.wj.android.cashbook.core.testing.util.captureMultiTheme
import cn.wj.android.cashbook.feature.records.model.SearchFilterState
import cn.wj.android.cashbook.feature.records.model.SearchFilterSummaryState
import cn.wj.android.cashbook.feature.records.model.SearchMode
import cn.wj.android.cashbook.feature.records.model.SearchQueryContext
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
    private val loaded = LoadStates(LoadState.NotLoading(false), LoadState.NotLoading(true), LoadState.NotLoading(true))

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val sampleRecords = listOf(
        RecordViewsEntity(
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
        ),
    )

    private val filterRecords = listOf(
        sampleRecords.single(),
        createRecordViewsModel(id = 2L, type = createRecordTypeModel(id = 20L, name = "报销收入", typeCategory = RecordTypeCategoryEnum.INCOME, iconName = "vector_type_three_meals_24"), amount = 80000L, finalAmount = 0L, relatedRecord = listOf(createRecordModel(id = 101L)), relatedAmount = 80000L).asEntity(),
        createRecordViewsModel(id = 3L, type = createRecordTypeModel(id = 30L, name = "银行卡转账", typeCategory = RecordTypeCategoryEnum.TRANSFER, iconName = "vector_type_three_meals_24"), amount = 500000L, finalAmount = -200L, charges = 200L).asEntity(),
    )

    private val keywordRecords = listOf(
        sampleRecords.single(),
        createRecordViewsModel(id = 202L, type = createRecordTypeModel(id = 20L, name = "餐补", typeCategory = RecordTypeCategoryEnum.INCOME, iconName = "vector_type_three_meals_24"), amount = 2000L, finalAmount = 2000L, remark = "午餐补贴").asEntity(),
        createRecordViewsModel(id = 203L, type = createRecordTypeModel(id = 30L, name = "优惠转账", typeCategory = RecordTypeCategoryEnum.TRANSFER, iconName = "vector_type_three_meals_24"), amount = 100000L, finalAmount = 50L, concessions = 50L, remark = "午餐转账").asEntity(),
    )
    private val keywordContext = SearchQueryContext(1L, 1L, SearchMode.Keyword, keyword = "午餐")
    private val keywordState = SearchFilterState(mode = SearchMode.Keyword, queryDraft = "午餐", submittedKeyword = "午餐", bookId = 1L, settingsLoaded = true, catalogLoaded = true, tagsLoaded = true, queryContext = keywordContext)
    private val keywordSummary = SearchFilterSummaryState.Ready(keywordContext, RecordFilterSummary(1, 0, 5000), listOf(RecordCategoryNet(1, RecordTypeCategoryEnum.EXPENDITURE, "餐饮", 5000)))

    @Composable
    private fun FilterContent(kind: String) {
        val filter = RecordFilter(tagIds = setOf(41L))
        val keyword = kind == "keyword" || kind == "combined"
        val mode = if (keyword) SearchMode.Keyword else SearchMode.Filter
        val context = SearchQueryContext(1L, 1L, mode, if (kind == "keyword") null else filter.toRequest(1L, 1), if (keyword) "午餐" else "")
        val rows = when {
            kind == "combined" -> keywordRecords.map { it.copy(relatedTags = listOf(createTagModel(id = 41L, name = "旅行"))) }
            kind == "ready" -> filterRecords
            keyword -> keywordRecords
            else -> emptyList()
        }
        val source = remember(kind) { flowOf(PagingData.from(rows, sourceLoadStates = loaded)) }
        val records = source.collectAsLazyPagingItems()
        val state = SearchFilterState(mode = mode, queryDraft = if (keyword) "午餐" else "", submittedKeyword = if (keyword) "午餐" else "", appliedFilter = if (kind == "keyword") null else filter, bookId = 1L, bookName = "日常账本", settingsLoaded = true, catalogLoaded = true, tagsLoaded = true, tags = listOf(createTagModel(id = 41L, name = "旅行")), queryContext = context)
        val summary = when (kind) {
            "error" -> SearchFilterSummaryState.Failed(context, "query_failed")
            "loading" -> SearchFilterSummaryState.Loading(context)
            "empty" -> SearchFilterSummaryState.Ready(context, RecordFilterSummary(0L, 0L, 0L))
            "keyword", "combined" -> SearchFilterSummaryState.Ready(
                context,
                RecordFilterSummary(3, 2000, 4950),
                listOf(
                    RecordCategoryNet(1, RecordTypeCategoryEnum.EXPENDITURE, "餐饮", 5000),
                    RecordCategoryNet(20, RecordTypeCategoryEnum.INCOME, "餐补", 2000),
                    RecordCategoryNet(30, RecordTypeCategoryEnum.TRANSFER, "优惠转账", -50),
                ),
            )
            else -> SearchFilterSummaryState.Ready(
                context,
                RecordFilterSummary(3L, 0L, 5200L),
                listOf(
                    RecordCategoryNet(1, RecordTypeCategoryEnum.EXPENDITURE, "餐饮", 5000),
                    RecordCategoryNet(30, RecordTypeCategoryEnum.TRANSFER, "银行卡转账", 200),
                    RecordCategoryNet(20, RecordTypeCategoryEnum.INCOME, "报销收入", 0),
                ),
            )
        }
        SearchScreen(null, {}, {}, emptyList(), {}, records, {}, {}, {}, {}, state, summary, {})
    }

    @Test fun searchScreen_filterSummary_multipleThemes() { composeTestRule.captureMultiTheme("SearchFilterSummary") { FilterContent("ready") } }

    @Test fun searchScreen_filterSummary_multipleDevices() { composeTestRule.captureMultiDevice("SearchFilterSummary") { CashbookTheme { FilterContent("ready") } } }

    @Test fun searchScreen_keywordSummary_multipleThemes() { composeTestRule.captureMultiTheme("SearchKeywordStatistics") { FilterContent("keyword") } }

    @Test fun searchScreen_keywordSummary_multipleDevices() { composeTestRule.captureMultiDevice("SearchKeywordStatistics") { CashbookTheme { FilterContent("keyword") } } }

    @Test fun searchScreen_combinedSummary_multipleThemes() { composeTestRule.captureMultiTheme("SearchCombinedStatistics") { FilterContent("combined") } }

    @Test fun searchScreen_combinedSummary_multipleDevices() { composeTestRule.captureMultiDevice("SearchCombinedStatistics") { CashbookTheme { FilterContent("combined") } } }

    @Test fun searchScreen_filterEmpty_multipleThemes() { composeTestRule.captureMultiTheme("SearchFilterEmpty") { FilterContent("empty") } }

    @Test fun searchScreen_filterLoading_multipleThemes() { composeTestRule.captureMultiTheme("SearchFilterLoading") { FilterContent("loading") } }

    @Test fun searchScreen_filterError_multipleThemes() { composeTestRule.captureMultiTheme("SearchFilterError") { FilterContent("error") } }

    @Test
    fun searchScreen_empty_multipleThemes() {
        composeTestRule.captureMultiTheme(
            name = "SearchScreen",
            overrideFileName = "SearchScreen_empty",
        ) {
            val recordList = flowOf(PagingData.from(emptyList<RecordViewsEntity>(), sourceLoadStates = loaded))
                .collectAsLazyPagingItems()
            SearchScreen(
                viewRecord = null,
                onRequestShowRecordDetailSheet = {},
                onRequestDismissBottomSheet = {},
                searchHistoryList = listOf("午餐", "交通"),
                onRequestClearSearchHistory = {},
                recordList = recordList,
                filterState = SearchFilterState(bookId = 1L, settingsLoaded = true, catalogLoaded = true, tagsLoaded = true),
                summaryState = SearchFilterSummaryState.Hidden,
                onFilterEvent = {},
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
            val recordList = flowOf(PagingData.from(sampleRecords, sourceLoadStates = loaded))
                .collectAsLazyPagingItems()
            SearchScreen(
                viewRecord = null,
                onRequestShowRecordDetailSheet = {},
                onRequestDismissBottomSheet = {},
                searchHistoryList = emptyList(),
                onRequestClearSearchHistory = {},
                recordList = recordList,
                filterState = keywordState,
                summaryState = keywordSummary,
                onFilterEvent = {},
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
                val recordList = flowOf(PagingData.from(sampleRecords, sourceLoadStates = loaded))
                    .collectAsLazyPagingItems()
                SearchScreen(
                    viewRecord = null,
                    onRequestShowRecordDetailSheet = {},
                    onRequestDismissBottomSheet = {},
                    searchHistoryList = emptyList(),
                    onRequestClearSearchHistory = {},
                    recordList = recordList,
                    filterState = keywordState,
                    summaryState = keywordSummary,
                    onFilterEvent = {},
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
                val recordList = flowOf(PagingData.from(emptyList<RecordViewsEntity>(), sourceLoadStates = loaded))
                    .collectAsLazyPagingItems()
                SearchScreen(
                    viewRecord = null,
                    onRequestShowRecordDetailSheet = {},
                    onRequestDismissBottomSheet = {},
                    searchHistoryList = listOf("午餐", "交通"),
                    onRequestClearSearchHistory = {},
                    recordList = recordList,
                    filterState = SearchFilterState(bookId = 1L, settingsLoaded = true, catalogLoaded = true, tagsLoaded = true),
                    summaryState = SearchFilterSummaryState.Hidden,
                    onFilterEvent = {},
                    onKeywordChange = {},
                    onRequestNaviToEditRecord = {},
                    onRequestNaviToAssetInfo = {},
                    onRequestPopBackStack = {},
                )
            }
        }
    }
}
