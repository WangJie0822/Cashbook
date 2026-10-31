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
import androidx.sqlite.db.SupportSQLiteQuery
import cn.wj.android.cashbook.core.database.dao.RecordFilterDao
import cn.wj.android.cashbook.core.database.relation.LauncherRecordViewRelation
import cn.wj.android.cashbook.core.database.relation.RecordFilterSummaryRelation
import cn.wj.android.cashbook.core.database.relation.RecordSearchStatisticsRelation
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.model.model.CategoryFilterSelection
import cn.wj.android.cashbook.core.model.model.RecordCategoryNet
import cn.wj.android.cashbook.core.model.model.RecordFilterRequest
import cn.wj.android.cashbook.core.model.model.RecordFilterSummary
import cn.wj.android.cashbook.core.model.model.RecordKeywordQuery
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertFailsWith

class RecordSearchStatisticsRepositoryTest {
    @Test fun groupedRows_produceAtomicSummaryAndSignedDistribution() = runTest {
        val repo = repository(listOf(row(10, count = 2, expenditure = 200), row(20, category = 1, income = 700), row(30, category = 2, expenditure = -300), row(40, expenditure = 0), row(-1101, count = 3, excluded = true), row(90, count = 2, category = null, excluded = true)))
        val result = repo.observeStatistics(request()).first()
        assertThat(result.summary).isEqualTo(RecordFilterSummary(10, 700, -100))
        assertThat(result.summary.balance).isEqualTo(800)
        assertThat(result.magnitude).isEqualTo(1200)
        assertThat(result.categories).containsExactly(
            RecordCategoryNet(20, RecordTypeCategoryEnum.INCOME, "同名", 700),
            RecordCategoryNet(30, RecordTypeCategoryEnum.TRANSFER, "同名", -300),
            RecordCategoryNet(10, RecordTypeCategoryEnum.EXPENDITURE, "同名", 200),
            RecordCategoryNet(40, RecordTypeCategoryEnum.EXPENDITURE, "同名", 0),
        ).inOrder()
    }

    @Test fun emptyRows_produceZeroWithoutInventingCategory() = runTest {
        val result = repository(emptyList()).observeStatistics(request()).first()
        assertThat(result.summary).isEqualTo(RecordFilterSummary(0, 0, 0))
        assertThat(result.categories).isEmpty()
    }

    @Test fun equalMagnitudes_haveStableIdOrder() = runTest {
        val result = repository(listOf(row(20, expenditure = 300), row(10, expenditure = -300))).observeStatistics(request()).first()
        assertThat(result.categories.map { it.typeId }).containsExactly(10L, 20L).inOrder()
    }

    @Test fun overflowingTransfer_isFailureInsteadOfZero() = runTest {
        assertFailsWith<ArithmeticException> {
            repository(listOf(row(30, category = 2, invalid = 1))).observeKeywordStatistics(RecordKeywordQuery(1, "测试", -1)).first()
        }
    }

    @Test fun sumAcrossGroups_cannotWrap() = runTest {
        assertFailsWith<ArithmeticException> {
            repository(listOf(row(10, expenditure = Long.MAX_VALUE), row(20, expenditure = 1))).observeStatistics(request()).first()
        }
    }

    @Test fun sqliteFailure_isPropagated() = runTest {
        val failure = IllegalStateException("integer overflow")
        val repo = RecordFilterRepositoryImpl(Dao(emptyList(), failure), Dispatchers.Unconfined)
        assertThat(assertFailsWith<IllegalStateException> { repo.observeStatistics(request()).first() }.message).isEqualTo("integer overflow")
    }

    private fun request() = RecordFilterRequest(1, 0, 2000, emptySet(), CategoryFilterSelection(), emptySet())
    private fun repository(rows: List<RecordSearchStatisticsRelation>) = RecordFilterRepositoryImpl(Dao(rows), Dispatchers.Unconfined)
    private fun row(id: Long, category: Int? = 0, count: Long = 1, income: Long = 0, expenditure: Long = 0, excluded: Boolean = false, invalid: Long = 0) =
        RecordSearchStatisticsRelation(id, "同名", category, excluded, count, income, expenditure, invalid)

    private class Dao(val rows: List<RecordSearchStatisticsRelation>, val failure: Throwable? = null) : RecordFilterDao {
        override fun observeStatistics(query: SupportSQLiteQuery): Flow<List<RecordSearchStatisticsRelation>> = flow {
            failure?.let { throw it }
            emit(rows)
        }
        override fun observeSummary(query: SupportSQLiteQuery): Flow<RecordFilterSummaryRelation> = flowOf(RecordFilterSummaryRelation(0, 0, 0))
        override fun pagingRecords(query: SupportSQLiteQuery): PagingSource<Int, LauncherRecordViewRelation> = error("statistics must not load records")
    }
}
