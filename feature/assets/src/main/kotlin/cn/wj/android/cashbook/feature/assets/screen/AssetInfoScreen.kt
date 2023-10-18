package cn.wj.android.cashbook.feature.assets.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.wj.android.cashbook.core.common.ext.withCNY
import cn.wj.android.cashbook.core.design.component.CashbookModalBottomSheet
import cn.wj.android.cashbook.core.design.component.CashbookScaffold
import cn.wj.android.cashbook.core.design.component.CashbookTopAppBar
import cn.wj.android.cashbook.core.design.component.Loading
import cn.wj.android.cashbook.core.design.icon.CashbookIcons
import cn.wj.android.cashbook.core.design.theme.PreviewTheme
import cn.wj.android.cashbook.core.model.entity.RecordViewsEntity
import cn.wj.android.cashbook.core.ui.DevicePreviews
import cn.wj.android.cashbook.core.ui.DialogState
import cn.wj.android.cashbook.core.ui.R
import cn.wj.android.cashbook.feature.assets.enums.AssetInfoDialogEnum
import cn.wj.android.cashbook.feature.assets.viewmodel.AssetInfoUiState
import cn.wj.android.cashbook.feature.assets.viewmodel.AssetInfoViewModel

/**
 * 资产信息界面
 *
 * @param assetRecordListContent 资产记录列表，参数：(资产id, 列表头布局, 列表item点击回调) -> [Unit]
 * @param recordDetailSheetContent 记录详情 sheet，参数：(记录数据，隐藏sheet回调) -> [Unit]
 * @param onRequestNaviToEditAsset 导航到编辑资产
 * @param onRequestPopBackStack 导航到上一级
 */
@Composable
internal fun AssetInfoRoute(
    modifier: Modifier = Modifier,
    assetId: Long = -1L,
    assetRecordListContent: @Composable (@Composable () -> Unit, (RecordViewsEntity) -> Unit) -> Unit = { _, _ -> },
    recordDetailSheetContent: @Composable (RecordViewsEntity?, () -> Unit) -> Unit = { _, _ -> },
    onRequestNaviToEditAsset: () -> Unit = {},
    onRequestPopBackStack: () -> Unit = {},
    viewModel: AssetInfoViewModel = hiltViewModel<AssetInfoViewModel>().apply {
        updateAssetId(assetId)
    },
) {

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    AssetInfoScreen(
        shouldDisplayBookmark = viewModel.shouldDisplayBookmark,
        onRequestDisplayBookmark = viewModel::displayBookmark,
        onRequestDismissBookmark = viewModel::dismissBookmark,
        dialogState = viewModel.dialogState,
        onRequestShowMoreDialog = viewModel::displayMoreDialog,
        onRequestDismissDialog = viewModel::dismissDialog,
        uiState = uiState,
        viewRecord = viewModel.viewRecordData,
        assetRecordListContent = { topContent ->
            assetRecordListContent(topContent, viewModel::onRecordItemClick)
        },
        recordDetailSheetContent = { record ->
            recordDetailSheetContent(record, viewModel::dismissRecordDetailSheet)
        },
        onEditAssetClick = onRequestNaviToEditAsset,
        onDeleteAssetClick = viewModel::showDeleteConfirmDialog,
        onConfirmDeleteAsset = { viewModel.deleteAsset(onSuccess = onRequestPopBackStack) },
        onRequestDismissBottomSheet = viewModel::dismissRecordDetailSheet,
        onBackClick = onRequestPopBackStack,
        modifier = modifier,
    )
}

