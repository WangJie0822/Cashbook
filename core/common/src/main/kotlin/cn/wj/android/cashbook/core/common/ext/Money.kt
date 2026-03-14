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

package cn.wj.android.cashbook.core.common.ext

import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.roundToLong

/**
 * 金额工具方法
 * 数据库及全链路使用 Long 类型（单位：分），仅在 UI 显示时转换
 */

/** Long(分) -> 显示字符串，始终两位小数: 1999L -> "19.99", -1999L -> "-19.99" */
fun Long.toMoneyString(): String {
    val negative = this < 0
    val absValue = if (this == Long.MIN_VALUE) Long.MAX_VALUE else if (negative) -this else this
    val yuan = absValue / 100
    val fen = absValue % 100
    val result = "$yuan.${fen.toString().padStart(2, '0')}"
    return if (negative) "-$result" else result
}

/** Long(分) -> 显示字符串，去除尾零: 2000L -> "20", 1990L -> "19.9", 1999L -> "19.99" */
fun Long.toMoneyFormat(): String {
    val str = toMoneyString()
    return when {
        str.endsWith("00") -> str.dropLast(3)
        str.endsWith("0") -> str.dropLast(1)
        else -> str
    }
}

/** Long(分) -> 带 CNY 符号: 1999L -> "¥19.99" */
fun Long.toMoneyCNY(): String = toMoneyString().withCNY()

/** 用户输入的金额字符串(元) -> Long(分): "19.99" -> 1999L */
fun String.toAmountCent(): Long {
    val bd = this.toBigDecimalOrNull() ?: return 0L
    return bd.multiply(BigDecimal(100)).setScale(0, RoundingMode.HALF_UP).toLong()
}

/** Double(元，兼容旧数据) -> Long(分): 19.99 -> 1999L */
fun Double.toCent(): Long = (this * 100).roundToLong()
