package cn.wj.android.cashbook.feature.records.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.wj.android.cashbook.core.common.ext.completeZero
import cn.wj.android.cashbook.core.common.ext.toBigDecimalOrZero
import cn.wj.android.cashbook.core.common.ext.withCNY
import cn.wj.android.cashbook.core.common.ext.yearMonth
import cn.wj.android.cashbook.core.design.component.CalendarView
import cn.wj.android.cashbook.core.design.component.CashbookModalBottomSheet
import cn.wj.android.cashbook.core.design.component.CashbookScaffold
import cn.wj.android.cashbook.core.design.component.CashbookTopAppBar
import cn.wj.android.cashbook.core.design.component.Empty
import cn.wj.android.cashbook.core.design.theme.LocalExtendedColors
import cn.wj.android.cashbook.core.design.theme.PreviewTheme
import cn.wj.android.cashbook.core.model.entity.RecordViewsEntity
import cn.wj.android.cashbook.core.ui.DevicePreviews
import cn.wj.android.cashbook.core.ui.DialogState
import cn.wj.android.cashbook.core.ui.R
import cn.wj.android.cashbook.feature.records.dialog.ConfirmDeleteRecordDialog
import cn.wj.android.cashbook.feature.records.dialog.SelectDateDialog
import cn.wj.android.cashbook.feature.records.viewmodel.CalendarUiState
import cn.wj.android.cashbook.feature.records.viewmodel.CalendarViewModel
import cn.wj.android.cashbook.feature.records.viewmodel.DialogType
import java.math.BigDecimal
import java.time.LocalDate

