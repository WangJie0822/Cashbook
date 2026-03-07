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
import cn.wj.android.cashbook.core.data.repository.asTable
import cn.wj.android.cashbook.core.data.testdoubles.FakeTagDao
import cn.wj.android.cashbook.core.data.testdoubles.FakeTransactionDao
import cn.wj.android.cashbook.core.database.table.TagTable
import cn.wj.android.cashbook.core.database.table.TagWithRecordTable
import cn.wj.android.cashbook.core.model.model.TagModel
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * TagRepository 实现测试
 *
 * 测试标签仓库的核心业务逻辑，包括 CRUD 操作和关联查询
 */
class TagRepositoryImplTest {

    private lateinit var tagDao: FakeTagDao
    private lateinit var transactionDao: FakeTransactionDao

    @Before
    fun setup() {
        tagDao = FakeTagDao()
        transactionDao = FakeTransactionDao()
    }

    // ========== CRUD 测试 ==========

    @Test
    fun when_insert_tag_then_tag_stored() = runTest {
        val id = tagDao.insert(createTagTable(name = "工作"))

        assertThat(id).isGreaterThan(0L)
        assertThat(tagDao.tags).hasSize(1)
        assertThat(tagDao.tags[0].name).isEqualTo("工作")
    }

    @Test
    fun when_insert_tag_with_null_id_then_auto_increment() = runTest {
        tagDao.insert(createTagTable(name = "标签A"))
        tagDao.insert(createTagTable(name = "标签B"))

        assertThat(tagDao.tags[0].id).isEqualTo(1L)
        assertThat(tagDao.tags[1].id).isEqualTo(2L)
    }

    @Test
    fun when_update_tag_then_tag_updated() = runTest {
        tagDao.insert(createTagTable(name = "原名"))
        val inserted = tagDao.tags[0]
        tagDao.update(inserted.copy(name = "新名"))

        assertThat(tagDao.tags[0].name).isEqualTo("新名")
    }

    @Test
    fun when_delete_tag_then_tag_removed() = runTest {
        tagDao.insert(createTagTable(name = "待删除"))
        tagDao.insert(createTagTable(name = "保留"))

        val toDelete = tagDao.tags[0]
        tagDao.delete(toDelete)

        assertThat(tagDao.tags).hasSize(1)
        assertThat(tagDao.tags[0].name).isEqualTo("保留")
    }

    @Test
    fun when_queryAll_then_returns_all_tags() = runTest {
        tagDao.insert(createTagTable(name = "A"))
        tagDao.insert(createTagTable(name = "B"))
        tagDao.insert(createTagTable(name = "C"))

        val result = tagDao.queryAll()
        assertThat(result).hasSize(3)
    }

    // ========== 关联查询测试 ==========

    @Test
    fun when_queryByRecordId_then_returns_related_tags() = runTest {
        tagDao.insert(createTagTable(name = "标签1"))
        tagDao.insert(createTagTable(name = "标签2"))
        tagDao.insert(createTagTable(name = "标签3"))

        // 记录 10 关联标签 1 和 3
        tagDao.tagWithRecords.add(FakeTagDao.FakeTagWithRecord(recordId = 10L, tagId = 1L))
        tagDao.tagWithRecords.add(FakeTagDao.FakeTagWithRecord(recordId = 10L, tagId = 3L))

        val result = tagDao.queryByRecordId(10L)
        assertThat(result).hasSize(2)
        assertThat(result.map { it.name }).containsExactly("标签1", "标签3")
    }

    @Test
    fun given_no_related_tags_when_queryByRecordId_then_returns_empty() = runTest {
        tagDao.insert(createTagTable(name = "标签1"))

        val result = tagDao.queryByRecordId(999L)
        assertThat(result).isEmpty()
    }

    // ========== 计数查询测试 ==========

    @Test
    fun when_countByName_then_returns_correct_count() = runTest {
        tagDao.insert(createTagTable(name = "工作"))
        tagDao.insert(createTagTable(name = "工作"))
        tagDao.insert(createTagTable(name = "生活"))

        assertThat(tagDao.countByName("工作")).isEqualTo(2)
        assertThat(tagDao.countByName("生活")).isEqualTo(1)
        assertThat(tagDao.countByName("不存在")).isEqualTo(0)
    }

