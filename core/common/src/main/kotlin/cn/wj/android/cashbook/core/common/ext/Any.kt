package cn.wj.android.cashbook.core.common.ext

import java.text.DecimalFormat

/** 对任意格式数字数据进行格式化并返回 [String] */
fun <T> T?.decimalFormat(pattern: String = "#.##"): String {
    return DecimalFormat(pattern).format(this?.toString()?.toBigDecimalOrNull() ?: return "")
}