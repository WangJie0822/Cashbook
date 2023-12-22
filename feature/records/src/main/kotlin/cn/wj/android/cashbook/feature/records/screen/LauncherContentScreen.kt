/*
 * Copyright 2021 The Cashbook Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
import androidx.compose.material3.BackdropValue
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberBackdropScaffoldState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.wj.android.cashbook.core.common.TestTag
import cn.wj.android.cashbook.core.common.ext.completeZero
import cn.wj.android.cashbook.core.common.ext.decimalFormat
import cn.wj.android.cashbook.core.common.ext.toDoubleOrZero
import cn.wj.android.cashbook.core.common.ext.withCNY
import cn.wj.android.cashbook.core.design.component.CashbookGradientBackground
import cn.wj.android.cashbook.core.design.component.CbDivider
import cn.wj.android.cashbook.core.design.component.CbFloatingActionButton
import cn.wj.android.cashbook.core.design.component.CbIconButton
import cn.wj.android.cashbook.core.design.component.CbListItem
import cn.wj.android.cashbook.core.design.component.CbModalBottomSheet
import cn.wj.android.cashbook.core.design.component.CbScaffold
import cn.wj.android.cashbook.core.design.component.CbTopAppBar
import cn.wj.android.cashbook.core.design.component.Empty
import cn.wj.android.cashbook.core.design.component.Footer
import cn.wj.android.cashbook.core.design.component.Loading
import cn.wj.android.cashbook.core.design.component.painterDrawableResource
import cn.wj.android.cashbook.core.design.icon.CbIcons
import cn.wj.android.cashbook.core.design.theme.LocalExtendedColors
import cn.wj.android.cashbook.core.model.entity.RecordDayEntity
import cn.wj.android.cashbook.core.model.entity.RecordViewsEntity
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.ui.DialogState
import cn.wj.android.cashbook.core.ui.R
import cn.wj.android.cashbook.core.ui.component.SelectDateDialog
import cn.wj.android.cashbook.core.ui.component.TypeIcon
import cn.wj.android.cashbook.core.ui.expand.typeColor
import cn.wj.android.cashbook.feature.records.viewmodel.LauncherContentUiState
import cn.wj.android.cashbook.feature.records.viewmodel.LauncherContentViewModel
import java.time.YearMonth

/**
 * 首页内容
 *
 * @param recordDetailSheetContent 记录详情 sheet，参数：(记录数据，隐藏sheet回调) -> [Unit]
 * @param onRequestOpenDrawer 打开抽屉菜单
 * @param onRequestNaviToEditRecord 导航到编辑记录
 * @param onRequestNaviToSearch 导航到搜索
 * @param onRequestNaviToCalendar 导航到日历
 * @param onRequestNaviToAnalytics 导航到数据分析
 * @param onShowSnackbar 显示 [androidx.compose.material3.Snackbar]，参数：(显示文本，action文本) -> [SnackbarResult]
 */
@Composable
internal fun LauncherContentRoute(
    recordDetailSheetContent: @Composable (RecordViewsEntity?, () -> Unit) -> Unit,
    onRequestOpenDrawer: () -> Unit,
    onRequestNaviToEditRecord: (Long) -> Unit,
    onRequestNaviToSearch: () -> Unit,
    onRequestNaviToCalendar: () -> Unit,
    onRequestNaviToAnalytics: () -> Unit,
    onShowSnackbar: suspend (String, String?) -> SnackbarResult,
    modifier: Modifier = Modifier,
    viewModel: LauncherContentViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val date by viewModel.dateData.collectAsStateWithLifecycle()

    LauncherContentScreen(
        shouldDisplayDeleteFailedBookmark = viewModel.shouldDisplayDeleteFailedBookmark,
        onRequestDismissBookmark = viewModel::dismissBookmark,
        recordDetailSheetContent = { record ->
            recordDetailSheetContent(record, viewModel::dismissSheet)
        },
        viewRecord = viewModel.viewRecord,
        date = date,
        onMenuClick = onRequestOpenDrawer,
        onDateClick = viewModel::displayDateSelectDialog,
        onDateSelected = viewModel::refreshSelectedDate,
        onSearchClick = onRequestNaviToSearch,
        onCalendarClick = onRequestNaviToCalendar,
        onAnalyticsClick = onRequestNaviToAnalytics,
        onAddClick = { onRequestNaviToEditRecord.invoke(-1L) },
        uiState = uiState,
        onRecordItemClick = viewModel::displayRecordDetailsSheet,
        dialogState = viewModel.dialogState,
        onRequestDismissDialog = viewModel::dismissDialog,
        onRequestDismissSheet = viewModel::dismissSheet,
        onShowSnackbar = onShowSnackbar,
        modifier = modifier,
    )
}

