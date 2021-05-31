package cn.wj.android.cashbook.widget.calculator

import cn.wj.android.cashbook.base.ext.base.logger
import java.math.BigDecimal
import java.util.regex.Pattern

/**
 * 计算器工具
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/5/31
 */
object CalculatorUtils {

    fun calculatorFromString(text: String): String {
        return try {
            calculatorCompatBracket(text)
        } catch (throwable: Throwable) {
            logger().e(throwable, "calculatorFromString")
            "${SYMBOL_ERROR}格式错误"
        }
    }

    fun calculatorCompatBracket(text: String): String {
        logger().d(text)
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
            logger().d("$text bracket: $bracket result: $bracketResult")
            return calculatorCompatBracket(text.replace(bracket, bracketResult))
        } else {
            // 没有括号，直接计算
            return calculator(text).toEngineeringString()
        }
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
                if (index == 0) {
                    result = calculator(s)
                } else {
                    result += calculator(s)
                }
            }
            return result
        }
        if (text.indexOf(SYMBOL_MINUS) > 0) {
            // 有减号，按照减号拆分
            var result = "0".toBigDecimal()
            text.split(SYMBOL_MINUS).forEachIndexed { index, s ->
                if (index == 0) {
                    result = calculator(s)
                } else {
                    result -= calculator(s)
                }
            }
            return result
        }
        if (text.indexOf(SYMBOL_TIMES) > 0) {
            // 有乘号，按照乘号拆分
            var result = "0".toBigDecimal()
            text.split(SYMBOL_TIMES).forEachIndexed { index, s ->
                if (index == 0) {
                    result = calculator(s)
                } else {
                    result *= calculator(s)
                }
            }
            return result
        }
        if (text.indexOf(SYMBOL_DIV) > 0) {
            // 有除号，按照除号拆分
            var result = "0".toBigDecimal()
            text.split(SYMBOL_DIV).forEachIndexed { index, s ->
                if (index == 0) {
                    result = calculator(s)
                } else {
                    result /= calculator(s)
                }
            }
            return result
        }
        return text.toBigDecimal()
    }


    private const val REGEX_COMPUTE_SIGN = ".*[+\\-×÷].*"

    fun hasComputeSign(text: String): Boolean {
        return Pattern.matches(REGEX_COMPUTE_SIGN, text)
    }

    private const val REGEX_SYMBOL = ".*[+\\-×÷()].*"


    fun hasSymbol(text: String): Boolean {
        return Pattern.matches(REGEX_SYMBOL, text)
    }

    private const val REGEX_BRACKET = ".*[()].*"


    fun hasBracket(text: String): Boolean {
        return Pattern.matches(REGEX_BRACKET, text)
    }

    private const val REGEX_NUMBER = ".*[0-9].*"

    fun hasNumber(text: String): Boolean {
        return Pattern.matches(REGEX_NUMBER, text)
    }
}