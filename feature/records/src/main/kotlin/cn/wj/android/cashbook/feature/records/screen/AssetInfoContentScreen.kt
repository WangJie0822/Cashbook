package cn.wj.android.cashbook.feature.records.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import cn.wj.android.cashbook.core.design.component.Empty
import cn.wj.android.cashbook.core.design.component.Footer
import cn.wj.android.cashbook.core.model.entity.RecordViewsEntity
import cn.wj.android.cashbook.core.ui.R
import cn.wj.android.cashbook.feature.records.viewmodel.AssetInfoContentViewModel

/**
 * 资产信息界面记录列表
 *
 * @param assetId 资产 id
 * @param topContent 列表头布局
 * @param onRecordItemClick 记录列表 item 点击回调
 */
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

/**
 * 资产信息界面记录列表
 *
 * @param topContent 列表头布局
 * @param recordList 记录列表数据
 * @param onRecordItemClick 记录列表 item 点击回调
 */
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
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            } else {
                items(count = recordList.itemCount) { index ->
                    recordList[index]?.let { item ->
                        RecordListItem(
                            item = item,
                            modifier = Modifier.clickable {
                                onRecordItemClick(item)
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