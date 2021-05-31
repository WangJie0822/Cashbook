package cn.wj.android.cashbook

import cn.wj.android.cashbook.widget.calculator.CalculatorUtils
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        val text = "(4+5)-(4+5+(3-1)-(2+1))"
        println("result: ${CalculatorUtils.calculatorFromString(text)}")
        assertEquals(4, 2 + 2)
    }
}