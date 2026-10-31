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

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import cn.wj.android.cashbook.core.common.NO_ASSET_ID
import cn.wj.android.cashbook.core.database.CashbookDatabase
import cn.wj.android.cashbook.core.database.table.RecordTable
import cn.wj.android.cashbook.core.database.table.TagTable
import cn.wj.android.cashbook.core.database.table.TagWithRecordTable
import cn.wj.android.cashbook.core.database.table.TypeTable
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.model.model.CategoryFilterSelection
import cn.wj.android.cashbook.core.model.model.RecordFilterRequest
import cn.wj.android.cashbook.core.model.model.RecordFilterSummary
import cn.wj.android.cashbook.core.model.model.RecordKeywordQuery
import cn.wj.android.cashbook.core.model.model.RecordSearchStatistics
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** 同一份合成数据对照既有 RecordDao，不复制一份 SQL 当作预言机。 */
@RunWith(AndroidJUnit4::class)
class RecordSearchStatisticsRoomTest {
    private lateinit var database: CashbookDatabase
    private lateinit var repository: RecordFilterRepositoryImpl

    @Before fun setup() = runBlocking {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), CashbookDatabase::class.java)
            .allowMainThreadQueries().build()
        repository = RecordFilterRepositoryImpl(database.recordFilterDao(), Dispatchers.IO)
        database.typeDao().insertTypes(
            type(10), type(11, parent = 10), type(12, parent = 10), type(20, category = 1), type(30, category = 2),
            type(31, parent = 30, category = 2), type(32, parent = 30, category = 2),
            type(40, parent = 999), type(41, parent = 20), type(42, parent = 11), type(90, category = 99),
        )
    }

    @After fun close() { if (::database.isInitialized) database.close() }

    @Test fun keywordMatchesOldDao_forWildcardsQuotesChineseNumbersAndWhitespace() = runTest {
        // 每笔各用一个一级分类，使聚合返回的组 ID 能独立核对旧 DAO 的实际命中 ID。
        for (id in 101L..105L) database.typeDao().insertType(type(id))
        put(1, type = 101, remark = "中文 买菜", amount = 12345)
        put(2, type = 102, remark = "O'Reilly_%", amount = 900, finalAmount = 12345)
        put(3, type = 103, remark = "OTHER ascii", amount = -1)
        put(4, type = 104, remark = " ", amount = 12345, book = 2)
        put(5, type = 105, remark = "abc\u0000tail")
        val cases = listOf("中文" to -1L, "%" to -1L, "_" to -1L, "'" to -1L, "123.45" to 12345L, "" to -1L, " " to -1L, "missing" to -1L, "other" to -1L, "tail" to -1L, "-1" to -1L)
        for ((keyword, amount) in cases) {
            val query = RecordKeywordQuery(1, keyword, amount)
            val old = database.recordDao().queryRecordByKeyword(1, keyword, amount, 0, 100)
            val stats = repository.observeKeywordStatistics(query).first()
            assertThat(stats.categories.map { it.typeId - 100 }).containsExactlyElementsIn(old.map { it.id!! })
            assertThat(stats.summary.totalCount).isEqualTo(old.size.toLong())
            assertThat(stats.summary.expenditure).isEqualTo(old.sumOf { it.finalAmount })
            assertThat(stats.categories.sumOf { it.amount }).isEqualTo(stats.summary.expenditure)
        }
    }

    @Test fun keywordAggregate_coversEveryOldDaoPageAndScopesBook() = runTest {
        for (id in 1L..61L) {
            database.typeDao().insertType(type(10000 + id))
            put(id, type = 10000 + id, remark = "匹配", amount = id)
        }
        put(62, remark = "匹配", amount = 99999, book = 2)
        put(63, remark = "不命中", amount = 99999)
        val ids = mutableListOf<Long>()
        for (offset in listOf(0, 20, 40, 60)) {
            ids += database.recordDao().queryRecordByKeyword(1, "匹配", -1, offset, 20).map { it.id!! }
        }
        assertThat(ids).containsExactlyElementsIn((1L..61L).toList())
        val stats = repository.observeKeywordStatistics(RecordKeywordQuery(1, "匹配", -1)).first()
        assertThat(stats.categories.map { it.typeId - 10000 }).containsExactlyElementsIn(ids)
        assertThat(stats.summary).isEqualTo(RecordFilterSummary(61, 0, 1891))
        assertThat(stats.categories.sumOf { it.amount }).isEqualTo(1891)
    }

    @Test fun parentAndChildren_netBeforeAbsoluteValueAndKeepZeroGroups() = runTest {
        put(1, type = 30, charge = 500, concessions = 0)
        put(2, type = 31, charge = 0, concessions = 300)
        put(3, type = 32, charge = 0, concessions = 0)
        put(4, type = 10, amount = 100)
        put(5, type = 11, amount = -100)
        put(6, type = 20, amount = 400)
        val stats = repository.observeStatistics(request()).first()
        assertThat(stats.summary).isEqualTo(RecordFilterSummary(6, 400, 200))
        assertThat(stats.categories.associate { it.typeId to it.amount }).containsExactly(30L, 200L, 10L, 0L, 20L, 400L)
        assertThat(stats.magnitude).isEqualTo(600)
        assertThat(stats.categories.map { it.typeName }.toSet()).containsExactly("同名")
        assertThat(stats.categories.map { it.typeCategory }.toSet()).containsExactly(RecordTypeCategoryEnum.INCOME, RecordTypeCategoryEnum.EXPENDITURE, RecordTypeCategoryEnum.TRANSFER)
    }

    @Test fun invalidParents_fallBackToSelfAndUnknownAndBalanceOnlyCount() = runTest {
        for (id in listOf(40L, 41L, 42L)) put(id, type = id, amount = id)
        put(50, type = 9999, amount = 10000)
        put(51, type = 90, amount = 10000)
        put(52, type = -1101, amount = 10000)
        put(53, type = -1102, amount = 10000)
        val stats = repository.observeStatistics(request()).first()
        assertThat(stats.summary).isEqualTo(RecordFilterSummary(7, 0, 123))
        assertThat(stats.categories.map { it.typeId }).containsExactly(40L, 41L, 42L)
        assertThat(stats.magnitude).isEqualTo(123)
    }

    @Test fun duplicateTags_doNotMultiplyStatisticsAndDeletedTagsStopMatching() = runTest {
        database.tagDao().insert(TagTable(1, "一", -1, 0))
        database.tagDao().insert(TagTable(2, "二", -1, 1))
        put(1, amount = 100)
        database.transactionDao().insertRelatedTags(listOf(TagWithRecordTable(null, 1, 1), TagWithRecordTable(null, 1, 2)))
        val request = request().copy(tagIds = setOf(1, 2))
        assertThat(repository.observeStatistics(request).first().summary).isEqualTo(RecordFilterSummary(1, 0, 100))
        database.tagDao().delete(TagTable(1, "一", -1, 0))
        database.tagDao().delete(TagTable(2, "二", -1, 1))
        assertThat(repository.observeStatistics(request).first().summary.totalCount).isEqualTo(0)
    }

    @Test fun malformedFirstLevelWithParent_staysItsOwnGroup() = runTest {
        database.typeDao().insertType(type(43, parent = 10).copy(typeLevel = 0))
        put(1, type = 43, amount = 150)
        val stats = repository.observeStatistics(request()).first()
        assertThat(stats.categories.single().typeId).isEqualTo(43)
        assertThat(stats.categories.single().amount).isEqualTo(150)
    }

    @Test fun noMatches_isZeroWithNoCategories() = runTest {
        put(1)
        val stats = repository.observeKeywordStatistics(RecordKeywordQuery(1, "不存在", -1)).first()
        assertThat(stats.summary).isEqualTo(RecordFilterSummary(0, 0, 0))
        assertThat(stats.categories).isEmpty()
        assertThat(stats.magnitude).isEqualTo(0)
    }

    @Test fun filteredStatistics_matchExistingSummaryWithAllPredicates() = runTest {
        database.tagDao().insert(TagTable(1, "旅行", -1, 0))
        database.tagDao().insert(TagTable(2, "重叠", -1, 1))
        put(1, type = 11, amount = 100)
        put(2, type = 12, amount = 200)
        put(3, type = 20, amount = 300)
        put(4, type = 11, amount = 400, book = 2)
        put(5, type = 11, amount = 500)
        database.transactionDao().insertRelatedTags(listOf(TagWithRecordTable(null, 1, 1), TagWithRecordTable(null, 1, 2), TagWithRecordTable(null, 2, 1), TagWithRecordTable(null, 3, 1), TagWithRecordTable(null, 4, 1)))
        val request = request().copy(
            typeCategories = setOf(RecordTypeCategoryEnum.EXPENDITURE, RecordTypeCategoryEnum.INCOME),
            categories = CategoryFilterSelection(wholeGroupIds = setOf(10), exactTypeIds = setOf(20), excludedTypeIds = setOf(12)),
            tagIds = setOf(1, 2),
        )
        val stats = repository.observeStatistics(request).first()
        assertThat(stats.summary).isEqualTo(RecordFilterSummary(2, 300, 100))
        assertThat(stats.summary).isEqualTo(repository.observeSummary(request).first())
        assertThat(stats.categories.associate { it.typeId to it.amount }).containsExactly(10L, 100L, 20L, 300L)
        assertThat(repository.observeStatistics(request.copy(fromInclusive = 1002)).first().summary).isEqualTo(RecordFilterSummary(1, 300, 0))
        assertThat(repository.observeStatistics(request.copy(toExclusive = 1001)).first().summary).isEqualTo(RecordFilterSummary(0, 0, 0))
    }

    @Test fun negativeOnlyAndZeroOnlyGroups_haveRealMagnitudes() = runTest {
        put(1, type = 30, charge = 200, concessions = 500)
        val negative = repository.observeStatistics(request()).first()
        assertThat(negative.categories.single().amount).isEqualTo(-300)
        assertThat(negative.magnitude).isEqualTo(300)
        put(2, type = 31, charge = 300)
        val zero = repository.observeStatistics(request()).first()
        assertThat(zero.summary.totalCount).isEqualTo(2)
        assertThat(zero.categories.single().amount).isEqualTo(0)
        assertThat(zero.magnitude).isEqualTo(0)
    }

    @Test fun transferDifferenceOverflow_isExplicitFailure() = runTest {
        put(1, type = 30, charge = Long.MAX_VALUE, concessions = -1)
        var caught: Throwable? = null
        try { repository.observeStatistics(request()).first() } catch (error: ArithmeticException) { caught = error }
        assertThat(caught).isInstanceOf(ArithmeticException::class.java)
    }

    @Test fun sqlSumOverflow_isPreservedAsFailure() = runTest {
        put(1, amount = Long.MAX_VALUE)
        put(2, amount = 1)
        var caught: Throwable? = null
        try { repository.observeStatistics(request()).first() } catch (error: Exception) { caught = error }
        assertThat(caught).isNotNull()
        assertThat(generateSequence(caught) { it.cause }.mapNotNull { it.message }.joinToString()).contains("overflow")
    }

    @Test fun liveRecordAndTypeWrites_refreshOneAtomicStatisticsEmission() = runTest {
        put(1, type = 11, amount = 100)
        withContext(Dispatchers.Default) {
            withTimeout(20000) {
                val initial = CompletableDeferred<RecordSearchStatistics>()
                val renamed = CompletableDeferred<RecordSearchStatistics>()
                val moved = CompletableDeferred<RecordSearchStatistics>()
                val inserted = CompletableDeferred<RecordSearchStatistics>()
                val job = launch {
                    repository.observeKeywordStatistics(RecordKeywordQuery(1, "匹配", -1)).collect { stats ->
                        // 每个发射同时核对总汇总与分布，不能只验证最后一次。
                        assertThat(stats.categories.sumOf { it.amount }).isEqualTo(stats.summary.expenditure)
                        if (!initial.isCompleted) initial.complete(stats)
                        if (stats.categories.any { it.typeName == "改名" }) renamed.complete(stats)
                        if (stats.categories.any { it.typeId == 11L }) moved.complete(stats)
                        if (stats.summary.totalCount == 2L) inserted.complete(stats)
                    }
                }
                try {
                    assertThat(initial.await().categories.single().typeId).isEqualTo(10)
                    database.typeDao().insertOrReplace(type(10).copy(name = "改名"))
                    assertThat(renamed.await().summary.totalCount).isEqualTo(1)
                    database.typeDao().updateTypeLevel(11, 999, 1)
                    assertThat(moved.await().categories.single().typeId).isEqualTo(11)
                    put(2, type = 11, amount = 200)
                    assertThat(inserted.await().summary).isEqualTo(RecordFilterSummary(2, 0, 300))
                } finally { job.cancelAndJoin() }
            }
        }
    }

    private fun request() = RecordFilterRequest(1, 0, 100000, emptySet(), CategoryFilterSelection(), emptySet())
    private fun type(id: Long, parent: Long = -1, category: Int = 0) =
        TypeTable(id, parent, "同名", "vector_type_three_meals_24", if (parent == -1L) 0 else 1, category, 0, 0)
    private suspend fun put(id: Long, type: Long = 11, amount: Long = 100, finalAmount: Long = amount, charge: Long = 0, concessions: Long = 0, book: Long = 1, remark: String = "匹配") {
        database.transactionDao().insertRecord(RecordTable(id, type, NO_ASSET_ID, NO_ASSET_ID, book, amount, finalAmount, concessions, charge, remark, 0, 1000 + id))
    }
}
