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

import cn.wj.android.cashbook.core.database.table.AssetTable
import cn.wj.android.cashbook.core.database.table.RecordTable
import cn.wj.android.cashbook.core.database.table.RecordWithRelatedTable
import cn.wj.android.cashbook.core.database.table.TYPE_TABLE_BALANCE_EXPENDITURE
import cn.wj.android.cashbook.core.database.table.TYPE_TABLE_BALANCE_INCOME
import cn.wj.android.cashbook.core.database.table.TypeTable
import cn.wj.android.cashbook.core.model.enums.ClassificationTypeEnum
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * 测试 TransactionDao 接口中的具体工具方法和 finalAmount 重算逻辑
 *
 * 这些方法是接口的默认实现（非抽象），FakeTransactionDao 直接继承
 */
class TransactionDaoLogicTest {

    private lateinit var dao: FakeTransactionDao

    @Before
    fun setUp() {
        dao = FakeTransactionDao()
    }

    // ========== resolveType 测试 ==========

    @Test
    fun when_resolveType_with_balance_expenditure_id_then_returns_balance_type() = runTest {
        val type = dao.resolveType(TYPE_TABLE_BALANCE_EXPENDITURE.id!!)
        assertThat(type).isEqualTo(TYPE_TABLE_BALANCE_EXPENDITURE)
    }

    @Test
    fun when_resolveType_with_balance_income_id_then_returns_balance_type() = runTest {
        val type = dao.resolveType(TYPE_TABLE_BALANCE_INCOME.id!!)
        assertThat(type).isEqualTo(TYPE_TABLE_BALANCE_INCOME)
    }

    @Test
    fun when_resolveType_with_normal_id_then_queries_database() = runTest {
        val expenditureType = createTypeTable(
            id = 1L,
            typeCategory = RecordTypeCategoryEnum.EXPENDITURE.ordinal,
        )
        dao.types.add(expenditureType)

        val type = dao.resolveType(1L)
        assertThat(type).isEqualTo(expenditureType)
    }

    @Test
    fun when_resolveType_with_unknown_id_then_returns_null() = runTest {
        val type = dao.resolveType(999L)
        assertThat(type).isNull()
    }

    // ========== calculateRecordAmount 测试 ==========

    @Test
    fun when_calculateRecordAmount_for_income_then_amount_minus_charge() {
        val record = createRecordTable(amount = 10000L, charge = 200L, concessions = 500L)
        val result = dao.calculateRecordAmount(record, RecordTypeCategoryEnum.INCOME)
        // 收入：金额 - 手续费 = 10000 - 200 = 9800（优惠不参与收入计算）
        assertThat(result).isEqualTo(9800L)
    }

    @Test
    fun when_calculateRecordAmount_for_expenditure_then_amount_plus_charge_minus_concessions() {
        val record = createRecordTable(amount = 10000L, charge = 200L, concessions = 500L)
        val result = dao.calculateRecordAmount(record, RecordTypeCategoryEnum.EXPENDITURE)
        // 支出：金额 + 手续费 - 优惠 = 10000 + 200 - 500 = 9700
        assertThat(result).isEqualTo(9700L)
    }

    @Test
    fun when_calculateRecordAmount_for_transfer_then_same_as_expenditure() {
        val record = createRecordTable(amount = 10000L, charge = 200L, concessions = 500L)
        val result = dao.calculateRecordAmount(record, RecordTypeCategoryEnum.TRANSFER)
        // 转账与支出同公式
        assertThat(result).isEqualTo(9700L)
    }

    // ========== recalculateAbsorberFinalAmount 测试 ==========

    @Test
    fun when_absorber_has_single_absorbed_then_finalAmount_correct() = runTest {
        // 设置：收入 I(60) 吸收支出 E(100)
        setupTypesForAbsorption()
        val expense = insertRecord(id = 1L, typeId = EXPENDITURE_TYPE_ID, amount = 10000L)
        val income = insertRecord(id = 2L, typeId = INCOME_TYPE_ID, amount = 6000L)
        dao.relatedRecords.add(
            RecordWithRelatedTable(id = 1L, recordId = 2L, relatedRecordId = 1L),
        )

        dao.recalculateAbsorberFinalAmount(2L)

        // I.finalAmount = I.recordAmount(6000) - E.fullAmount(10000) = -4000
        val updatedIncome = dao.queryRecordById(2L)!!
        assertThat(updatedIncome.finalAmount).isEqualTo(-4000L)
    }

