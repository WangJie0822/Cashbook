package cn.wj.android.cashbook.feature.assets.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheetState
import androidx.compose.material3.ModalBottomSheetValue
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.wj.android.cashbook.core.common.ext.withCNY
import cn.wj.android.cashbook.core.design.component.CashbookBottomSheetScaffold
import cn.wj.android.cashbook.core.design.component.CashbookFloatingActionButton
import cn.wj.android.cashbook.core.design.component.CashbookTopAppBar
import cn.wj.android.cashbook.core.design.component.Empty
import cn.wj.android.cashbook.core.design.component.Loading
import cn.wj.android.cashbook.core.design.icon.CashbookIcons
import cn.wj.android.cashbook.core.design.theme.PreviewTheme
import cn.wj.android.cashbook.core.model.entity.RecordViewsEntity
import cn.wj.android.cashbook.core.model.model.ResultModel
import cn.wj.android.cashbook.core.ui.BackPressHandler
import cn.wj.android.cashbook.core.ui.DevicePreviews
import cn.wj.android.cashbook.core.ui.DialogState
import cn.wj.android.cashbook.core.ui.R
import cn.wj.android.cashbook.feature.assets.viewmodel.AssetInfoUiState
import cn.wj.android.cashbook.feature.assets.viewmodel.AssetInfoViewModel

@Composable
internal fun AssetInfoRoute(
    assetId: Long,
    assetRecordListContent: @Composable (topContent: @Composable () -> Unit, onRecordItemClick: (RecordViewsEntity) -> Unit) -> Unit,
    recordDetailSheetContent: @Composable (recordInfo: RecordViewsEntity?, onRecordDeleteClick: (Long) -> Unit, dismissBottomSheet: () -> Unit) -> Unit,
    confirmDeleteRecordDialogContent: @Composable (recordId: Long, onResult: (ResultModel) -> Unit, onDialogDismiss: () -> Unit) -> Unit,
    onEditAssetClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AssetInfoViewModel = hiltViewModel<AssetInfoViewModel>().apply {
        updateAssetId(assetId)
    },
) {

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    AssetInfoScreen(
        uiState = uiState,
        viewRecord = viewModel.viewRecordData,
        assetRecordListContent = { topContent ->
            assetRecordListContent(
                topContent = topContent,
                onRecordItemClick = viewModel::onRecordItemClick,
            )
        },
        recordDetailSheetContent = { record ->
            recordDetailSheetContent(
                recordInfo = record,
                onRecordDeleteClick = viewModel::onDeleteRecordClick,
                dismissBottomSheet = viewModel::dismissRecordDetailSheet,
            )
        },
        dialogState = viewModel.dialogState,
        confirmDeleteRecordDialogContent = { recordId ->
            confirmDeleteRecordDialogContent(
                recordId = recordId,
                onResult = viewModel::onDeleteRecordResult,
                onDialogDismiss = viewModel::dismissDeleteConfirmDialog,
            )
        },
        onEditAssetClick = onEditAssetClick,
        dismissBottomSheet = viewModel::dismissRecordDetailSheet,
        shouldDisplayBookmark = viewModel.shouldDisplayDeleteFailedBookmark,
        onBookmarkDismiss = viewModel::dismissBookmark,
        onBackClick = onBackClick,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AssetInfoScreen(
    uiState: AssetInfoUiState,
    viewRecord: RecordViewsEntity?,
    dialogState: DialogState,
    assetRecordListContent: @Composable (topContent: @Composable () -> Unit) -> Unit,
    recordDetailSheetContent: @Composable (RecordViewsEntity?) -> Unit,
    confirmDeleteRecordDialogContent: @Composable (recordId: Long) -> Unit,
    onEditAssetClick: () -> Unit,
    dismissBottomSheet: () -> Unit,
    shouldDisplayBookmark: Boolean,
    onBookmarkDismiss: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: ModalBottomSheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        confirmValueChange = {
            if (it == ModalBottomSheetValue.Hidden) {
                dismissBottomSheet()
            }
            true
        }),
    snackbarHostState: SnackbarHostState = remember {
        SnackbarHostState()
    }
) {

    if (sheetState.isVisible) {
        // sheet 显示时，返回隐藏 sheet
        BackPressHandler {
            dismissBottomSheet.invoke()
        }
    }

    // 显示数据不为空时，显示详情 sheet
    LaunchedEffect(viewRecord) {
        if (null != viewRecord) {
            // 显示详情弹窗
            sheetState.show()
        } else {
            sheetState.hide()
        }
    }

    // 提示
    val deleteFailedText = stringResource(id = R.string.delete_failed)
    LaunchedEffect(key1 = shouldDisplayBookmark, block = {
        if (shouldDisplayBookmark) {
            val result = snackbarHostState.showSnackbar(deleteFailedText)
            if (result == SnackbarResult.Dismissed) {
                onBookmarkDismiss()
            }
        }
    })

    CashbookBottomSheetScaffold(
        modifier = modifier,
        topBar = {
            CashbookTopAppBar(
                text = uiState.title,
                onBackClick = onBackClick,
                actions = {
                    IconButton(onClick = onEditAssetClick) {
                        Icon(imageVector = CashbookIcons.EditNote, contentDescription = null)
                    }
                    IconButton(onClick = { /*TODO*/ }) {
                        Icon(imageVector = CashbookIcons.MoreVert, contentDescription = null)
                    }
                },
            )
        },
        floatingActionButton = {
            CashbookFloatingActionButton(onClick = { /*TODO*/ }) {
                Icon(imageVector = CashbookIcons.Add, contentDescription = null)
            }
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState)
        },
        sheetState = sheetState,
        sheetContent = {
            recordDetailSheetContent.invoke(viewRecord)
        },
        content = { paddingValues ->
            Box(
                modifier = Modifier.padding(paddingValues),
            ) {
                (dialogState as? DialogState.Shown<*>)?.let {
                    val recordId = it.data
                    if (recordId is Long) {
                        // 显示删除确认弹窗
                        dismissBottomSheet.invoke()

                        confirmDeleteRecordDialogContent(
                            recordId = recordId,
                        )
                    }
                }

                when (uiState) {
                    is AssetInfoUiState.Loading -> {
                        Loading()
                    }

                    is AssetInfoUiState.Success -> {
                        assetRecordListContent(
                            topContent = {
                                AssetInfoContent(
                                    isCreditCard = uiState.isCreditCard,
                                    balance = uiState.balance,
                                    totalAmount = uiState.totalAmount,
                                    billingDate = uiState.billingDate,
                                    repaymentDate = uiState.repaymentDate,
                                )
                            },
                        )
                    }
                }
            }
        },
    )
}

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
        val isCreditCard = false
        val balance = "200"
        val totalAmount = "2000"
        val billingDate = "20"
        val repaymentDate = ""
        AssetInfoScreen(
            uiState = AssetInfoUiState.Success(
                assetName = "现金",
                isCreditCard = isCreditCard,
                balance = balance,
                totalAmount = totalAmount,
                billingDate = billingDate,
                repaymentDate = repaymentDate,
            ),
            viewRecord = null,
            assetRecordListContent = {
                Column {
                    AssetInfoContent(
                        isCreditCard = isCreditCard,
                        balance = balance,
                        totalAmount = totalAmount,
                        billingDate = billingDate,
                        repaymentDate = repaymentDate,
                    )

                    Empty(hintText = "无数据")
                }
            },
            recordDetailSheetContent = {},
            dialogState = DialogState.Dismiss,
            confirmDeleteRecordDialogContent = {},
            onEditAssetClick = {},
            dismissBottomSheet = {},
            shouldDisplayBookmark = false,
            onBookmarkDismiss = {},
            onBackClick = {},
            modifier = Modifier,
        )
    }
}

