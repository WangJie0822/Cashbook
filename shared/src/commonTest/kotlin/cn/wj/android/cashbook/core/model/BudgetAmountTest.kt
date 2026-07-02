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

package cn.wj.android.cashbook.core.model

import cn.wj.android.cashbook.core.model.model.parseBudgetAmountCent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * [parseBudgetAmountCent] 限额校验纯函数测试
 */
class BudgetAmountTest {

    @Test fun valid() = assertEquals(199900L, parseBudgetAmountCent("1999"))

    @Test fun valid_decimal() = assertEquals(1999L, parseBudgetAmountCent("19.99"))

    @Test fun reject_zero() = assertNull(parseBudgetAmountCent("0"))

    @Test fun reject_negative() = assertNull(parseBudgetAmountCent("-50"))

    @Test fun reject_non_number() = assertNull(parseBudgetAmountCent("abc"))

    @Test fun reject_empty() = assertNull(parseBudgetAmountCent(""))

    @Test fun reject_overflow_huge() = assertNull(parseBudgetAmountCent("99999999999999999999"))

    @Test fun reject_above_upper_bound() = assertNull(parseBudgetAmountCent("10000000"))

    @Test fun accept_upper_bound() = assertEquals(99999900L, parseBudgetAmountCent("999999"))

    @Test fun reject_scientificNotation() {
        // KMP commonMain 纯 Long 实现不支持科学计数法（有意比 JVM BigDecimal 更严格拒绝）
        assertNull(parseBudgetAmountCent("1E5"))
        assertNull(parseBudgetAmountCent("1e5"))
    }

    /**
     * ≥3 位小数的 HALF_UP 四舍五入边界（触发 fracPart.length > 2 分支）。
     * ground-truth 与原 BigDecimal.setScale(2, HALF_UP) 逐例等价（0-4 舍 / 5-9 入，只看第 3 位小数）。
     */
    @Test fun round_halfUp_boundary() {
        assertEquals(1L, parseBudgetAmountCent("0.005")) // 恰 .5 → 上入
        assertNull(parseBudgetAmountCent("0.004")) // < .5 → 下舍到 0 → null
        assertEquals(2L, parseBudgetAmountCent("0.015")) // .5 → 上入
        assertEquals(2000L, parseBudgetAmountCent("19.995")) // 上入
        assertEquals(1999L, parseBudgetAmountCent("19.994")) // 下舍
        assertEquals(1000L, parseBudgetAmountCent("9.999")) // 小数进位入整数部分
    }

    /** 前导/末尾小数点边界（空整数部分 / 空小数部分路径）。 */
    @Test fun leading_or_trailing_dot() {
        assertEquals(50L, parseBudgetAmountCent(".5")) // 空整数部分
        assertEquals(500L, parseBudgetAmountCent("5.")) // 空小数部分
    }
}
