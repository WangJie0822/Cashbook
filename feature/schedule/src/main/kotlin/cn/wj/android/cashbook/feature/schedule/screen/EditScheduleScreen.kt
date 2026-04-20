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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Switch
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.wj.android.cashbook.core.common.Symbol
import cn.wj.android.cashbook.core.common.ext.completeZero
import cn.wj.android.cashbook.core.common.ext.withCNY
import cn.wj.android.cashbook.core.common.tools.dateFormat
import cn.wj.android.cashbook.core.design.component.Calculator
import cn.wj.android.cashbook.core.design.component.CbAlertDialog
import cn.wj.android.cashbook.core.design.component.CbFloatingActionButton
import cn.wj.android.cashbook.core.design.component.CbHorizontalDivider
import cn.wj.android.cashbook.core.design.component.CbListItem
import cn.wj.android.cashbook.core.design.component.CbModalBottomSheet
import cn.wj.android.cashbook.core.design.component.CbScaffold
import cn.wj.android.cashbook.core.design.component.CbTab
import cn.wj.android.cashbook.core.design.component.CbTabRow
import cn.wj.android.cashbook.core.design.component.CbTextButton
import cn.wj.android.cashbook.core.design.component.CbTextField
import cn.wj.android.cashbook.core.design.component.CbTopAppBar
import cn.wj.android.cashbook.core.design.component.Loading
import cn.wj.android.cashbook.core.design.component.TextFieldState
import cn.wj.android.cashbook.core.design.icon.CbIcons
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.model.enums.ScheduleFrequencyEnum
import cn.wj.android.cashbook.core.ui.LocalProgressDialogController
import cn.wj.android.cashbook.core.ui.R
import cn.wj.android.cashbook.core.ui.expand.text
import cn.wj.android.cashbook.core.ui.expand.typeColor
import cn.wj.android.cashbook.feature.schedule.viewmodel.EditScheduleBottomSheetEnum
import cn.wj.android.cashbook.feature.schedule.viewmodel.EditScheduleUiState
import cn.wj.android.cashbook.feature.schedule.viewmodel.EditScheduleViewModel
import java.util.Calendar

/**
 * 编辑周期规则界面
 */
