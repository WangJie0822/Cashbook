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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import cn.wj.android.cashbook.core.design.component.CbHorizontalDivider
import cn.wj.android.cashbook.core.design.component.CbTab
import cn.wj.android.cashbook.core.design.component.CbTabRow
import cn.wj.android.cashbook.core.design.component.CbWheelPicker
import cn.wj.android.cashbook.core.model.entity.DateSelectionEntity
import cn.wj.android.cashbook.core.model.enums.DateSelectionTypeEnum
import cn.wj.android.cashbook.core.ui.R
import java.time.LocalDate
import java.time.YearMonth
import java.util.Calendar

/**
 * 日期选择 Popup
 *
 * @param expanded 是否展开
 * @param onDismissRequest 关闭回调
 * @param currentSelection 当前选择
 * @param onDateSelected 日期选择回调
 */
@Composable
fun DateSelectionPopup(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    currentSelection: DateSelectionEntity,
    onDateSelected: (DateSelectionEntity) -> Unit,
) {
    val popupWidth = 300.dp

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
    ) {
        val tabTitles = listOf(
            stringResource(R.string.date_selection_by_day),
            stringResource(R.string.date_selection_by_month),
            stringResource(R.string.date_selection_by_year),
            stringResource(R.string.date_selection_date_range),
            stringResource(R.string.date_selection_all),
        )
        val types = DateSelectionTypeEnum.entries

        var selectedTabIndex by remember(currentSelection) {
            mutableIntStateOf(types.indexOf(currentSelection.type))
        }

        // 缓存各模式的选择状态
        var cachedDay by remember {
            mutableStateOf(
                (currentSelection as? DateSelectionEntity.ByDay)?.date ?: LocalDate.now(),
            )
        }
        var cachedMonth by remember {
            mutableStateOf(
                (currentSelection as? DateSelectionEntity.ByMonth)?.yearMonth ?: YearMonth.now(),
            )
        }
        var cachedYear by remember {
            mutableIntStateOf(
                (currentSelection as? DateSelectionEntity.ByYear)?.year ?: LocalDate.now().year,
            )
        }
        var cachedRangeFrom by remember {
            mutableStateOf(
                (currentSelection as? DateSelectionEntity.DateRange)?.from
                    ?: LocalDate.now().withDayOfMonth(1),
            )
        }
        var cachedRangeTo by remember {
            mutableStateOf(
                (currentSelection as? DateSelectionEntity.DateRange)?.to ?: LocalDate.now(),
            )
        }

        // 内容区域，使用 DropdownMenuItem 包裹并设置固定宽度，避免 intrinsic 测量崩溃
        DropdownMenuItem(
            text = {
                when (types[selectedTabIndex]) {
                    DateSelectionTypeEnum.BY_DAY -> {
                        DayPicker(
                            date = cachedDay,
                            onDateChanged = { newDate ->
                                cachedDay = newDate
                                onDateSelected(DateSelectionEntity.ByDay(newDate))
                            },
                        )
                    }

                    DateSelectionTypeEnum.BY_MONTH -> {
                        MonthPicker(
                            yearMonth = cachedMonth,
                            onYearMonthChanged = { newYearMonth ->
                                cachedMonth = newYearMonth
                                onDateSelected(DateSelectionEntity.ByMonth(newYearMonth))
                            },
                        )
                    }

                    DateSelectionTypeEnum.BY_YEAR -> {
                        YearPicker(
                            year = cachedYear,
                            onYearChanged = { newYear ->
                                cachedYear = newYear
                                onDateSelected(DateSelectionEntity.ByYear(newYear))
                            },
                        )
                    }

                    DateSelectionTypeEnum.DATE_RANGE -> {
                        DateRangePicker(
                            from = cachedRangeFrom,
                            to = cachedRangeTo,
                            onRangeChanged = { newFrom, newTo ->
                                cachedRangeFrom = newFrom
                                cachedRangeTo = newTo
                                onDateSelected(DateSelectionEntity.DateRange(newFrom, newTo))
                            },
                        )
                    }

                    DateSelectionTypeEnum.ALL -> {
                        AllContent()
                    }
                }
            },
            onClick = {},
            modifier = Modifier.width(popupWidth),
            contentPadding = PaddingValues(0.dp),
        )

        // 底部 Tab
        CbHorizontalDivider(modifier = Modifier.width(popupWidth))
        DropdownMenuItem(
            text = {
                CbTabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = MaterialTheme.colorScheme.surface,
                ) {
                    tabTitles.forEachIndexed { index, title ->
                        CbTab(
                            selected = selectedTabIndex == index,
                            onClick = {
                                selectedTabIndex = index
                                // 切换 Tab 时仅更新数据，不关闭弹窗
                                val selection = when (types[index]) {
                                    DateSelectionTypeEnum.BY_DAY -> DateSelectionEntity.ByDay(cachedDay)
                                    DateSelectionTypeEnum.BY_MONTH -> DateSelectionEntity.ByMonth(cachedMonth)
                                    DateSelectionTypeEnum.BY_YEAR -> DateSelectionEntity.ByYear(cachedYear)
                                    DateSelectionTypeEnum.DATE_RANGE -> DateSelectionEntity.DateRange(cachedRangeFrom, cachedRangeTo)
                                    DateSelectionTypeEnum.ALL -> DateSelectionEntity.All
                                }
                                onDateSelected(selection)
                            },
                            text = {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1,
                                )
                            },
                        )
                    }
                }
            },
            onClick = {},
            modifier = Modifier.width(popupWidth).height(48.dp),
            contentPadding = PaddingValues(0.dp),
        )
    }
}

