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

package cn.wj.android.cashbook.core.data.repository

import cn.wj.android.cashbook.core.common.FIXED_TYPE_ID_REFUND
import cn.wj.android.cashbook.core.common.FIXED_TYPE_ID_REIMBURSE
import cn.wj.android.cashbook.core.model.enums.RecordRelatedNatureEnum
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.model.model.RecordModel
import cn.wj.android.cashbook.core.model.model.recordAmount

/**
 * 计算关联金额（单条 invoke / 批量 transBatch / 首页分页 mapper 三条转换路径共用，保证口径一致）。
 * 关联 category 由主 category 取反推断（零查询）：
 * - 主支出：关联收入，recordAmount(INCOME)=amount−charges
 * - 主收入：关联支出，recordAmount(EXPENDITURE)=amount+charges−concessions
 * - 其它（TRANSFER 等）：不累加
 *
 * 从 RecordModelTransToViewsUseCase 迁出（原 private），因 core:model 无 core:common 依赖故落在 core:data。
 */
fun sumRelatedAmount(
    typeCategory: RecordTypeCategoryEnum,
    relatedRecord: List<RecordModel>,
): Long {
    val relatedCategory = when (typeCategory) {
        RecordTypeCategoryEnum.EXPENDITURE -> RecordTypeCategoryEnum.INCOME
        RecordTypeCategoryEnum.INCOME -> RecordTypeCategoryEnum.EXPENDITURE
        else -> return 0L
    }
    return relatedRecord.sumOf { record ->
        recordAmount(relatedCategory, record.amount, record.charges, record.concessions)
    }
}

/**
 * 计算被吸收支出的关联性质（在已物化 relatedRecord 上判定，零查询）。
 * 仅 EXPENDITURE 主记录有性质；relatedRecord 为吸收它的收入（报销/退款款）。
 * 标准链路下关联收入 typeId 经 migrateSpecialTypes 为固定负 ID（REIMBURSE/REFUND）；
 * 若出现其它 typeId（未迁移/历史导入等），归 MIXED 兜底。
 *
 * 从 RecordModelTransToViewsUseCase 迁出（原 private）。
 */
fun computeRelatedNature(
    typeCategory: RecordTypeCategoryEnum,
    relatedRecord: List<RecordModel>,
): RecordRelatedNatureEnum {
    if (typeCategory != RecordTypeCategoryEnum.EXPENDITURE || relatedRecord.isEmpty()) {
        return RecordRelatedNatureEnum.NONE
    }
    val allReimburse = relatedRecord.all { it.typeId == FIXED_TYPE_ID_REIMBURSE }
    val allRefund = relatedRecord.all { it.typeId == FIXED_TYPE_ID_REFUND }
    return when {
        allReimburse -> RecordRelatedNatureEnum.REIMBURSED
        allRefund -> RecordRelatedNatureEnum.REFUNDED
        else -> RecordRelatedNatureEnum.MIXED
    }
}
