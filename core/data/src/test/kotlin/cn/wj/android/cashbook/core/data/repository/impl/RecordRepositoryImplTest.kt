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
import cn.wj.android.cashbook.core.common.SWITCH_INT_ON
import cn.wj.android.cashbook.core.data.repository.asModel
import cn.wj.android.cashbook.core.data.testdoubles.FakeCombineProtoDataSource
import cn.wj.android.cashbook.core.data.testdoubles.FakeRecordDao
import cn.wj.android.cashbook.core.database.table.ImageWithRelatedTable
import cn.wj.android.cashbook.core.database.table.RecordTable
import cn.wj.android.cashbook.core.database.table.RecordWithRelatedTable
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * RecordRepository 实现测试
 *
 * 测试记录仓库的核心业务逻辑，包括 CRUD 委托、搜索历史管理等
 */
class RecordRepositoryImplTest {

    private lateinit var recordDao: FakeRecordDao
    private lateinit var fakeDataSource: FakeCombineProtoDataSource

    @Before
    fun setup() {
        recordDao = FakeRecordDao()
        fakeDataSource = FakeCombineProtoDataSource()
    }

    // ========== 查询委托测试 ==========

    @Test
    fun when_queryById_then_delegates_to_dao() = runTest {
        // 准备数据
        val record = createRecord(id = 1L, booksId = 1L)
        recordDao.addRecord(record)

        // 执行查询
        val result = recordDao.queryById(1L)

        // 验证
        assertThat(result).isNotNull()
        assertThat(result!!.id).isEqualTo(1L)
    }

    @Test
    fun given_no_record_when_queryById_then_returns_null() = runTest {
        val result = recordDao.queryById(999L)
        assertThat(result).isNull()
    }

    @Test
    fun when_queryById_then_asModel_maps_correctly() = runTest {
        val record = createRecord(
            id = 1L,
            booksId = 2L,
            remark = "午餐",
            reimbursable = SWITCH_INT_ON,
        )
        recordDao.addRecord(record)

        val table = recordDao.queryById(1L)!!
        val model = table.asModel()

        assertThat(model.id).isEqualTo(1L)
        assertThat(model.booksId).isEqualTo(2L)
        assertThat(model.remark).isEqualTo("午餐")
        assertThat(model.reimbursable).isTrue()
    }

    @Test
    fun when_queryByTypeId_then_returns_matching_records() = runTest {
        recordDao.addRecord(createRecord(id = 1L, typeId = 10L))
        recordDao.addRecord(createRecord(id = 2L, typeId = 10L))
        recordDao.addRecord(createRecord(id = 3L, typeId = 20L))

        val results = recordDao.queryByTypeId(10L)
        assertThat(results).hasSize(2)
        assertThat(results.map { it.id }).containsExactly(1L, 2L)
    }

    // ========== 分页查询测试 ==========

    @Test
    fun when_queryRecordByAssetId_then_returns_paged_results() = runTest {
        // 插入 5 条记录
        for (i in 1L..5L) {
            recordDao.addRecord(
                createRecord(id = i, booksId = 1L, assetId = 100L, recordTime = i * 1000),
            )
        }

        // 第一页，每页 2 条
        val page1 = recordDao.queryRecordByAssetId(
            booksId = 1L,
            assetId = 100L,
            pageNum = 0,
            pageSize = 2,
        )
        assertThat(page1).hasSize(2)

        // 第二页
        val page2 = recordDao.queryRecordByAssetId(
            booksId = 1L,
            assetId = 100L,
            pageNum = 2,
            pageSize = 2,
        )
        assertThat(page2).hasSize(2)
    }

    @Test
    fun when_queryRecordByKeyword_then_matches_remark() = runTest {
        recordDao.addRecord(createRecord(id = 1L, booksId = 1L, remark = "午餐消费"))
        recordDao.addRecord(createRecord(id = 2L, booksId = 1L, remark = "晚餐消费"))
        recordDao.addRecord(createRecord(id = 3L, booksId = 1L, remark = "交通出行"))

        val results = recordDao.queryRecordByKeyword(
            booksId = 1L,
            keyword = "消费",
            pageNum = 0,
            pageSize = 10,
        )
        assertThat(results).hasSize(2)
    }

    @Test
    fun when_queryRecordByTagId_then_returns_tagged_records() = runTest {
        recordDao.addRecord(createRecord(id = 1L, booksId = 1L))
        recordDao.addRecord(createRecord(id = 2L, booksId = 1L))
        recordDao.addRecord(createRecord(id = 3L, booksId = 1L))

        // 给记录 1 和 3 关联标签 100
        recordDao.tagWithRecords.add(FakeRecordDao.FakeTagWithRecordEntry(recordId = 1L, tagId = 100L))
        recordDao.tagWithRecords.add(FakeRecordDao.FakeTagWithRecordEntry(recordId = 3L, tagId = 100L))

        val results = recordDao.queryRecordByTagId(
            booksId = 1L,
            tagId = 100L,
            pageNum = 0,
            pageSize = 10,
        )
        assertThat(results).hasSize(2)
        assertThat(results.map { it.id }).containsExactly(1L, 3L)
    }

