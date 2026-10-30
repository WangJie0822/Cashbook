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

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import cn.wj.android.cashbook.core.common.annotation.CashbookDispatchers
import cn.wj.android.cashbook.core.common.annotation.Dispatcher
import cn.wj.android.cashbook.core.data.repository.RecordFilterRepository
import cn.wj.android.cashbook.core.data.repository.RecordFilterValidation
import cn.wj.android.cashbook.core.database.dao.RecordFilterDao
import cn.wj.android.cashbook.core.model.model.RecordFilterRequest
import cn.wj.android.cashbook.core.model.model.RecordFilterSummary
import cn.wj.android.cashbook.core.model.model.RecordViewsModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class RecordFilterRepositoryImpl @Inject constructor(
    private val dao: RecordFilterDao,
    @param:Dispatcher(CashbookDispatchers.IO) private val coroutineContext: CoroutineContext,
) : RecordFilterRepository {
    override fun validate(request: RecordFilterRequest): RecordFilterValidation {
        if (request.fromInclusive >= request.toExclusive) return RecordFilterValidation.InvalidRange
        return budgetValidation(RecordFilterSqlBuilder.build(request).requiredBindCount)
    }

    override fun observeRecords(request: RecordFilterRequest): Flow<PagingData<RecordViewsModel>> = flow {
        val query = checkedQueries(request).records
        emitAll(
            Pager(PagingConfig(pageSize = 20, initialLoadSize = 20), pagingSourceFactory = { dao.pagingRecords(query) })
                .flow.map { paging -> paging.map { it.toRecordViewsModel() } },
        )
    }.flowOn(coroutineContext)

    override fun observeSummary(request: RecordFilterRequest): Flow<RecordFilterSummary> = flow {
        val query = checkedQueries(request).summary
        emitAll(dao.observeSummary(query).map { RecordFilterSummary(it.totalCount, it.income, it.expenditure) })
    }.flowOn(coroutineContext)

    private fun checkedQueries(request: RecordFilterRequest): RecordFilterQueries {
        require(request.fromInclusive < request.toExclusive) { "Invalid record filter range" }
        val queries = RecordFilterSqlBuilder.build(request)
        require(budgetValidation(queries.requiredBindCount) == RecordFilterValidation.Valid) {
            "Record filter bind budget exceeded (${queries.requiredBindCount}/$SQL_IN_CHUNK_SIZE)"
        }
        return queries
    }

    private fun budgetValidation(required: Int): RecordFilterValidation =
        if (required > SQL_IN_CHUNK_SIZE) RecordFilterValidation.TooManyParameters(required, SQL_IN_CHUNK_SIZE) else RecordFilterValidation.Valid
}
