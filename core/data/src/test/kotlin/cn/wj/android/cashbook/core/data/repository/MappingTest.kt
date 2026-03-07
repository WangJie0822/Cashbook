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

import cn.wj.android.cashbook.core.common.SWITCH_INT_OFF
import cn.wj.android.cashbook.core.common.SWITCH_INT_ON
import cn.wj.android.cashbook.core.database.table.AssetTable
import cn.wj.android.cashbook.core.database.table.BooksTable
import cn.wj.android.cashbook.core.database.table.ImageWithRelatedTable
import cn.wj.android.cashbook.core.database.table.RecordTable
import cn.wj.android.cashbook.core.database.table.TagTable
import cn.wj.android.cashbook.core.database.table.TypeTable
import cn.wj.android.cashbook.core.model.enums.AssetClassificationEnum
import cn.wj.android.cashbook.core.model.enums.ClassificationTypeEnum
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.model.enums.TypeLevelEnum
import cn.wj.android.cashbook.core.model.model.AssetModel
import cn.wj.android.cashbook.core.model.model.BooksModel
import cn.wj.android.cashbook.core.model.model.ImageModel
import cn.wj.android.cashbook.core.model.model.RecordModel
import cn.wj.android.cashbook.core.model.model.RecordTypeModel
import cn.wj.android.cashbook.core.model.model.TagModel
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * 模型映射扩展函数测试
 *
 * 测试 Table <-> Model 之间的映射是否正确
 */
class MappingTest {

    // ========== RecordTable <-> RecordModel ==========

    @Test
    fun given_recordTable_with_id_when_asModel_then_id_mapped() {
        val table = createRecordTable(id = 10L)
        val model = table.asModel()
        assertThat(model.id).isEqualTo(10L)
    }

    @Test
    fun given_recordTable_with_null_id_when_asModel_then_id_is_negative_one() {
        val table = createRecordTable(id = null)
        val model = table.asModel()
        assertThat(model.id).isEqualTo(-1L)
    }

    @Test
    fun given_recordTable_when_asModel_then_fields_mapped_correctly() {
        val table = createRecordTable(
            id = 5L,
            booksId = 1L,
            typeId = 2L,
            assetId = 3L,
            intoAssetId = 4L,
            amount = 100.5,
            finalAmount = 95.0,
            charge = 5.5,
            concessions = 10.0,
            remark = "测试备注",
            reimbursable = SWITCH_INT_ON,
        )
        val model = table.asModel()

        assertThat(model.booksId).isEqualTo(1L)
        assertThat(model.typeId).isEqualTo(2L)
        assertThat(model.assetId).isEqualTo(3L)
        assertThat(model.relatedAssetId).isEqualTo(4L)
        assertThat(model.amount).isEqualTo("100.5")
        assertThat(model.finalAmount).isEqualTo("95.0")
        assertThat(model.charges).isEqualTo("5.5")
        assertThat(model.concessions).isEqualTo("10.0")
        assertThat(model.remark).isEqualTo("测试备注")
        assertThat(model.reimbursable).isTrue()
    }

    @Test
    fun given_recordTable_with_reimbursable_off_when_asModel_then_reimbursable_is_false() {
        val table = createRecordTable(reimbursable = SWITCH_INT_OFF)
        val model = table.asModel()
        assertThat(model.reimbursable).isFalse()
    }

    @Test
    fun given_recordModel_with_negative_one_id_when_asTable_then_id_is_null() {
        val model = createRecordModel(id = -1L)
        val table = model.asTable()
        assertThat(table.id).isNull()
    }

    @Test
    fun given_recordModel_with_valid_id_when_asTable_then_id_preserved() {
        val model = createRecordModel(id = 10L)
        val table = model.asTable()
        assertThat(table.id).isEqualTo(10L)
    }

