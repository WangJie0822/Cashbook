package cn.wj.android.cashbook.feature.records.screen

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import cn.wj.android.cashbook.core.design.component.Empty
import cn.wj.android.cashbook.core.design.component.Footer
import cn.wj.android.cashbook.core.model.entity.RecordViewsEntity
import cn.wj.android.cashbook.core.ui.R
import cn.wj.android.cashbook.feature.records.viewmodel.AssetInfoContentViewModel

@Composable
internal fun AssetInfoContentRoute(
    assetId: Long,
    topContent: @Composable () -> Unit,
    onRecordItemClick: (RecordViewsEntity) -> Unit,
    viewModel: AssetInfoContentViewModel = hiltViewModel<AssetInfoContentViewModel>().apply {
        updateAssetId(assetId)
    },
) {

    val recordList = viewModel.recordList.collectAsLazyPagingItems()

    AssetInfoContentScreen(
        topContent = topContent,
        recordList = recordList,
        onRecordItemClick = onRecordItemClick,
    )
}

@Composable
internal fun AssetInfoContentScreen(
    topContent: @Composable () -> Unit,
    recordList: LazyPagingItems<RecordViewsEntity>,
    onRecordItemClick: (RecordViewsEntity) -> Unit,
) {

    LazyColumn(
        content = {

            item {
                topContent.invoke()
            }

            if (recordList.itemCount <= 0) {
                item {
                    Empty(
                        hintText = stringResource(id = R.string.asset_no_record_data_hint),
                    )
                }
            } else {
                items(count = recordList.itemCount) { index ->
                    recordList[index]?.let { item ->
                        RecordListItem(
                            recordViewsEntity = item,
                            onRecordItemClick = {
                                onRecordItemClick.invoke(item)
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