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
import kotlin.test.assertTrue

/**
 * Money.kt 金额工具方法单元测试
 */
class MoneyTest {

    // ========== toMoneyString() ==========

    @Test
    fun when_zero_toMoneyString_then_returns_0_00() {
        assertEquals("0.00", 0L.toMoneyString())
    }

    @Test
    fun when_positive_toMoneyString_then_returns_formatted() {
        assertEquals("19.99", 1999L.toMoneyString())
    }

    @Test
    fun when_negative_toMoneyString_then_returns_negative_formatted() {
        assertEquals("-19.99", (-1999L).toMoneyString())
    }

    @Test
    fun when_1_cent_toMoneyString_then_returns_0_01() {
        assertEquals("0.01", 1L.toMoneyString())
    }

    @Test
    fun when_99_cents_toMoneyString_then_returns_0_99() {
        assertEquals("0.99", 99L.toMoneyString())
    }

    @Test
    fun when_100_cents_toMoneyString_then_returns_1_00() {
        assertEquals("1.00", 100L.toMoneyString())
    }

    @Test
    fun when_large_amount_toMoneyString_then_returns_formatted() {
        assertEquals("123456.78", 12345678L.toMoneyString())
    }

    @Test
    fun when_negative_1_cent_toMoneyString_then_returns_negative_0_01() {
        assertEquals("-0.01", (-1L).toMoneyString())
    }

    @Test
    fun when_long_min_value_toMoneyString_then_does_not_crash() {
        // Long.MIN_VALUE 的绝对值用 Long.MAX_VALUE 替代，验证不崩溃
        val result = Long.MIN_VALUE.toMoneyString()
        assertTrue(result.startsWith("-"))
    }

    @Test
    fun when_5_cents_toMoneyString_then_returns_padded_0_05() {
        assertEquals("0.05", 5L.toMoneyString())
    }

    // ========== toMoneyFormat() ==========

    @Test
    fun when_whole_yuan_toMoneyFormat_then_no_decimal() {
        assertEquals("20", 2000L.toMoneyFormat())
    }

    @Test
    fun when_one_decimal_toMoneyFormat_then_one_digit() {
        assertEquals("19.9", 1990L.toMoneyFormat())
    }

    @Test
    fun when_two_decimals_toMoneyFormat_then_two_digits() {
        assertEquals("19.99", 1999L.toMoneyFormat())
    }

    @Test
    fun when_zero_toMoneyFormat_then_returns_0() {
        assertEquals("0", 0L.toMoneyFormat())
    }

    @Test
    fun when_negative_whole_yuan_toMoneyFormat_then_no_decimal() {
        assertEquals("-20", (-2000L).toMoneyFormat())
    }

    @Test
    fun when_negative_one_decimal_toMoneyFormat_then_one_digit() {
        assertEquals("-19.9", (-1990L).toMoneyFormat())
    }

    @Test
    fun when_negative_two_decimals_toMoneyFormat_then_two_digits() {
        assertEquals("-19.99", (-1999L).toMoneyFormat())
    }

    @Test
    fun when_10_cents_toMoneyFormat_then_returns_0_1() {
        assertEquals("0.1", 10L.toMoneyFormat())
    }

    // ========== toMoneyCNY() ==========

    @Test
    fun when_positive_toMoneyCNY_then_has_cny_prefix() {
        assertEquals("¥19.99", 1999L.toMoneyCNY())
    }

    @Test
    fun when_zero_toMoneyCNY_then_has_cny_prefix() {
        assertEquals("¥0.00", 0L.toMoneyCNY())
    }

    @Test
    fun when_negative_toMoneyCNY_then_negative_before_cny() {
        assertEquals("-¥19.99", (-1999L).toMoneyCNY())
    }

    // ========== toAmountCent() ==========

    @Test
    fun when_valid_string_toAmountCent_then_returns_cents() {
        assertEquals(1999L, "19.99".toAmountCent())
    }

    @Test
    fun when_integer_string_toAmountCent_then_returns_cents() {
        assertEquals(2000L, "20".toAmountCent())
    }

    @Test
    fun when_zero_string_toAmountCent_then_returns_0() {
        assertEquals(0L, "0".toAmountCent())
    }

    @Test
    fun when_empty_string_toAmountCent_then_returns_0() {
        assertEquals(0L, "".toAmountCent())
    }

    @Test
    fun when_non_numeric_string_toAmountCent_then_returns_0() {
        assertEquals(0L, "abc".toAmountCent())
    }

    @Test
    fun when_negative_string_toAmountCent_then_returns_negative_cents() {
        assertEquals(-1999L, "-19.99".toAmountCent())
    }

    @Test
    fun when_one_decimal_string_toAmountCent_then_returns_correct_cents() {
        assertEquals(1990L, "19.9".toAmountCent())
    }

    @Test
    fun when_three_decimal_half_up_toAmountCent_then_rounds_correctly() {
        // 19.995 -> HALF_UP -> 2000
        assertEquals(2000L, "19.995".toAmountCent())
    }

    @Test
    fun when_three_decimal_round_down_toAmountCent_then_rounds_correctly() {
        // 19.994 -> HALF_UP -> 1999
        assertEquals(1999L, "19.994".toAmountCent())
    }

    @Test
    fun when_large_amount_string_toAmountCent_then_returns_correct_cents() {
        assertEquals(12345678L, "123456.78".toAmountCent())
    }

    @Test
    fun when_small_decimal_string_toAmountCent_then_returns_correct_cents() {
        assertEquals(1L, "0.01".toAmountCent())
    }

    // ========== toCent() ==========

    @Test
    fun when_positive_double_toCent_then_returns_cents() {
        assertEquals(1999L, 19.99.toCent())
    }

    @Test
    fun when_zero_double_toCent_then_returns_0() {
        assertEquals(0L, 0.0.toCent())
    }

    @Test
    fun when_negative_double_toCent_then_returns_negative_cents() {
        assertEquals(-1999L, (-19.99).toCent())
    }

    @Test
    fun when_whole_yuan_double_toCent_then_returns_cents() {
        assertEquals(2000L, 20.0.toCent())
    }

    @Test
    fun when_one_decimal_double_toCent_then_returns_correct_cents() {
        assertEquals(1990L, 19.9.toCent())
    }

    @Test
    fun when_small_double_toCent_then_returns_correct_cents() {
        assertEquals(1L, 0.01.toCent())
    }

    @Test
    fun when_large_double_toCent_then_returns_correct_cents() {
        assertEquals(12345678L, 123456.78.toCent())
    }

    // ========== 往返转换一致性 ==========

    @Test
    fun when_roundtrip_cent_to_string_to_cent_then_consistent() {
        val original = 1999L
        val str = original.toMoneyString()
        val restored = str.toAmountCent()
        assertEquals(original, restored)
    }

    @Test
    fun when_roundtrip_negative_cent_to_string_to_cent_then_consistent() {
        val original = -5050L
        val str = original.toMoneyString()
        val restored = str.toAmountCent()
        assertEquals(original, restored)
    }

    @Test
    fun when_roundtrip_zero_cent_to_string_to_cent_then_consistent() {
        val original = 0L
        val str = original.toMoneyString()
        val restored = str.toAmountCent()
        assertEquals(original, restored)
    }
}