/**
 * 首页内容
 *
 * @param shouldDisplayDeleteFailedBookmark 删除失败提示
 * @param onRequestDismissBookmark 隐藏提示
 * @param viewRecord 需要显示的记录数据
 * @param recordDetailSheetContent 记录详情 sheet，参数：(记录数据) -> [Unit]
 * @param date 当前选择的年月信息
 * @param onMenuClick 菜单点击回调
 * @param onDateClick 日期点击回调
 * @param onDateSelected 日期选择回调
 * @param onSearchClick 搜索点击回调
 * @param onCalendarClick 日历点击回调
 * @param onAnalyticsClick 分析点击回调
 * @param onAddClick 添加点击回调
 * @param uiState 界面 UI 数据
 * @param onRecordItemClick 记录列表 item 点击回调
 * @param dialogState 弹窗状态
 * @param onRequestDismissDialog 隐藏弹窗
 * @param onRequestDismissSheet 隐藏 sheet
 * @param onShowSnackbar 显示 [androidx.compose.material3.Snackbar]，参数：(显示文本，action文本) -> [SnackbarResult]
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LauncherContentScreen(
    shouldDisplayDeleteFailedBookmark: Int,
    onRequestDismissBookmark: () -> Unit,
    viewRecord: RecordViewsEntity?,
    recordDetailSheetContent: @Composable (RecordViewsEntity?) -> Unit,
    date: YearMonth,
    onMenuClick: () -> Unit,
    onDateClick: () -> Unit,
    onDateSelected: (YearMonth) -> Unit,
    onSearchClick: () -> Unit,
    onCalendarClick: () -> Unit,
    onAnalyticsClick: () -> Unit,
    onAddClick: () -> Unit,
    uiState: LauncherContentUiState,
    onRecordItemClick: (RecordViewsEntity) -> Unit,
    dialogState: DialogState,
    onRequestDismissDialog: () -> Unit,
    onRequestDismissSheet: () -> Unit,
    onShowSnackbar: suspend (String, String?) -> SnackbarResult,
    modifier: Modifier = Modifier,
) {
    // 提示文本
    val deleteFailedFormatText = stringResource(id = R.string.delete_failed_format)
    // 显示提示
    LaunchedEffect(shouldDisplayDeleteFailedBookmark) {
        if (shouldDisplayDeleteFailedBookmark > 0) {
            val result = onShowSnackbar.invoke(
                deleteFailedFormatText.format(shouldDisplayDeleteFailedBookmark),
                null,
            )
            if (SnackbarResult.Dismissed == result) {
                onRequestDismissBookmark.invoke()
            }
        }
    }

    CbScaffold(
        modifier = modifier,
        topBar = {
            LauncherTopBar(
                dateStr = "${date.year}-${date.monthValue.completeZero()}",
                onMenuClick = onMenuClick,
                onDateClick = onDateClick,
                onSearchClick = onSearchClick,
                onCalendarClick = onCalendarClick,
                onAnalyticsClick = onAnalyticsClick,
            )
        },
        floatingActionButton = {
            CbFloatingActionButton(onClick = onAddClick) {
                Icon(imageVector = CbIcons.Add, contentDescription = null)
            }
        },
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            if (null != viewRecord) {
                // 显示记录详情底部抽屉
                CbModalBottomSheet(
                    onDismissRequest = onRequestDismissSheet,
                    sheetState = rememberModalBottomSheetState(
                        confirmValueChange = {
                            if (it == SheetValue.Hidden) {
                                onRequestDismissSheet()
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
                        scaffoldState = rememberBackdropScaffoldState(initialValue = BackdropValue.Revealed),
                        appBar = { /* 使用上层 topBar 处理 */ },
                        peekHeight = paddingValues.calculateTopPadding(),
                        backLayerBackgroundColor = Color.Transparent,
                        backLayerContent = {
                            // 背景布局
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
                                onDateClick = onDateClick,
                                onDateSelected = onDateSelected,
                                onRequestDismissDialog = onRequestDismissDialog,
                                recordMap = uiState.recordMap,
                                onRecordItemClick = onRecordItemClick,
                            )
                        },
                    )
                }
            }
        }
    }
}

