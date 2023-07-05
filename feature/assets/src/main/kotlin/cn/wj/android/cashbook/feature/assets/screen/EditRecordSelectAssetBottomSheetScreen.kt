package cn.wj.android.cashbook.feature.assets.screen

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.wj.android.cashbook.core.design.component.CommonDivider
import cn.wj.android.cashbook.core.model.entity.AssetEntity
import cn.wj.android.cashbook.core.ui.R
import cn.wj.android.cashbook.feature.assets.component.AssetListItem
import cn.wj.android.cashbook.feature.assets.component.NotAssociatedAssetListItem
import cn.wj.android.cashbook.feature.assets.viewmodel.EditRecordSelectAssetBottomSheetViewModel

@Composable
internal fun EditRecordSelectAssetBottomSheetRoute(
    currentTypeId: Long,
    isRelated: Boolean,
    onAssetChange: (Long) -> Unit,
    onAddAssetClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EditRecordSelectAssetBottomSheetViewModel = hiltViewModel<EditRecordSelectAssetBottomSheetViewModel>().apply {
        update(currentTypeId, isRelated)
    },
) {
    val assetList by viewModel.assetListData.collectAsStateWithLifecycle()

    EditRecordSelectAssetBottomSheetScreen(
        assetList = assetList,
        onAssetChange = onAssetChange,
        onAddAssetClick = onAddAssetClick,
        modifier = modifier,
    )
}

@Composable
internal fun EditRecordSelectAssetBottomSheetScreen(
    assetList: List<AssetEntity>,
    onAssetChange: (Long) -> Unit,
    onAddAssetClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        content = {
            item {
                ConstraintLayout(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                ) {
                    val (title, subTitle, add) = createRefs()
                    Text(
                        text = stringResource(id = R.string.please_select_asset),
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.constrainAs(title) {
                            top.linkTo(parent.top)
                            start.linkTo(parent.start)
                        },
                    )
                    Text(
                        text = stringResource(id = R.string.unable_to_select_invisible_asset),
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.constrainAs(subTitle) {
                            top.linkTo(title.bottom, 8.dp)
                            start.linkTo(parent.start)
                        },
                    )
                    TextButton(
                        modifier = Modifier.constrainAs(add) {
                            top.linkTo(parent.top)
                            end.linkTo(parent.end)
                            bottom.linkTo(parent.bottom)
                        },
                        onClick = onAddAssetClick,
                    ) {
                        Text(
                            text = stringResource(id = R.string.add),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                CommonDivider()
            }
            item {
                NotAssociatedAssetListItem(
                    onNotAssociatedAssetClick = { onAssetChange(-1L) },
                )
            }
            items(assetList) {
                AssetListItem(
                    type = it.type,
                    name = it.name,
                    iconPainter = painterResource(id = it.iconResId),
                    balance = it.balance,
                    totalAmount = it.totalAmount,
                    onItemClick = { onAssetChange(it.id) },
                )
            }
        },
    )
}