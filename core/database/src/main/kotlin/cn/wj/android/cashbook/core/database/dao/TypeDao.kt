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

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import cn.wj.android.cashbook.core.database.table.TypeTable
import kotlinx.coroutines.flow.Flow

/**
 * 记录类型数据库操作类
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/2/20
 */
@Dao
interface TypeDao {

    @Insert
    suspend fun insertType(type: TypeTable): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(type: TypeTable): Long

    @Insert
    suspend fun insertTypes(vararg types: TypeTable)

    @Query("SELECT * FROM db_type")
    fun queryAll(): Flow<List<TypeTable>>

    @Query("SELECT * FROM db_type WHERE type_category=:typeCategory")
    fun queryByTypeCategory(typeCategory: Int): Flow<List<TypeTable>>

    @Query("SELECT * FROM db_type WHERE type_level=:typeLevel AND type_category=:typeCategory")
    suspend fun queryByLevelAndTypeCategory(typeLevel: Int, typeCategory: Int): List<TypeTable>

    @Query("SELECT * FROM db_type WHERE type_level=:typeLevel")
    suspend fun queryByLevel(typeLevel: Int): List<TypeTable>

    @Query("SELECT * FROM db_type WHERE parent_id=:parentId")
    suspend fun queryByParentId(parentId: Long): List<TypeTable>

    @Query("SELECT * FROM db_type WHERE id=:typeId")
    suspend fun queryById(typeId: Long): TypeTable?

    @Query("SELECT * FROM db_type WHERE name=:name")
    suspend fun queryByName(name: String): TypeTable?

    @Query("UPDATE db_type SET parent_id=:parentId, type_level=:typeLevel WHERE id=:id")
    suspend fun updateTypeLevel(
        id: Long,
        parentId: Long,
        typeLevel: Int,
    )

    @Query("DELETE FROM db_type WHERE id=:id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM db_type WHERE name=:name")
    suspend fun countByName(name: String): Int

    @Query("SELECT COUNT(*) FROM db_type WHERE type_level=:level")
    suspend fun countByLevel(level: Int): Int

    @Query("SELECT COUNT(*) FROM db_type WHERE parent_id=:parentId")
    suspend fun countByParentId(parentId: Long): Int
}