/**
 * 标题栏
 *
 * @param dateStr 日期文本
 * @param onMenuClick 菜单点击回调
 * @param onDateClick 日期点击回调
 * @param onSearchClick 搜索点击回调
 * @param onCalendarClick 日历点击回调
 * @param onAnalyticsClick 分析点击回调
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LauncherTopBar(
    dateStr: String,
    onMenuClick: () -> Unit,
    onDateClick: () -> Unit,
    onSearchClick: () -> Unit,
    onCalendarClick: () -> Unit,
    onAnalyticsClick: () -> Unit,
) {
    CbTopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
        ),
        title = {
            Row(
                modifier = Modifier
                    .clickable(onClick = onDateClick)
                    .testTag(TestTag.Launcher.LAUNCHER_TITLE),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = dateStr,
                    modifier = Modifier.clickable(onClick = onDateClick),
                )
                Icon(
                    imageVector = CbIcons.ArrowDropDown,
                    contentDescription = null,
                )
            }
        },
        navigationIcon = {
            CbIconButton(onClick = onMenuClick) {
                Icon(
                    imageVector = CbIcons.Menu,
                    contentDescription = null,
                )
            }
        },
        actions = {
            CbIconButton(onClick = onSearchClick) {
                Icon(
                    imageVector = CbIcons.Search,
                    contentDescription = stringResource(id = R.string.cd_search),
                )
            }
            CbIconButton(onClick = onCalendarClick) {
                Icon(
                    imageVector = CbIcons.CalendarMonth,
                    contentDescription = stringResource(id = R.string.cd_calendar),
                )
            }
            CbIconButton(onClick = onAnalyticsClick) {
                Icon(
                    imageVector = CbIcons.Analytics,
                    contentDescription = stringResource(id = R.string.cd_analytics),
                )
            }
        },
    )
}

/**
 * 背景布局
 *
 * @param paddingValues 背景 padding 数据
 * @param monthIncome 月收入
 * @param monthExpand 月支出
 * @param monthBalance 月结余
 */
@Composable
private fun BackLayerContent(
    paddingValues: PaddingValues,
    monthIncome: String,
    monthExpand: String,
    monthBalance: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(230.dp)
            .paint(
                painter = painterResource(id = R.drawable.im_top_background),
                contentScale = ContentScale.Crop,
            ),
    ) {
        Column(
            modifier = modifier
                .padding(paddingValues)
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            val textModifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                    shape = MaterialTheme.shapes.small,
                )
                .padding(horizontal = 8.dp)
            CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onPrimaryContainer) {
                Text(
                    modifier = textModifier,
                    text = stringResource(id = R.string.month_income),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    modifier = textModifier,
                    text = monthIncome.withCNY(),
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row {
                    Box(modifier = Modifier.weight(1f)) {
                        Text(
                            modifier = textModifier,
                            text = "${stringResource(id = R.string.month_expend)} ${monthExpand.withCNY()}",
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        Text(
                            modifier = textModifier,
                            text = "${stringResource(id = R.string.month_balance)} ${monthBalance.withCNY()}",
                        )
                    }
                }
            }
        }
    }
}

/**
 * 内容布局
 *
 * @param dialogState 弹窗状态
 * @param onDateClick 日期点击回调
 * @param onDateSelected 日期选中回调
 * @param onRequestDismissDialog 隐藏弹窗
 * @param recordMap 记录列表数据
 * @param onRecordItemClick 记录列表 item 点击回调
 */
