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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheetLayout
import androidx.compose.material3.ModalBottomSheetState
import androidx.compose.material3.ModalBottomSheetValue
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberBackdropScaffoldState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
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
import cn.wj.android.cashbook.core.common.ext.decimalFormat
import cn.wj.android.cashbook.core.common.ext.toDoubleOrZero
import cn.wj.android.cashbook.core.common.ext.toIntOrZero
import cn.wj.android.cashbook.core.common.ext.withCNY
import cn.wj.android.cashbook.core.design.component.CashbookFloatingActionButton
import cn.wj.android.cashbook.core.design.component.CashbookGradientBackground
import cn.wj.android.cashbook.core.design.component.CashbookScaffold
import cn.wj.android.cashbook.core.design.component.CommonDivider
import cn.wj.android.cashbook.core.design.component.Empty
import cn.wj.android.cashbook.core.design.component.Footer
import cn.wj.android.cashbook.core.design.component.Loading
import cn.wj.android.cashbook.core.design.component.TranparentListItem
import cn.wj.android.cashbook.core.design.component.painterDrawableResource
import cn.wj.android.cashbook.core.design.icon.CashbookIcons
import cn.wj.android.cashbook.core.design.theme.LocalExtendedColors
import cn.wj.android.cashbook.core.design.theme.PreviewTheme
import cn.wj.android.cashbook.core.model.entity.RecordDayEntity
import cn.wj.android.cashbook.core.model.entity.RecordViewsEntity
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.ui.BackPressHandler
import cn.wj.android.cashbook.core.ui.DevicePreviews
import cn.wj.android.cashbook.core.ui.DialogState
import cn.wj.android.cashbook.core.ui.R
import cn.wj.android.cashbook.feature.records.dialog.ConfirmDeleteRecordDialog
import cn.wj.android.cashbook.feature.records.viewmodel.LauncherContentUiState
import cn.wj.android.cashbook.feature.records.viewmodel.LauncherContentViewModel
import java.util.Calendar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
internal fun LauncherContentRoute(
    onEditRecordClick: (Long) -> Unit,
    onMenuClick: () -> Unit,
    onSearchClick: () -> Unit,
    onCalendarClick: () -> Unit,
    onMyAssetClick: () -> Unit,
    onShowSnackbar: suspend (String, String?) -> SnackbarResult,
    modifier: Modifier = Modifier,
    viewModel: LauncherContentViewModel = hiltViewModel(),
) {

    val currentBookName by viewModel.currentBookName.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LauncherContentScreen(
        shouldDisplayDeleteFailedBookmark = viewModel.shouldDisplayDeleteFailedBookmark,
        onBookmarkDismiss = viewModel::onBookmarkDismiss,
        onShowSnackbar = onShowSnackbar,
        viewRecord = viewModel.viewRecord,
        onRecordItemEditClick = {
            viewModel.onRecordDetailsSheetDismiss()
            onEditRecordClick.invoke(it)
        },
        onRecordItemDeleteClick = viewModel::onRecordItemDeleteClick,
        bookName = currentBookName,
        onMenuClick = onMenuClick,
        onSearchClick = onSearchClick,
        onCalendarClick = onCalendarClick,
        onMyAssetClick = onMyAssetClick,
        onAddClick = { onEditRecordClick.invoke(-1L) },
        uiState = uiState,
        onRecordItemClick = viewModel::onRecordItemClick,
        dialogState = viewModel.dialogState,
        onDeleteRecordConfirm = viewModel::onDeleteRecordConfirm,
        onDialogDismiss = viewModel::onDialogDismiss,
        sheetState = rememberModalBottomSheetState(
            initialValue = ModalBottomSheetValue.Hidden,
            confirmValueChange = {
                if (it == ModalBottomSheetValue.Hidden) {
                    // sheet 隐藏
                    viewModel.onRecordDetailsSheetDismiss()
                }
                true
            },
        ),
        modifier = modifier,
    )
}

