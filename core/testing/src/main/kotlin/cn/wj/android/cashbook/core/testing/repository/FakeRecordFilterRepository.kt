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

import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.sqlite.db.SupportSQLiteQuery
import cn.wj.android.cashbook.core.data.repository.RecordFilterRepository
import cn.wj.android.cashbook.core.data.repository.RecordFilterValidation
import cn.wj.android.cashbook.core.data.repository.impl.RecordFilterRepositoryImpl
import cn.wj.android.cashbook.core.database.dao.RecordFilterDao
import cn.wj.android.cashbook.core.database.relation.LauncherRecordViewRelation
import cn.wj.android.cashbook.core.database.relation.RecordFilterSummaryRelation
import cn.wj.android.cashbook.core.database.relation.RecordSearchStatisticsRelation
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.model.enums.TypeLevelEnum
import cn.wj.android.cashbook.core.model.model.RECORD_TYPE_BALANCE_EXPENDITURE
import cn.wj.android.cashbook.core.model.model.RecordCategoryNet
import cn.wj.android.cashbook.core.model.model.RecordFilterRequest
import cn.wj.android.cashbook.core.model.model.RecordFilterSummary
import cn.wj.android.cashbook.core.model.model.RecordKeywordQuery
import cn.wj.android.cashbook.core.model.model.RecordSearchStatistics
import cn.wj.android.cashbook.core.model.model.RecordTypeModel
import cn.wj.android.cashbook.core.model.model.RecordViewsModel
import cn.wj.android.cashbook.core.model.model.TagModel
import cn.wj.android.cashbook.core.model.model.recordFilterContribution
import cn.wj.android.cashbook.core.model.transfer.asEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/** 内存测试替身；SQL 正确性由真实 Room 测试承担，参数预算复用实际仓库。 */
class FakeRecordFilterRepository(
    private val catalog: Flow<List<RecordTypeModel>> = flowOf(emptyList()),
    private val tags: Flow<List<TagModel>> = flowOf(emptyList()),
) : RecordFilterRepository {
    private val records = MutableStateFlow<List<RecordViewsModel>>(emptyList())
    val recordRequests = mutableListOf<RecordFilterRequest>()
    val summaryRequests = mutableListOf<RecordFilterRequest>()
    val keywordRequests = mutableListOf<RecordKeywordQuery>()
    val combinedRecordRequests = mutableListOf<Pair<RecordFilterRequest, RecordKeywordQuery>>()
    val combinedStatisticsRequests = mutableListOf<Pair<RecordFilterRequest, RecordKeywordQuery>>()
    var validationOverride: RecordFilterValidation? = null
    var summaryProvider: ((RecordFilterRequest) -> Flow<RecordFilterSummary>)? = null
    var keywordStatisticsProvider: ((RecordKeywordQuery) -> Flow<RecordSearchStatistics>)? = null
    var recordsProvider: ((RecordFilterRequest) -> Flow<PagingData<RecordViewsModel>>)? = null
    var combinedRecordsProvider: ((RecordFilterRequest, RecordKeywordQuery) -> Flow<PagingData<RecordViewsModel>>)? = null
    var combinedStatisticsProvider: ((RecordFilterRequest, RecordKeywordQuery) -> Flow<RecordSearchStatistics>)? = null

    private val validator = RecordFilterRepositoryImpl(
        object : RecordFilterDao {
            override fun pagingRecords(query: SupportSQLiteQuery): PagingSource<Int, LauncherRecordViewRelation> = error("No database IO in fake validation")
            override fun observeSummary(query: SupportSQLiteQuery): Flow<RecordFilterSummaryRelation> = error("No database IO in fake validation")
            override fun observeStatistics(query: SupportSQLiteQuery): Flow<List<RecordSearchStatisticsRelation>> = error("No database IO in fake validation")
        },
        Dispatchers.Unconfined,
    )

    fun setRecords(value: List<RecordViewsModel>) { records.value = value.toList() }
    override fun validate(request: RecordFilterRequest) = validationOverride ?: validator.validate(request)
    override fun validate(request: RecordFilterRequest, hasKeyword: Boolean) = validationOverride ?: validator.validate(request, hasKeyword)
    override fun observeRecords(request: RecordFilterRequest): Flow<PagingData<RecordViewsModel>> {
        recordRequests += request
        return recordsProvider?.invoke(request) ?: matching(request).map { PagingData.from(it, sourceLoadStates = LoadStates(LoadState.NotLoading(false), LoadState.NotLoading(true), LoadState.NotLoading(true))) }
    }

    override fun observeRecords(request: RecordFilterRequest, keyword: RecordKeywordQuery): Flow<PagingData<RecordViewsModel>> {
        require(request.bookId == keyword.bookId) { "Keyword and filter book must match" }
        combinedRecordRequests += request to keyword
        return combinedRecordsProvider?.invoke(request, keyword) ?: matching(request).map { rows ->
            PagingData.from(rows.filter { keyword.keyword.isBlank() || matchesKeyword(it, keyword) }, sourceLoadStates = LoadStates(LoadState.NotLoading(false), LoadState.NotLoading(true), LoadState.NotLoading(true)))
        }
    }
    override fun observeSummary(request: RecordFilterRequest): Flow<RecordFilterSummary> {
        summaryRequests += request
        return summaryProvider?.invoke(request) ?: matching(request).map { rows ->
            val contributions = rows.map { recordFilterContribution(it.asEntity()) }
            RecordFilterSummary(rows.size.toLong(), contributions.sumOf { it.income }, contributions.sumOf { it.expenditure })
        }
    }

    override fun observeStatistics(request: RecordFilterRequest): Flow<RecordSearchStatistics> {
        if (summaryProvider != null) return observeSummary(request).map { RecordSearchStatistics(it, emptyList()) }
        summaryRequests += request
        return combine(matching(request, normalizeTypes = false), catalog) { rows, types -> statistics(rows, types) }
    }

    override fun observeKeywordStatistics(query: RecordKeywordQuery): Flow<RecordSearchStatistics> {
        keywordRequests += query
        return keywordStatisticsProvider?.invoke(query) ?: combine(records, catalog) { rows, types ->
            statistics(
                rows.filter { matchesKeyword(it, query) },
                types,
            )
        }
    }

    override fun observeStatistics(request: RecordFilterRequest, keyword: RecordKeywordQuery): Flow<RecordSearchStatistics> {
        require(request.bookId == keyword.bookId) { "Keyword and filter book must match" }
        combinedStatisticsRequests += request to keyword
        return combinedStatisticsProvider?.invoke(request, keyword) ?: combine(matching(request, normalizeTypes = false), catalog) { rows, types ->
            statistics(rows.filter { keyword.keyword.isBlank() || matchesKeyword(it, keyword) }, types)
        }
    }

    private fun matchesKeyword(row: RecordViewsModel, query: RecordKeywordQuery): Boolean = row.booksId == query.bookId &&
        (sqliteLikeContains(row.remark, query.keyword) || (query.amountCent != -1L && (row.amount == query.amountCent || row.finalAmount == query.amountCent)))

    private fun statistics(rows: List<RecordViewsModel>, types: List<RecordTypeModel>): RecordSearchStatistics {
        val byId = types.associateBy { it.id }
        val groups = mutableMapOf<Pair<RecordTypeCategoryEnum, Long>, RecordCategoryNet>()
        var income = 0L
        var expenditure = 0L
        rows.forEach { row ->
            if (row.isBalanceRecord) return@forEach
            val type = byId[row.type.id] ?: return@forEach
            val amount = when (type.typeCategory) {
                RecordTypeCategoryEnum.TRANSFER -> Math.subtractExact(row.charges, row.concessions)
                else -> row.finalAmount
            }
            if (type.typeCategory == RecordTypeCategoryEnum.INCOME) {
                income = Math.addExact(income, amount)
            } else {
                expenditure = Math.addExact(expenditure, amount)
            }
            val group = byId[type.parentId]?.takeIf {
                type.typeLevel == TypeLevelEnum.SECOND && it.parentId == -1L && it.typeLevel == TypeLevelEnum.FIRST && it.typeCategory == type.typeCategory
            } ?: type
            val key = type.typeCategory to group.id
            groups[key] = RecordCategoryNet(group.id, type.typeCategory, group.name, Math.addExact(groups[key]?.amount ?: 0L, amount))
        }
        val categories = groups.values.toList()
        val result = RecordSearchStatistics(RecordFilterSummary(rows.size.toLong(), income, expenditure), categories)
        return result.copy(categories = categories.sortedWith(compareByDescending<RecordCategoryNet> { kotlin.math.abs(it.amount) }.thenBy { it.typeId }.thenBy { it.typeCategory.code }))
    }

    private fun matching(request: RecordFilterRequest, normalizeTypes: Boolean = true) = combine(records, catalog, tags) { rows, types, allTags ->
        val byId = types.associateBy { it.id }
        val groups = request.categories.wholeGroupIds.filter { byId[it]?.let { t -> t.parentId == -1L && t.typeLevel == TypeLevelEnum.FIRST } == true }.toSet()
        val existingTags = allTags.map { it.id }.toSet()
        rows.filter { row ->
            val type = byId[row.type.id] ?: row.type.takeIf { row.isBalanceRecord }
            val category = request.categories
            row.booksId == request.bookId && row.recordTime >= request.fromInclusive && row.recordTime < request.toExclusive &&
                (request.typeCategories.isEmpty() || type?.typeCategory in request.typeCategories) &&
                (
                    category.isUnrestricted || (
                        row.type.id !in category.excludedTypeIds &&
                            ((type != null && type.id in category.exactTypeIds) || row.type.id in groups || type?.parentId in groups)
                        )
                    ) &&
                (request.tagIds.isEmpty() || row.relatedTags.any { it.id in request.tagIds && it.id in existingTags })
        }.distinctBy { it.id }.map { if (normalizeTypes) it.copy(type = byId[it.type.id] ?: it.type.takeIf { type -> type.id == -1101L || type.id == -1102L } ?: RECORD_TYPE_BALANCE_EXPENDITURE) else it }
            .sortedWith(compareByDescending<RecordViewsModel> { it.recordTime }.thenByDescending { it.id })
    }
}