    @Test
    fun given_recordModel_when_asTable_then_reimbursable_mapped_to_int() {
        val modelTrue = createRecordModel(reimbursable = true)
        val modelFalse = createRecordModel(reimbursable = false)

        assertThat(modelTrue.asTable().reimbursable).isEqualTo(SWITCH_INT_ON)
        assertThat(modelFalse.asTable().reimbursable).isEqualTo(SWITCH_INT_OFF)
    }

    @Test
    fun given_recordModel_when_asTable_then_relatedAssetId_mapped_to_intoAssetId() {
        val model = createRecordModel(relatedAssetId = 42L)
        val table = model.asTable()
        assertThat(table.intoAssetId).isEqualTo(42L)
    }

    // ========== BooksTable <-> BooksModel ==========

    @Test
    fun given_booksTable_with_null_id_when_asModel_then_id_is_negative_one() {
        val table = BooksTable(id = null, name = "测试", description = "描述", bgUri = "", modifyTime = 1000L)
        val model = table.asModel()
        assertThat(model.id).isEqualTo(-1L)
    }

    @Test
    fun given_booksTable_with_id_when_asModel_then_fields_mapped() {
        val table = BooksTable(id = 5L, name = "账本", description = "我的账本", bgUri = "uri://bg", modifyTime = 2000L)
        val model = table.asModel()

        assertThat(model.id).isEqualTo(5L)
        assertThat(model.name).isEqualTo("账本")
        assertThat(model.description).isEqualTo("我的账本")
        assertThat(model.bgUri).isEqualTo("uri://bg")
        assertThat(model.modifyTime).isEqualTo(2000L)
    }

    @Test
    fun given_booksModel_with_negative_one_id_when_asTable_then_id_is_null() {
        val model = BooksModel(id = -1L, name = "新账本", description = "", bgUri = "", modifyTime = 3000L)
        val table = model.asTable()
        assertThat(table.id).isNull()
    }

    @Test
    fun given_booksModel_with_valid_id_when_asTable_then_id_preserved() {
        val model = BooksModel(id = 7L, name = "旧账本", description = "desc", bgUri = "bg", modifyTime = 4000L)
        val table = model.asTable()
        assertThat(table.id).isEqualTo(7L)
    }

    // ========== TagTable <-> TagModel ==========

    @Test
    fun given_tagTable_with_null_id_when_asModel_then_id_is_negative_one() {
        val table = TagTable(id = null, name = "标签", booksId = 1L, invisible = SWITCH_INT_OFF)
        val model = table.asModel()
        assertThat(model.id).isEqualTo(-1L)
    }

    @Test
    fun given_tagTable_when_asModel_then_invisible_mapped_to_boolean() {
        val visible = TagTable(id = 1L, name = "可见", booksId = 1L, invisible = SWITCH_INT_OFF)
        val invisible = TagTable(id = 2L, name = "隐藏", booksId = 1L, invisible = SWITCH_INT_ON)

        assertThat(visible.asModel().invisible).isFalse()
        assertThat(invisible.asModel().invisible).isTrue()
    }

    @Test
    fun given_tagModel_with_negative_one_id_when_asTable_then_id_is_null() {
        val model = TagModel(id = -1L, name = "新标签", invisible = false)
        val table = model.asTable()
        assertThat(table.id).isNull()
    }

    @Test
    fun given_tagModel_when_asTable_then_invisible_mapped_to_int() {
        val visible = TagModel(id = 1L, name = "可见", invisible = false)
        val hidden = TagModel(id = 2L, name = "隐藏", invisible = true)

        assertThat(visible.asTable().invisible).isEqualTo(SWITCH_INT_OFF)
        assertThat(hidden.asTable().invisible).isEqualTo(SWITCH_INT_ON)
    }

    @Test
    fun given_tagModel_when_asTable_then_booksId_is_negative_one() {
        // TagModel 不包含 booksId，转换时默认为 -1
        val model = TagModel(id = 1L, name = "标签", invisible = false)
        val table = model.asTable()
        assertThat(table.booksId).isEqualTo(-1L)
    }

    // ========== TypeTable <-> RecordTypeModel ==========

