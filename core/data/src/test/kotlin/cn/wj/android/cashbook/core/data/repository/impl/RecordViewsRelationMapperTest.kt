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

import cn.wj.android.cashbook.core.common.FIXED_TYPE_ID_REIMBURSE
import cn.wj.android.cashbook.core.common.SWITCH_INT_ON
import cn.wj.android.cashbook.core.database.relation.LauncherRecordViewRelation
import cn.wj.android.cashbook.core.database.table.AssetTable
import cn.wj.android.cashbook.core.database.table.ImageWithRelatedTable
import cn.wj.android.cashbook.core.database.table.RecordTable
import cn.wj.android.cashbook.core.database.table.TagTable
import cn.wj.android.cashbook.core.database.table.TypeTable
import cn.wj.android.cashbook.core.model.enums.RecordRelatedNatureEnum
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.model.model.RECORD_TYPE_BALANCE_EXPENDITURE
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * [toRecordViewsModel] 组装等价性单测：平账合成类型、relatedRecord 双向选向、关联金额/性质。
 */
class RecordViewsRelationMapperTest {

    private fun recordTable(id: Long, typeId: Long, amount: Long = 1000L) = RecordTable(
        id = id, typeId = typeId, assetId = 10L, intoAssetId = -1L, booksId = 1L,
        amount = amount, finalAmount = amount, concessions = 0L, charge = 0L,
        remark = "r", reimbursable = 0, recordTime = 100L, reimbursed = 0,
    )

    private fun typeTable(id: Long, category: Int) = TypeTable(
        id = id,
        parentId = -1L,
        name = "餐饮",
        iconName = "ic",
        typeLevel = 0,
        typeCategory = category,
        protected = 0,
        sort = 0,
    )

    private fun assetTable(id: Long) = AssetTable(
        id = id, booksId = 1L, name = "现金", balance = 0L, totalAmount = 0L,
        billingDate = "", repaymentDate = "", type = 0, classification = 0,
        invisible = 0, openBank = "", cardNo = "", remark = "", sort = 0, modifyTime = 0L,
    )

    @Test
    fun normal_expenditure_maps_all_fields() {
        val pojo = LauncherRecordViewRelation(
            record = recordTable(id = 1L, typeId = 5L),
            types = listOf(typeTable(5L, RecordTypeCategoryEnum.EXPENDITURE.ordinal)),
            assets = listOf(assetTable(10L)),
            intoAssets = emptyList(),
            images = emptyList(),
            tags = listOf(TagTable(id = 3L, name = "旅行", booksId = 1L, invisible = 0)),
            relatedAsRecordId = emptyList(),
            relatedAsRelatedId = emptyList(),
        )
        val m = pojo.toRecordViewsModel()
        assertThat(m.id).isEqualTo(1L)
        assertThat(m.type.typeCategory).isEqualTo(RecordTypeCategoryEnum.EXPENDITURE)
        assertThat(m.asset?.id).isEqualTo(10L)
        assertThat(m.relatedAsset).isNull()
        assertThat(m.relatedTags.map { it.id }).containsExactly(3L)
        assertThat(m.relatedNature).isEqualTo(RecordRelatedNatureEnum.NONE)
        assertThat(m.relatedAmount).isEqualTo(0L)
    }

    @Test
    fun balance_type_empty_types_resolves_synthetic() {
        val pojo = LauncherRecordViewRelation(
            record = recordTable(id = 2L, typeId = RECORD_TYPE_BALANCE_EXPENDITURE.id),
            types = emptyList(),
            assets = emptyList(),
            intoAssets = emptyList(),
            images = emptyList(),
            tags = emptyList(),
            relatedAsRecordId = emptyList(),
            relatedAsRelatedId = emptyList(),
        )
        val m = pojo.toRecordViewsModel()
        assertThat(m.type.id).isEqualTo(RECORD_TYPE_BALANCE_EXPENDITURE.id)
        assertThat(m.isBalanceRecord).isTrue()
    }

