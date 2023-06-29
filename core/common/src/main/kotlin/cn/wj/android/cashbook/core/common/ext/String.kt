package cn.wj.android.cashbook.core.common.ext

import cn.wj.android.cashbook.core.common.Symbol

/** 给字符串添加上 CNY 符号 */
fun String.withCNY(): String {
    if (this.contains(Symbol.CNY)) {
        return this
    }
    val negative = this.startsWith("-")
    return if (negative) {
        "-${Symbol.CNY}${this.replace("-", "")}"
    } else {
        "${Symbol.CNY}$this"
    }
}