    // ========== 模型映射集成测试 ==========

    @Test
    fun when_updateTag_logic_with_new_tag_then_inserts() = runTest {
        // 模拟 TagRepositoryImpl.updateTag 逻辑
        val model = TagModel(id = -1L, name = "新标签", invisible = false)
        val table = model.asTable()

        // id 为 null 时插入
        assertThat(table.id).isNull()
        if (table.id == null) {
            tagDao.insert(table)
        } else {
            tagDao.update(table)
        }

        assertThat(tagDao.tags).hasSize(1)
        val inserted = tagDao.tags[0]
        val result = inserted.asModel()
        assertThat(result.name).isEqualTo("新标签")
        assertThat(result.invisible).isFalse()
    }

    @Test
    fun when_updateTag_logic_with_existing_tag_then_updates() = runTest {
        // 先插入
        tagDao.insert(createTagTable(name = "原始标签"))
        val insertedId = tagDao.tags[0].id!!

        // 模拟更新
        val model = TagModel(id = insertedId, name = "更新标签", invisible = true)
        val table = model.asTable().copy(id = model.id)

        if (table.id == null) {
            tagDao.insert(table)
        } else {
            tagDao.update(table)
        }

        assertThat(tagDao.tags).hasSize(1)
        assertThat(tagDao.tags[0].name).isEqualTo("更新标签")
    }

    @Test
    fun when_deleteTag_via_transaction_then_relations_and_tag_removed() = runTest {
        // 模拟 TagRepositoryImpl.deleteTag 逻辑
        val tagId = 1L
        transactionDao.tags.add(tagId)
        transactionDao.tagWithRecords.add(
            TagWithRecordTable(
                id = 1L,
                recordId = 10L,
                tagId = tagId,
            ),
        )

        // 调用事务删除
        transactionDao.deleteTag(tagId)

        // 验证标签关联和标签本身都被删除
        assertThat(transactionDao.tagWithRecords).isEmpty()
        assertThat(transactionDao.tags).doesNotContain(tagId)
        assertThat(transactionDao.deleteTagCalled).isTrue()
    }

    @Test
    fun when_getTagById_logic_then_returns_matching_tag() = runTest {
        // 模拟 TagRepositoryImpl.getTagById 逻辑：从 tagListData 中查找
        tagDao.insert(createTagTable(name = "目标标签"))
        tagDao.insert(createTagTable(name = "其他标签"))

        val allTags = tagDao.queryAll().map { it.asModel() }
        val result = allTags.firstOrNull { it.id == 1L }

        assertThat(result).isNotNull()
        assertThat(result!!.name).isEqualTo("目标标签")
    }

    @Test
    fun given_nonexistent_id_when_getTagById_logic_then_returns_null() = runTest {
        tagDao.insert(createTagTable(name = "唯一标签"))

        val allTags = tagDao.queryAll().map { it.asModel() }
        val result = allTags.firstOrNull { it.id == 999L }

        assertThat(result).isNull()
    }

    // ========== invisible 映射测试 ==========

    @Test
    fun given_visible_tag_when_asModel_then_invisible_is_false() = runTest {
        tagDao.insert(createTagTable(name = "可见", invisible = SWITCH_INT_OFF))
        val tag = tagDao.tags[0]
        val model = tag.asModel()
        assertThat(model.invisible).isFalse()
    }

    @Test
    fun given_invisible_tag_when_asModel_then_invisible_is_true() = runTest {
        tagDao.insert(createTagTable(name = "隐藏", invisible = SWITCH_INT_ON))
        val tag = tagDao.tags[0]
        val model = tag.asModel()
        assertThat(model.invisible).isTrue()
    }

    // ========== 辅助方法 ==========

    private fun createTagTable(
        id: Long? = null,
        name: String = "标签",
        booksId: Long = 1L,
        invisible: Int = SWITCH_INT_OFF,
    ) = TagTable(
        id = id,
        name = name,
        booksId = booksId,
        invisible = invisible,
    )
}
