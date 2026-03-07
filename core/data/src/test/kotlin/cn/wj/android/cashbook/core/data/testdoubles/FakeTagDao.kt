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

import cn.wj.android.cashbook.core.database.dao.TagDao
import cn.wj.android.cashbook.core.database.table.TagTable

/**
 * TagDao 的测试替身，使用内存列表存储数据
 */
class FakeTagDao : TagDao {

    /** 标签数据列表 */
    val tags = mutableListOf<TagTable>()

    /** 标签与记录关联列表 */
    val tagWithRecords = mutableListOf<FakeTagWithRecord>()

    /** 自增主键计数器 */
    private var nextId = 1L

    override suspend fun insert(tag: TagTable): Long {
        val id = tag.id ?: nextId++
        val withId = tag.copy(id = id)
        tags.add(withId)
        return id
    }

    override suspend fun update(tag: TagTable) {
        val index = tags.indexOfFirst { it.id == tag.id }
        if (index >= 0) {
            tags[index] = tag
        }
    }

    override suspend fun delete(tag: TagTable) {
        tags.removeAll { it.id == tag.id }
    }

    override suspend fun queryAll(): List<TagTable> {
        return tags.toList()
    }

    override suspend fun queryByRecordId(recordId: Long): List<TagTable> {
        val tagIds = tagWithRecords.filter { it.recordId == recordId }.map { it.tagId }
        return tags.filter { it.id in tagIds }
    }

    override suspend fun deleteRelatedWithAsset(assetId: Long) {
        // 简化实现：根据 assetId 删除关联标签记录
        // 实际 SQL 是通过关联 record 表来查找的，这里简化处理
        tagWithRecords.removeAll { it.assetId == assetId }
    }

    override suspend fun countByName(name: String): Int {
        return tags.count { it.name == name }
    }

    /** 标签与记录关联简易数据 */
    data class FakeTagWithRecord(
        val recordId: Long,
        val tagId: Long,
        val assetId: Long = -1L,
    )
}