/** 年份范围 */
private val yearRange: List<Int>
    get() {
        val currentYear = Calendar.getInstance()[Calendar.YEAR]
        return (2000..currentYear + 1).toList()
    }

/** 月份列表 */
private val monthRange: List<Int> = (1..12).toList()

/**
 * 按日选择器
 */
@Composable
private fun DayPicker(
    date: LocalDate,
    onDateChanged: (LocalDate) -> Unit,
) {
    val years = remember { yearRange }
    val yearItems = remember(years) { years.map { "${it}年" } }
    val monthItems = remember { monthRange.map { "${it}月" } }
    val yearIndex = remember(date) { years.indexOf(date.year).coerceAtLeast(0) }
    val monthIndex = remember(date) { date.monthValue - 1 }

    var selectedYear by remember(date) { mutableIntStateOf(date.year) }
    var selectedMonth by remember(date) { mutableIntStateOf(date.monthValue) }

    val daysInMonth by remember(selectedYear, selectedMonth) {
        derivedStateOf {
            YearMonth.of(selectedYear, selectedMonth).lengthOfMonth()
        }
    }
    val dayRange by remember(daysInMonth) {
        derivedStateOf { (1..daysInMonth).toList() }
    }
    val dayItems = remember(dayRange) { dayRange.map { "${it}日" } }
    val dayIndex = remember(date, daysInMonth) {
        (date.dayOfMonth - 1).coerceIn(0, daysInMonth - 1)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        CbWheelPicker(
            items = yearItems,
            selectedIndex = yearIndex,
            onItemSelected = { index ->
                selectedYear = years[index]
                val newDay = date.dayOfMonth.coerceAtMost(
                    YearMonth.of(years[index], selectedMonth).lengthOfMonth(),
                )
                onDateChanged(LocalDate.of(years[index], selectedMonth, newDay))
            },
            modifier = Modifier.weight(1f),
        )
        CbWheelPicker(
            items = monthItems,
            selectedIndex = monthIndex,
            onItemSelected = { index ->
                selectedMonth = monthRange[index]
                val newDay = date.dayOfMonth.coerceAtMost(
                    YearMonth.of(selectedYear, monthRange[index]).lengthOfMonth(),
                )
                onDateChanged(LocalDate.of(selectedYear, monthRange[index], newDay))
            },
            modifier = Modifier.weight(1f),
        )
        CbWheelPicker(
            items = dayItems,
            selectedIndex = dayIndex,
            onItemSelected = { index ->
                onDateChanged(LocalDate.of(selectedYear, selectedMonth, dayRange[index]))
            },
            modifier = Modifier.weight(1f),
        )
    }
}

/**
 * 按月选择器
 */
@Composable
private fun MonthPicker(
    yearMonth: YearMonth,
    onYearMonthChanged: (YearMonth) -> Unit,
) {
    val years = remember { yearRange }
    val yearItems = remember(years) { years.map { "${it}年" } }
    val monthItems = remember { monthRange.map { "${it}月" } }
    val yearIndex = remember(yearMonth) { years.indexOf(yearMonth.year).coerceAtLeast(0) }
    val monthIndex = remember(yearMonth) { yearMonth.monthValue - 1 }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        CbWheelPicker(
            items = yearItems,
            selectedIndex = yearIndex,
            onItemSelected = { index ->
                onYearMonthChanged(YearMonth.of(years[index], yearMonth.monthValue))
            },
            modifier = Modifier.weight(1f),
        )
        CbWheelPicker(
            items = monthItems,
            selectedIndex = monthIndex,
            onItemSelected = { index ->
                onYearMonthChanged(YearMonth.of(yearMonth.year, monthRange[index]))
            },
            modifier = Modifier.weight(1f),
        )
    }
}

/**
 * 按年选择器
 */
