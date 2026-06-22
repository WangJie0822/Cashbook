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

import androidx.compose.foundation.layout.Arrangement
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
import cn.wj.android.cashbook.core.model.model.AssetMonthSummaryModel
import cn.wj.android.cashbook.core.ui.R
import cn.wj.android.cashbook.feature.records.viewmodel.LauncherListItem

/**
 * 月份切换器 / 固定周期文字 + 收入/支出/结余 3 列汇总卡。
 * 供资产详情、分类统计、标签统计共用。
 *
 * @param periodText 当前周期显示文本
 * @param monthSwitchable true 显示前后翻月箭头；false 仅居中文字（固定周期）
 * @param summary 收支结余汇总
 * @param showTransferHint true 时以提示文案代替 3 列（转账类型不计入收支）
 * @param onPreviousMonth 切换到上一月回调（仅 [monthSwitchable] 时使用）
 * @param onNextMonth 切换到下一月回调（仅 [monthSwitchable] 时使用）
 */
@Composable
internal fun RecordMonthSummaryHeader(
    periodText: String,
    monthSwitchable: Boolean,
    summary: AssetMonthSummaryModel,
    showTransferHint: Boolean,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
) {
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
            // 周期行：月份模式保持「上一月 - 文字 - 下一月」原布局；固定周期模式仅居中文字
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
                    Text(
                        text = periodText,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    CbIconButton(onClick = onNextMonth) {
                        Icon(
                            imageVector = CbIcons.KeyboardArrowRight,
                            contentDescription = stringResource(id = R.string.cd_next),
                        )
                    }
                } else {
                    Text(
                        text = periodText,
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
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
                        label = stringResource(id = R.string.month_income),
                        amount = summary.income,
                        modifier = Modifier.weight(1f),
                    )
                    SummaryColumn(
                        label = stringResource(id = R.string.month_expend),
                        amount = summary.expenditure,
                        modifier = Modifier.weight(1f),
                    )
                    SummaryColumn(
                        label = stringResource(id = R.string.month_balance),
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
 * 记录列表按日分组头（资产/分类/标签统计共用）
 *
 * @param item 日期头数据
 */
@Composable
internal fun RecordDayHeader(item: LauncherListItem.DayHeader) {
    val dayTypeSuffix = when (item.dayType) {
        0 -> stringResource(id = R.string.today_with_brackets)
        -1 -> stringResource(id = R.string.yesterday_with_brackets)
        -2 -> stringResource(id = R.string.before_yesterday_with_brackets)
        else -> ""
    }
    Column {
        Text(
            text = "${item.day}${stringResource(id = R.string.day)}$dayTypeSuffix",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        )
        CbHorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
    }
}
