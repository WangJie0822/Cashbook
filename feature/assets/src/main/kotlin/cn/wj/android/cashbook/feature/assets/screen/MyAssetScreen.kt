package cn.wj.android.cashbook.feature.assets.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.BackdropScaffold
import androidx.compose.material3.BackdropScaffoldState
import androidx.compose.material3.BackdropValue
import androidx.compose.material3.Divider
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBackdropScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.wj.android.cashbook.core.common.ext.withCNY
import cn.wj.android.cashbook.core.design.component.CashbookFloatingActionButton
import cn.wj.android.cashbook.core.design.component.CashbookGradientBackground
import cn.wj.android.cashbook.core.design.component.CashbookScaffold
import cn.wj.android.cashbook.core.design.component.CashbookSmallFloatingActionButton
import cn.wj.android.cashbook.core.design.component.CashbookTopAppBar
import cn.wj.android.cashbook.core.design.component.Empty
import cn.wj.android.cashbook.core.design.component.Footer
import cn.wj.android.cashbook.core.design.theme.CashbookTheme
import cn.wj.android.cashbook.core.model.enums.ClassificationTypeEnum
import cn.wj.android.cashbook.core.model.model.AssetTypeViewsModel
import cn.wj.android.cashbook.core.model.model.assetModelTestObject
import cn.wj.android.cashbook.core.ui.BackPressHandler
import cn.wj.android.cashbook.core.ui.DevicePreviews
import cn.wj.android.cashbook.core.ui.R
import cn.wj.android.cashbook.feature.assets.component.AssetListItem
import cn.wj.android.cashbook.feature.assets.viewmodel.MyAssetViewModel

/**
 * 我的资产
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/6/27
 */
@Composable
internal fun MyAssetRoute(
    onAssetItemClick: (Long) -> Unit,
    onAddAssetClick: () -> Unit,
    onInvisibleAssetClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MyAssetViewModel = hiltViewModel(),
) {

    val assetTypedListData by viewModel.assetTypedListData.collectAsStateWithLifecycle()
    val netAsset by viewModel.netAsset.collectAsStateWithLifecycle()
    val totalAsset by viewModel.totalAsset.collectAsStateWithLifecycle()
    val totalLiabilities by viewModel.totalLiabilities.collectAsStateWithLifecycle()

    val showMoreDialog = viewModel.showMoreDialog

    if (showMoreDialog) {
        BackPressHandler {
            viewModel.dismissShowMoreDialog()
        }
    }

    MyAssetScreen(
        showMoreDialog = showMoreDialog,
        displayShowMoreDialog = viewModel::displayShowMoreDialog,
        dismissShowMoreDialog = viewModel::dismissShowMoreDialog,
        netAsset = netAsset,
        totalAsset = totalAsset,
        totalLiabilities = totalLiabilities,
        assetTypedListData = assetTypedListData,
        onAssetItemClick = onAssetItemClick,
        onAddAssetClick = onAddAssetClick,
        onInvisibleAssetClick = onInvisibleAssetClick,
        onBackClick = onBackClick,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MyAssetScreen(
    showMoreDialog: Boolean,
    displayShowMoreDialog: () -> Unit,
    dismissShowMoreDialog: () -> Unit,
    netAsset: String,
    totalAsset: String,
    totalLiabilities: String,
    assetTypedListData: List<AssetTypeViewsModel>,
    onAssetItemClick: (Long) -> Unit,
    onAddAssetClick: () -> Unit,
    onInvisibleAssetClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    scaffoldState: BackdropScaffoldState = rememberBackdropScaffoldState(initialValue = BackdropValue.Revealed),
) {

    Box {
        CashbookScaffold(
            modifier = modifier,
            topBar = {
                CashbookTopAppBar(
                    text = if (scaffoldState.isConcealed) stringResource(id = R.string.my_assets) else null,
                    onBackClick = onBackClick,
                )
            },
            floatingActionButton = {
                CashbookFloatingActionButton(onClick = displayShowMoreDialog) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = null,
                    )
                }
            },
        ) { paddingValues ->
            MyAssetBackdropScaffold(
                netAsset = netAsset,
                totalAsset = totalAsset,
                totalLiabilities = totalLiabilities,
                assetTypedListData = assetTypedListData,
                onAssetItemClick = onAssetItemClick,
                paddingValues = paddingValues,
                scaffoldState = scaffoldState,
            )
        }

        if (showMoreDialog) {
            ShowMoreContent(
                onAddAssetClick = onAddAssetClick,
                onInvisibleAssetClick = onInvisibleAssetClick,
                onCloseClick = dismissShowMoreDialog,
            )
        }
    }
}

