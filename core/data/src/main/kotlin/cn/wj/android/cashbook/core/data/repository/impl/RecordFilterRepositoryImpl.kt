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
import cn.wj.android.cashbook.core.database.relation.RecordSearchStatisticsRelation
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.model.model.RecordCategoryNet
import cn.wj.android.cashbook.core.model.model.RecordFilterRequest
import cn.wj.android.cashbook.core.model.model.RecordFilterSummary
import cn.wj.android.cashbook.core.model.model.RecordKeywordQuery
import cn.wj.android.cashbook.core.model.model.RecordSearchStatistics
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
    override fun validate(request: RecordFilterRequest): RecordFilterValidation = validate(request, false)

    override fun validate(request: RecordFilterRequest, hasKeyword: Boolean): RecordFilterValidation {
        if (request.fromInclusive >= request.toExclusive) return RecordFilterValidation.InvalidRange
        val base = RecordFilterSqlBuilder.build(request).requiredBindCount
        return budgetValidation(Math.addExact(base, if (hasKeyword) RecordFilterSqlBuilder.KEYWORD_BIND_COUNT else 0))
    }

    override fun observeRecords(request: RecordFilterRequest): Flow<PagingData<RecordViewsModel>> = records(request, null)

    override fun observeRecords(request: RecordFilterRequest, keyword: RecordKeywordQuery): Flow<PagingData<RecordViewsModel>> = records(request, keyword)

    override fun observeStatistics(request: RecordFilterRequest, keyword: RecordKeywordQuery): Flow<RecordSearchStatistics> = statistics(request, keyword)

    private fun records(request: RecordFilterRequest, keyword: RecordKeywordQuery?): Flow<PagingData<RecordViewsModel>> = flow {
        val query = checkedQueries(request, keyword).records
        emitAll(
            Pager(PagingConfig(pageSize = 20, initialLoadSize = 20), pagingSourceFactory = { dao.pagingRecords(query) })
                .flow.map { paging -> paging.map { it.toRecordViewsModel() } },
        )
    }.flowOn(coroutineContext)

    override fun observeSummary(request: RecordFilterRequest): Flow<RecordFilterSummary> = flow {
        val query = checkedQueries(request).summary
        emitAll(dao.observeSummary(query).map { RecordFilterSummary(it.totalCount, it.income, it.expenditure) })
    }.flowOn(coroutineContext)

    override fun observeStatistics(request: RecordFilterRequest): Flow<RecordSearchStatistics> = statistics(request, null)

    private fun statistics(request: RecordFilterRequest, keyword: RecordKeywordQuery?): Flow<RecordSearchStatistics> = flow {
        emitAll(dao.observeStatistics(checkedQueries(request, keyword).statistics).map { it.toStatistics() })
    }.flowOn(coroutineContext)

    override fun observeKeywordStatistics(query: RecordKeywordQuery): Flow<RecordSearchStatistics> = flow {
        emitAll(dao.observeStatistics(RecordSearchStatisticsSqlBuilder.keyword(query)).map { it.toStatistics() })
    }.flowOn(coroutineContext)

    private fun checkedQueries(request: RecordFilterRequest, keyword: RecordKeywordQuery? = null): RecordFilterQueries {
        require(request.fromInclusive < request.toExclusive) { "Invalid record filter range" }
        val queries = RecordFilterSqlBuilder.build(request, keyword)
        require(budgetValidation(queries.requiredBindCount) == RecordFilterValidation.Valid) {
            "Record filter bind budget exceeded (${queries.requiredBindCount}/$SQL_IN_CHUNK_SIZE)"
        }
        return queries
    }

    private fun budgetValidation(required: Int): RecordFilterValidation =
        if (required > SQL_IN_CHUNK_SIZE) RecordFilterValidation.TooManyParameters(required, SQL_IN_CHUNK_SIZE) else RecordFilterValidation.Valid
}

/** 不额外查询汇总；所有计数与金额来自同一次 SQL 发射。 */
private fun List<RecordSearchStatisticsRelation>.toStatistics(): RecordSearchStatistics {
    var count = 0L
    var income = 0L
    var expenditure = 0L
    val categories = mapNotNull { row ->
        if (row.invalidAmounts != 0L) throw ArithmeticException("Transfer contribution overflow")
        count = Math.addExact(count, row.totalCount)
        income = Math.addExact(income, row.income)
        expenditure = Math.addExact(expenditure, row.expenditure)
        val category = RecordTypeCategoryEnum.entries.firstOrNull { it.code == row.typeCategory }
        if (row.excluded || category == null) return@mapNotNull null
        RecordCategoryNet(row.typeId, category, requireNotNull(row.typeName), if (category == RecordTypeCategoryEnum.INCOME) row.income else row.expenditure)
    }
    val statistics = RecordSearchStatistics(RecordFilterSummary(count, income, expenditure), categories)
    return statistics.copy(categories = categories.sortedWith(compareByDescending<RecordCategoryNet> { kotlin.math.abs(it.amount) }.thenBy { it.typeId }.thenBy { it.typeCategory.code }))
}
