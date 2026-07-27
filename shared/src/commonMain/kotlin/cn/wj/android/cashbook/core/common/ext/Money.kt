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
 * 用户输入的金额字符串(元) -> Long(分)，非法输入返回 null: "19.99" -> 1999L，"1e3"/"abc" -> null。
 *
 * null 语义供调用方区分「非法输入」与「合法的 0 元」——搜索金额匹配以此回落哨兵而非按 0 元误匹配；
 * 写库路径可据此拒绝并提示而非静默存 0。文法契约与实现见 [parseDecimalCentOrNull]（金额解析单一真源，
 * 与 [parseBudgetAmountCent][cn.wj.android.cashbook.core.model.model.parseBudgetAmountCent] 同源）。
 */
fun String.toAmountCentOrNull(): Long? = parseDecimalCentOrNull(this)

/**
 * 用户输入的金额字符串(元) -> Long(分): "19.99" -> 1999L；非法输入返回 0L（兼容旧语义的薄委托，
 * 调用方需感知非法输入时用 [toAmountCentOrNull]）。文法契约见 [parseDecimalCentOrNull]。
 */
fun String.toAmountCent(): Long = toAmountCentOrNull() ?: 0L

/** Double(元，兼容旧数据) -> Long(分): 19.99 -> 1999L */
fun Double.toCent(): Long = (this * 100).roundToLong()
