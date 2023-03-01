package cn.wj.android.cashbook.feature.assets.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.wj.android.cashbook.core.common.Symbol
import cn.wj.android.cashbook.core.model.entity.AssetEntity
import cn.wj.android.cashbook.core.model.enums.ClassificationTypeEnum
import cn.wj.android.cashbook.feature.assets.R
import cn.wj.android.cashbook.feature.assets.enums.BottomAssetListTypeEnum
import cn.wj.android.cashbook.feature.assets.viewmodel.SelectAssetViewModel

/**
 * 选择资产菜单
 */
@Composable
internal fun SelectAssetBottomSheet(
    onAddAssetClick: () -> Unit,
    onAssetItemClick: (AssetEntity?) -> Unit,
    viewModel:SelectAssetViewModel = hiltViewModel()
) {
    val assetList by viewModel.assetListData.collectAsStateWithLifecycle()
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
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
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.secondary),
                    ) {
                        Text(text = stringResource(id = R.string.add))
                    }
                }
            }
            item {
                AssetItem(
                    type = BottomAssetListTypeEnum.NO_SELECT,
                    name = stringResource(id = R.string.unselect_asset),
                    iconResId = R.drawable.vector_baseline_not_select_24,
                    balance = "",
                    totalAmount = "",
                    onAssetItemClick = { onAssetItemClick(null) },
                )
            }
            items(assetList) {
                val type = if (it.type == ClassificationTypeEnum.CREDIT_CARD_ACCOUNT) {
                    BottomAssetListTypeEnum.CREDIT_CARD
                } else {
                    BottomAssetListTypeEnum.CAPITAL
                }
                AssetItem(
                    type = type,
                    name = it.name,
                    iconResId = it.iconResId,
                    balance = it.balance,
                    totalAmount = it.totalAmount,
                    onAssetItemClick = { onAssetItemClick(it) },
                )
            }
        },
    )
}

@Composable
internal fun AssetItem(
    type: BottomAssetListTypeEnum,
    name: String,
    iconResId: Int,
    balance: String,
    totalAmount: String,
    onAssetItemClick: () -> Unit
) {
    ConstraintLayout(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 70.dp)
            .clickable {
                onAssetItemClick()
            }
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
    ) {
        val isNoSelect = type == BottomAssetListTypeEnum.NO_SELECT
        val isCreditCard = type == BottomAssetListTypeEnum.CREDIT_CARD

        val (iconRef, nameRef, balanceRef, progressRef, usedRef) = createRefs()

        Icon(
            painter = painterResource(id = iconResId),
            contentDescription = null,
            modifier = Modifier.constrainAs(iconRef) {
                start.linkTo(parent.start)
                top.linkTo(parent.top)
                bottom.linkTo(parent.bottom)
            },
        )
        Text(
            text = name,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.constrainAs(nameRef) {
                start.linkTo(iconRef.end, 8.dp)
                top.linkTo(parent.top)
                if (!isCreditCard) {
                    bottom.linkTo(parent.bottom)
                } else {
                    bottom.linkTo(progressRef.top)
                }
            },
        )

        if (!isNoSelect) {
            Text(
                text = Symbol.rmb + if (isCreditCard) totalAmount else balance,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.constrainAs(balanceRef) {
                    end.linkTo(parent.end)
                    top.linkTo(nameRef.top)
                    bottom.linkTo(nameRef.bottom)
                },
            )
        }

        if (isCreditCard) {
            // 信用卡类型
            var floatTotalAmount = totalAmount.toFloatOrNull() ?: 1f
            if (floatTotalAmount == 0f) {
                floatTotalAmount = 1f
            }
            val progress = (balance.toFloatOrNull() ?: 0f) / floatTotalAmount
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier.constrainAs(progressRef) {
                    start.linkTo(nameRef.start)
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                },
            )
            Text(
                text = stringResource(id = R.string.used_with_colon) + Symbol.rmb + balance,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.constrainAs(usedRef) {
                    start.linkTo(progressRef.start)
                    top.linkTo(progressRef.bottom)
                },
            )
        }
    }
}