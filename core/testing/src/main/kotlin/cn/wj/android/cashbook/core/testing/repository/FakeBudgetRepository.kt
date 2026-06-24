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

package cn.wj.android.cashbook.core.testing.repository

import cn.wj.android.cashbook.core.data.repository.BudgetRepository
import cn.wj.android.cashbook.core.model.model.BudgetModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * [BudgetRepository] 测试替身，内存存储，忠实复刻 (books_id, type_id) 唯一约束 upsert 语义。
 */
class FakeBudgetRepository : BudgetRepository {

    private val data = MutableStateFlow<List<BudgetModel>>(emptyList())

    override fun getBudgetsByBooksFlow(booksId: Long): Flow<List<BudgetModel>> =
        data.map { list -> list.filter { it.booksId == booksId } }

    override suspend fun getBudgetsByBooks(booksId: Long): List<BudgetModel> =
        data.value.filter { it.booksId == booksId }

    override suspend fun upsertBudget(booksId: Long, typeId: Long, amount: Long) {
        // 复刻 (books_id, type_id) 唯一：存在则替换，否则新增
        val others = data.value.filterNot { it.booksId == booksId && it.typeId == typeId }
        data.value = others + BudgetModel(id = null, booksId = booksId, typeId = typeId, amount = amount)
    }

    override suspend fun deleteBudget(booksId: Long, typeId: Long) {
        data.value = data.value.filterNot { it.booksId == booksId && it.typeId == typeId }
    }
}