/**
 * 资产信息界面
 *
 * @param uiState 界面 UI 状态
 * @param viewRecord 显示详情数据
 * @param assetRecordListContent 资产记录列表
 * @param recordDetailSheetContent 记录详情
 * @param onEditAssetClick 编辑资产点击回调
 * @param onRequestDismissBottomSheet 隐藏底部抽屉
 * @param onBackClick 返回点击回调
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AssetInfoScreen(
    shouldDisplayBookmark: String,
    onRequestDisplayBookmark: () -> Unit,
    onRequestDismissBookmark: () -> Unit,
    dialogState: DialogState,
    onRequestShowMoreDialog: () -> Unit,
    onRequestDismissDialog: () -> Unit,
    uiState: AssetInfoUiState,
    viewRecord: RecordViewsEntity?,
    assetRecordListContent: @Composable (@Composable () -> Unit) -> Unit,
    recordDetailSheetContent: @Composable (RecordViewsEntity?) -> Unit,
    onEditAssetClick: () -> Unit,
    onDeleteAssetClick: () -> Unit,
    onConfirmDeleteAsset: () -> Unit,
    onRequestDismissBottomSheet: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember {
        SnackbarHostState()
    }
) {

    LaunchedEffect(shouldDisplayBookmark) {
        if (shouldDisplayBookmark.isNotBlank()) {
            val result = snackbarHostState.showSnackbar(shouldDisplayBookmark)
            if (result == SnackbarResult.Dismissed) {
                onRequestDismissBookmark()
            }
        }
    }

    CashbookScaffold(
        modifier = modifier,
        topBar = {
            CashbookTopAppBar(
                onBackClick = onBackClick,
                title = { Text(text = uiState.title) },
                actions = {
                    if (uiState is AssetInfoUiState.Success) {
                        IconButton(onClick = onEditAssetClick) {
                            Icon(imageVector = CashbookIcons.EditNote, contentDescription = null)
                        }
                        IconButton(onClick = onDeleteAssetClick) {
                            Icon(
                                imageVector = CashbookIcons.DeleteForever,
                                contentDescription = null
                            )
                        }
                        if (uiState.shouldDisplayMore) {
                            IconButton(onClick = onRequestShowMoreDialog) {
                                Icon(
                                    imageVector = CashbookIcons.Info,
                                    contentDescription = null
                                )
                            }
                        }
                    }
                },
            )
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState)
        },
        content = { paddingValues ->
            Box(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize(),
            ) {
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
                            recordDetailSheetContent(viewRecord)
                        },
                    )
                }

                (dialogState as? DialogState.Shown<*>)?.data?.let { data ->
                    when (data) {
                        AssetInfoDialogEnum.MORE_INFO -> {
                            if (uiState is AssetInfoUiState.Success && uiState.shouldDisplayMore) {
                                val clipboardManager = LocalClipboardManager.current
                                AlertDialog(
                                    onDismissRequest = onRequestDismissDialog,
                                    text = {
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                        ) {
                                            val textStartModifier = Modifier.width(50.dp)
                                            if (uiState.openBank.isNotBlank()) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = stringResource(id = R.string.open_bank),
                                                        modifier = textStartModifier,
                                                    )
                                                    Text(
                                                        text = uiState.openBank,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                    IconButton(onClick = {
                                                        clipboardManager.setText(
                                                            AnnotatedString(
                                                                uiState.openBank
                                                            )
                                                        )
                                                        onRequestDisplayBookmark()
                                                    }) {
                                                        Icon(
                                                            imageVector = CashbookIcons.ContentCopy,
                                                            contentDescription = null
                                                        )
                                                    }
                                                }
                                            }
                                            if (uiState.cardNo.isNotBlank()) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = stringResource(id = R.string.card_no),
                                                        modifier = textStartModifier,
                                                    )
                                                    Text(
                                                        text = uiState.cardNo,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                    IconButton(onClick = {
                                                        clipboardManager.setText(
                                                            AnnotatedString(
                                                                uiState.cardNo
                                                            )
                                                        )
                                                        onRequestDisplayBookmark()
                                                    }) {
                                                        Icon(
                                                            imageVector = CashbookIcons.ContentCopy,
                                                            contentDescription = null
                                                        )
                                                    }
                                                }
                                            }
                                            if (uiState.remark.isNotBlank()) {
                                                Row {
                                                    Text(
                                                        text = stringResource(id = R.string.remark),
                                                        modifier = textStartModifier,
                                                    )
                                                    Text(
                                                        text = uiState.remark,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                }
                                            }
                                        }
                                    },
                                    confirmButton = {
                                        TextButton(onClick = {
                                            clipboardManager.setText(AnnotatedString("${uiState.openBank}\n${uiState.cardNo}"))
                                            onRequestDisplayBookmark()
                                        }) {
                                            Text(text = stringResource(id = R.string.copy_all))
                                        }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = onRequestDismissDialog) {
                                            Text(text = stringResource(id = R.string.cancel))
                                        }
                                    }
                                )
                            }
                        }

                        AssetInfoDialogEnum.DELETE_ASSET -> {
                            // 删除资产
                            AlertDialog(
                                onDismissRequest = onRequestDismissDialog,
                                title = { Text(text = stringResource(id = R.string.sure_to_delete_asset)) },
                                text = { Text(text = stringResource(id = R.string.delete_asset_hint)) },
                                confirmButton = {
                                    TextButton(onClick = onConfirmDeleteAsset) {
                                        Text(text = stringResource(id = R.string.confirm))
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = onRequestDismissDialog) {
                                        Text(text = stringResource(id = R.string.cancel))
                                    }
                                }
                            )
                        }
                    }
                }

                when (uiState) {
                    AssetInfoUiState.Loading -> {
                        Loading(modifier = Modifier.align(Alignment.Center))
                    }

                    is AssetInfoUiState.Success -> {
                        assetRecordListContent {
                            AssetInfoContent(
                                isCreditCard = uiState.isCreditCard,
                                balance = uiState.balance,
                                totalAmount = uiState.totalAmount,
                                billingDate = uiState.billingDate,
                                repaymentDate = uiState.repaymentDate,
                            )
                        }
                    }
                }
            }
        },
    )
}

/**
 * 资产信息
 *
 * @param isCreditCard 是否是信用卡
 * @param balance 余额
 * @param totalAmount 总额度
 * @param billingDate 账单日
 * @param repaymentDate 还款日
 */
