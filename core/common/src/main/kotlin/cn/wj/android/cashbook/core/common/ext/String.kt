package cn.wj.android.cashbook.core.common.ext

import cn.wj.android.cashbook.core.common.Symbol

/** 给字符串添加上 CNY 符号 */
fun String.withCNY(): String {
    val source = if (this.contains(Symbol.CNY)) {
        this.replace(Symbol.CNY, "")
    } else {
        this
    }
    val negative = source.startsWith("-")
    return if (negative) {
        "-${Symbol.CNY}${source.replace("-", "")}"
    } else {
        "${Symbol.CNY}$source"
    }
}