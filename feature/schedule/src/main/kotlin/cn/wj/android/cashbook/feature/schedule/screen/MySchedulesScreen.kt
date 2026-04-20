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

package cn.wj.android.cashbook.feature.schedule.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.wj.android.cashbook.core.common.ext.toMoneyCNY
import cn.wj.android.cashbook.core.common.tools.toDateString
import cn.wj.android.cashbook.core.common.tools.toTimeString
import cn.wj.android.cashbook.core.design.component.CbAlertDialog
import cn.wj.android.cashbook.core.design.component.CbFloatingActionButton
import cn.wj.android.cashbook.core.design.component.CbHorizontalDivider
import cn.wj.android.cashbook.core.design.component.CbListItem
import cn.wj.android.cashbook.core.design.component.CbModalBottomSheet
import cn.wj.android.cashbook.core.design.component.CbScaffold
import cn.wj.android.cashbook.core.design.component.CbTextButton
import cn.wj.android.cashbook.core.design.component.CbTopAppBar
import cn.wj.android.cashbook.core.design.component.Empty
import cn.wj.android.cashbook.core.design.component.painterDrawableResource
import cn.wj.android.cashbook.core.design.icon.CbIcons
import cn.wj.android.cashbook.core.design.theme.LocalExtendedColors
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.model.enums.ScheduleFrequencyEnum
import cn.wj.android.cashbook.core.model.model.ScheduleModel
import cn.wj.android.cashbook.core.ui.DialogState
import cn.wj.android.cashbook.core.ui.R
import cn.wj.android.cashbook.core.ui.component.TypeIcon
import cn.wj.android.cashbook.core.ui.expand.typeColor
import cn.wj.android.cashbook.feature.schedule.model.ScheduleViewsEntity
import cn.wj.android.cashbook.feature.schedule.viewmodel.MySchedulesViewModel

/**
 * 周期记账列表
 */
