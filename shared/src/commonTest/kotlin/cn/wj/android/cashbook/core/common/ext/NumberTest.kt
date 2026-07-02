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

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Number.kt 数字转换工具方法单元测试（纯 Kotlin 部分）。
 *
 * `toBigDecimalOrZero` 的测试保留在 `core:common`（该函数留在 Android 侧，见 Number.kt 说明）。
 */
class NumberTest {

    // ========== String?.toFloatOrZero() ==========

    @Test
    fun when_null_string_toFloatOrZero_then_returns_zero() {
        val input: String? = null
        assertEquals(0f, input.toFloatOrZero())
    }

    @Test
    fun when_valid_string_toFloatOrZero_then_returns_value() {
        assertEquals(3.14f, "3.14".toFloatOrZero(), 1e-4f)
    }

    @Test
    fun when_invalid_string_toFloatOrZero_then_returns_zero() {
        assertEquals(0f, "xyz".toFloatOrZero())
    }

    // ========== String?.toDoubleOrZero() ==========

    @Test
    fun when_null_string_toDoubleOrZero_then_returns_zero() {
        val input: String? = null
        assertEquals(0.0, input.toDoubleOrZero())
    }

    @Test
    fun when_valid_string_toDoubleOrZero_then_returns_value() {
        assertEquals(19.99, "19.99".toDoubleOrZero(), 1e-9)
    }

    @Test
    fun when_invalid_string_toDoubleOrZero_then_returns_zero() {
        assertEquals(0.0, "abc".toDoubleOrZero())
    }

    // ========== String?.toIntOrZero() ==========

    @Test
    fun when_null_string_toIntOrZero_then_returns_zero() {
        val input: String? = null
        assertEquals(0, input.toIntOrZero())
    }

    @Test
    fun when_valid_string_toIntOrZero_then_returns_value() {
        assertEquals(42, "42".toIntOrZero())
    }

    @Test
    fun when_invalid_string_toIntOrZero_then_returns_zero() {
        assertEquals(0, "abc".toIntOrZero())
    }

    @Test
    fun when_float_string_toIntOrZero_then_returns_zero() {
        assertEquals(0, "3.14".toIntOrZero())
    }

    // ========== Int.completeZero() ==========

    @Test
    fun when_single_digit_completeZero_then_pads() {
        assertEquals("05", 5.completeZero())
    }

    @Test
    fun when_double_digit_completeZero_then_no_pad() {
        assertEquals("10", 10.completeZero())
    }

    @Test
    fun when_zero_completeZero_then_pads() {
        assertEquals("00", 0.completeZero())
    }

    @Test
    fun when_nine_completeZero_then_pads() {
        assertEquals("09", 9.completeZero())
    }

    @Test
    fun when_large_number_completeZero_then_no_pad() {
        assertEquals("99", 99.completeZero())
    }
}
