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

package cn.wj.android.cashbook.domain.usecase

import cn.wj.android.cashbook.core.common.annotation.CashbookDispatchers
import cn.wj.android.cashbook.core.common.annotation.Dispatcher
import cn.wj.android.cashbook.core.data.repository.RecordRepository
import cn.wj.android.cashbook.core.data.repository.TypeRepository
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.model.model.AssetMonthSummaryModel
import cn.wj.android.cashbook.core.model.model.analyticsPieNetAmount
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

/**
 * 计算指定类型/标签在 [[startDate], [endDate]) 的收入/支出/结余汇总。
 *
 * 净自付口径（[analyticsPieNetAmount]，对齐数据分析饼图，刻意区别于 [GetAssetMonthSummaryUseCase] 的 recordAmount）。
 * TRANSFER 记录不计入收支卡（金丝雀：必须在求和前 continue）。
 * 平账合成类型（-1101/-1102）按 type/tag 入口不可达，getRecordTypeCategories 未命中即跳过，无需兜底。
 *
 * > 创建于 2026/6/22
 */
class GetTypedMonthSummaryUseCase @Inject constructor(
    private val recordRepository: RecordRepository,
    private val typeRepository: TypeRepository,
    @Dispatcher(CashbookDispatchers.IO) private val coroutineContext: CoroutineContext,
) {

    suspend operator fun invoke(
        isType: Boolean,
        id: Long,
        startDate: Long,
        endDate: Long,
        includeChildTypes: Boolean,
    ): AssetMonthSummaryModel = withContext(coroutineContext) {
        if (id == -1L) return@withContext AssetMonthSummaryModel(0L, 0L, 0L)
        val records = if (isType) {
            recordRepository.queryRecordsByTypeIdInRange(id, startDate, endDate, includeChildTypes)
        } else {
            recordRepository.queryRecordsByTagIdInRange(id, startDate, endDate)
        }
        val categoryMap = typeRepository.getRecordTypeCategories(records.map { it.typeId })
        var income = 0L
        var expenditure = 0L
        for (record in records) {
            val category = categoryMap[record.typeId] ?: continue
            if (category == RecordTypeCategoryEnum.TRANSFER) continue
            val amount = analyticsPieNetAmount(
                typeCategory = category,
                finalAmount = record.finalAmount,
                amount = record.amount,
                charges = record.charges,
                concessions = record.concessions,
            )
            when (category) {
                RecordTypeCategoryEnum.INCOME -> income += amount
                RecordTypeCategoryEnum.EXPENDITURE -> expenditure += amount
                else -> Unit
            }
        }
        AssetMonthSummaryModel(income = income, expenditure = expenditure, balance = income - expenditure)
    }
}
