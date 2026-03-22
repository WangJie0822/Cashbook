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

import cn.wj.android.cashbook.core.database.dao.TypeDao
import cn.wj.android.cashbook.core.database.table.TypeTable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * TypeDao 的测试替身，使用内存列表存储数据
 */
class FakeTypeDao : TypeDao {

    /** 类型数据列表 */
    private val typesFlow = MutableStateFlow<List<TypeTable>>(emptyList())

    /** 自增主键计数器 */
    private var nextId = 1L

    /** 获取当前类型列表的快照 */
    val types: List<TypeTable>
        get() = typesFlow.value

    override suspend fun insertType(type: TypeTable): Long {
        val id = type.id ?: nextId++
        val withId = type.copy(id = id)
        typesFlow.value = typesFlow.value + withId
        return id
    }

    override suspend fun insertOrReplace(type: TypeTable): Long {
        val existing = typesFlow.value.indexOfFirst { it.id == type.id }
        return if (existing >= 0) {
            // 替换
            val mutable = typesFlow.value.toMutableList()
            mutable[existing] = type
            typesFlow.value = mutable
            type.id!!
        } else {
            // 插入
            insertType(type)
        }
    }

    override suspend fun insertTypes(vararg types: TypeTable) {
        val newTypes = types.map { type ->
            if (type.id == null) type.copy(id = nextId++) else type
        }
        typesFlow.value = typesFlow.value + newTypes
    }

    override fun queryAll(): Flow<List<TypeTable>> {
        return typesFlow
    }

    override fun queryByTypeCategory(typeCategory: Int): Flow<List<TypeTable>> {
        return typesFlow.map { list -> list.filter { it.typeCategory == typeCategory } }
    }

    override suspend fun queryByLevelAndTypeCategory(
        typeLevel: Int,
        typeCategory: Int,
    ): List<TypeTable> {
        return typesFlow.value.filter {
            it.typeLevel == typeLevel && it.typeCategory == typeCategory
        }
    }

    override suspend fun queryByLevel(typeLevel: Int): List<TypeTable> {
        return typesFlow.value.filter { it.typeLevel == typeLevel }
    }

    override suspend fun queryByParentId(parentId: Long): List<TypeTable> {
        return typesFlow.value.filter { it.parentId == parentId }
    }

    override suspend fun queryById(typeId: Long): TypeTable? {
        return typesFlow.value.firstOrNull { it.id == typeId }
    }

    override suspend fun queryByName(name: String): TypeTable? {
        return typesFlow.value.firstOrNull { it.name == name }
    }

    override suspend fun updateTypeLevel(id: Long, parentId: Long, typeLevel: Int) {
        val mutable = typesFlow.value.toMutableList()
        val index = mutable.indexOfFirst { it.id == id }
        if (index >= 0) {
            mutable[index] = mutable[index].copy(parentId = parentId, typeLevel = typeLevel)
            typesFlow.value = mutable
        }
    }

    override suspend fun deleteById(id: Long) {
        typesFlow.value = typesFlow.value.filter { it.id != id }
    }

    override suspend fun countByName(name: String): Int {
        return typesFlow.value.count { it.name == name }
    }

    override suspend fun countByLevel(level: Int): Int {
        return typesFlow.value.count { it.typeLevel == level }
    }

    override suspend fun countByParentId(parentId: Long): Int {
        return typesFlow.value.count { it.parentId == parentId }
    }

    // 迁移相关方法（用于测试）
    /** 记录数据列表（用于模拟迁移操作） */
    private val records = mutableListOf<FakeRecord>()

    data class FakeRecord(val id: Long, var typeId: Long)

    fun addRecord(id: Long, typeId: Long) {
        records.add(FakeRecord(id, typeId))
    }

    override suspend fun updateRecordTypeId(oldTypeId: Long, newTypeId: Long) {
        records.filter { it.typeId == oldTypeId }.forEach { it.typeId = newTypeId }
    }

    override suspend fun promoteChildTypes(parentId: Long) {
        val mutable = typesFlow.value.toMutableList()
        mutable.forEachIndexed { index, table ->
            if (table.parentId == parentId) {
                mutable[index] = table.copy(parentId = -1L, typeLevel = 0)
            }
        }
        typesFlow.value = mutable
    }

    override suspend fun countRecordsByTypeId(typeId: Long): Int {
        return records.count { it.typeId == typeId }
    }
}