    @Test
    fun given_typeTable_with_null_id_when_asModel_then_id_is_negative_one() {
        val table = createTypeTable(id = null)
        val model = table.asModel(needRelated = false)
        assertThat(model.id).isEqualTo(-1L)
    }

    @Test
    fun given_typeTable_when_asModel_then_fields_mapped_correctly() {
        val table = createTypeTable(
            id = 10L,
            parentId = 5L,
            name = "餐饮",
            iconName = "icon_food",
            typeLevel = TypeLevelEnum.SECOND.ordinal,
            typeCategory = RecordTypeCategoryEnum.EXPENDITURE.ordinal,
            protected = SWITCH_INT_ON,
            sort = 3,
        )
        val model = table.asModel(needRelated = true)

        assertThat(model.id).isEqualTo(10L)
        assertThat(model.parentId).isEqualTo(5L)
        assertThat(model.name).isEqualTo("餐饮")
        assertThat(model.iconName).isEqualTo("icon_food")
        assertThat(model.typeLevel).isEqualTo(TypeLevelEnum.SECOND)
        assertThat(model.typeCategory).isEqualTo(RecordTypeCategoryEnum.EXPENDITURE)
        assertThat(model.protected).isTrue()
        assertThat(model.sort).isEqualTo(3)
        assertThat(model.needRelated).isTrue()
    }

    @Test
    fun given_typeTable_when_asModel_then_protected_mapped_from_int() {
        val protectedType = createTypeTable(protected = SWITCH_INT_ON)
        val normalType = createTypeTable(protected = SWITCH_INT_OFF)

        assertThat(protectedType.asModel(false).protected).isTrue()
        assertThat(normalType.asModel(false).protected).isFalse()
    }

    @Test
    fun given_recordTypeModel_with_negative_one_id_when_asTable_then_id_is_null() {
        val model = createRecordTypeModel(id = -1L)
        val table = model.asTable()
        assertThat(table.id).isNull()
    }

    @Test
    fun given_recordTypeModel_when_asTable_then_protected_mapped_to_int() {
        val protectedModel = createRecordTypeModel(protected = true)
        val normalModel = createRecordTypeModel(protected = false)

        assertThat(protectedModel.asTable().protected).isEqualTo(SWITCH_INT_ON)
        assertThat(normalModel.asTable().protected).isEqualTo(SWITCH_INT_OFF)
    }

    @Test
    fun given_recordTypeModel_when_asTable_then_enum_mapped_to_ordinal() {
        val model = createRecordTypeModel(
            typeLevel = TypeLevelEnum.SECOND,
            typeCategory = RecordTypeCategoryEnum.INCOME,
        )
        val table = model.asTable()

        assertThat(table.typeLevel).isEqualTo(TypeLevelEnum.SECOND.ordinal)
        assertThat(table.typeCategory).isEqualTo(RecordTypeCategoryEnum.INCOME.ordinal)
    }

    // ========== ImageWithRelatedTable <-> ImageModel ==========

    @Test
    fun given_imageTable_with_null_id_when_asModel_then_id_is_negative_one() {
        val table = ImageWithRelatedTable(
            id = null,
            recordId = 1L,
            path = "/path/to/image",
            bytes = byteArrayOf(1, 2, 3),
        )
        val model = table.asModel()
        assertThat(model.id).isEqualTo(-1L)
    }

    @Test
    fun given_imageTable_when_asModel_then_fields_mapped() {
        val bytes = byteArrayOf(10, 20, 30)
        val table = ImageWithRelatedTable(
            id = 5L,
            recordId = 3L,
            path = "/img.png",
            bytes = bytes,
        )
        val model = table.asModel()

        assertThat(model.id).isEqualTo(5L)
        assertThat(model.recordId).isEqualTo(3L)
        assertThat(model.path).isEqualTo("/img.png")
        assertThat(model.bytes).isEqualTo(bytes)
    }