    @Test
    fun expenditure_picks_relatedAsRelatedId_side() {
        val pojo = LauncherRecordViewRelation(
            record = recordTable(id = 4L, typeId = 5L),
            types = listOf(typeTable(5L, RecordTypeCategoryEnum.EXPENDITURE.ordinal)),
            assets = emptyList(),
            intoAssets = emptyList(),
            images = emptyList(),
            tags = emptyList(),
            relatedAsRecordId = listOf(recordTable(id = 99L, typeId = 5L)), // 不应被选
            relatedAsRelatedId = listOf(recordTable(id = 7L, typeId = FIXED_TYPE_ID_REIMBURSE, amount = 1000L)),
        )
        val m = pojo.toRecordViewsModel()
        assertThat(m.relatedRecord.map { it.id }).containsExactly(7L)
        assertThat(m.relatedNature).isEqualTo(RecordRelatedNatureEnum.REIMBURSED)
        assertThat(m.relatedAmount).isEqualTo(1000L) // recordAmount(INCOME)=amount-charges=1000
    }

    @Test
    fun income_picks_relatedAsRecordId_side() {
        val pojo = LauncherRecordViewRelation(
            record = recordTable(id = 2L, typeId = 8L),
            types = listOf(typeTable(8L, RecordTypeCategoryEnum.INCOME.ordinal)),
            assets = emptyList(),
            intoAssets = emptyList(),
            images = emptyList(),
            tags = emptyList(),
            relatedAsRecordId = listOf(recordTable(id = 5L, typeId = 1L)), // 收入侧应选此
            relatedAsRelatedId = listOf(recordTable(id = 9L, typeId = 1L)), // 不应被选
        )
        val m = pojo.toRecordViewsModel()
        assertThat(m.type.typeCategory).isEqualTo(RecordTypeCategoryEnum.INCOME)
        assertThat(m.relatedRecord.map { it.id }).containsExactly(5L)
    }

    @Test
    fun duplicate_tag_relation_rows_deduped() {
        // db_tag_with_record 无 (record_id,tag_id) 唯一约束，重复关联行经 @Relation JOIN 物化重复 TagTable；
        // 须与单条版 id IN 子查询 / 批量版 .distinct() 一致去重
        val tag = TagTable(id = 3L, name = "旅行", booksId = 1L, invisible = 0)
        val pojo = LauncherRecordViewRelation(
            record = recordTable(id = 1L, typeId = 5L),
            types = listOf(typeTable(5L, RecordTypeCategoryEnum.EXPENDITURE.ordinal)),
            assets = emptyList(),
            intoAssets = emptyList(),
            images = emptyList(),
            tags = listOf(tag, tag),
            relatedAsRecordId = emptyList(),
            relatedAsRelatedId = emptyList(),
        )
        val m = pojo.toRecordViewsModel()
        assertThat(m.relatedTags.map { it.id }).containsExactly(3L)
    }

    @Test
    fun maps_asset_relatedAsset_image_reimbursable_charges_fields() {
        val record = RecordTable(
            id = 1L, typeId = 5L, assetId = 10L, intoAssetId = 20L, booksId = 1L,
            amount = 1000L, finalAmount = 800L, concessions = 50L, charge = 30L,
            remark = "备注", reimbursable = SWITCH_INT_ON, recordTime = 100L, reimbursed = SWITCH_INT_ON,
        )
        val pojo = LauncherRecordViewRelation(
            record = record,
            types = listOf(typeTable(5L, RecordTypeCategoryEnum.EXPENDITURE.ordinal)),
            assets = listOf(assetTable(10L)),
            intoAssets = listOf(assetTable(20L)),
            images = listOf(ImageWithRelatedTable(id = 1L, recordId = 1L, path = "/img.jpg", bytes = byteArrayOf(1))),
            tags = emptyList(),
            relatedAsRecordId = emptyList(),
            relatedAsRelatedId = emptyList(),
        )
        val m = pojo.toRecordViewsModel()
        assertThat(m.asset?.id).isEqualTo(10L)
        assertThat(m.relatedAsset?.id).isEqualTo(20L)
        assertThat(m.charges).isEqualTo(30L)
        assertThat(m.concessions).isEqualTo(50L)
        assertThat(m.finalAmount).isEqualTo(800L)
        assertThat(m.reimbursable).isTrue()
        assertThat(m.reimbursed).isTrue()
        assertThat(m.relatedImage).hasSize(1)
    }
}
