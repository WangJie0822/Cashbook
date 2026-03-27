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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cn.wj.android.cashbook.core.model.model.BillSummary
import cn.wj.android.cashbook.core.model.model.BooksModel
import cn.wj.android.cashbook.core.ui.R

/**
 * 导入概览区域
 *
 * @param fileName 文件名
 * @param summary 账单汇总
 * @param selectedBooksId 当前选中的账本 ID
 * @param booksList 可选账本列表
 * @param onBookSelected 账本选择回调
 */
@Composable
internal fun ImportSummarySection(
    fileName: String,
    summary: BillSummary,
    selectedBooksId: Long,
    booksList: List<BooksModel>,
    onBookSelected: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            text = fileName,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Text(
            text = stringResource(R.string.import_summary_total, summary.totalCount),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 4.dp),
        )

        Row(modifier = Modifier.padding(top = 2.dp)) {
            Text(
                text = stringResource(
                    R.string.import_summary_income,
                    summary.incomeCount,
                    "%.2f".format(summary.incomeAmount),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "  ",
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = stringResource(
                    R.string.import_summary_expenditure,
                    summary.expenditureCount,
                    "%.2f".format(summary.expenditureAmount),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        // 账本选择器
        val selectedBook = booksList.find { it.id == selectedBooksId }
        var expanded by remember { mutableStateOf(false) }
        Row(
            modifier = Modifier
                .padding(top = 8.dp)
                .clickable { expanded = true },
        ) {
            Text(
                text = stringResource(R.string.import_target_book) + "：",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = selectedBook?.name ?: "",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                booksList.forEach { book ->
                    DropdownMenuItem(
                        text = { Text(text = book.name) },
                        onClick = {
                            expanded = false
                            onBookSelected(book.id)
                        },
                    )
                }
            }
        }
    }
}
