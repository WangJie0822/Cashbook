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

import cn.wj.android.cashbook.core.model.entity.RecordViewsEntity
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum

/** 当前筛选的全量统计，金额单位为分。 */
data class RecordFilterSummary(val totalCount: Long, val income: Long, val expenditure: Long) {
    val balance: Long get() = income - expenditure
}

data class RecordFilterContribution(val income: Long, val expenditure: Long)

/** 与首页一致的净额口径，不能把转账本金或已对冲的报销实收额再次计入。 */
fun recordFilterContribution(record: RecordViewsEntity): RecordFilterContribution = when {
    record.isBalanceRecord -> RecordFilterContribution(0L, 0L)
    record.typeCategory == RecordTypeCategoryEnum.INCOME -> RecordFilterContribution(record.finalAmount, 0L)
    record.typeCategory == RecordTypeCategoryEnum.TRANSFER -> RecordFilterContribution(0L, record.charges - record.concessions)
    else -> RecordFilterContribution(0L, record.finalAmount)
}

/** 保持 RecordListItem 现有主金额，不改变共享列表默认显示。 */
fun recordFilterDisplayAmount(record: RecordViewsEntity): Long = when {
    record.typeCategory == RecordTypeCategoryEnum.TRANSFER -> record.amount
    record.typeCategory == RecordTypeCategoryEnum.INCOME && record.relatedRecord.isNotEmpty() -> record.amount
    else -> record.finalAmount
}

fun shouldShowFilterContribution(record: RecordViewsEntity): Boolean {
    val contribution = recordFilterContribution(record)
    val amount = if (record.typeCategory == RecordTypeCategoryEnum.INCOME) contribution.income else contribution.expenditure
    return recordFilterDisplayAmount(record) != amount
}
