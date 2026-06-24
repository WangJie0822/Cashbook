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
import androidx.room.Query
import androidx.room.Upsert
import cn.wj.android.cashbook.core.database.table.BudgetTable
import kotlinx.coroutines.flow.Flow

/**
 * 预算数据库操作类
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2026/6/23
 */
@Dao
interface BudgetDao {

    @Query("SELECT * FROM db_budget WHERE books_id = :booksId")
    fun queryByBooksFlow(booksId: Long): Flow<List<BudgetTable>>

    @Query("SELECT * FROM db_budget WHERE books_id = :booksId")
    suspend fun queryByBooks(booksId: Long): List<BudgetTable>

    @Query("SELECT * FROM db_budget WHERE books_id = :booksId AND type_id = :typeId")
    suspend fun queryByBooksAndType(booksId: Long, typeId: Long): BudgetTable?

    @Upsert
    suspend fun upsert(budget: BudgetTable)

    @Query("DELETE FROM db_budget WHERE books_id = :booksId AND type_id = :typeId")
    suspend fun deleteByBooksAndType(booksId: Long, typeId: Long)

    @Query("DELETE FROM db_budget WHERE books_id = :booksId")
    suspend fun deleteByBooksId(booksId: Long)

    @Query("DELETE FROM db_budget WHERE type_id = :typeId")
    suspend fun deleteByTypeId(typeId: Long)
}