@Composable
internal fun CalendarRoute(
    recordDetailSheetContent: @Composable (recordInfo: RecordViewsEntity?, onRecordDeleteClick: (Long) -> Unit, dismissBottomSheet: () -> Unit) -> Unit,
    onBackClick: () -> Unit,
    onShowSnackbar: suspend (String, String?) -> SnackbarResult,
    modifier: Modifier = Modifier,
    viewModel: CalendarViewModel = hiltViewModel(),
) {

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedDate by viewModel.dateData.collectAsStateWithLifecycle()

    CalendarScreen(
        shouldDisplayDeleteFailedBookmark = viewModel.shouldDisplayDeleteFailedBookmark,
        onBookmarkDismiss = viewModel::onBookmarkDismiss,
        selectedDate = selectedDate,
        onDateClick = viewModel::showDateSelectDialog,
        uiState = uiState,
        dialogState = viewModel.dialogState,
        onDialogDismiss = viewModel::onDialogDismiss,
        onDateSelected = viewModel::onDateSelected,
        onRecordItemClick = viewModel::onRecordItemClick,
        recordDetailSheetContent = { record ->
            recordDetailSheetContent(
                recordInfo = record,
                onRecordDeleteClick = viewModel::onRecordItemDeleteClick,
                dismissBottomSheet = viewModel::onSheetDismiss,
            )
        },
        sheetViewData = viewModel.viewRecord,
        onSheetDismiss = viewModel::onSheetDismiss,
        onDeleteRecordConfirm = viewModel::onDeleteRecordConfirm,
        onBackClick = onBackClick,
        onShowSnackbar = onShowSnackbar,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CalendarScreen(
    shouldDisplayDeleteFailedBookmark: Int,
    onBookmarkDismiss: () -> Unit,
    selectedDate: LocalDate,
    onDateClick: () -> Unit,
    uiState: CalendarUiState,
    dialogState: DialogState,
    onDialogDismiss: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    onRecordItemClick: (RecordViewsEntity) -> Unit,
    recordDetailSheetContent: @Composable (RecordViewsEntity?) -> Unit,
    sheetViewData: RecordViewsEntity?,
    onSheetDismiss: () -> Unit,
    onDeleteRecordConfirm: (Long) -> Unit,
    onBackClick: () -> Unit,
    onShowSnackbar: suspend (String, String?) -> SnackbarResult,
    modifier: Modifier = Modifier,
) {
    // 提示文本
    val deleteFailedFormatText = stringResource(id = R.string.delete_failed_format)
    // 显示提示
    LaunchedEffect(shouldDisplayDeleteFailedBookmark) {
        if (shouldDisplayDeleteFailedBookmark > 0) {
            val result = onShowSnackbar.invoke(
                deleteFailedFormatText.format(shouldDisplayDeleteFailedBookmark), null
            )
            if (SnackbarResult.Dismissed == result) {
                onBookmarkDismiss.invoke()
            }
        }
    }

    CashbookScaffold(
        modifier = modifier,
        topBar = {
            CashbookTopAppBar(onBackClick = onBackClick, title = {
                Text(
                    text = "${selectedDate.year}-${selectedDate.monthValue.completeZero()}",
                    modifier = Modifier
                        .clickable(onClick = onDateClick)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
            })
        },
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            if (null != sheetViewData) {
                CashbookModalBottomSheet(
                    onDismissRequest = onSheetDismiss,
                    sheetState = rememberModalBottomSheetState(
                        confirmValueChange = {
                            if (it == SheetValue.Hidden) {
                                onSheetDismiss()
                            }
                            true
                        },
                    ),
                    content = {
                        recordDetailSheetContent(sheetViewData)
                    },
                )
            }

            (dialogState as? DialogState.Shown<*>)?.let {
                when (val dialogType = it.data as? DialogType) {
                    is DialogType.DeleteRecord -> {
                        // 显示删除确认弹窗
                        onSheetDismiss()
                        ConfirmDeleteRecordDialog(
                            recordId = dialogType.recordId,
                            onDeleteRecordConfirm = onDeleteRecordConfirm,
                            onDialogDismiss = onDialogDismiss,
                        )
                    }

                    is DialogType.SelectDate -> {
                        SelectDateDialog(
                            onDialogDismiss = onDialogDismiss,
                            date = selectedDate.yearMonth,
                            onDateSelected = { ym ->
                                if (selectedDate.yearMonth != ym) {
                                    onDateSelected(ym.atDay(1))
                                }
                            },
                        )
                    }

                    null -> {
                        // empty block
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                content = {
                    item {
                        CalendarView(
                            selectDate = selectedDate,
                            onDateSelected = onDateSelected,
                            schemeContent = { date, _ ->
                                if (uiState is CalendarUiState.Success) {
                                    uiState.schemas[date]?.let {
                                        if (it.dayIncome.toBigDecimalOrZero() != BigDecimal.ZERO) {
                                            Text(
                                                text = it.dayIncome.withCNY(),
                                                color = LocalExtendedColors.current.income,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontSize = 8.sp
                                                ),
                                                modifier = Modifier.padding(
                                                    start = 4.dp, top = 4.dp
                                                ),
                                            )
                                        }
                                        if (it.dayExpand.toBigDecimalOrZero() != BigDecimal.ZERO) {
                                            Text(
                                                text = it.dayExpand.withCNY(),
                                                color = LocalExtendedColors.current.expenditure,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontSize = 8.sp
                                                ),
                                                modifier = Modifier
                                                    .align(Alignment.BottomEnd)
                                                    .padding(end = 4.dp, bottom = 4.dp),
                                            )
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    when (uiState) {
                        CalendarUiState.Loading -> {
                            item {
                                Empty(
                                    hintText = stringResource(id = R.string.no_record_data),
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }

                        is CalendarUiState.Success -> {
                            if (uiState.recordList.isEmpty()) {
                                item {
                                    Empty(
                                        hintText = stringResource(id = R.string.no_record_data),
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                            } else {
                                item {
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp)
                                            .padding(top = 8.dp),
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 16.dp)
                                        ) {
                                            CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onPrimaryContainer) {
                                                ProvideTextStyle(value = MaterialTheme.typography.labelLarge) {
                                                    Text(
                                                        text = stringResource(id = R.string.month_income),
                                                        textAlign = TextAlign.Center,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                    Text(
                                                        text = stringResource(id = R.string.month_expend),
                                                        textAlign = TextAlign.Center,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                    Text(
                                                        text = stringResource(id = R.string.month_balance),
                                                        textAlign = TextAlign.Center,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                }
                                            }
                                        }
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 8.dp, bottom = 16.dp),
                                        ) {
                                            ProvideTextStyle(value = MaterialTheme.typography.labelMedium) {
                                                Text(
                                                    text = uiState.monthIncome.withCNY(),
                                                    color = LocalExtendedColors.current.income,
                                                    textAlign = TextAlign.Center,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Text(
                                                    text = uiState.monthExpand.withCNY(),
                                                    color = LocalExtendedColors.current.expenditure,
                                                    textAlign = TextAlign.Center,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Text(
                                                    text = uiState.monthBalance.withCNY(),
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(
                                                        alpha = 0.5f
                                                    ),
                                                    textAlign = TextAlign.Center,
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                        }
                                    }
                                }
                                items(uiState.recordList) {
                                    RecordListItem(
                                        recordViewsEntity = it,
                                        onRecordItemClick = {
                                            onRecordItemClick(it)
                                        },
                                    )
                                }
                            }
                        }
                    }
                },
            )
        }
    }
}

@DevicePreviews
@Composable
private fun CalendarPreview() {
    PreviewTheme(defaultEmptyImagePainter = painterResource(id = R.drawable.vector_no_data_200)) {
        CalendarRoute(recordDetailSheetContent = { _, _, _ -> },
            onBackClick = {},
            onShowSnackbar = { _, _ -> SnackbarResult.Dismissed })
    }
}