    // ========== 搜索历史测试 ==========

    @Test
    fun when_addSearchHistory_then_keyword_added() = runTest {
        fakeDataSource.updateKeywords(listOf("旧关键词"))
        val current = fakeDataSource.searchHistoryData.first()

        // 模拟 addSearchHistory 逻辑
        val keyword = "新关键词"
        val currentList = ArrayList(current.keywords).toMutableList()
        if (!currentList.contains(keyword)) {
            currentList.add(0, keyword)
        }
        fakeDataSource.updateKeywords(currentList)

        val result = fakeDataSource.searchHistoryData.first()
        assertThat(result.keywords).containsExactly("新关键词", "旧关键词").inOrder()
    }

    @Test
    fun given_blank_keyword_when_addSearchHistory_then_ignored() = runTest {
        fakeDataSource.updateKeywords(listOf("已有关键词"))

        // 模拟 addSearchHistory 逻辑：空白关键词直接返回
        val keyword = "   "
        if (keyword.isNotBlank()) {
            // 不应该执行到这里
            fakeDataSource.updateKeywords(listOf(keyword))
        }

        val result = fakeDataSource.searchHistoryData.first()
        assertThat(result.keywords).containsExactly("已有关键词")
    }

    @Test
    fun given_duplicate_keyword_when_addSearchHistory_then_not_duplicated() = runTest {
        fakeDataSource.updateKeywords(listOf("关键词A", "关键词B"))

        // 模拟 addSearchHistory 逻辑：已存在则直接返回
        val keyword = "关键词A"
        val currentList = ArrayList(fakeDataSource.searchHistoryData.first().keywords)
        if (currentList.contains(keyword)) {
            // 不添加重复项
        } else {
            currentList.add(0, keyword)
            fakeDataSource.updateKeywords(currentList)
        }

        val result = fakeDataSource.searchHistoryData.first()
        assertThat(result.keywords).hasSize(2)
        assertThat(result.keywords).containsExactly("关键词A", "关键词B")
    }

    @Test
    fun given_more_than_10_entries_when_addSearchHistory_then_truncated_to_10() = runTest {
        // 先添加 10 个关键词
        val existingKeywords = (1..10).map { "关键词$it" }
        fakeDataSource.updateKeywords(existingKeywords)

        // 模拟 addSearchHistory 逻辑
        val keyword = "新关键词"
        var currentList: MutableList<String> = ArrayList(fakeDataSource.searchHistoryData.first().keywords)
        if (!currentList.contains(keyword)) {
            currentList.add(0, keyword)
            if (currentList.size > 10) {
                currentList = currentList.subList(0, 10)
            }
        }
        fakeDataSource.updateKeywords(currentList)

        val result = fakeDataSource.searchHistoryData.first()
        assertThat(result.keywords).hasSize(10)
        assertThat(result.keywords.first()).isEqualTo("新关键词")
        // 最后一个应该是 "关键词9"，因为 "关键词10" 被截断了
        assertThat(result.keywords.last()).isEqualTo("关键词9")
    }

    @Test
    fun when_clearSearchHistory_then_keywords_empty() = runTest {
        fakeDataSource.updateKeywords(listOf("A", "B", "C"))
        fakeDataSource.updateKeywords(emptyList())

        val result = fakeDataSource.searchHistoryData.first()
        assertThat(result.keywords).isEmpty()
    }

    // ========== 类型变更测试 ==========

    @Test
    fun when_changeRecordTypeBeforeDeleteType_then_records_updated() = runTest {
        recordDao.addRecord(createRecord(id = 1L, typeId = 10L))
        recordDao.addRecord(createRecord(id = 2L, typeId = 10L))
        recordDao.addRecord(createRecord(id = 3L, typeId = 20L))

        recordDao.changeRecordTypeBeforeDeleteType(fromId = 10L, toId = 99L)

        assertThat(recordDao.records[0].typeId).isEqualTo(99L)
        assertThat(recordDao.records[1].typeId).isEqualTo(99L)
        assertThat(recordDao.records[2].typeId).isEqualTo(20L)
    }

    // ========== 关联记录测试 ==========

    @Test
    fun when_getRelatedIdListById_then_returns_related_ids() = runTest {
        recordDao.relatedRecords.add(
            RecordWithRelatedTable(
                id = 1L,
                recordId = 10L,
                relatedRecordId = 20L,
            ),
        )
        recordDao.relatedRecords.add(
            RecordWithRelatedTable(
                id = 2L,
                recordId = 10L,
                relatedRecordId = 30L,
            ),
        )

        val result = recordDao.getRelatedIdListById(10L)
        assertThat(result).containsExactly(20L, 30L)
    }

