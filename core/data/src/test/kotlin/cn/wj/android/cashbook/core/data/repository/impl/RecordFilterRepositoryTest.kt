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

import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.sqlite.db.SupportSQLiteQuery
import cn.wj.android.cashbook.core.data.repository.RecordFilterValidation
import cn.wj.android.cashbook.core.database.dao.RecordFilterDao
import cn.wj.android.cashbook.core.database.relation.LauncherRecordViewRelation
import cn.wj.android.cashbook.core.database.relation.RecordFilterSummaryRelation
import cn.wj.android.cashbook.core.database.relation.RecordSearchStatisticsRelation
import cn.wj.android.cashbook.core.model.model.CategoryFilterSelection
import cn.wj.android.cashbook.core.model.model.RecordFilterRequest
import cn.wj.android.cashbook.core.model.model.RecordFilterSummary
import cn.wj.android.cashbook.core.model.model.RecordKeywordQuery
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertFailsWith

class RecordFilterRepositoryTest {

    @Test
    fun combinedBudget_precheckReservesAllFourKeywordBindingsWithoutIO() {
        val dao = FakeFilterDao()
        val repo = RecordFilterRepositoryImpl(dao, Dispatchers.Unconfined)
        val lastValid = request().copy(tagIds = (1L..885L).toSet())
        val firstInvalid = request().copy(tagIds = (1L..886L).toSet())
        assertThat(repo.validate(lastValid, true)).isEqualTo(RecordFilterValidation.Valid)
        assertThat(repo.validate(firstInvalid, false)).isEqualTo(RecordFilterValidation.Valid)
        assertThat(repo.validate(firstInvalid, true)).isEqualTo(RecordFilterValidation.TooManyParameters(901, 900))
        assertThat(dao.calls).isEqualTo(0)
    }

    @Test
    fun combinedExecution_rechecksBudgetBeforeAnyStatisticsOrPagingIO() = runTest {
        val dao = FakeFilterDao()
        val repo = RecordFilterRepositoryImpl(dao, Dispatchers.Unconfined)
        val filter = request().copy(tagIds = (1L..886L).toSet())
        val query = RecordKeywordQuery(2L, "午餐", -1L)
        assertFailsWith<IllegalArgumentException> { repo.observeStatistics(filter, query).first() }
        assertFailsWith<IllegalArgumentException> { repo.observeRecords(filter, query).first() }
        assertThat(dao.calls).isEqualTo(0)
    }

    @Test
    fun combinedExecution_rejectsCrossBookQueryWithoutIO() = runTest {
        val dao = FakeFilterDao()
        val repo = RecordFilterRepositoryImpl(dao, Dispatchers.Unconfined)
        val query = RecordKeywordQuery(7L, "午餐", -1L)
        assertFailsWith<IllegalArgumentException> { repo.observeStatistics(request(), query).first() }
        assertFailsWith<IllegalArgumentException> { repo.observeRecords(request(), query).first() }
        assertThat(dao.calls).isEqualTo(0)
    }

    @Test
    fun validRequest_doesNotQueryDuringValidation() {
        val dao = FakeFilterDao()
        val repo = RecordFilterRepositoryImpl(dao, Dispatchers.Unconfined)
        assertThat(repo.validate(request())).isEqualTo(RecordFilterValidation.Valid)
        assertThat(dao.calls).isEqualTo(0)
    }

    @Test
    fun mixedIdCollections_exceedWholeStatementBudget() {
        val repo = RecordFilterRepositoryImpl(FakeFilterDao(), Dispatchers.Unconfined)
        val result = repo.validate(request().copy(categories = CategoryFilterSelection(exactTypeIds = (1L..600L).toSet()), tagIds = (1L..300L).toSet()))
        assertThat(result).isInstanceOf(RecordFilterValidation.TooManyParameters::class.java)
        val failure = result as RecordFilterValidation.TooManyParameters
        assertThat(failure.limit).isEqualTo(900)
        assertThat(failure.required).isGreaterThan(900)
    }

    @Test
    fun reversedDateRange_isRejected() {
        val repo = RecordFilterRepositoryImpl(FakeFilterDao(), Dispatchers.Unconfined)
        assertThat(repo.validate(request().copy(fromInclusive = 2000L, toExclusive = 1000L)))
            .isEqualTo(RecordFilterValidation.InvalidRange)
    }

    @Test
    fun emptyDateRange_isRejected() {
        val repo = RecordFilterRepositoryImpl(FakeFilterDao(), Dispatchers.Unconfined)
        assertThat(repo.validate(request().copy(toExclusive = 1000L))).isEqualTo(RecordFilterValidation.InvalidRange)
    }

    @Test
    fun summary_mapsRealRepositoryOutput() = runTest {
        val repo = RecordFilterRepositoryImpl(FakeFilterDao(), Dispatchers.Unconfined)
        assertThat(repo.observeSummary(request()).first()).isEqualTo(RecordFilterSummary(3L, 800L, 500L))
    }

    @Test
    fun bypassingValidation_stillCannotQueryOverBudget() = runTest {
        val dao = FakeFilterDao()
        val repo = RecordFilterRepositoryImpl(dao, Dispatchers.Unconfined)
        val oversized = request().copy(tagIds = (1L..901L).toSet())
        val failure = assertFailsWith<IllegalArgumentException> { repo.observeSummary(oversized).first() }
        assertThat(failure.message).contains("budget")
        assertThat(dao.calls).isEqualTo(0)
    }

    @Test
    fun oversizedPaging_isRejectedBeforeCreatingSource() = runTest {
        val dao = FakeFilterDao()
        val repo = RecordFilterRepositoryImpl(dao, Dispatchers.Unconfined)
        val failure = assertFailsWith<IllegalArgumentException> { repo.observeRecords(request().copy(tagIds = (1L..901L).toSet())).first() }
        assertThat(failure.message).contains("budget")
        assertThat(dao.calls).isEqualTo(0)
    }

    private fun request() = RecordFilterRequest(2L, 1000L, 2000L, emptySet(), CategoryFilterSelection(), emptySet())

    /** 守卫失败时也只返回有限桩结果，不会意外打开真实数据库或网络。 */
    private class FakeFilterDao : RecordFilterDao {
        var calls = 0
        override fun observeStatistics(query: SupportSQLiteQuery): Flow<List<RecordSearchStatisticsRelation>> { calls++; return flowOf(emptyList()) }
        override fun observeSummary(query: SupportSQLiteQuery): Flow<RecordFilterSummaryRelation> {
            calls++
            return flowOf(RecordFilterSummaryRelation(3L, 800L, 500L))
        }

        override fun pagingRecords(query: SupportSQLiteQuery): PagingSource<Int, LauncherRecordViewRelation> {
            calls++
            return object : PagingSource<Int, LauncherRecordViewRelation>() {
                override fun getRefreshKey(state: PagingState<Int, LauncherRecordViewRelation>): Int? = null
                override suspend fun load(params: LoadParams<Int>): LoadResult<Int, LauncherRecordViewRelation> =
                    LoadResult.Page(emptyList(), null, null)
            }
        }
    }
}
