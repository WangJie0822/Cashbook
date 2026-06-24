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

package cn.wj.android.cashbook.core.data.repository

import cn.wj.android.cashbook.core.model.model.BudgetModel
import kotlinx.coroutines.flow.Flow

/**
 * 预算数据仓库
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2026/6/23
 */
interface BudgetRepository {

    /** 按账本订阅预算列表（驱动 UI 响应式刷新） */
    fun getBudgetsByBooksFlow(booksId: Long): Flow<List<BudgetModel>>

    /** 一次性查询账本下的预算列表 */
    suspend fun getBudgetsByBooks(booksId: Long): List<BudgetModel>

    /** 设置/更新限额（按 books_id + type_id upsert） */
    suspend fun upsertBudget(booksId: Long, typeId: Long, amount: Long)

    /** 删除单项预算 */
    suspend fun deleteBudget(booksId: Long, typeId: Long)
}
