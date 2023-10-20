package cn.wj.android.cashbook.feature.records.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ElevatedSuggestionChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import cn.wj.android.cashbook.core.design.component.CashbookModalBottomSheet
import cn.wj.android.cashbook.core.design.component.Empty
import cn.wj.android.cashbook.core.design.component.Footer
import cn.wj.android.cashbook.core.design.icon.CashbookIcons
import cn.wj.android.cashbook.core.model.entity.RecordViewsEntity
import cn.wj.android.cashbook.core.ui.R
import cn.wj.android.cashbook.feature.records.view.RecordDetailsSheet
import cn.wj.android.cashbook.feature.records.viewmodel.SearchViewModel

@Composable
internal fun SearchRoute(
    onRequestNaviToEditRecord: (Long) -> Unit,
    onRequestPopBackStack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = hiltViewModel(),
) {

    val searchHistoryList by viewModel.searchHistoryListData.collectAsStateWithLifecycle()
    val recordList = viewModel.recordListData.collectAsLazyPagingItems()

    SearchScreen(
        viewRecord = viewModel.viewRecordData,
        onRequestShowRecordDetailSheet = viewModel::showRecordDetailSheet,
        onRequestDismissBottomSheet = viewModel::dismissRecordDetailSheet,
        searchHistoryList = searchHistoryList,
        onRequestClearSearchHistory = viewModel::clearSearchHistory,
        recordList = recordList,
        onKeywordChange = viewModel::onKeywordChange,
        onRequestNaviToEditRecord = onRequestNaviToEditRecord,
        onRequestPopBackStack = onRequestPopBackStack,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun SearchScreen(
    viewRecord: RecordViewsEntity?,
    onRequestShowRecordDetailSheet: (RecordViewsEntity) -> Unit,
    onRequestDismissBottomSheet: () -> Unit,
    searchHistoryList: List<String>,
    onRequestClearSearchHistory: () -> Unit,
    recordList: LazyPagingItems<RecordViewsEntity>,
    onKeywordChange: (String) -> Unit,
    onRequestNaviToEditRecord: (Long) -> Unit,
    onRequestPopBackStack: () -> Unit,
    modifier: Modifier = Modifier,
) {

    var text by remember { mutableStateOf("") }
    var active by remember { mutableStateOf(false) }

    if (null != viewRecord) {
        CashbookModalBottomSheet(
            onDismissRequest = onRequestDismissBottomSheet,
            sheetState = rememberModalBottomSheetState(
                confirmValueChange = {
                    if (it == SheetValue.Hidden) {
                        onRequestDismissBottomSheet()
                    }
                    true
                },
            ),
            content = {
                RecordDetailsSheet(
                    recordData = viewRecord,
                    onRequestNaviToEditRecord = onRequestNaviToEditRecord,
                    onRequestDismissSheet = onRequestDismissBottomSheet
                )
            },
        )
    }

    Column(modifier = modifier.fillMaxSize()) {
        SearchBar(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            query = text,
            onQueryChange = { text = it },
            onSearch = {
                onKeywordChange(it)
                active = false
            },
            active = active,
            onActiveChange = { active = it },
            placeholder = { Text(text = stringResource(id = R.string.record_search_hint)) },
            leadingIcon = {
                IconButton(onClick = {
                    if (active) {
                        active = false
                    } else {
                        onRequestPopBackStack()
                    }
                }) {
                    Icon(
                        imageVector = CashbookIcons.ArrowBack,
                        contentDescription = null
                    )
                }
            },
            trailingIcon = {
                if (active && text.isNotBlank()) {
                    IconButton(onClick = { text = "" }) {
                        Icon(imageVector = CashbookIcons.Cancel, contentDescription = null)
                    }
                }
            },
            content = {
                if (searchHistoryList.isEmpty()) {
                    Empty(
                        hintText = stringResource(id = R.string.record_search_no_history),
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    )
                } else {
                    TextButton(onClick = onRequestClearSearchHistory) {
                        Text(text = stringResource(id = R.string.clear))
                    }
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        searchHistoryList.forEach {
                            ElevatedSuggestionChip(
                                onClick = { text = it },
                                label = { Text(text = it) },
                            )
                        }
                    }
                }
            },
        )
        LazyColumn(
            modifier = Modifier.padding(top = 8.dp),
            content = {
                if (recordList.itemCount <= 0) {
                    item {
                        Empty(
                            hintText = stringResource(id = R.string.record_search_no_data_hint),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                } else {
                    items(count = recordList.itemCount) { index ->
                        recordList[index]?.let { item ->
                            RecordListItem(
                                item = item,
                                modifier = Modifier.clickable {
                                    onRequestShowRecordDetailSheet(item)
                                },
                            )
                        }
                    }
                    item {
                        Footer(hintText = stringResource(id = R.string.footer_hint_default))
                    }
                }
            },
        )
    }
}