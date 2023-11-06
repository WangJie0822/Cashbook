package cn.wj.android.cashbook.feature.records.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import cn.wj.android.cashbook.core.design.component.CashbookModalBottomSheet
import cn.wj.android.cashbook.core.design.component.CashbookScaffold
import cn.wj.android.cashbook.core.design.component.CashbookTopAppBar
import cn.wj.android.cashbook.core.design.component.Empty
import cn.wj.android.cashbook.core.design.component.Footer
import cn.wj.android.cashbook.core.design.component.Loading
import cn.wj.android.cashbook.core.model.entity.RecordViewsEntity
import cn.wj.android.cashbook.core.ui.R
import cn.wj.android.cashbook.feature.records.view.RecordDetailsSheet
import cn.wj.android.cashbook.feature.records.viewmodel.TypedAnalyticsUiState
import cn.wj.android.cashbook.feature.records.viewmodel.TypedAnalyticsViewModel

/**
 * 分类数据分析
 * - TODO 待完善，按日拆分，饼状图，日期
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/10/27
 */
@Composable
internal fun TypedAnalyticsRoute(
    typeId: Long,
    tagId: Long,
    onRequestNaviToEditRecord: (Long) -> Unit,
    onRequestPopBackStack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TypedAnalyticsViewModel = hiltViewModel<TypedAnalyticsViewModel>().apply {
        updateId(tagId = tagId, typeId = typeId)
    },
) {

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val recordList = viewModel.recordList.collectAsLazyPagingItems()

    TypedAnalyticsScreen(
        viewRecord = viewModel.viewRecord,
        onRequestShowRecordDetailsSheet = viewModel::showRecordDetailsSheet,
        onRequestDismissBottomSheet = viewModel::dismissRecordDetailSheet,
        uiState = uiState,
        recordList = recordList,
        onRequestNaviToEditRecord = onRequestNaviToEditRecord,
        onRequestPopBackStack = onRequestPopBackStack,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TypedAnalyticsScreen(
    viewRecord: RecordViewsEntity?,
    onRequestShowRecordDetailsSheet: (RecordViewsEntity) -> Unit,
    onRequestDismissBottomSheet: () -> Unit,
    uiState: TypedAnalyticsUiState,
    recordList: LazyPagingItems<RecordViewsEntity>,
    onRequestNaviToEditRecord: (Long) -> Unit,
    onRequestPopBackStack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CashbookScaffold(
        modifier = modifier,
        topBar = {
            CashbookTopAppBar(title = {
                if (uiState is TypedAnalyticsUiState.Success) {
                    Text(text = uiState.titleText)
                }
            }, onBackClick = onRequestPopBackStack)
        },
        content = { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues)) {

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
                                onRequestDismissSheet = onRequestDismissBottomSheet,
                            )
                        },
                    )
                }

                when (uiState) {
                    TypedAnalyticsUiState.Loading -> {
                        Loading(modifier = Modifier.align(Alignment.Center))
                    }

                    is TypedAnalyticsUiState.Success -> {
                        LazyColumn(content = {
                            if (recordList.itemCount <= 0) {
                                item {
                                    Empty(
                                        hintText = stringResource(id = R.string.asset_no_record_data_hint),
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                            } else {
                                items(count = recordList.itemCount) { index ->
                                    recordList[index]?.let { item ->
                                        RecordListItem(
                                            item = item,
                                            modifier = Modifier.clickable {
                                                onRequestShowRecordDetailsSheet(item)
                                            },
                                        )
                                    }
                                }
                                item {
                                    Footer(hintText = stringResource(id = R.string.footer_hint_default))
                                }
                            }
                        })
                    }
                }
            }
        }
    )
}