@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun FrontLayerContent(
    dialogState: DialogState,
    onDateClick: () -> Unit,
    onDateSelected: (YearMonth) -> Unit,
    onRequestDismissDialog: () -> Unit,
    recordMap: Map<RecordDayEntity, List<RecordViewsEntity>>,
    onRecordItemClick: (RecordViewsEntity) -> Unit,
) {
    CashbookGradientBackground {
        Box(
            modifier = Modifier.fillMaxSize(),
        ) {
            (dialogState as? DialogState.Shown<*>)?.data?.let { date ->
                if (date is YearMonth) {
                    // 显示选择日期弹窗
                    SelectDateDialog(
                        onDialogDismiss = onRequestDismissDialog,
                        currentDate = date,
                        onDateSelected = { ym, _ -> onDateSelected(ym) },
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
                        val recordList = recordMap[key] ?: emptyList()
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
                                    },
                                )
                                Text(
                                    text = buildString {
                                        val totalIncome = key.dayIncome.toDoubleOrZero()
                                        val totalExpenditure = key.dayExpand.toDoubleOrZero()
                                        val hasIncome = totalIncome != 0.0
                                        if (hasIncome) {
                                            append(
                                                stringResource(id = R.string.income_with_colon) + totalIncome.decimalFormat()
                                                    .withCNY(),
                                            )
                                        }
                                        if (totalExpenditure != 0.0) {
                                            if (hasIncome) {
                                                append(", ")
                                            }
                                            append(
                                                stringResource(id = R.string.expend_with_colon) + totalExpenditure.decimalFormat()
                                                    .withCNY(),
                                            )
                                        }
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            CbDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = DividerDefaults.color.copy(0.3f),
                            )
                        }
                        items(recordList, key = { it.id }) {
                            RecordListItem(
                                item = it,
                                modifier = Modifier.clickable {
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

/**
 * 记录列表 item 布局
 *
 * @param item 记录数据
 */
@Composable
internal fun RecordListItem(
    item: RecordViewsEntity,
    modifier: Modifier = Modifier,
    showTags: Boolean = true,
    showRemarks: Boolean = true,
) {
    CbListItem(
        modifier = modifier,
        leadingContent = {
            TypeIcon(
                painter = painterDrawableResource(idStr = item.typeIconResName),
                containerColor = item.typeCategory.typeColor,
            )
        },
        headlineContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = item.typeName)
                val tags = item.relatedTags
                if (showTags && tags.isNotEmpty()) {
                    val tagsText = with(StringBuilder()) {
                        tags.forEach { tag ->
                            if (isNotBlank()) {
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
                                shape = MaterialTheme.shapes.small,
                            )
                            .padding(horizontal = 4.dp),
                    )
                }
            }
        },
        supportingContent = {
            Text(
                text = buildAnnotatedString {
                    append(item.recordTime.split(" ").first())
                    if (showRemarks) {
                        withStyle(SpanStyle(color = LocalContentColor.current.copy(alpha = 0.7f))) {
                            append("  ${item.remark}")
                        }
                    }
                },
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
            )
        },
        trailingContent = {
            Column(
                horizontalAlignment = Alignment.End,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (item.relatedRecord.isNotEmpty()) {
                        Text(
                            text = stringResource(
                                id = if (item.typeCategory == RecordTypeCategoryEnum.EXPENDITURE) {
                                    R.string.refund_reimbursed_simple
                                } else {
                                    R.string.related
                                },
                            ) + "(${item.relatedAmount.withCNY()})",
                            color = LocalContentColor.current.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(end = 8.dp),
                        )
                    }
                    Text(
                        text = item.amount.withCNY(),
                        color = item.typeCategory.typeColor,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
                item.assetName?.let { assetName ->
                    Text(
                        text = buildAnnotatedString {
                            val hasCharges = item.charges.toDoubleOrZero() > 0.0
                            val hasConcessions = item.concessions.toDoubleOrZero() > 0.0
                            if (hasCharges || hasConcessions) {
                                // 有手续费、优惠信息
                                withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.onSurface)) {
                                    append("(")
                                }
                                if (hasCharges) {
                                    withStyle(style = SpanStyle(color = LocalExtendedColors.current.expenditure)) {
                                        append("-${item.charges}".withCNY())
                                    }
                                }
                                if (hasConcessions) {
                                    if (hasCharges) {
                                        append(" ")
                                    }
                                    withStyle(style = SpanStyle(color = LocalExtendedColors.current.income)) {
                                        append("+${item.concessions.withCNY()}")
                                    }
                                }
                                withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.onSurface)) {
                                    append(") ")
                                }
                            }
                            append(assetName)
                            if (item.typeCategory == RecordTypeCategoryEnum.TRANSFER) {
                                append(" -> ${item.relatedAssetName}")
                            }
                        },
                    )
                }
            }
        },
    )
}
