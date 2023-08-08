package cn.wj.android.cashbook.core.common.ext

import java.time.LocalDate
import java.time.YearMonth

val LocalDate.yearMonth: YearMonth
    get() = YearMonth.of(year, month)