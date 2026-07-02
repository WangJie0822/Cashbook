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
 * BigDecimalExt.kt 数字转换工具方法单元测试（BigDecimal 部分）。
 *
 * 纯 Kotlin 部分（toFloat/Double/IntOrZero、completeZero）的测试已随实现迁至 shared/commonTest 的 NumberTest。
 */
class BigDecimalExtTest {

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
}
