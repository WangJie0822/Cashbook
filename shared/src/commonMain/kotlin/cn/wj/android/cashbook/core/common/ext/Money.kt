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

/**
 * 用户输入的金额字符串(元) -> Long(分): "19.99" -> 1999L；非法输入返回 0L。
 *
 * 纯 Long/字符串算术实现（KMP commonMain 无 java.math.BigDecimal）：小数四舍五入到分只需看第 3 位小数
 * （0-4 舍 / 5-9 入），舍入余数量级完全由该位决定、与其后数字无关，等价于原 BigDecimal `HALF_UP`。
 * 数字判定仅接受 ASCII `0`..`9`：科学计数法（`1e3`）、下划线、Unicode 数字均视为非法（返回 0L），
 * 与 [parseBudgetAmountCent][cn.wj.android.cashbook.core.model.model.parseBudgetAmountCent] 的 ASCII-only
 * 约定一致，且不依赖各平台 `Char.isDigit()` 的 Unicode 表（会引入平台分歧）。整数位数超 16 位视为非法，
 * 避免 `*100` 时 Long 溢出回绕。
 */
fun String.toAmountCent(): Long {
    val trimmed = this.trim()
    if (trimmed.isEmpty()) return 0L

    var body = trimmed
    var negative = false
    if (body.startsWith("+")) {
        body = body.substring(1)
    } else if (body.startsWith("-")) {
        negative = true
        body = body.substring(1)
    }
    if (body.isEmpty()) return 0L

    val dotIndex = body.indexOf('.')
    val intPart: String
    val fracPart: String
    if (dotIndex < 0) {
        intPart = body
        fracPart = ""
    } else {
        if (body.indexOf('.', dotIndex + 1) >= 0) return 0L
        intPart = body.substring(0, dotIndex)
        fracPart = body.substring(dotIndex + 1)
    }

    if (intPart.isEmpty() && fracPart.isEmpty()) return 0L
    if (intPart.isNotEmpty() && !intPart.all { it in '0'..'9' }) return 0L
    if (fracPart.isNotEmpty() && !fracPart.all { it in '0'..'9' }) return 0L
    if (intPart.length > 16) return 0L

    val intValue = if (intPart.isEmpty()) 0L else intPart.toLong()
    var cent = intValue * 100
    if (fracPart.isNotEmpty()) {
        cent += if (fracPart.length <= 2) {
            fracPart.padEnd(2, '0').toLong()
        } else {
            val keep = fracPart.substring(0, 2).toLong()
            if (fracPart[2] >= '5') keep + 1 else keep
        }
    }
    return if (negative) -cent else cent
}

/** Double(元，兼容旧数据) -> Long(分): 19.99 -> 1999L */
fun Double.toCent(): Long = (this * 100).roundToLong()
