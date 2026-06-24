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

package cn.wj.android.cashbook.feature.records.view

import cn.wj.android.cashbook.core.model.entity.RecordViewsEntity
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum

/**
 * 支出记录「未关联」时的报销显示态（待报销列表 / 详情弹窗 / 主列表共用，防两处逻辑漂移）。
 *
 * 已关联（relatedRecord 非空）的记录走各处既有 relatedNature 标签，此处返回 [NONE]。
 */
internal enum class ReimbursementDisplayStatus { MARKED_REIMBURSED, PENDING, NONE }

/**
 * 计算支出记录的「未关联」报销显示态：
 * - 非支出 / 已关联 / 不可报销 → [ReimbursementDisplayStatus.NONE]
 * - 可报销 + 未关联 + 已手动标记 → [ReimbursementDisplayStatus.MARKED_REIMBURSED]（显示「已报销」、可改回）
 * - 可报销 + 未关联 + 未标记 → [ReimbursementDisplayStatus.PENDING]（显示「待报销」、可标记）
 */
internal fun RecordViewsEntity.reimbursementDisplayStatus(): ReimbursementDisplayStatus = when {
    typeCategory != RecordTypeCategoryEnum.EXPENDITURE -> ReimbursementDisplayStatus.NONE
    relatedRecord.isNotEmpty() -> ReimbursementDisplayStatus.NONE
    !reimbursable -> ReimbursementDisplayStatus.NONE
    reimbursed -> ReimbursementDisplayStatus.MARKED_REIMBURSED
    else -> ReimbursementDisplayStatus.PENDING
}
