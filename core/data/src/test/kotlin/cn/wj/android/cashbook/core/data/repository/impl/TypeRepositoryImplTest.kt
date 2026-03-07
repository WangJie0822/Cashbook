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

import cn.wj.android.cashbook.core.common.SWITCH_INT_OFF
import cn.wj.android.cashbook.core.data.repository.asModel
import cn.wj.android.cashbook.core.data.repository.asTable
import cn.wj.android.cashbook.core.data.testdoubles.FakeCombineProtoDataSource
import cn.wj.android.cashbook.core.data.testdoubles.FakeTypeDao
import cn.wj.android.cashbook.core.database.table.TypeTable
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.model.enums.TypeLevelEnum
import cn.wj.android.cashbook.core.model.model.RecordTypeModel
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * TypeRepository 实现测试
 *
 * 测试类型仓库的核心业务逻辑，包括类型层级管理、特殊标记查询等
 */
class TypeRepositoryImplTest {

    private lateinit var typeDao: FakeTypeDao
    private lateinit var fakeDataSource: FakeCombineProtoDataSource

    @Before
    fun setup() {
        typeDao = FakeTypeDao()
        fakeDataSource = FakeCombineProtoDataSource()
    }

    // ========== 类型层级管理测试 ==========

    @Test
    fun when_queryByLevel_first_then_returns_first_level_types() = runTest {
        typeDao.insertType(createTypeTable(name = "餐饮", typeLevel = TypeLevelEnum.FIRST.ordinal))
        typeDao.insertType(createTypeTable(name = "午餐", typeLevel = TypeLevelEnum.SECOND.ordinal))
        typeDao.insertType(createTypeTable(name = "交通", typeLevel = TypeLevelEnum.FIRST.ordinal))

        val firstLevel = typeDao.queryByLevel(TypeLevelEnum.FIRST.ordinal)
        assertThat(firstLevel).hasSize(2)
        assertThat(firstLevel.map { it.name }).containsExactly("餐饮", "交通")
    }

    @Test
    fun when_queryByParentId_then_returns_child_types() = runTest {
        val parentId = typeDao.insertType(
            createTypeTable(name = "餐饮", typeLevel = TypeLevelEnum.FIRST.ordinal),
        )
        typeDao.insertType(
            createTypeTable(
                name = "午餐",
                typeLevel = TypeLevelEnum.SECOND.ordinal,
                parentId = parentId,
            ),
        )
        typeDao.insertType(
            createTypeTable(
                name = "晚餐",
                typeLevel = TypeLevelEnum.SECOND.ordinal,
                parentId = parentId,
            ),
        )
        typeDao.insertType(
            createTypeTable(
                name = "地铁",
                typeLevel = TypeLevelEnum.SECOND.ordinal,
                parentId = 999L,
            ),
        )

        val children = typeDao.queryByParentId(parentId)
        assertThat(children).hasSize(2)
        assertThat(children.map { it.name }).containsExactly("午餐", "晚餐")
    }

    @Test
    fun when_changeTypeToSecond_then_type_level_updated() = runTest {
        val id = typeDao.insertType(
            createTypeTable(name = "类型A", typeLevel = TypeLevelEnum.FIRST.ordinal),
        )
        val parentId = typeDao.insertType(
            createTypeTable(name = "父类型", typeLevel = TypeLevelEnum.FIRST.ordinal),
        )

        // 模拟 changeTypeToSecond 逻辑
        typeDao.updateTypeLevel(
            id = id,
            parentId = parentId,
            typeLevel = TypeLevelEnum.SECOND.ordinal,
        )

        val updated = typeDao.queryById(id)!!
        assertThat(updated.typeLevel).isEqualTo(TypeLevelEnum.SECOND.ordinal)
        assertThat(updated.parentId).isEqualTo(parentId)
    }

    @Test
    fun when_changeSecondTypeToFirst_then_type_level_updated() = runTest {
        val parentId = typeDao.insertType(
            createTypeTable(name = "父类型", typeLevel = TypeLevelEnum.FIRST.ordinal),
        )
        val childId = typeDao.insertType(
            createTypeTable(
                name = "子类型",
                typeLevel = TypeLevelEnum.SECOND.ordinal,
                parentId = parentId,
            ),
        )

        // 模拟 changeSecondTypeToFirst 逻辑
        typeDao.updateTypeLevel(
            id = childId,
            parentId = -1L,
            typeLevel = TypeLevelEnum.FIRST.ordinal,
        )

        val updated = typeDao.queryById(childId)!!
        assertThat(updated.typeLevel).isEqualTo(TypeLevelEnum.FIRST.ordinal)
        assertThat(updated.parentId).isEqualTo(-1L)
    }