@Composable
internal fun MySchedulesRoute(
    onRequestNaviToEditSchedule: (Long) -> Unit,
    onRequestPopBackStack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MySchedulesViewModel = hiltViewModel(),
) {
    val scheduleList by viewModel.scheduleListData.collectAsStateWithLifecycle()

    MySchedulesScreen(
        scheduleList = scheduleList,
        viewSchedule = viewModel.viewSchedule,
        dialogState = viewModel.dialogState,
        onToggleEnabled = viewModel::toggleEnabled,
        onShowDetails = viewModel::showScheduleDetails,
        onDismissDetails = viewModel::dismissScheduleDetails,
        onEditClick = onRequestNaviToEditSchedule,
        onDeleteClick = viewModel::showDeleteDialog,
        onConfirmDelete = viewModel::confirmDelete,
        onDismissDialog = viewModel::dismissDialog,
        onAddClick = { onRequestNaviToEditSchedule(-1L) },
        onBackClick = onRequestPopBackStack,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MySchedulesScreen(
    scheduleList: List<ScheduleViewsEntity>,
    viewSchedule: ScheduleViewsEntity?,
    dialogState: DialogState,
    onToggleEnabled: (ScheduleModel) -> Unit,
    onShowDetails: (ScheduleViewsEntity) -> Unit,
    onDismissDetails: () -> Unit,
    onEditClick: (Long) -> Unit,
    onDeleteClick: (ScheduleModel) -> Unit,
    onConfirmDelete: (Long, Boolean) -> Unit,
    onDismissDialog: () -> Unit,
    onAddClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CbScaffold(
        modifier = modifier,
        topBar = {
            CbTopAppBar(
                onBackClick = onBackClick,
                title = { Text(text = stringResource(id = R.string.my_schedules)) },
            )
        },
        floatingActionButton = {
            CbFloatingActionButton(onClick = onAddClick) {
                Icon(imageVector = CbIcons.Add, contentDescription = stringResource(id = R.string.cd_add))
            }
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            // 删除确认弹窗
            if (dialogState is DialogState.Shown<*>) {
                (dialogState.data as? ScheduleModel)?.let { schedule ->
                    var deleteRecords by remember(dialogState) { mutableStateOf(false) }
                    CbAlertDialog(
                        onDismissRequest = onDismissDialog,
                        title = { Text(text = stringResource(id = R.string.delete_schedule)) },
                        text = {
                            Column {
                                Text(text = stringResource(id = R.string.delete_schedule_confirm))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { deleteRecords = !deleteRecords }
                                        .padding(top = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Checkbox(
                                        checked = deleteRecords,
                                        onCheckedChange = { deleteRecords = it },
                                    )
                                    Text(
                                        text = stringResource(id = R.string.delete_schedule_related_records),
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                }
                            }
                        },
                        dismissButton = {
                            CbTextButton(onClick = onDismissDialog) {
                                Text(text = stringResource(id = R.string.cancel))
                            }
                        },
                        confirmButton = {
                            CbTextButton(onClick = { onConfirmDelete(schedule.id, deleteRecords) }) {
                                Text(text = stringResource(id = R.string.confirm))
                            }
                        },
                    )
                }
            }

            // 详情 BottomSheet
            if (viewSchedule != null) {
                CbModalBottomSheet(
                    onDismissRequest = onDismissDetails,
                    sheetState = rememberModalBottomSheetState(
                        confirmValueChange = {
                            if (it == SheetValue.Hidden) {
                                onDismissDetails()
                            }
                            true
                        },
                    ),
                ) {
                    ScheduleDetailsSheet(
                        scheduleViews = viewSchedule,
                        onEditClick = {
                            onEditClick(viewSchedule.schedule.id)
                            onDismissDetails()
                        },
                        onDeleteClick = {
                            onDeleteClick(viewSchedule.schedule)
                        },
                    )
                }
            }

            if (scheduleList.isEmpty()) {
                Empty(
                    hintText = stringResource(id = R.string.schedule_empty_hint),
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(scheduleList, key = { it.schedule.id }) { item ->
                        ScheduleListItem(
                            item = item,
                            onToggleEnabled = { onToggleEnabled(item.schedule) },
                            onItemClick = { onShowDetails(item) },
                        )
                        CbHorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    }
                }
            }
        }
    }
}

/**
 * 周期记账列表项
 */
@Composable
private fun ScheduleListItem(
    item: ScheduleViewsEntity,
    onToggleEnabled: () -> Unit,
    onItemClick: () -> Unit,
) {
    val schedule = item.schedule
    CbListItem(
        modifier = Modifier.clickable(onClick = onItemClick),
        leadingContent = {
            TypeIcon(
                painter = painterDrawableResource(idStr = item.typeIconResName),
                containerColor = schedule.typeCategory.typeColor,
            )
        },
        headlineContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = item.typeName)
                val tags = item.tagNameList
                if (tags.isNotEmpty()) {
                    val tagsText = with(StringBuilder()) {
                        tags.forEach { tag ->
                            if (isNotBlank()) {
                                append(",")
                            }
                            append(tag)
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
                    append(schedule.frequency.displayName())
                    append(" ")
                    append(schedule.startDate.toDateString())
                    if (schedule.remark.isNotBlank()) {
                        withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))) {
                            append("  ${schedule.remark}")
                        }
                    }
                },
                maxLines = 1,
                style = MaterialTheme.typography.bodySmall,
            )
        },
        trailingContent = {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = schedule.amount.toMoneyCNY(),
                    color = schedule.typeCategory.typeColor,
                    style = MaterialTheme.typography.labelLarge,
                )
                item.assetName?.let { assetName ->
                    Text(
                        text = buildAnnotatedString {
                            val hasCharges = schedule.charges > 0L
                            val hasConcessions = schedule.concessions > 0L
                            if (hasCharges || hasConcessions) {
                                withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.onSurface)) {
                                    append("(")
                                }
                                if (hasCharges) {
                                    withStyle(style = SpanStyle(color = LocalExtendedColors.current.expenditure)) {
                                        append("-${schedule.charges.toMoneyCNY()}")
                                    }
                                }
                                if (hasConcessions) {
                                    if (hasCharges) {
                                        append(" ")
                                    }
                                    withStyle(style = SpanStyle(color = LocalExtendedColors.current.income)) {
                                        append("+${schedule.concessions.toMoneyCNY()}")
                                    }
                                }
                                withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.onSurface)) {
                                    append(") ")
                                }
                            }
                            append(assetName)
                        },
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
    )
}

/**
 * 周期规则详情 Sheet
 */
