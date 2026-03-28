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

package cn.wj.android.cashbook.core.data.testdoubles

import cn.wj.android.cashbook.core.common.FIXED_TYPE_ID_CREDIT_CARD_PAYMENT
import cn.wj.android.cashbook.core.common.FIXED_TYPE_ID_REFUND
import cn.wj.android.cashbook.core.common.FIXED_TYPE_ID_REIMBURSE
import cn.wj.android.cashbook.core.database.table.RecordTable
import cn.wj.android.cashbook.core.database.table.TypeTable
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.model.enums.TypeLevelEnum
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * 测试 TransactionDao.migrateTypeRecords 的迁移逻辑
 *
 * migrateTypeRecords 是 TransactionDao 接口中的默认实现方法，
 * 通过 FakeTransactionDao 的基础方法（updateRecordTypeId、promoteChildTypes、
 * countRecordsByTypeId、deleteTypeById）验证完整的迁移流程。
 */
class SpecialTypeMigrationTest {

    private lateinit var dao: FakeTransactionDao

    @Before
    fun setUp() {
        dao = FakeTransactionDao()
    }

    // ========== migrateTypeRecords 基础流程测试 ==========

    @Test
    fun when_migrate_type_records_then_records_updated_and_old_type_deleted() = runTest {
        // 准备：旧类型（id=42, 名称"退款"）和引用该类型的记录
        dao.types.add(
            createTypeTable(
                id = 42L,
                name = "退款",
                typeCategory = RecordTypeCategoryEnum.INCOME.ordinal,
            ),
        )
        dao.records.add(createRecordTable(id = 1L, typeId = 42L))

        // 执行迁移：将旧类型 42 的记录迁移到固定类型 FIXED_TYPE_ID_REFUND
        dao.migrateTypeRecords(oldTypeId = 42L, fixedTypeId = FIXED_TYPE_ID_REFUND)

        // 断言：记录的 typeId 已更新为固定 ID
        assertThat(dao.records).hasSize(1)
        assertThat(dao.records[0].typeId).isEqualTo(FIXED_TYPE_ID_REFUND)
        // 断言：旧类型已被删除（无剩余记录引用）
        assertThat(dao.types).isEmpty()
    }

    @Test
    fun when_migrate_type_with_children_then_children_promoted_to_first_level() = runTest {
        // 准备：旧的一级类型和其子类型
        dao.types.add(
            createTypeTable(
                id = 42L,
                name = "退款",
                typeCategory = RecordTypeCategoryEnum.INCOME.ordinal,
                typeLevel = TypeLevelEnum.FIRST.ordinal,
                parentId = -1L,
            ),
        )
        dao.types.add(
            createTypeTable(
                id = 43L,
                name = "退款-子类型A",
                typeCategory = RecordTypeCategoryEnum.INCOME.ordinal,
                typeLevel = TypeLevelEnum.SECOND.ordinal,
                parentId = 42L,
            ),
        )
        dao.types.add(
            createTypeTable(
                id = 44L,
                name = "退款-子类型B",
                typeCategory = RecordTypeCategoryEnum.INCOME.ordinal,
                typeLevel = TypeLevelEnum.SECOND.ordinal,
                parentId = 42L,
            ),
        )
        // 准备一条引用旧类型的记录
        dao.records.add(createRecordTable(id = 1L, typeId = 42L))

        // 执行迁移
        dao.migrateTypeRecords(oldTypeId = 42L, fixedTypeId = FIXED_TYPE_ID_REFUND)

        // 断言：记录已迁移
        assertThat(dao.records[0].typeId).isEqualTo(FIXED_TYPE_ID_REFUND)
        // 断言：旧父类型已删除
        assertThat(dao.types.any { it.id == 42L }).isFalse()
        // 断言：子类型提升为一级，parentId 变为 -1，typeLevel 变为 0
        val childA = dao.types.first { it.id == 43L }
        assertThat(childA.parentId).isEqualTo(-1L)
        assertThat(childA.typeLevel).isEqualTo(TypeLevelEnum.FIRST.ordinal)
        val childB = dao.types.first { it.id == 44L }
        assertThat(childB.parentId).isEqualTo(-1L)
        assertThat(childB.typeLevel).isEqualTo(TypeLevelEnum.FIRST.ordinal)
    }

    @Test
    fun when_migrate_type_with_multiple_records_then_all_records_updated() = runTest {
        // 准备：旧类型有多条记录引用
        dao.types.add(
            createTypeTable(
                id = 50L,
                name = "报销",
                typeCategory = RecordTypeCategoryEnum.INCOME.ordinal,
            ),
        )
        dao.records.add(createRecordTable(id = 1L, typeId = 50L))
        dao.records.add(createRecordTable(id = 2L, typeId = 50L))
        dao.records.add(createRecordTable(id = 3L, typeId = 50L))
        // 另有不相关的记录
        dao.records.add(createRecordTable(id = 4L, typeId = 99L))

        // 执行迁移
        dao.migrateTypeRecords(oldTypeId = 50L, fixedTypeId = FIXED_TYPE_ID_REIMBURSE)

        // 断言：所有旧类型的记录都已更新
        val migratedRecords = dao.records.filter { it.typeId == FIXED_TYPE_ID_REIMBURSE }
        assertThat(migratedRecords).hasSize(3)
        assertThat(migratedRecords.map { it.id }).containsExactly(1L, 2L, 3L)
        // 断言：不相关的记录未受影响
        assertThat(dao.records.first { it.id == 4L }.typeId).isEqualTo(99L)
        // 断言：旧类型已删除
        assertThat(dao.types).isEmpty()
    }