    // ========== 特殊标记查询测试 ==========

    @Test
    fun when_needRelated_with_refundTypeId_then_returns_true() = runTest {
        fakeDataSource.updateRefundTypeId(10L)

        val result = fakeDataSource.needRelated(10L)
        assertThat(result).isTrue()
    }

    @Test
    fun when_needRelated_with_reimburseTypeId_then_returns_true() = runTest {
        fakeDataSource.updateReimburseTypeId(20L)

        val result = fakeDataSource.needRelated(20L)
        assertThat(result).isTrue()
    }

    @Test
    fun when_needRelated_with_normal_typeId_then_returns_false() = runTest {
        fakeDataSource.updateRefundTypeId(10L)
        fakeDataSource.updateReimburseTypeId(20L)

        val result = fakeDataSource.needRelated(30L)
        assertThat(result).isFalse()
    }

    @Test
    fun when_queryByName_for_refund_type_then_returns_matching_type() = runTest {
        typeDao.insertType(createTypeTable(name = "退款", typeCategory = RecordTypeCategoryEnum.INCOME.ordinal))
        typeDao.insertType(createTypeTable(name = "报销", typeCategory = RecordTypeCategoryEnum.INCOME.ordinal))

        val refund = typeDao.queryByName("退款")
        assertThat(refund).isNotNull()
        assertThat(refund!!.name).isEqualTo("退款")

        val reimburse = typeDao.queryByName("报销")
        assertThat(reimburse).isNotNull()
        assertThat(reimburse!!.name).isEqualTo("报销")
    }

    @Test
    fun when_queryByName_not_found_then_returns_null() = runTest {
        val result = typeDao.queryByName("不存在的类型")
        assertThat(result).isNull()
    }

    // ========== CRUD 测试 ==========

    @Test
    fun when_insertType_then_type_stored_with_id() = runTest {
        val id = typeDao.insertType(createTypeTable(name = "新类型"))

        assertThat(id).isGreaterThan(0L)
        assertThat(typeDao.types).hasSize(1)
        assertThat(typeDao.types[0].name).isEqualTo("新类型")
    }

    @Test
    fun when_insertOrReplace_existing_then_replaced() = runTest {
        val id = typeDao.insertType(createTypeTable(name = "原始"))
        val original = typeDao.queryById(id)!!

        typeDao.insertOrReplace(original.copy(name = "替换后"))

        val result = typeDao.queryById(id)!!
        assertThat(result.name).isEqualTo("替换后")
        assertThat(typeDao.types).hasSize(1)
    }

    @Test
    fun when_insertOrReplace_new_then_inserted() = runTest {
        typeDao.insertOrReplace(createTypeTable(name = "新插入"))

        assertThat(typeDao.types).hasSize(1)
    }

    @Test
    fun when_deleteById_then_type_removed() = runTest {
        val id = typeDao.insertType(createTypeTable(name = "待删除"))
        typeDao.insertType(createTypeTable(name = "保留"))

        typeDao.deleteById(id)

        assertThat(typeDao.types).hasSize(1)
        assertThat(typeDao.types[0].name).isEqualTo("保留")
    }

    @Test
    fun when_queryById_then_returns_matching_type() = runTest {
        val id = typeDao.insertType(createTypeTable(name = "目标"))

        val result = typeDao.queryById(id)
        assertThat(result).isNotNull()
        assertThat(result!!.name).isEqualTo("目标")
    }

    @Test
    fun given_nonexistent_id_when_queryById_then_returns_null() = runTest {
        val result = typeDao.queryById(999L)
        assertThat(result).isNull()
    }

    // ========== 计数查询测试 ==========

    @Test
    fun when_countByName_then_returns_correct_count() = runTest {
        typeDao.insertType(createTypeTable(name = "餐饮"))
        typeDao.insertType(createTypeTable(name = "餐饮"))
        typeDao.insertType(createTypeTable(name = "交通"))

        assertThat(typeDao.countByName("餐饮")).isEqualTo(2)
        assertThat(typeDao.countByName("交通")).isEqualTo(1)
        assertThat(typeDao.countByName("不存在")).isEqualTo(0)
    }

    @Test
    fun when_countByLevel_then_returns_correct_count() = runTest {
        typeDao.insertType(createTypeTable(typeLevel = TypeLevelEnum.FIRST.ordinal))
        typeDao.insertType(createTypeTable(typeLevel = TypeLevelEnum.FIRST.ordinal))
        typeDao.insertType(createTypeTable(typeLevel = TypeLevelEnum.SECOND.ordinal))

        assertThat(typeDao.countByLevel(TypeLevelEnum.FIRST.ordinal)).isEqualTo(2)
        assertThat(typeDao.countByLevel(TypeLevelEnum.SECOND.ordinal)).isEqualTo(1)
    }

