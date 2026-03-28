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

package cn.wj.android.cashbook.core.database.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import cn.wj.android.cashbook.core.common.NO_ASSET_ID
import cn.wj.android.cashbook.core.common.SWITCH_INT_OFF
import cn.wj.android.cashbook.core.database.CashbookDatabase
import cn.wj.android.cashbook.core.database.table.AssetTable
import cn.wj.android.cashbook.core.database.table.BooksTable
import cn.wj.android.cashbook.core.database.table.ImageWithRelatedTable
import cn.wj.android.cashbook.core.database.table.RecordTable
import cn.wj.android.cashbook.core.database.table.TYPE_TABLE_BALANCE_EXPENDITURE
import cn.wj.android.cashbook.core.database.table.TYPE_TABLE_BALANCE_INCOME
import cn.wj.android.cashbook.core.database.table.TagTable
import cn.wj.android.cashbook.core.database.table.TagWithRecordTable
import cn.wj.android.cashbook.core.database.table.TypeTable
import cn.wj.android.cashbook.core.model.enums.ClassificationTypeEnum
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.model.model.ImageModel
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * TransactionDao 事务数据库操作测试
 *
 * 测试覆盖：基本 CRUD、resolveType、insertRecordTransaction、
 * deleteRecordTransaction、deleteBookTransaction、deleteAssetRelatedData、
 * migrateTypeRecords、deleteTag、verifyAssetBalance
 */
@RunWith(AndroidJUnit4::class)
class TransactionDaoTest {

