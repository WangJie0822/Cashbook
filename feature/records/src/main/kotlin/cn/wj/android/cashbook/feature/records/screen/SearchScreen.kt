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

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ElevatedSuggestionChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import cn.wj.android.cashbook.core.design.component.CbCircularProgressIndicator
import cn.wj.android.cashbook.core.design.component.CbIconButton
import cn.wj.android.cashbook.core.design.component.CbModalBottomSheet
import cn.wj.android.cashbook.core.design.component.CbTextButton
import cn.wj.android.cashbook.core.design.component.Empty
import cn.wj.android.cashbook.core.design.component.Footer
import cn.wj.android.cashbook.core.design.icon.CbIcons
import cn.wj.android.cashbook.core.design.theme.rememberHapticOnClick
import cn.wj.android.cashbook.core.model.entity.RecordViewsEntity
import cn.wj.android.cashbook.core.model.model.RecordFilter
import cn.wj.android.cashbook.core.model.model.RecordSearchStatistics
import cn.wj.android.cashbook.core.ui.R
import cn.wj.android.cashbook.feature.records.model.SearchFilterEvent
import cn.wj.android.cashbook.feature.records.model.SearchFilterMessage
import cn.wj.android.cashbook.feature.records.model.SearchFilterState
import cn.wj.android.cashbook.feature.records.model.SearchFilterSummaryState
import cn.wj.android.cashbook.feature.records.model.SearchMode
import cn.wj.android.cashbook.feature.records.model.SearchQueryContext
import cn.wj.android.cashbook.feature.records.view.SearchFilterConstraintNotice
import cn.wj.android.cashbook.feature.records.view.SearchFilterDrawer
import cn.wj.android.cashbook.feature.records.view.SearchFilterRecordItem
import cn.wj.android.cashbook.feature.records.view.SearchFilterSummary
import cn.wj.android.cashbook.feature.records.view.categorySelectionSummary
import cn.wj.android.cashbook.feature.records.view.filterMessageText
import cn.wj.android.cashbook.feature.records.view.filterTypeLabel
import cn.wj.android.cashbook.feature.records.view.rememberSearchDistributionState
import cn.wj.android.cashbook.feature.records.view.searchResultDistribution
import cn.wj.android.cashbook.feature.records.viewmodel.SearchViewModel
import cn.wj.android.cashbook.feature.records.R as FilterR

