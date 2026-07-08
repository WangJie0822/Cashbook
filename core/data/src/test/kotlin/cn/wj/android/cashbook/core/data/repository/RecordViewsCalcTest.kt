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
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * [sumRelatedAmount] 与 [computeRelatedNature] 纯函数单测（从 RecordModelTransToViewsUseCase 迁出）。
 */
class RecordViewsCalcTest {

    private fun record(typeId: Long, amount: Long, charges: Long = 0L, concessions: Long = 0L) =
        RecordModel(
            id = 1L, booksId = 1L, typeId = typeId, assetId = -1L, relatedAssetId = -1L,
            amount = amount, finalAmount = amount, charges = charges, concessions = concessions,
            remark = "", reimbursable = false, recordTime = 0L, reimbursed = false,
        )

    @Test
    fun sumRelatedAmount_expenditure_sums_income_side() {
        // 主支出 → 关联收入口径 recordAmount(INCOME)=amount-charges
        val related = listOf(record(FIXED_TYPE_ID_REIMBURSE, amount = 1000L, charges = 100L))
        val sum = sumRelatedAmount(RecordTypeCategoryEnum.EXPENDITURE, related)
        assertThat(sum).isEqualTo(900L)
    }

    @Test
    fun sumRelatedAmount_transfer_returns_zero() {
        val sum = sumRelatedAmount(RecordTypeCategoryEnum.TRANSFER, listOf(record(1L, 500L)))
        assertThat(sum).isEqualTo(0L)
    }

    @Test
    fun computeRelatedNature_all_reimburse_is_reimbursed() {
        val related = listOf(record(FIXED_TYPE_ID_REIMBURSE, 1000L))
        val nature = computeRelatedNature(RecordTypeCategoryEnum.EXPENDITURE, related)
        assertThat(nature).isEqualTo(RecordRelatedNatureEnum.REIMBURSED)
    }

    @Test
    fun computeRelatedNature_mixed_is_mixed() {
        val related = listOf(
            record(FIXED_TYPE_ID_REIMBURSE, 1000L),
            record(FIXED_TYPE_ID_REFUND, 1000L),
        )
        val nature = computeRelatedNature(RecordTypeCategoryEnum.EXPENDITURE, related)
        assertThat(nature).isEqualTo(RecordRelatedNatureEnum.MIXED)
    }

    @Test
    fun computeRelatedNature_non_expenditure_is_none() {
        val nature = computeRelatedNature(RecordTypeCategoryEnum.INCOME, listOf(record(1L, 1000L)))
        assertThat(nature).isEqualTo(RecordRelatedNatureEnum.NONE)
    }
}
