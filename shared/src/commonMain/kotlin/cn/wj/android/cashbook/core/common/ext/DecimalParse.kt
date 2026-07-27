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

/** 整数部分有效位数上界（前导零剥除后计长）：超过必致 `*100` 时 Long 溢出，提前拒绝。 */
internal const val MAX_AMOUNT_INT_PART_DIGITS = 16

/**
 * 十进制金额字符串(元) -> Long(分)，HALF_UP；非法返回 null。**金额解析单一真源**——
 * [toAmountCent]/[toAmountCentOrNull] 与 `parseBudgetAmountCent` 均委托本函数，规则修订只改此处。
 *
 * 纯 Long/字符串算术实现（KMP commonMain 无 java.math.BigDecimal）：小数四舍五入到分只需看第 3 位小数
 * （0-4 舍 / 5-9 入），舍入余数量级完全由该位决定、与其后数字无关，等价于 BigDecimal `HALF_UP`
 * （负数先按绝对值量级计算、末尾置符号，保持 HALF_UP 远离零方向）。
 *
 * 文法契约（相对旧 BigDecimal 实现的差异均为有意约定，由 MoneyTest 契约用例逐条固化）：
 * - 仅 ASCII `0`..`9`：科学计数法（`1e3`）、下划线、Unicode 数字（如全角 `３`）均非法——**收紧**于原
 *   `toBigDecimalOrNull()`（后者实测接受指数记法与 Unicode 十进制数字）；收紧是为不依赖各 KMP 平台
 *   `Char.isDigit()` 的 Unicode 表、并消除指数输入强制物化超长精度的放大面
 * - 首尾空白 trim 后接受——**放宽**于原实现（`BigDecimal(" 19.99 ")` 抛异常）
 * - 可选 `+`/`-` 前缀；单小数点；`.5`/`5.` 合法
 * - 整数部分**有效位数**（前导零剥除后）> [MAX_AMOUNT_INT_PART_DIGITS] 非法，防 Long 溢出回绕
 */
internal fun parseDecimalCentOrNull(input: String): Long? {
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

    // 限长防 *100 溢出：按有效数字计长（前导零不改变量级，剥除后再判）
    val significantIntPart = intPart.trimStart('0')
    if (significantIntPart.length > MAX_AMOUNT_INT_PART_DIGITS) return null

    val intValue = if (significantIntPart.isEmpty()) 0L else significantIntPart.toLong()
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
