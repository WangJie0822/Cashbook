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

/**
 * Money.kt 金额工具方法单元测试
 */
class MoneyTest {

    // ========== toMoneyString() ==========

    @Test
    fun when_zero_toMoneyString_then_returns_0_00() {
        assertThat(0L.toMoneyString()).isEqualTo("0.00")
    }

    @Test
    fun when_positive_toMoneyString_then_returns_formatted() {
        assertThat(1999L.toMoneyString()).isEqualTo("19.99")
    }

    @Test
    fun when_negative_toMoneyString_then_returns_negative_formatted() {
        assertThat((-1999L).toMoneyString()).isEqualTo("-19.99")
    }

    @Test
    fun when_1_cent_toMoneyString_then_returns_0_01() {
        assertThat(1L.toMoneyString()).isEqualTo("0.01")
    }

    @Test
    fun when_99_cents_toMoneyString_then_returns_0_99() {
        assertThat(99L.toMoneyString()).isEqualTo("0.99")
    }

    @Test
    fun when_100_cents_toMoneyString_then_returns_1_00() {
        assertThat(100L.toMoneyString()).isEqualTo("1.00")
    }

    @Test
    fun when_large_amount_toMoneyString_then_returns_formatted() {
        assertThat(12345678L.toMoneyString()).isEqualTo("123456.78")
    }

    @Test
    fun when_negative_1_cent_toMoneyString_then_returns_negative_0_01() {
        assertThat((-1L).toMoneyString()).isEqualTo("-0.01")
    }

    @Test
    fun when_long_min_value_toMoneyString_then_does_not_crash() {
        // Long.MIN_VALUE 的绝对值用 Long.MAX_VALUE 替代，验证不崩溃
        val result = Long.MIN_VALUE.toMoneyString()
        assertThat(result).startsWith("-")
    }

    @Test
    fun when_5_cents_toMoneyString_then_returns_padded_0_05() {
        assertThat(5L.toMoneyString()).isEqualTo("0.05")
    }

    // ========== toMoneyFormat() ==========

    @Test
    fun when_whole_yuan_toMoneyFormat_then_no_decimal() {
        assertThat(2000L.toMoneyFormat()).isEqualTo("20")
    }

    @Test
    fun when_one_decimal_toMoneyFormat_then_one_digit() {
        assertThat(1990L.toMoneyFormat()).isEqualTo("19.9")
    }

    @Test
    fun when_two_decimals_toMoneyFormat_then_two_digits() {
        assertThat(1999L.toMoneyFormat()).isEqualTo("19.99")
    }

    @Test
    fun when_zero_toMoneyFormat_then_returns_0() {
        assertThat(0L.toMoneyFormat()).isEqualTo("0")
    }

    @Test
    fun when_negative_whole_yuan_toMoneyFormat_then_no_decimal() {
        assertThat((-2000L).toMoneyFormat()).isEqualTo("-20")
    }

    @Test
    fun when_negative_one_decimal_toMoneyFormat_then_one_digit() {
        assertThat((-1990L).toMoneyFormat()).isEqualTo("-19.9")
    }

    @Test
    fun when_negative_two_decimals_toMoneyFormat_then_two_digits() {
        assertThat((-1999L).toMoneyFormat()).isEqualTo("-19.99")
    }

    @Test
    fun when_10_cents_toMoneyFormat_then_returns_0_1() {
        assertThat(10L.toMoneyFormat()).isEqualTo("0.1")
    }

    // ========== toMoneyCNY() ==========

    @Test
    fun when_positive_toMoneyCNY_then_has_cny_prefix() {
        assertThat(1999L.toMoneyCNY()).isEqualTo("¥19.99")
    }

    @Test
    fun when_zero_toMoneyCNY_then_has_cny_prefix() {
        assertThat(0L.toMoneyCNY()).isEqualTo("¥0.00")
    }

    @Test
    fun when_negative_toMoneyCNY_then_negative_before_cny() {
        assertThat((-1999L).toMoneyCNY()).isEqualTo("-¥19.99")
    }