@Composable
private fun ShowMoreContent(
    onAddAssetClick: () -> Unit,
    onInvisibleAssetClick: () -> Unit,
    onCloseClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onCloseClick.invoke() })
            },
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.BottomEnd),
            horizontalAlignment = Alignment.End,
        ) {
            Row(
                modifier = Modifier
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier
                        .padding(end = 8.dp),
                    shape = FloatingActionButtonDefaults.smallShape,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    tonalElevation = 6.dp,
                    shadowElevation = 6.dp,
                ) {
                    Text(
                        modifier = Modifier
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        text = stringResource(id = R.string.add_asset),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                CashbookSmallFloatingActionButton(
                    onClick = {
                        onAddAssetClick.invoke()
                        onCloseClick.invoke()
                    },
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                }
            }

            Row(
                modifier = Modifier
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier
                        .padding(end = 8.dp),
                    shape = FloatingActionButtonDefaults.smallShape,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    tonalElevation = 6.dp,
                    shadowElevation = 6.dp,
                ) {
                    Text(
                        modifier = Modifier
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        text = stringResource(id = R.string.invisible_asset),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                CashbookSmallFloatingActionButton(
                    onClick = {
                        onInvisibleAssetClick.invoke()
                        onCloseClick.invoke()
                    },
                ) {
                    Icon(
                        imageVector = Icons.Default.VisibilityOff,
                        contentDescription = null
                    )
                }
            }

            CashbookFloatingActionButton(
                onClick = onCloseClick,
            ) {
                Icon(imageVector = Icons.Default.Close, contentDescription = null)
            }
        }
    }
}

@Composable
internal fun MyAssetBackdropScaffold(
    netAsset: String,
    totalAsset: String,
    totalLiabilities: String,
    assetTypedListData: List<AssetTypeViewsModel>,
    onAssetItemClick: (Long) -> Unit,
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
                netAsset = netAsset,
                totalAsset = totalAsset,
                totalLiabilities = totalLiabilities,
            )
        },
        frontLayerScrimColor = Color.Unspecified,
        frontLayerContent = {
            CashbookGradientBackground {
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
                        Column(
                            modifier = Modifier.verticalScroll(state = rememberScrollState()),
                        ) {
                            assetTypedListData.forEach { assetTypedInfo ->
                                AssetTypedInfoItem(
                                    assetTypedInfo = assetTypedInfo,
                                    onAssetItemClick = onAssetItemClick,
                                )
                            }
                            Footer(hintText = stringResource(id = R.string.footer_hint_default))
                        }
                    }
                }
            }
        },
    )
}

@Composable
internal fun AssetTypedInfoItem(
    assetTypedInfo: AssetTypeViewsModel,
    onAssetItemClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    expandDefault: Boolean = true,
) {
    var expand by remember {
        mutableStateOf(expandDefault)
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                expand = !expand
            }
            .padding(16.dp),
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = stringResource(id = assetTypedInfo.nameResId)
        )
        Text(
            text = assetTypedInfo.totalAmount.withCNY(),
            style = MaterialTheme.typography.titleMedium,
        )
        Icon(
            imageVector = if (expand) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
            contentDescription = null,
        )
    }
    if (expand) {
        Divider(
            modifier = Modifier.padding(horizontal = 16.dp),
            color = DividerDefaults.color.copy(0.5f),
        )
        assetTypedInfo.assetList.forEach { assetModel ->
            AssetListItem(
                type = assetModel.type,
                name = assetModel.name,
                iconPainter = painterResource(id = assetModel.iconResId),
                balance = assetModel.balance,
                totalAmount = assetModel.totalAmount,
                onItemClick = { onAssetItemClick.invoke(assetModel.id) },
            )
        }
    }
}

@Composable
internal fun BackLayerContent(
    paddingValues: PaddingValues,
    netAsset: String,
    totalAsset: String,
    totalLiabilities: String,
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
            Text(text = netAsset.withCNY(), style = MaterialTheme.typography.titleLarge)
            Row(
                modifier = Modifier.padding(top = 8.dp),
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = stringResource(id = R.string.total_asset),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f),
                    )
                    Text(
                        text = totalAsset.withCNY(),
                        style = MaterialTheme.typography.bodyLarge,
                    )
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
                        text = totalLiabilities.withCNY(),
                        style = MaterialTheme.typography.bodyLarge,
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
                showMoreDialog = false,
                displayShowMoreDialog = {},
                dismissShowMoreDialog = {},
                netAsset = "$netAsset".withCNY(),
                totalAsset = "$totalIncome".withCNY(),
                totalLiabilities = "$totalLiabilities".withCNY(),
                assetTypedListData = listOf(
                    AssetTypeViewsModel(
                        R.string.asset_classifications_capital_account, "2000", listOf(
                            assetModelTestObject.copy(
                                id = 1L,
                                name = "建设银行",
                                iconResId = R.drawable.vector_bank_js_24
                            ),
                            assetModelTestObject.copy(
                                id = 2L,
                                name = "交通银行",
                                iconResId = R.drawable.vector_bank_jt_24
                            ),
                        )
                    ),
                    AssetTypeViewsModel(
                        R.string.asset_classifications_credit_card_account, "2000", listOf(
                            assetModelTestObject.copy(
                                id = 3,
                                name = "中国银行",
                                type = ClassificationTypeEnum.CREDIT_CARD_ACCOUNT,
                                iconResId = R.drawable.vector_bank_zg_24
                            ),
                            assetModelTestObject.copy(
                                id = 4,
                                name = "花呗",
                                type = ClassificationTypeEnum.CREDIT_CARD_ACCOUNT,
                                iconResId = R.drawable.vector_ant_credit_pay_circle_24
                            ),
                        )
                    ),
                ),
                onAssetItemClick = {},
                onAddAssetClick = {},
                onInvisibleAssetClick = {},
                onBackClick = {},
            )
        }
    }
}