@DevicePreviews
@Composable
private fun CreditAssetInfoScreenPreview() {
    PreviewTheme(
        defaultEmptyImagePainter = painterResource(id = R.drawable.vector_no_data_200),
    ) {
        val isCreditCard = true
        val balance = "200"
        val totalAmount = "2000"
        val billingDate = "20"
        val repaymentDate = ""
        AssetInfoScreen(
            uiState = AssetInfoUiState.Success(
                assetName = "招商银行",
                isCreditCard = isCreditCard,
                balance = balance,
                totalAmount = totalAmount,
                billingDate = billingDate,
                repaymentDate = repaymentDate,
            ),
            viewRecord = null,
            assetRecordListContent = {
                Column {
                    AssetInfoContent(
                        isCreditCard = isCreditCard,
                        balance = balance,
                        totalAmount = totalAmount,
                        billingDate = billingDate,
                        repaymentDate = repaymentDate,
                    )

                    Empty(hintText = "无数据")
                }
            },
            recordDetailSheetContent = {},
            dialogState = DialogState.Dismiss,
            confirmDeleteRecordDialogContent = {},
            onEditAssetClick = {},
            dismissBottomSheet = {},
            shouldDisplayBookmark = false,
            onBookmarkDismiss = {},
            onBackClick = {},
            modifier = Modifier,
        )
    }
}

@DevicePreviews
@Composable
private fun AssetInfoScreenLoadingPreview() {
    PreviewTheme(
        defaultEmptyImagePainter = painterResource(id = R.drawable.vector_no_data_200),
    ) {
        AssetInfoScreen(
            uiState = AssetInfoUiState.Loading,
            viewRecord = null,
            assetRecordListContent = {},
            recordDetailSheetContent = {},
            dialogState = DialogState.Dismiss,
            confirmDeleteRecordDialogContent = {},
            onEditAssetClick = {},
            dismissBottomSheet = {},
            shouldDisplayBookmark = false,
            onBookmarkDismiss = {},
            onBackClick = {},
            modifier = Modifier,
        )
    }
}