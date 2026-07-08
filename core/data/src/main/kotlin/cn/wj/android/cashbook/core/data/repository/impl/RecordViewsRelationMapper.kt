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

package cn.wj.android.cashbook.core.data.repository.impl

import cn.wj.android.cashbook.core.common.FIXED_TYPE_ID_REFUND
import cn.wj.android.cashbook.core.common.FIXED_TYPE_ID_REIMBURSE
import cn.wj.android.cashbook.core.common.SWITCH_INT_ON
import cn.wj.android.cashbook.core.data.repository.asModel
import cn.wj.android.cashbook.core.data.repository.computeRelatedNature
import cn.wj.android.cashbook.core.data.repository.sumRelatedAmount
import cn.wj.android.cashbook.core.database.relation.LauncherRecordViewRelation
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.model.model.RECORD_TYPE_BALANCE_EXPENDITURE
import cn.wj.android.cashbook.core.model.model.RECORD_TYPE_BALANCE_INCOME
import cn.wj.android.cashbook.core.model.model.RecordViewsModel

/**
 * 把 @Relation 分页视图 [LauncherRecordViewRelation] 组装为 [RecordViewsModel]，
 * 与单条 RecordModelTransToViewsUseCase.invoke 逐字段等价。
 * 所有关联已由 Room @Relation 批量物化，此处仅内存组装（零查询）。
 *
 * - type/asset 一对一取 firstOrNull；平账（types 空）按 typeId 映射合成类型；
 * - relatedRecord 按 typeCategory 选向：INCOME 用 relatedAsRecordId，其余用 relatedAsRelatedId（复刻单条版）；
 * - relatedAmount/relatedNature 复用 [sumRelatedAmount]/[computeRelatedNature]。
 */
internal fun LauncherRecordViewRelation.toRecordViewsModel(): RecordViewsModel {
    val typeTable = types.firstOrNull()
    val type = if (typeTable != null) {
        val id = typeTable.id ?: -1L
        typeTable.asModel(needRelated = id == FIXED_TYPE_ID_REFUND || id == FIXED_TYPE_ID_REIMBURSE)
    } else {
        // 平账合成类型（typeId 负、db_type 无匹配 → @Relation 空 List）
        when (record.typeId) {
            RECORD_TYPE_BALANCE_INCOME.id -> RECORD_TYPE_BALANCE_INCOME
            else -> RECORD_TYPE_BALANCE_EXPENDITURE
        }
    }
    val relatedTables = if (type.typeCategory == RecordTypeCategoryEnum.INCOME) {
        relatedAsRecordId
    } else {
        relatedAsRelatedId
    }
    val relatedRecords = relatedTables.map { it.asModel() }
    return RecordViewsModel(
        id = record.id ?: -1L,
        booksId = record.booksId,
        type = type,
        asset = assets.firstOrNull()?.asModel(),
        relatedAsset = intoAssets.firstOrNull()?.asModel(),
        amount = record.amount,
        finalAmount = record.finalAmount,
        charges = record.charge,
        concessions = record.concessions,
        remark = record.remark,
        reimbursable = record.reimbursable == SWITCH_INT_ON,
        relatedTags = tags.map { it.asModel() },
        relatedImage = images.map { it.asModel() },
        relatedRecord = relatedRecords,
        relatedAmount = sumRelatedAmount(type.typeCategory, relatedRecords),
        relatedNature = computeRelatedNature(type.typeCategory, relatedRecords),
        recordTime = record.recordTime,
        reimbursed = record.reimbursed == SWITCH_INT_ON,
    )
}