    // ========== toAmountCent() ==========

    @Test
    fun when_valid_string_toAmountCent_then_returns_cents() {
        assertThat("19.99".toAmountCent()).isEqualTo(1999L)
    }

    @Test
    fun when_integer_string_toAmountCent_then_returns_cents() {
        assertThat("20".toAmountCent()).isEqualTo(2000L)
    }

    @Test
    fun when_zero_string_toAmountCent_then_returns_0() {
        assertThat("0".toAmountCent()).isEqualTo(0L)
    }

    @Test
    fun when_empty_string_toAmountCent_then_returns_0() {
        assertThat("".toAmountCent()).isEqualTo(0L)
    }

    @Test
    fun when_non_numeric_string_toAmountCent_then_returns_0() {
        assertThat("abc".toAmountCent()).isEqualTo(0L)
    }

    @Test
    fun when_negative_string_toAmountCent_then_returns_negative_cents() {
        assertThat("-19.99".toAmountCent()).isEqualTo(-1999L)
    }

    @Test
    fun when_one_decimal_string_toAmountCent_then_returns_correct_cents() {
        assertThat("19.9".toAmountCent()).isEqualTo(1990L)
    }

    @Test
    fun when_three_decimal_half_up_toAmountCent_then_rounds_correctly() {
        // 19.995 -> HALF_UP -> 2000
        assertThat("19.995".toAmountCent()).isEqualTo(2000L)
    }

    @Test
    fun when_three_decimal_round_down_toAmountCent_then_rounds_correctly() {
        // 19.994 -> HALF_UP -> 1999
        assertThat("19.994".toAmountCent()).isEqualTo(1999L)
    }

    @Test
    fun when_large_amount_string_toAmountCent_then_returns_correct_cents() {
        assertThat("123456.78".toAmountCent()).isEqualTo(12345678L)
    }

    @Test
    fun when_small_decimal_string_toAmountCent_then_returns_correct_cents() {
        assertThat("0.01".toAmountCent()).isEqualTo(1L)
    }

    // ========== toCent() ==========

    @Test
    fun when_positive_double_toCent_then_returns_cents() {
        assertThat(19.99.toCent()).isEqualTo(1999L)
    }

    @Test
    fun when_zero_double_toCent_then_returns_0() {
        assertThat(0.0.toCent()).isEqualTo(0L)
    }

    @Test
    fun when_negative_double_toCent_then_returns_negative_cents() {
        assertThat((-19.99).toCent()).isEqualTo(-1999L)
    }

    @Test
    fun when_whole_yuan_double_toCent_then_returns_cents() {
        assertThat(20.0.toCent()).isEqualTo(2000L)
    }

    @Test
    fun when_one_decimal_double_toCent_then_returns_correct_cents() {
        assertThat(19.9.toCent()).isEqualTo(1990L)
    }

    @Test
    fun when_small_double_toCent_then_returns_correct_cents() {
        assertThat(0.01.toCent()).isEqualTo(1L)
    }

    @Test
    fun when_large_double_toCent_then_returns_correct_cents() {
        assertThat(123456.78.toCent()).isEqualTo(12345678L)
    }

    // ========== 往返转换一致性 ==========

    @Test
    fun when_roundtrip_cent_to_string_to_cent_then_consistent() {
        val original = 1999L
        val str = original.toMoneyString()
        val restored = str.toAmountCent()
        assertThat(restored).isEqualTo(original)
    }

    @Test
    fun when_roundtrip_negative_cent_to_string_to_cent_then_consistent() {
        val original = -5050L
        val str = original.toMoneyString()
        val restored = str.toAmountCent()
        assertThat(restored).isEqualTo(original)
    }

    @Test
    fun when_roundtrip_zero_cent_to_string_to_cent_then_consistent() {
        val original = 0L
        val str = original.toMoneyString()
        val restored = str.toAmountCent()
        assertThat(restored).isEqualTo(original)
    }
}