@Composable
private fun AssetInfoContent(
    isCreditCard: Boolean,
    balance: String,
    totalAmount: String,
    billingDate: String,
    repaymentDate: String
) {
    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),

            ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(id = if (isCreditCard) R.string.current_debt else R.string.asset_balance),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f),
                )
                Text(
                    text = balance.withCNY(),
                    style = MaterialTheme.typography.titleLarge
                )
                if (isCreditCard) {
                    Row(
                        modifier = Modifier.padding(top = 8.dp),
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = stringResource(id = R.string.total_amount),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(
                                    alpha = 0.7f
                                ),
                            )
                            Text(
                                text = totalAmount.withCNY(),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                        Column(
                            modifier = Modifier
                                .weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = stringResource(id = R.string.billing_date),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(
                                    alpha = 0.7f
                                ),
                            )
                            val billingDateText = if (billingDate.isBlank()) {
                                stringResource(id = R.string.un_set)
                            } else {
                                billingDate + stringResource(id = R.string.day)
                            }
                            Text(
                                text = billingDateText,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                        Column(
                            modifier = Modifier
                                .weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = stringResource(id = R.string.repayment_date),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(
                                    alpha = 0.7f
                                ),
                            )
                            val repaymentDateText = if (repaymentDate.isBlank()) {
                                stringResource(id = R.string.un_set)
                            } else {
                                repaymentDate + stringResource(id = R.string.day)
                            }
                            Text(
                                text = repaymentDateText,
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
private fun AssetInfoScreenPreview() {
    PreviewTheme(
        defaultEmptyImagePainter = painterResource(id = R.drawable.vector_no_data_200),
    ) {
        AssetInfoRoute()
    }
}