    @Test
    fun when_absorber_has_multiple_absorbed_then_finalAmount_sums_all() = runTest {
        // 设置：收入 I(60) 吸收 E1(100) 和 E2(80)
        setupTypesForAbsorption()
        insertRecord(id = 1L, typeId = EXPENDITURE_TYPE_ID, amount = 10000L)
        insertRecord(id = 2L, typeId = EXPENDITURE_TYPE_ID, amount = 8000L)
        insertRecord(id = 3L, typeId = INCOME_TYPE_ID, amount = 6000L)
        dao.relatedRecords.add(
            RecordWithRelatedTable(id = 1L, recordId = 3L, relatedRecordId = 1L),
        )
        dao.relatedRecords.add(
            RecordWithRelatedTable(id = 2L, recordId = 3L, relatedRecordId = 2L),
        )

        dao.recalculateAbsorberFinalAmount(3L)

        // I.finalAmount = 6000 - (10000 + 8000) = -12000
        val updatedIncome = dao.queryRecordById(3L)!!
        assertThat(updatedIncome.finalAmount).isEqualTo(-12000L)
    }

    @Test
    fun when_absorber_has_no_absorbed_then_finalAmount_is_own_amount() = runTest {
        // 设置：收入 I(60) 无关联
        setupTypesForAbsorption()
        insertRecord(id = 1L, typeId = INCOME_TYPE_ID, amount = 6000L)

        dao.recalculateAbsorberFinalAmount(1L)

        val updated = dao.queryRecordById(1L)!!
        assertThat(updated.finalAmount).isEqualTo(6000L)
    }

    @Test
    fun when_absorber_excludes_specific_absorbed_then_excluded_not_counted() = runTest {
        // 设置：I 吸收 E1 和 E2，但重算时排除 E1
        setupTypesForAbsorption()
        insertRecord(id = 1L, typeId = EXPENDITURE_TYPE_ID, amount = 10000L)
        insertRecord(id = 2L, typeId = EXPENDITURE_TYPE_ID, amount = 8000L)
        insertRecord(id = 3L, typeId = INCOME_TYPE_ID, amount = 6000L)
        dao.relatedRecords.add(
            RecordWithRelatedTable(id = 1L, recordId = 3L, relatedRecordId = 1L),
        )
        dao.relatedRecords.add(
            RecordWithRelatedTable(id = 2L, recordId = 3L, relatedRecordId = 2L),
        )

        // 排除 E1(id=1)
        dao.recalculateAbsorberFinalAmount(3L, excludeAbsorbedId = 1L)

        // I.finalAmount = 6000 - 8000 = -2000（只计 E2）
        val updated = dao.queryRecordById(3L)!!
        assertThat(updated.finalAmount).isEqualTo(-2000L)
    }

    // ========== deleteBookTransaction 测试 ==========

    @Test
    fun when_deleteBookTransaction_then_assets_also_deleted() = runTest {
        val bookId = 1L
        dao.books.add(createBooksTable(id = bookId))
        dao.records.add(createRecordTable(id = 1L, booksId = bookId))
        dao.assets.add(createAssetTable(id = 10L, booksId = bookId))
        dao.assets.add(createAssetTable(id = 20L, booksId = 2L))

        dao.deleteBookTransaction(bookId)

        // 本账本资产被删除，其他账本资产保留
        assertThat(dao.assets).hasSize(1)
        assertThat(dao.assets[0].booksId).isEqualTo(2L)
        assertThat(dao.records).isEmpty()
        assertThat(dao.books).isEmpty()
    }

    @Test
    fun when_deleteBookTransaction_then_only_book_records_deleted() = runTest {
        val bookId = 1L
        dao.books.add(createBooksTable(id = bookId))
        dao.records.add(createRecordTable(id = 1L, booksId = bookId))
        dao.records.add(createRecordTable(id = 2L, booksId = bookId))
        dao.records.add(createRecordTable(id = 3L, booksId = 2L))

        dao.deleteBookTransaction(bookId)

        assertThat(dao.records).hasSize(1)
        assertThat(dao.records[0].booksId).isEqualTo(2L)
    }

    // ========== deleteAssetRelatedData 测试 ==========

