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

import androidx.paging.PagingSource
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import cn.wj.android.cashbook.core.common.SWITCH_INT_OFF
import cn.wj.android.cashbook.core.database.CashbookDatabase
import cn.wj.android.cashbook.core.database.relation.LauncherRecordViewRelation
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
 * [RecordDao.pagingLauncherRecordViews] 的 @Relation 分页加载真库测试。
 *
 * 首页分页优化（R6a）用 @Transaction+@Relation 一次批量物化 type/asset/tags/images/双向 relatedRecord 消 N+1，
 * FakeRecordDao 的桩仅保证记录层过滤/排序、关联置空，故 @Relation 组装语义（尤其双向 relatedRecord 不混淆、
 * 平账 type 天然 LEFT 空）必须在此真库层验证。
 */
@RunWith(AndroidJUnit4::class)
class RecordDaoRelationTest {

    private lateinit var database: CashbookDatabase
    private lateinit var recordDao: RecordDao
    private lateinit var transactionDao: TransactionDao
    private lateinit var typeDao: TypeDao
    private lateinit var assetDao: AssetDao
    private lateinit var tagDao: TagDao
    private lateinit var booksDao: BooksDao

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

    private suspend fun createTestBook(): Long = booksDao.insert(
        BooksTable(
            id = null,
            name = "测试账本",
            description = "",
            bgUri = "",
            modifyTime = System.currentTimeMillis(),
        ),
    )

    private fun createType(
        name: String = "测试类型",
        typeCategory: Int = RecordTypeCategoryEnum.EXPENDITURE.ordinal,
    ) = TypeTable(
        id = null,
        parentId = -1L,
        name = name,
        iconName = "icon_test",
        typeLevel = 0,
        typeCategory = typeCategory,
        protected = 0,
        sort = 0,
    )

    private fun createAsset(id: Long, name: String) = AssetTable(
        id = id,
        booksId = testBookId,
        name = name,
        balance = 0L,
        totalAmount = 0L,
        billingDate = "",
        repaymentDate = "",
        type = 0,
        classification = 0,
        invisible = SWITCH_INT_OFF,
        openBank = "",
        cardNo = "",
        remark = "",
        sort = 0,
        modifyTime = System.currentTimeMillis(),
    )

    private fun createRecord(
        typeId: Long,
        assetId: Long = -1L,
        intoAssetId: Long = -1L,
        recordTime: Long = 1000000L,
        remark: String = "",
    ) = RecordTable(
        id = null,
        typeId = typeId,
        assetId = assetId,
        intoAssetId = intoAssetId,
        booksId = testBookId,
        amount = 10000L,
        finalAmount = 10000L,
        concessions = 0L,
        charge = 0L,
        remark = remark,
        reimbursable = SWITCH_INT_OFF,
        recordTime = recordTime,
        reimbursed = SWITCH_INT_OFF,
    )

    private suspend fun loadPage(): List<LauncherRecordViewRelation> {
        val source = recordDao.pagingLauncherRecordViews(
            booksId = testBookId,
            startDate = 0L,
            endDate = Long.MAX_VALUE,
        )
        val result = source.load(
            PagingSource.LoadParams.Refresh(key = null, loadSize = 20, placeholdersEnabled = false),
        ) as PagingSource.LoadResult.Page
        return result.data
    }

    // endregion

    @Test
    fun paging_relation_loads_all_relations_and_bidirectional_related() = runTest {
        testBookId = createTestBook()
        val expTypeId = typeDao.insertType(createType(name = "餐饮", typeCategory = RecordTypeCategoryEnum.EXPENDITURE.ordinal))
        val incTypeId = typeDao.insertType(createType(name = "报销款", typeCategory = RecordTypeCategoryEnum.INCOME.ordinal))
        val assetAId = 100L
        val assetBId = 200L
        assetDao.insert(createAsset(id = assetAId, name = "银行卡"))
        assetDao.insert(createAsset(id = assetBId, name = "现金"))
        val tagId = tagDao.insert(TagTable(id = null, name = "旅行", booksId = testBookId, invisible = SWITCH_INT_OFF))

        // 支出记录（带资产/转入资产/标签/图片），收入记录吸收它
        val expId = transactionDao.insertRecord(
            createRecord(typeId = expTypeId, assetId = assetAId, intoAssetId = assetBId, recordTime = 2000L, remark = "支出"),
        )
        val incId = transactionDao.insertRecord(
            createRecord(typeId = incTypeId, recordTime = 3000L, remark = "报销款"),
        )
        // 关联：收入(record_id) -> 支出(related_record_id)
        transactionDao.insertRelatedRecord(
            listOf(RecordWithRelatedTable(id = null, recordId = incId, relatedRecordId = expId)),
        )
        transactionDao.insertRelatedTags(
            listOf(TagWithRecordTable(id = null, recordId = expId, tagId = tagId)),
        )
        transactionDao.insertRelatedImages(
            listOf(ImageWithRelatedTable(id = null, recordId = expId, path = "/img.jpg", bytes = byteArrayOf(1, 2, 3))),
        )

        val page = loadPage()
        val exp = page.first { it.record.id == expId }
        val inc = page.first { it.record.id == incId }

        // 一对一关联物化
        assertThat(exp.types.single().id).isEqualTo(expTypeId)
        assertThat(exp.assets.single().id).isEqualTo(assetAId)
        assertThat(exp.intoAssets.single().id).isEqualTo(assetBId)
        assertThat(exp.tags.single().id).isEqualTo(tagId)
        assertThat(exp.images.single().path).isEqualTo("/img.jpg")

        // 双向 relatedRecord 不混淆：
        // 支出侧（related_record_id）→ 被收入吸收
        assertThat(exp.relatedAsRelatedId.map { it.id }).containsExactly(incId)
        assertThat(exp.relatedAsRecordId).isEmpty()
        // 收入侧（record_id）→ 吸收支出
        assertThat(inc.relatedAsRecordId.map { it.id }).containsExactly(expId)
        assertThat(inc.relatedAsRelatedId).isEmpty()
    }

    @Test
    fun paging_relation_balance_record_has_empty_type() = runTest {
        testBookId = createTestBook()
        // 平账支出 typeId=-1101（db_type 无此行）——RECORD_TYPE_BALANCE_EXPENDITURE.id
        transactionDao.insertRecord(createRecord(typeId = -1101L, recordTime = 5000L, remark = "平账"))

        val page = loadPage()
        val balance = page.first { it.record.typeId == -1101L }
        // @Relation 天然 LEFT：主记录不丢、types 空 List（映射层据此走合成平账类型）
        assertThat(balance.types).isEmpty()
    }
}