    @Test
    fun given_imageModel_with_negative_one_id_when_asModel_then_table_id_is_null() {
        val model = ImageModel(
            id = -1L,
            recordId = 1L,
            path = "/path",
            bytes = byteArrayOf(),
        )
        // ImageModel.asModel() 返回 ImageWithRelatedTable
        val table = model.asModel()
        assertThat(table.id).isNull()
    }

    @Test
    fun given_imageModel_with_valid_id_when_asModel_then_table_id_preserved() {
        val model = ImageModel(
            id = 7L,
            recordId = 2L,
            path = "/path",
            bytes = byteArrayOf(1),
        )
        val table = model.asModel()
        assertThat(table.id).isEqualTo(7L)
    }

    // ========== AssetTable <-> AssetModel ==========

    @Test
    fun given_assetTable_with_null_id_when_asModel_then_id_is_negative_one() {
        val table = createAssetTable(id = null)
        val model = table.asModel()
        assertThat(model.id).isEqualTo(-1L)
    }

    @Test
    fun given_assetTable_when_asModel_then_invisible_mapped_to_boolean() {
        val visible = createAssetTable(invisible = SWITCH_INT_OFF)
        val hidden = createAssetTable(invisible = SWITCH_INT_ON)

        assertThat(visible.asModel().invisible).isFalse()
        assertThat(hidden.asModel().invisible).isTrue()
    }

    @Test
    fun given_assetTable_when_asModel_then_type_and_classification_mapped_to_enum() {
        val table = createAssetTable(
            type = ClassificationTypeEnum.CAPITAL_ACCOUNT.ordinal,
            classification = AssetClassificationEnum.CASH.ordinal,
        )
        val model = table.asModel()

        assertThat(model.type).isEqualTo(ClassificationTypeEnum.CAPITAL_ACCOUNT)
        assertThat(model.classification).isEqualTo(AssetClassificationEnum.CASH)
    }

    @Test
    fun given_assetModel_with_negative_one_id_when_asTable_then_id_is_null() {
        val model = createAssetModel(id = -1L)
        val table = model.asTable()
        assertThat(table.id).isNull()
    }

    @Test
    fun given_assetModel_when_asTable_then_invisible_mapped_to_int() {
        val visible = createAssetModel(invisible = false)
        val hidden = createAssetModel(invisible = true)

        assertThat(visible.asTable().invisible).isEqualTo(SWITCH_INT_OFF)
        assertThat(hidden.asTable().invisible).isEqualTo(SWITCH_INT_ON)
    }

    @Test
    fun given_assetModel_when_asTable_then_enum_mapped_to_ordinal() {
        val model = createAssetModel(
            type = ClassificationTypeEnum.CREDIT_CARD_ACCOUNT,
            classification = AssetClassificationEnum.CREDIT_CARD,
        )
        val table = model.asTable()

        assertThat(table.type).isEqualTo(ClassificationTypeEnum.CREDIT_CARD_ACCOUNT.ordinal)
        assertThat(table.classification).isEqualTo(AssetClassificationEnum.CREDIT_CARD.ordinal)
    }

    // ========== 辅助方法 ==========

    private fun createRecordTable(
        id: Long? = 1L,
        typeId: Long = 1L,
        assetId: Long = 1L,
        intoAssetId: Long = -1L,
        booksId: Long = 1L,
        amount: Double = 0.0,
        finalAmount: Double = 0.0,
        concessions: Double = 0.0,
        charge: Double = 0.0,
        remark: String = "",
        reimbursable: Int = SWITCH_INT_OFF,
        recordTime: Long = 1000L,
    ) = RecordTable(
        id = id,
        typeId = typeId,
        assetId = assetId,
        intoAssetId = intoAssetId,
        booksId = booksId,
        amount = amount,
        finalAmount = finalAmount,
        concessions = concessions,
        charge = charge,
        remark = remark,
        reimbursable = reimbursable,
        recordTime = recordTime,
    )

