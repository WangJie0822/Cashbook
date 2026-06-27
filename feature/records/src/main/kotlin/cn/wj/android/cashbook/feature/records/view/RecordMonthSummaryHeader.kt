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

package cn.wj.android.cashbook.feature.records.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cn.wj.android.cashbook.core.common.ext.toMoneyCNY
import cn.wj.android.cashbook.core.design.component.CbCard
import cn.wj.android.cashbook.core.design.component.CbHorizontalDivider
import cn.wj.android.cashbook.core.design.component.CbIconButton
import cn.wj.android.cashbook.core.design.icon.CbIcons
import cn.wj.android.cashbook.core.design.theme.rememberHapticOnClick
import cn.wj.android.cashbook.core.model.entity.DateSelectionEntity
import cn.wj.android.cashbook.core.model.enums.DateSelectionTypeEnum
import cn.wj.android.cashbook.core.model.model.AssetMonthSummaryModel
import cn.wj.android.cashbook.core.ui.R
import cn.wj.android.cashbook.core.ui.component.DateSelectionPopup
import cn.wj.android.cashbook.feature.records.viewmodel.LauncherListItem
import cn.wj.android.cashbook.feature.records.viewmodel.recordDayHeaderDateText

/**
 * 月份切换器 / 固定周期文字 + 收入/支出/结余 3 列汇总卡。
 * 供资产详情、分类统计、标签统计共用。点击周期文本打开日期选择 [DateSelectionPopup]。
 *
 * @param dateSelection 当前周期选择（内部派生显示文本与月份可切换性）
 * @param summary 收支结余汇总
 * @param showTransferHint true 时以提示文案代替 3 列（转账类型不计入收支）
 * @param showDatePopup 日期选择 Popup 是否展开
 * @param onDateClick 点击周期区域（打开 Popup）回调
 * @param onDismissDatePopup 关闭 Popup 回调
 * @param onDateSelected 日期选择回调
 * @param onPreviousMonth 切换到上一月回调（仅 ByMonth 态箭头使用）
 * @param onNextMonth 切换到下一月回调（仅 ByMonth 态箭头使用）
 */
@Composable
internal fun RecordMonthSummaryHeader(
    dateSelection: DateSelectionEntity,
    summary: AssetMonthSummaryModel,
    showTransferHint: Boolean,
    showDatePopup: Boolean = false,
    onDateClick: () -> Unit = {},
    onDismissDatePopup: () -> Unit = {},
    onDateSelected: (DateSelectionEntity) -> Unit = {},
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
) {
    val monthSwitchable = dateSelection is DateSelectionEntity.ByMonth
    val periodText = dateSelection.getDisplayText()
    CbCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            // 周期行 + 内嵌日期选择 Popup（Popup 锚定在本 Box）
            Box {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    if (monthSwitchable) {
                        CbIconButton(onClick = onPreviousMonth) {
                            Icon(
                                imageVector = CbIcons.ArrowBack,
                                contentDescription = stringResource(id = R.string.cd_previous),
                            )
                        }
                        Row(
                            modifier = Modifier.clickable(
                                onClick = rememberHapticOnClick(onClick = onDateClick),
                            ),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = periodText,
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Icon(
                                imageVector = CbIcons.ArrowDropDown,
                                contentDescription = null,
                            )
                        }
                        CbIconButton(onClick = onNextMonth) {
                            Icon(
                                imageVector = CbIcons.KeyboardArrowRight,
                                contentDescription = stringResource(id = R.string.cd_next),
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(onClick = rememberHapticOnClick(onClick = onDateClick)),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = periodText,
                                style = MaterialTheme.typography.titleMedium,
                                textAlign = TextAlign.Center,
                            )
                            Icon(
                                imageVector = CbIcons.ArrowDropDown,
                                contentDescription = null,
                            )
                        }
                    }
                }
                DateSelectionPopup(
                    expanded = showDatePopup,
                    onDismissRequest = onDismissDatePopup,
                    currentSelection = dateSelection,
                    onDateSelected = onDateSelected,
                )
            }

            if (showTransferHint) {
                Text(
                    text = stringResource(id = R.string.transfer_not_counted_in_summary),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                ) {
                    SummaryColumn(
                        label = stringResource(
                            id = if (monthSwitchable) R.string.month_income else R.string.summary_income,
                        ),
                        amount = summary.income,
                        modifier = Modifier.weight(1f),
                    )
                    SummaryColumn(
                        label = stringResource(
                            id = if (monthSwitchable) R.string.month_expend else R.string.summary_expend,
                        ),
                        amount = summary.expenditure,
                        modifier = Modifier.weight(1f),
                    )
                    SummaryColumn(
                        label = stringResource(
                            id = if (monthSwitchable) R.string.month_balance else R.string.summary_balance,
                        ),
                        amount = summary.balance,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

/**
 * 统计卡单列：标题 + 金额
 */
@Composable
private fun SummaryColumn(
    label: String,
    amount: Long,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = amount.toMoneyCNY(),
            style = MaterialTheme.typography.titleSmall,
        )
    }
}

/**
 * 记录列表按日分组头（资产/分类/标签统计共用）。
 * 日期文案按 [dateSelectionType] 自适应（与首页 DayHeaderItem 单一真源，见 [recordDayHeaderDateText]）。
 *
 * @param item 日期头数据
 * @param dateSelectionType 当前周期类型
 * @param byMonthCrossesNaturalMonth BY_MONTH 周期是否跨自然月（monthStartDay≠1）
 */
@Composable
internal fun RecordDayHeader(
    item: LauncherListItem.DayHeader,
    dateSelectionType: DateSelectionTypeEnum,
    byMonthCrossesNaturalMonth: Boolean,
) {
    val dayTypeSuffix = when (item.dayType) {
        0 -> stringResource(id = R.string.today_with_brackets)
        -1 -> stringResource(id = R.string.yesterday_with_brackets)
        -2 -> stringResource(id = R.string.before_yesterday_with_brackets)
        else -> ""
    }
    val dateArray = item.dateStr.split("-")
    val year = dateArray.getOrNull(0)?.toIntOrNull() ?: 0
    val month = dateArray.getOrNull(1)?.toIntOrNull() ?: 0
    val dateText = recordDayHeaderDateText(
        type = dateSelectionType,
        year = year,
        month = month,
        day = item.day,
        dayTypeSuffix = dayTypeSuffix,
        dayLabel = stringResource(id = R.string.day),
        monthLabel = stringResource(id = R.string.month),
        yearLabel = stringResource(id = R.string.year),
        byMonthCrossesNaturalMonth = byMonthCrossesNaturalMonth,
    )
    Column {
        Text(
            text = dateText,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        )
        CbHorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
    }
}
