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
import cn.wj.android.cashbook.core.common.SWITCH_INT_OFF
import cn.wj.android.cashbook.core.common.SWITCH_INT_ON
import cn.wj.android.cashbook.core.database.CashbookDatabase
import cn.wj.android.cashbook.core.database.table.AssetTable
import cn.wj.android.cashbook.core.database.table.BooksTable
import cn.wj.android.cashbook.core.database.table.ImageWithRelatedTable
import cn.wj.android.cashbook.core.database.table.RecordTable
import cn.wj.android.cashbook.core.database.table.RecordWithRelatedTable
import cn.wj.android.cashbook.core.database.table.TagTable
import cn.wj.android.cashbook.core.database.table.TagWithRecordTable
import cn.wj.android.cashbook.core.database.table.TypeTable
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * RecordDao 数据库操作测试
 *
 * 验证 RecordDao 中各种查询、更新、删除方法的 SQL 行为是否正确，
 * 包括日期范围过滤、JOIN 查询、子类型子查询、标签关联、关键字匹配等场景。
 */
@RunWith(AndroidJUnit4::class)
class RecordDaoTest {

    private lateinit var database: CashbookDatabase
    private lateinit var recordDao: RecordDao
    private lateinit var transactionDao: TransactionDao
    private lateinit var typeDao: TypeDao
    private lateinit var assetDao: AssetDao
    private lateinit var tagDao: TagDao
    private lateinit var booksDao: BooksDao