    // ========== 各固定类型迁移测试 ==========

    @Test
    fun when_migrate_refund_type_then_uses_fixed_refund_id() = runTest {
        dao.types.add(
            createTypeTable(
                id = 10L,
                name = "退款",
                typeCategory = RecordTypeCategoryEnum.INCOME.ordinal,
            ),
        )
        dao.records.add(createRecordTable(id = 1L, typeId = 10L))

        dao.migrateTypeRecords(oldTypeId = 10L, fixedTypeId = FIXED_TYPE_ID_REFUND)

        assertThat(dao.records[0].typeId).isEqualTo(-2001L)
    }

    @Test
    fun when_migrate_reimburse_type_then_uses_fixed_reimburse_id() = runTest {
        dao.types.add(
            createTypeTable(
                id = 20L,
                name = "报销",
                typeCategory = RecordTypeCategoryEnum.INCOME.ordinal,
            ),
        )
        dao.records.add(createRecordTable(id = 1L, typeId = 20L))

        dao.migrateTypeRecords(oldTypeId = 20L, fixedTypeId = FIXED_TYPE_ID_REIMBURSE)

        assertThat(dao.records[0].typeId).isEqualTo(-2002L)
    }

    @Test
    fun when_migrate_credit_card_payment_type_then_uses_fixed_payment_id() = runTest {
        dao.types.add(
            createTypeTable(
                id = 30L,
                name = "信用卡还款",
                typeCategory = RecordTypeCategoryEnum.TRANSFER.ordinal,
            ),
        )
        dao.records.add(createRecordTable(id = 1L, typeId = 30L))

        dao.migrateTypeRecords(
            oldTypeId = 30L,
            fixedTypeId = FIXED_TYPE_ID_CREDIT_CARD_PAYMENT,
        )

        assertThat(dao.records[0].typeId).isEqualTo(-2003L)
    }

    // ========== 边界情况测试 ==========

    @Test
    fun when_migrate_type_with_no_records_then_old_type_still_deleted() = runTest {
        // 旧类型存在但没有任何记录引用
        dao.types.add(
            createTypeTable(
                id = 42L,
                name = "退款",
                typeCategory = RecordTypeCategoryEnum.INCOME.ordinal,
            ),
        )

        dao.migrateTypeRecords(oldTypeId = 42L, fixedTypeId = FIXED_TYPE_ID_REFUND)

        // 无记录需要更新，countRecordsByTypeId(42) == 0，旧类型仍被删除
        assertThat(dao.types).isEmpty()
    }

    @Test
    fun when_migrate_type_with_no_children_then_only_records_migrated() = runTest {
        // 旧类型无子类型，只有记录引用
        dao.types.add(
            createTypeTable(
                id = 42L,
                name = "退款",
                typeCategory = RecordTypeCategoryEnum.INCOME.ordinal,
            ),
        )
        // 另一个不相关的类型
        dao.types.add(
            createTypeTable(
                id = 100L,
                name = "工资",
                typeCategory = RecordTypeCategoryEnum.INCOME.ordinal,
            ),
        )
        dao.records.add(createRecordTable(id = 1L, typeId = 42L))

        dao.migrateTypeRecords(oldTypeId = 42L, fixedTypeId = FIXED_TYPE_ID_REFUND)

        // 旧类型被删除
        assertThat(dao.types).hasSize(1)
        assertThat(dao.types[0].id).isEqualTo(100L)
        // 记录已迁移
        assertThat(dao.records[0].typeId).isEqualTo(FIXED_TYPE_ID_REFUND)
    }

    // ========== 辅助方法 ==========

    private fun createRecordTable(
        id: Long? = null,
        typeId: Long = 1L,
        assetId: Long = -1L,
        intoAssetId: Long = -1L,
        booksId: Long = 1L,
        amount: Long = 10000L,
    ) = RecordTable(
        id = id,
        typeId = typeId,
        assetId = assetId,
        intoAssetId = intoAssetId,
        booksId = booksId,
        amount = amount,
        finalAmount = amount,
        concessions = 0L,
        charge = 0L,
        remark = "",
        reimbursable = 0,
        recordTime = System.currentTimeMillis(),
    )

    private fun createTypeTable(
        id: Long? = null,
        name: String = "类型",
        typeCategory: Int = RecordTypeCategoryEnum.EXPENDITURE.ordinal,
        typeLevel: Int = TypeLevelEnum.FIRST.ordinal,
        parentId: Long = -1L,
    ) = TypeTable(
        id = id,
        parentId = parentId,
        name = name,
        iconName = "icon",
        typeLevel = typeLevel,
        typeCategory = typeCategory,
        protected = 0,
        sort = 0,
    )
}
