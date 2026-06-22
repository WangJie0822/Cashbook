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
import cn.wj.android.cashbook.core.database.table.ImageWithRelatedTable
import cn.wj.android.cashbook.core.database.table.RecordTable
import cn.wj.android.cashbook.core.database.table.RecordWithRelatedTable
import cn.wj.android.cashbook.core.database.table.TYPE_TABLE_BALANCE_EXPENDITURE
import cn.wj.android.cashbook.core.database.table.TYPE_TABLE_BALANCE_INCOME
import cn.wj.android.cashbook.core.database.table.TagWithRecordTable
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

    // ========== recalculateFinalAmountForCluster 委托/排除场景测试（净自付）==========

    @Test
    fun when_absorber_recalc_single_absorbed_then_cluster_net_self_paid() = runTest {
        // I(60) 吸收 E(100) → 净自付：E.fa=40, I.fa=0（旧吸收模型曾为 I.fa=-40）
        setupTypesForAbsorption()
        insertRecord(id = 1L, typeId = EXPENDITURE_TYPE_ID, amount = 10000L)
        insertRecord(id = 2L, typeId = INCOME_TYPE_ID, amount = 6000L)
        dao.relatedRecords.add(RecordWithRelatedTable(id = 1L, recordId = 2L, relatedRecordId = 1L))

        dao.recalculateFinalAmountForCluster(2L)

        assertThat(dao.queryRecordById(1L)!!.finalAmount).isEqualTo(4000L) // 净自付，非负
        assertThat(dao.queryRecordById(2L)!!.finalAmount).isEqualTo(0L)
    }

    @Test
    fun when_absorber_recalc_multiple_absorbed_then_greedy_by_id() = runTest {
        // I(60) 吸收 E1(100),E2(80)：I 先填 E1 → E1.fa=40,E2.fa=80,I.fa=0
        setupTypesForAbsorption()
        insertRecord(id = 1L, typeId = EXPENDITURE_TYPE_ID, amount = 10000L)
        insertRecord(id = 2L, typeId = EXPENDITURE_TYPE_ID, amount = 8000L)
        insertRecord(id = 3L, typeId = INCOME_TYPE_ID, amount = 6000L)
        dao.relatedRecords.add(RecordWithRelatedTable(id = 1L, recordId = 3L, relatedRecordId = 1L))
        dao.relatedRecords.add(RecordWithRelatedTable(id = 2L, recordId = 3L, relatedRecordId = 2L))

        dao.recalculateFinalAmountForCluster(3L)

        assertThat(dao.queryRecordById(1L)!!.finalAmount).isEqualTo(4000L)
        assertThat(dao.queryRecordById(2L)!!.finalAmount).isEqualTo(8000L)
        assertThat(dao.queryRecordById(3L)!!.finalAmount).isEqualTo(0L)
    }

    @Test
    fun when_absorber_recalc_no_absorbed_then_finalAmount_is_recordAmount() = runTest {
        setupTypesForAbsorption()
        insertRecord(id = 1L, typeId = INCOME_TYPE_ID, amount = 6000L)

        dao.recalculateFinalAmountForCluster(1L)

        assertThat(dao.queryRecordById(1L)!!.finalAmount).isEqualTo(6000L)
    }

    @Test
    fun when_absorber_recalc_excludes_specific_absorbed_then_excluded_not_in_cluster() = runTest {
        // I(60) 吸收 E1(100),E2(80)，排除 E1 → 簇={I,E2}：E2.fa=20,I.fa=0
        setupTypesForAbsorption()
        insertRecord(id = 1L, typeId = EXPENDITURE_TYPE_ID, amount = 10000L)
        insertRecord(id = 2L, typeId = EXPENDITURE_TYPE_ID, amount = 8000L)
        insertRecord(id = 3L, typeId = INCOME_TYPE_ID, amount = 6000L)
        dao.relatedRecords.add(RecordWithRelatedTable(id = 1L, recordId = 3L, relatedRecordId = 1L))
        dao.relatedRecords.add(RecordWithRelatedTable(id = 2L, recordId = 3L, relatedRecordId = 2L))

        dao.recalculateFinalAmountForCluster(3L, excludeRecordIds = setOf(1L))

        assertThat(dao.queryRecordById(2L)!!.finalAmount).isEqualTo(2000L) // 6000 填 E2 → 8000-6000
        assertThat(dao.queryRecordById(3L)!!.finalAmount).isEqualTo(0L)
    }

    // ========== 净自付簇算法 recalculateFinalAmountForCluster 测试 ==========

    @Test
    fun when_cluster_1to1_partial_then_expense_net_income_zero() = runTest {
        // E(100) 被 I(80) 部分吸收 → E.fa=20, I.fa=0
        setupTypesForAbsorption()
        insertRecord(id = 1L, typeId = EXPENDITURE_TYPE_ID, amount = 10000L)
        insertRecord(id = 2L, typeId = INCOME_TYPE_ID, amount = 8000L)
        dao.relatedRecords.add(RecordWithRelatedTable(id = 1L, recordId = 2L, relatedRecordId = 1L))

        dao.recalculateFinalAmountForCluster(2L)

        assertThat(dao.queryRecordById(1L)!!.finalAmount).isEqualTo(2000L) // 净自付
        assertThat(dao.queryRecordById(2L)!!.finalAmount).isEqualTo(0L) // 溢出
    }

    @Test
    fun when_cluster_over_reimburse_then_floor_zero_and_overflow_to_income() = runTest {
        // E(100) 被 I(120) 超额吸收 → E.fa=0, I.fa=20
        setupTypesForAbsorption()
        insertRecord(id = 1L, typeId = EXPENDITURE_TYPE_ID, amount = 10000L)
        insertRecord(id = 2L, typeId = INCOME_TYPE_ID, amount = 12000L)
        dao.relatedRecords.add(RecordWithRelatedTable(id = 1L, recordId = 2L, relatedRecordId = 1L))

        dao.recalculateFinalAmountForCluster(2L)

        assertThat(dao.queryRecordById(1L)!!.finalAmount).isEqualTo(0L)
        assertThat(dao.queryRecordById(2L)!!.finalAmount).isEqualTo(2000L)
    }

    @Test
    fun when_cluster_1toN_then_greedy_fill_by_id_asc() = runTest {
        // I(80) 吸收 E1(100),E2(50) → E1.fa=20,E2.fa=50,I.fa=0
        setupTypesForAbsorption()
        insertRecord(id = 1L, typeId = EXPENDITURE_TYPE_ID, amount = 10000L)
        insertRecord(id = 2L, typeId = EXPENDITURE_TYPE_ID, amount = 5000L)
        insertRecord(id = 3L, typeId = INCOME_TYPE_ID, amount = 8000L)
        dao.relatedRecords.add(RecordWithRelatedTable(id = 1L, recordId = 3L, relatedRecordId = 1L))
        dao.relatedRecords.add(RecordWithRelatedTable(id = 2L, recordId = 3L, relatedRecordId = 2L))

        dao.recalculateFinalAmountForCluster(3L)

        assertThat(dao.queryRecordById(1L)!!.finalAmount).isEqualTo(2000L) // 80 先填 E1 → 100-80
        assertThat(dao.queryRecordById(2L)!!.finalAmount).isEqualTo(5000L) // 无剩余，E2 全额自付
        assertThat(dao.queryRecordById(3L)!!.finalAmount).isEqualTo(0L)
    }

    @Test
    fun when_cluster_Nto1_then_expense_offset_by_sum() = runTest {
        // E(100) 被 I1(30),I2(40) 吸收 → E.fa=30,I1.fa=0,I2.fa=0
        setupTypesForAbsorption()
        insertRecord(id = 1L, typeId = EXPENDITURE_TYPE_ID, amount = 10000L)
        insertRecord(id = 2L, typeId = INCOME_TYPE_ID, amount = 3000L)
        insertRecord(id = 3L, typeId = INCOME_TYPE_ID, amount = 4000L)
        dao.relatedRecords.add(RecordWithRelatedTable(id = 1L, recordId = 2L, relatedRecordId = 1L))
        dao.relatedRecords.add(RecordWithRelatedTable(id = 2L, recordId = 3L, relatedRecordId = 1L))

        dao.recalculateFinalAmountForCluster(1L)

        assertThat(dao.queryRecordById(1L)!!.finalAmount).isEqualTo(3000L) // 100-30-40
        assertThat(dao.queryRecordById(2L)!!.finalAmount).isEqualTo(0L)
        assertThat(dao.queryRecordById(3L)!!.finalAmount).isEqualTo(0L)
    }

    @Test
    fun when_cluster_charges_nonzero_then_net_self_paid_uses_recordAmount() = runTest {
        // M9: E(amount100,charge0)=100；I(amount80,charge5)→recordAmount(I)=75 → E.fa=25,I.fa=0
        setupTypesForAbsorption()
        insertRecord(id = 1L, typeId = EXPENDITURE_TYPE_ID, amount = 10000L)
        insertRecord(id = 2L, typeId = INCOME_TYPE_ID, amount = 8000L, charge = 500L)
        dao.relatedRecords.add(RecordWithRelatedTable(id = 1L, recordId = 2L, relatedRecordId = 1L))

        dao.recalculateFinalAmountForCluster(2L)

        assertThat(dao.queryRecordById(1L)!!.finalAmount).isEqualTo(2500L) // 100-75
        assertThat(dao.queryRecordById(2L)!!.finalAmount).isEqualTo(0L)
    }

    // ========== insertRecordTransaction 净自付测试 ==========

    @Test
    fun when_insert_income_absorber_then_cluster_net_self_paid() = runTest {
        // 先存在支出 E(100)，再经 insertRecordTransaction 插入收入 I(80) 关联 E → E.fa=20, I.fa=0
        setupTypesForAbsorption()
        val expenseId = dao.insertRecord(createRecordTable(typeId = EXPENDITURE_TYPE_ID, amount = 10000L))

        dao.insertRecordTransaction(
            record = createRecordTable(typeId = INCOME_TYPE_ID, amount = 8000L),
            tagIdList = emptyList(),
            needRelated = true,
            relatedRecordIdList = listOf(expenseId),
            relatedImageList = emptyList(),
        )
        val incomeId = dao.records.first { it.amount == 8000L }.id!!

        assertThat(dao.queryRecordById(expenseId)!!.finalAmount).isEqualTo(2000L)
        assertThat(dao.queryRecordById(incomeId)!!.finalAmount).isEqualTo(0L)
    }

    // ========== deleteRecordTransaction 净自付测试 ==========

    @Test
    fun when_delete_absorber_income_single_then_expense_restores_full() = runTest {
        // I(80) 吸收 E(100)：删 I → E 恢复全额 100
        setupTypesForAbsorption()
        insertRecord(id = 1L, typeId = EXPENDITURE_TYPE_ID, amount = 10000L)
        val income = insertRecord(id = 2L, typeId = INCOME_TYPE_ID, amount = 8000L)
        dao.relatedRecords.add(RecordWithRelatedTable(id = 1L, recordId = 2L, relatedRecordId = 1L))
        dao.recalculateFinalAmountForCluster(2L) // E.fa=20

        dao.deleteRecordTransaction(income)

        assertThat(dao.queryRecordById(1L)!!.finalAmount).isEqualTo(10000L) // 恢复全额
        assertThat(dao.queryRecordById(2L)).isNull()
    }

    @Test
    fun when_delete_one_of_two_absorbers_then_remaining_recalc_excludes_deleted() = runTest {
        // E(100) 被 I1(30,id=2),I2(40,id=3) 吸收：删 I1 → E 只被 I2 吸收 → E.fa=60,I2.fa=0
        // 判别性用例：旧 INCOME 删分支经簇化后会误把待删 I1 算进簇得 E.fa=30，新分支排除 I1 得 60
        setupTypesForAbsorption()
        insertRecord(id = 1L, typeId = EXPENDITURE_TYPE_ID, amount = 10000L)
        val income1 = insertRecord(id = 2L, typeId = INCOME_TYPE_ID, amount = 3000L)
        insertRecord(id = 3L, typeId = INCOME_TYPE_ID, amount = 4000L)
        dao.relatedRecords.add(RecordWithRelatedTable(id = 1L, recordId = 2L, relatedRecordId = 1L))
        dao.relatedRecords.add(RecordWithRelatedTable(id = 2L, recordId = 3L, relatedRecordId = 1L))
        dao.recalculateFinalAmountForCluster(1L) // E.fa=30,I1.fa=0,I2.fa=0

        dao.deleteRecordTransaction(income1)

        assertThat(dao.queryRecordById(1L)!!.finalAmount).isEqualTo(6000L) // 100-40（只剩 I2）
        assertThat(dao.queryRecordById(3L)!!.finalAmount).isEqualTo(0L)
        assertThat(dao.queryRecordById(2L)).isNull()
    }

    @Test
    fun when_delete_shared_expense_then_remaining_absorbers_restore() = runTest {
        // E(100) 被 I1(30),I2(40)：删 E → I1,I2 恢复各自 recordAmount
        setupTypesForAbsorption()
        val expense = insertRecord(id = 1L, typeId = EXPENDITURE_TYPE_ID, amount = 10000L)
        insertRecord(id = 2L, typeId = INCOME_TYPE_ID, amount = 3000L)
        insertRecord(id = 3L, typeId = INCOME_TYPE_ID, amount = 4000L)
        dao.relatedRecords.add(RecordWithRelatedTable(id = 1L, recordId = 2L, relatedRecordId = 1L))
        dao.relatedRecords.add(RecordWithRelatedTable(id = 2L, recordId = 3L, relatedRecordId = 1L))
        dao.recalculateFinalAmountForCluster(1L) // E.fa=30,I1.fa=0,I2.fa=0

        dao.deleteRecordTransaction(expense)

        assertThat(dao.queryRecordById(1L)).isNull()
        assertThat(dao.queryRecordById(2L)!!.finalAmount).isEqualTo(3000L) // 恢复 recordAmount
        assertThat(dao.queryRecordById(3L)!!.finalAmount).isEqualTo(4000L)
    }

    @Test
    fun when_update_absorbed_expense_then_relation_broken_both_standalone() = runTest {
        // H4：I(80) 吸收 E(100)（E.fa=20）；编辑 E 金额为 120 → 关联断开，E 独立 120，I 独立 80
        setupTypesForAbsorption()
        insertRecord(id = 1L, typeId = EXPENDITURE_TYPE_ID, amount = 10000L)
        insertRecord(id = 2L, typeId = INCOME_TYPE_ID, amount = 8000L)
        dao.relatedRecords.add(RecordWithRelatedTable(id = 1L, recordId = 2L, relatedRecordId = 1L))
        dao.recalculateFinalAmountForCluster(2L)

        dao.updateRecordTransaction(
            record = createRecordTable(id = 1L, typeId = EXPENDITURE_TYPE_ID, amount = 12000L),
            tagIdList = emptyList(),
            needRelated = false,
            relatedRecordIdList = emptyList(),
            relatedImageList = emptyList(),
        )

        assertThat(dao.relatedRecords).isEmpty()
        assertThat(dao.queryRecordById(1L)!!.finalAmount).isEqualTo(12000L)
        assertThat(dao.queryRecordById(2L)!!.finalAmount).isEqualTo(8000L)
    }

    // ========== discoverClusterIds 测试 ==========

    @Test
    fun when_discoverClusterIds_then_returns_cluster_and_outEdges() = runTest {
        setupTypesForAbsorption()
        // E1(1),E2(2) 被 I(3) 吸收
        insertRecord(id = 1L, typeId = EXPENDITURE_TYPE_ID, amount = 10000L)
        insertRecord(id = 2L, typeId = EXPENDITURE_TYPE_ID, amount = 5000L)
        insertRecord(id = 3L, typeId = INCOME_TYPE_ID, amount = 8000L)
        dao.relatedRecords.add(RecordWithRelatedTable(id = 1L, recordId = 3L, relatedRecordId = 1L))
        dao.relatedRecords.add(RecordWithRelatedTable(id = 2L, recordId = 3L, relatedRecordId = 2L))

        val result = dao.discoverClusterIds(3L)

        assertThat(result.clusterIds).containsExactly(1L, 2L, 3L)
        // outEdges 缓存：吸收者 I(3) 的出边是其吸收的支出 [1,2]
        assertThat(result.outEdges[3L]).containsExactly(1L, 2L)
    }

    // ========== recalculateAllFinalAmount 全量测试 ==========

    @Test
    fun when_recalcAll_then_all_clusters_net_self_paid_and_idempotent() = runTest {
        setupTypesForAbsorption()
        // 簇A：E1(100) 被 I1(80)；独立支出 E2(50)；转账 T(amount200,charge5,concession2)
        insertRecord(id = 1L, typeId = EXPENDITURE_TYPE_ID, amount = 10000L)
        insertRecord(id = 2L, typeId = INCOME_TYPE_ID, amount = 8000L)
        insertRecord(id = 3L, typeId = EXPENDITURE_TYPE_ID, amount = 5000L)
        insertRecord(id = 4L, typeId = TRANSFER_TYPE_ID, amount = 20000L, charge = 500L, concessions = 200L)
        dao.relatedRecords.add(RecordWithRelatedTable(id = 1L, recordId = 2L, relatedRecordId = 1L))

        dao.recalculateAllFinalAmount()

        assertThat(dao.queryRecordById(1L)!!.finalAmount).isEqualTo(2000L) // 净自付 100-80
        assertThat(dao.queryRecordById(2L)!!.finalAmount).isEqualTo(0L) // 溢出 0
        assertThat(dao.queryRecordById(3L)!!.finalAmount).isEqualTo(5000L) // 独立支出全额
        assertThat(dao.queryRecordById(4L)!!.finalAmount).isEqualTo(-300L) // 转账 concessions-charge=200-500

        // 幂等：连跑两次结果一致
        dao.recalculateAllFinalAmount()
        assertThat(dao.queryRecordById(1L)!!.finalAmount).isEqualTo(2000L)
        assertThat(dao.queryRecordById(2L)!!.finalAmount).isEqualTo(0L)
    }

    @Test
    fun when_recalcAll_equals_incremental_cluster_result() = runTest {
        // M6/§5：全量与增量逐字段一致
        setupTypesForAbsorption()
        insertRecord(id = 1L, typeId = EXPENDITURE_TYPE_ID, amount = 10000L)
        insertRecord(id = 2L, typeId = EXPENDITURE_TYPE_ID, amount = 5000L)
        insertRecord(id = 3L, typeId = INCOME_TYPE_ID, amount = 8000L)
        dao.relatedRecords.add(RecordWithRelatedTable(id = 1L, recordId = 3L, relatedRecordId = 1L))
        dao.relatedRecords.add(RecordWithRelatedTable(id = 2L, recordId = 3L, relatedRecordId = 2L))

        dao.recalculateAllFinalAmount()
        val fullA = dao.queryRecordById(1L)!!.finalAmount
        val fullB = dao.queryRecordById(2L)!!.finalAmount
        val fullC = dao.queryRecordById(3L)!!.finalAmount

        dao.recalculateFinalAmountForCluster(3L)
        assertThat(dao.queryRecordById(1L)!!.finalAmount).isEqualTo(fullA)
        assertThat(dao.queryRecordById(2L)!!.finalAmount).isEqualTo(fullB)
        assertThat(dao.queryRecordById(3L)!!.finalAmount).isEqualTo(fullC)
    }

    // ========== deleteBookTransaction 测试 ==========

    @Test
    fun when_deleteBook_all_in_book_cluster_then_no_survivor_recalc() = runTest {
        // 区分力：全簇随账本同删 → 无存活簇 → 重算 0 次（旧逐条路径会 >0）
        setupTypesForAbsorption()
        val bookId = 1L
        dao.books.add(createBooksTable(id = bookId))
        insertRecord(id = 1L, typeId = EXPENDITURE_TYPE_ID, amount = 10000L, booksId = bookId)
        insertRecord(id = 2L, typeId = INCOME_TYPE_ID, amount = 8000L, booksId = bookId)
        dao.relatedRecords.add(RecordWithRelatedTable(id = 1L, recordId = 2L, relatedRecordId = 1L))
        dao.recalculateFinalAmountForCluster(2L)
        dao.queryRecordByIdsCallCount = 0 // 重置，只测删除阶段

        dao.deleteBookTransaction(bookId)

        assertThat(dao.queryRecordByIdsCallCount).isEqualTo(0)
        assertThat(dao.records).isEmpty()
    }

    @Test
    fun when_deleteAsset_clears_absorbed_expense_then_surviving_absorber_recalced() = runTest {
        // 存活簇正向守卫（L2）：删资产清掉被吸收支出 → 存活吸收者恢复 recordAmount
        setupTypesForAbsorption()
        dao.assets.add(createAssetTable(id = 10L, balance = 0L))
        dao.assets.add(createAssetTable(id = 20L, balance = 0L))
        insertRecord(id = 1L, typeId = EXPENDITURE_TYPE_ID, amount = 10000L, assetId = 10L)
        insertRecord(id = 2L, typeId = INCOME_TYPE_ID, amount = 8000L, assetId = 20L)
        dao.relatedRecords.add(RecordWithRelatedTable(id = 1L, recordId = 2L, relatedRecordId = 1L))
        dao.recalculateFinalAmountForCluster(2L) // E.fa=2000, I.fa=0

        dao.deleteAssetRelatedData(10L) // 删 E（asset=10）

        assertThat(dao.queryRecordById(1L)).isNull()
        assertThat(dao.queryRecordById(2L)!!.finalAmount).isEqualTo(8000L) // 恢复 recordAmount
    }

    @Test
    fun when_deleteAsset_with_transfer_then_surviving_counterpart_balance_reverted() = runTest {
        // AUDIT-2/2b：删资产 A(10)，转账 B(20)→A，源资产 B 存活余额回退；A 行存活余额回退
        setupTypesForAbsorption()
        // 转账后状态：B=80000(被扣 20000), A=20000(被加 20000)
        dao.assets.add(createAssetTable(id = 10L, balance = 20000L))
        dao.assets.add(createAssetTable(id = 20L, balance = 80000L))
        insertRecord(id = 1L, typeId = TRANSFER_TYPE_ID, amount = 20000L, assetId = 20L, intoAssetId = 10L)

        dao.deleteAssetRelatedData(10L)

        assertThat(dao.queryRecordById(1L)).isNull()
        assertThat(dao.queryAssetById(20L)!!.balance).isEqualTo(100000L) // 源 B 回退 +20000
        assertThat(dao.queryAssetById(10L)).isNotNull() // A 行存活(AUDIT-2)
        assertThat(dao.queryAssetById(10L)!!.balance).isEqualTo(0L) // A 回退 -20000
    }

    @Test
    fun when_deleteBookTransaction_then_assets_also_deleted() = runTest {
        setupTypesForAbsorption()
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
        setupTypesForAbsorption()
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
    fun when_deleteAssetRelatedData_clears_dangling_relation_and_recalcs_survivor() = runTest {
        // 存活吸收者 S(id=2,asset=20) 吸收被删支出 D(id=1,asset=10)；删 asset=10 → D 删、悬空关联清、S 重算
        setupTypesForAbsorption()
        dao.assets.add(createAssetTable(id = 10L, balance = 0L))
        dao.assets.add(createAssetTable(id = 20L, balance = 0L))
        insertRecord(id = 1L, typeId = EXPENDITURE_TYPE_ID, amount = 10000L, assetId = 10L)
        insertRecord(id = 2L, typeId = INCOME_TYPE_ID, amount = 8000L, assetId = 20L)
        dao.relatedRecords.add(RecordWithRelatedTable(id = 1L, recordId = 2L, relatedRecordId = 1L))
        dao.recalculateFinalAmountForCluster(2L) // E.fa=2000, I.fa=0

        dao.deleteAssetRelatedData(10L)

        assertThat(dao.queryRecordById(1L)).isNull() // 被删支出删除
        assertThat(dao.relatedRecords).isEmpty() // 悬空关联清（双向 IN-OR）
        assertThat(dao.queryRecordById(2L)!!.finalAmount).isEqualTo(8000L) // 存活吸收者重算恢复 recordAmount
    }

    @Test
    fun when_deleteAssetRelatedData_then_all_related_records_deleted() = runTest {
        setupTypesForAbsorption()
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
        setupTypesForAbsorption()
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

    // ========== batchImportRecordsTransaction 测试 ==========

    @Test
    fun when_batchImport_capital_asset_then_balance_aggregated_and_ids_returned() = runTest {
        setupTypesForAbsorption()
        dao.assets.add(createAssetTable(id = 10L, balance = 50000L))

        val ids = dao.batchImportRecordsTransaction(
            listOf(
                createRecordTable(typeId = INCOME_TYPE_ID, amount = 8000L, assetId = 10L),
                createRecordTable(typeId = EXPENDITURE_TYPE_ID, amount = 3000L, assetId = 10L),
            ),
        )

        // 返回两条插入 id
        assertThat(ids).hasSize(2)
        // 非信用卡：余额 + 收入 - 支出 = 50000 + 8000 - 3000 = 55000
        assertThat(dao.queryAssetById(10L)!!.balance).isEqualTo(55000L)
        // finalAmount = recordAmount（无吸收关联）
        assertThat(dao.queryRecordById(ids[0])!!.finalAmount).isEqualTo(8000L) // 收入 = amount - charge
        assertThat(dao.queryRecordById(ids[1])!!.finalAmount).isEqualTo(3000L) // 支出 = amount + charge - concessions
    }

    @Test
    fun when_batchImport_credit_card_asset_then_balance_uses_inverted_sign() = runTest {
        setupTypesForAbsorption()
        dao.assets.add(
            createAssetTable(
                id = 20L,
                balance = 10000L,
                type = ClassificationTypeEnum.CREDIT_CARD_ACCOUNT.ordinal,
            ),
        )

        dao.batchImportRecordsTransaction(
            listOf(
                createRecordTable(typeId = EXPENDITURE_TYPE_ID, amount = 5000L, assetId = 20L),
                createRecordTable(typeId = INCOME_TYPE_ID, amount = 2000L, assetId = 20L),
            ),
        )

        // 信用卡：已用额度 - 收入 + 支出 = 10000 - 2000 + 5000 = 13000
        // （非信用卡同输入会得 10000 + 2000 - 5000 = 7000，符号相反，用于区分两分支）
        assertThat(dao.queryAssetById(20L)!!.balance).isEqualTo(13000L)
    }

    @Test
    fun when_batchImport_with_charge_and_concessions_then_recordAmount_and_per_asset_grouping() =
        runTest {
            setupTypesForAbsorption()
            dao.assets.add(createAssetTable(id = 10L, balance = 0L))
            dao.assets.add(createAssetTable(id = 20L, balance = 0L))

            val ids = dao.batchImportRecordsTransaction(
                listOf(
                    createRecordTable(
                        typeId = EXPENDITURE_TYPE_ID,
                        amount = 10000L,
                        charge = 200L,
                        concessions = 500L,
                        assetId = 10L,
                    ),
                    createRecordTable(
                        typeId = INCOME_TYPE_ID,
                        amount = 8000L,
                        charge = 300L,
                        assetId = 20L,
                    ),
                ),
            )

            // 支出 recordAmount = 10000 + 200 - 500 = 9700；资产10 = 0 - 9700
            assertThat(dao.queryRecordById(ids[0])!!.finalAmount).isEqualTo(9700L)
            assertThat(dao.queryAssetById(10L)!!.balance).isEqualTo(-9700L)
            // 收入 recordAmount = 8000 - 300 = 7700；资产20 = 0 + 7700（按资产独立聚合）
            assertThat(dao.queryRecordById(ids[1])!!.finalAmount).isEqualTo(7700L)
            assertThat(dao.queryAssetById(20L)!!.balance).isEqualTo(7700L)
        }

    @Test
    fun when_batchImport_record_without_asset_then_no_balance_update() = runTest {
        setupTypesForAbsorption()
        dao.assets.add(createAssetTable(id = 10L, balance = 50000L))

        val ids = dao.batchImportRecordsTransaction(
            listOf(
                // assetId 默认 -1，不关联资产
                createRecordTable(typeId = EXPENDITURE_TYPE_ID, amount = 5000L),
            ),
        )

        assertThat(ids).hasSize(1)
        // assetId <= 0 不参与余额聚合，资产余额不变
        assertThat(dao.queryAssetById(10L)!!.balance).isEqualTo(50000L)
        assertThat(dao.queryRecordById(ids[0])!!.finalAmount).isEqualTo(5000L)
    }

    @Test
    fun when_batchImport_unknown_type_then_skipped_and_not_inserted() = runTest {
        setupTypesForAbsorption()
        dao.assets.add(createAssetTable(id = 10L, balance = 0L))

        val ids = dao.batchImportRecordsTransaction(
            listOf(
                createRecordTable(typeId = EXPENDITURE_TYPE_ID, amount = 5000L, assetId = 10L),
                // typeId 无对应 type，queryTypeById 返 null → continue 跳过
                createRecordTable(typeId = 999L, amount = 3000L, assetId = 10L),
            ),
        )

        // 仅合法记录被插入
        assertThat(ids).hasSize(1)
        // 余额仅累计合法记录的支出：0 - 5000
        assertThat(dao.queryAssetById(10L)!!.balance).isEqualTo(-5000L)
    }

    @Test
    fun when_batchImport_transfer_then_debits_source_only_and_ignores_into_asset() = runTest {
        setupTypesForAbsorption()
        dao.assets.add(createAssetTable(id = 10L, balance = 50000L)) // 源资产
        dao.assets.add(createAssetTable(id = 20L, balance = 30000L)) // 目标资产

        val ids = dao.batchImportRecordsTransaction(
            listOf(
                createRecordTable(
                    typeId = TRANSFER_TYPE_ID,
                    amount = 10000L,
                    assetId = 10L,
                    intoAssetId = 20L,
                ),
            ),
        )

        // TRANSFER recordAmount = amount + charge - concessions = 10000
        assertThat(dao.queryRecordById(ids[0])!!.finalAmount).isEqualTo(10000L)
        // 转账按支出借记源资产：50000 - 10000 = 40000
        assertThat(dao.queryAssetById(10L)!!.balance).isEqualTo(40000L)
        // 批量导入不处理 intoAssetId（与单条 insertRecordTransaction 不同），目标资产不变
        assertThat(dao.queryAssetById(20L)!!.balance).isEqualTo(30000L)
    }

    @Test
    fun when_batchImport_record_with_zero_asset_id_then_no_balance_update() = runTest {
        setupTypesForAbsorption()
        dao.assets.add(createAssetTable(id = 10L, balance = 50000L))

        // assetId == 0 与 assetId == -1 同走 (assetId > 0) 守卫的否定分支
        val ids = dao.batchImportRecordsTransaction(
            listOf(
                createRecordTable(typeId = EXPENDITURE_TYPE_ID, amount = 5000L, assetId = 0L),
            ),
        )

        assertThat(ids).hasSize(1)
        assertThat(dao.queryAssetById(10L)!!.balance).isEqualTo(50000L)
    }

    // ========== 辅助方法 ==========

    // ========== byIds 批量删 SQL 测试（P-M1）==========

    @Test
    fun when_deleteRecordsByIds_then_returns_deleted_row_count() = runTest {
        dao.records.add(createRecordTable(id = 1L))
        dao.records.add(createRecordTable(id = 2L))
        dao.records.add(createRecordTable(id = 3L))

        val deleted = dao.deleteRecordsByIds(listOf(1L, 2L))

        assertThat(deleted).isEqualTo(2)
        assertThat(dao.records.map { it.id }).containsExactly(3L)
    }

    @Test
    fun when_deleteRecordRelationsByRecordIds_then_clears_both_directions() = runTest {
        // 吸收者 2（存活）指向被删支出 1：record_id=2, related_record_id=1
        dao.relatedRecords.add(RecordWithRelatedTable(id = 1L, recordId = 2L, relatedRecordId = 1L))
        // 反向：record_id=1（被删）指向 3
        dao.relatedRecords.add(RecordWithRelatedTable(id = 2L, recordId = 1L, relatedRecordId = 3L))
        // 无关边
        dao.relatedRecords.add(RecordWithRelatedTable(id = 3L, recordId = 5L, relatedRecordId = 6L))

        dao.deleteRecordRelationsByRecordIds(listOf(1L))

        // 含 1 的两条（任一方向）被清，无关边保留
        assertThat(dao.relatedRecords.map { it.id }).containsExactly(3L)
    }

    @Test
    fun when_deleteTagAndImageRelationsByRecordIds_then_only_matching_cleared() = runTest {
        dao.tagWithRecords.add(TagWithRecordTable(id = 1L, recordId = 1L, tagId = 100L))
        dao.tagWithRecords.add(TagWithRecordTable(id = 2L, recordId = 2L, tagId = 100L))
        dao.imageWithRecords.add(ImageWithRelatedTable(id = 1L, recordId = 1L, path = "/a", bytes = ByteArray(0)))
        dao.imageWithRecords.add(ImageWithRelatedTable(id = 2L, recordId = 2L, path = "/b", bytes = ByteArray(0)))

        dao.deleteTagRelationsByRecordIds(listOf(1L))
        dao.deleteImageRelationsByRecordIds(listOf(1L))

        assertThat(dao.tagWithRecords.map { it.recordId }).containsExactly(2L)
        assertThat(dao.imageWithRecords.map { it.recordId }).containsExactly(2L)
    }

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