    /** 测试用账本 id */
    private var testBookId: Long = 0L

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            CashbookDatabase::class.java,
        ).allowMainThreadQueries().build()
        recordDao = database.recordDao()
        transactionDao = database.transactionDao()
        typeDao = database.typeDao()
        assetDao = database.assetDao()
        tagDao = database.tagDao()
        booksDao = database.booksDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    // region 辅助方法

    /** 创建测试用账本并返回其 id */
    private suspend fun createTestBook(): Long {
        return booksDao.insert(
            BooksTable(
                id = null,
                name = "测试账本",
                description = "",
                bgUri = "",
                modifyTime = System.currentTimeMillis(),
            ),
        )
    }

    /** 创建测试用类型 */
    private fun createType(
        id: Long? = null,
        parentId: Long = -1L,
        name: String = "测试类型",
        iconName: String = "icon_test",
        typeLevel: Int = 0,
        typeCategory: Int = RecordTypeCategoryEnum.EXPENDITURE.ordinal,
        protected_: Int = 0,
        sort: Int = 0,
    ) = TypeTable(id, parentId, name, iconName, typeLevel, typeCategory, protected_, sort)

    /** 创建测试用记录 */
    private fun createRecord(
        id: Long? = null,
        typeId: Long = 1L,
        assetId: Long = -1L,
        intoAssetId: Long = -1L,
        booksId: Long = testBookId,
        amount: Long = 10000L,
        finalAmount: Long = 10000L,
        concessions: Long = 0L,
        charge: Long = 0L,
        remark: String = "",
        reimbursable: Int = SWITCH_INT_OFF,
        recordTime: Long = 1000000L,
    ) = RecordTable(
        id, typeId, assetId, intoAssetId, booksId,
        amount, finalAmount, concessions, charge, remark, reimbursable, recordTime,
    )

    /** 创建测试用资产 */
    private fun createAsset(
        id: Long? = null,
        booksId: Long = testBookId,
        name: String = "测试资产",
        balance: Long = 0L,
        totalAmount: Long = 0L,
        billingDate: String = "",
        repaymentDate: String = "",
        type: Int = 0,
        classification: Int = 0,
        invisible: Int = SWITCH_INT_OFF,
        openBank: String = "",
        cardNo: String = "",
        remark: String = "",
        sort: Int = 0,
        modifyTime: Long = System.currentTimeMillis(),
    ) = AssetTable(
        id, booksId, name, balance, totalAmount, billingDate, repaymentDate,
        type, classification, invisible, openBank, cardNo, remark, sort, modifyTime,
    )

    /** 插入记录（直接使用 transactionDao.insertRecord 避免触发复杂的事务逻辑） */
    private suspend fun insertRecord(record: RecordTable): Long {
        return transactionDao.insertRecord(record)
    }

    // endregion

    // region 1. queryById

    @Test
    fun when_queryById_then_returnsRecord() = runTest {
        testBookId = createTestBook()
        val typeId = typeDao.insertType(createType())

        val recordId = insertRecord(
            createRecord(typeId = typeId, amount = 5000L, remark = "午餐"),
        )

        val result = recordDao.queryById(recordId)
        assertThat(result).isNotNull()
        assertThat(result!!.amount).isEqualTo(5000L)
        assertThat(result.remark).isEqualTo("午餐")
    }

    @Test
    fun when_queryByNonExistentId_then_returnsNull() = runTest {
        val result = recordDao.queryById(999L)
        assertThat(result).isNull()
    }

    // endregion

    // region 3. queryRelatedById

    @Test
    fun when_queryRelatedById_then_returnsRelatedRecords() = runTest {
        testBookId = createTestBook()
        val typeId = typeDao.insertType(createType())

        // 插入主记录和两条关联记录
        val mainRecordId = insertRecord(createRecord(typeId = typeId, remark = "主记录"))
        val relatedId1 = insertRecord(createRecord(typeId = typeId, remark = "关联1"))
        val relatedId2 = insertRecord(createRecord(typeId = typeId, remark = "关联2"))
        // 另一条不关联的记录
        insertRecord(createRecord(typeId = typeId, remark = "无关记录"))

        // 建立关联关系
        transactionDao.insertRelatedRecord(
            listOf(
                RecordWithRelatedTable(id = null, recordId = mainRecordId, relatedRecordId = relatedId1),
                RecordWithRelatedTable(id = null, recordId = mainRecordId, relatedRecordId = relatedId2),
            ),
        )

        // 查询主记录的关联记录
        val result = recordDao.queryRelatedById(mainRecordId)
        assertThat(result).hasSize(2)
        val remarks = result.map { it.remark }
        assertThat(remarks).containsExactly("关联1", "关联2")
    }

    // endregion

    // region 4. queryByBooksIdAfterDate

    @Test
    fun when_queryByBooksIdAfterDate_then_filtersCorrectly() = runTest {
        testBookId = createTestBook()
        val typeId = typeDao.insertType(createType())
        val cutoffTime = 5000L

        // 在截止时间之前的记录
        insertRecord(createRecord(typeId = typeId, recordTime = 3000L, remark = "早期记录"))
        // 刚好等于截止时间的记录（应包含，>= 语义）
        insertRecord(createRecord(typeId = typeId, recordTime = 5000L, remark = "临界记录"))
        // 在截止时间之后的记录
        insertRecord(createRecord(typeId = typeId, recordTime = 8000L, remark = "晚期记录"))

        val result = recordDao.queryByBooksIdAfterDate(testBookId, cutoffTime)
        assertThat(result).hasSize(2)
        val remarks = result.map { it.remark }
        assertThat(remarks).containsExactly("临界记录", "晚期记录")
    }

    // endregion

    // region 5. queryByBooksIdBetweenDate

    @Test
    fun when_queryByBooksIdBetweenDate_then_filtersCorrectly() = runTest {
        testBookId = createTestBook()
        val typeId = typeDao.insertType(createType())
        val startDate = 5000L
        val endDate = 10000L

        // 在范围之前
        insertRecord(createRecord(typeId = typeId, recordTime = 3000L, remark = "范围前"))
        // 等于起始时间（包含，>= 语义）
        insertRecord(createRecord(typeId = typeId, recordTime = 5000L, remark = "起始边界"))
        // 范围中间
        insertRecord(createRecord(typeId = typeId, recordTime = 7000L, remark = "范围内"))
        // 等于结束时间（不包含，< 语义）
        insertRecord(createRecord(typeId = typeId, recordTime = 10000L, remark = "结束边界"))
        // 超出范围
        insertRecord(createRecord(typeId = typeId, recordTime = 15000L, remark = "范围后"))

        val result = recordDao.queryByBooksIdBetweenDate(testBookId, startDate, endDate)
        assertThat(result).hasSize(2)
        val remarks = result.map { it.remark }
        assertThat(remarks).containsExactly("起始边界", "范围内")
    }

    // endregion

    // region 6. queryViewsBetweenDate (JOIN 查询)

    @Test
    fun when_queryViewsBetweenDate_then_returnsJoinedData() = runTest {
        testBookId = createTestBook()

        // 插入类型
        val typeId = typeDao.insertType(
            createType(name = "餐饮", iconName = "icon_food", typeCategory = RecordTypeCategoryEnum.EXPENDITURE.ordinal),
        )

        // 插入资产
        assetDao.insert(createAsset(name = "银行卡", classification = 1))
        val assetId = 1L // autoGenerate 从 1 开始
        assetDao.insert(createAsset(name = "现金", classification = 0))
        val intoAssetId = 2L

        // 插入记录（在日期范围内）
        insertRecord(
            createRecord(
                typeId = typeId,
                assetId = assetId,
                intoAssetId = intoAssetId,
                amount = 3000L,
                finalAmount = 3000L,
                charge = 100L,
                concessions = 50L,
                remark = "午餐",
                reimbursable = SWITCH_INT_ON,
                recordTime = 7000L,
            ),
        )

        // 插入范围外记录
        insertRecord(
            createRecord(
                typeId = typeId,
                assetId = assetId,
                amount = 2000L,
                recordTime = 15000L,
                remark = "范围外",
            ),
        )

        val result = recordDao.queryViewsBetweenDate(testBookId, 5000L, 10000L)
        assertThat(result).hasSize(1)

        val view = result[0]
        // 验证 JOIN 字段
        assertThat(view.typeName).isEqualTo("餐饮")
        assertThat(view.typeIconResName).isEqualTo("icon_food")
        assertThat(view.typeCategory).isEqualTo(RecordTypeCategoryEnum.EXPENDITURE.ordinal)
        assertThat(view.assetName).isEqualTo("银行卡")
        assertThat(view.assetClassification).isEqualTo(1)
        assertThat(view.relatedAssetName).isEqualTo("现金")
        assertThat(view.relatedAssetClassification).isEqualTo(0)
        // 验证记录字段
        assertThat(view.amount).isEqualTo(3000L)
        assertThat(view.finalAmount).isEqualTo(3000L)
        assertThat(view.charges).isEqualTo(100L)
        assertThat(view.concessions).isEqualTo(50L)
        assertThat(view.remark).isEqualTo("午餐")
        assertThat(view.reimbursable).isEqualTo(SWITCH_INT_ON)
    }

    // endregion

    // region 7. queryReimburseByBooksIdAfterDate

    @Test
    fun when_queryReimburseByBooksIdAfterDate_then_returnsOnlyReimbursable() = runTest {
        testBookId = createTestBook()
        val typeId = typeDao.insertType(createType())
        val cutoffTime = 5000L

        // 可报销的记录（在截止时间之后）
        insertRecord(
            createRecord(typeId = typeId, reimbursable = SWITCH_INT_ON, recordTime = 8000L, remark = "可报销"),
        )
        // 不可报销的记录（在截止时间之后）
        insertRecord(
            createRecord(typeId = typeId, reimbursable = SWITCH_INT_OFF, recordTime = 8000L, remark = "不可报销"),
        )
        // 可报销但在截止时间之前
        insertRecord(
            createRecord(typeId = typeId, reimbursable = SWITCH_INT_ON, recordTime = 3000L, remark = "过早的可报销"),
        )

        val result = recordDao.queryReimburseByBooksIdAfterDate(testBookId, cutoffTime)
        assertThat(result).hasSize(1)
        assertThat(result[0].remark).isEqualTo("可报销")
    }

    // endregion

    // region 8. query (JOIN 查询)

    @Test
    fun when_query_then_returnsJoinedData() = runTest {
        testBookId = createTestBook()

        // 插入类型
        val typeId = typeDao.insertType(
            createType(name = "交通", iconName = "icon_transport", typeCategory = RecordTypeCategoryEnum.EXPENDITURE.ordinal),
        )

        // 插入资产
        assetDao.insert(createAsset(name = "支付宝", classification = 2))
        val assetId = 1L

        // 插入记录（无转入资产）
        insertRecord(
            createRecord(
                typeId = typeId,
                assetId = assetId,
                intoAssetId = -1L,
                amount = 1500L,
                remark = "地铁",
                recordTime = 7000L,
            ),
        )

        val result = recordDao.query(testBookId)
        assertThat(result).hasSize(1)

        val view = result[0]
        assertThat(view.typeName).isEqualTo("交通")
        assertThat(view.typeIconResName).isEqualTo("icon_transport")
        assertThat(view.assetName).isEqualTo("支付宝")
        assertThat(view.assetClassification).isEqualTo(2)
        // 转入资产为 -1L，LEFT JOIN 应为 null
        assertThat(view.relatedAssetName).isNull()
        assertThat(view.relatedAssetClassification).isNull()
    }

    // endregion

    // region 9. queryRecordByAssetId

    @Test
    fun when_queryRecordByAssetId_then_includesBothAssetAndIntoAsset() = runTest {
        testBookId = createTestBook()
        val typeId = typeDao.insertType(createType())
        val targetAssetId = 100L

        // asset_id 匹配的记录
        insertRecord(
            createRecord(typeId = typeId, assetId = targetAssetId, intoAssetId = -1L, remark = "从该资产支出"),
        )
        // into_asset_id 匹配的记录
        insertRecord(
            createRecord(typeId = typeId, assetId = -1L, intoAssetId = targetAssetId, remark = "转入该资产"),
        )
        // 都不匹配的记录
        insertRecord(
            createRecord(typeId = typeId, assetId = 200L, intoAssetId = 300L, remark = "无关记录"),
        )

        val result = recordDao.queryRecordByAssetId(
            booksId = testBookId,
            assetId = targetAssetId,
            pageNum = 0,
            pageSize = 10,
        )
        assertThat(result).hasSize(2)
        val remarks = result.map { it.remark }
        assertThat(remarks).containsExactly("从该资产支出", "转入该资产")
    }

    // endregion

    // region 10 & 11. queryRecordByTypeId vs queryRecordByTypeIdExact

    @Test
    fun when_queryRecordByTypeId_then_includesChildTypes() = runTest {
        testBookId = createTestBook()

        // 插入父类型
        val parentTypeId = typeDao.insertType(createType(name = "餐饮", typeLevel = 0))
        // 插入子类型（parentId 指向父类型）
        val childTypeId = typeDao.insertType(createType(name = "早餐", parentId = parentTypeId, typeLevel = 1))

        // 使用父类型的记录
        insertRecord(createRecord(typeId = parentTypeId, remark = "父类型记录"))
        // 使用子类型的记录
        insertRecord(createRecord(typeId = childTypeId, remark = "子类型记录"))
        // 使用其他类型的记录
        val otherTypeId = typeDao.insertType(createType(name = "其他"))
        insertRecord(createRecord(typeId = otherTypeId, remark = "其他类型记录"))

        // queryRecordByTypeId 应包含父类型和子类型的记录
        val result = recordDao.queryRecordByTypeId(
            booksId = testBookId,
            typeId = parentTypeId,
            pageNum = 0,
            pageSize = 10,
        )
        assertThat(result).hasSize(2)
        val remarks = result.map { it.remark }
        assertThat(remarks).containsExactly("父类型记录", "子类型记录")
    }

    @Test
    fun when_queryRecordByTypeIdExact_then_excludesChildTypes() = runTest {
        testBookId = createTestBook()

        // 插入父类型
        val parentTypeId = typeDao.insertType(createType(name = "餐饮", typeLevel = 0))
        // 插入子类型
        val childTypeId = typeDao.insertType(createType(name = "早餐", parentId = parentTypeId, typeLevel = 1))

        // 使用父类型的记录
        insertRecord(createRecord(typeId = parentTypeId, remark = "父类型记录"))
        // 使用子类型的记录
        insertRecord(createRecord(typeId = childTypeId, remark = "子类型记录"))

        // queryRecordByTypeIdExact 应只包含精确匹配父类型的记录
        val result = recordDao.queryRecordByTypeIdExact(
            booksId = testBookId,
            typeId = parentTypeId,
            pageNum = 0,
            pageSize = 10,
        )
        assertThat(result).hasSize(1)
        assertThat(result[0].remark).isEqualTo("父类型记录")
    }

    // endregion

    // region 12. queryRecordByTagId

    @Test
    fun when_queryRecordByTagId_then_returnsTaggedRecords() = runTest {
        testBookId = createTestBook()
        val typeId = typeDao.insertType(createType())

        // 插入标签
        val tagId = tagDao.insert(TagTable(id = null, name = "工作", booksId = testBookId, invisible = SWITCH_INT_OFF))

        // 插入记录
        val taggedRecordId = insertRecord(createRecord(typeId = typeId, remark = "有标签记录"))
        val untaggedRecordId = insertRecord(createRecord(typeId = typeId, remark = "无标签记录"))

        // 建立标签关联
        transactionDao.insertRelatedTags(
            listOf(
                TagWithRecordTable(id = null, recordId = taggedRecordId, tagId = tagId),
            ),
        )

        val result = recordDao.queryRecordByTagId(
            booksId = testBookId,
            tagId = tagId,
            pageNum = 0,
            pageSize = 10,
        )
        assertThat(result).hasSize(1)
        assertThat(result[0].remark).isEqualTo("有标签记录")
    }

    // endregion

    // region 13. queryRecordByKeyword

    @Test
    fun when_queryRecordByKeyword_then_matchesPartialRemark() = runTest {
        testBookId = createTestBook()
        val typeId = typeDao.insertType(createType())

        insertRecord(createRecord(typeId = typeId, remark = "今天去星巴克喝咖啡"))
        insertRecord(createRecord(typeId = typeId, remark = "超市购物"))
        insertRecord(createRecord(typeId = typeId, remark = "星巴克外卖"))

        // 搜索"星巴克"应匹配两条
        val result = recordDao.queryRecordByKeyword(
            booksId = testBookId,
            keyword = "星巴克",
            pageNum = 0,
            pageSize = 10,
        )
        assertThat(result).hasSize(2)
        val remarks = result.map { it.remark }
        assertThat(remarks).containsExactly("今天去星巴克喝咖啡", "星巴克外卖")
    }

    // endregion

    // region 14. queryByIds

    @Test
    fun when_queryByIds_then_returnsMatchingRecords() = runTest {
        testBookId = createTestBook()
        val typeId = typeDao.insertType(createType())

        val id1 = insertRecord(createRecord(typeId = typeId, remark = "记录1"))
        val id2 = insertRecord(createRecord(typeId = typeId, remark = "记录2"))
        val id3 = insertRecord(createRecord(typeId = typeId, remark = "记录3"))

        // 只查询 id1 和 id3
        val result = recordDao.queryByIds(listOf(id1, id3))
        assertThat(result).hasSize(2)
        val remarks = result.map { it.remark }
        assertThat(remarks).containsExactly("记录1", "记录3")
    }

    // endregion

    // region 15. queryByTypeCategory

    @Test
    fun when_queryByTypeCategory_then_returnsMatchingRecords() = runTest {
        testBookId = createTestBook()

        // 插入支出类型
        val expenditureTypeId = typeDao.insertType(
            createType(name = "支出类型", typeCategory = RecordTypeCategoryEnum.EXPENDITURE.ordinal),
        )
        // 插入收入类型
        val incomeTypeId = typeDao.insertType(
            createType(name = "收入类型", typeCategory = RecordTypeCategoryEnum.INCOME.ordinal),
        )

        // 插入支出记录
        insertRecord(createRecord(typeId = expenditureTypeId, remark = "支出1"))
        insertRecord(createRecord(typeId = expenditureTypeId, remark = "支出2"))
        // 插入收入记录
        insertRecord(createRecord(typeId = incomeTypeId, remark = "收入1"))

        // 按支出分类查询
        val result = recordDao.queryByTypeCategory(RecordTypeCategoryEnum.EXPENDITURE.ordinal)
        assertThat(result).hasSize(2)
        val remarks = result.map { it.remark }
        assertThat(remarks).containsExactly("支出1", "支出2")
    }

    // endregion

    // region 16. updateRecord

    @Test
    fun when_updateRecord_then_changesAreApplied() = runTest {
        testBookId = createTestBook()
        val typeId = typeDao.insertType(createType())

        val recordId = insertRecord(createRecord(typeId = typeId, amount = 5000L, remark = "原始备注"))

        // 查询出记录，修改后更新
        val original = recordDao.queryById(recordId)!!
        val updated = original.copy(amount = 8000L, remark = "修改后备注")

        val count = recordDao.updateRecord(listOf(updated))
        assertThat(count).isEqualTo(1)

        // 验证更新生效
        val result = recordDao.queryById(recordId)!!
        assertThat(result.amount).isEqualTo(8000L)
        assertThat(result.remark).isEqualTo("修改后备注")
    }

    // endregion

    // region 17. changeRecordTypeBeforeDeleteType

    @Test
    fun when_changeRecordTypeBeforeDeleteType_then_updatesTypeId() = runTest {
        testBookId = createTestBook()

        val oldTypeId = typeDao.insertType(createType(name = "旧类型"))
        val newTypeId = typeDao.insertType(createType(name = "新类型"))

        // 插入使用旧类型的记录
        val recordId1 = insertRecord(createRecord(typeId = oldTypeId, remark = "记录1"))
        val recordId2 = insertRecord(createRecord(typeId = oldTypeId, remark = "记录2"))
        // 使用新类型的记录不受影响
        val recordId3 = insertRecord(createRecord(typeId = newTypeId, remark = "记录3"))

        // 执行类型迁移
        recordDao.changeRecordTypeBeforeDeleteType(fromId = oldTypeId, toId = newTypeId)

        // 验证旧类型的记录已迁移
        assertThat(recordDao.queryById(recordId1)!!.typeId).isEqualTo(newTypeId)
        assertThat(recordDao.queryById(recordId2)!!.typeId).isEqualTo(newTypeId)
        // 新类型的记录不受影响
        assertThat(recordDao.queryById(recordId3)!!.typeId).isEqualTo(newTypeId)
    }

    // endregion

    // region 18. deleteWithAsset

    @Test
    fun when_deleteWithAsset_then_removesAssetRecords() = runTest {
        testBookId = createTestBook()
        val typeId = typeDao.insertType(createType())
        val targetAssetId = 100L

        // asset_id 匹配
        insertRecord(createRecord(typeId = typeId, assetId = targetAssetId, intoAssetId = -1L, remark = "A"))
        // into_asset_id 匹配
        insertRecord(createRecord(typeId = typeId, assetId = -1L, intoAssetId = targetAssetId, remark = "B"))
        // 都不匹配
        val survivorId = insertRecord(createRecord(typeId = typeId, assetId = 200L, intoAssetId = -1L, remark = "幸存者"))

        recordDao.deleteWithAsset(targetAssetId)

        // 匹配的记录应被删除
        val allRecords = recordDao.queryByIds(listOf(1L, 2L, survivorId))
        assertThat(allRecords).hasSize(1)
        assertThat(allRecords[0].remark).isEqualTo("幸存者")
    }

    // endregion

    // region 19. queryImagesByRecordId

    @Test
    fun when_queryImagesByRecordId_then_returnsImages() = runTest {
        testBookId = createTestBook()
        val typeId = typeDao.insertType(createType())

        val recordId = insertRecord(createRecord(typeId = typeId, remark = "有图片"))
        val otherRecordId = insertRecord(createRecord(typeId = typeId, remark = "其他记录"))

        // 插入关联图片
        val imageBytes1 = byteArrayOf(1, 2, 3)
        val imageBytes2 = byteArrayOf(4, 5, 6)
        transactionDao.insertRelatedImages(
            listOf(
                ImageWithRelatedTable(id = null, recordId = recordId, path = "/path/img1.jpg", bytes = imageBytes1),
                ImageWithRelatedTable(id = null, recordId = recordId, path = "/path/img2.png", bytes = imageBytes2),
                // 其他记录的图片
                ImageWithRelatedTable(id = null, recordId = otherRecordId, path = "/path/other.jpg", bytes = byteArrayOf(7)),
            ),
        )

        val result = recordDao.queryImagesByRecordId(recordId)
        assertThat(result).hasSize(2)
        val paths = result.map { it.path }
        assertThat(paths).containsExactly("/path/img1.jpg", "/path/img2.png")
        // 验证图片内容
        assertThat(result.first { it.path == "/path/img1.jpg" }.bytes).isEqualTo(imageBytes1)
        assertThat(result.first { it.path == "/path/img2.png" }.bytes).isEqualTo(imageBytes2)
    }

    // endregion

    // region 20. getRecordCountByAssetIdAfterTime

    @Test
    fun when_getRecordCountByAssetIdAfterTime_then_returnsCorrectCount() = runTest {
        testBookId = createTestBook()
        val typeId = typeDao.insertType(createType())
        val targetAssetId = 100L
        val cutoffTime = 5000L

        // asset_id 匹配且在截止时间之后
        insertRecord(createRecord(typeId = typeId, assetId = targetAssetId, recordTime = 8000L))
        // into_asset_id 匹配且在截止时间之后
        insertRecord(createRecord(typeId = typeId, intoAssetId = targetAssetId, recordTime = 9000L))
        // asset_id 匹配但在截止时间之前
        insertRecord(createRecord(typeId = typeId, assetId = targetAssetId, recordTime = 3000L))
        // 不匹配的资产
        insertRecord(createRecord(typeId = typeId, assetId = 200L, recordTime = 8000L))
        // 刚好等于截止时间（包含，>= 语义）
        insertRecord(createRecord(typeId = typeId, assetId = targetAssetId, recordTime = 5000L))

        val count = recordDao.getRecordCountByAssetIdAfterTime(targetAssetId, cutoffTime)
        assertThat(count).isEqualTo(3) // 8000L + 9000L + 5000L
    }

    // endregion

    // region 21. queryRecordByTypeIdBetween

    @Test
    fun when_queryRecordByTypeIdBetween_then_includesChildTypesInDateRange() = runTest {
        testBookId = createTestBook()

        // 插入父类型和子类型
        val parentTypeId = typeDao.insertType(createType(name = "餐饮", typeLevel = 0))
        val childTypeId = typeDao.insertType(createType(name = "早餐", parentId = parentTypeId, typeLevel = 1))
        val otherTypeId = typeDao.insertType(createType(name = "交通"))

        val startDate = 5000L
        val endDate = 10000L

        // 父类型、范围内
        insertRecord(createRecord(typeId = parentTypeId, recordTime = 7000L, remark = "父-范围内"))
        // 子类型、范围内
        insertRecord(createRecord(typeId = childTypeId, recordTime = 8000L, remark = "子-范围内"))
        // 父类型、范围外
        insertRecord(createRecord(typeId = parentTypeId, recordTime = 12000L, remark = "父-范围外"))
        // 其他类型、范围内
        insertRecord(createRecord(typeId = otherTypeId, recordTime = 7000L, remark = "其他-范围内"))
        // 子类型、范围边界（endDate，不包含）
        insertRecord(createRecord(typeId = childTypeId, recordTime = 10000L, remark = "子-结束边界"))

        val result = recordDao.queryRecordByTypeIdBetween(
            booksId = testBookId,
            typeId = parentTypeId,
            startDate = startDate,
            endDate = endDate,
            pageNum = 0,
            pageSize = 10,
        )
        assertThat(result).hasSize(2)
        val remarks = result.map { it.remark }
        assertThat(remarks).containsExactly("子-范围内", "父-范围内")
    }

    // endregion

    // region 补充测试：queryRelatedRecord, queryRelatedRecordCountByID, getRelatedIdListById, getRecordIdListFromRelatedId

    @Test
    fun when_queryRelatedRecord_then_returnsAllRelations() = runTest {
        testBookId = createTestBook()
        val typeId = typeDao.insertType(createType())

        val id1 = insertRecord(createRecord(typeId = typeId))
        val id2 = insertRecord(createRecord(typeId = typeId))
        val id3 = insertRecord(createRecord(typeId = typeId))

        transactionDao.insertRelatedRecord(
            listOf(
                RecordWithRelatedTable(id = null, recordId = id1, relatedRecordId = id2),
                RecordWithRelatedTable(id = null, recordId = id1, relatedRecordId = id3),
            ),
        )

        val result = recordDao.queryRelatedRecord()
        assertThat(result).hasSize(2)
    }

    @Test
    fun when_queryRelatedRecordCountByID_then_countsCorrectly() = runTest {
        testBookId = createTestBook()
        val typeId = typeDao.insertType(createType())

        val id1 = insertRecord(createRecord(typeId = typeId))
        val id2 = insertRecord(createRecord(typeId = typeId))
        val id3 = insertRecord(createRecord(typeId = typeId))

        // id1 -> id2, id1 -> id3
        transactionDao.insertRelatedRecord(
            listOf(
                RecordWithRelatedTable(id = null, recordId = id1, relatedRecordId = id2),
                RecordWithRelatedTable(id = null, recordId = id1, relatedRecordId = id3),
            ),
        )

        // id1 作为 record_id 出现 2 次
        assertThat(recordDao.queryRelatedRecordCountByID(id1)).isEqualTo(2)
        // id2 作为 related_record_id 出现 1 次
        assertThat(recordDao.queryRelatedRecordCountByID(id2)).isEqualTo(1)
        // id3 作为 related_record_id 出现 1 次
        assertThat(recordDao.queryRelatedRecordCountByID(id3)).isEqualTo(1)
    }

    @Test
    fun when_getRelatedIdListById_then_returnsRelatedRecordIds() = runTest {
        testBookId = createTestBook()
        val typeId = typeDao.insertType(createType())

        val mainId = insertRecord(createRecord(typeId = typeId))
        val relatedId1 = insertRecord(createRecord(typeId = typeId))
        val relatedId2 = insertRecord(createRecord(typeId = typeId))

        transactionDao.insertRelatedRecord(
            listOf(
                RecordWithRelatedTable(id = null, recordId = mainId, relatedRecordId = relatedId1),
                RecordWithRelatedTable(id = null, recordId = mainId, relatedRecordId = relatedId2),
            ),
        )

        val result = recordDao.getRelatedIdListById(mainId)
        assertThat(result).containsExactly(relatedId1, relatedId2)
    }

    @Test
    fun when_getRecordIdListFromRelatedId_then_returnsRecordIds() = runTest {
        testBookId = createTestBook()
        val typeId = typeDao.insertType(createType())

        val mainId1 = insertRecord(createRecord(typeId = typeId))
        val mainId2 = insertRecord(createRecord(typeId = typeId))
        val relatedId = insertRecord(createRecord(typeId = typeId))

        transactionDao.insertRelatedRecord(
            listOf(
                RecordWithRelatedTable(id = null, recordId = mainId1, relatedRecordId = relatedId),
                RecordWithRelatedTable(id = null, recordId = mainId2, relatedRecordId = relatedId),
            ),
        )

        val result = recordDao.getRecordIdListFromRelatedId(relatedId)
        assertThat(result).containsExactly(mainId1, mainId2)
    }

    // endregion

    // region 补充测试：queryByTypeId, getExpenditureRecordListAfterTime, deleteRelatedWithAsset

    @Test
    fun when_queryByTypeId_then_returnsExactTypeRecords() = runTest {
        testBookId = createTestBook()

        val typeId1 = typeDao.insertType(createType(name = "类型A"))
        val typeId2 = typeDao.insertType(createType(name = "类型B"))

        insertRecord(createRecord(typeId = typeId1, remark = "A1"))
        insertRecord(createRecord(typeId = typeId1, remark = "A2"))
        insertRecord(createRecord(typeId = typeId2, remark = "B1"))

        val result = recordDao.queryByTypeId(typeId1)
        assertThat(result).hasSize(2)
        assertThat(result.map { it.remark }).containsExactly("A1", "A2")
    }

    @Test
    fun when_getExpenditureRecordListAfterTime_then_filtersCorrectly() = runTest {
        testBookId = createTestBook()

        // 插入支出类型和收入类型
        val expenditureTypeId = typeDao.insertType(
            createType(name = "支出", typeCategory = RecordTypeCategoryEnum.EXPENDITURE.ordinal),
        )
        val incomeTypeId = typeDao.insertType(
            createType(name = "收入", typeCategory = RecordTypeCategoryEnum.INCOME.ordinal),
        )

        val cutoffTime = 5000L

        // 支出、时间之后
        insertRecord(createRecord(typeId = expenditureTypeId, recordTime = 8000L, remark = "支出1"))
        // 支出、时间之前
        insertRecord(createRecord(typeId = expenditureTypeId, recordTime = 3000L, remark = "支出-早期"))
        // 收入、时间之后
        insertRecord(createRecord(typeId = incomeTypeId, recordTime = 8000L, remark = "收入1"))

        // 默认参数 incomeCategory = EXPENDITURE.ordinal = 0
        val result = recordDao.getExpenditureRecordListAfterTime(testBookId, cutoffTime)
        assertThat(result).hasSize(1)
        assertThat(result[0].remark).isEqualTo("支出1")
    }

    @Test
    fun when_getExpenditureReimburseRecordListAfterTime_then_filtersCorrectly() = runTest {
        testBookId = createTestBook()
        val typeId = typeDao.insertType(createType())
        val cutoffTime = 5000L

        // 可报销、时间之后
        insertRecord(createRecord(typeId = typeId, reimbursable = SWITCH_INT_ON, recordTime = 8000L, remark = "可报销1"))
        // 不可报销、时间之后
        insertRecord(createRecord(typeId = typeId, reimbursable = SWITCH_INT_OFF, recordTime = 8000L, remark = "不可报销"))
        // 可报销、时间之前
        insertRecord(createRecord(typeId = typeId, reimbursable = SWITCH_INT_ON, recordTime = 3000L, remark = "可报销-早期"))

        val result = recordDao.getExpenditureReimburseRecordListAfterTime(testBookId, cutoffTime)
        assertThat(result).hasSize(1)
        assertThat(result[0].remark).isEqualTo("可报销1")
    }

    @Test
    fun when_deleteRelatedWithAsset_then_removesRelatedEntries() = runTest {
        testBookId = createTestBook()
        val typeId = typeDao.insertType(createType())
        val targetAssetId = 100L

        // 创建与目标资产关联的记录
        val recordWithAsset = insertRecord(createRecord(typeId = typeId, assetId = targetAssetId))
        // 创建不相关的记录
        val otherRecord = insertRecord(createRecord(typeId = typeId, assetId = 200L))
        // 创建第三条记录用于建立关联
        val thirdRecord = insertRecord(createRecord(typeId = typeId, assetId = 300L))

        // 建立关联关系
        transactionDao.insertRelatedRecord(
            listOf(
                RecordWithRelatedTable(id = null, recordId = recordWithAsset, relatedRecordId = otherRecord),
                RecordWithRelatedTable(id = null, recordId = thirdRecord, relatedRecordId = otherRecord),
            ),
        )

        // 删除与目标资产关联的记录关联
        recordDao.deleteRelatedWithAsset(targetAssetId)

        // 验证：recordWithAsset 的关联被删除，thirdRecord 的关联保留
        val allRelated = recordDao.queryRelatedRecord()
        assertThat(allRelated).hasSize(1)
        assertThat(allRelated[0].recordId).isEqualTo(thirdRecord)
    }

    // endregion

    // region 补充测试：queryRecordByTypeIdExactBetween, getExpenditureRecordListByKeywordAfterTime

    @Test
    fun when_queryRecordByTypeIdExactBetween_then_excludesChildTypes() = runTest {
        testBookId = createTestBook()

        val parentTypeId = typeDao.insertType(createType(name = "餐饮", typeLevel = 0))
        val childTypeId = typeDao.insertType(createType(name = "早餐", parentId = parentTypeId, typeLevel = 1))

        val startDate = 5000L
        val endDate = 10000L

        // 父类型、范围内
        insertRecord(createRecord(typeId = parentTypeId, recordTime = 7000L, remark = "父-范围内"))
        // 子类型、范围内
        insertRecord(createRecord(typeId = childTypeId, recordTime = 8000L, remark = "子-范围内"))

        val result = recordDao.queryRecordByTypeIdExactBetween(
            booksId = testBookId,
            typeId = parentTypeId,
            startDate = startDate,
            endDate = endDate,
            pageNum = 0,
            pageSize = 10,
        )
        // 精确匹配，不包含子类型
        assertThat(result).hasSize(1)
        assertThat(result[0].remark).isEqualTo("父-范围内")
    }

    @Test
    fun when_getExpenditureRecordListByKeywordAfterTime_then_filtersCorrectly() = runTest {
        testBookId = createTestBook()

        val expenditureTypeId = typeDao.insertType(
            createType(name = "支出", typeCategory = RecordTypeCategoryEnum.EXPENDITURE.ordinal),
        )
        val incomeTypeId = typeDao.insertType(
            createType(name = "收入", typeCategory = RecordTypeCategoryEnum.INCOME.ordinal),
        )

        val cutoffTime = 5000L

        // 支出、匹配关键字、时间之后
        insertRecord(createRecord(typeId = expenditureTypeId, recordTime = 8000L, remark = "星巴克咖啡"))
        // 支出、不匹配关键字、时间之后
        insertRecord(createRecord(typeId = expenditureTypeId, recordTime = 8000L, remark = "超市购物"))
        // 收入、匹配关键字、时间之后
        insertRecord(createRecord(typeId = incomeTypeId, recordTime = 8000L, remark = "星巴克退款"))

        // remark LIKE :keyword，需要手动添加通配符
        val result = recordDao.getExpenditureRecordListByKeywordAfterTime(
            keyword = "%星巴克%",
            booksId = testBookId,
            recordTime = cutoffTime,
        )
        assertThat(result).hasSize(1)
        assertThat(result[0].remark).isEqualTo("星巴克咖啡")
    }

    @Test
    fun when_getLastThreeMonthExpenditureReimburseRecordListByKeyword_then_filtersCorrectly() = runTest {
        testBookId = createTestBook()
        val typeId = typeDao.insertType(createType())
        val cutoffTime = 5000L

        // 可报销、匹配关键字、时间之后
        insertRecord(createRecord(typeId = typeId, reimbursable = SWITCH_INT_ON, recordTime = 8000L, remark = "出差打车"))
        // 可报销、不匹配关键字、时间之后
        insertRecord(createRecord(typeId = typeId, reimbursable = SWITCH_INT_ON, recordTime = 8000L, remark = "办公用品"))
        // 不可报销、匹配关键字、时间之后
        insertRecord(createRecord(typeId = typeId, reimbursable = SWITCH_INT_OFF, recordTime = 8000L, remark = "个人打车"))

        val result = recordDao.getLastThreeMonthExpenditureReimburseRecordListByKeyword(
            keyword = "%打车%",
            booksId = testBookId,
            recordTime = cutoffTime,
        )
        assertThat(result).hasSize(1)
        assertThat(result[0].remark).isEqualTo("出差打车")
    }

    // endregion
}
