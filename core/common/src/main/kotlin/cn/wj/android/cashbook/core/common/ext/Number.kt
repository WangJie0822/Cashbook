package cn.wj.android.cashbook.core.common.ext

import java.math.BigDecimal

fun String?.toBigDecimalOrZero(): BigDecimal {
    return this?.toBigDecimalOrNull() ?: "0".toBigDecimal()
}

fun Number?.toBigDecimalOrZero(): BigDecimal {
    return this?.toString().toBigDecimalOrZero()
}

fun String?.toDoubleOrZero(): Double {
    return this?.toDoubleOrNull() ?: 0.0
}

fun String?.toIntOrZero(): Int {
    return this?.toIntOrNull() ?: 0
}

fun Int.completeZero(): String {
    return if (this >= 10) {
        this.toString()
    } else {
        "0$this"
    }
}