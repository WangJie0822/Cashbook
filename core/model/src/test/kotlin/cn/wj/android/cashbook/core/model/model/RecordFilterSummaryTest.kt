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
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class RecordFilterSummaryTest {

    @Test
    fun balance_isIncomeMinusExpenditureIncludingNegativeResult() {
        assertThat(RecordFilterSummary(2L, 10000L, 20000L).balance).isEqualTo(-10000L)
    }

    @Test
    fun expenditure_usesNetAmountRatherThanOriginalAmount() {
        val item = record(amount = 100000L, finalAmount = 20000L)
        assertThat(recordFilterContribution(item)).isEqualTo(RecordFilterContribution(0L, 20000L))
        assertThat(recordFilterDisplayAmount(item)).isEqualTo(20000L)
        assertThat(shouldShowFilterContribution(item)).isFalse()
    }

    @Test
    fun transfer_excludesPrincipalAndShowsFeeContribution() {
        val item = record(category = RecordTypeCategoryEnum.TRANSFER, amount = 500000L, finalAmount = -200L, charges = 200L)
        assertThat(recordFilterContribution(item)).isEqualTo(RecordFilterContribution(0L, 200L))
        assertThat(recordFilterDisplayAmount(item)).isEqualTo(500000L)
        assertThat(shouldShowFilterContribution(item)).isTrue()
    }

    @Test
    fun transfer_concessionsCanProduceNegativeExpenditure() {
        val item = record(category = RecordTypeCategoryEnum.TRANSFER, amount = 500000L, charges = 200L, concessions = 300L)
        assertThat(recordFilterContribution(item)).isEqualTo(RecordFilterContribution(0L, -100L))
    }

    @Test
    fun relatedIncome_keepsReceivedAmountButContributesOnlyOverflow() {
        val item = record(category = RecordTypeCategoryEnum.INCOME, amount = 80000L, finalAmount = 0L, related = true)
        assertThat(recordFilterContribution(item)).isEqualTo(RecordFilterContribution(0L, 0L))
        assertThat(recordFilterDisplayAmount(item)).isEqualTo(80000L)
        assertThat(shouldShowFilterContribution(item)).isTrue()
    }

    @Test
    fun ordinaryIncome_displaysAndContributesNetAmount() {
        val item = record(category = RecordTypeCategoryEnum.INCOME, amount = 10000L, finalAmount = 9800L, charges = 200L)
        assertThat(recordFilterContribution(item)).isEqualTo(RecordFilterContribution(9800L, 0L))
        assertThat(recordFilterDisplayAmount(item)).isEqualTo(9800L)
        assertThat(shouldShowFilterContribution(item)).isFalse()
    }

    @Test
    fun balanceExpenditure_contributesZero() {
        val item = record(typeId = RECORD_TYPE_BALANCE_EXPENDITURE.id, amount = 10000L, finalAmount = 10000L)
        assertThat(recordFilterContribution(item)).isEqualTo(RecordFilterContribution(0L, 0L))
        assertThat(shouldShowFilterContribution(item)).isTrue()
    }

    @Test
    fun balanceIncome_contributesZero() {
        val item = record(category = RecordTypeCategoryEnum.INCOME, typeId = RECORD_TYPE_BALANCE_INCOME.id, amount = 10000L, finalAmount = 10000L)
        assertThat(recordFilterContribution(item)).isEqualTo(RecordFilterContribution(0L, 0L))
        assertThat(shouldShowFilterContribution(item)).isTrue()
    }

    private fun record(
        category: RecordTypeCategoryEnum = RecordTypeCategoryEnum.EXPENDITURE,
        typeId: Long = 1L,
        amount: Long = 10000L,
        finalAmount: Long = amount,
        charges: Long = 0L,
        concessions: Long = 0L,
        related: Boolean = false,
    ) = RecordViewsEntity(
        id = 1L,
        typeId = typeId,
        typeCategory = category,
        typeName = "测试",
        typeIconResName = "",
        assetId = null,
        assetName = null,
        assetIconResId = null,
        relatedAssetId = null,
        relatedAssetName = null,
        relatedAssetIconResId = null,
        amount = amount,
        finalAmount = finalAmount,
        charges = charges,
        concessions = concessions,
        remark = "",
        reimbursable = false,
        relatedTags = emptyList(),
        relatedImage = emptyList(),
        relatedRecord = if (related) listOf(RecordModel(2L, 1L, 1L, -1L, -1L, 80000L, 0L, 0L, 0L, "", false, 1L)) else emptyList(),
        relatedAmount = if (related) 80000L else 0L,
        recordTime = 1L,
    )
}
