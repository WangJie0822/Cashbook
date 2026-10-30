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

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.cachedIn
import cn.wj.android.cashbook.core.common.DEFAULT_PAGE_SIZE
import cn.wj.android.cashbook.core.common.ext.logger
import cn.wj.android.cashbook.core.common.model.recordDataVersion
import cn.wj.android.cashbook.core.data.repository.BooksRepository
import cn.wj.android.cashbook.core.data.repository.RecordFilterRepository
import cn.wj.android.cashbook.core.data.repository.RecordFilterValidation
import cn.wj.android.cashbook.core.data.repository.RecordRepository
import cn.wj.android.cashbook.core.data.repository.SettingRepository
import cn.wj.android.cashbook.core.data.repository.TagRepository
import cn.wj.android.cashbook.core.data.repository.TypeRepository
import cn.wj.android.cashbook.core.model.entity.RecordViewsEntity
import cn.wj.android.cashbook.core.model.entity.normalizeMonthStartDay
import cn.wj.android.cashbook.core.model.model.RecordFilter
import cn.wj.android.cashbook.core.model.model.changeFilterTypes
import cn.wj.android.cashbook.core.model.model.selectCategoryGroup
import cn.wj.android.cashbook.core.model.model.selectCategoryItem
import cn.wj.android.cashbook.core.model.model.toRequest
import cn.wj.android.cashbook.domain.usecase.GetFilteredRecordViewsUseCase
import cn.wj.android.cashbook.domain.usecase.GetSearchRecordViewsUseCase
import cn.wj.android.cashbook.feature.records.model.CategorySelectionKind
import cn.wj.android.cashbook.feature.records.model.SearchFilterEvent
import cn.wj.android.cashbook.feature.records.model.SearchFilterMessage
import cn.wj.android.cashbook.feature.records.model.SearchFilterState
import cn.wj.android.cashbook.feature.records.model.SearchFilterSummaryState
import cn.wj.android.cashbook.feature.records.model.SearchMode
import cn.wj.android.cashbook.feature.records.model.SearchQueryContext
import cn.wj.android.cashbook.feature.records.model.SearchRecordsSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val recordRepository: RecordRepository,
    private val getSearchRecordViewsUseCase: GetSearchRecordViewsUseCase,
    private val getFilteredRecordViewsUseCase: GetFilteredRecordViewsUseCase,
    private val recordFilterRepository: RecordFilterRepository,
    private val typeRepository: TypeRepository,
    private val tagRepository: TagRepository,
    private val booksRepository: BooksRepository,
    private val settingRepository: SettingRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    var viewRecordData by mutableStateOf<RecordViewsEntity?>(null)
        private set

    private val _filterState = MutableStateFlow(SearchFilterStateCodec.read(savedStateHandle))
    val filterState = _filterState.asStateFlow()
    private val _filterSummary = MutableStateFlow<SearchFilterSummaryState>(SearchFilterSummaryState.Hidden)
    val filterSummary = _filterSummary.asStateFlow()
    private val _recordSources = MutableStateFlow(SearchRecordsSource())
    val recordSources = _recordSources.asStateFlow()
    val recordListData: Flow<PagingData<RecordViewsEntity>> = recordSources.flatMapLatest { it.data }

    val searchHistoryListData = recordRepository.searchHistoryListData.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000L),
        emptyList(),
    )

    private var searchHistoryJob: Job? = null
    private var queryScope: CoroutineScope? = null
    private var attempt = 0L
    private val metadataJobs = mutableListOf<Job>()

    init { observeMetadata() }

    private fun observeMetadata() {
        metadataJobs.forEach { it.cancel() }
        metadataJobs.clear()
        fun <T> observe(flow: Flow<T>, failed: (SearchFilterState) -> SearchFilterState, update: (SearchFilterState, T) -> SearchFilterState) {
            metadataJobs += viewModelScope.launch {
                flow.catch { error ->
                    if (error is CancellationException) throw error
                    updateState(failed(filterState.value).copy(message = SearchFilterMessage.METADATA_FAILED))
                }.collect { updateState(update(filterState.value, it)) }
            }
        }
        observe(booksRepository.currentBook, { it.copy(bookId = null) }) { state, book -> state.copy(bookId = book.id, bookName = book.name) }
        observe(settingRepository.recordSettingsModel, { it.copy(settingsLoaded = false) }) { state, settings -> state.copy(monthStartDay = normalizeMonthStartDay(settings.monthStartDay), settingsLoaded = true) }
        observe(typeRepository.allTypeListData, { it.copy(catalogLoaded = false) }) { state, types -> state.copy(catalog = types, catalogLoaded = true) }
        observe(tagRepository.tagListData, { it.copy(tagsLoaded = false) }) { state, tags -> state.copy(tags = tags, tagsLoaded = true) }
    }

    /** 条件与上下文原子发布；目录改名、数据失效不改变滚动位置标识。 */
    private fun updateState(candidate: SearchFilterState, retry: Boolean = false) {
        val mode = if (candidate.submittedKeyword.isNotBlank()) SearchMode.Keyword else if (candidate.appliedFilter != null) SearchMode.Filter else SearchMode.Initial
        val activeMode = if (candidate.bookId != null && candidate.settingsLoaded) mode else SearchMode.Initial
        val request = if (activeMode == SearchMode.Filter) runCatching { candidate.appliedFilter!!.toRequest(candidate.bookId!!, candidate.monthStartDay) }.getOrNull() else null
        val old = filterState.value.queryContext
        val key = SearchQueryContext(old.criteriaRevision, candidate.bookId ?: -1L, activeMode, request, candidate.submittedKeyword.takeIf { activeMode == SearchMode.Keyword }.orEmpty())
        val changed = key != old
        val context = key.copy(criteriaRevision = old.criteriaRevision + if (changed) 1 else 0)
        _filterState.value = candidate.copy(mode = mode, queryContext = context, recordQueryFailure = if (changed || retry) null else candidate.recordQueryFailure)
        SearchFilterStateCodec.write(savedStateHandle, _filterState.value)
        if (changed || retry) startQuery(context)
    }

    private fun startQuery(context: SearchQueryContext) {
        queryScope?.cancel()
        val epoch = ++attempt
        val scope = CoroutineScope(viewModelScope.coroutineContext + SupervisorJob(viewModelScope.coroutineContext[Job]))
        queryScope = scope
        fun isCurrent() = epoch == attempt && context == filterState.value.queryContext
        _filterSummary.value = if (context.mode == SearchMode.Filter) SearchFilterSummaryState.Loading(context) else SearchFilterSummaryState.Hidden
        val request = context.request
        val validation = if (context.mode == SearchMode.Filter && request != null) recordFilterRepository.validate(request) else RecordFilterValidation.Valid
        val pages = when {
            validation != RecordFilterValidation.Valid || (context.mode == SearchMode.Filter && request == null) -> {
                _filterSummary.value = SearchFilterSummaryState.Failed(context, "invalid_filter")
                _filterState.value = filterState.value.copy(recordQueryFailure = context)
                flowOf(PagingData.empty())
            }
            context.mode == SearchMode.Filter -> getFilteredRecordViewsUseCase(request!!)
            context.mode == SearchMode.Keyword -> recordDataVersion.flatMapLatest {
                Pager(PagingConfig(pageSize = DEFAULT_PAGE_SIZE, initialLoadSize = DEFAULT_PAGE_SIZE)) {
                    SearchRecordPagingSource(context.keyword, getSearchRecordViewsUseCase)
                }.flow
            }
            else -> flowOf(PagingData.empty())
        }.catch { error ->
            if (error is CancellationException) throw error
            if (isCurrent()) _filterState.value = filterState.value.copy(recordQueryFailure = context, message = SearchFilterMessage.QUERY_FAILED)
            emit(PagingData.empty())
        }.cachedIn(scope)
        _recordSources.value = SearchRecordsSource(context, pages)
        if (context.mode == SearchMode.Filter && request != null && validation == RecordFilterValidation.Valid) {
            scope.launch {
                try {
                    recordFilterRepository.observeSummary(request).collect { summary ->
                        if (isCurrent()) _filterSummary.value = SearchFilterSummaryState.Ready(context, summary)
                    }
                } catch (error: Throwable) {
                    if (error is CancellationException) throw error
                    if (isCurrent()) _filterSummary.value = SearchFilterSummaryState.Failed(context, "query_failed")
                }
            }
        }
    }

    fun onKeywordChange(keyword: String) {
        updateState(filterState.value.copy(queryDraft = keyword, submittedKeyword = keyword))
        searchHistoryJob?.cancel()
        searchHistoryJob = viewModelScope.launch { delay(500L); recordRepository.addSearchHistory(keyword) }
    }

    fun onFilterEvent(event: SearchFilterEvent) {
        val state = filterState.value
        val draft = state.drawerDraft
        val selection = state.categoryDraft
        val next = when (event) {
            SearchFilterEvent.OpenDrawer -> if (state.metadataReady && state.mode != SearchMode.Keyword) state.copy(drawerDraft = state.appliedFilter ?: RecordFilter(), categoryDraft = null, message = null, removedTypeIds = emptySet()) else state
            SearchFilterEvent.CancelDrawer -> state.copy(drawerDraft = null, categoryDraft = null, message = null, removedTypeIds = emptySet())
            SearchFilterEvent.ResetDrawer -> if (draft != null) state.copy(drawerDraft = RecordFilter(), categoryDraft = null, message = null, removedTypeIds = emptySet()) else state
            SearchFilterEvent.ApplyDrawer -> {
                if (draft == null || !state.metadataReady) return
                val request = runCatching { draft.toRequest(state.bookId!!, state.monthStartDay) }.getOrNull()
                val validation = request?.let(recordFilterRepository::validate) ?: RecordFilterValidation.InvalidRange
                if (validation != RecordFilterValidation.Valid) {
                    updateState(state.copy(message = if (validation == RecordFilterValidation.InvalidRange) SearchFilterMessage.INVALID_DATE else SearchFilterMessage.TOO_MANY_CONDITIONS))
                    return
                }
                state.copy(appliedFilter = draft, drawerDraft = null, categoryDraft = null, message = null)
            }
            is SearchFilterEvent.UpdateDate -> if (draft != null) state.copy(drawerDraft = draft.copy(dateSelection = event.date), message = null) else state
            is SearchFilterEvent.UpdateTypes -> if (draft != null) {
                val updated = changeFilterTypes(draft, event.types, state.catalog)
                state.copy(
                    drawerDraft = updated.filter,
                    removedTypeIds = updated.removedTypeIds,
                    message = SearchFilterMessage.INCOMPATIBLE_CATEGORIES_REMOVED.takeIf { updated.removedTypeIds.isNotEmpty() },
                )
            } else {
                state
            }
            is SearchFilterEvent.ToggleTag -> if (draft != null) state.copy(drawerDraft = draft.copy(tagIds = if (event.id in draft.tagIds) draft.tagIds - event.id else draft.tagIds + event.id)) else state
            SearchFilterEvent.OpenCategories -> if (draft != null) {
                val allowed = draft.typeCategories
                val browse = state.categoryBrowseType.takeIf { allowed.isEmpty() || it in allowed } ?: allowed.first()
                state.copy(categoryDraft = draft.categories, categoryBrowseType = browse, categoryBrowseParentId = null, categorySearchQuery = "")
            } else {
                state
            }
            SearchFilterEvent.CancelCategories -> state.copy(categoryDraft = null)
            SearchFilterEvent.ConfirmCategories -> if (draft != null && selection != null) state.copy(drawerDraft = draft.copy(categories = selection), categoryDraft = null) else state
            is SearchFilterEvent.ToggleCategoryGroup -> if (selection != null && draft != null && state.catalog.any { it.id == event.id && it.parentId == -1L && (draft.typeCategories.isEmpty() || it.typeCategory in draft.typeCategories) }) {
                state.copy(categoryDraft = selectCategoryGroup(selection, event.id, event.selected, state.catalog))
            } else {
                state
            }
            is SearchFilterEvent.ToggleCategoryItem -> {
                val type = state.catalog.find { it.id == event.id }
                if (selection != null && draft != null && type != null && (draft.typeCategories.isEmpty() || type.typeCategory in draft.typeCategories)) state.copy(categoryDraft = selectCategoryItem(selection, type, event.selected)) else state
            }
            is SearchFilterEvent.ChangeCategoryBrowser -> if (draft != null && (draft.typeCategories.isEmpty() || event.type in draft.typeCategories)) state.copy(categoryBrowseType = event.type, categoryBrowseParentId = event.parentId, categorySearchQuery = "") else state
            is SearchFilterEvent.ChangeCategorySearch -> state.copy(categorySearchQuery = event.keyword)
            is SearchFilterEvent.RemoveCategory -> {
                val current = selection ?: draft?.categories ?: return
                val categories = when (event.kind) {
                    CategorySelectionKind.EXACT -> current.copy(exactTypeIds = current.exactTypeIds - event.id)
                    CategorySelectionKind.GROUP -> selectCategoryGroup(current, event.id, false, state.catalog)
                    CategorySelectionKind.EXCLUDED -> current.copy(excludedTypeIds = current.excludedTypeIds - event.id)
                }
                if (selection != null) state.copy(categoryDraft = categories) else state.copy(drawerDraft = draft!!.copy(categories = categories))
            }
            is SearchFilterEvent.UpdateQueryDraft -> state.copy(queryDraft = event.keyword)
            SearchFilterEvent.ClearSubmittedKeyword -> state.copy(queryDraft = "", submittedKeyword = "")
            SearchFilterEvent.Retry -> {
                if (!state.metadataReady) observeMetadata()
                updateState(filterState.value.copy(message = null), retry = true)
                return
            }
        }
        updateState(next)
    }

    fun showRecordDetailSheet(item: RecordViewsEntity) { viewRecordData = item }
    fun dismissRecordDetailSheet() { viewRecordData = null }
    fun clearSearchHistory() { viewModelScope.launch { recordRepository.clearSearchHistory() } }
}

private class SearchRecordPagingSource(
    private val keyword: String,
    private val getSearchRecordViewsUseCase: GetSearchRecordViewsUseCase,
) : PagingSource<Int, RecordViewsEntity>() {
    override fun getRefreshKey(state: PagingState<Int, RecordViewsEntity>): Int? = null
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, RecordViewsEntity> {
        return try {
            val page = params.key ?: 0
            val items = getSearchRecordViewsUseCase(keyword, page, params.loadSize)
            LoadResult.Page(items, if (page > 0) page - 1 else null, if (items.isNotEmpty()) page + 1 else null)
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            logger().e(error, "load()")
            LoadResult.Error(error)
        }
    }
}