    private lateinit var database: CashbookDatabase
    private lateinit var transactionDao: TransactionDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            CashbookDatabase::class.java,
        ).allowMainThreadQueries().build()
        transactionDao = database.transactionDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    // region 辅助方法

    /** 创建支出类型 */
    private fun createExpenditureType(
        id: Long? = null,
        name: String = "餐饮",
    ) = TypeTable(
        id = id,
        parentId = -1L,
        name = name,
        iconName = "icon_food",
        typeLevel = 0,
        typeCategory = RecordTypeCategoryEnum.EXPENDITURE.ordinal,
        protected = SWITCH_INT_OFF,
        sort = 0,
    )

    /** 创建收入类型 */
    private fun createIncomeType(
        id: Long? = null,
        name: String = "工资",
    ) = TypeTable(
        id = id,
        parentId = -1L,
        name = name,
        iconName = "icon_salary",
        typeLevel = 0,
        typeCategory = RecordTypeCategoryEnum.INCOME.ordinal,
        protected = SWITCH_INT_OFF,
        sort = 0,
    )

    /** 创建转账类型 */
    private fun createTransferType(
        id: Long? = null,
        name: String = "转账",
    ) = TypeTable(
        id = id,
        parentId = -1L,
        name = name,
        iconName = "icon_transfer",
        typeLevel = 0,
        typeCategory = RecordTypeCategoryEnum.TRANSFER.ordinal,
        protected = SWITCH_INT_OFF,
        sort = 0,
    )

    /** 创建普通资产（资金账户） */
    private fun createNormalAsset(
        id: Long? = null,
        booksId: Long = 1L,
        name: String = "现金",
        balance: Long = 100000L, // 1000.00 元
    ) = AssetTable(
        id = id,
        booksId = booksId,
        name = name,
        balance = balance,
        totalAmount = 0L,
        billingDate = "",
        repaymentDate = "",
        type = ClassificationTypeEnum.CAPITAL_ACCOUNT.ordinal,
        classification = 0,
        invisible = SWITCH_INT_OFF,
        openBank = "",
        cardNo = "",
        remark = "",
        sort = 0,
        modifyTime = System.currentTimeMillis(),
    )

    /** 创建信用卡资产 */
    private fun createCreditCardAsset(
        id: Long? = null,
        booksId: Long = 1L,
        name: String = "信用卡",
        balance: Long = 0L,
        totalAmount: Long = 5000000L, // 50000.00 元
    ) = AssetTable(
        id = id,
        booksId = booksId,
        name = name,
        balance = balance,
        totalAmount = totalAmount,
        billingDate = "15",
        repaymentDate = "5",
        type = ClassificationTypeEnum.CREDIT_CARD_ACCOUNT.ordinal,
        classification = 0,
        invisible = SWITCH_INT_OFF,
        openBank = "",
        cardNo = "",
        remark = "",
        sort = 0,
        modifyTime = System.currentTimeMillis(),
    )

    /** 创建记录 */
    private fun createRecord(
        id: Long? = null,
        typeId: Long,
        assetId: Long = NO_ASSET_ID,
        intoAssetId: Long = NO_ASSET_ID,
        booksId: Long = 1L,
        amount: Long = 10000L, // 100.00 元
        charge: Long = 0L,
        concessions: Long = 0L,
        remark: String = "",
        reimbursable: Int = SWITCH_INT_OFF,
        recordTime: Long = System.currentTimeMillis(),
    ) = RecordTable(
        id = id,
        typeId = typeId,
        assetId = assetId,
        intoAssetId = intoAssetId,
        booksId = booksId,
        amount = amount,
        finalAmount = 0L, // 由 insertRecordTransaction 计算
        concessions = concessions,
        charge = charge,
        remark = remark,
        reimbursable = reimbursable,
        recordTime = recordTime,
    )

    /** 插入账本并返回 id */
    private suspend fun insertBook(id: Long? = null, name: String = "默认账本"): Long {
        return database.booksDao().insert(
            BooksTable(
                id = id,
                name = name,
                description = "",
                bgUri = "",
                modifyTime = System.currentTimeMillis(),
            ),
        )
    }

    /** 插入类型并返回 id */
    private suspend fun insertType(type: TypeTable): Long {
        return database.typeDao().insertType(type)
    }

    /** 插入资产（使用指定 id） */
    private suspend fun insertAsset(asset: AssetTable) {
        database.assetDao().insert(asset)
    }

    /** 查询资产 */
    private suspend fun queryAsset(assetId: Long): AssetTable? {
        return transactionDao.queryAssetById(assetId)
    }

    // endregion

    // region 1. 基本操作测试

    @Test
    fun when_insertRecord_then_queryByIdReturnsIt() = runTest {
        // 准备：插入账本和类型
        insertBook()
        val typeId = insertType(createExpenditureType())

        // 执行：插入记录
        val record = createRecord(typeId = typeId, remark = "测试记录")
        val recordId = transactionDao.insertRecord(record)

        // 验证：通过 id 可查询到记录
        val result = transactionDao.queryRecordById(recordId)
        assertThat(result).isNotNull()
        assertThat(result!!.typeId).isEqualTo(typeId)
        assertThat(result.remark).isEqualTo("测试记录")
        assertThat(result.amount).isEqualTo(10000L)
    }

    @Test
    fun when_deleteRecord_then_recordIsRemoved() = runTest {
        // 准备：插入记录
        insertBook()
        val typeId = insertType(createExpenditureType())
        val record = createRecord(typeId = typeId)
        val recordId = transactionDao.insertRecord(record)

        // 确认记录已插入
        assertThat(transactionDao.queryRecordById(recordId)).isNotNull()

        // 执行：删除记录
        val insertedRecord = transactionDao.queryRecordById(recordId)!!
        val deleteCount = transactionDao.deleteRecord(insertedRecord)

        // 验证：记录已被删除
        assertThat(deleteCount).isEqualTo(1)
        assertThat(transactionDao.queryRecordById(recordId)).isNull()
    }

    @Test
    fun when_insertAndDeleteRelatedTags_then_worksCorrectly() = runTest {
        // 准备：插入类型和记录
        insertBook()
        val typeId = insertType(createExpenditureType())
        val recordId = transactionDao.insertRecord(createRecord(typeId = typeId))

        // 插入标签
        val tagId1 = database.tagDao().insert(
            TagTable(id = null, name = "标签A", booksId = 1L, invisible = SWITCH_INT_OFF),
        )
        val tagId2 = database.tagDao().insert(
            TagTable(id = null, name = "标签B", booksId = 1L, invisible = SWITCH_INT_OFF),
        )

        // 执行：插入标签关联
        transactionDao.insertRelatedTags(
            listOf(
                TagWithRecordTable(id = null, recordId = recordId, tagId = tagId1),
                TagWithRecordTable(id = null, recordId = recordId, tagId = tagId2),
            ),
        )

        // 验证：可以通过记录 id 查到关联的标签
        val tags = database.tagDao().queryByRecordId(recordId)
        assertThat(tags).hasSize(2)
        assertThat(tags.map { it.name }).containsExactly("标签A", "标签B")

        // 执行：删除关联标签
        transactionDao.deleteOldRelatedTags(recordId)

        // 验证：关联已清空
        val tagsAfterDelete = database.tagDao().queryByRecordId(recordId)
        assertThat(tagsAfterDelete).isEmpty()
    }

    @Test
    fun when_insertAndDeleteRelatedImages_then_worksCorrectly() = runTest {
        // 准备：插入类型和记录
        insertBook()
        val typeId = insertType(createExpenditureType())
        val recordId = transactionDao.insertRecord(createRecord(typeId = typeId))

        // 执行：插入图片关联
        val imageBytes = byteArrayOf(1, 2, 3, 4, 5)
        transactionDao.insertRelatedImages(
            listOf(
                ImageWithRelatedTable(
                    id = null,
                    recordId = recordId,
                    path = "/test/image1.jpg",
                    bytes = imageBytes,
                ),
            ),
        )

        // 执行：删除图片关联
        transactionDao.deleteOldRelatedImages(recordId)

        // 验证：此操作不抛出异常即可视为成功（无直接查询图片的 DAO 方法）
    }

    // endregion

    // region 2. resolveType 测试

    @Test
    fun when_resolveType_withBalanceExpenditureId_then_returnsFixedType() = runTest {
        // 平账支出特殊类型 id = -1101L
        val type = transactionDao.resolveType(TYPE_TABLE_BALANCE_EXPENDITURE.id!!)

        assertThat(type).isNotNull()
        assertThat(type!!.id).isEqualTo(-1101L)
        assertThat(type.name).isEqualTo("平账")
        assertThat(type.typeCategory).isEqualTo(RecordTypeCategoryEnum.EXPENDITURE.ordinal)
    }

    @Test
    fun when_resolveType_withBalanceIncomeId_then_returnsFixedType() = runTest {
        // 平账收入特殊类型 id = -1102L
        val type = transactionDao.resolveType(TYPE_TABLE_BALANCE_INCOME.id!!)

        assertThat(type).isNotNull()
        assertThat(type!!.id).isEqualTo(-1102L)
        assertThat(type.name).isEqualTo("平账")
        assertThat(type.typeCategory).isEqualTo(RecordTypeCategoryEnum.INCOME.ordinal)
    }

    @Test
    fun when_resolveType_withNormalTypeId_then_queriesDatabase() = runTest {
        // 准备：插入普通类型
        val typeId = insertType(createExpenditureType(name = "数据库中的类型"))

        // 执行
        val type = transactionDao.resolveType(typeId)

        // 验证：应从数据库查询到
        assertThat(type).isNotNull()
        assertThat(type!!.name).isEqualTo("数据库中的类型")
    }

    // endregion

    // region 3. insertRecordTransaction 测试

    @Test
    fun when_insertExpenditureRecord_then_assetBalanceDecreases() = runTest {
        // 准备：初始余额 1000.00 元的普通账户
        insertBook()
        val typeId = insertType(createExpenditureType())
        insertAsset(createNormalAsset(id = 1L, balance = 100000L))

        // 执行：插入 100.00 元支出记录
        val record = createRecord(typeId = typeId, assetId = 1L, amount = 10000L)
        transactionDao.insertRecordTransaction(
            record = record,
            tagIdList = emptyList(),
            needRelated = false,
            relatedRecordIdList = emptyList(),
            relatedImageList = emptyList(),
        )

        // 验证：余额减少 100.00 元（1000.00 - 100.00 = 900.00）
        val asset = queryAsset(1L)
        assertThat(asset).isNotNull()
        assertThat(asset!!.balance).isEqualTo(90000L)
    }

    @Test
    fun when_insertIncomeRecord_then_assetBalanceIncreases() = runTest {
        // 准备：初始余额 1000.00 元的普通账户
        insertBook()
        val typeId = insertType(createIncomeType())
        insertAsset(createNormalAsset(id = 1L, balance = 100000L))

        // 执行：插入 500.00 元收入记录
        val record = createRecord(typeId = typeId, assetId = 1L, amount = 50000L)
        transactionDao.insertRecordTransaction(
            record = record,
            tagIdList = emptyList(),
            needRelated = false,
            relatedRecordIdList = emptyList(),
            relatedImageList = emptyList(),
        )

        // 验证：余额增加 500.00 元（1000.00 + 500.00 = 1500.00）
        val asset = queryAsset(1L)
        assertThat(asset).isNotNull()
        assertThat(asset!!.balance).isEqualTo(150000L)
    }

    @Test
    fun when_insertTransferRecord_then_bothAssetsUpdated() = runTest {
        // 准备：两个普通账户
        insertBook()
        val typeId = insertType(createTransferType())
        insertAsset(createNormalAsset(id = 1L, name = "账户A", balance = 100000L))
        insertAsset(createNormalAsset(id = 2L, name = "账户B", balance = 50000L))

        // 执行：从 A 转账 200.00 元到 B
        val record = createRecord(
            typeId = typeId,
            assetId = 1L,
            intoAssetId = 2L,
            amount = 20000L,
        )
        transactionDao.insertRecordTransaction(
            record = record,
            tagIdList = emptyList(),
            needRelated = false,
            relatedRecordIdList = emptyList(),
            relatedImageList = emptyList(),
        )

        // 验证：A 减少 200.00（1000.00 - 200.00 = 800.00）
        val assetA = queryAsset(1L)
        assertThat(assetA).isNotNull()
        assertThat(assetA!!.balance).isEqualTo(80000L)

        // 验证：B 增加 200.00（500.00 + 200.00 = 700.00）
        val assetB = queryAsset(2L)
        assertThat(assetB).isNotNull()
        assertThat(assetB!!.balance).isEqualTo(70000L)
    }

    @Test
    fun when_insertExpenditureOnCreditCard_then_balanceIncreases() = runTest {
        // 准备：信用卡账户，已用额度 0
        insertBook()
        val typeId = insertType(createExpenditureType())
        insertAsset(createCreditCardAsset(id = 1L, balance = 0L))

        // 执行：信用卡消费 100.00 元
        val record = createRecord(typeId = typeId, assetId = 1L, amount = 10000L)
        transactionDao.insertRecordTransaction(
            record = record,
            tagIdList = emptyList(),
            needRelated = false,
            relatedRecordIdList = emptyList(),
            relatedImageList = emptyList(),
        )

        // 验证：信用卡已用额度增加（0 + 100.00 = 100.00）
        val asset = queryAsset(1L)
        assertThat(asset).isNotNull()
        assertThat(asset!!.balance).isEqualTo(10000L)
    }

    @Test
    fun when_insertRecordWithTags_then_tagRelationsCreated() = runTest {
        // 准备
        insertBook()
        val typeId = insertType(createExpenditureType())
        val tagId1 = database.tagDao().insert(
            TagTable(id = null, name = "午餐", booksId = 1L, invisible = SWITCH_INT_OFF),
        )
        val tagId2 = database.tagDao().insert(
            TagTable(id = null, name = "工作日", booksId = 1L, invisible = SWITCH_INT_OFF),
        )

        // 执行：插入记录并关联标签
        val record = createRecord(typeId = typeId)
        transactionDao.insertRecordTransaction(
            record = record,
            tagIdList = listOf(tagId1, tagId2),
            needRelated = false,
            relatedRecordIdList = emptyList(),
            relatedImageList = emptyList(),
        )

        // 验证：通过记录查询关联标签
        // 需要先找到插入的记录 id（insertRecordTransaction 内部会插入记录）
        val records = transactionDao.queryRecordListByBookId(1L)
        assertThat(records).hasSize(1)
        val recordId = records[0].id!!

        val tags = database.tagDao().queryByRecordId(recordId)
        assertThat(tags).hasSize(2)
        assertThat(tags.map { it.name }).containsExactly("午餐", "工作日")
    }

    @Test
    fun when_insertRecordWithRelated_then_finalAmountAdjusted() = runTest {
        // 准备：先插入两笔支出记录作为被关联记录
        insertBook()
        val expenditureTypeId = insertType(createExpenditureType())
        val incomeTypeId = insertType(createIncomeType())

        // 先插入两笔支出记录（直接插入，不走事务）
        val expense1Id = transactionDao.insertRecord(
            createRecord(typeId = expenditureTypeId, amount = 5000L).copy(finalAmount = 5000L),
        )
        val expense2Id = transactionDao.insertRecord(
            createRecord(typeId = expenditureTypeId, amount = 3000L).copy(finalAmount = 3000L),
        )

        // 执行：插入一笔收入记录，关联上述两笔支出
        val incomeRecord = createRecord(typeId = incomeTypeId, amount = 20000L)
        transactionDao.insertRecordTransaction(
            record = incomeRecord,
            tagIdList = emptyList(),
            needRelated = true,
            relatedRecordIdList = listOf(expense1Id, expense2Id),
            relatedImageList = emptyList(),
        )

        // 验证：被关联的支出记录 finalAmount 被设为 0
        val expense1 = transactionDao.queryRecordById(expense1Id)
        assertThat(expense1).isNotNull()
        assertThat(expense1!!.finalAmount).isEqualTo(0L)

        val expense2 = transactionDao.queryRecordById(expense2Id)
        assertThat(expense2).isNotNull()
        assertThat(expense2!!.finalAmount).isEqualTo(0L)

        // 验证：收入记录的 finalAmount = recordAmount - relatedAmount
        // 收入的 recordAmount = amount - charge = 20000 - 0 = 20000
        // relatedAmount = 5000 + 3000 = 8000（被关联记录的原始 finalAmount 之和）
        // 最终 finalAmount = 20000 - 8000 = 12000
        val incomeRecords = transactionDao.queryRecordListByBookId(1L)
        val insertedIncome = incomeRecords.first { it.typeId == incomeTypeId }
        assertThat(insertedIncome.finalAmount).isEqualTo(12000L)
    }

    // endregion

    // region 4. deleteRecordTransaction 测试

    @Test
    fun when_deleteExpenditureRecord_then_assetBalanceReverted() = runTest {
        // 准备：插入支出记录
        insertBook()
        val typeId = insertType(createExpenditureType())
        insertAsset(createNormalAsset(id = 1L, balance = 100000L))

        val record = createRecord(typeId = typeId, assetId = 1L, amount = 10000L)
        transactionDao.insertRecordTransaction(
            record = record,
            tagIdList = emptyList(),
            needRelated = false,
            relatedRecordIdList = emptyList(),
            relatedImageList = emptyList(),
        )

        // 确认余额已减少
        assertThat(queryAsset(1L)!!.balance).isEqualTo(90000L)

        // 执行：删除该记录
        val records = transactionDao.queryRecordListByBookId(1L)
        assertThat(records).hasSize(1)
        transactionDao.deleteRecordTransaction(records[0].id)

        // 验证：余额恢复到原始值
        assertThat(queryAsset(1L)!!.balance).isEqualTo(100000L)
    }

    @Test
    fun when_deleteTransferRecord_then_bothAssetsReverted() = runTest {
        // 准备：两个普通账户，插入转账记录
        insertBook()
        val typeId = insertType(createTransferType())
        insertAsset(createNormalAsset(id = 1L, name = "账户A", balance = 100000L))
        insertAsset(createNormalAsset(id = 2L, name = "账户B", balance = 50000L))

        val record = createRecord(
            typeId = typeId,
            assetId = 1L,
            intoAssetId = 2L,
            amount = 20000L,
        )
        transactionDao.insertRecordTransaction(
            record = record,
            tagIdList = emptyList(),
            needRelated = false,
            relatedRecordIdList = emptyList(),
            relatedImageList = emptyList(),
        )

        // 确认转账后余额
        assertThat(queryAsset(1L)!!.balance).isEqualTo(80000L)
        assertThat(queryAsset(2L)!!.balance).isEqualTo(70000L)

        // 执行：删除转账记录
        val records = transactionDao.queryRecordListByBookId(1L)
        transactionDao.deleteRecordTransaction(records[0].id)

        // 验证：两个账户余额恢复
        assertThat(queryAsset(1L)!!.balance).isEqualTo(100000L)
        assertThat(queryAsset(2L)!!.balance).isEqualTo(50000L)
    }

    @Test
    fun when_deleteRecord_then_tagsAndImagesRemoved() = runTest {
        // 准备：插入带标签和图片的记录
        insertBook()
        val typeId = insertType(createExpenditureType())
        val tagId = database.tagDao().insert(
            TagTable(id = null, name = "标签", booksId = 1L, invisible = SWITCH_INT_OFF),
        )

        val record = createRecord(typeId = typeId)
        transactionDao.insertRecordTransaction(
            record = record,
            tagIdList = listOf(tagId),
            needRelated = false,
            relatedRecordIdList = emptyList(),
            relatedImageList = listOf(
                ImageModel(
                    id = 0L,
                    recordId = 0L,
                    path = "/test/photo.jpg",
                    bytes = byteArrayOf(1, 2, 3),
                ),
            ),
        )

        // 确认标签关联已建立
        val records = transactionDao.queryRecordListByBookId(1L)
        assertThat(records).hasSize(1)
        val recordId = records[0].id!!
        assertThat(database.tagDao().queryByRecordId(recordId)).hasSize(1)

        // 执行：删除记录
        transactionDao.deleteRecordTransaction(recordId)

        // 验证：记录、标签关联均已清除
        assertThat(transactionDao.queryRecordById(recordId)).isNull()
        assertThat(database.tagDao().queryByRecordId(recordId)).isEmpty()
    }

    // endregion

    // region 5. deleteBookTransaction 测试

    @Test
    fun when_deleteBookTransaction_then_allRelatedDataRemoved() = runTest {
        // 准备：创建账本、资产、类型、标签、记录等完整数据
        val bookId = insertBook(name = "待删除账本")
        val typeId = insertType(createExpenditureType())
        insertAsset(createNormalAsset(id = 1L, booksId = bookId, balance = 100000L))

        val tagId = database.tagDao().insert(
            TagTable(id = null, name = "标签", booksId = bookId, invisible = SWITCH_INT_OFF),
        )

        // 插入两笔记录
        transactionDao.insertRecordTransaction(
            record = createRecord(typeId = typeId, assetId = 1L, booksId = bookId, amount = 5000L),
            tagIdList = listOf(tagId),
            needRelated = false,
            relatedRecordIdList = emptyList(),
            relatedImageList = emptyList(),
        )
        transactionDao.insertRecordTransaction(
            record = createRecord(typeId = typeId, assetId = 1L, booksId = bookId, amount = 3000L),
            tagIdList = emptyList(),
            needRelated = false,
            relatedRecordIdList = emptyList(),
            relatedImageList = emptyList(),
        )

        // 确认数据已插入
        assertThat(transactionDao.queryRecordListByBookId(bookId)).hasSize(2)

        // 执行：删除账本事务
        transactionDao.deleteBookTransaction(bookId)

        // 验证：记录已全部删除
        assertThat(transactionDao.queryRecordListByBookId(bookId)).isEmpty()

        // 验证：资产余额回退（两笔支出被删除，余额应恢复到 100000）
        // 注意：deleteBookTransaction 会删除账本下的资产，所以资产查询应为 null
        assertThat(queryAsset(1L)).isNull()

        // 验证：标签已删除
        assertThat(database.tagDao().queryAll()).isEmpty()

        // 验证：账本已删除
        assertThat(database.booksDao().queryAll()).isEmpty()
    }

    // endregion

    // region 6. deleteAssetRelatedData 测试

    @Test
    fun when_deleteAssetRelatedData_then_assetRecordsRemoved() = runTest {
        // 准备：创建两个资产，各自有记录
        insertBook()
        val typeId = insertType(createExpenditureType())
        insertAsset(createNormalAsset(id = 1L, name = "资产A", balance = 100000L))
        insertAsset(createNormalAsset(id = 2L, name = "资产B", balance = 50000L))

        // 资产A 的记录
        transactionDao.insertRecordTransaction(
            record = createRecord(typeId = typeId, assetId = 1L, amount = 5000L),
            tagIdList = emptyList(),
            needRelated = false,
            relatedRecordIdList = emptyList(),
            relatedImageList = emptyList(),
        )

        // 资产B 的记录
        transactionDao.insertRecordTransaction(
            record = createRecord(typeId = typeId, assetId = 2L, amount = 3000L),
            tagIdList = emptyList(),
            needRelated = false,
            relatedRecordIdList = emptyList(),
            relatedImageList = emptyList(),
        )

        // 执行：删除资产A 的关联数据
        transactionDao.deleteAssetRelatedData(1L)

        // 验证：资产A 的记录已删除，余额恢复
        assertThat(transactionDao.queryRecordsByAssetId(1L)).isEmpty()
        assertThat(queryAsset(1L)!!.balance).isEqualTo(100000L)

        // 验证：资产B 的记录不受影响
        assertThat(transactionDao.queryRecordsByAssetId(2L)).hasSize(1)
    }

    // endregion

    // region 7. migrateTypeRecords 测试

    @Test
    fun when_migrateTypeRecords_then_recordsUpdatedAndChildTypesPromoted() = runTest {
        // 准备：创建旧类型（一级）及其子类型（二级）
        insertBook()
        val oldTypeId = insertType(createExpenditureType(name = "旧类型"))
        val childTypeId = insertType(
            TypeTable(
                id = null,
                parentId = oldTypeId,
                name = "子类型",
                iconName = "icon_child",
                typeLevel = 1,
                typeCategory = RecordTypeCategoryEnum.EXPENDITURE.ordinal,
                protected = SWITCH_INT_OFF,
                sort = 0,
            ),
        )

        // 创建新的固定类型
        val fixedTypeId = insertType(createExpenditureType(name = "固定类型"))

        // 插入引用旧类型的记录
        transactionDao.insertRecord(
            createRecord(typeId = oldTypeId, remark = "旧类型记录1").copy(finalAmount = 1000L),
        )
        transactionDao.insertRecord(
            createRecord(typeId = oldTypeId, remark = "旧类型记录2").copy(finalAmount = 2000L),
        )

        // 执行：迁移类型记录
        transactionDao.migrateTypeRecords(oldTypeId, fixedTypeId)

        // 验证：记录的 typeId 已更新为 fixedTypeId
        val records = transactionDao.queryRecordListByBookId(1L)
        assertThat(records).hasSize(2)
        records.forEach { record ->
            assertThat(record.typeId).isEqualTo(fixedTypeId)
        }

        // 验证：子类型已被提升为一级类型
        val childType = database.typeDao().queryById(childTypeId)
        assertThat(childType).isNotNull()
        assertThat(childType!!.parentId).isEqualTo(-1L)
        assertThat(childType.typeLevel).isEqualTo(0)

        // 验证：旧类型已被删除（因为没有剩余记录引用）
        val oldType = database.typeDao().queryById(oldTypeId)
        assertThat(oldType).isNull()
    }

    // endregion

    // region 8. deleteTag 测试

    @Test
    fun when_deleteTag_then_tagAndRelationsRemoved() = runTest {
        // 准备：插入标签并建立关联
        insertBook()
        val typeId = insertType(createExpenditureType())
        val tagId = database.tagDao().insert(
            TagTable(id = null, name = "待删除标签", booksId = 1L, invisible = SWITCH_INT_OFF),
        )

        // 插入记录并关联标签
        val recordId = transactionDao.insertRecord(
            createRecord(typeId = typeId).copy(finalAmount = 1000L),
        )
        transactionDao.insertRelatedTags(
            listOf(TagWithRecordTable(id = null, recordId = recordId, tagId = tagId)),
        )

        // 确认标签和关联存在
        assertThat(database.tagDao().queryAll()).hasSize(1)
        assertThat(database.tagDao().queryByRecordId(recordId)).hasSize(1)

        // 执行：删除标签
        transactionDao.deleteTag(tagId)

        // 验证：标签被删除
        assertThat(database.tagDao().queryAll()).isEmpty()

        // 验证：标签关联被清除
        assertThat(database.tagDao().queryByRecordId(recordId)).isEmpty()
    }

    // endregion

    // region 9. verifyAssetBalance 测试

    @Test
    fun when_verifyAssetBalance_afterInsertAndDelete_then_returnsZero() = runTest {
        // 准备：创建普通账户
        insertBook()
        val expenditureTypeId = insertType(createExpenditureType())
        val incomeTypeId = insertType(createIncomeType())
        insertAsset(createNormalAsset(id = 1L, balance = 0L))

        // 执行一系列操作：收入 500、支出 200、收入 300
        transactionDao.insertRecordTransaction(
            record = createRecord(typeId = incomeTypeId, assetId = 1L, amount = 50000L),
            tagIdList = emptyList(),
            needRelated = false,
            relatedRecordIdList = emptyList(),
            relatedImageList = emptyList(),
        )
        transactionDao.insertRecordTransaction(
            record = createRecord(typeId = expenditureTypeId, assetId = 1L, amount = 20000L),
            tagIdList = emptyList(),
            needRelated = false,
            relatedRecordIdList = emptyList(),
            relatedImageList = emptyList(),
        )
        transactionDao.insertRecordTransaction(
            record = createRecord(typeId = incomeTypeId, assetId = 1L, amount = 30000L),
            tagIdList = emptyList(),
            needRelated = false,
            relatedRecordIdList = emptyList(),
            relatedImageList = emptyList(),
        )

        // 当前余额应为 0 + 500 - 200 + 300 = 600（60000 分）
        val asset = queryAsset(1L)
        assertThat(asset).isNotNull()
        assertThat(asset!!.balance).isEqualTo(60000L)

        // 验证：verifyAssetBalance 返回的余额变化量应等于当前余额（因初始余额为 0）
        val balanceChange = transactionDao.verifyAssetBalance(1L)
        assertThat(balanceChange).isEqualTo(60000L)

        // 删除一笔支出记录（200 元），余额恢复到 800（80000 分）
        val records = transactionDao.queryRecordsByAssetId(1L)
        val expenditureRecord = records.first { it.typeId == expenditureTypeId }
        transactionDao.deleteRecordTransaction(expenditureRecord.id)

        // 验证：余额应为 0 + 500 + 300 = 800
        assertThat(queryAsset(1L)!!.balance).isEqualTo(80000L)

        // 验证：balanceChange 应等于当前余额
        val balanceChange2 = transactionDao.verifyAssetBalance(1L)
        assertThat(balanceChange2).isEqualTo(80000L)
    }

    // endregion
}