@Composable
internal fun LauncherContentScreen(
    // 删除失败提示
    shouldDisplayDeleteFailedBookmark: Int,
    onBookmarkDismiss: () -> Unit,
    onShowSnackbar: suspend (String, String?) -> SnackbarResult,
    // 记录详情
    viewRecord: RecordViewsEntity?,
    onRecordItemEditClick: (Long) -> Unit,
    onRecordItemDeleteClick: (Long) -> Unit,
    // 标题栏
    bookName: String,
    onMenuClick: () -> Unit,
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
    onDeleteRecordConfirm: (Long) -> Unit,
    onDialogDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: ModalBottomSheetState = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden),
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
) {

    if (sheetState.isVisible) {
        // sheet 显示时，返回隐藏 sheet
        BackPressHandler {
            coroutineScope.launch {
                sheetState.hide()
            }
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

    ModalBottomSheetLayout(
        modifier = modifier,
        sheetState = sheetState,
        content = {
            LauncherLayoutContent(
                bookName = bookName,
                onMenuClick = onMenuClick,
                onSearchClick = onSearchClick,
                onCalendarClick = onCalendarClick,
                onMyAssetClick = onMyAssetClick,
                onAddClick = onAddClick,
                uiState = uiState,
                onRecordItemClick = onRecordItemClick,
                dialogState = dialogState,
                onDeleteRecordConfirm = onDeleteRecordConfirm,
                onDialogDismiss = onDialogDismiss,
                sheetState = sheetState,
            )
        },
        sheetContent = {
            LauncherLayoutSheetContent(
                recordEntity = viewRecord,
                onRecordItemEditClick = onRecordItemEditClick,
                onRecordItemDeleteClick = onRecordItemDeleteClick,
            )
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LauncherLayoutContent(
    // 标题栏
    bookName: String,
    onMenuClick: () -> Unit,
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
    onDeleteRecordConfirm: (Long) -> Unit,
    onDialogDismiss: () -> Unit,
    // sheet 状态
    sheetState: ModalBottomSheetState = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden),
) {
    CashbookScaffold(
        topBar = {
            LauncherTopBar(
                bookName = bookName,
                onMenuClick = onMenuClick,
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
        LauncherLayoutBackdropScaffold(
            paddingValues = paddingValues,
            uiState = uiState,
            onCalendarClick = onCalendarClick,
            onRecordItemClick = onRecordItemClick,
            dialogState = dialogState,
            onDeleteRecordConfirm = onDeleteRecordConfirm,
            onDialogDismiss = onDialogDismiss,
            sheetState = sheetState,
        )
    }
}

@Composable
internal fun LauncherLayoutBackdropScaffold(
    paddingValues: PaddingValues,
    // 月收支信息
    uiState: LauncherContentUiState,
    onCalendarClick: () -> Unit,
    onRecordItemClick: (RecordViewsEntity) -> Unit,
    // 弹窗信息
    dialogState: DialogState,
    onDeleteRecordConfirm: (Long) -> Unit,
    onDialogDismiss: () -> Unit,
    sheetState: ModalBottomSheetState = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden),
    scaffoldState: BackdropScaffoldState = rememberBackdropScaffoldState(initialValue = BackdropValue.Revealed),
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
) {
    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        when (uiState) {
            is LauncherContentUiState.Loading -> {
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
                            dialogState = dialogState,
                            coroutineScope = coroutineScope,
                            sheetState = sheetState,
                            onDialogDismiss = onDialogDismiss,
                            onDeleteRecordConfirm = onDeleteRecordConfirm,
                            recordMap = uiState.recordMap,
                            onCalendarClick = onCalendarClick,
                            onRecordItemClick = onRecordItemClick,
                        )
                    },
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun FrontLayerContent(
    dialogState: DialogState,
    coroutineScope: CoroutineScope,
    sheetState: ModalBottomSheetState,
    onDialogDismiss: () -> Unit,
    onDeleteRecordConfirm: (Long) -> Unit,
    recordMap: Map<RecordDayEntity, List<RecordViewsEntity>>,
    onCalendarClick: () -> Unit,
    onRecordItemClick: (RecordViewsEntity) -> Unit
) {
    CashbookGradientBackground {
        Box(
            modifier = Modifier.fillMaxSize(),
        ) {
            (dialogState as? DialogState.Shown<*>)?.let {
                val recordId = it.data
                if (recordId is Long) {
                    // 显示删除确认弹窗
                    coroutineScope.launch {
                        sheetState.hide()
                    }
                    ConfirmDeleteRecordDialog(
                        recordId = recordId,
                        onDeleteRecordConfirm = onDeleteRecordConfirm,
                        onDialogDismiss = onDialogDismiss,
                    )
                }
            }

            if (recordMap.isEmpty()) {
                Empty(
                    hintText = stringResource(id = R.string.launcher_no_data_hint),
                    buttonText = stringResource(id = R.string.launcher_no_data_button),
                    onButtonClick = onCalendarClick,
                )
            } else {
                val todayInt = Calendar.getInstance()[Calendar.DAY_OF_MONTH]
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
                                    text = when (key.day.toIntOrZero()) {
                                        todayInt -> stringResource(id = R.string.today)
                                        todayInt - 1 -> stringResource(id = R.string.yesterday)
                                        todayInt - 2 -> stringResource(id = R.string.before_yesterday)
                                        else -> key.day + stringResource(id = R.string.day)
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
                                    onRecordItemClick.invoke(it)
                                    coroutineScope.launch {
                                        sheetState.show()
                                    }
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

@Composable
internal fun LauncherLayoutSheetContent(
    recordEntity: RecordViewsEntity?,
    onRecordItemEditClick: (Long) -> Unit,
    onRecordItemDeleteClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    RecordDetailsSheet(
        recordEntity = recordEntity,
        onRecordItemEditClick = onRecordItemEditClick,
        onRecordItemDeleteClick = onRecordItemDeleteClick,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LauncherTopBar(
    bookName: String,
    onMenuClick: () -> Unit,
    onSearchClick: () -> Unit,
    onCalendarClick: () -> Unit,
    onMyAssetClick: () -> Unit,
) {
    TopAppBar(
        colors = TopAppBarDefaults.smallTopAppBarColors(
            containerColor = Color.Transparent,
        ),
        title = {
            Text(text = bookName)
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
    TranparentListItem(
        modifier = Modifier.clickable {
            onRecordItemClick()
        },
        leadingContent = {
            Icon(
                painter = painterDrawableResource(idStr = recordViewsEntity.typeIconResName),
                contentDescription = null
            )
        },
        headlineText = {
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
        supportingText = {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RecordDetailsSheet(
    recordEntity: RecordViewsEntity?,
    onRecordItemEditClick: (Long) -> Unit,
    onRecordItemDeleteClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    CashbookGradientBackground {
        Box(
            modifier = modifier.fillMaxWidth(),
        ) {
            if (null == recordEntity) {
                // 无数据
                Empty(
                    hintText = stringResource(id = R.string.no_record_data),
                )
            } else {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(id = R.string.record_details),
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(
                            onClick = { onRecordItemEditClick(recordEntity.id) },
                        ) {
                            Text(
                                text = stringResource(id = R.string.edit),
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        TextButton(
                            onClick = { onRecordItemDeleteClick(recordEntity.id) },
                        ) {
                            Text(
                                text = stringResource(id = R.string.delete),
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                    CommonDivider()

                    // 金额
                    TranparentListItem(
                        headlineText = { Text(text = stringResource(id = R.string.amount)) },
                        trailingContent = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                if (recordEntity.typeCategory == RecordTypeCategoryEnum.EXPENDITURE && recordEntity.reimbursable) {
                                    // 支出类型，并且可报销
                                    val text = if (recordEntity.relatedRecord.isEmpty()) {
                                        // 未报销
                                        stringResource(id = R.string.reimbursable)
                                    } else {
                                        // 已报销
                                        stringResource(id = R.string.reimbursed)
                                    }
                                    Text(
                                        text = text,
                                        style = MaterialTheme.typography.labelMedium,
                                        modifier = Modifier.padding(end = 8.dp),
                                    )
                                }
                                Text(
                                    text = recordEntity.amount.withCNY(),
                                    color = when (recordEntity.typeCategory) {
                                        RecordTypeCategoryEnum.EXPENDITURE -> LocalExtendedColors.current.expenditure
                                        RecordTypeCategoryEnum.INCOME -> LocalExtendedColors.current.income
                                        RecordTypeCategoryEnum.TRANSFER -> LocalExtendedColors.current.transfer
                                    },
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            }
                        },
                    )

                    if (recordEntity.charges.toDoubleOrZero() > 0.0) {
                        // 手续费
                        TranparentListItem(
                            headlineText = { Text(text = stringResource(id = R.string.charges)) },
                            trailingContent = {
                                Text(
                                    text = "-${recordEntity.charges}".withCNY(),
                                    color = LocalExtendedColors.current.expenditure,
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            },
                        )
                    }

                    if (recordEntity.typeCategory != RecordTypeCategoryEnum.INCOME && recordEntity.concessions.toDoubleOrZero() > 0.0) {
                        // 优惠
                        TranparentListItem(
                            headlineText = { Text(text = stringResource(id = R.string.concessions)) },
                            trailingContent = {
                                Text(
                                    text = "+${recordEntity.concessions.withCNY()}",
                                    color = LocalExtendedColors.current.income,
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            },
                        )
                    }

                    // 类型
                    TranparentListItem(
                        headlineText = { Text(text = stringResource(id = R.string.type)) },
                        trailingContent = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterDrawableResource(idStr = recordEntity.typeIconResName),
                                    contentDescription = null,
                                    modifier = Modifier.padding(end = 8.dp),
                                )
                                Text(
                                    text = recordEntity.typeName,
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            }
                        },
                    )

                    // TODO 关联的记录

                    recordEntity.assetName?.let { assetName ->
                        // 资产
                        TranparentListItem(
                            headlineText = { Text(text = stringResource(id = R.string.asset)) },
                            trailingContent = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        painter = painterResource(id = recordEntity.assetIconResId!!),
                                        contentDescription = null,
                                        tint = Color.Unspecified,
                                        modifier = Modifier.padding(end = 8.dp),
                                    )
                                    Text(
                                        text = assetName,
                                        style = MaterialTheme.typography.labelLarge,
                                    )
                                    // 关联资产
                                    recordEntity.relatedAssetName?.let { relatedName ->
                                        Text(
                                            text = "->",
                                            style = MaterialTheme.typography.labelLarge,
                                            modifier = Modifier.padding(horizontal = 8.dp),
                                        )
                                        Icon(
                                            painter = painterResource(id = recordEntity.relatedAssetIconResId!!),
                                            contentDescription = null,
                                            tint = Color.Unspecified,
                                            modifier = Modifier.padding(end = 8.dp),
                                        )
                                        Text(
                                            text = relatedName,
                                            style = MaterialTheme.typography.labelLarge,
                                        )
                                    }
                                }
                            },
                        )
                    }

                    if (recordEntity.relatedTags.isNotEmpty()) {
                        // 标签
                        TranparentListItem(
                            headlineText = { Text(text = stringResource(id = R.string.tags)) },
                            trailingContent = {
                                val tagsText = with(StringBuilder()) {
                                    recordEntity.relatedTags.forEach { tag ->
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
                                    style = MaterialTheme.typography.labelLarge,
                                    modifier = Modifier
                                        .background(
                                            color = MaterialTheme.colorScheme.secondaryContainer,
                                            shape = MaterialTheme.shapes.small
                                        )
                                        .padding(horizontal = 4.dp),
                                )
                            },
                        )
                    }

                    if (recordEntity.remark.isNotBlank()) {
                        // 备注
                        TranparentListItem(
                            headlineText = { Text(text = stringResource(id = R.string.remark)) },
                            trailingContent = {
                                Text(
                                    text = recordEntity.remark,
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            },
                        )
                    }

                    // 时间
                    TranparentListItem(
                        headlineText = { Text(text = stringResource(id = R.string.time)) },
                        trailingContent = {
                            Text(
                                text = recordEntity.recordTime,
                                style = MaterialTheme.typography.labelLarge,
                            )
                        },
                    )
                }
            }
        }
    }
}

@DevicePreviews
@Composable
internal fun LauncherContentScreenPreview() {
    PreviewTheme(
        defaultEmptyImagePainter = painterResource(id = R.drawable.vector_no_data_200),
    ) {
        LauncherContentScreen(
            shouldDisplayDeleteFailedBookmark = 0,
            onBookmarkDismiss = { },
            onShowSnackbar = { _, _ -> SnackbarResult.Dismissed },
            viewRecord = null,
            onRecordItemEditClick = {},
            onRecordItemDeleteClick = {},
            bookName = "默认账本",
            onMenuClick = { },
            onSearchClick = { },
            onCalendarClick = { },
            onMyAssetClick = { },
            onAddClick = { },
            uiState = LauncherContentUiState.Success(
                monthIncome = "0",
                monthExpand = "0",
                monthBalance = "0",
                recordMap = mapOf(),
            ),
            onRecordItemClick = {},
            dialogState = DialogState.Dismiss,
            onDeleteRecordConfirm = {},
            onDialogDismiss = { },
        )
    }
}

@DevicePreviews
@Composable
internal fun LauncherContentLoadingScreenPreview() {
    PreviewTheme(
        defaultEmptyImagePainter = painterResource(id = R.drawable.vector_no_data_200),
    ) {
        LauncherContentScreen(
            shouldDisplayDeleteFailedBookmark = 0,
            onBookmarkDismiss = { },
            onShowSnackbar = { _, _ -> SnackbarResult.Dismissed },
            viewRecord = null,
            onRecordItemEditClick = {},
            onRecordItemDeleteClick = {},
            bookName = "默认账本",
            onMenuClick = { },
            onSearchClick = { },
            onCalendarClick = { },
            onMyAssetClick = { },
            onAddClick = { },
            uiState = LauncherContentUiState.Loading,
            onRecordItemClick = {},
            dialogState = DialogState.Dismiss,
            onDeleteRecordConfirm = {},
            onDialogDismiss = { },
        )
    }
}