    @Test
    fun when_countByParentId_then_returns_correct_count() = runTest {
        typeDao.insertType(createTypeTable(parentId = 1L))
        typeDao.insertType(createTypeTable(parentId = 1L))
        typeDao.insertType(createTypeTable(parentId = 2L))

        assertThat(typeDao.countByParentId(1L)).isEqualTo(2)
        assertThat(typeDao.countByParentId(2L)).isEqualTo(1)
    }

    // ========== Flow 查询测试 ==========

    @Test
    fun when_queryAll_flow_then_emits_all_types() = runTest {
        typeDao.insertType(createTypeTable(name = "类型A"))
        typeDao.insertType(createTypeTable(name = "类型B"))

        val result = typeDao.queryAll().first()
        assertThat(result).hasSize(2)
    }

    @Test
    fun when_queryByTypeCategory_flow_then_filters_by_category() = runTest {
        typeDao.insertType(
            createTypeTable(name = "餐饮", typeCategory = RecordTypeCategoryEnum.EXPENDITURE.ordinal),
        )
        typeDao.insertType(
            createTypeTable(name = "工资", typeCategory = RecordTypeCategoryEnum.INCOME.ordinal),
        )
        typeDao.insertType(
            createTypeTable(name = "交通", typeCategory = RecordTypeCategoryEnum.EXPENDITURE.ordinal),
        )

        val expenditure = typeDao.queryByTypeCategory(RecordTypeCategoryEnum.EXPENDITURE.ordinal).first()
        assertThat(expenditure).hasSize(2)
        assertThat(expenditure.map { it.name }).containsExactly("餐饮", "交通")
    }

    // ========== 模型映射集成测试 ==========

    @Test
    fun when_model_asTable_and_insert_then_roundtrip_works() = runTest {
        val model = RecordTypeModel(
            id = -1L,
            parentId = -1L,
            name = "测试类型",
            iconName = "icon_test",
            typeLevel = TypeLevelEnum.FIRST,
            typeCategory = RecordTypeCategoryEnum.EXPENDITURE,
            protected = false,
            sort = 1,
            needRelated = false,
        )

        val table = model.asTable()
        assertThat(table.id).isNull()

        val id = typeDao.insertType(table)
        val queried = typeDao.queryById(id)!!
        val result = queried.asModel(needRelated = false)

        assertThat(result.name).isEqualTo("测试类型")
        assertThat(result.iconName).isEqualTo("icon_test")
        assertThat(result.typeLevel).isEqualTo(TypeLevelEnum.FIRST)
        assertThat(result.typeCategory).isEqualTo(RecordTypeCategoryEnum.EXPENDITURE)
    }

    @Test
    fun when_generateSortById_logic_then_sort_calculated_correctly() = runTest {
        // 模拟 generateSortById 逻辑中的 countByLevel 和 countByParentId
        typeDao.insertType(createTypeTable(typeLevel = TypeLevelEnum.FIRST.ordinal))
        typeDao.insertType(createTypeTable(typeLevel = TypeLevelEnum.FIRST.ordinal))

        // 一级分类的排序 = countByLevel(FIRST) + 1
        val firstLevelSort = typeDao.countByLevel(TypeLevelEnum.FIRST.ordinal) + 1
        assertThat(firstLevelSort).isEqualTo(3)

        // 二级分类的排序 = parentSort * 1000 + countByParentId(parentId)
        val parentId = 1L
        typeDao.insertType(createTypeTable(typeLevel = TypeLevelEnum.SECOND.ordinal, parentId = parentId))
        val childCount = typeDao.countByParentId(parentId)
        assertThat(childCount).isEqualTo(1)
    }

    // ========== 信用卡还款类型测试 ==========

    @Test
    fun when_setCreditPaymentType_then_isCreditPaymentType_returns_true() = runTest {
        typeDao.insertType(createTypeTable(name = "还信用卡"))
        val type = typeDao.queryByName("还信用卡")!!
        val typeId = type.id!!

        fakeDataSource.updateCreditCardPaymentTypeId(typeId)

        val settings = fakeDataSource.recordSettingsData.first()
        assertThat(settings.creditCardPaymentTypeId).isEqualTo(typeId)
    }

    // ========== 辅助方法 ==========

    private fun createTypeTable(
        id: Long? = null,
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
}
