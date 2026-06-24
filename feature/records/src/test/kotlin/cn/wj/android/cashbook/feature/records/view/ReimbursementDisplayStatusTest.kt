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
import cn.wj.android.cashbook.core.model.enums.RecordRelatedNatureEnum
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.model.model.RecordModel
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ReimbursementDisplayStatusTest {

    private fun entity(
        category: RecordTypeCategoryEnum = RecordTypeCategoryEnum.EXPENDITURE,
        reimbursable: Boolean = true,
        reimbursed: Boolean = false,
        related: List<RecordModel> = emptyList(),
    ) = RecordViewsEntity(
        id = 1L,
        typeId = 1L,
        typeCategory = category,
        typeName = "t",
        typeIconResName = "i",
        assetId = null,
        assetName = null,
        assetIconResId = null,
        relatedAssetId = null,
        relatedAssetName = null,
        relatedAssetIconResId = null,
        amount = 0L,
        finalAmount = 0L,
        charges = 0L,
        concessions = 0L,
        remark = "",
        reimbursable = reimbursable,
        relatedTags = emptyList(),
        relatedImage = emptyList(),
        relatedRecord = related,
        relatedAmount = 0L,
        relatedNature = RecordRelatedNatureEnum.NONE,
        recordTime = 0L,
        reimbursed = reimbursed,
    )

    @Test
    fun pending_when_reimbursable_unrelated_unmarked() {
        assertThat(entity().reimbursementDisplayStatus()).isEqualTo(ReimbursementDisplayStatus.PENDING)
    }

    @Test
    fun marked_when_reimbursed_flag_set() {
        assertThat(entity(reimbursed = true).reimbursementDisplayStatus())
            .isEqualTo(ReimbursementDisplayStatus.MARKED_REIMBURSED)
    }

    @Test
    fun none_when_not_expenditure() {
        assertThat(entity(category = RecordTypeCategoryEnum.INCOME).reimbursementDisplayStatus())
            .isEqualTo(ReimbursementDisplayStatus.NONE)
    }

    @Test
    fun none_when_related() {
        val related = listOf(
            RecordModel(2L, 1L, 1L, 1L, 1L, 0L, 0L, 0L, 0L, "", false, 0L),
        )
        assertThat(entity(reimbursed = true, related = related).reimbursementDisplayStatus())
            .isEqualTo(ReimbursementDisplayStatus.NONE)
    }

    @Test
    fun none_when_not_reimbursable() {
        assertThat(entity(reimbursable = false).reimbursementDisplayStatus())
            .isEqualTo(ReimbursementDisplayStatus.NONE)
    }
}
