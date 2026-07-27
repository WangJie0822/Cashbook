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

import cn.wj.android.cashbook.core.common.ext.parseDecimalCentOrNull

/** 预算限额上界：999 万元（分） */
const val BUDGET_AMOUNT_MAX_CENT = 999999_00L

/**
 * 解析用户输入的限额（元）为分；非法返回 null。拒绝：非数字 / ≤0 / 超上界。
 *
 * 解析委托金额单一真源 [parseDecimalCentOrNull]（HALF_UP 到分、ASCII-only 拒科学计数法与 Unicode 数字
 * ——**收紧**于原 `toBigDecimalOrNull()`（后者实测接受两者），为保各 KMP 平台一致；有效整数位 >16 拒绝
 * 防 Long 溢出，前导零剥除后计长），本函数仅追加预算业务界：`(0, BUDGET_AMOUNT_MAX_CENT]`。
 */
fun parseBudgetAmountCent(input: String): Long? {
    val cent = parseDecimalCentOrNull(input) ?: return null
    if (cent < 1L || cent > BUDGET_AMOUNT_MAX_CENT) return null
    return cent
}
