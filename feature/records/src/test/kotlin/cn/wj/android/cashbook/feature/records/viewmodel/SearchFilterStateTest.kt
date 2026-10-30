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

package cn.wj.android.cashbook.feature.records.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.PagingDataEvent
import androidx.paging.PagingDataPresenter
import cn.wj.android.cashbook.core.data.repository.RecordFilterValidation
import cn.wj.android.cashbook.core.model.entity.DateSelectionEntity
import cn.wj.android.cashbook.core.model.entity.RecordViewsEntity
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.model.enums.TypeLevelEnum
import cn.wj.android.cashbook.core.model.model.CategoryFilterSelection
import cn.wj.android.cashbook.core.model.model.RecordFilterSummary
import cn.wj.android.cashbook.core.testing.data.createBooksModel
import cn.wj.android.cashbook.core.testing.data.createRecordTypeModel
import cn.wj.android.cashbook.core.testing.data.createRecordViewsModel
import cn.wj.android.cashbook.core.testing.data.createTagModel
import cn.wj.android.cashbook.core.testing.repository.FakeAssetRepository
import cn.wj.android.cashbook.core.testing.repository.FakeBooksRepository
import cn.wj.android.cashbook.core.testing.repository.FakeRecordFilterRepository
import cn.wj.android.cashbook.core.testing.repository.FakeRecordRepository
import cn.wj.android.cashbook.core.testing.repository.FakeSettingRepository
import cn.wj.android.cashbook.core.testing.repository.FakeTagRepository
import cn.wj.android.cashbook.core.testing.repository.FakeTypeRepository
import cn.wj.android.cashbook.core.testing.util.TestDispatcherRule
import cn.wj.android.cashbook.domain.usecase.GetFilteredRecordViewsUseCase
import cn.wj.android.cashbook.domain.usecase.GetSearchRecordViewsUseCase
import cn.wj.android.cashbook.domain.usecase.RecordModelTransToViewsUseCase
import cn.wj.android.cashbook.feature.records.model.SearchFilterEvent
import cn.wj.android.cashbook.feature.records.model.SearchFilterMessage
import cn.wj.android.cashbook.feature.records.model.SearchFilterSummaryState
import cn.wj.android.cashbook.feature.records.model.SearchMode
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class SearchFilterStateTest {
    @get:Rule val dispatcherRule = TestDispatcherRule()
    private lateinit var f: Fixture

    @Before fun setup() { f = Fixture() }

    @After fun tearDown() { f.close() }

    @Test
    fun initial_doesNotQueryAll() = runTest {
        activate()
        runCurrent()
        assertThat(f.vm.filterState.value.mode).isEqualTo(SearchMode.Initial)
        assertThat(f.filters.recordRequests).isEmpty()
        assertThat(f.filters.summaryRequests).isEmpty()
    }

    @Test
    fun typing_doesNotSubmit() = runTest {
        activate()
        f.vm.onFilterEvent(SearchFilterEvent.UpdateQueryDraft("午餐"))
        advanceTimeBy(1000L)
        runCurrent()
        assertThat(f.vm.filterState.value.submittedKeyword).isEmpty()
        assertThat(f.vm.filterState.value.mode).isEqualTo(SearchMode.Initial)
        assertThat(f.filters.recordRequests).isEmpty()
        assertThat(f.records.searchHistoryListData.first()).isEmpty()
    }

    @Test
    fun resetThenApply_unrestrictedIsExplicit() = runTest {
        activate()
        f.vm.onFilterEvent(SearchFilterEvent.OpenDrawer)
        f.vm.onFilterEvent(SearchFilterEvent.ApplyDrawer)
        runCurrent()
        assertThat(f.vm.filterState.value.mode).isEqualTo(SearchMode.Filter)
        assertThat(f.filters.recordRequests.last().categories.isUnrestricted).isTrue()
        assertThat(f.filters.recordRequests.last().bookId).isEqualTo(1L)
        assertThat(ready().summary).isEqualTo(RecordFilterSummary(2L, 200L, 100L))
    }

    @Test
    fun keyword_ignoresPreviouslyAppliedFilter() = runTest {
        activate()
        applyTag(41L)
        val requests = f.filters.recordRequests.size
        f.vm.onKeywordChange("  午餐  ")
        runCurrent()
        assertThat(f.vm.filterState.value.mode).isEqualTo(SearchMode.Keyword)
        assertThat(f.vm.filterState.value.submittedKeyword).isEqualTo("  午餐  ")
        assertThat(f.filters.recordRequests).hasSize(requests)
        assertThat(f.vm.filterSummary.value).isEqualTo(SearchFilterSummaryState.Hidden)
    }

    @Test
    fun clearKeyword_restoresFilter() = runTest {
        activate()
        applyTag(41L)
        f.vm.onKeywordChange("午餐")
        f.vm.onFilterEvent(SearchFilterEvent.ClearSubmittedKeyword)
        runCurrent()
        assertThat(f.vm.filterState.value.mode).isEqualTo(SearchMode.Filter)
        assertThat(f.vm.filterState.value.appliedFilter!!.tagIds).containsExactly(41L)
        assertThat(ready().summary).isEqualTo(RecordFilterSummary(1L, 0L, 100L))
    }

    @Test
    fun clearingSearch_doesNotCancelOriginalHistoryDelay() = runTest {
        activate()
        f.vm.onKeywordChange("午餐")
        advanceTimeBy(250L)
        f.vm.onFilterEvent(SearchFilterEvent.ClearSubmittedKeyword)
        advanceTimeBy(249L)
        runCurrent()
        assertThat(f.records.searchHistoryListData.first()).isEmpty()
        advanceTimeBy(1L)
        runCurrent()
        assertThat(f.records.searchHistoryListData.first()).containsExactly("午餐")
    }

    @Test
    fun cancelDraft_doesNotChangeResults() = runTest {
        activate()
        applyTag(41L)
        val revision = f.vm.filterState.value.queryContext.criteriaRevision
        f.vm.onFilterEvent(SearchFilterEvent.OpenDrawer)
        f.vm.onFilterEvent(SearchFilterEvent.ToggleTag(42L))
        f.vm.onFilterEvent(SearchFilterEvent.CancelDrawer)
        runCurrent()
        assertThat(f.vm.filterState.value.appliedFilter!!.tagIds).containsExactly(41L)
        assertThat(f.vm.filterState.value.queryContext.criteriaRevision).isEqualTo(revision)
        assertThat(ready().summary).isEqualTo(RecordFilterSummary(1L, 0L, 100L))
    }

    @Test
    fun reset_changesOnlyDraftUntilExplicitApply() = runTest {
        activate()
        applyTag(41L)
        val context = f.vm.filterState.value.queryContext
        f.vm.onFilterEvent(SearchFilterEvent.OpenDrawer)
        f.vm.onFilterEvent(SearchFilterEvent.ResetDrawer)
        assertThat(f.vm.filterState.value.drawerDraft!!.tagIds).isEmpty()
        assertThat(f.vm.filterState.value.appliedFilter!!.tagIds).containsExactly(41L)
        assertThat(f.vm.filterState.value.queryContext).isEqualTo(context)
        f.vm.onFilterEvent(SearchFilterEvent.ApplyDrawer)
        runCurrent()
        assertThat(ready().summary.totalCount).isEqualTo(2L)
    }

    @Test
    fun invalidDate_keepsAppliedFilterAndDrawerForCorrection() = runTest {
        activate()
        applyTag(41L)
        f.vm.onFilterEvent(SearchFilterEvent.OpenDrawer)
        f.vm.onFilterEvent(SearchFilterEvent.UpdateDate(DateSelectionEntity.DateRange(LocalDate.of(2024, 2, 2), LocalDate.of(2024, 2, 1))))
        f.vm.onFilterEvent(SearchFilterEvent.ApplyDrawer)
        assertThat(f.vm.filterState.value.message).isEqualTo(SearchFilterMessage.INVALID_DATE)
        assertThat(f.vm.filterState.value.drawerDraft).isNotNull()
        assertThat(f.vm.filterState.value.appliedFilter!!.tagIds).containsExactly(41L)
    }

    @Test
    fun deletedTag_remainsAConstraintInsteadOfBecomingUnrestricted() = runTest {
        activate()
        applyTag(41L)
        f.tags.deleteTag(createTagModel(id = 41L))
        runCurrent()
        assertThat(f.vm.filterState.value.appliedFilter!!.tagIds).containsExactly(41L)
        assertThat(ready().summary.totalCount).isEqualTo(0L)
    }

    @Test
    fun newCriteria_usesFreshPagingCacheAndRejectsLateOldPages() = runTest {
        val old = CompletableDeferred<PagingData<cn.wj.android.cashbook.core.model.model.RecordViewsModel>>()
        f.filters.recordsProvider = { request ->
            if (request.tagIds == setOf(41L)) {
                flow { emit(old.await()) }
            } else {
                flowOf(PagingData.from(listOf(createRecordViewsModel(id = 99L, type = f.income, amount = 200L, finalAmount = 200L))))
            }
        }
        activate()
        applyTag(41L)
        val oldSource = f.vm.recordSources.value
        f.vm.onFilterEvent(SearchFilterEvent.OpenDrawer)
        f.vm.onFilterEvent(SearchFilterEvent.ToggleTag(41L))
        f.vm.onFilterEvent(SearchFilterEvent.ToggleTag(42L))
        f.vm.onFilterEvent(SearchFilterEvent.ApplyDrawer)
        val freshSource = f.vm.recordSources.value
        assertThat(freshSource.data).isNotSameInstanceAs(oldSource.data)
        val pages = CompletableDeferred<List<RecordViewsEntity>>()
        val presenter = object : PagingDataPresenter<RecordViewsEntity>(dispatcherRule.testDispatcher) {
            override suspend fun presentPagingDataEvent(event: PagingDataEvent<RecordViewsEntity>) = Unit
        }
        presenter.addOnPagesUpdatedListener { pages.complete(presenter.snapshot().items) }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { freshSource.data.collectLatest { presenter.collectFrom(it) } }
        assertThat(pages.await().map { it.id }).containsExactly(99L)
        old.complete(PagingData.from(listOf(createRecordViewsModel(id = 1L, type = f.child))))
        runCurrent()
        assertThat(presenter.snapshot().items.map { it.id }).containsExactly(99L)
        assertThat(freshSource.context).isEqualTo(f.vm.filterState.value.queryContext)
    }

    @Test
    fun nestedCategoryCancel_keepsDrawerDraft() = runTest {
        activate()
        f.vm.onFilterEvent(SearchFilterEvent.OpenDrawer)
        f.vm.onFilterEvent(SearchFilterEvent.OpenCategories)
        f.vm.onFilterEvent(SearchFilterEvent.ToggleCategoryItem(11L, true))
        f.vm.onFilterEvent(SearchFilterEvent.CancelCategories)
        assertThat(f.vm.filterState.value.drawerDraft!!.categories).isEqualTo(CategoryFilterSelection())
        assertThat(f.vm.filterState.value.categoryDraft).isNull()
        f.vm.onFilterEvent(SearchFilterEvent.OpenCategories)
        f.vm.onFilterEvent(SearchFilterEvent.ToggleCategoryGroup(10L, true))
        f.vm.onFilterEvent(SearchFilterEvent.ConfirmCategories)
        assertThat(f.vm.filterState.value.drawerDraft!!.categories.wholeGroupIds).containsExactly(10L)
        assertThat(f.vm.filterState.value.appliedFilter).isNull()
    }

    @Test
    fun incomeSelection_prunedWhenTypesBecomeExpenditure() = runTest {
        activate()
        f.vm.onFilterEvent(SearchFilterEvent.OpenDrawer)
        f.vm.onFilterEvent(SearchFilterEvent.OpenCategories)
        f.vm.onFilterEvent(SearchFilterEvent.ChangeCategoryBrowser(RecordTypeCategoryEnum.INCOME))
        f.vm.onFilterEvent(SearchFilterEvent.ToggleCategoryItem(21L, true))
        f.vm.onFilterEvent(SearchFilterEvent.ConfirmCategories)
        f.vm.onFilterEvent(SearchFilterEvent.UpdateTypes(setOf(RecordTypeCategoryEnum.EXPENDITURE)))
        assertThat(f.vm.filterState.value.drawerDraft!!.categories.exactTypeIds).isEmpty()
        assertThat(f.vm.filterState.value.removedTypeIds).containsExactly(21L)
    }

    @Test
    fun allTypes_doesNotRemoveCompatibleSelection() = runTest {
        activate()
        f.vm.onFilterEvent(SearchFilterEvent.OpenDrawer)
        f.vm.onFilterEvent(SearchFilterEvent.OpenCategories)
        f.vm.onFilterEvent(SearchFilterEvent.ToggleCategoryItem(11L, true))
        f.vm.onFilterEvent(SearchFilterEvent.ConfirmCategories)
        f.vm.onFilterEvent(SearchFilterEvent.UpdateTypes(emptySet()))
        assertThat(f.vm.filterState.value.drawerDraft!!.categories.exactTypeIds).containsExactly(11L)
    }

    @Test
    fun hiddenTags_remainSelectableAndApplied() = runTest {
        activate()
        applyTag(42L)
        assertThat(f.vm.filterState.value.tags.first { it.id == 42L }.invisible).isTrue()
        assertThat(ready().summary).isEqualTo(RecordFilterSummary(1L, 200L, 0L))
    }

    @Test
    fun bookSwitch_preservesFilterButScopesRecords() = runTest {
        activate()
        applyTag(41L)
        f.books.setCurrentBook(createBooksModel(id = 2L))
        runCurrent()
        assertThat(f.vm.filterState.value.appliedFilter!!.tagIds).containsExactly(41L)
        assertThat(f.filters.recordRequests.last().bookId).isEqualTo(2L)
        assertThat(ready().summary).isEqualTo(RecordFilterSummary(1L, 0L, 300L))
    }

    @Test
    fun invalidApplication_keepsOriginalCriteriaAndResults() = runTest {
        activate()
        applyTag(41L)
        val context = f.vm.filterState.value.queryContext
        f.filters.validationOverride = RecordFilterValidation.TooManyParameters(1000, 900)
        f.vm.onFilterEvent(SearchFilterEvent.OpenDrawer)
        f.vm.onFilterEvent(SearchFilterEvent.ToggleTag(42L))
        f.vm.onFilterEvent(SearchFilterEvent.ApplyDrawer)
        assertThat(f.vm.filterState.value.queryContext).isEqualTo(context)
        assertThat(f.vm.filterState.value.appliedFilter!!.tagIds).containsExactly(41L)
        assertThat(f.vm.filterState.value.message).isEqualTo(SearchFilterMessage.TOO_MANY_CONDITIONS)
    }

    @Test
    fun independentTypeVersion_refreshesTreeAndQuery() = runTest {
        activate()
        f.vm.onFilterEvent(SearchFilterEvent.OpenDrawer)
        f.vm.onFilterEvent(SearchFilterEvent.OpenCategories)
        f.vm.onFilterEvent(SearchFilterEvent.ToggleCategoryGroup(10L, true))
        f.vm.onFilterEvent(SearchFilterEvent.ConfirmCategories)
        f.vm.onFilterEvent(SearchFilterEvent.ApplyDrawer)
        runCurrent()
        val revision = f.vm.filterState.value.queryContext.criteriaRevision
        val requestCount = f.filters.recordRequests.size
        f.types.update(f.child.copy(parentId = 40L))
        runCurrent()
        assertThat(f.vm.filterState.value.catalog.first { it.id == 11L }.parentId).isEqualTo(40L)
        assertThat(ready().summary.totalCount).isEqualTo(0L)
        assertThat(f.vm.filterState.value.queryContext.criteriaRevision).isEqualTo(revision)
        assertThat(f.filters.recordRequests).hasSize(requestCount)
    }

    @Test
    fun oldSummary_cannotOverwriteNewCriteria() = runTest {
        val old = CompletableDeferred<RecordFilterSummary>()
        val fresh = CompletableDeferred<RecordFilterSummary>()
        f.filters.summaryProvider = { request -> flow { emit(if (request.tagIds == setOf(41L)) old.await() else fresh.await()) } }
        activate()
        applyTag(41L)
        f.vm.onFilterEvent(SearchFilterEvent.OpenDrawer)
        f.vm.onFilterEvent(SearchFilterEvent.ToggleTag(41L))
        f.vm.onFilterEvent(SearchFilterEvent.ToggleTag(42L))
        f.vm.onFilterEvent(SearchFilterEvent.ApplyDrawer)
        fresh.complete(RecordFilterSummary(1L, 200L, 0L))
        runCurrent()
        old.complete(RecordFilterSummary(1L, 0L, 100L))
        runCurrent()
        assertThat(ready().context.request!!.tagIds).containsExactly(42L)
        assertThat(ready().summary).isEqualTo(RecordFilterSummary(1L, 200L, 0L))
    }

    @Test
    fun retry_oldFailureCannotReplaceNewSuccessForSameCriteria() = runTest {
        val old = CompletableDeferred<Unit>()
        var attempts = 0
        f.filters.summaryProvider = {
            if (attempts++ == 0) {
                flow { withContext(NonCancellable) { old.await(); error("old attempt failed") } }
            } else {
                flowOf(RecordFilterSummary(1L, 0L, 100L))
            }
        }
        try {
            activate()
            applyTag(41L)
            f.vm.onFilterEvent(SearchFilterEvent.Retry)
            runCurrent()
            assertThat(ready().summary).isEqualTo(RecordFilterSummary(1L, 0L, 100L))
            old.complete(Unit)
            runCurrent()
            assertThat(ready().summary).isEqualTo(RecordFilterSummary(1L, 0L, 100L))
        } finally {
            old.complete(Unit)
        }
    }

    @Test
    fun codec_roundTripKeepsAppliedModeAndUnsubmittedDraft() = runTest {
        activate()
        applyTag(41L)
        f.vm.onFilterEvent(SearchFilterEvent.UpdateQueryDraft("午餐, | 未提交"))
        val handle = SavedStateHandle()
        SearchFilterStateCodec.write(handle, f.vm.filterState.value)
        val restored = SearchFilterStateCodec.read(handle)
        assertThat(restored.mode).isEqualTo(SearchMode.Filter)
        assertThat(restored.appliedFilter!!.tagIds).containsExactly(41L)
        assertThat(restored.queryDraft).isEqualTo("午餐, | 未提交")
        assertThat(restored.submittedKeyword).isEmpty()
    }

    @Test
    fun codec_unknownTypeDoesNotBroadenIntoUnrestrictedFilter() {
        val handle = SavedStateHandle(mapOf(SearchFilterStateCodec.KEY to arrayListOf("1", "", "", "true", "全部", "999", "", "", "", "")))
        val restored = SearchFilterStateCodec.read(handle)
        assertThat(restored.mode).isEqualTo(SearchMode.Initial)
        assertThat(restored.appliedFilter).isNull()
        assertThat(restored.message).isEqualTo(SearchFilterMessage.RESTORE_FAILED)
    }

    private fun TestScope.activate() {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { f.vm.recordListData.collect() }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { f.vm.filterSummary.collect() }
    }

    private fun applyTag(id: Long) {
        f.vm.onFilterEvent(SearchFilterEvent.OpenDrawer)
        f.vm.onFilterEvent(SearchFilterEvent.ToggleTag(id))
        f.vm.onFilterEvent(SearchFilterEvent.ApplyDrawer)
    }

    private fun ready(): SearchFilterSummaryState.Ready {
        assertThat(f.vm.filterSummary.value).isInstanceOf(SearchFilterSummaryState.Ready::class.java)
        return f.vm.filterSummary.value as SearchFilterSummaryState.Ready
    }

    private inner class Fixture(handle: SavedStateHandle = SavedStateHandle()) {
        val records = FakeRecordRepository()
        val types = FakeTypeRepository()
        val tags = FakeTagRepository()
        val books = FakeBooksRepository()
        val settings = FakeSettingRepository()
        val child = createRecordTypeModel(id = 11L, parentId = 10L, typeLevel = TypeLevelEnum.SECOND, iconName = "vector_type_three_meals_24")
        val income = createRecordTypeModel(id = 21L, parentId = 20L, typeLevel = TypeLevelEnum.SECOND, typeCategory = RecordTypeCategoryEnum.INCOME)
        init {
            types.addType(createRecordTypeModel(id = 10L))
            types.addType(child)
            types.addType(createRecordTypeModel(id = 20L, typeCategory = RecordTypeCategoryEnum.INCOME))
            types.addType(income)
            types.addType(createRecordTypeModel(id = 40L))
            tags.addTag(createTagModel(id = 41L))
            tags.addTag(createTagModel(id = 42L, invisible = true))
            books.addBook(createBooksModel(id = 1L))
            books.addBook(createBooksModel(id = 2L))
        }
        val filters = FakeRecordFilterRepository(types.allTypeListData, tags.tagListData).apply {
            setRecords(
                listOf(
                    createRecordViewsModel(id = 1L, type = child, amount = 100L, finalAmount = 100L, relatedTags = listOf(createTagModel(id = 41L))),
                    createRecordViewsModel(id = 2L, type = income, amount = 200L, finalAmount = 200L, relatedTags = listOf(createTagModel(id = 42L, invisible = true))),
                    createRecordViewsModel(id = 3L, booksId = 2L, type = child, amount = 300L, finalAmount = 300L, relatedTags = listOf(createTagModel(id = 41L))),
                ),
            )
        }
        private val mapper = RecordModelTransToViewsUseCase(records, types, FakeAssetRepository(), tags, dispatcherRule.testDispatcher)
        val vm = SearchViewModel(
            recordRepository = records,
            getSearchRecordViewsUseCase = GetSearchRecordViewsUseCase(records, mapper, dispatcherRule.testDispatcher),
            getFilteredRecordViewsUseCase = GetFilteredRecordViewsUseCase(filters),
            recordFilterRepository = filters,
            typeRepository = types,
            tagRepository = tags,
            booksRepository = books,
            settingRepository = settings,
            savedStateHandle = handle,
        )
        fun close() { vm.viewModelScope.cancel() }
    }
}