@Composable
internal fun SearchRoute(
    onRequestNaviToEditRecord: (Long) -> Unit,
    onRequestNaviToAssetInfo: (Long) -> Unit,
    onRequestPopBackStack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = hiltViewModel(),
    recordDetailContent: @Composable (RecordViewsEntity) -> Unit = { record ->
        RecordDetailSheetContent(record, onRequestNaviToEditRecord, onRequestNaviToAssetInfo, viewModel::dismissRecordDetailSheet)
    },
) {
    val history by viewModel.searchHistoryListData.collectAsStateWithLifecycle()
    val state by viewModel.filterState.collectAsStateWithLifecycle()
    val summary by viewModel.filterSummary.collectAsStateWithLifecycle()
    val source by viewModel.recordSources.collectAsStateWithLifecycle()
    val records = key(source.data) { source.data.collectAsLazyPagingItems() }
    SearchScreen(
        viewRecord = viewModel.viewRecordData,
        onRequestShowRecordDetailSheet = viewModel::showRecordDetailSheet,
        onRequestDismissBottomSheet = viewModel::dismissRecordDetailSheet,
        searchHistoryList = history,
        onRequestClearSearchHistory = viewModel::clearSearchHistory,
        recordList = records,
        onKeywordChange = viewModel::onKeywordChange,
        onRequestNaviToEditRecord = onRequestNaviToEditRecord,
        onRequestNaviToAssetInfo = onRequestNaviToAssetInfo,
        onRequestPopBackStack = onRequestPopBackStack,
        filterState = state, summaryState = summary, onFilterEvent = viewModel::onFilterEvent,
        recordContext = source.context, recordDetailContent = recordDetailContent, modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun SearchScreen(
    viewRecord: RecordViewsEntity?,
    onRequestShowRecordDetailSheet: (RecordViewsEntity) -> Unit,
    onRequestDismissBottomSheet: () -> Unit,
    searchHistoryList: List<String>,
    onRequestClearSearchHistory: () -> Unit,
    recordList: LazyPagingItems<RecordViewsEntity>,
    onKeywordChange: (String) -> Unit,
    onRequestNaviToEditRecord: (Long) -> Unit,
    onRequestNaviToAssetInfo: (Long) -> Unit,
    onRequestPopBackStack: () -> Unit,
    filterState: SearchFilterState,
    summaryState: SearchFilterSummaryState,
    onFilterEvent: (SearchFilterEvent) -> Unit,
    modifier: Modifier = Modifier,
    recordContext: SearchQueryContext = filterState.queryContext,
    recordDetailContent: @Composable (RecordViewsEntity) -> Unit = { record ->
        RecordDetailSheetContent(record, onRequestNaviToEditRecord, onRequestNaviToAssetInfo, onRequestDismissBottomSheet)
    },
) {
    if (viewRecord != null) {
        CbModalBottomSheet(
            onDismissRequest = onRequestDismissBottomSheet,
            sheetState = rememberModalBottomSheetState(confirmValueChange = { if (it == SheetValue.Hidden) onRequestDismissBottomSheet(); true }),
        ) { recordDetailContent(viewRecord) }
    }

    var expanded by rememberSaveable { mutableStateOf(false) }
    val focus = LocalFocusManager.current
    val listState = rememberLazyListState()
    // 重建 ViewModel 时 revision 从头计数；保存实际条件键才能保住配置变化后的滚动位置。
    val criteriaKey = filterState.queryContext.copy(criteriaRevision = 0L).toString()
    var positionedCriteria by rememberSaveable { mutableStateOf(criteriaKey) }
    LaunchedEffect(criteriaKey) {
        if (positionedCriteria != criteriaKey) { listState.scrollToItem(0); positionedCriteria = criteriaKey }
    }
    BackHandler(enabled = expanded && filterState.drawerDraft == null) { expanded = false }
    val context = filterState.queryContext
    val filtering = filterState.mode == SearchMode.Filter
    val keywordMode = filterState.mode == SearchMode.Keyword
    val clearKeywordHint = stringResource(FilterR.string.search_filter_clear_keyword_first)
    val readySummary = (summaryState as? SearchFilterSummaryState.Ready)?.takeIf { it.context == context }
    val readyStatistics = remember(readySummary) { readySummary?.let { RecordSearchStatistics(it.summary, it.categories) } }
    val distributionState = rememberSearchDistributionState(criteriaKey, readySummary?.categories)
    val summaryFailed = (summaryState as? SearchFilterSummaryState.Failed)?.context == context
    val failed = filterState.recordQueryFailure == context || recordList.loadState.refresh is LoadState.Error || filterState.message == SearchFilterMessage.METADATA_FAILED
    val pending = filterState.mode != SearchMode.Initial && !failed &&
        (
            context.mode == SearchMode.Initial || recordContext != context || recordList.loadState.refresh is LoadState.Loading ||
                (readySummary != null && readySummary.summary.totalCount > 0L && recordList.itemCount == 0)
            )

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Box {
            Column(Modifier.fillMaxSize()) {
                SearchBar(
                    inputField = {
                        SearchBarDefaults.InputField(
                            modifier = Modifier.fillMaxWidth().testTag("search_query"),
                            query = filterState.queryDraft,
                            onQueryChange = { onFilterEvent(if (it.isEmpty()) SearchFilterEvent.ClearSubmittedKeyword else SearchFilterEvent.UpdateQueryDraft(it)) },
                            onSearch = { onKeywordChange(it); expanded = false; focus.clearFocus() },
                            expanded = expanded, onExpandedChange = { expanded = it },
                            placeholder = { Text(stringResource(R.string.record_search_hint)) },
                            leadingIcon = { CbIconButton(onClick = { if (expanded) expanded = false else onRequestPopBackStack() }, modifier = Modifier.testTag("search_back")) { Icon(CbIcons.ArrowBack, stringResource(R.string.cd_navigate_back)) } },
                            trailingIcon = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (filterState.queryDraft.isNotEmpty() || filterState.submittedKeyword.isNotEmpty()) CbIconButton(onClick = { onFilterEvent(SearchFilterEvent.ClearSubmittedKeyword); expanded = false; focus.clearFocus() }, modifier = Modifier.testTag("search_clear")) { Icon(CbIcons.Cancel, stringResource(R.string.cd_clear)) }
                                    CbTextButton(enabled = filterState.metadataReady && !keywordMode, onClick = { expanded = false; focus.clearFocus(); onFilterEvent(SearchFilterEvent.OpenDrawer) }, modifier = Modifier.testTag("search_filter_button").semantics { if (keywordMode) stateDescription = clearKeywordHint }) {
                                        Text(stringResource(FilterR.string.search_filter))
                                        if (filtering) Box(Modifier.padding(start = 4.dp).size(6.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                                    }
                                }
                            },
                        )
                    },
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) {
                    if (searchHistoryList.isEmpty()) {
                        Empty(stringResource(R.string.record_search_no_history), modifier = Modifier.align(Alignment.CenterHorizontally))
                    } else {
                        CbTextButton(onClick = onRequestClearSearchHistory) { Text(stringResource(R.string.clear)) }
                        FlowRow(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            searchHistoryList.forEach { keyword -> ElevatedSuggestionChip(onClick = { onFilterEvent(SearchFilterEvent.UpdateQueryDraft(keyword)) }, label = { Text(keyword) }) }
                        }
                    }
                }
                // 加载期间不测量一个更短的临时列表，避免恢复的滚动索引被自动夹到零。
                if (pending || failed) {
                    Column(Modifier.weight(1f).fillMaxWidth()) {
                        if (filtering) {
                            Column(Modifier.padding(16.dp)) {
                                Text(filterConditionsText(filterState.appliedFilter!!, filterState), style = MaterialTheme.typography.bodySmall)
                                SearchFilterConstraintNotice(filterState.appliedFilter, filterState)
                            }
                        }
                        if (failed) {
                            Column(Modifier.fillMaxWidth().padding(24.dp).testTag("search_query_error"), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(stringResource(if (filterState.message == SearchFilterMessage.METADATA_FAILED) FilterR.string.search_filter_metadata_failed else FilterR.string.search_filter_failed))
                                CbTextButton(onClick = { onFilterEvent(SearchFilterEvent.Retry) }, modifier = Modifier.testTag("search_retry")) { Text(stringResource(FilterR.string.search_filter_retry)) }
                            }
                        } else {
                            Row(Modifier.fillMaxWidth().padding(24.dp).testTag("search_query_loading"), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                                CbCircularProgressIndicator(Modifier.size(24.dp))
                                Text(stringResource(R.string.data_in_loading), Modifier.padding(start = 12.dp))
                            }
                        }
                    }
                } else {
                    LazyColumn(state = listState, modifier = Modifier.weight(1f).fillMaxWidth().testTag("search_results")) {
                        if (filtering) {
                            item(key = "filter_conditions") {
                                Column(Modifier.padding(16.dp)) {
                                    Text(filterConditionsText(filterState.appliedFilter!!, filterState), Modifier.testTag("filter_conditions"), style = MaterialTheme.typography.bodySmall)
                                    SearchFilterConstraintNotice(filterState.appliedFilter, filterState)
                                }
                            }
                        }
                        if (filterState.mode == SearchMode.Keyword && filterState.appliedFilter != null) {
                            item(key = "filter_paused") {
                                Text(stringResource(FilterR.string.search_filter_keyword_independent), Modifier.padding(16.dp), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        if (filterState.message == SearchFilterMessage.METADATA_FAILED || filterState.message == SearchFilterMessage.RESTORE_FAILED) {
                            item(key = "metadata_error") {
                                Column(Modifier.padding(16.dp)) {
                                    Text(filterMessageText(filterState.message), color = MaterialTheme.colorScheme.error)
                                    CbTextButton(onClick = { onFilterEvent(SearchFilterEvent.Retry) }) { Text(stringResource(FilterR.string.search_filter_retry)) }
                                }
                            }
                        }
                        if (filterState.mode != SearchMode.Initial) {
                            if (readySummary != null) {
                                item(key = "filter_summary") { SearchFilterSummary(readySummary.summary, Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) }
                                searchResultDistribution(readyStatistics!!, distributionState)
                            } else if (summaryFailed) {
                                item(key = "statistics_error") {
                                    Column(Modifier.padding(16.dp).testTag("search_statistics_error")) {
                                        Text(stringResource(FilterR.string.search_statistics_failed), color = MaterialTheme.colorScheme.error)
                                        CbTextButton(onClick = { onFilterEvent(SearchFilterEvent.Retry) }, modifier = Modifier.testTag("search_statistics_retry")) { Text(stringResource(FilterR.string.search_filter_retry)) }
                                    }
                                }
                            } else {
                                item(key = "statistics_loading") {
                                    Row(Modifier.padding(16.dp).testTag("search_statistics_loading"), verticalAlignment = Alignment.CenterVertically) {
                                        CbCircularProgressIndicator(Modifier.size(20.dp))
                                        Text(stringResource(R.string.data_in_loading), Modifier.padding(start = 12.dp))
                                    }
                                }
                            }
                        }
                        if (recordList.itemCount == 0) {
                            item(key = "empty") {
                                Empty(hintText = stringResource(if (filtering) FilterR.string.search_filter_empty else if (filterState.mode == SearchMode.Initial) FilterR.string.search_filter_initial_hint else R.string.record_search_no_data_hint), modifier = Modifier.fillMaxWidth())
                            }
                        } else {
                            items(recordList.itemCount, key = { recordList.peek(it)?.id ?: "placeholder_$it" }) { index ->
                                recordList[index]?.let { record ->
                                    if (filtering || keywordMode) {
                                        SearchFilterRecordItem(record, { onRequestShowRecordDetailSheet(record) }, Modifier.testTag("search_record_${record.id}"))
                                    } else {
                                        RecordListItem(record, modifier = Modifier.testTag("search_record_${record.id}").clickable(onClick = rememberHapticOnClick { onRequestShowRecordDetailSheet(record) }))
                                    }
                                }
                            }
                            if (recordList.loadState.append is LoadState.Error) {
                                item(key = "append_error") { CbTextButton(onClick = recordList::retry) { Text(stringResource(FilterR.string.search_filter_retry)) } }
                            } else if (recordList.loadState.append is LoadState.Loading) {
                                item(key = "append_loading") { CbCircularProgressIndicator(Modifier.padding(16.dp)) }
                            } else if (recordList.loadState.append.endOfPaginationReached) item(key = "footer") { Footer(stringResource(R.string.footer_hint_default)) }
                        }
                    }
                }
            }
            if (filterState.drawerDraft != null) {
                BoxWithConstraints(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.32f)).windowInsetsPadding(WindowInsets.safeDrawing).imePadding()) {
                    val drawerWidth = minOf(maxWidth * 0.92f, 600.dp)
                    Box(Modifier.fillMaxHeight().width(maxWidth - drawerWidth).testTag("filter_scrim").clickable { onFilterEvent(SearchFilterEvent.CancelDrawer) })
                    SearchFilterDrawer(filterState, onFilterEvent, Modifier.align(Alignment.CenterEnd).width(drawerWidth))
                }
            }
        }
    }
}

@Composable
private fun filterConditionsText(filter: RecordFilter, state: SearchFilterState): String {
    val types = filter.typeCategories.map { filterTypeLabel(it) }
    val category = categorySelectionSummary(filter.categories, state)
    val tags = filter.tagIds.take(2).map { id -> state.tags.find { it.id == id }?.name ?: stringResource(FilterR.string.search_filter_unavailable, id) }
    return listOf(
        state.bookName,
        filter.dateSelection.getDisplayText(),
        types.joinToString("、").ifEmpty { stringResource(FilterR.string.search_filter_any_type) },
        category,
        if (tags.isEmpty()) stringResource(FilterR.string.search_filter_any_tag) else tags.joinToString("、") + if (filter.tagIds.size > 2) " +${filter.tagIds.size - 2}" else "",
    ).joinToString(" · ")
}
