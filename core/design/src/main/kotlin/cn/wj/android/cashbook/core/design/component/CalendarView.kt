package cn.wj.android.cashbook.core.design.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cn.wj.android.cashbook.core.common.util.LunarUtils
import cn.wj.android.cashbook.core.design.theme.CashbookTheme
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Year
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.flow.collectLatest

/**
 * 日历视图
 *
 * @param onDateSelected 日期选择回调
 * @param modifier [Modifier] 对象
 * @param selectDate 选中日期，默认取当前日期
 * @param minYear 显示的最小年份
 * @param maxYear 显示的最大年份
 * @param weekStart 每周开始，取 [DayOfWeek]
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/8/7
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CalendarView(
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    selectDate: LocalDate = LocalDate.now(),
    minYear: Int = Year.now().value - 20,
    maxYear: Int = Year.now().value + 1,
    weekStart: DayOfWeek = DayOfWeek.SUNDAY,
    schemeContent: @Composable BoxScope.(date: LocalDate, selected: Boolean) -> Unit = { _, _ -> },
) {
    Column(modifier = modifier) {
        // 周视图
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
        ) {
            CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant) {
                for (w in weekStart.ordinal until weekStart.ordinal + 7) {
                    val week = DayOfWeek.of(w % 7 + 1)
                    Box(
                        modifier = Modifier
                            .defaultMinSize(minHeight = 36.dp)
                            .weight(1f),
                    ) {
                        Text(
                            text = week.getDisplayName(
                                TextStyle.SHORT,
                                Locale.getDefault()
                            ),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.align(Alignment.Center),
                        )
                    }

                }
            }
        }

        // 月视图
        val yearMonthList = generateYearMonthList(minYear, maxYear)
        var initialPage = yearMonthList.indexOfFirst {
            it.year == selectDate.year && it.monthValue == selectDate.monthValue
        }
        if (initialPage < 0) {
            initialPage = 0
        }
        val pagerState = rememberPagerState(
            initialPage = initialPage,
            initialPageOffsetFraction = 0f,
            pageCount = {
                yearMonthList.size
            })
        var scrollDate: YearMonth? by remember {
            mutableStateOf(YearMonth.of(selectDate.year, selectDate.monthValue))
        }

        LaunchedEffect(selectDate) {
            val current = yearMonthList[pagerState.currentPage]
            val selected = YearMonth.of(selectDate.year, selectDate.monthValue)
            if (current != selected) {
                val targetPage = yearMonthList.indexOfFirst {
                    it.year == selected.year && it.monthValue == selected.monthValue
                }
                if (targetPage >= 0) {
                    scrollDate = selected
                    pagerState.scrollToPage(targetPage)
                }
            }
        }

        LaunchedEffect(pagerState) {
            snapshotFlow { pagerState.currentPage }.collectLatest {
                yearMonthList.getOrNull(it)?.let { newMonth ->
                    if (newMonth != scrollDate) {
                        scrollDate = null
                        onDateSelected(newMonth.atDay(1))
                    }
                }
            }
        }

        HorizontalPager(
            modifier = Modifier.fillMaxWidth(),
            state = pagerState,
        ) {
            MonthView(
                yearMonth = yearMonthList[it],
                onDateSelected = onDateSelected,
                selectDate = selectDate,
                weekStart = weekStart,
                schemeContent = schemeContent,
            )
        }
    }
}

@Composable
internal fun MonthView(
    yearMonth: YearMonth,
    onDateSelected: (LocalDate) -> Unit,
    selectDate: LocalDate,
    modifier: Modifier = Modifier,
    weekStart: DayOfWeek = DayOfWeek.SUNDAY,
    schemeContent: @Composable BoxScope.(LocalDate, Boolean) -> Unit,
) {
    Column(modifier = modifier) {
        val nowDate = LocalDate.now()
        val startDay = yearMonth.atDay(1)
        val endDay = yearMonth.atEndOfMonth()
        val monthStartOffset = 6 - weekStart.ordinal + startDay.dayOfWeek.ordinal
        val monthLengthWithOffset = monthStartOffset + yearMonth.lengthOfMonth()
        val lineCount = monthLengthWithOffset / 7 + 1
        for (c in 0 until lineCount) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
            ) {
                for (r in 0 until 7) {
                    val currentDay = c * 7 + r - monthStartOffset
                    val isLastMonth = currentDay <= 0
                    val isNextMonth = currentDay > endDay.dayOfMonth
                    val currentDate = if (isLastMonth) {
                        startDay.plusDays(currentDay - 1L)
                    } else if (isNextMonth) {
                        endDay.plusDays((currentDay - endDay.dayOfMonth).toLong())
                    } else {
                        yearMonth.atDay(currentDay)
                    }
                    val selected = selectDate == currentDate
                    val isToday = nowDate == currentDate
                    val textColor = when {
                        isLastMonth || isNextMonth -> MaterialTheme.colorScheme.onSurface.copy(0.3f)
                        selected -> MaterialTheme.colorScheme.onPrimaryContainer
                        isToday -> MaterialTheme.colorScheme.secondary
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                    val textWeight = if (isToday) {
                        FontWeight.Bold
                    } else {
                        FontWeight.Normal
                    }
                    val dayModifier: Modifier = Modifier
                        .defaultMinSize(minHeight = 36.dp)
                        .weight(1f)
                        .align(Alignment.CenterVertically)
                        .clickable {
                            onDateSelected(currentDate)
                        }
                    val selectedDayModifier: Modifier = Modifier
                        .defaultMinSize(minHeight = 36.dp)
                        .weight(1f)
                        .align(Alignment.CenterVertically)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = ShapeDefaults.Small,
                        )
                    Box(
                        modifier = if (selected) selectedDayModifier else dayModifier,
                    ) {
                        Column(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = currentDate.dayOfMonth.toString(),
                                color = textColor,
                                fontWeight = textWeight,
                                style = MaterialTheme.typography.labelLarge,
                            )
                            Text(
                                text = LunarUtils.getLunarTextWithFestival(currentDate),
                                color = MaterialTheme.colorScheme.onSurface.copy(0.3f),
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }

                        schemeContent(currentDate, selected)
                    }
                }
            }
        }
    }
}

private fun generateYearMonthList(minYear: Int, maxYear: Int): List<YearMonth> {
    val yearMonthList = mutableListOf<YearMonth>()
    for (y in minYear..maxYear) {
        val year = Year.of(y)
        for (m in 1..12) {
            yearMonthList.add(year.atMonth(m))
        }
    }
    return yearMonthList
}

@Preview
@Composable
private fun MonthViewPreview() {
    CashbookTheme {
        CashbookGradientBackground {
            CalendarView(
                onDateSelected = {},
            )
        }
    }
}