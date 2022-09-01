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
        Flavor.values().forEach {

        }
        val text = "(4+5)-(4+5+(3-1)-(2+1))"
        println("result: ${CalculatorUtils.calculatorFromString(text)}")
        assertEquals(4, 2 + 2)
    }
}

enum class Dim {
    ContentType
}

enum class Flavor(val dim: Dim, val vname: String, val version: Int) {
    Online(Dim.ContentType,"online",1), Dev(Dim.ContentType,"dev",2)
}