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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.BackdropScaffold
import androidx.compose.material3.BackdropScaffoldState
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
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.rememberBackdropScaffoldState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import cn.wj.android.cashbook.core.common.TestTag
import cn.wj.android.cashbook.core.common.ext.toMoneyCNY
import cn.wj.android.cashbook.core.common.ext.withCNY
import cn.wj.android.cashbook.core.common.tools.toDateTimeString
import cn.wj.android.cashbook.core.common.tools.toTimeString
import cn.wj.android.cashbook.core.design.component.CashbookGradientBackground
import cn.wj.android.cashbook.core.design.component.CbFloatingActionButton
import cn.wj.android.cashbook.core.design.component.CbHorizontalDivider
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
import cn.wj.android.cashbook.core.design.preview.PreviewTheme
import cn.wj.android.cashbook.core.design.theme.LocalExtendedColors
import cn.wj.android.cashbook.core.model.entity.DateSelectionEntity
import cn.wj.android.cashbook.core.model.entity.RecordDayEntity
import cn.wj.android.cashbook.core.model.entity.RecordViewsEntity
import cn.wj.android.cashbook.core.model.enums.DateSelectionTypeEnum
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.ui.DevicePreviews
import cn.wj.android.cashbook.core.ui.R
import cn.wj.android.cashbook.core.ui.component.DateSelectionPopup
import cn.wj.android.cashbook.core.ui.component.TypeIcon
import cn.wj.android.cashbook.core.ui.expand.bookImageRatio
import cn.wj.android.cashbook.core.ui.expand.typeColor
import cn.wj.android.cashbook.feature.records.viewmodel.LauncherContentUiState
import cn.wj.android.cashbook.feature.records.viewmodel.LauncherContentViewModel
import cn.wj.android.cashbook.feature.records.viewmodel.LauncherListItem
import coil.compose.AsyncImage
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
    val dateSelection by viewModel.dateSelection.collectAsStateWithLifecycle()
    val dailySummaries by viewModel.dailySummaries.collectAsStateWithLifecycle()
    val pagingItems = viewModel.recordPagingData.collectAsLazyPagingItems()

    LauncherContentScreen(
        shouldDisplayDeleteFailedBookmark = viewModel.shouldDisplayDeleteFailedBookmark,
        onRequestDismissBookmark = viewModel::dismissBookmark,
        recordDetailSheetContent = { record ->
            recordDetailSheetContent(record, viewModel::dismissSheet)
        },
        viewRecord = viewModel.viewRecord,
        dateSelection = dateSelection,
        showDatePopup = viewModel.showDatePopup,
        onMenuClick = onRequestOpenDrawer,
        onDateClick = viewModel::displayDatePopup,
        onDateSelected = viewModel::updateDateSelection,
        onDismissDatePopup = viewModel::dismissDatePopup,
        onSearchClick = onRequestNaviToSearch,
        onCalendarClick = onRequestNaviToCalendar,
        onAnalyticsClick = onRequestNaviToAnalytics,
        onAddClick = { onRequestNaviToEditRecord.invoke(-1L) },
        uiState = uiState,
        pagingItems = pagingItems,
        dailySummaries = dailySummaries,
        onRecordItemClick = viewModel::displayRecordDetailsSheet,
        onRequestDismissSheet = viewModel::dismissSheet,
        onShowSnackbar = onShowSnackbar,
        modifier = modifier,
    )
}

