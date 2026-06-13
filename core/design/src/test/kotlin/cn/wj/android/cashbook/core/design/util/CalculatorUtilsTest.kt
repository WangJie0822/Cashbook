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

package cn.wj.android.cashbook.core.design.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * CalculatorUtils 计算器引擎单元测试
 *
 * 覆盖：表达式求值（四则运算 + 运算符优先级 + 括号 + BigDecimal 精度 + 错误处理）
 * 与输入态机（数字 / 符号 / 小数点 / 括号 / 回退）。
 *
 * 注：× = U+00D7（乘）、÷ = U+00F7（除），与生产代码符号常量一致。
 */
class CalculatorUtilsTest {

    // ---------------- 计算引擎 onEqualsClick ----------------

    @Test
    fun onEqualsClick_addition() {
        assertThat(CalculatorUtils.onEqualsClick("1+1")).isEqualTo("2")
    }

    @Test
    fun onEqualsClick_subtraction() {
        assertThat(CalculatorUtils.onEqualsClick("5-3")).isEqualTo("2")
    }

    @Test
    fun onEqualsClick_multiplication() {
        assertThat(CalculatorUtils.onEqualsClick("6×7")).isEqualTo("42")
    }

    @Test
    fun onEqualsClick_division() {
        assertThat(CalculatorUtils.onEqualsClick("8÷2")).isEqualTo("4")
    }

    @Test
    fun onEqualsClick_respects_multiplication_precedence() {
        // 2+3×4：乘法优先 → 14（若按左到右顺序会误得 20）
        assertThat(CalculatorUtils.onEqualsClick("2+3×4")).isEqualTo("14")
    }

    @Test
    fun onEqualsClick_respects_division_precedence() {
        // 10-6÷2：除法优先 → 7（若按左到右顺序会误得 2）
        assertThat(CalculatorUtils.onEqualsClick("10-6÷2")).isEqualTo("7")
    }

    @Test
    fun onEqualsClick_brackets_override_precedence() {
        // (1+2)×3 → 9（无括号的 1+2×3 会得 7，用于区分括号是否生效）
        assertThat(CalculatorUtils.onEqualsClick("(1+2)×3")).isEqualTo("9")
    }

    @Test
    fun onEqualsClick_nested_brackets() {
        // ((1+2)×2)+1 → 7
        assertThat(CalculatorUtils.onEqualsClick("((1+2)×2)+1")).isEqualTo("7")
    }

    @Test
    fun onEqualsClick_division_rounds_to_two_decimals() {
        // 1÷3 = 0.3333... → 格式化 #.## 截断为 0.33
        assertThat(CalculatorUtils.onEqualsClick("1÷3")).isEqualTo("0.33")
    }

    @Test
    fun onEqualsClick_decimal_addition_is_precise() {
        // BigDecimal 精确运算：0.1+0.2 = 0.3（非二进制浮点的 0.30000000004）
        assertThat(CalculatorUtils.onEqualsClick("0.1+0.2")).isEqualTo("0.3")
    }

    @Test
    fun onEqualsClick_error_input_passthrough() {
        // 以错误标记 # 开头的文本原样返回，不再二次计算
        assertThat(CalculatorUtils.onEqualsClick("#格式错误")).isEqualTo("#格式错误")
    }

    @Test
    fun onEqualsClick_invalid_then_backPressed_restores_expression() {
        // 先消费可能残留的 history（object 单例跨用例共享状态）
        CalculatorUtils.onBackPressed("0")
        // 除零触发计算错误，结果以 # 开头
        val errorResult = CalculatorUtils.onEqualsClick("1÷0")
        assertThat(errorResult).startsWith("#")
        // onBackPressed 应从 history 恢复原算式（支持"错误结果退回为算式"）
        assertThat(CalculatorUtils.onBackPressed(errorResult)).isEqualTo("1÷0")
    }

    // ---------------- 输入态机 ----------------

    @Test
    fun needShowEqualSign_true_when_has_compute_sign() {
        assertThat(CalculatorUtils.needShowEqualSign("1+2")).isTrue()
    }

    @Test
    fun needShowEqualSign_false_for_plain_number() {
        assertThat(CalculatorUtils.needShowEqualSign("123")).isFalse()
    }

    @Test
    fun onNumberClick_replaces_leading_zero() {
        assertThat(CalculatorUtils.onNumberClick("0", "5")).isEqualTo("5")
    }

    @Test
    fun onNumberClick_inserts_multiply_after_closing_bracket() {
        // 以右括号结尾再输入数字，自动补乘号
        assertThat(CalculatorUtils.onNumberClick("(2)", "3")).isEqualTo("(2)×3")
    }

    @Test
    fun onComputeSignClick_replaces_trailing_sign() {
        // 末尾已是运算符时，新符号替换而非追加
        assertThat(CalculatorUtils.onComputeSignClick("1+", "-")).isEqualTo("1-")
    }

    @Test
    fun onBracketClick_inserts_multiply_when_balanced() {
        // 括号已配平且末尾是数字时，补乘号再开新括号
        assertThat(CalculatorUtils.onBracketClick("2")).isEqualTo("2×(")
    }

    @Test
    fun onPointClick_prevents_second_decimal_point() {
        // 当前数字已有小数点时再点不响应
        assertThat(CalculatorUtils.onPointClick("1.5")).isEqualTo("1.5")
    }
}
