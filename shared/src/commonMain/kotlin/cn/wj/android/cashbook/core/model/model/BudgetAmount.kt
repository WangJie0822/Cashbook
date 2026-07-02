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

package cn.wj.android.cashbook.core.model.model

/** 预算限额上界：999 万元（分） */
const val BUDGET_AMOUNT_MAX_CENT = 999999_00L

/** 整数部分位数上界：超过该位数必超上界，提前拒绝以避免 Long 乘 100 溢出。 */
private const val MAX_INT_PART_DIGITS = 16

/**
 * 解析用户输入的限额（元）为分；非法返回 null。
 *
 * 拒绝：非数字 / ≤0 / 超上界。纯 Long/字符串算术实现（KMP commonMain 无 java.math.BigDecimal）：
 * 整数部分位数提前限界避免 Long 溢出；小数部分四舍五入到分——只需看第 3 位小数（0-4 舍/5-9 入），
 * 因为舍入余数的量级完全由该位决定，与其后数字无关，等价于 BigDecimal HALF_UP。
 *
 * 数字判定仅接受 ASCII `0`..`9`：科学计数法（`1e3`）、下划线（`1_000`）、Unicode 数字（如阿拉伯数字 `٣`、
 * 全角 `３`）均被拒（返回 null）。与原 `String.toBigDecimalOrNull()` 的 ASCII-only 正则筛保持一致，
 * 且不依赖各 KMP 平台的 `Char.isDigit()` Unicode 表（`isDigit` 对 Unicode 数字返回 true，会引入平台分歧）。
 * 整数位数按字符数计长（前导零计入），故 17+ 位前导零串即便去零后是合法小值也被拒。
 */
fun parseBudgetAmountCent(input: String): Long? {
    val trimmed = input.trim()
    if (trimmed.isEmpty()) return null

    var body = trimmed
    var negative = false
    if (body.startsWith("+")) {
        body = body.substring(1)
    } else if (body.startsWith("-")) {
        negative = true
        body = body.substring(1)
    }
    if (body.isEmpty()) return null

    val dotIndex = body.indexOf('.')
    val intPart: String
    val fracPart: String
    if (dotIndex < 0) {
        intPart = body
        fracPart = ""
    } else {
        if (body.indexOf('.', dotIndex + 1) >= 0) return null
        intPart = body.substring(0, dotIndex)
        fracPart = body.substring(dotIndex + 1)
    }

    if (intPart.isEmpty() && fracPart.isEmpty()) return null
    if (intPart.isNotEmpty() && !intPart.all { it in '0'..'9' }) return null
    if (fracPart.isNotEmpty() && !fracPart.all { it in '0'..'9' }) return null

    // 整数位数过长必超上界，提前拒绝避免 *100 时 Long 溢出回绕
    if (intPart.length > MAX_INT_PART_DIGITS) return null

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

    if (negative) cent = -cent

    if (cent < 1L || cent > BUDGET_AMOUNT_MAX_CENT) return null
    return cent
}
