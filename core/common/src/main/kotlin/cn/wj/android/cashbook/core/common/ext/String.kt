package cn.wj.android.cashbook.core.common.ext

import cn.wj.android.cashbook.core.common.Symbol

/** 给字符串添加上 rmb 符号 */
fun String.withSymbol(): String {
    val negative = this.startsWith("-")
    return if (negative) {
        "-${Symbol.rmb}${this.replace("-", "")}"
    } else {
        "${Symbol.rmb}$this"
    }
}