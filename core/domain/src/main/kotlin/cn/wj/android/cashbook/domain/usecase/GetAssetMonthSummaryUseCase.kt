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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

/**
 * 计算某资产在指定月份范围的收支结余（资产余额口径）。
 * 结余 = 收入 − 支出 = 该资产当月余额净变化（对齐 TransactionDao.verifyAssetBalance 方向规则）。
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2026/5/29
 */
class GetAssetMonthSummaryUseCase @Inject constructor(
    private val recordRepository: RecordRepository,
    private val typeRepository: TypeRepository,
    @Dispatcher(CashbookDispatchers.IO) private val coroutineContext: CoroutineContext,
) {

    suspend operator fun invoke(
        assetId: Long,
        isCreditCard: Boolean,
        startDate: Long,
        endDate: Long,
    ): AssetMonthSummaryModel = withContext(coroutineContext) {
        if (assetId == -1L) return@withContext AssetMonthSummaryModel(0L, 0L, 0L)
        val records = recordRepository.queryAssetRecordsBetweenDateFlow(assetId, startDate, endDate).first()
        var income = 0L
        var expenditure = 0L
        for (record in records) {
            val category = typeRepository.getRecordTypeById(record.typeId)?.typeCategory ?: continue
            // 复用 calculateRecordAmount 公式：收入=amount-charges；其余=amount+charges-concessions
            val recordAmount = if (category == RecordTypeCategoryEnum.INCOME) {
                record.amount - record.charges
            } else {
                record.amount + record.charges - record.concessions
            }
            var delta = 0L
            // 本资产作为源资产
            if (record.assetId == assetId) {
                delta += if (isCreditCard) {
                    if (category == RecordTypeCategoryEnum.INCOME) -recordAmount else recordAmount
                } else {
                    if (category == RecordTypeCategoryEnum.INCOME) recordAmount else -recordAmount
                }
            }
            // 本资产作为转账目标
            if (record.relatedAssetId == assetId && category == RecordTypeCategoryEnum.TRANSFER) {
                delta += if (isCreditCard) -record.amount else record.amount
            }
            if (delta >= 0) income += delta else expenditure += -delta
        }
        AssetMonthSummaryModel(income = income, expenditure = expenditure, balance = income - expenditure)
    }
}
