package cn.wj.android.cashbook.feature.records.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.BackdropScaffold
import androidx.compose.material3.BackdropScaffoldState
import androidx.compose.material3.BackdropValue
import androidx.compose.material3.Divider
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberBackdropScaffoldState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.wj.android.cashbook.core.common.ext.completeZero
import cn.wj.android.cashbook.core.common.ext.decimalFormat
import cn.wj.android.cashbook.core.common.ext.toDoubleOrZero
import cn.wj.android.cashbook.core.common.ext.withCNY
import cn.wj.android.cashbook.core.design.component.CashbookFloatingActionButton
import cn.wj.android.cashbook.core.design.component.CashbookGradientBackground
import cn.wj.android.cashbook.core.design.component.CashbookModalBottomSheet
import cn.wj.android.cashbook.core.design.component.CashbookScaffold
import cn.wj.android.cashbook.core.design.component.Empty
import cn.wj.android.cashbook.core.design.component.Footer
import cn.wj.android.cashbook.core.design.component.Loading
import cn.wj.android.cashbook.core.design.component.TransparentListItem
import cn.wj.android.cashbook.core.design.component.painterDrawableResource
import cn.wj.android.cashbook.core.design.icon.CashbookIcons
import cn.wj.android.cashbook.core.design.theme.LocalExtendedColors
import cn.wj.android.cashbook.core.design.theme.PreviewTheme
import cn.wj.android.cashbook.core.model.entity.RecordDayEntity
import cn.wj.android.cashbook.core.model.entity.RecordViewsEntity
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.ui.DevicePreviews
import cn.wj.android.cashbook.core.ui.DialogState
import cn.wj.android.cashbook.core.ui.R
import cn.wj.android.cashbook.feature.records.dialog.SelectDateDialog
import cn.wj.android.cashbook.feature.records.viewmodel.LauncherContentUiState
import cn.wj.android.cashbook.feature.records.viewmodel.LauncherContentViewModel
import java.time.YearMonth

