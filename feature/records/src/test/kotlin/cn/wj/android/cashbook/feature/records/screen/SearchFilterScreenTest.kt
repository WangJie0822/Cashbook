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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import cn.wj.android.cashbook.core.design.component.CbTextButton
import cn.wj.android.cashbook.core.design.component.LocalDefaultEmptyImagePainter
import cn.wj.android.cashbook.core.design.component.LocalDefaultLoadingHint
import cn.wj.android.cashbook.core.design.theme.CashbookTheme
import cn.wj.android.cashbook.core.model.entity.RecordViewsEntity
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.model.model.RecordFilter
import cn.wj.android.cashbook.core.model.model.RecordFilterSummary
import cn.wj.android.cashbook.core.model.model.toRequest
import cn.wj.android.cashbook.core.model.transfer.asEntity
import cn.wj.android.cashbook.core.testing.data.createBooksModel
import cn.wj.android.cashbook.core.testing.data.createRecordModel
import cn.wj.android.cashbook.core.testing.data.createRecordTypeModel
import cn.wj.android.cashbook.core.testing.data.createRecordViewsModel
import cn.wj.android.cashbook.core.testing.data.createTagModel
import cn.wj.android.cashbook.feature.records.model.SearchFilterState
import cn.wj.android.cashbook.feature.records.model.SearchFilterSummaryState
import cn.wj.android.cashbook.feature.records.model.SearchMode
import cn.wj.android.cashbook.feature.records.model.SearchQueryContext
import cn.wj.android.cashbook.feature.records.view.RecordDetailsSheet
import cn.wj.android.cashbook.feature.records.view.SearchFilterTestFixture
import cn.wj.android.cashbook.feature.records.viewmodel.SearchViewModel
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltTestApplication
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = HiltTestApplication::class, qualifiers = "zh-rCN-w411dp-h891dp-480dpi")
@LooperMode(LooperMode.Mode.PAUSED)
class SearchFilterScreenTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private lateinit var fixture: SearchFilterTestFixture

    @After fun close() { if (::fixture.isInitialized) compose.runOnIdle { fixture.close() } }

    private fun show(before: (SearchFilterTestFixture) -> Unit = {}) {
        compose.runOnUiThread {
            fixture = SearchFilterTestFixture()
            val expense = createRecordTypeModel(id = 11L, parentId = 10L, name = "午餐")
            val income = createRecordTypeModel(id = 20L, name = "工资", typeCategory = RecordTypeCategoryEnum.INCOME)
            val transfer = createRecordTypeModel(id = 30L, name = "转账", typeCategory = RecordTypeCategoryEnum.TRANSFER)
            val travel = listOf(createTagModel(id = 41L, name = "旅行"))
            fixture.filters.setRecords(
                listOf(
                    createRecordViewsModel(id = 1L, type = expense, amount = 100L, finalAmount = 100L, relatedTags = travel),
                    createRecordViewsModel(id = 2L, type = income, amount = 80000L, finalAmount = 0L, relatedRecord = listOf(createRecordModel(id = 101L)), relatedTags = travel),
                    createRecordViewsModel(id = 3L, type = transfer, amount = 500000L, finalAmount = -200L, charges = 200L, relatedTags = travel),
                    createRecordViewsModel(id = 4L, type = expense, amount = 300L, finalAmount = 300L, remark = "关键词", relatedTags = listOf(createTagModel(id = 42L))),
                ),
            )
            fixture.records.addRecord(createRecordModel(id = 4L, typeId = 11L, amount = 300L, finalAmount = 300L, remark = "关键词"))
            before(fixture)
        }
        compose.setContent { CompositionLocalProvider(LocalDefaultEmptyImagePainter provides ColorPainter(Color.Gray), LocalDefaultLoadingHint provides "数据加载中") { CashbookTheme { SearchRoute({}, {}, {}, viewModel = fixture.vm) } } }
    }

    private fun waitFor(tag: String) {
        compose.waitUntil(10_000L) { compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun applyTravel(awaitTag: String = "filter_summary_count") {
        compose.onNodeWithTag("search_filter_button").performClick()
        compose.onNodeWithTag("filter_tag_41").performScrollTo().performClick()
        compose.onNodeWithText("完成").performClick()
        waitFor(awaitTag)
    }

    @Test fun realSearchEntry_appliesTagAndCategoryIntersectionToListAndSummary() {
        show()
        compose.onNodeWithTag("search_filter_button").performClick()
        compose.onNodeWithTag("filter_tag_41").performScrollTo().performClick()
        compose.onNodeWithTag("filter_categories").performScrollTo().performClick()
        compose.onNodeWithTag("category_group_10").performClick()
        compose.onNodeWithText("完成").performClick()
        compose.onNodeWithText("完成").performClick()
        waitFor("filter_summary_count")
        compose.onNodeWithTag("search_record_1").assertIsDisplayed()
        compose.onNodeWithTag("search_record_4").assertDoesNotExist()
        compose.onNodeWithTag("filter_summary_count").assertIsDisplayed()
        compose.onNodeWithText("1 笔").assertIsDisplayed()
        compose.runOnIdle { assertThat(fixture.vm.filterState.value.appliedFilter!!.categories.wholeGroupIds).containsExactly(10L) }
    }

    @Test fun transferAndRelatedIncome_keepMainAmountsAndExplainContributions() {
        show()
        applyTravel()
        compose.onNodeWithText("¥5000.00").assertIsDisplayed()
        compose.onNodeWithText("统计计入：支出 ¥2.00").assertIsDisplayed()
        compose.onNodeWithText("¥800.00").assertIsDisplayed()
        compose.onNodeWithText("统计计入：收入 ¥0.00").assertIsDisplayed()
        compose.onNodeWithTag("filter_contribution_1").assertDoesNotExist()
        compose.onNodeWithText("3 笔").assertIsDisplayed()
        compose.onNodeWithTag("filter_summary_expenditure").assertIsDisplayed()
    }

    @Test fun submittedKeyword_ignoresFiltersAndClearRestoresThem() {
        show()
        applyTravel()
        compose.onNodeWithTag("search_query").performTextInput("关键词")
        compose.onNodeWithTag("search_query").performImeAction()
        waitFor("search_record_4")
        compose.onNodeWithTag("search_record_4").assertIsDisplayed()
        compose.onNodeWithTag("filter_summary_count").assertIsDisplayed()
        compose.onNodeWithText("1 笔").assertIsDisplayed()
        compose.onNodeWithTag("filter_contribution_4").assertDoesNotExist()
        compose.runOnIdle { assertThat(fixture.vm.filterState.value.mode).isEqualTo(SearchMode.Keyword) }
        compose.onNodeWithTag("search_clear").performClick()
        waitFor("filter_summary_count")
        compose.onNodeWithText("3 笔").assertIsDisplayed()
        compose.onNodeWithTag("search_record_4").assertDoesNotExist()
    }

    @Test fun keyword_disablesFilterEntryAndExplainsHowToRestoreIt() {
        show()
        applyTravel()
        compose.onNodeWithTag("search_query").performTextInput("关键词")
        compose.onNodeWithTag("search_filter_button").assertIsEnabled()
        compose.onNodeWithTag("search_query").performImeAction()
        waitFor("search_record_4")
        compose.onNodeWithTag("search_filter_button").assertIsNotEnabled()
        compose.onNodeWithTag("search_filter_button").assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "清空关键字后筛选"))
        compose.runOnIdle { assertThat(fixture.vm.filterState.value.drawerDraft).isNull() }
        compose.onNodeWithTag("search_clear").performClick()
        waitFor("filter_summary_count")
        compose.onNodeWithTag("search_filter_button").assertIsEnabled().performClick()
        compose.runOnIdle { assertThat(fixture.vm.filterState.value.drawerDraft!!.tagIds).containsExactly(41L) }
    }

    @Test fun summaryFailure_showsRetryAndNeverZeroStatistics() {
        show { it.filters.summaryProvider = { flow { error("synthetic failure") } } }
        applyTravel("search_statistics_error")
        compose.onNodeWithTag("search_statistics_error").assertIsDisplayed()
        compose.onNodeWithTag("search_query_error").assertDoesNotExist()
        compose.onNodeWithTag("search_record_1").assertIsDisplayed()
        compose.onNodeWithTag("filter_summary_count").assertDoesNotExist()
        compose.runOnIdle { fixture.filters.summaryProvider = null }
        compose.onNodeWithTag("search_statistics_retry").performClick()
        waitFor("filter_summary_count")
        compose.onNodeWithText("3 笔").assertIsDisplayed()
        compose.onNodeWithTag("distribution_chart").assertIsDisplayed()
    }

    @Test fun statisticsLoadingDoesNotHideAlreadyLoadedRecords() {
        show { it.filters.summaryProvider = { flow { kotlinx.coroutines.awaitCancellation() } } }
        applyTravel("search_statistics_loading")
        compose.onNodeWithTag("search_statistics_loading").assertIsDisplayed()
        compose.onNodeWithTag("search_record_1").assertIsDisplayed()
        compose.onNodeWithTag("filter_summary_count").assertDoesNotExist()
    }

    @Test fun distributionInteractionsDoNotChangeTheQueryOrItsRecords() {
        show()
        applyTravel()
        val context = fixture.vm.filterState.value.queryContext
        val requests = fixture.filters.recordRequests.size
        compose.onNodeWithTag("distribution_toggle").performClick()
        compose.onNodeWithTag("distribution_chart").assertDoesNotExist()
        compose.onNodeWithTag("distribution_toggle").performClick()
        compose.onNodeWithTag("distribution_group_2_30").performClick()
        compose.runOnIdle {
            assertThat(fixture.vm.filterState.value.queryContext).isEqualTo(context)
            assertThat(fixture.filters.recordRequests).hasSize(requests)
            assertThat(fixture.vm.filterState.value.appliedFilter!!.tagIds).containsExactly(41L)
        }
        compose.onNodeWithTag("search_record_3").assertIsDisplayed()
    }

    @Test fun recordsFailure_canRetryWithoutShowingZeroSummary() {
        show { it.filters.recordsProvider = { flow { error("synthetic paging failure") } } }
        applyTravel("search_query_error")
        compose.onNodeWithTag("filter_summary_count").assertDoesNotExist()
        compose.runOnIdle { fixture.filters.recordsProvider = null }
        compose.onNodeWithTag("search_retry").performClick()
        waitFor("filter_summary_count")
        compose.onNodeWithText("3 笔").assertIsDisplayed()
        compose.onNodeWithTag("search_record_3").assertIsDisplayed()
    }

    @Test fun drawerScrim_cancelsDraftInsteadOfApplyingIt() {
        show()
        compose.onNodeWithTag("search_filter_button").performClick()
        compose.onNodeWithTag("filter_tag_41").performScrollTo().performClick()
        compose.onNodeWithTag("filter_scrim").performClick()
        compose.runOnIdle {
            assertThat(fixture.vm.filterState.value.drawerDraft).isNull()
            assertThat(fixture.vm.filterState.value.appliedFilter).isNull()
            assertThat(fixture.filters.recordRequests).isEmpty()
        }
    }

    @Test fun navigation_backToSameEntryKeepsFilterAndPosition_butReenterResets() {
        compose.setContent {
            CompositionLocalProvider(LocalDefaultEmptyImagePainter provides ColorPainter(Color.Gray), LocalDefaultLoadingHint provides "数据加载中") {
                CashbookTheme {
                    val nav = rememberNavController()
                    NavHost(nav, startDestination = "home") {
                        composable("home") { CbTextButton(onClick = { nav.navigate("search") }, modifier = Modifier.testTag("open_search")) { Text("打开搜索") } }
                        composable("search") { entry ->
                            val factory = remember(entry) {
                                object : ViewModelProvider.Factory {
                                    @Suppress("UNCHECKED_CAST")
                                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                                        require(modelClass == SearchViewModel::class.java)
                                        fixture = SearchFilterTestFixture()
                                        fixture.filters.setRecords((1L..80L).map { id -> createRecordViewsModel(id = id, type = createRecordTypeModel(id = 11L, parentId = 10L), amount = 100L, finalAmount = 100L, relatedTags = listOf(createTagModel(id = 41L))) })
                                        return fixture.vm as T
                                    }
                                }
                            }
                            val vm = viewModel<SearchViewModel>(entry, factory = factory)
                            SearchRoute(
                                onRequestNaviToEditRecord = { vm.dismissRecordDetailSheet(); nav.navigate("edit") },
                                onRequestNaviToAssetInfo = {},
                                onRequestPopBackStack = { nav.popBackStack() },
                                viewModel = vm,
                                recordDetailContent = { record -> RecordDetailsSheet(record, { vm.dismissRecordDetailSheet(); nav.navigate("edit") }, {}, {}, {}, vm::dismissRecordDetailSheet) },
                            )
                        }
                        composable("edit") { CbTextButton(onClick = { nav.popBackStack() }, modifier = Modifier.testTag("back_to_search")) { Text("返回搜索") } }
                    }
                }
            }
        }
        compose.onNodeWithTag("open_search").performClick()
        waitFor("search_filter_button")
        applyTravel()
        compose.onNodeWithTag("search_results").performScrollToNode(hasTestTag("search_record_50"))
        compose.onNodeWithTag("search_record_50").performClick()
        compose.onNodeWithText("编辑").performClick()
        compose.runOnIdle { fixture.filters.setRecords((1L..80L).map { id -> createRecordViewsModel(id = id, type = createRecordTypeModel(id = 11L, parentId = 10L), amount = 200L, finalAmount = 200L, relatedTags = listOf(createTagModel(id = 41L))) }) }
        compose.onNodeWithTag("back_to_search").performClick()
        waitFor("search_record_50")
        compose.onNodeWithTag("search_record_50").assertIsDisplayed()
        compose.runOnIdle { assertThat(fixture.vm.filterState.value.appliedFilter!!.tagIds).containsExactly(41L) }
        compose.onNodeWithTag("search_filter_button").performClick()
        compose.onNodeWithTag("filter_all_tags").performScrollTo().performClick()
        compose.onNodeWithText("完成").performClick()
        waitFor("filter_summary_count")
        compose.onNodeWithTag("filter_summary_count").assertIsDisplayed()
        compose.onNodeWithTag("search_back").performClick()
        compose.onNodeWithTag("open_search").performClick()
        waitFor("search_filter_button")
        compose.runOnIdle {
            assertThat(fixture.vm.filterState.value.mode).isEqualTo(SearchMode.Initial)
            assertThat(fixture.vm.filterState.value.appliedFilter).isNull()
            assertThat(fixture.filters.recordRequests).isEmpty()
        }
        compose.onNodeWithTag("filter_summary_count").assertDoesNotExist()
    }

    @Test fun newViewModel_delayedBookRestoresAppliedCriteriaAndScroll() {
        val handle = SavedStateHandle()
        val rows = (80L downTo 1L).map {
            createRecordViewsModel(id = it, type = createRecordTypeModel(id = 11L, iconName = "vector_type_three_meals_24"), amount = 100L, finalAmount = 100L, relatedTags = listOf(createTagModel(id = 41L)))
        }
        compose.runOnUiThread { fixture = SearchFilterTestFixture(handle).apply { filters.setRecords(rows) } }
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            CompositionLocalProvider(LocalDefaultEmptyImagePainter provides ColorPainter(Color.Gray)) { CashbookTheme { SearchRoute({}, {}, {}, viewModel = fixture.vm) } }
        }
        applyTravel()
        compose.onNodeWithTag("search_filter_button").performClick()
        compose.onNodeWithTag("filter_tag_42").performScrollTo().performClick()
        compose.onNodeWithText("完成").performClick()
        waitFor("filter_summary_count")
        compose.onNodeWithTag("search_results").performScrollToNode(hasTestTag("search_record_50"))
        compose.onNodeWithTag("search_record_50").assertIsDisplayed()
        val previousRevision = fixture.vm.filterState.value.queryContext.criteriaRevision
        val delayedBook = MutableSharedFlow<cn.wj.android.cashbook.core.model.model.BooksModel>(replay = 1)
        compose.runOnIdle {
            val restoredHandle = SavedStateHandle(handle.keys().associateWith { handle.get<Any?>(it) })
            fixture.close()
            fixture = SearchFilterTestFixture(restoredHandle, delayedBook).apply { filters.setRecords(rows) }
            assertThat(fixture.vm.filterState.value.metadataReady).isFalse()
        }
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("search_query_loading").assertIsDisplayed()
        compose.runOnIdle { runBlocking { delayedBook.emit(createBooksModel()) } }
        waitFor("search_results")
        compose.onNodeWithTag("search_record_50").assertIsDisplayed()
        compose.runOnIdle {
            assertThat(fixture.vm.filterState.value.appliedFilter!!.tagIds).containsExactly(41L, 42L)
            assertThat(fixture.vm.filterState.value.queryContext.criteriaRevision).isLessThan(previousRevision)
            assertThat(fixture.filters.recordRequests.last().bookId).isEqualTo(1L)
        }
    }

    @Test fun restoredScroll_usesActualCriteriaInsteadOfRestartedRevisionCounter() {
        val filter = RecordFilter(tagIds = setOf(41L))
        val context = SearchQueryContext(7L, 1L, SearchMode.Filter, filter.toRequest(1L, 1))
        val state = mutableStateOf(SearchFilterState(mode = SearchMode.Filter, appliedFilter = filter, queryContext = context))
        val rows = (80L downTo 1L).map { createRecordViewsModel(id = it, type = createRecordTypeModel(id = 11L, iconName = "vector_type_three_meals_24"), amount = 100L, finalAmount = 100L).asEntity() }
        val pages = flowOf(PagingData.from(rows, sourceLoadStates = LoadStates(LoadState.NotLoading(false), LoadState.NotLoading(true), LoadState.NotLoading(true))))
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            CompositionLocalProvider(LocalDefaultEmptyImagePainter provides ColorPainter(Color.Gray)) {
                CashbookTheme {
                    val records = pages.collectAsLazyPagingItems()
                    SearchScreen(
                        null, {}, {}, emptyList(), {}, records, {}, {}, {}, {}, state.value,
                        SearchFilterSummaryState.Ready(state.value.queryContext, RecordFilterSummary(80L, 0L, 8000L)), {},
                    )
                }
            }
        }
        waitFor("filter_summary_count")
        compose.onNodeWithTag("search_results").performScrollToNode(hasTestTag("search_record_50"))
        compose.runOnIdle { state.value = state.value.copy(queryContext = context.copy(criteriaRevision = 1L)) }
        restoration.emulateSavedInstanceStateRestore()
        waitFor("search_record_50")
        compose.onNodeWithTag("search_record_50").assertIsDisplayed()
    }

    @Test fun deletedAppliedTag_keepsConstraintAndExplainsEmptyResult() {
        show()
        applyTravel()
        compose.runOnIdle { runBlocking { fixture.tags.deleteTag(createTagModel(id = 41L)) } }
        waitFor("filter_constraint_notice")
        compose.onNodeWithText("部分选项已不可用，仍保留筛选约束").assertIsDisplayed()
        compose.onNodeWithText("0 笔").assertIsDisplayed()
        compose.runOnIdle { assertThat(fixture.vm.filterState.value.appliedFilter!!.tagIds).containsExactly(41L) }
    }

    @Test fun externalTypeChange_keepsOriginalIntersectionAndShowsConflict() {
        show()
        compose.onNodeWithTag("search_filter_button").performClick()
        compose.onNodeWithTag("filter_type_EXPENDITURE").performScrollTo().performClick()
        compose.onNodeWithTag("filter_categories").performScrollTo().performClick()
        compose.onNodeWithTag("category_item_11").performClick()
        compose.onNodeWithText("完成").performClick()
        compose.onNodeWithText("完成").performClick()
        waitFor("filter_summary_count")
        compose.runOnIdle { runBlocking { fixture.types.update(fixture.types.getRecordTypeById(11L)!!.copy(typeCategory = RecordTypeCategoryEnum.INCOME)) } }
        waitFor("filter_constraint_notice")
        compose.onNodeWithText("部分分类不在当前收支类型内，仍保留原筛选条件").assertIsDisplayed()
        compose.onNodeWithText("0 笔").assertIsDisplayed()
        compose.runOnIdle { assertThat(fixture.vm.filterState.value.appliedFilter!!.categories.exactTypeIds).containsExactly(11L) }
    }

    @Test fun oldPageAndSummary_areHiddenUntilTheirContextMatchesNewCriteria() {
        val filter = RecordFilter(tagIds = setOf(42L))
        val context = SearchQueryContext(2L, 1L, SearchMode.Filter, filter.toRequest(1L, 1))
        val old = context.copy(criteriaRevision = 1L, request = RecordFilter(tagIds = setOf(41L)).toRequest(1L, 1))
        val state = SearchFilterState(mode = SearchMode.Filter, appliedFilter = filter, queryContext = context)
        val pages = flowOf(PagingData.from(listOf(createRecordViewsModel(id = 99L, type = createRecordTypeModel(iconName = "vector_type_three_meals_24")).asEntity()), sourceLoadStates = LoadStates(LoadState.NotLoading(false), LoadState.NotLoading(true), LoadState.NotLoading(true))))
        compose.setContent {
            CashbookTheme {
                val records = pages.collectAsLazyPagingItems()
                SearchScreen(null, {}, {}, emptyList(), {}, records, {}, {}, {}, {}, state, SearchFilterSummaryState.Ready(old, RecordFilterSummary(1L, 0L, 100L)), {}, recordContext = old)
            }
        }
        compose.onNodeWithTag("search_query_loading").assertIsDisplayed()
        compose.onNodeWithTag("search_record_99").assertDoesNotExist()
        compose.onNodeWithTag("filter_summary_count").assertDoesNotExist()
    }

    @Test fun positiveStatisticsDoNotPretendAnEmptyPageIsTheFinalEmptyResult() {
        val filter = RecordFilter()
        val context = SearchQueryContext(1, 1, SearchMode.Filter, filter.toRequest(1, 1))
        val state = SearchFilterState(mode = SearchMode.Filter, appliedFilter = filter, queryContext = context)
        val pages = flowOf(PagingData.from(emptyList<RecordViewsEntity>(), sourceLoadStates = LoadStates(LoadState.NotLoading(false), LoadState.NotLoading(true), LoadState.NotLoading(true))))
        compose.setContent {
            CashbookTheme {
                val records = pages.collectAsLazyPagingItems()
                SearchScreen(null, {}, {}, emptyList(), {}, records, {}, {}, {}, {}, state, SearchFilterSummaryState.Ready(context, RecordFilterSummary(1, 0, 100)), {})
            }
        }
        compose.onNodeWithTag("search_query_loading").assertIsDisplayed()
        compose.onNodeWithTag("filter_summary_count").assertDoesNotExist()
    }
}