/**
 * 首页内容
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LauncherContentScreen(
    modifier: Modifier = Modifier,
    shouldDisplayDeleteFailedBookmark: Int,
    onRequestDismissBookmark: () -> Unit,
    viewRecord: RecordViewsEntity?,
    recordDetailSheetContent: @Composable (RecordViewsEntity?) -> Unit,
    dateSelection: DateSelectionEntity,
    showDatePopup: Boolean,
    onMenuClick: () -> Unit,
    onDateClick: () -> Unit,
    onDateSelected: (DateSelectionEntity) -> Unit,
    onDismissDatePopup: () -> Unit,
    onSearchClick: () -> Unit,
    onCalendarClick: () -> Unit,
    onAnalyticsClick: () -> Unit,
    onAddClick: () -> Unit,
    uiState: LauncherContentUiState,
    pagingItems: LazyPagingItems<LauncherListItem>? = null,
    dailySummaries: Map<String, RecordDayEntity> = emptyMap(),
    onRecordItemClick: (RecordViewsEntity) -> Unit,
    onRequestDismissSheet: () -> Unit,
    onShowSnackbar: suspend (String, String?) -> SnackbarResult,
    scaffoldState: BackdropScaffoldState = rememberBackdropScaffoldState(initialValue = BackdropValue.Revealed),
    windowAdaptiveInfo: WindowAdaptiveInfo = currentWindowAdaptiveInfo(),
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
                scaffoldState = scaffoldState,
                dateStr = dateSelection.getDisplayText(),
                showDatePopup = showDatePopup,
                dateSelection = dateSelection,
                onMenuClick = onMenuClick,
                onDateClick = onDateClick,
                onDateSelected = onDateSelected,
                onDismissDatePopup = onDismissDatePopup,
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
                    Box(modifier = Modifier.fillMaxSize()) {
                        AsyncImage(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(windowAdaptiveInfo.bookImageRatio),
                            model = uiState.topBgUri.toUri(),
                            placeholder = painterResource(id = R.drawable.im_top_background),
                            error = painterResource(id = R.drawable.im_top_background),
                            fallback = painterResource(id = R.drawable.im_top_background),
                            contentScale = ContentScale.FillBounds,
                            contentDescription = null,
                        )
                        BackdropScaffold(
                            scaffoldState = scaffoldState,
                            appBar = { /* 使用上层 topBar 处理 */ },
                            peekHeight = paddingValues.calculateTopPadding(),
                            backLayerBackgroundColor = Color.Transparent,
                            backLayerContent = {
                                // 背景布局
                                BackLayerContent(
                                    paddingValues = paddingValues,
                                    dateSelectionType = dateSelection.type,
                                    totalIncome = uiState.totalIncome,
                                    totalExpand = uiState.totalExpand,
                                    totalBalance = uiState.totalBalance,
                                )
                            },
                            frontLayerScrimColor = Color.Unspecified,
                            frontLayerContent = {
                                FrontLayerContent(
                                    dateSelectionType = dateSelection.type,
                                    onDateClick = onDateClick,
                                    pagingItems = pagingItems,
                                    dailySummaries = dailySummaries,
                                    onRecordItemClick = onRecordItemClick,
                                )
                            },
                        )
                    }
                }
            }
        }
    }
}

/**
 * 标题栏
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LauncherTopBar(
    scaffoldState: BackdropScaffoldState,
    dateStr: String,
    showDatePopup: Boolean,
    dateSelection: DateSelectionEntity,
    onMenuClick: () -> Unit,
    onDateClick: () -> Unit,
    onDateSelected: (DateSelectionEntity) -> Unit,
    onDismissDatePopup: () -> Unit,
    onSearchClick: () -> Unit,
    onCalendarClick: () -> Unit,
    onAnalyticsClick: () -> Unit,
) {
    CbTopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = if (scaffoldState.isRevealed) {
                Color.Transparent
            } else {
                MaterialTheme.colorScheme.primaryContainer.copy(
                    alpha = 0.7f,
                )
            },
        ),
        title = {
            Box {
                Row(
                    modifier = Modifier
                        .clickable(onClick = onDateClick)
                        .testTag(TestTag.Launcher.LAUNCHER_TITLE),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (dateSelection is DateSelectionEntity.DateRange) {
                        Column {
                            Text(
                                text = dateSelection.from.let { "${it.year}-${it.monthValue.toString().padStart(2, '0')}-${it.dayOfMonth.toString().padStart(2, '0')}" },
                                style = MaterialTheme.typography.labelMedium,
                            )
                            Text(
                                text = dateSelection.to.let { "${it.year}-${it.monthValue.toString().padStart(2, '0')}-${it.dayOfMonth.toString().padStart(2, '0')}" },
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    } else {
                        Text(text = dateStr)
                    }
                    Icon(
                        imageVector = CbIcons.ArrowDropDown,
                        contentDescription = null,
                    )
                }
                DateSelectionPopup(
                    expanded = showDatePopup,
                    onDismissRequest = onDismissDatePopup,
                    currentSelection = dateSelection,
                    onDateSelected = onDateSelected,
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
 */
