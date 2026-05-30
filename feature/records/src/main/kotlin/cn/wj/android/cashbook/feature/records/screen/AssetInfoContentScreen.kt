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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import cn.wj.android.cashbook.core.common.ext.toMoneyCNY
import cn.wj.android.cashbook.core.design.component.CbCard
import cn.wj.android.cashbook.core.design.component.CbHorizontalDivider
import cn.wj.android.cashbook.core.design.component.CbIconButton
import cn.wj.android.cashbook.core.design.component.Empty
import cn.wj.android.cashbook.core.design.component.Footer
import cn.wj.android.cashbook.core.design.icon.CbIcons
import cn.wj.android.cashbook.core.design.theme.rememberHapticOnClick
import cn.wj.android.cashbook.core.model.entity.DateSelectionEntity
import cn.wj.android.cashbook.core.model.entity.RecordViewsEntity
import cn.wj.android.cashbook.core.model.model.AssetMonthSummaryModel
import cn.wj.android.cashbook.core.ui.R
import cn.wj.android.cashbook.feature.records.viewmodel.AssetInfoContentViewModel
import cn.wj.android.cashbook.feature.records.viewmodel.LauncherListItem
import java.time.YearMonth

/**
 * 资产信息界面记录列表
 *
 * @param assetId 资产 id
 * @param isCreditCard 当前资产是否为信用卡（影响收支方向口径）
 * @param topContent 列表头布局（资产信息卡片，由资产模块提供）
 * @param onRecordItemClick 记录列表 item 点击回调
 */
@Composable
internal fun AssetInfoContentRoute(
    assetId: Long,
    isCreditCard: Boolean,
    topContent: @Composable () -> Unit,
    onRecordItemClick: (RecordViewsEntity) -> Unit,
    viewModel: AssetInfoContentViewModel = hiltViewModel<AssetInfoContentViewModel>().apply {
        updateAssetId(assetId)
        updateIsCreditCard(isCreditCard)
    },
) {
    val recordList = viewModel.recordList.collectAsLazyPagingItems()
    val dateSelection by viewModel.dateSelection.collectAsStateWithLifecycle()
    val summary by viewModel.summary.collectAsStateWithLifecycle()

    val currentMonth = (dateSelection as? DateSelectionEntity.ByMonth)?.yearMonth ?: YearMonth.now()

    AssetInfoContentScreen(
        topContent = topContent,
        dateSelection = dateSelection,
        summary = summary,
        recordList = recordList,
        onPreviousMonth = { viewModel.updateMonth(currentMonth.minusMonths(1)) },
        onNextMonth = { viewModel.updateMonth(currentMonth.plusMonths(1)) },
        onRecordItemClick = onRecordItemClick,
    )
}

/**
 * 资产信息界面记录列表
 *
 * @param topContent 列表头布局（资产信息卡片）
 * @param dateSelection 当前月份选择
 * @param summary 当前月份收支结余汇总
 * @param recordList 记录列表数据
 * @param onPreviousMonth 切换到上一月回调
 * @param onNextMonth 切换到下一月回调
 * @param onRecordItemClick 记录列表 item 点击回调
 */
@Composable
internal fun AssetInfoContentScreen(
    topContent: @Composable () -> Unit,
    dateSelection: DateSelectionEntity,
    summary: AssetMonthSummaryModel,
    recordList: LazyPagingItems<LauncherListItem>,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onRecordItemClick: (RecordViewsEntity) -> Unit,
) {
    LazyColumn(
        content = {
            item {
                topContent.invoke()
            }

            item {
                AssetMonthHeader(
                    dateSelection = dateSelection,
                    summary = summary,
                    onPreviousMonth = onPreviousMonth,
                    onNextMonth = onNextMonth,
                )
            }

            if (recordList.itemCount <= 0) {
                item {
                    Empty(
                        hintText = stringResource(id = R.string.asset_no_record_data_hint),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            } else {
                items(
                    count = recordList.itemCount,
                    key = { index ->
                        when (val item = recordList.peek(index)) {
                            is LauncherListItem.DayHeader -> "header_${item.dateStr}"
                            is LauncherListItem.Record -> "record_${item.entity.id}"
                            null -> "placeholder_$index"
                        }
                    },
                ) { index ->
                    when (val item = recordList[index]) {
                        is LauncherListItem.DayHeader -> {
                            AssetRecordDayHeader(item = item)
                        }

                        is LauncherListItem.Record -> {
                            RecordListItem(
                                item = item.entity,
                                showDate = false,
                                modifier = Modifier.clickable(
                                    onClick = rememberHapticOnClick {
                                        onRecordItemClick(item.entity)
                                    },
                                ),
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
        },
    )
}

/**
 * 资产详情记录列表的日期分组头
 *
 * @param item 日期头数据
 */
@Composable
private fun AssetRecordDayHeader(item: LauncherListItem.DayHeader) {
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

/**
 * 月份切换器 + 收入/支出/结余统计卡
 *
 * @param dateSelection 当前月份选择
 * @param summary 当前月份收支结余汇总
 * @param onPreviousMonth 切换到上一月回调
 * @param onNextMonth 切换到下一月回调
 */
@Composable
private fun AssetMonthHeader(
    dateSelection: DateSelectionEntity,
    summary: AssetMonthSummaryModel,
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
            // 月份切换器
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                CbIconButton(onClick = onPreviousMonth) {
                    Icon(
                        imageVector = CbIcons.ArrowBack,
                        contentDescription = stringResource(id = R.string.cd_previous),
                    )
                }
                Text(
                    text = dateSelection.getDisplayText(),
                    style = MaterialTheme.typography.titleMedium,
                )
                CbIconButton(onClick = onNextMonth) {
                    Icon(
                        imageVector = CbIcons.KeyboardArrowRight,
                        contentDescription = stringResource(id = R.string.cd_next),
                    )
                }
            }

            // 收入 / 支出 / 结余 统计
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
