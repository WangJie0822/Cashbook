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

package cn.wj.android.cashbook.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import cn.wj.android.cashbook.core.design.component.CommonDivider
import cn.wj.android.cashbook.core.ui.R
import java.time.YearMonth
import java.util.Calendar

@Composable
fun SelectDateDialog(
    onDialogDismiss: () -> Unit,
    currentDate: YearMonth,
    yearSelectable: Boolean = false,
    yearSelected: Boolean = false,
    onDateSelected: (YearMonth, Boolean) -> Unit,
) {
    Dialog(onDismissRequest = onDialogDismiss) {
        Surface(
            shape = AlertDialogDefaults.shape,
            color = AlertDialogDefaults.containerColor,
            tonalElevation = AlertDialogDefaults.TonalElevation,
        ) {
            Column(
                modifier = Modifier
                    .sizeIn(minWidth = 280.dp, maxWidth = 560.dp)
                    .padding(16.dp),
            ) {
                val currentYear = currentDate.year
                val currentMonth = currentDate.monthValue
                var selectedYear by remember(currentDate) {
                    mutableIntStateOf(currentYear)
                }
                val startYear = Calendar.getInstance()[Calendar.YEAR] + 1
                val yearList = mutableListOf<Int>()
                for (i in startYear downTo 2000) {
                    yearList.add(i)
                }
                LazyRow(
                    modifier = Modifier.padding(vertical = 4.dp),
                    content = {
                        items(yearList) { year ->
                            val containerColor = if (year == selectedYear) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                Color.Transparent
                            }
                            Text(
                                text = "$year${stringResource(id = R.string.year)}",
                                color = contentColorFor(backgroundColor = containerColor),
                                modifier = Modifier
                                    .clickable {
                                        selectedYear = year
                                    }
                                    .background(
                                        color = containerColor,
                                        shape = MaterialTheme.shapes.large,
                                    )
                                    .padding(
                                        horizontal = 8.dp,
                                        vertical = 4.dp,
                                    ),
                            )
                        }
                    },
                )
                CommonDivider(modifier = Modifier.fillMaxWidth())
                Column(
                    modifier = Modifier
                        .fillMaxWidth(),
                    content = {
                        for (c in 0 until 4) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                content = {
                                    for (r in 0 until 3) {
                                        val month = c * 3 + r + 1
                                        val containerColor =
                                            if (!(yearSelectable && yearSelected) && selectedYear == currentYear && month == currentMonth) {
                                                MaterialTheme.colorScheme.primaryContainer
                                            } else {
                                                Color.Transparent
                                            }
                                        Text(
                                            text = "$month${stringResource(id = R.string.month)}",
                                            textAlign = TextAlign.Center,
                                            color = contentColorFor(backgroundColor = containerColor),
                                            modifier = Modifier
                                                .clickable {
                                                    onDateSelected(
                                                        YearMonth.of(
                                                            selectedYear,
                                                            month,
                                                        ),
                                                        false,
                                                    )
                                                }
                                                .background(
                                                    color = containerColor,
                                                    shape = MaterialTheme.shapes.large,
                                                )
                                                .weight(1f)
                                                .padding(vertical = 8.dp),
                                        )
                                    }
                                },
                            )
                        }
                    },
                )
                if (yearSelectable) {
                    val containerColor =
                        if (yearSelected && selectedYear == currentYear) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            Color.Transparent
                        }
                    Text(
                        text = "全年",
                        textAlign = TextAlign.Center,
                        color = contentColorFor(backgroundColor = containerColor),
                        modifier = Modifier
                            .clickable {
                                onDateSelected(
                                    YearMonth.of(
                                        selectedYear,
                                        1,
                                    ),
                                    true,
                                )
                            }
                            .fillMaxWidth()
                            .background(
                                color = containerColor,
                                shape = MaterialTheme.shapes.large,
                            )
                            .padding(vertical = 8.dp),
                    )
                }
            }
        }
    }
}