@Composable
private fun ScheduleDetailsSheet(
    scheduleViews: ScheduleViewsEntity,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val schedule = scheduleViews.schedule

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(id = R.string.schedule_details),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            CbTextButton(onClick = onEditClick) {
                Text(
                    text = stringResource(id = R.string.edit),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            CbTextButton(onClick = onDeleteClick) {
                Text(
                    text = stringResource(id = R.string.delete),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
        CbHorizontalDivider()

        // 金额
        CbListItem(
            headlineContent = { Text(text = stringResource(id = R.string.amount)) },
            trailingContent = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (schedule.typeCategory == RecordTypeCategoryEnum.EXPENDITURE && schedule.reimbursable) {
                        Text(
                            text = stringResource(id = R.string.reimbursable),
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(end = 8.dp),
                        )
                    }
                    Text(
                        text = schedule.amount.toMoneyCNY(),
                        color = schedule.typeCategory.typeColor,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            },
        )

        if (schedule.charges > 0L) {
            CbListItem(
                headlineContent = { Text(text = stringResource(id = R.string.charges)) },
                trailingContent = {
                    Text(
                        text = "-${schedule.charges.toMoneyCNY()}",
                        color = LocalExtendedColors.current.expenditure,
                        style = MaterialTheme.typography.labelLarge,
                    )
                },
            )
        }

        if (schedule.typeCategory != RecordTypeCategoryEnum.INCOME && schedule.concessions > 0L) {
            CbListItem(
                headlineContent = { Text(text = stringResource(id = R.string.concessions)) },
                trailingContent = {
                    Text(
                        text = "+${schedule.concessions.toMoneyCNY()}",
                        color = LocalExtendedColors.current.income,
                        style = MaterialTheme.typography.labelLarge,
                    )
                },
            )
        }

        // 类型
        CbListItem(
            headlineContent = { Text(text = stringResource(id = R.string.type)) },
            trailingContent = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TypeIcon(
                        painter = painterDrawableResource(idStr = scheduleViews.typeIconResName),
                        containerColor = schedule.typeCategory.typeColor,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = scheduleViews.typeName,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            },
        )

        // 资产
        scheduleViews.assetName?.let { assetName ->
            CbListItem(
                headlineContent = { Text(text = stringResource(id = R.string.asset)) },
                trailingContent = {
                    Text(
                        text = assetName,
                        style = MaterialTheme.typography.labelLarge,
                    )
                },
            )
        }

        // 标签
        if (scheduleViews.tagNameList.isNotEmpty()) {
            CbListItem(
                headlineContent = { Text(text = stringResource(id = R.string.tags)) },
                trailingContent = {
                    val tagsText = with(StringBuilder()) {
                        scheduleViews.tagNameList.forEach { tag ->
                            if (isNotBlank()) append(",")
                            append(tag)
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
                                shape = MaterialTheme.shapes.small,
                            )
                            .padding(horizontal = 4.dp),
                    )
                },
            )
        }

        // 备注
        if (schedule.remark.isNotBlank()) {
            CbListItem(
                headlineContent = { Text(text = stringResource(id = R.string.remark)) },
                trailingContent = {
                    Text(
                        text = schedule.remark,
                        style = MaterialTheme.typography.labelLarge,
                    )
                },
            )
        }

        // 频率
        CbListItem(
            headlineContent = { Text(text = stringResource(id = R.string.select_frequency)) },
            trailingContent = {
                Text(
                    text = schedule.frequency.displayName(),
                    style = MaterialTheme.typography.labelLarge,
                )
            },
        )

        // 开始日期
        CbListItem(
            headlineContent = { Text(text = stringResource(id = R.string.schedule_start_date)) },
            trailingContent = {
                Text(
                    text = schedule.startDate.toDateString(),
                    style = MaterialTheme.typography.labelLarge,
                )
            },
        )

        // 结束日期
        schedule.endDate?.let { endDate ->
            CbListItem(
                headlineContent = { Text(text = stringResource(id = R.string.schedule_end_date)) },
                trailingContent = {
                    Text(
                        text = endDate.toDateString(),
                        style = MaterialTheme.typography.labelLarge,
                    )
                },
            )
        }

        // 记账时间
        CbListItem(
            headlineContent = { Text(text = stringResource(id = R.string.schedule_record_time)) },
            trailingContent = {
                Text(
                    text = schedule.recordTime.toTimeString(),
                    style = MaterialTheme.typography.labelLarge,
                )
            },
        )

        // 上次执行日期
        schedule.lastExecutedDate?.let { lastDate ->
            CbListItem(
                headlineContent = { Text(text = stringResource(id = R.string.schedule_last_executed_date)) },
                trailingContent = {
                    Text(
                        text = lastDate.toDateString(),
                        style = MaterialTheme.typography.labelLarge,
                    )
                },
            )
        }

        // 启用状态
        CbListItem(
            headlineContent = { Text(text = stringResource(id = R.string.schedule_enabled)) },
            trailingContent = {
                Switch(
                    checked = schedule.enabled,
                    onCheckedChange = null,
                )
            },
        )
    }
}

@Composable
private fun ScheduleFrequencyEnum.displayName(): String {
    return when (this) {
        ScheduleFrequencyEnum.DAILY -> stringResource(id = R.string.schedule_frequency_daily)
        ScheduleFrequencyEnum.WEEKLY -> stringResource(id = R.string.schedule_frequency_weekly)
        ScheduleFrequencyEnum.MONTHLY -> stringResource(id = R.string.schedule_frequency_monthly)
        ScheduleFrequencyEnum.YEARLY -> stringResource(id = R.string.schedule_frequency_yearly)
    }
}
