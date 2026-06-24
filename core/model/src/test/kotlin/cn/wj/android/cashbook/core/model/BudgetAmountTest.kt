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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

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
}