@Composable
internal fun LauncherContentRoute(
    recordDetailSheetContent: @Composable (recordInfo: RecordViewsEntity?, dismissBottomSheet: () -> Unit) -> Unit,
    onEditRecordClick: (Long) -> Unit,
    onMenuClick: () -> Unit,
    onSearchClick: () -> Unit,
    onCalendarClick: () -> Unit,
    onMyAssetClick: () -> Unit,
    onShowSnackbar: suspend (String, String?) -> SnackbarResult,
    modifier: Modifier = Modifier,
    viewModel: LauncherContentViewModel = hiltViewModel(),
) {

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val date by viewModel.dateData.collectAsStateWithLifecycle()

    LauncherContentScreen(
        shouldDisplayDeleteFailedBookmark = viewModel.shouldDisplayDeleteFailedBookmark,
        onBookmarkDismiss = viewModel::onBookmarkDismiss,
        onShowSnackbar = onShowSnackbar,
        recordDetailSheetContent = { record ->
            recordDetailSheetContent(
                recordInfo = record,
                dismissBottomSheet = viewModel::onSheetDismiss,
            )
        },
        viewRecord = viewModel.viewRecord,
        date = date,
        onMenuClick = onMenuClick,
        onDateClick = viewModel::showDateSelectDialog,
        onDateSelected = viewModel::onDateSelected,
        onSearchClick = onSearchClick,
        onCalendarClick = onCalendarClick,
        onMyAssetClick = onMyAssetClick,
        onAddClick = { onEditRecordClick.invoke(-1L) },
        uiState = uiState,
        onRecordItemClick = viewModel::onRecordItemClick,
        dialogState = viewModel.dialogState,
        onDialogDismiss = viewModel::onDialogDismiss,
        onSheetDismiss = viewModel::onSheetDismiss,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LauncherContentScreen(
    // 删除失败提示
    shouldDisplayDeleteFailedBookmark: Int,
    onBookmarkDismiss: () -> Unit,
    onShowSnackbar: suspend (String, String?) -> SnackbarResult,
    // 记录详情
    recordDetailSheetContent: @Composable (RecordViewsEntity?) -> Unit,
    viewRecord: RecordViewsEntity?,
    // 标题栏
    date: YearMonth,
    onMenuClick: () -> Unit,
    onDateClick: () -> Unit,
    onDateSelected: (YearMonth) -> Unit,
    onSearchClick: () -> Unit,
    onCalendarClick: () -> Unit,
    onMyAssetClick: () -> Unit,
    // 添加按钮
    onAddClick: () -> Unit,
    // 月收支信息
    uiState: LauncherContentUiState,
    onRecordItemClick: (RecordViewsEntity) -> Unit,
    // 弹窗信息
    dialogState: DialogState,
    onDialogDismiss: () -> Unit,
    onSheetDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    scaffoldState: BackdropScaffoldState = rememberBackdropScaffoldState(initialValue = BackdropValue.Revealed),
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
            LauncherTopBar(
                dateStr = "${date.year}-${date.monthValue.completeZero()}",
                onMenuClick = onMenuClick,
                onDateClick = onDateClick,
                onSearchClick = onSearchClick,
                onCalendarClick = onCalendarClick,
                onMyAssetClick = onMyAssetClick,
            )
        },
        floatingActionButton = {
            CashbookFloatingActionButton(onClick = onAddClick) {
                Icon(imageVector = CashbookIcons.Add, contentDescription = null)
            }
        },
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            if (null != viewRecord) {
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
                        recordDetailSheetContent(viewRecord)
                    },
                )
            }

            when (uiState) {
                LauncherContentUiState.Loading -> {
                    Loading(modifier = Modifier.align(Alignment.Center))
                }

                is LauncherContentUiState.Success -> {
                    BackdropScaffold(
                        scaffoldState = scaffoldState,
                        appBar = { /* 使用上层 topBar 处理 */ },
                        peekHeight = paddingValues.calculateTopPadding(),
                        backLayerBackgroundColor = MaterialTheme.colorScheme.tertiaryContainer,
                        backLayerContent = {
                            BackLayerContent(
                                paddingValues = paddingValues,
                                monthIncome = uiState.monthIncome,
                                monthExpand = uiState.monthExpand,
                                monthBalance = uiState.monthBalance,
                            )
                        },
                        frontLayerScrimColor = Color.Unspecified,
                        frontLayerContent = {
                            FrontLayerContent(
                                onDateSelected = onDateSelected,
                                dialogState = dialogState,
                                onDialogDismiss = onDialogDismiss,
                                recordMap = uiState.recordMap,
                                onDateClick = onDateClick,
                                onRecordItemClick = onRecordItemClick,
                            )
                        },
                    )
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun FrontLayerContent(
    onDateSelected: (YearMonth) -> Unit,
    dialogState: DialogState,
    onDialogDismiss: () -> Unit,
    recordMap: Map<RecordDayEntity, List<RecordViewsEntity>>,
    onDateClick: () -> Unit,
    onRecordItemClick: (RecordViewsEntity) -> Unit
) {
    CashbookGradientBackground {
        Box(
            modifier = Modifier.fillMaxSize(),
        ) {
            (dialogState as? DialogState.Shown<*>)?.data?.let { date ->
                if (date is YearMonth) {
                    SelectDateDialog(
                        onDialogDismiss = onDialogDismiss,
                        date = date,
                        onDateSelected = onDateSelected,
                    )
                }
            }

            if (recordMap.isEmpty()) {
                Empty(
                    hintText = stringResource(id = R.string.launcher_no_data_hint),
                    button = {
                        FilledTonalButton(onClick = onDateClick) {
                            Text(text = stringResource(id = R.string.launcher_no_data_button))
                        }
                    },
                    modifier = Modifier.align(Alignment.TopCenter),
                )
            } else {
                LazyColumn {
                    recordMap.keys.reversed().forEach { key ->
                        val recordList = recordMap[key] ?: listOf()
                        stickyHeader {
                            Row(
                                modifier = Modifier
                                    .background(color = MaterialTheme.colorScheme.surface)
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    modifier = Modifier.weight(1f),
                                    text = when (key.dayType) {
                                        0 -> stringResource(id = R.string.today)
                                        -1 -> stringResource(id = R.string.yesterday)
                                        -2 -> stringResource(id = R.string.before_yesterday)
                                        else -> "${key.day}${stringResource(id = R.string.day)}"
                                    }
                                )
                                Text(
                                    text = buildString {
                                        val totalIncome = key.dayIncome.toDoubleOrZero()
                                        val totalExpenditure = key.dayExpand.toDoubleOrZero()
                                        val hasIncome = totalIncome != 0.0
                                        if (hasIncome) {
                                            append(
                                                stringResource(id = R.string.income_with_colon) + totalIncome.decimalFormat()
                                                    .withCNY()
                                            )
                                        }
                                        if (totalExpenditure != 0.0) {
                                            if (hasIncome) {
                                                append(", ")
                                            }
                                            append(
                                                stringResource(id = R.string.expend_with_colon) + totalExpenditure.decimalFormat()
                                                    .withCNY()
                                            )
                                        }
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            Divider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = DividerDefaults.color.copy(0.3f),
                            )
                        }
                        items(recordList, key = { it.id }) {
                            RecordListItem(
                                recordViewsEntity = it,
                                onRecordItemClick = {
                                    onRecordItemClick(it)
                                },
                            )
                        }
                    }

                    item {
                        Footer(hintText = stringResource(id = R.string.footer_hint_default))
                    }
                }
            }
        }
    }
}

@Composable
private fun BackLayerContent(
    paddingValues: PaddingValues,
    monthIncome: String,
    monthExpand: String,
    monthBalance: String,
    modifier: Modifier = Modifier,
) {
    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onTertiaryContainer) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(paddingValues)
                .padding(16.dp),
        ) {
            Text(text = stringResource(id = R.string.month_income))
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = monthIncome.withCNY())
            Spacer(modifier = Modifier.height(24.dp))
            Row {
                Text(
                    modifier = Modifier.weight(1f),
                    text = "${stringResource(id = R.string.month_expend)} ${monthExpand.withCNY()}",
                )
                Text(
                    modifier = Modifier.weight(1f),
                    text = "${stringResource(id = R.string.month_balance)} ${monthBalance.withCNY()}",
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LauncherTopBar(
    dateStr: String,
    onMenuClick: () -> Unit,
    onDateClick: () -> Unit,
    onSearchClick: () -> Unit,
    onCalendarClick: () -> Unit,
    onMyAssetClick: () -> Unit,
) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
        ),
        title = {
            Text(
                text = dateStr,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .clickable(onClick = onDateClick)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        },
        navigationIcon = {
            IconButton(onClick = onMenuClick) {
                Icon(
                    imageVector = CashbookIcons.Menu,
                    contentDescription = null,
                )
            }
        },
        actions = {
            IconButton(onClick = onSearchClick) {
                Icon(
                    imageVector = CashbookIcons.Search,
                    contentDescription = null,
                )
            }
            IconButton(onClick = onCalendarClick) {
                Icon(
                    imageVector = CashbookIcons.CalendarMonth,
                    contentDescription = null,
                )
            }
            IconButton(onClick = onMyAssetClick) {
                Icon(
                    imageVector = CashbookIcons.WebAsset,
                    contentDescription = null,
                )
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RecordListItem(
    recordViewsEntity: RecordViewsEntity,
    onRecordItemClick: () -> Unit,
) {
    TransparentListItem(
        modifier = Modifier.clickable {
            onRecordItemClick()
        },
        leadingContent = {
            Icon(
                painter = painterDrawableResource(idStr = recordViewsEntity.typeIconResName),
                contentDescription = null
            )
        },
        headlineContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = recordViewsEntity.typeName)
                val tags = recordViewsEntity.relatedTags
                if (tags.isNotEmpty()) {
                    val tagsText = with(StringBuilder()) {
                        tags.forEach { tag ->
                            if (!isBlank()) {
                                append(",")
                            }
                            append(tag.name)
                        }
                        var result = toString()
                        if (result.length > 12) {
                            result = result.substring(0, 12) + "…"
                        }
                        result
                    }
                    Text(
                        text = tagsText,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .background(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = MaterialTheme.shapes.small
                            )
                            .padding(horizontal = 4.dp),
                    )
                }
            }
        },
        supportingContent = {
            Text(
                text = "${
                    recordViewsEntity.recordTime.split(" ").first()
                } ${recordViewsEntity.remark}"
            )
        },
        trailingContent = {
            Column(
                horizontalAlignment = Alignment.End
            ) {
                // TODO 关联记录
                Text(
                    text = buildAnnotatedString {
                        append(recordViewsEntity.amount.withCNY())
                    },
                    color = when (recordViewsEntity.typeCategory) {
                        RecordTypeCategoryEnum.EXPENDITURE -> LocalExtendedColors.current.expenditure
                        RecordTypeCategoryEnum.INCOME -> LocalExtendedColors.current.income
                        RecordTypeCategoryEnum.TRANSFER -> LocalExtendedColors.current.transfer
                    },
                    style = MaterialTheme.typography.labelLarge,
                )
                recordViewsEntity.assetName?.let { assetName ->
                    Text(text = buildAnnotatedString {
                        val hasCharges = recordViewsEntity.charges.toDoubleOrZero() > 0.0
                        val hasConcessions = recordViewsEntity.concessions.toDoubleOrZero() > 0.0
                        if (hasCharges || hasConcessions) {
                            // 有手续费、优惠信息
                            withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.onSurface)) {
                                append("(")
                            }
                            if (hasCharges) {
                                withStyle(style = SpanStyle(color = LocalExtendedColors.current.expenditure)) {
                                    append("-${recordViewsEntity.charges}".withCNY())
                                }
                            }
                            if (hasConcessions) {
                                if (hasCharges) {
                                    append(" ")
                                }
                                withStyle(style = SpanStyle(color = LocalExtendedColors.current.income)) {
                                    append("+${recordViewsEntity.concessions.withCNY()}")
                                }
                            }
                            withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.onSurface)) {
                                append(") ")
                            }
                        }
                        append(assetName)
                        if (recordViewsEntity.typeCategory == RecordTypeCategoryEnum.TRANSFER) {
                            append(" -> ${recordViewsEntity.relatedAssetName}")
                        }
                    })
                }
            }
        },
    )
}

@DevicePreviews
@Composable
internal fun LauncherContentScreenPreview() {
    PreviewTheme(
        defaultEmptyImagePainter = painterResource(id = R.drawable.vector_no_data_200),
    ) {
        LauncherContentRoute(
            recordDetailSheetContent = { _, _ -> },
            onEditRecordClick = {},
            onMenuClick = {},
            onSearchClick = {},
            onCalendarClick = {},
            onMyAssetClick = {},
            onShowSnackbar = { _, _ -> SnackbarResult.Dismissed },
        )
    }
}