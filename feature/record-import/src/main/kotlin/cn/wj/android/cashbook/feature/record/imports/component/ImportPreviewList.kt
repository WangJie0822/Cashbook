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

package cn.wj.android.cashbook.feature.record.imports.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cn.wj.android.cashbook.core.common.tools.DATE_FORMAT_NO_SECONDS
import cn.wj.android.cashbook.core.common.tools.dateFormat
import cn.wj.android.cashbook.core.model.model.BillDirection
import cn.wj.android.cashbook.core.model.model.DuplicateStatus
import cn.wj.android.cashbook.core.model.model.ImportPreviewItem
import cn.wj.android.cashbook.core.ui.R

/**
 * 记录预览列表
 *
 * @param items 预览条目列表
 * @param onToggleSelection 切换选中状态
 * @param onTypeClick 点击分类标签
 * @param listState LazyColumn 状态
 */
@Composable
internal fun ImportPreviewList(
    items: List<ImportPreviewItem>,
    onToggleSelection: (Int) -> Unit,
    onTypeClick: (Int) -> Unit,
    listState: LazyListState,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        state = listState,
        modifier = modifier,
    ) {
        itemsIndexed(items) { index, item ->
            ImportPreviewRow(
                item = item,
                onToggleSelection = { onToggleSelection(index) },
                onTypeClick = { onTypeClick(index) },
            )
        }
    }
}

@Composable
private fun ImportPreviewRow(
    item: ImportPreviewItem,
    onToggleSelection: () -> Unit,
    onTypeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onToggleSelection)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = item.selected,
            onCheckedChange = { onToggleSelection() },
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = item.billItem.counterparty,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Text(
                    text = "${if (item.billItem.direction == BillDirection.EXPENDITURE) "-" else "+"}¥${"%.2f".format(item.billItem.amount)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (item.billItem.direction == BillDirection.EXPENDITURE) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = item.billItem.transactionTime.dateFormat(DATE_FORMAT_NO_SECONDS),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // 重复状态标签
                    when (item.duplicateStatus) {
                        DuplicateStatus.EXACT -> {
                            DuplicateTag(
                                text = stringResource(R.string.import_duplicate_exact),
                                isExact = true,
                            )
                        }

                        DuplicateStatus.POSSIBLE -> {
                            DuplicateTag(
                                text = stringResource(R.string.import_duplicate_possible),
                                isExact = false,
                            )
                        }

                        DuplicateStatus.NONE -> { /* 不显示 */ }
                    }

                    // 分类标签
                    if (item.mappedTypeName.isNotBlank()) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier
                                .padding(start = 4.dp)
                                .clickable(onClick = onTypeClick),
                        ) {
                            Text(
                                text = item.mappedTypeName,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DuplicateTag(
    text: String,
    isExact: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = if (isExact) {
            MaterialTheme.colorScheme.errorContainer
        } else {
            MaterialTheme.colorScheme.tertiaryContainer
        },
        shape = MaterialTheme.shapes.small,
        modifier = modifier,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = if (isExact) {
                MaterialTheme.colorScheme.onErrorContainer
            } else {
                MaterialTheme.colorScheme.onTertiaryContainer
            },
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}
