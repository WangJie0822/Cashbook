@file:Suppress("unused")
@file:JvmName("BigDecimalExt")

package cn.wj.android.cashbook.base.ext.base

import cn.wj.android.cashbook.widget.calculator.SYMBOL_ZERO
import java.math.BigDecimal
import java.text.DecimalFormat

fun BigDecimal.formatToNumber(): String {
    val df = DecimalFormat("#.##")
    return if (compareTo(BigDecimal.ZERO) == 0) {
        SYMBOL_ZERO
    } else if (compareTo(BigDecimal.ZERO) > 0 && compareTo(BigDecimal(1)) < 0) {
        SYMBOL_ZERO + df.format(this).toString()
    } else {
        df.format(this).toString()
    }
}