@Composable
private fun BackLayerContent(
    paddingValues: PaddingValues,
    dateSelectionType: DateSelectionTypeEnum,
    totalIncome: String,
    totalExpand: String,
    totalBalance: String,
    modifier: Modifier = Modifier,
) {
    val incomeLabel = when (dateSelectionType) {
        DateSelectionTypeEnum.BY_DAY -> R.string.day_income
        DateSelectionTypeEnum.BY_MONTH -> R.string.month_income
        DateSelectionTypeEnum.BY_YEAR -> R.string.year_income
        DateSelectionTypeEnum.DATE_RANGE, DateSelectionTypeEnum.ALL -> R.string.total_income
    }
    val expendLabel = when (dateSelectionType) {
        DateSelectionTypeEnum.BY_DAY -> R.string.day_expend
        DateSelectionTypeEnum.BY_MONTH -> R.string.month_expend
        DateSelectionTypeEnum.BY_YEAR -> R.string.year_expend
        DateSelectionTypeEnum.DATE_RANGE, DateSelectionTypeEnum.ALL -> R.string.total_expenditure
    }
    val balanceLabel = when (dateSelectionType) {
        DateSelectionTypeEnum.BY_DAY -> R.string.day_balance
        DateSelectionTypeEnum.BY_MONTH -> R.string.month_balance
        DateSelectionTypeEnum.BY_YEAR -> R.string.year_balance
        DateSelectionTypeEnum.DATE_RANGE, DateSelectionTypeEnum.ALL -> R.string.total_balance
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
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
                    text = stringResource(id = incomeLabel),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    modifier = textModifier,
                    text = totalIncome.withCNY(),
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row {
                    Box(modifier = Modifier.weight(1f)) {
                        Text(
                            modifier = textModifier,
                            text = "${stringResource(id = expendLabel)} ${totalExpand.withCNY()}",
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        Text(
                            modifier = textModifier,
                            text = "${stringResource(id = balanceLabel)} ${totalBalance.withCNY()}",
                        )
                    }
                }
            }
        }
    }
}

/**
 * 内容布局
 */
