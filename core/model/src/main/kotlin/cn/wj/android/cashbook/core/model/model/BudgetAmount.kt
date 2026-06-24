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

package cn.wj.android.cashbook.core.model.model

import java.math.BigDecimal
import java.math.RoundingMode

/** 预算限额上界：999 万元（分） */
const val BUDGET_AMOUNT_MAX_CENT = 999999_00L

/**
 * 解析用户输入的限额（元）为分；非法返回 null。
 *
 * 拒绝：非数字 / ≤0 / 超上界。用 BigDecimal 比较避免 toLong 溢出回绕。
 */
fun parseBudgetAmountCent(input: String): Long? {
    val bd = input.trim().toBigDecimalOrNull() ?: return null
    val cent = bd.multiply(BigDecimal(100)).setScale(0, RoundingMode.HALF_UP)
    if (cent < BigDecimal.ONE || cent > BigDecimal.valueOf(BUDGET_AMOUNT_MAX_CENT)) return null
    return cent.toLong()
}
