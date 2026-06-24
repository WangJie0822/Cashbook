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

import cn.wj.android.cashbook.core.common.annotation.CashbookDispatchers
import cn.wj.android.cashbook.core.common.annotation.Dispatcher
import cn.wj.android.cashbook.core.data.repository.BudgetRepository
import cn.wj.android.cashbook.core.database.dao.BudgetDao
import cn.wj.android.cashbook.core.database.table.BudgetTable
import cn.wj.android.cashbook.core.model.model.BudgetModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

/**
 * 预算数据仓库实现
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2026/6/23
 */
class BudgetRepositoryImpl @Inject constructor(
    private val budgetDao: BudgetDao,
    @Dispatcher(CashbookDispatchers.IO) private val coroutineContext: CoroutineContext,
) : BudgetRepository {

    override fun getBudgetsByBooksFlow(booksId: Long): Flow<List<BudgetModel>> =
        budgetDao.queryByBooksFlow(booksId).map { list -> list.map { it.asBudgetModel() } }

    override suspend fun getBudgetsByBooks(booksId: Long): List<BudgetModel> =
        withContext(coroutineContext) {
            budgetDao.queryByBooks(booksId).map { it.asBudgetModel() }
        }

    override suspend fun upsertBudget(booksId: Long, typeId: Long, amount: Long): Unit =
        withContext(coroutineContext) {
            // 先查 existing.id 再 @Upsert：@Upsert 按主键 id 冲突，已存在记录须带其 id 才走 UPDATE；
            // 若传 id=null，会按 (books_id, type_id) 唯一索引撞约束抛 SQLiteConstraintException。
            // 「先查后写」存在理论竞态窗口（查到 null 但写时已被插入），但单用户记账几乎不可能并发设同一预算，
            // 故保留此正确清晰的实现，不改为 @Insert(onConflict=REPLACE)——后者删旧插新会改变 id 语义。
            val existing = budgetDao.queryByBooksAndType(booksId, typeId)
            budgetDao.upsert(
                BudgetTable(id = existing?.id, booksId = booksId, typeId = typeId, amount = amount),
            )
        }

    override suspend fun deleteBudget(booksId: Long, typeId: Long): Unit =
        withContext(coroutineContext) {
            budgetDao.deleteByBooksAndType(booksId, typeId)
        }
}

private fun BudgetTable.asBudgetModel() =
    BudgetModel(id = id, booksId = booksId, typeId = typeId, amount = amount)
