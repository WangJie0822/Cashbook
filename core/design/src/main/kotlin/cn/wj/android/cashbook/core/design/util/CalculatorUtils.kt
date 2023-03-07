package cn.wj.android.cashbook.core.design.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.util.regex.Pattern

/**
 * 计算器工具
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/5/31
 */
internal object CalculatorUtils {

    private const val SYMBOL_PLUS = "+"
    private const val SYMBOL_MINUS = "-"
    private const val SYMBOL_TIMES = "×"
    private const val SYMBOL_DIV = "÷"
    private const val SYMBOL_POINT = "."
    private const val SYMBOL_BRACKET_START = "("
    private const val SYMBOL_BRACKET_END = ")"

    private const val SYMBOL_ERROR = "#"

    private const val SYMBOL_ZERO = "0"

    fun needShowEqualSign(text: String): Boolean {
        // 有运算符或没有数字，需要显示等号
        return hasComputeSign(text) || !hasNumber(text)
    }

    /** 用于记录上次输入结果，支持结果退回为算式 */
    private var history = ""

    fun onBackPressed(text: String): String {
        var result = history
        if (result.isBlank()) {
            // 没有历史记录，移除最后一个字符，长度不够为 0
            result = if (text.length > 1) {
                text.dropLast(1)
            } else {
                SYMBOL_ZERO
            }
        } else {
            history = ""
        }
        return result
    }

    fun onComputeSignClick(text: String, sign: String): String {
        // 最后一个符号
        val last = text.last().toString()
        return if (hasComputeSign(last) || last == SYMBOL_POINT) {
            // 最后一个是计算符号或者小数点，直接替换
            text.dropLast(1) + sign
        } else if (last == SYMBOL_BRACKET_START) {
            // 最后一个是括号开始，不变
            text
        } else {
            // 其他情况，直接拼接
            text + sign
        }
    }

    fun onNumberClick(text: String, number: String): String {
        return when {
            text == SYMBOL_ZERO -> {
                // 为 0 时直接替换值
                number
            }

            text.endsWith(SYMBOL_BRACKET_END) -> {
                // 以括号结尾，添加乘号
                text + SYMBOL_TIMES + number
            }

            else -> {
                // 其他，直接拼接
                text + number
            }
        }
    }

    fun onPointClick(text: String): String {
        val last = text.last().toString()
        return if (hasComputeSign(last) || last == SYMBOL_BRACKET_START) {
            // 是计算符号或括号开始，添加 0
            text + SYMBOL_ZERO + SYMBOL_POINT
        } else if (last == SYMBOL_BRACKET_END || last == SYMBOL_POINT) {
            // 是括号结束或小数点，不响应
            text
        } else {
            val pattern = Pattern.compile(REGEX_SYMBOL)
            val matcher = pattern.matcher(text)
            var lastSymbol = ""
            while (matcher.find()) {
                lastSymbol = matcher.group()
            }
            if (lastSymbol.isBlank() || lastSymbol != SYMBOL_POINT) {
                // 当前数字没有小数点
                text + SYMBOL_POINT
            } else {
                // 当前数字已有小数点，不响应
                text
            }
        }
    }

    fun onBracketClick(text: String): String {
        val last = text.last().toString()
        val startCount = text.count { it.toString() == SYMBOL_BRACKET_START }
        val endCount = text.count { it.toString() == SYMBOL_BRACKET_END }
        return if (text == SYMBOL_ZERO) {
            // 默认 0，替换为括号
            SYMBOL_BRACKET_START
        } else if (last == SYMBOL_BRACKET_START || hasComputeSign(last)) {
            // 是括号开始或是计算符号，继续添加括号开始
            text + SYMBOL_BRACKET_START
        } else if (last == SYMBOL_POINT) {
            // 小数点
            if (startCount == endCount) {
                // 括号已完成匹配，将小数点替换为乘号并添加括号
                text.dropLast(1) + SYMBOL_TIMES + SYMBOL_BRACKET_START
            } else {
                // 括号不匹配，移除小数点并添加括号
                text.dropLast(1) + SYMBOL_BRACKET_END
            }
        } else {
            // 其他情况
            if (startCount == endCount) {
                // 括号已完成匹配，添加乘号及括号
                text + SYMBOL_TIMES + SYMBOL_BRACKET_START
            } else {
                // 括号未完成匹配，添加括号结束
                text + SYMBOL_BRACKET_END
            }
        }
    }

