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

import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum

/**
 * 记录实际金额（DAO/月度结余口径，单位：分）。
 * - 收入：金额 - 手续费
 * - 支出 / 转账：金额 + 手续费 - 优惠
 *
 * 与 [analyticsPieAmount] 是两种不同口径：本函数 TRANSFER 归"非收入"分支（当支出），
 * Pie 口径 TRANSFER 归"非支出"分支（当收入），二者对 TRANSFER 处理相反，不可互换。
 */
fun recordAmount(
    category: RecordTypeCategoryEnum,
    amount: Long,
    charges: Long,
    concessions: Long,
): Long = if (category == RecordTypeCategoryEnum.INCOME) {
    amount - charges
} else {
    amount + charges - concessions
}

/**
 * Analytics 饼图金额口径（单位：分）。
 * - 支出：金额 + 手续费 - 优惠
 * - 收入 / 转账：金额 - 手续费
 *
 * 与 [recordAmount] 口径相反（见该函数注释）：本函数 TRANSFER 归"非支出"分支（当收入）。
 * 仅用于收支分类饼图统计。
 */
fun analyticsPieAmount(
    typeCategory: RecordTypeCategoryEnum,
    amount: Long,
    charges: Long,
    concessions: Long,
): Long = if (typeCategory == RecordTypeCategoryEnum.EXPENDITURE) {
    amount + charges - concessions
} else {
    amount - charges
}

/**
 * Analytics 饼图净自付口径（单位：分）。
 * - EXPENDITURE / INCOME：用净自付 [finalAmount]（被吸收支出显净自付、报销款溢出不虚增收入）
 * - TRANSFER：保持 [analyticsPieAmount] 口径（amount - charges），转账不参与吸收
 *
 * finalAmount 净自付重构后饼图分类占比改用本口径；TRANSFER 仍走旧口径守 #10b 金丝雀（两口径对 TRANSFER 不可统一）。
 */
fun analyticsPieNetAmount(
    typeCategory: RecordTypeCategoryEnum,
    finalAmount: Long,
    amount: Long,
    charges: Long,
    concessions: Long,
): Long = if (typeCategory == RecordTypeCategoryEnum.TRANSFER) {
    analyticsPieAmount(typeCategory, amount, charges, concessions)
} else {
    finalAmount
}
