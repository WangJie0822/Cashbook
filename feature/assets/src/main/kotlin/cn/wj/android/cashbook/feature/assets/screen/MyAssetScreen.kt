package cn.wj.android.cashbook.feature.assets.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.BackdropScaffold
import androidx.compose.material3.BackdropScaffoldState
import androidx.compose.material3.BackdropValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBackdropScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.wj.android.cashbook.core.common.Symbol
import cn.wj.android.cashbook.core.common.ext.withSymbol
import cn.wj.android.cashbook.core.design.component.CashbookGradientBackground
import cn.wj.android.cashbook.core.design.component.CashbookScaffold
import cn.wj.android.cashbook.core.design.component.CashbookTopAppBar
import cn.wj.android.cashbook.core.design.component.Empty
import cn.wj.android.cashbook.core.design.theme.CashbookTheme
import cn.wj.android.cashbook.core.model.model.AssetTypeViewsModel
import cn.wj.android.cashbook.core.model.model.assetModelTestObject
import cn.wj.android.cashbook.core.ui.DevicePreviews
import cn.wj.android.cashbook.core.ui.R
import cn.wj.android.cashbook.feature.assets.enums.BottomAssetListTypeEnum
import cn.wj.android.cashbook.feature.assets.viewmodel.MyAssetViewModel

/**
 * 我的资产
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/6/27
 */
@Composable
internal fun MyAssetRoute(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MyAssetViewModel = hiltViewModel(),
) {

    val assetTypedListData by viewModel.assetTypedListData.collectAsStateWithLifecycle()

    MyAssetScreen(
        netAssetText = "",
        totalIncomeText = "",
        totalLiabilitiesText = "",
        assetTypedListData = assetTypedListData,
        onBackClick = onBackClick,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MyAssetScreen(
    netAssetText: String,
    totalIncomeText: String,
    totalLiabilitiesText: String,
    assetTypedListData: List<AssetTypeViewsModel>,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    scaffoldState: BackdropScaffoldState = rememberBackdropScaffoldState(initialValue = BackdropValue.Revealed),
) {
    CashbookScaffold(
        modifier = modifier,
        topBar = {
            CashbookTopAppBar(
                text = if (scaffoldState.isConcealed) stringResource(id = R.string.my_assets) else null,
                onBackClick = onBackClick,
            )
        },
    ) { paddingValues ->
        MyAssetBackdropScaffold(
            netAssetText = netAssetText,
            totalIncomeText = totalIncomeText,
            totalLiabilitiesText = totalLiabilitiesText,
            assetTypedListData = assetTypedListData,
            paddingValues = paddingValues,
            scaffoldState = scaffoldState,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun MyAssetBackdropScaffold(
    netAssetText: String,
    totalIncomeText: String,
    totalLiabilitiesText: String,
    assetTypedListData: List<AssetTypeViewsModel>,
    paddingValues: PaddingValues,
    scaffoldState: BackdropScaffoldState = rememberBackdropScaffoldState(initialValue = BackdropValue.Revealed),
) {
    BackdropScaffold(
        scaffoldState = scaffoldState,
        appBar = { /* 使用上层 topBar 处理 */ },
        peekHeight = paddingValues.calculateTopPadding(),
        backLayerBackgroundColor = MaterialTheme.colorScheme.tertiaryContainer,
        backLayerContent = {
            BackLayerContent(
                paddingValues = paddingValues,
                netAssetText = netAssetText,
                totalIncomeText = totalIncomeText,
                totalLiabilitiesText = totalLiabilitiesText,
            )
        },
        frontLayerScrimColor = Color.Unspecified,
        frontLayerContent = {
            Box(
                modifier = Modifier.fillMaxSize(),
            ) {
                if (assetTypedListData.isEmpty()) {
                    // 无数据
                    Empty(
                        imagePainter = painterResource(id = R.drawable.vector_no_data_200),
                        hintText = stringResource(id = R.string.asset_no_data),
                    )
                } else {
                    LazyColumn {
                        assetTypedListData.forEach { assetInfo ->
                            stickyHeader(key = assetInfo.name) {
//                     TODO            TranparentListItem(headlineText = { /*TODO*/ })
                                Text(text = assetInfo.name)
                            }
                            items(items = assetInfo.assetList, key = { it.id }) {
                                AssetItem(
                                    type = BottomAssetListTypeEnum.CAPITAL,
                                    name = it.name,
                                    iconPainter = painterResource(id = it.iconResId),
                                    balance = it.balance,
                                    totalAmount = it.totalAmount,
                                ) {

                                }
                                Text(text = it.name)
                            }
                        }
                    }
                }
            }
        },
    )
}

@Composable
internal fun BackLayerContent(
    paddingValues: PaddingValues,
    netAssetText: String,
    totalIncomeText: String,
    totalLiabilitiesText: String
) {
    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onTertiaryContainer) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(paddingValues)
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(id = R.string.net_asset),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f),
            )
            Text(text = netAssetText, style = MaterialTheme.typography.titleLarge)
            Row(
                modifier = Modifier.padding(top = 8.dp),
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = stringResource(id = R.string.total_income),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f),
                    )
                    Text(text = totalIncomeText, style = MaterialTheme.typography.bodyLarge)
                }
                Column(
                    modifier = Modifier
                        .weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = stringResource(id = R.string.total_liabilities),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f),
                    )
                    Text(
                        text = totalLiabilitiesText,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}

@DevicePreviews
@Composable
private fun MyAssetScreenPreview() {
    CashbookTheme {
        CashbookGradientBackground {
            val totalIncome = 2000
            val totalLiabilities = 3000
            val netAsset = totalIncome - totalLiabilities
            MyAssetScreen(
                netAssetText = "$netAsset".withSymbol(),
                totalIncomeText = "$totalIncome".withSymbol(),
                totalLiabilitiesText = "$totalLiabilities".withSymbol(),
                assetTypedListData = listOf(
                    AssetTypeViewsModel(
                        "资金账户", "2000", listOf(
                            assetModelTestObject
                        )
                    )
                ),
                onBackClick = {},
            )
        }
    }
}