    fun onEqualsClick(text: String): String {
        val result = calculatorFromString(text)
        return if (result.startsWith(SYMBOL_ERROR)) {
            if (history.isBlank()) {
                history = text
            }
            result.replace(SYMBOL_ERROR, "")
        } else {
            result
        }
    }

    private fun calculatorFromString(text: String): String {
        return try {
            calculatorCompatBracket(text)
        } catch (throwable: Throwable) {
//            logger().e(throwable, "calculatorFromString")
            "${SYMBOL_ERROR}格式错误"
        }
    }

    private fun calculatorCompatBracket(text: String): String {
//        logger().d(text)
        if (hasBracket(text)) {
            // 有括号
            val firstEndIndex = text.indexOfFirst {
                it.toString() == SYMBOL_BRACKET_END
            }
            // 获取在此之前最后一个左括号
            val lastStartIndex = text.substring(0, firstEndIndex + 1).indexOfLast {
                it.toString() == SYMBOL_BRACKET_START
            }
            val bracket = text.substring(lastStartIndex, firstEndIndex + 1)
            val bracketResult = calculatorCompatBracket(bracket.dropLast(1).drop(1))
//            logger().d("$text bracket: $bracket result: $bracketResult")
            return calculatorCompatBracket(text.replace(bracket, bracketResult))
        } else {
            // 没有括号，直接计算
            return calculator(text).decimalFormat()
        }
    }

    /** 对任意格式数字数据进行格式化并返回 [String] */
    private fun <T> T?.decimalFormat(pattern: String = "#.##"): String {
        return DecimalFormat(pattern).format(this?.toString()?.toBigDecimalOrNull() ?: return "")
    }

    private fun calculator(text: String): BigDecimal {
        if (!hasComputeSign(text)) {
            // 没有符号，直接返回
            return text.toBigDecimal()
        }
        if (text.indexOf(SYMBOL_PLUS) > 0) {
            // 有加号，按照加号拆分
            var result = "0".toBigDecimal()
            text.split(SYMBOL_PLUS).forEachIndexed { index, s ->
                result = if (index == 0) {
                    calculator(s)
                } else {
                    result.add(calculator(s))
                }
            }
            return result
        }
        if (text.indexOf(SYMBOL_MINUS) > 0) {
            // 有减号，按照减号拆分
            var result = "0".toBigDecimal()
            text.split(SYMBOL_MINUS).forEachIndexed { index, s ->
                result = if (index == 0) {
                    calculator(s)
                } else {
                    result.minus(calculator(s))
                }
            }
            return result
        }
        if (text.indexOf(SYMBOL_TIMES) > 0) {
            // 有乘号，按照乘号拆分
            var result = "0".toBigDecimal()
            text.split(SYMBOL_TIMES).forEachIndexed { index, s ->
                result = if (index == 0) {
                    calculator(s)
                } else {
                    result.times(calculator(s))
                }
            }
            return result
        }
        if (text.indexOf(SYMBOL_DIV) > 0) {
            // 有除号，按照除号拆分
            var result = "0".toBigDecimal()
            text.split(SYMBOL_DIV).forEachIndexed { index, s ->
                result = if (index == 0) {
                    calculator(s)
                } else {
                    result.divide(calculator(s), 10, RoundingMode.HALF_EVEN)
                }
            }
            return result
        }
        return text.toBigDecimal()
    }


    private const val REGEX_COMPUTE_SIGN = ".*[+\\-×÷].*"

    private fun hasComputeSign(text: String): Boolean {
        return Pattern.matches(REGEX_COMPUTE_SIGN, text)
    }

    private const val REGEX_SYMBOL = "[+\\-×÷().]"


    private const val REGEX_BRACKET = ".*[()].*"

    private fun hasBracket(text: String): Boolean {
        return Pattern.matches(REGEX_BRACKET, text)
    }

    private const val REGEX_NUMBER = ".*[0-9].*"

    private fun hasNumber(text: String): Boolean {
        return Pattern.matches(REGEX_NUMBER, text)
    }
}