    @Test
    fun when_deleteAssetRelatedData_then_all_related_records_deleted() = runTest {
        val assetId = 10L
        // 包含源资产和目标资产（转账）的记录都应被删除
        dao.records.add(createRecordTable(id = 1L, assetId = assetId, intoAssetId = 20L))
        dao.records.add(createRecordTable(id = 2L, assetId = 30L, intoAssetId = assetId))
        dao.records.add(createRecordTable(id = 3L, assetId = 30L, intoAssetId = 40L))

        dao.deleteAssetRelatedData(assetId)

        // 只有 id=3 的记录保留
        assertThat(dao.records).hasSize(1)
        assertThat(dao.records[0].id).isEqualTo(3L)
    }

    @Test
    fun when_deleteAssetRelatedData_then_tag_and_image_relations_cleaned() = runTest {
        val assetId = 10L
        dao.records.add(createRecordTable(id = 1L, assetId = assetId))
        dao.tagWithRecords.add(
            cn.wj.android.cashbook.core.database.table.TagWithRecordTable(
                id = 1L,
                recordId = 1L,
                tagId = 100L,
            ),
        )
        dao.imageWithRecords.add(
            cn.wj.android.cashbook.core.database.table.ImageWithRelatedTable(
                id = 1L,
                recordId = 1L,
                path = "path",
                bytes = byteArrayOf(),
            ),
        )

        dao.deleteAssetRelatedData(assetId)

        assertThat(dao.tagWithRecords).isEmpty()
        assertThat(dao.imageWithRecords).isEmpty()
    }

    // ========== 辅助方法 ==========

    companion object {
        private const val EXPENDITURE_TYPE_ID = 1L
        private const val INCOME_TYPE_ID = 2L
        private const val TRANSFER_TYPE_ID = 3L
    }

    private fun setupTypesForAbsorption() {
        dao.types.add(
            createTypeTable(
                id = EXPENDITURE_TYPE_ID,
                typeCategory = RecordTypeCategoryEnum.EXPENDITURE.ordinal,
            ),
        )
        dao.types.add(
            createTypeTable(
                id = INCOME_TYPE_ID,
                typeCategory = RecordTypeCategoryEnum.INCOME.ordinal,
            ),
        )
        dao.types.add(
            createTypeTable(
                id = TRANSFER_TYPE_ID,
                typeCategory = RecordTypeCategoryEnum.TRANSFER.ordinal,
            ),
        )
    }

    private fun insertRecord(
        id: Long,
        typeId: Long = EXPENDITURE_TYPE_ID,
        amount: Long = 0L,
        charge: Long = 0L,
        concessions: Long = 0L,
        assetId: Long = -1L,
        intoAssetId: Long = -1L,
        booksId: Long = 1L,
    ): RecordTable {
        val record = createRecordTable(
            id = id,
            typeId = typeId,
            amount = amount,
            charge = charge,
            concessions = concessions,
            assetId = assetId,
            intoAssetId = intoAssetId,
            booksId = booksId,
        )
        dao.records.add(record)
        return record
    }

    private fun createRecordTable(
        id: Long? = null,
        typeId: Long = 1L,
        assetId: Long = -1L,
        intoAssetId: Long = -1L,
        booksId: Long = 1L,
        amount: Long = 0L,
        finalAmount: Long = 0L,
        concessions: Long = 0L,
        charge: Long = 0L,
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
        remark = "",
        reimbursable = 0,
        recordTime = System.currentTimeMillis(),
    )

    private fun createTypeTable(
        id: Long? = null,
        typeCategory: Int = RecordTypeCategoryEnum.EXPENDITURE.ordinal,
    ) = TypeTable(
        id = id,
        parentId = -1L,
        name = "类型$id",
        iconName = "icon",
        typeLevel = 0,
        typeCategory = typeCategory,
        protected = 0,
        sort = 0,
    )

    private fun createAssetTable(
        id: Long? = null,
        booksId: Long = 1L,
        balance: Long = 0L,
        type: Int = ClassificationTypeEnum.CAPITAL_ACCOUNT.ordinal,
    ) = AssetTable(
        id = id,
        booksId = booksId,
        name = "资产$id",
        balance = balance,
        totalAmount = 0L,
        billingDate = "",
        repaymentDate = "",
        type = type,
        classification = 0,
        invisible = 0,
        openBank = "",
        cardNo = "",
        remark = "",
        sort = 0,
        modifyTime = System.currentTimeMillis(),
    )

    private fun createBooksTable(id: Long? = null) =
        cn.wj.android.cashbook.core.database.table.BooksTable(
            id = id,
            name = "账本$id",
            description = "",
            bgUri = "",
            modifyTime = System.currentTimeMillis(),
        )
}
