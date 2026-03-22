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
import cn.wj.android.cashbook.core.database.CashbookDatabase
import cn.wj.android.cashbook.core.database.table.RecordTable
import cn.wj.android.cashbook.core.database.table.TagTable
import cn.wj.android.cashbook.core.database.table.TagWithRecordTable
import cn.wj.android.cashbook.core.database.table.TypeTable
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * TagDao 数据库操作测试
 */
@RunWith(AndroidJUnit4::class)
class TagDaoTest {

    private lateinit var database: CashbookDatabase
    private lateinit var tagDao: TagDao
    private lateinit var transactionDao: TransactionDao
    private lateinit var typeDao: TypeDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            CashbookDatabase::class.java,
        ).allowMainThreadQueries().build()
        tagDao = database.tagDao()
        transactionDao = database.transactionDao()
        typeDao = database.typeDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun when_insertTag_then_queryAllReturnsIt() = runTest {
        // 插入一条标签记录
        val tag = TagTable(
            id = null,
            name = "餐饮",
            booksId = 1L,
            invisible = SWITCH_INT_OFF,
        )
        val insertedId = tagDao.insert(tag)

        // 查询所有标签，验证包含插入的记录
        val allTags = tagDao.queryAll()
        assertThat(allTags).hasSize(1)
        assertThat(allTags[0].id).isEqualTo(insertedId)
        assertThat(allTags[0].name).isEqualTo("餐饮")
        assertThat(allTags[0].booksId).isEqualTo(1L)
        assertThat(allTags[0].invisible).isEqualTo(SWITCH_INT_OFF)
    }

    @Test
    fun when_updateTag_then_queryReflectsChanges() = runTest {
        // 先插入一条标签
        val tag = TagTable(
            id = null,
            name = "原始标签",
            booksId = 1L,
            invisible = SWITCH_INT_OFF,
        )
        val insertedId = tagDao.insert(tag)

        // 更新标签名称
        val updatedTag = TagTable(
            id = insertedId,
            name = "更新后标签",
            booksId = 2L,
            invisible = 1,
        )
        tagDao.update(updatedTag)

        // 验证更新后的数据
        val allTags = tagDao.queryAll()
        assertThat(allTags).hasSize(1)
        assertThat(allTags[0].id).isEqualTo(insertedId)
        assertThat(allTags[0].name).isEqualTo("更新后标签")
        assertThat(allTags[0].booksId).isEqualTo(2L)
        assertThat(allTags[0].invisible).isEqualTo(1)
    }

    @Test
    fun when_deleteTag_then_queryAllDoesNotContainIt() = runTest {
        // 插入两条标签
        val tag1 = TagTable(id = null, name = "标签一", booksId = 1L, invisible = SWITCH_INT_OFF)
        val tag2 = TagTable(id = null, name = "标签二", booksId = 1L, invisible = SWITCH_INT_OFF)
        val id1 = tagDao.insert(tag1)
        tagDao.insert(tag2)

        // 删除第一条标签
        tagDao.delete(tag1.copy(id = id1))

        // 验证仅剩第二条标签
        val allTags = tagDao.queryAll()
        assertThat(allTags).hasSize(1)
        assertThat(allTags[0].name).isEqualTo("标签二")
    }

    @Test
    fun when_queryByRecordId_then_returnsAssociatedTags() = runTest {
        // 先插入类型（记录需要引用 typeId）
        val typeId = typeDao.insertType(
            TypeTable(
                id = null,
                parentId = -1L,
                name = "支出类型",
                iconName = "icon",
                typeLevel = 0,
                typeCategory = 0,
                protected = SWITCH_INT_OFF,
                sort = 0,
            ),
        )

        // 插入标签
        val tagId1 = tagDao.insert(
            TagTable(id = null, name = "标签A", booksId = 1L, invisible = SWITCH_INT_OFF),
        )
        val tagId2 = tagDao.insert(
            TagTable(id = null, name = "标签B", booksId = 1L, invisible = SWITCH_INT_OFF),
        )
        val tagId3 = tagDao.insert(
            TagTable(id = null, name = "标签C", booksId = 1L, invisible = SWITCH_INT_OFF),
        )

        // 插入记录
        val recordId = transactionDao.insertRecord(
            RecordTable(
                id = null,
                typeId = typeId,
                assetId = -1L,
                intoAssetId = -1L,
                booksId = 1L,
                amount = 10000L,
                finalAmount = 10000L,
                concessions = 0L,
                charge = 0L,
                remark = "测试记录",
                reimbursable = SWITCH_INT_OFF,
                recordTime = System.currentTimeMillis(),
            ),
        )

        // 插入标签与记录的关联（标签A和标签B关联到该记录，标签C不关联）
        transactionDao.insertRelatedTags(
            listOf(
                TagWithRecordTable(id = null, recordId = recordId, tagId = tagId1),
                TagWithRecordTable(id = null, recordId = recordId, tagId = tagId2),
            ),
        )

        // 通过记录ID查询关联标签
        val tags = tagDao.queryByRecordId(recordId)
        assertThat(tags).hasSize(2)
        val tagNames = tags.map { it.name }
        assertThat(tagNames).containsExactly("标签A", "标签B")
    }

    @Test
    fun when_deleteRelatedWithAsset_then_removesTagRelationsForAssetRecords() = runTest {
        // 先插入类型
        val typeId = typeDao.insertType(
            TypeTable(
                id = null,
                parentId = -1L,
                name = "支出类型",
                iconName = "icon",
                typeLevel = 0,
                typeCategory = 0,
                protected = SWITCH_INT_OFF,
                sort = 0,
            ),
        )

        // 插入标签
        val tagId = tagDao.insert(
            TagTable(id = null, name = "标签X", booksId = 1L, invisible = SWITCH_INT_OFF),
        )

        val targetAssetId = 100L

        // 插入记录1：assetId 为目标资产
        val recordId1 = transactionDao.insertRecord(
            RecordTable(
                id = null,
                typeId = typeId,
                assetId = targetAssetId,
                intoAssetId = -1L,
                booksId = 1L,
                amount = 5000L,
                finalAmount = 5000L,
                concessions = 0L,
                charge = 0L,
                remark = "记录1",
                reimbursable = SWITCH_INT_OFF,
                recordTime = System.currentTimeMillis(),
            ),
        )

        // 插入记录2：intoAssetId 为目标资产
        val recordId2 = transactionDao.insertRecord(
            RecordTable(
                id = null,
                typeId = typeId,
                assetId = -1L,
                intoAssetId = targetAssetId,
                booksId = 1L,
                amount = 3000L,
                finalAmount = 3000L,
                concessions = 0L,
                charge = 0L,
                remark = "记录2",
                reimbursable = SWITCH_INT_OFF,
                recordTime = System.currentTimeMillis(),
            ),
        )

        // 插入记录3：与目标资产无关
        val recordId3 = transactionDao.insertRecord(
            RecordTable(
                id = null,
                typeId = typeId,
                assetId = 200L,
                intoAssetId = -1L,
                booksId = 1L,
                amount = 2000L,
                finalAmount = 2000L,
                concessions = 0L,
                charge = 0L,
                remark = "记录3",
                reimbursable = SWITCH_INT_OFF,
                recordTime = System.currentTimeMillis(),
            ),
        )

        // 为所有记录关联标签
        transactionDao.insertRelatedTags(
            listOf(
                TagWithRecordTable(id = null, recordId = recordId1, tagId = tagId),
                TagWithRecordTable(id = null, recordId = recordId2, tagId = tagId),
                TagWithRecordTable(id = null, recordId = recordId3, tagId = tagId),
            ),
        )

        // 删除目标资产相关的标签关联
        tagDao.deleteRelatedWithAsset(targetAssetId)

        // 验证：记录1和记录2的标签关联已被删除，记录3的标签关联仍在
        assertThat(tagDao.queryByRecordId(recordId1)).isEmpty()
        assertThat(tagDao.queryByRecordId(recordId2)).isEmpty()
        assertThat(tagDao.queryByRecordId(recordId3)).hasSize(1)
    }

    @Test
    fun when_countByName_then_returnsCorrectCount() = runTest {
        // 插入多条同名和不同名的标签
        tagDao.insert(TagTable(id = null, name = "重复标签", booksId = 1L, invisible = SWITCH_INT_OFF))
        tagDao.insert(TagTable(id = null, name = "重复标签", booksId = 2L, invisible = SWITCH_INT_OFF))
        tagDao.insert(TagTable(id = null, name = "其他标签", booksId = 1L, invisible = SWITCH_INT_OFF))

        // 验证同名标签计数
        assertThat(tagDao.countByName("重复标签")).isEqualTo(2)
        assertThat(tagDao.countByName("其他标签")).isEqualTo(1)
        assertThat(tagDao.countByName("不存在的标签")).isEqualTo(0)
    }
}
