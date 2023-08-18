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
import cn.wj.android.cashbook.core.design.component.Loading
import cn.wj.android.cashbook.core.design.icon.CashbookIcons
import cn.wj.android.cashbook.core.design.theme.PreviewTheme
import cn.wj.android.cashbook.core.model.model.AssetTypeViewsModel
import cn.wj.android.cashbook.core.ui.BackPressHandler
import cn.wj.android.cashbook.core.ui.DevicePreviews
import cn.wj.android.cashbook.core.ui.R
import cn.wj.android.cashbook.feature.assets.component.AssetListItem
import cn.wj.android.cashbook.feature.assets.viewmodel.MyAssetUiState
import cn.wj.android.cashbook.feature.assets.viewmodel.MyAssetViewModel

/**
 * 我的资产
 *
 * @param onRequestNaviToAssetInfo 导航到资产信息
 * @param onRequestNaviToAddAsset 导航到添加资产
 * @param onRequestNaviToInvisibleAsset 导航到隐藏资产
 * @param onRequestPopBackStack 导航到上一级
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/6/27
 */
@Composable
internal fun MyAssetRoute(
    modifier: Modifier = Modifier,
    onRequestNaviToAssetInfo: (Long) -> Unit = {},
    onRequestNaviToAddAsset: () -> Unit = {},
    onRequestNaviToInvisibleAsset: () -> Unit = {},
    onRequestPopBackStack: () -> Unit = {},
    viewModel: MyAssetViewModel = hiltViewModel(),
) {

    val assetTypedListData by viewModel.assetTypedListData.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val showMoreDialog = viewModel.showMoreDialog

    // 显示更多显示时返回隐藏弹窗
    if (showMoreDialog) {
        BackPressHandler {
            viewModel.dismissShowMoreDialog()
        }
    }

    MyAssetScreen(
        uiState = uiState,
        showMoreDialog = showMoreDialog,
        onRequestDisplayShowMoreDialog = viewModel::displayShowMoreDialog,
        onRequestDismissShowMoreDialog = viewModel::dismissShowMoreDialog,
        assetTypedListData = assetTypedListData,
        onAssetItemClick = onRequestNaviToAssetInfo,
        onAddAssetClick = onRequestNaviToAddAsset,
        onInvisibleAssetClick = onRequestNaviToInvisibleAsset,
        onBackClick = onRequestPopBackStack,
        modifier = modifier,
    )
}

/**
 * 我的资产界面
 *
 * @param uiState 界面 UI 状态
 * @param showMoreDialog 是否显示更多弹窗
 * @param onRequestDisplayShowMoreDialog 显示弹窗
 * @param onRequestDismissShowMoreDialog 隐藏弹窗
 * @param assetTypedListData 资产列表数据
 * @param onAssetItemClick 资产列表 item 点击回调
 * @param onAddAssetClick 添加资产点击回调
 * @param onInvisibleAssetClick 隐藏资产点击回调
 * @param onBackClick 返回点击回调
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MyAssetScreen(
    uiState: MyAssetUiState,
    showMoreDialog: Boolean,
    onRequestDisplayShowMoreDialog: () -> Unit,
    onRequestDismissShowMoreDialog: () -> Unit,
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
                    onBackClick = onBackClick,
                    title = {
                        if (scaffoldState.isConcealed) {
                            Text(text = stringResource(id = R.string.my_assets))
                        }
                    },
                )
            },
            floatingActionButton = {
                CashbookFloatingActionButton(onClick = onRequestDisplayShowMoreDialog) {
                    Icon(
                        imageVector = CashbookIcons.MoreVert,
                        contentDescription = null,
                    )
                }
            },
        ) { paddingValues ->
            MyAssetBackdropScaffold(
                uiState = uiState,
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
                onCloseClick = onRequestDismissShowMoreDialog,
            )
        }
    }
}

/**
 * 显示更多弹窗内容
 *
 * @param onAddAssetClick 添加资产点击回调
 * @param onInvisibleAssetClick 隐藏资产点击回调
 * @param onCloseClick 关闭点击回调
 */
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
                    Icon(imageVector = CashbookIcons.Add, contentDescription = null)
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
                        imageVector = CashbookIcons.VisibilityOff,
                        contentDescription = null
                    )
                }
            }

            CashbookFloatingActionButton(
                onClick = onCloseClick,
            ) {
                Icon(imageVector = CashbookIcons.Close, contentDescription = null)
            }
        }
    }
}

/**
 * 我的资产界面
 *
 * @param paddingValues 界面 padding 数据
 * @param uiState 界面 UI 状态
 * @param assetTypedListData 资产列表数据
 * @param onAssetItemClick 资产列表 item 点击回调
 */
@Composable
internal fun MyAssetBackdropScaffold(
    paddingValues: PaddingValues,
    uiState: MyAssetUiState,
    assetTypedListData: List<AssetTypeViewsModel>,
    onAssetItemClick: (Long) -> Unit,
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
                uiState = uiState,
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
                            hintText = stringResource(id = R.string.asset_no_data),
                            modifier = Modifier.align(Alignment.TopCenter),
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

/**
 * 资产列表 item
 *
 * @param assetTypedInfo 资产数据
 * @param onAssetItemClick 资产点击回调
 * @param expandDefault 是否默认展开
 */
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
            imageVector = if (expand) CashbookIcons.KeyboardArrowDown else CashbookIcons.KeyboardArrowRight,
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

/**
 * 我的资产界面 - 统计数据
 *
 * @param paddingValues 界面 padding 数据
 * @param uiState 界面 UI 状态
 */
@Composable
internal fun BackLayerContent(
    paddingValues: PaddingValues,
    uiState: MyAssetUiState,
) {
    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onTertiaryContainer) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(paddingValues)
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when (uiState) {
                MyAssetUiState.Loading -> {
                    Loading()
                }

                is MyAssetUiState.Success -> {
                    Text(
                        text = stringResource(id = R.string.net_asset),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f),
                    )
                    Text(
                        text = uiState.netAsset.withCNY(),
                        style = MaterialTheme.typography.titleLarge
                    )
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
                                text = uiState.totalAsset.withCNY(),
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
                                text = uiState.totalLiabilities.withCNY(),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    }
                }
            }
        }
    }
}

@DevicePreviews
@Composable
private fun MyAssetScreenPreview() {
    PreviewTheme(
        defaultEmptyImagePainter = painterResource(id = R.drawable.vector_no_data_200)
    ) {
        MyAssetRoute()
    }
}