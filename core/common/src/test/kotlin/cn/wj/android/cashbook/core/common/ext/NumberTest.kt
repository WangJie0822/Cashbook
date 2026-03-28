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

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.math.BigDecimal

/**
 * Number.kt 数字转换工具方法单元测试
 */
class NumberTest {

    // ========== String?.toBigDecimalOrZero() ==========

    @Test
    fun when_null_string_toBigDecimalOrZero_then_returns_zero() {
        val input: String? = null
        assertThat(input.toBigDecimalOrZero()).isEqualTo(BigDecimal("0"))
    }

    @Test
    fun when_empty_string_toBigDecimalOrZero_then_returns_zero() {
        assertThat("".toBigDecimalOrZero()).isEqualTo(BigDecimal("0"))
    }

    @Test
    fun when_invalid_string_toBigDecimalOrZero_then_returns_zero() {
        assertThat("abc".toBigDecimalOrZero()).isEqualTo(BigDecimal("0"))
    }

    @Test
    fun when_valid_string_toBigDecimalOrZero_then_returns_value() {
        assertThat("19.99".toBigDecimalOrZero()).isEqualTo(BigDecimal("19.99"))
    }

    @Test
    fun when_negative_string_toBigDecimalOrZero_then_returns_value() {
        assertThat("-5.5".toBigDecimalOrZero()).isEqualTo(BigDecimal("-5.5"))
    }

    // ========== Number?.toBigDecimalOrZero() ==========

    @Test
    fun when_null_number_toBigDecimalOrZero_then_returns_zero() {
        val input: Number? = null
        assertThat(input.toBigDecimalOrZero()).isEqualTo(BigDecimal("0"))
    }

    @Test
    fun when_valid_number_toBigDecimalOrZero_then_returns_value() {
        assertThat((42 as Number).toBigDecimalOrZero()).isEqualTo(BigDecimal("42"))
    }

    @Test
    fun when_double_number_toBigDecimalOrZero_then_returns_value() {
        assertThat((3.14 as Number).toBigDecimalOrZero()).isEqualTo(BigDecimal("3.14"))
    }

    // ========== String?.toFloatOrZero() ==========

    @Test
    fun when_null_string_toFloatOrZero_then_returns_zero() {
        val input: String? = null
        assertThat(input.toFloatOrZero()).isEqualTo(0f)
    }

    @Test
    fun when_valid_string_toFloatOrZero_then_returns_value() {
        assertThat("3.14".toFloatOrZero()).isWithin(1e-4f).of(3.14f)
    }

    @Test
    fun when_invalid_string_toFloatOrZero_then_returns_zero() {
        assertThat("xyz".toFloatOrZero()).isEqualTo(0f)
    }

    // ========== String?.toDoubleOrZero() ==========

    @Test
    fun when_null_string_toDoubleOrZero_then_returns_zero() {
        val input: String? = null
        assertThat(input.toDoubleOrZero()).isEqualTo(0.0)
    }

    @Test
    fun when_valid_string_toDoubleOrZero_then_returns_value() {
        assertThat("19.99".toDoubleOrZero()).isWithin(1e-9).of(19.99)
    }

    @Test
    fun when_invalid_string_toDoubleOrZero_then_returns_zero() {
        assertThat("abc".toDoubleOrZero()).isEqualTo(0.0)
    }

    // ========== String?.toIntOrZero() ==========

    @Test
    fun when_null_string_toIntOrZero_then_returns_zero() {
        val input: String? = null
        assertThat(input.toIntOrZero()).isEqualTo(0)
    }

    @Test
    fun when_valid_string_toIntOrZero_then_returns_value() {
        assertThat("42".toIntOrZero()).isEqualTo(42)
    }

    @Test
    fun when_invalid_string_toIntOrZero_then_returns_zero() {
        assertThat("abc".toIntOrZero()).isEqualTo(0)
    }

    @Test
    fun when_float_string_toIntOrZero_then_returns_zero() {
        assertThat("3.14".toIntOrZero()).isEqualTo(0)
    }

    // ========== Int.completeZero() ==========

    @Test
    fun when_single_digit_completeZero_then_pads() {
        assertThat(5.completeZero()).isEqualTo("05")
    }

    @Test
    fun when_double_digit_completeZero_then_no_pad() {
        assertThat(10.completeZero()).isEqualTo("10")
    }

    @Test
    fun when_zero_completeZero_then_pads() {
        assertThat(0.completeZero()).isEqualTo("00")
    }

    @Test
    fun when_nine_completeZero_then_pads() {
        assertThat(9.completeZero()).isEqualTo("09")
    }

    @Test
    fun when_large_number_completeZero_then_no_pad() {
        assertThat(99.completeZero()).isEqualTo("99")
    }
}
