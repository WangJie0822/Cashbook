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

package cn.wj.android.cashbook.core.model.entity

/** 总体预算 type_id 哨兵 */
const val BUDGET_TYPE_ID_TOTAL = -1L

/** 预算状态：正常 / 接近(80~100%) / 超支(>100%) */
enum class BudgetStateEnum { NORMAL, NEAR, OVER }

/**
 * 单条预算进度项
 *
 * @param typeId 一级分类 id；[BUDGET_TYPE_ID_TOTAL] 表总体
 * @param limit 限额（分）
 * @param spent 已花（分，净自付口径）
 * @param progress 进度 [0,1]，limit<=0 时为 null（不画进度条）
 * @param overAmount 超支额 = max(0, spent-limit)
 */
data class BudgetItem(
    val typeId: Long,
    val typeName: String,
    val typeIconName: String,
    val limit: Long,
    val spent: Long,
    val progress: Float?,
    val overAmount: Long,
    val state: BudgetStateEnum,
)

/**
 * 预算进度聚合
 *
 * @param overall 总体预算进度；未设总体预算时为 null
 * @param categoryList 各一级支出分类预算进度（仅已设预算的）
 */
data class BudgetProgressEntity(
    val overall: BudgetItem?,
    val categoryList: List<BudgetItem>,
)

/** 组装单条预算进度（纯函数，便于单测） */
internal fun buildBudgetItem(
    typeId: Long,
    typeName: String,
    typeIconName: String,
    limit: Long,
    spent: Long,
): BudgetItem {
    val ratio = if (limit > 0) spent.toFloat() / limit else 0f
    val progress = if (limit > 0) ratio.coerceAtMost(1f) else null
    val state = when {
        limit <= 0 -> BudgetStateEnum.NORMAL
        ratio > 1f -> BudgetStateEnum.OVER
        ratio >= 0.8f -> BudgetStateEnum.NEAR
        else -> BudgetStateEnum.NORMAL
    }
    val overAmount = if (limit > 0) (spent - limit).coerceAtLeast(0L) else 0L
    return BudgetItem(typeId, typeName, typeIconName, limit, spent, progress, overAmount, state)
}