@Composable
private fun FrontLayerContent(
    dateSelectionType: DateSelectionTypeEnum,
    onDateClick: () -> Unit,
    pagingItems: LazyPagingItems<LauncherListItem>?,
    dailySummaries: Map<String, RecordDayEntity>,
    onRecordItemClick: (RecordViewsEntity) -> Unit,
) {
    CashbookGradientBackground {
        Box(
            modifier = Modifier.fillMaxSize(),
        ) {
            if (pagingItems == null || pagingItems.itemCount == 0) {
                val hintText = if (dateSelectionType == DateSelectionTypeEnum.BY_MONTH) {
                    stringResource(id = R.string.launcher_no_data_hint)
                } else {
                    stringResource(id = R.string.launcher_no_data_hint_general)
                }
                val buttonText = if (dateSelectionType == DateSelectionTypeEnum.BY_MONTH) {
                    stringResource(id = R.string.launcher_no_data_button)
                } else {
                    stringResource(id = R.string.launcher_no_data_button_general)
                }
                Empty(
                    hintText = hintText,
                    button = {
                        FilledTonalButton(onClick = onDateClick) {
                            Text(text = buttonText)
                        }
                    },
                    modifier = Modifier.align(Alignment.TopCenter),
                )
            } else {
                val items = pagingItems!!
                LazyColumn {
                    items(
                        count = items.itemCount,
                        key = { index ->
                            when (val item = items.peek(index)) {
                                is LauncherListItem.DayHeader -> "header_${item.dateStr}"
                                is LauncherListItem.Record -> "record_${item.entity.id}"
                                null -> "placeholder_$index"
                            }
                        },
                    ) { index ->
                        when (val item = items[index]) {
                            is LauncherListItem.DayHeader -> {
                                val summary = dailySummaries[item.dateStr]
                                DayHeaderItem(
                                    dateStr = item.dateStr,
                                    day = item.day,
                                    dayType = item.dayType,
                                    dateSelectionType = dateSelectionType,
                                    dayIncome = summary?.dayIncome ?: 0L,
                                    dayExpand = summary?.dayExpand ?: 0L,
                                )
                            }

                            is LauncherListItem.Record -> {
                                RecordListItem(
                                    item = item.entity,
                                    showDate = false,
                                    modifier = Modifier.clickable {
                                        onRecordItemClick(item.entity)
                                    },
                                )
                            }

                            null -> {
                                // 占位
                            }
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
 * 日期头布局
 */
@Composable
private fun DayHeaderItem(
    dateStr: String,
    day: Int,
    dayType: Int,
    dateSelectionType: DateSelectionTypeEnum,
    dayIncome: Long,
    dayExpand: Long,
) {
    val dateArray = dateStr.split("-")
    val month = dateArray.getOrNull(1)?.toIntOrNull() ?: 0
    val year = dateArray.getOrNull(0)?.toIntOrNull() ?: 0
    val dayTypeSuffix = when (dayType) {
        0 -> stringResource(id = R.string.today_with_brackets)
        -1 -> stringResource(id = R.string.yesterday_with_brackets)
        -2 -> stringResource(id = R.string.before_yesterday_with_brackets)
        else -> ""
    }
    val dateText = when (dateSelectionType) {
        DateSelectionTypeEnum.BY_DAY, DateSelectionTypeEnum.BY_MONTH ->
            "${day}${stringResource(id = R.string.day)}$dayTypeSuffix"
        DateSelectionTypeEnum.BY_YEAR ->
            "${month}${stringResource(id = R.string.month)}${day}${stringResource(id = R.string.day)}$dayTypeSuffix"
        DateSelectionTypeEnum.DATE_RANGE, DateSelectionTypeEnum.ALL ->
            "${year}${stringResource(id = R.string.year)}${month}${stringResource(id = R.string.month)}${day}${stringResource(id = R.string.day)}"
    }
    Column {
        Row(
            modifier = Modifier
                .background(color = MaterialTheme.colorScheme.surface)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = dateText,
            )
            Text(
                text = buildString {
                    val hasIncome = dayIncome != 0L
                    if (hasIncome) {
                        append(
                            stringResource(id = R.string.income_with_colon) + dayIncome.toMoneyCNY(),
                        )
                    }
                    if (dayExpand != 0L) {
                        if (hasIncome) {
                            append(", ")
                        }
                        append(
                            stringResource(id = R.string.expend_with_colon) + dayExpand.toMoneyCNY(),
                        )
                    }
                },
                style = MaterialTheme.typography.bodySmall,
            )
        }
        CbHorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            color = DividerDefaults.color.copy(0.3f),
        )
    }
}

/**
 * 记录列表 item 布局
 */
@Composable
internal fun RecordListItem(
    item: RecordViewsEntity,
    modifier: Modifier = Modifier,
    showTags: Boolean = true,
    showRemarks: Boolean = true,
    showDate: Boolean = true,
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
                            result = result.substring(0, 12) + "..."
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
                    if (showDate) {
                        append(item.recordTime.toDateTimeString())
                    } else {
                        append(item.recordTime.toTimeString())
                    }
                    if (showRemarks) {
                        withStyle(SpanStyle(color = LocalContentColor.current.copy(alpha = 0.7f))) {
                            append("  ${item.remark}")
                        }
                    }
                },
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
                style = MaterialTheme.typography.bodySmall,
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
                            ) + "(${item.relatedAmount.toMoneyCNY()})",
                            color = LocalContentColor.current.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(end = 8.dp),
                        )
                    }
                    val displayAmount = when {
                        item.typeCategory == RecordTypeCategoryEnum.TRANSFER -> {
                            item.amount
                        }
                        item.typeCategory == RecordTypeCategoryEnum.EXPENDITURE &&
                            item.relatedRecord.isNotEmpty() -> {
                            item.amount
                        }
                        else -> item.finalAmount
                    }
                    val isReimbursed = item.typeCategory == RecordTypeCategoryEnum.EXPENDITURE &&
                        item.relatedRecord.isNotEmpty()
                    Text(
                        text = displayAmount.toMoneyCNY(),
                        color = item.typeCategory.typeColor,
                        style = MaterialTheme.typography.labelLarge.copy(
                            textDecoration = if (isReimbursed) {
                                TextDecoration.LineThrough
                            } else {
                                TextDecoration.None
                            },
                        ),
                    )
                }
                item.assetName?.let { assetName ->
                    Text(
                        text = buildAnnotatedString {
                            val hasCharges = item.charges > 0L
                            val hasConcessions = item.concessions > 0L
                            if (hasCharges || hasConcessions) {
                                withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.onSurface)) {
                                    append("(")
                                }
                                if (hasCharges) {
                                    withStyle(style = SpanStyle(color = LocalExtendedColors.current.expenditure)) {
                                        append("-${item.charges.toMoneyCNY()}")
                                    }
                                }
                                if (hasConcessions) {
                                    if (hasCharges) {
                                        append(" ")
                                    }
                                    withStyle(style = SpanStyle(color = LocalExtendedColors.current.income)) {
                                        append("+${item.concessions.toMoneyCNY()}")
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

@Composable
@DevicePreviews
private fun LauncherContentScreenPreviewRevealed() {
    PreviewTheme {
        LauncherContentScreen(
            shouldDisplayDeleteFailedBookmark = 0,
            onRequestDismissBookmark = { },
            recordDetailSheetContent = { },
            viewRecord = null,
            dateSelection = DateSelectionEntity.ByMonth(YearMonth.now()),
            showDatePopup = false,
            onMenuClick = { },
            onDateClick = { },
            onDateSelected = { },
            onDismissDatePopup = { },
            onSearchClick = { },
            onCalendarClick = { },
            onAnalyticsClick = { },
            onAddClick = { },
            uiState = LauncherContentUiState.Success(
                "",
                "300",
                "200",
                "100",
            ),
            onRecordItemClick = { },
            onRequestDismissSheet = { },
            onShowSnackbar = { _, _ -> SnackbarResult.Dismissed },
            scaffoldState = rememberBackdropScaffoldState(initialValue = BackdropValue.Revealed),
        )
    }
}

@Composable
@DevicePreviews
private fun LauncherContentScreenPreviewConcealed() {
    PreviewTheme {
        LauncherContentScreen(
            shouldDisplayDeleteFailedBookmark = 0,
            onRequestDismissBookmark = { },
            recordDetailSheetContent = { },
            viewRecord = null,
            dateSelection = DateSelectionEntity.ByMonth(YearMonth.now()),
            showDatePopup = false,
            onMenuClick = { },
            onDateClick = { },
            onDateSelected = { },
            onDismissDatePopup = { },
            onSearchClick = { },
            onCalendarClick = { },
            onAnalyticsClick = { },
            onAddClick = { },
            uiState = LauncherContentUiState.Success(
                "",
                "300",
                "200",
                "100",
            ),
            onRecordItemClick = { },
            onRequestDismissSheet = { },
            onShowSnackbar = { _, _ -> SnackbarResult.Dismissed },
            scaffoldState = rememberBackdropScaffoldState(initialValue = BackdropValue.Concealed),
        )
    }
}