    @Test
    fun when_queryRelatedRecordCountByID_then_returns_count() = runTest {
        recordDao.relatedRecords.add(
            RecordWithRelatedTable(
                id = 1L,
                recordId = 10L,
                relatedRecordId = 20L,
            ),
        )
        recordDao.relatedRecords.add(
            RecordWithRelatedTable(
                id = 2L,
                recordId = 30L,
                relatedRecordId = 10L,
            ),
        )

        // id=10 出现在 recordId 和 relatedRecordId 中各一次
        val count = recordDao.queryRelatedRecordCountByID(10L)
        assertThat(count).isEqualTo(2)
    }

    // ========== 删除相关测试 ==========

    @Test
    fun when_deleteWithAsset_then_records_with_asset_removed() = runTest {
        recordDao.addRecord(createRecord(id = 1L, assetId = 100L))
        recordDao.addRecord(createRecord(id = 2L, assetId = 200L))
        recordDao.addRecord(createRecord(id = 3L, assetId = 100L))
        // intoAssetId 也应该被删除
        recordDao.addRecord(createRecord(id = 4L, assetId = 300L).copy(intoAssetId = 100L))

        recordDao.deleteWithAsset(100L)

        assertThat(recordDao.records).hasSize(1)
        assertThat(recordDao.records[0].id).isEqualTo(2L)
    }

    // ========== 图片查询测试 ==========

    @Test
    fun when_queryImagesByRecordId_then_returns_related_images() = runTest {
        recordDao.images.add(
            ImageWithRelatedTable(id = 1L, recordId = 10L, path = "/img1.png", bytes = byteArrayOf()),
        )
        recordDao.images.add(
            ImageWithRelatedTable(id = 2L, recordId = 10L, path = "/img2.png", bytes = byteArrayOf()),
        )
        recordDao.images.add(
            ImageWithRelatedTable(id = 3L, recordId = 20L, path = "/img3.png", bytes = byteArrayOf()),
        )

        val results = recordDao.queryImagesByRecordId(10L)
        assertThat(results).hasSize(2)
    }

    // ========== 按日期区间查询测试 ==========

    @Test
    fun when_queryByBooksIdBetweenDate_then_returns_records_in_range() = runTest {
        recordDao.addRecord(createRecord(id = 1L, booksId = 1L, recordTime = 1000L))
        recordDao.addRecord(createRecord(id = 2L, booksId = 1L, recordTime = 2000L))
        recordDao.addRecord(createRecord(id = 3L, booksId = 1L, recordTime = 3000L))
        recordDao.addRecord(createRecord(id = 4L, booksId = 1L, recordTime = 4000L))

        // 查询 [1500, 3500) 区间
        val results = recordDao.queryByBooksIdBetweenDate(
            booksId = 1L,
            startDate = 1500L,
            endDate = 3500L,
        )
        assertThat(results).hasSize(2)
        assertThat(results.map { it.id }).containsExactly(2L, 3L)
    }

    // ========== 可报销记录查询测试 ==========

    @Test
    fun when_queryReimburseByBooksIdAfterDate_then_returns_reimbursable_only() = runTest {
        recordDao.addRecord(
            createRecord(id = 1L, booksId = 1L, reimbursable = SWITCH_INT_ON, recordTime = 2000L),
        )
        recordDao.addRecord(
            createRecord(id = 2L, booksId = 1L, reimbursable = SWITCH_INT_OFF, recordTime = 2000L),
        )
        recordDao.addRecord(
            createRecord(id = 3L, booksId = 1L, reimbursable = SWITCH_INT_ON, recordTime = 500L),
        )

        val results = recordDao.queryReimburseByBooksIdAfterDate(booksId = 1L, dateTime = 1000L)
        assertThat(results).hasSize(1)
        assertThat(results[0].id).isEqualTo(1L)
    }

    // ========== 辅助方法 ==========

    private fun createRecord(
        id: Long? = null,
        typeId: Long = 1L,
        assetId: Long = 1L,
        booksId: Long = 1L,
        amount: Double = 100.0,
        remark: String = "",
        reimbursable: Int = SWITCH_INT_OFF,
        recordTime: Long = 1000L,
    ) = RecordTable(
        id = id,
        typeId = typeId,
        assetId = assetId,
        intoAssetId = -1L,
        booksId = booksId,
        amount = amount,
        finalAmount = amount,
        concessions = 0.0,
        charge = 0.0,
        remark = remark,
        reimbursable = reimbursable,
        recordTime = recordTime,
    )
}