@Composable
private fun YearPicker(
    year: Int,
    onYearChanged: (Int) -> Unit,
) {
    val years = remember { yearRange }
    val yearItems = remember(years) { years.map { "${it}年" } }
    val yearIndex = remember(year) { years.indexOf(year).coerceAtLeast(0) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 80.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        CbWheelPicker(
            items = yearItems,
            selectedIndex = yearIndex,
            onItemSelected = { index ->
                onYearChanged(years[index])
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * 时间范围选择
 */
@Composable
private fun DateRangePicker(
    from: LocalDate,
    to: LocalDate,
    onRangeChanged: (LocalDate, LocalDate) -> Unit,
) {
    var showFromPicker by remember { mutableStateOf(false) }
    var showToPicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        // 起始日期行
        DateRangeRow(
            label = stringResource(R.string.date_selection_start_date),
            dateText = "${from.year}-${from.monthValue.toString().padStart(2, '0')}-${from.dayOfMonth.toString().padStart(2, '0')}",
            onClick = { showFromPicker = true },
        )
        Spacer(modifier = Modifier.height(12.dp))
        // 结束日期行
        DateRangeRow(
            label = stringResource(R.string.date_selection_end_date),
            dateText = "${to.year}-${to.monthValue.toString().padStart(2, '0')}-${to.dayOfMonth.toString().padStart(2, '0')}",
            onClick = { showToPicker = true },
        )
    }

    if (showFromPicker) {
        DatePickerWheelDialog(
            title = stringResource(R.string.date_selection_select_start_date),
            currentDate = from,
            onDismiss = { showFromPicker = false },
            onConfirm = { newFrom ->
                showFromPicker = false
                val adjustedTo = if (newFrom.isAfter(to)) newFrom else to
                onRangeChanged(newFrom, adjustedTo)
            },
        )
    }

    if (showToPicker) {
        DatePickerWheelDialog(
            title = stringResource(R.string.date_selection_select_end_date),
            currentDate = to,
            onDismiss = { showToPicker = false },
            onConfirm = { newTo ->
                showToPicker = false
                val adjustedFrom = if (newTo.isBefore(from)) newTo else from
                onRangeChanged(adjustedFrom, newTo)
            },
        )
    }
}

/**
 * 日期范围行
 */
@Composable
private fun DateRangeRow(
    label: String,
    dateText: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.small,
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = dateText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

/**
 * 全部模式内容
 */
@Composable
private fun AllContent() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.date_selection_view_all),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * 日期选择滚轮弹窗（用于时间范围模式）
 */
@Composable
private fun DatePickerWheelDialog(
    title: String,
    currentDate: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit,
) {
    var selectedDate by remember { mutableStateOf(currentDate) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = AlertDialogDefaults.shape,
            color = AlertDialogDefaults.containerColor,
            tonalElevation = AlertDialogDefaults.TonalElevation,
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 16.dp),
                )

                val years = remember { yearRange }
                val yearItems = remember(years) { years.map { "${it}年" } }
                val monthItems = remember { monthRange.map { "${it}月" } }
                val yearIndex = remember(selectedDate) {
                    years.indexOf(selectedDate.year).coerceAtLeast(0)
                }
                val monthIndex = remember(selectedDate) { selectedDate.monthValue - 1 }
                val daysInMonth by remember(selectedDate.year, selectedDate.monthValue) {
                    derivedStateOf {
                        YearMonth.of(selectedDate.year, selectedDate.monthValue).lengthOfMonth()
                    }
                }
                val dayRange by remember(daysInMonth) {
                    derivedStateOf { (1..daysInMonth).toList() }
                }
                val dayItems = remember(dayRange) { dayRange.map { "${it}日" } }
                val dayIndex = remember(selectedDate, daysInMonth) {
                    (selectedDate.dayOfMonth - 1).coerceIn(0, daysInMonth - 1)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    CbWheelPicker(
                        items = yearItems,
                        selectedIndex = yearIndex,
                        onItemSelected = { index ->
                            val newDay = selectedDate.dayOfMonth.coerceAtMost(
                                YearMonth.of(years[index], selectedDate.monthValue).lengthOfMonth(),
                            )
                            selectedDate = LocalDate.of(years[index], selectedDate.monthValue, newDay)
                        },
                        modifier = Modifier.weight(1f),
                    )
                    CbWheelPicker(
                        items = monthItems,
                        selectedIndex = monthIndex,
                        onItemSelected = { index ->
                            val month = monthRange[index]
                            val newDay = selectedDate.dayOfMonth.coerceAtMost(
                                YearMonth.of(selectedDate.year, month).lengthOfMonth(),
                            )
                            selectedDate = LocalDate.of(selectedDate.year, month, newDay)
                        },
                        modifier = Modifier.weight(1f),
                    )
                    CbWheelPicker(
                        items = dayItems,
                        selectedIndex = dayIndex,
                        onItemSelected = { index ->
                            selectedDate = LocalDate.of(
                                selectedDate.year,
                                selectedDate.monthValue,
                                dayRange[index],
                            )
                        },
                        modifier = Modifier.weight(1f),
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(
                    onClick = { onConfirm(selectedDate) },
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text(text = stringResource(android.R.string.ok))
                }
            }
        }
    }
}
