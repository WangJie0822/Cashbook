package cn.wj.android.cashbook.feature.records.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.wj.android.cashbook.core.design.component.CashbookFloatingActionButton
import cn.wj.android.cashbook.core.design.component.CashbookScaffold
import cn.wj.android.cashbook.core.design.component.CashbookTopAppBar
import cn.wj.android.cashbook.core.design.component.CompatTextField
import cn.wj.android.cashbook.core.design.component.Empty
import cn.wj.android.cashbook.core.design.component.Loading
import cn.wj.android.cashbook.core.design.component.TextFieldState
import cn.wj.android.cashbook.core.design.icon.CashbookIcons
import cn.wj.android.cashbook.core.ui.LocalNavController
import cn.wj.android.cashbook.core.ui.R
import cn.wj.android.cashbook.feature.records.navigation.ROUTE_EDIT_RECORD
import cn.wj.android.cashbook.feature.records.viewmodel.EditRecordViewModel
import cn.wj.android.cashbook.feature.records.viewmodel.SelectRelatedRecordUiState
import cn.wj.android.cashbook.feature.records.viewmodel.SelectRelatedRecordViewModel

@Composable
internal fun SelectRelatedRecordRoute(
    onRequestPopBackStack: () -> Unit,
    modifier: Modifier = Modifier,
    parentViewModel: EditRecordViewModel = hiltViewModel(
        LocalNavController.current.getBackStackEntry(
            ROUTE_EDIT_RECORD
        )
    ),
    viewModel: SelectRelatedRecordViewModel = hiltViewModel<SelectRelatedRecordViewModel>().apply {
        updateData(parentViewModel.currentRecord(), parentViewModel.currentRelatedRecord())
    },
) {

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    SelectRelatedRecordScreen(
        uiState = uiState,
        onKeywordChanged = viewModel::onKeywordsChanged,
        onRelatedRecordItemClick = viewModel::removeFromRelated,
        onRecordItemClick = viewModel::addToRelated,
        onRequestSaveRelatedRecord = parentViewModel::updateRelatedRecord,
        onRequestPopBackStack = onRequestPopBackStack,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
internal fun SelectRelatedRecordScreen(
    uiState: SelectRelatedRecordUiState,
    onKeywordChanged: (String) -> Unit,
    onRelatedRecordItemClick: (Long) -> Unit,
    onRecordItemClick: (Long) -> Unit,
    onRequestSaveRelatedRecord: (List<Long>) -> Unit,
    onRequestPopBackStack: () -> Unit,
    modifier: Modifier = Modifier,
) {

    CashbookScaffold(
        modifier = modifier,
        topBar = {
            CashbookTopAppBar(
                onBackClick = onRequestPopBackStack,
                title = { Text(text = "选择关联记录") },
            )
        },
        floatingActionButton = {
            if (uiState is SelectRelatedRecordUiState.Success) {
                CashbookFloatingActionButton(onClick = {
                    onRequestSaveRelatedRecord(uiState.relatedRecordList.map { it.id })
                    onRequestPopBackStack()
                }) {
                    Icon(imageVector = CashbookIcons.SaveAs, contentDescription = null)
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.padding(paddingValues),
        ) {
            when (uiState) {
                SelectRelatedRecordUiState.Loading -> {
                    Loading(modifier = Modifier.fillMaxWidth())
                }
                is SelectRelatedRecordUiState.Success -> {
                    val keyword = remember {
                        TextFieldState(
                            delayAfterTextChange = 1000L,
                            afterTextChange = onKeywordChanged,
                        )
                    }
                    CompatTextField(
                        textFieldState = keyword,
                        label = { Text(text = "搜索") },
                        placeholder = { Text(text = "对金额、备注进行搜索") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                    )
                    LazyColumn(content = {
                        stickyHeader {
                            Text(
                                text = "已选择关联记录",
                                color = LocalContentColor.current.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .background(color = MaterialTheme.colorScheme.surface)
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                            )
                        }
                        if (uiState.relatedRecordList.isEmpty()) {
                            item {
                                Text(
                                    text = "暂未选择",
                                    color = LocalContentColor.current.copy(alpha = 0.5f),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                )
                            }
                        } else {
                            items(items = uiState.relatedRecordList) {
                                RecordListItem(
                                    item = it,
                                    modifier = Modifier.clickable { onRelatedRecordItemClick(it.id) },
                                )
                            }
                        }
                        stickyHeader {
                            Text(
                                text = "可选择未关联记录",
                                color = LocalContentColor.current.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .background(color = MaterialTheme.colorScheme.surface)
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                            )
                        }
                        if (uiState.recordList.isEmpty()) {
                            item {
                                Empty(
                                    hintText = stringResource(id = R.string.no_record_data),
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        } else {
                            items(items = uiState.recordList) {
                                RecordListItem(
                                    item = it,
                                    modifier = Modifier.clickable { onRecordItemClick(it.id) },
                                )
                            }
                        }
                    })
                }
            }
        }
    }
}