@Composable
internal fun EditScheduleRoute(
    scheduleId: Long,
    typeListContent: @Composable (typeCategory: RecordTypeCategoryEnum, currentTypeId: Long, onTypeChange: (Long) -> Unit) -> Unit,
    assetBottomSheetContent: @Composable (currentTypeId: Long, selectedAssetId: Long, onAssetChange: (Long) -> Unit) -> Unit,
    tagBottomSheetContent: @Composable (selectedTagIdList: List<Long>, onTagIdListChange: (List<Long>) -> Unit, onRequestDismissSheet: () -> Unit) -> Unit,
    onRequestPopBackStack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EditScheduleViewModel = hiltViewModel(),
) {
    LaunchedEffect(scheduleId) {
        viewModel.updateScheduleId(scheduleId)
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle(initialValue = EditScheduleUiState.Loading)
    val savingHintText = stringResource(id = R.string.saving)
    val progressDialogController = LocalProgressDialogController.current

    EditScheduleScreen(
        isCreate = scheduleId == -1L,
        uiState = uiState,
        typeListContent = typeListContent,
        assetBottomSheetContent = assetBottomSheetContent,
        tagBottomSheetContent = tagBottomSheetContent,
        bottomSheetType = viewModel.bottomSheetType,
        onBottomSheetDismiss = viewModel::dismissBottomSheet,
        onTypeCategorySelect = viewModel::updateTypeCategory,
        onShowSelectAssetSheet = viewModel::showSelectAssetSheet,
        onShowSelectFrequencySheet = viewModel::showSelectFrequencySheet,
        onShowSelectTagSheet = viewModel::showSelectTagSheet,
        onShowAmountSheet = viewModel::showAmountSheet,
        onShowChargesSheet = viewModel::showChargesSheet,
        onShowConcessionsSheet = viewModel::showConcessionsSheet,
        onTypeChange = viewModel::updateType,
        onAssetChange = viewModel::updateAsset,
        onFrequencyChange = viewModel::updateFrequency,
        onStartDateChange = viewModel::updateStartDate,
        onEndDateChange = viewModel::updateEndDate,
        onRecordTimeChange = viewModel::updateRecordTime,
        onAmountChange = viewModel::updateAmount,
        onChargesChange = viewModel::updateCharges,
        onConcessionsChange = viewModel::updateConcessions,
        onRemarkChange = viewModel::updateRemark,
        onEnabledChange = viewModel::updateEnabled,
        onReimbursableClick = viewModel::updateReimbursable,
        onTagChange = viewModel::updateTagIdList,
        onSaveClick = {
            viewModel.trySave(progressDialogController, savingHintText, onRequestPopBackStack)
        },
        onBackClick = onRequestPopBackStack,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun EditScheduleScreen(
    isCreate: Boolean,
    uiState: EditScheduleUiState,
    typeListContent: @Composable (typeCategory: RecordTypeCategoryEnum, currentTypeId: Long, onTypeChange: (Long) -> Unit) -> Unit,
    assetBottomSheetContent: @Composable (currentTypeId: Long, selectedAssetId: Long, onAssetChange: (Long) -> Unit) -> Unit,
    tagBottomSheetContent: @Composable (selectedTagIdList: List<Long>, onTagIdListChange: (List<Long>) -> Unit, onRequestDismissSheet: () -> Unit) -> Unit,
    bottomSheetType: EditScheduleBottomSheetEnum,
    onBottomSheetDismiss: () -> Unit,
    onTypeCategorySelect: (RecordTypeCategoryEnum) -> Unit,
    onShowSelectAssetSheet: () -> Unit,
    onShowSelectFrequencySheet: () -> Unit,
    onShowSelectTagSheet: () -> Unit,
    onShowAmountSheet: () -> Unit,
    onShowChargesSheet: () -> Unit,
    onShowConcessionsSheet: () -> Unit,
    onTypeChange: (Long, RecordTypeCategoryEnum) -> Unit,
    onAssetChange: (Long) -> Unit,
    onFrequencyChange: (ScheduleFrequencyEnum) -> Unit,
    onStartDateChange: (Long) -> Unit,
    onEndDateChange: (Long?) -> Unit,
    onRecordTimeChange: (Long) -> Unit,
    onAmountChange: (String) -> Unit,
    onChargesChange: (String) -> Unit,
    onConcessionsChange: (String) -> Unit,
    onRemarkChange: (String) -> Unit,
    onEnabledChange: (Boolean) -> Unit,
    onReimbursableClick: (Boolean) -> Unit,
    onTagChange: (List<Long>) -> Unit,
    onSaveClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val typeColor = (uiState as? EditScheduleUiState.Success)?.typeCategory?.typeColor
        ?: MaterialTheme.colorScheme.primary

    CbScaffold(
        modifier = modifier,
        topBar = {
            CbTopAppBar(
                onBackClick = onBackClick,
                title = {
                    if (uiState is EditScheduleUiState.Success) {
                        CbTabRow(
                            modifier = Modifier.fillMaxSize(),
                            selectedTabIndex = uiState.typeCategory.ordinal,
                            containerColor = Color.Unspecified,
                            contentColor = Color.Unspecified,
                            indicator = { tabPositions ->
                                TabRowDefaults.SecondaryIndicator(
                                    modifier = Modifier.tabIndicatorOffset(tabPositions[uiState.typeCategory.ordinal]),
                                    color = typeColor,
                                )
                            },
                            divider = {},
                        ) {
                            // 周期记账只支持支出和收入
                            listOf(
                                RecordTypeCategoryEnum.EXPENDITURE,
                                RecordTypeCategoryEnum.INCOME,
                            ).forEach { enum ->
                                CbTab(
                                    selected = uiState.typeCategory == enum,
                                    onClick = { onTypeCategorySelect(enum) },
                                    text = { Text(text = enum.text) },
                                    selectedContentColor = typeColor,
                                    unselectedContentColor = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    } else {
                        Text(
                            text = stringResource(
                                id = if (isCreate) R.string.new_schedule else R.string.edit_schedule,
                            ),
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            if (uiState is EditScheduleUiState.Success) {
                CbFloatingActionButton(
                    onClick = onSaveClick,
                ) {
                    Icon(
                        imageVector = CbIcons.SaveAs,
                        contentDescription = stringResource(id = R.string.cd_confirm),
                    )
                }
            }
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
        ) {
            if (bottomSheetType != EditScheduleBottomSheetEnum.NONE) {
                CbModalBottomSheet(
                    onDismissRequest = onBottomSheetDismiss,
                    sheetState = rememberModalBottomSheetState(
                        confirmValueChange = {
                            if (it == SheetValue.Hidden) {
                                onBottomSheetDismiss()
                            }
                            true
                        },
                    ),
                    dragHandle = if (bottomSheetType.isCalculator) {
                        null
                    } else {
                        @Composable {
                            androidx.compose.material3.BottomSheetDefaults.DragHandle()
                        }
                    },
                ) {
                    when (bottomSheetType) {
                        EditScheduleBottomSheetEnum.TYPE -> {
                            // 类型列表现在直接显示在页面上，不再在 bottom sheet 中
                        }

                        EditScheduleBottomSheetEnum.ASSET -> {
                            if (uiState is EditScheduleUiState.Success) {
                                assetBottomSheetContent(
                                    uiState.typeId,
                                    uiState.assetId,
                                ) { assetId ->
                                    onAssetChange(assetId)
                                    onBottomSheetDismiss()
                                }
                            }
                        }

                        EditScheduleBottomSheetEnum.FREQUENCY -> {
                            SelectFrequencySheet(
                                selected = (uiState as? EditScheduleUiState.Success)?.frequency,
                                onSelect = { frequency ->
                                    onFrequencyChange(frequency)
                                    onBottomSheetDismiss()
                                },
                            )
                        }

                        EditScheduleBottomSheetEnum.AMOUNT -> {
                            (uiState as? EditScheduleUiState.Success)?.let { data ->
                                Calculator(
                                    defaultText = data.amountText,
                                    primaryColor = typeColor,
                                    onConfirmClick = {
                                        onAmountChange(it)
                                        onBottomSheetDismiss()
                                    },
                                )
                            }
                        }

                        EditScheduleBottomSheetEnum.CHARGES -> {
                            (uiState as? EditScheduleUiState.Success)?.let { data ->
                                Calculator(
                                    defaultText = data.chargesText,
                                    primaryColor = typeColor,
                                    onConfirmClick = {
                                        onChargesChange(it)
                                        onBottomSheetDismiss()
                                    },
                                )
                            }
                        }

                        EditScheduleBottomSheetEnum.CONCESSIONS -> {
                            (uiState as? EditScheduleUiState.Success)?.let { data ->
                                Calculator(
                                    defaultText = data.concessionsText,
                                    primaryColor = typeColor,
                                    onConfirmClick = {
                                        onConcessionsChange(it)
                                        onBottomSheetDismiss()
                                    },
                                )
                            }
                        }

                        EditScheduleBottomSheetEnum.TAG -> {
                            (uiState as? EditScheduleUiState.Success)?.let { data ->
                                tagBottomSheetContent(
                                    data.tagIdList,
                                    onTagChange,
                                    onBottomSheetDismiss,
                                )
                            }
                        }

                        else -> {}
                    }
                }
            }

            when (uiState) {
                EditScheduleUiState.Loading -> {
                    Loading(modifier = Modifier.align(Alignment.Center))
                }

                is EditScheduleUiState.Success -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(state = rememberScrollState())
                            .padding(top = 8.dp)
                            .padding(horizontal = 16.dp),
                    ) {
                        // 金额
                        Amount(
                            amount = uiState.amountText,
                            primaryColor = typeColor,
                            onAmountClick = onShowAmountSheet,
                        )
                        CbHorizontalDivider()

                        // 类型标签
                        Text(
                            text = stringResource(id = R.string.record_type),
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
                        )

                        // 类型列表
                        typeListContent(
                            uiState.typeCategory,
                            uiState.typeId,
                        ) { typeId ->
                            onTypeChange(typeId, uiState.typeCategory)
                        }

                        // 备注
                        val remarkTextState = remember(uiState.remark) {
                            TextFieldState(
                                defaultText = uiState.remark,
                                filter = { text ->
                                    onRemarkChange(text)
                                    true
                                },
                            )
                        }
                        CbTextField(
                            textFieldState = remarkTextState,
                            label = { Text(text = stringResource(id = R.string.remark)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                        )

                        // 其他选项
                        FlowRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            // 资产
                            val hasAsset = uiState.assetId > 0
                            ElevatedFilterChip(
                                selected = hasAsset,
                                onClick = onShowSelectAssetSheet,
                                label = {
                                    Text(
                                        text = stringResource(id = R.string.target_asset) +
                                            if (hasAsset) ":${uiState.assetText}" else "",
                                    )
                                },
                            )

                            // 记账时间
                            var showTimePicker by remember { mutableStateOf(false) }
                            val recordTimeCalendar = Calendar.getInstance().apply {
                                timeInMillis = uiState.recordTime
                            }
                            val timeStr = "${recordTimeCalendar.get(Calendar.HOUR_OF_DAY).completeZero()}:${recordTimeCalendar.get(Calendar.MINUTE).completeZero()}"
                            ElevatedFilterChip(
                                selected = true,
                                onClick = { showTimePicker = true },
                                label = { Text(text = stringResource(id = R.string.schedule_record_time) + " $timeStr") },
                            )

                            if (showTimePicker) {
                                val timePickerState = rememberTimePickerState(
                                    initialHour = recordTimeCalendar.get(Calendar.HOUR_OF_DAY),
                                    initialMinute = recordTimeCalendar.get(Calendar.MINUTE),
                                    is24Hour = true,
                                )
                                CbAlertDialog(
                                    onDismissRequest = { showTimePicker = false },
                                    confirmButton = {
                                        CbTextButton(
                                            onClick = {
                                                val newCalendar = Calendar.getInstance().apply {
                                                    timeInMillis = uiState.recordTime
                                                    set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                                                    set(Calendar.MINUTE, timePickerState.minute)
                                                    set(Calendar.SECOND, 0)
                                                    set(Calendar.MILLISECOND, 0)
                                                }
                                                onRecordTimeChange(newCalendar.timeInMillis)
                                                showTimePicker = false
                                            },
                                        ) {
                                            Text(text = stringResource(id = R.string.confirm))
                                        }
                                    },
                                    dismissButton = {
                                        CbTextButton(onClick = { showTimePicker = false }) {
                                            Text(text = stringResource(id = R.string.cancel))
                                        }
                                    },
                                    text = {
                                        TimePicker(state = timePickerState)
                                    },
                                )
                            }

                            // 频率
                            ElevatedFilterChip(
                                selected = true,
                                onClick = onShowSelectFrequencySheet,
                                label = { Text(text = uiState.frequency.displayName()) },
                            )

                            // 标签
                            val hasTag = uiState.tagText.isNotBlank()
                            ElevatedFilterChip(
                                selected = hasTag,
                                onClick = onShowSelectTagSheet,
                                label = {
                                    Text(
                                        text = stringResource(id = R.string.tags) +
                                            if (hasTag) ":${uiState.tagText}" else "",
                                    )
                                },
                            )

                            // 手续费
                            val hasCharges = uiState.chargesText.isNotBlank() && uiState.chargesText != "0"
                            ElevatedFilterChip(
                                selected = hasCharges,
                                onClick = onShowChargesSheet,
                                label = {
                                    Text(
                                        text = stringResource(id = R.string.charges) +
                                            if (hasCharges) ":${uiState.chargesText.withCNY()}" else "",
                                    )
                                },
                            )

                            if (uiState.typeCategory == RecordTypeCategoryEnum.EXPENDITURE) {
                                // 只有支出类型显示是否可报销
                                val reimbursable = uiState.reimbursable
                                ElevatedFilterChip(
                                    selected = reimbursable,
                                    onClick = { onReimbursableClick(!reimbursable) },
                                    leadingIcon = {
                                        if (reimbursable) {
                                            Icon(
                                                imageVector = CbIcons.Check,
                                                contentDescription = null,
                                            )
                                        }
                                    },
                                    label = { Text(text = stringResource(id = R.string.reimbursable)) },
                                )
                            }

                            // 优惠（非收入类型才有）
                            if (uiState.typeCategory != RecordTypeCategoryEnum.INCOME) {
                                val hasConcessions = uiState.concessionsText.isNotBlank() && uiState.concessionsText != "0"
                                ElevatedFilterChip(
                                    selected = hasConcessions,
                                    onClick = onShowConcessionsSheet,
                                    label = {
                                        Text(
                                            text = stringResource(id = R.string.concessions) +
                                                if (hasConcessions) ":${uiState.concessionsText.withCNY()}" else "",
                                        )
                                    },
                                )
                            }
                        }

                        CbHorizontalDivider(modifier = Modifier.padding(top = 8.dp))

                        // 开始日期
                        DatePickerListItem(
                            label = stringResource(id = R.string.schedule_start_date),
                            dateMs = uiState.startDate,
                            onDateSelected = onStartDateChange,
                        )

                        // 结束日期
                        DatePickerListItem(
                            label = stringResource(id = R.string.schedule_end_date),
                            dateMs = uiState.endDate,
                            onDateSelected = onEndDateChange,
                            clearable = true,
                        )

                        CbHorizontalDivider(modifier = Modifier.padding(top = 8.dp))

                        // 启用开关
                        CbListItem(
                            headlineContent = {
                                Text(
                                    text = stringResource(id = R.string.schedule_enabled),
                                    modifier = Modifier.padding(start = 16.dp),
                                )
                            },
                            trailingContent = {
                                Switch(
                                    checked = uiState.enabled,
                                    onCheckedChange = onEnabledChange,
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
 * 金额显示框
 */
@Composable
private fun Amount(
    amount: String,
    primaryColor: Color,
    onAmountClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onAmountClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = Symbol.CNY,
            color = primaryColor,
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = amount,
            color = primaryColor,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(8.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerListItem(
    label: String,
    dateMs: Long?,
    onDateSelected: (Long) -> Unit,
    clearable: Boolean = false,
    onDateCleared: (() -> Unit)? = null,
) {
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        val datePickerState = rememberDatePickerState(dateMs)
        DatePickerDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                CbTextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { onDateSelected(it) }
                        showDialog = false
                    },
                ) {
                    Text(text = stringResource(id = R.string.confirm))
                }
            },
            dismissButton = {
                CbTextButton(onClick = { showDialog = false }) {
                    Text(text = stringResource(id = R.string.cancel))
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    CbListItem(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showDialog = true },
        headlineContent = {
            Text(text = label, modifier = Modifier.padding(start = 16.dp))
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = dateMs?.dateFormat() ?: stringResource(id = R.string.un_set),
                )
                Icon(
                    imageVector = CbIcons.KeyboardArrowRight,
                    contentDescription = null,
                )
            }
        },
    )
}

@Composable
private fun SelectFrequencySheet(
    selected: ScheduleFrequencyEnum?,
    onSelect: (ScheduleFrequencyEnum) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(id = R.string.select_frequency),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        )
        CbHorizontalDivider()
        ScheduleFrequencyEnum.entries.forEach { frequency ->
            CbListItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(frequency) },
                headlineContent = {
                    Text(
                        text = frequency.displayName(),
                        modifier = Modifier.padding(start = 16.dp),
                    )
                },
                trailingContent = {
                    if (frequency == selected) {
                        Icon(imageVector = CbIcons.Check, contentDescription = null)
                    }
                },
            )
        }
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