    private fun createRecordModel(
        id: Long = 1L,
        booksId: Long = 1L,
        typeId: Long = 1L,
        assetId: Long = 1L,
        relatedAssetId: Long = -1L,
        amount: String = "0",
        finalAmount: String = "0",
        charges: String = "",
        concessions: String = "",
        remark: String = "",
        reimbursable: Boolean = false,
        recordTime: String = "2024-01-01 00:00",
    ) = RecordModel(
        id = id,
        booksId = booksId,
        typeId = typeId,
        assetId = assetId,
        relatedAssetId = relatedAssetId,
        amount = amount,
        finalAmount = finalAmount,
        charges = charges,
        concessions = concessions,
        remark = remark,
        reimbursable = reimbursable,
        recordTime = recordTime,
    )

    private fun createTypeTable(
        id: Long? = 1L,
        parentId: Long = -1L,
        name: String = "类型",
        iconName: String = "icon",
        typeLevel: Int = TypeLevelEnum.FIRST.ordinal,
        typeCategory: Int = RecordTypeCategoryEnum.EXPENDITURE.ordinal,
        protected: Int = SWITCH_INT_OFF,
        sort: Int = 0,
    ) = TypeTable(
        id = id,
        parentId = parentId,
        name = name,
        iconName = iconName,
        typeLevel = typeLevel,
        typeCategory = typeCategory,
        protected = protected,
        sort = sort,
    )

    private fun createRecordTypeModel(
        id: Long = 1L,
        parentId: Long = -1L,
        name: String = "类型",
        iconName: String = "icon",
        typeLevel: TypeLevelEnum = TypeLevelEnum.FIRST,
        typeCategory: RecordTypeCategoryEnum = RecordTypeCategoryEnum.EXPENDITURE,
        protected: Boolean = false,
        sort: Int = 0,
        needRelated: Boolean = false,
    ) = RecordTypeModel(
        id = id,
        parentId = parentId,
        name = name,
        iconName = iconName,
        typeLevel = typeLevel,
        typeCategory = typeCategory,
        protected = protected,
        sort = sort,
        needRelated = needRelated,
    )

    private fun createAssetTable(
        id: Long? = 1L,
        booksId: Long = 1L,
        name: String = "现金",
        balance: Double = 0.0,
        totalAmount: Double = 0.0,
        billingDate: String = "",
        repaymentDate: String = "",
        type: Int = ClassificationTypeEnum.CAPITAL_ACCOUNT.ordinal,
        classification: Int = AssetClassificationEnum.CASH.ordinal,
        invisible: Int = SWITCH_INT_OFF,
        openBank: String = "",
        cardNo: String = "",
        remark: String = "",
        sort: Int = 0,
        modifyTime: Long = 1000L,
    ) = AssetTable(
        id = id,
        booksId = booksId,
        name = name,
        balance = balance,
        totalAmount = totalAmount,
        billingDate = billingDate,
        repaymentDate = repaymentDate,
        type = type,
        classification = classification,
        invisible = invisible,
        openBank = openBank,
        cardNo = cardNo,
        remark = remark,
        sort = sort,
        modifyTime = modifyTime,
    )

    private fun createAssetModel(
        id: Long = 1L,
        booksId: Long = 1L,
        name: String = "现金",
        balance: String = "0",
        totalAmount: String = "0",
        billingDate: String = "",
        repaymentDate: String = "",
        type: ClassificationTypeEnum = ClassificationTypeEnum.CAPITAL_ACCOUNT,
        classification: AssetClassificationEnum = AssetClassificationEnum.CASH,
        invisible: Boolean = false,
        openBank: String = "",
        cardNo: String = "",
        remark: String = "",
        sort: Int = 0,
        modifyTime: String = "2024-01-01 00:00:00",
    ) = AssetModel(
        id = id,
        booksId = booksId,
        name = name,
        iconResId = 0,
        totalAmount = totalAmount,
        billingDate = billingDate,
        repaymentDate = repaymentDate,
        type = type,
        classification = classification,
        invisible = invisible,
        openBank = openBank,
        cardNo = cardNo,
        remark = remark,
        sort = sort,
        modifyTime = modifyTime,
        balance = balance,
    )
}
