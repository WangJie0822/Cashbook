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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import cn.wj.android.cashbook.core.design.component.Empty
import cn.wj.android.cashbook.core.design.component.Footer
import cn.wj.android.cashbook.core.design.theme.rememberHapticOnClick
import cn.wj.android.cashbook.core.model.entity.DateSelectionEntity
import cn.wj.android.cashbook.core.model.entity.RecordViewsEntity
import cn.wj.android.cashbook.core.model.model.AssetMonthSummaryModel
import cn.wj.android.cashbook.core.ui.R
import cn.wj.android.cashbook.feature.records.view.RecordDayHeader
import cn.wj.android.cashbook.feature.records.view.RecordMonthSummaryHeader
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
        showDatePopup = viewModel.showDatePopup,
        onDateClick = viewModel::displayDatePopup,
        onDismissDatePopup = viewModel::dismissDatePopup,
        onDateSelected = viewModel::updateDateSelection,
        onPreviousMonth = { viewModel.updateMonth(currentMonth.minusMonths(1)) },
        onNextMonth = { viewModel.updateMonth(currentMonth.plusMonths(1)) },
        onRecordItemClick = onRecordItemClick,
    )
}

/**
 * 资产信息界面记录列表
 *
 * @param topContent 列表头布局（资产信息卡片）
 * @param dateSelection 当前周期选择（全部/年/月/时间段/按日）
 * @param summary 当前周期收支结余汇总
 * @param recordList 记录列表数据
 * @param showDatePopup 日期选择 Popup 是否展开
 * @param onDateClick 点击周期区域（打开 Popup）回调
 * @param onDismissDatePopup 关闭 Popup 回调
 * @param onDateSelected 日期选择回调
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
    showDatePopup: Boolean = false,
    onDateClick: () -> Unit = {},
    onDismissDatePopup: () -> Unit = {},
    onDateSelected: (DateSelectionEntity) -> Unit = {},
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
                RecordMonthSummaryHeader(
                    dateSelection = dateSelection,
                    summary = summary,
                    showTransferHint = false,
                    showDatePopup = showDatePopup,
                    onDateClick = onDateClick,
                    onDismissDatePopup = onDismissDatePopup,
                    onDateSelected = onDateSelected,
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
                            RecordDayHeader(item = item)
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
