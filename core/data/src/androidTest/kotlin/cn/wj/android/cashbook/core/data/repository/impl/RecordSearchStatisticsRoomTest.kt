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

import androidx.paging.LoadState
import androidx.paging.PagingDataEvent
import androidx.paging.PagingDataPresenter
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
import cn.wj.android.cashbook.core.model.model.RecordViewsModel
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
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

    @Test fun combinedPagingAndStatistics_matchOldDaoIntersectionAcrossAllPages() = runTest {
        withContext(Dispatchers.Default) {
            database.tagDao().insert(TagTable(1, "偶数", -1, 0))
            database.tagDao().insert(TagTable(2, "三的倍数", -1, 1))
            for (id in 1L..61L) {
                database.typeDao().insertType(type(10000 + id))
                put(id, type = 10000 + id, amount = id, remark = "组合匹配")
                val tags = buildList {
                    if (id % 2 == 0L) add(1L)
                    if (id % 3 == 0L) add(2L)
                }
                attachTags(id, tags)
            }
            put(62, book = 2, amount = 99999, remark = "组合匹配")
            attachTags(62, listOf(1, 2))
            put(63, amount = 99999, remark = "组合匹配") // 有关键词、无选中标签。
            put(64, amount = 99999, remark = "其他内容") // 有标签、无关键词。
            attachTags(64, listOf(1))
            val filter = request().copy(tagIds = setOf(1, 2))
            val keyword = RecordKeywordQuery(1, "组合匹配", -1)
            val oldKeywordIds = oldKeywordIds(keyword)
            val oldFilterIds = records(filter).map { it.id }.toSet()
            assertThat(oldKeywordIds).hasSize(62)
            assertThat(oldFilterIds).hasSize(41)
            val expectedIds = listOf(
                2L, 3L, 4L, 6L, 8L, 9L, 10L, 12L, 14L, 15L,
                16L, 18L, 20L, 21L, 22L, 24L, 26L, 27L, 28L, 30L,
                32L, 33L, 34L, 36L, 38L, 39L, 40L, 42L, 44L, 45L,
                46L, 48L, 50L, 51L, 52L, 54L, 56L, 57L, 58L, 60L,
            )
            val combined = records(filter, keyword)
            assertThat(combined.map { it.id }).containsExactlyElementsIn(oldKeywordIds intersect oldFilterIds)
            assertThat(combined.map { it.id }).containsExactlyElementsIn(expectedIds.reversed()).inOrder()
            assertThat(combined).hasSize(40)
            val stats = withTimeout(20000) { repository.observeStatistics(filter, keyword).first() }
            assertThat(stats.summary).isEqualTo(RecordFilterSummary(40, 0, 1230))
            // 每笔有独立一级分类：列表漏页不能靠分类合并或相同总额掩盖。
            assertThat(stats.categories.map { it.typeId - 10000 }).containsExactlyElementsIn(expectedIds)
            assertThat(stats.categories.associate { it.typeId - 10000 to it.amount })
                .containsExactlyEntriesIn(expectedIds.associateWith { it })
            assertThat(stats.magnitude).isEqualTo(1230)
        }
    }

    @Test fun combinedPredicates_keepParentExclusionsNetAmountsTagsAndDateBoundaries() = runTest {
        withContext(Dispatchers.Default) {
            database.tagDao().insert(TagTable(1, "公开", -1, 0))
            database.tagDao().insert(TagTable(2, "隐藏", -1, 1))
            put(100, type = 11, amount = 99999, remark = "组合", time = 1000) // 起点之前。
            put(1, type = 11, amount = 100, finalAmount = 80, remark = "组合") // 起点包含。
            put(2, type = 12, amount = 99999, remark = "组合") // 显式排除的子类。
            put(3, type = 20, amount = 300, finalAmount = 250, remark = "组合")
            put(4, type = 31, amount = 500000, charge = 100, concessions = 400, remark = "组合")
            put(5, type = 32, amount = 500000, charge = 50, concessions = 250, remark = "组合")
            put(6, type = 11, amount = 99999, remark = "组合") // 终点不包含。
            put(7, type = 11, amount = 99999, book = 2, remark = "组合", time = 1002)
            put(8, type = 11, amount = 99999, remark = "其他内容", time = 1002)
            put(9, type = 11, amount = 99999, remark = "组合", time = 1002) // 无标签。
            put(10, type = 40, amount = 99999, remark = "组合", time = 1002) // 不在分类集合。
            for (id in listOf(100L, 1L, 2L, 3L, 5L, 6L, 7L, 8L, 10L)) attachTags(id, listOf(1, 2))
            attachTags(4, listOf(2))
            val filter = request().copy(
                fromInclusive = 1001,
                toExclusive = 1006,
                typeCategories = setOf(
                    RecordTypeCategoryEnum.EXPENDITURE,
                    RecordTypeCategoryEnum.INCOME,
                    RecordTypeCategoryEnum.TRANSFER,
                ),
                categories = CategoryFilterSelection(
                    wholeGroupIds = setOf(10, 30),
                    exactTypeIds = setOf(20),
                    excludedTypeIds = setOf(12),
                ),
                tagIds = setOf(1, 2),
            )
            val keyword = RecordKeywordQuery(1, "组合", -1)
            val combinedIds = records(filter, keyword).map { it.id }
            assertThat(combinedIds).containsExactly(5L, 4L, 3L, 1L).inOrder()
            assertThat(combinedIds).containsExactlyElementsIn(oldKeywordIds(keyword) intersect records(filter).map { it.id }.toSet())
            val stats = withTimeout(20000) { repository.observeStatistics(filter, keyword).first() }
            assertThat(stats.summary).isEqualTo(RecordFilterSummary(4, 250, -420))
            assertThat(stats.categories.associate { it.typeId to it.amount }).containsExactly(10L, 80L, 20L, 250L, 30L, -500L)
            assertThat(stats.magnitude).isEqualTo(830)
            val incomeOnly = filter.copy(typeCategories = setOf(RecordTypeCategoryEnum.INCOME))
            assertThat(records(incomeOnly, keyword).map { it.id }).containsExactly(3L)
            assertThat(withTimeout(20000) { repository.observeStatistics(incomeOnly, keyword).first() }.summary)
                .isEqualTo(RecordFilterSummary(1, 250, 0))
            assertThat(records(filter.copy(fromInclusive = 1002), keyword).map { it.id })
                .containsExactly(5L, 4L, 3L).inOrder()
            assertThat(records(filter.copy(toExclusive = 1005), keyword).map { it.id })
                .containsExactly(4L, 3L, 1L).inOrder()
        }
    }

    @Test fun combinedLiveTagDeletionAndRecordInsertion_refreshPagingAndStatistics() = runTest {
        val deletedTag = TagTable(1, "待删除", -1, 0)
        database.tagDao().insert(deletedTag)
        database.tagDao().insert(TagTable(2, "隐藏但仍有效", -1, 1))
        put(1, amount = 100, remark = "组合")
        attachTags(1, listOf(1))
        put(3, amount = 99999, remark = "其他内容")
        attachTags(3, listOf(2))
        val filter = request().copy(tagIds = setOf(1, 2))
        val keyword = RecordKeywordQuery(1, "组合", -1)
        withContext(Dispatchers.Default) {
            withTimeout(20000) {
                withContext(Dispatchers.Main) {
                    val initial = CompletableDeferred<RecordSearchStatistics>()
                    val deleted = CompletableDeferred<RecordSearchStatistics>()
                    val inserted = CompletableDeferred<RecordSearchStatistics>()
                    val presenter = presenter()
                    val pagingJob = launch {
                        repository.observeRecords(filter, keyword).collectLatest { presenter.collectFrom(it) }
                    }
                    val statisticsJob = launch {
                        repository.observeStatistics(filter, keyword).collect { stats ->
                            assertThat(stats.categories.sumOf { it.amount }).isEqualTo(stats.summary.expenditure)
                            if (!initial.isCompleted) initial.complete(stats)
                            if (stats.summary.totalCount == 0L) deleted.complete(stats)
                            if (stats.summary == RecordFilterSummary(1, 0, 200)) inserted.complete(stats)
                        }
                    }
                    try {
                        settled(presenter)
                        assertThat(presenter.snapshot().items.map { it.id }).containsExactly(1L)
                        assertThat(initial.await().summary).isEqualTo(RecordFilterSummary(1, 0, 100))
                        val removedPage = async(start = CoroutineStart.UNDISPATCHED) {
                            presenter.onPagesUpdatedFlow.first { presenter.snapshot().items.isEmpty() }
                        }
                        database.tagDao().delete(deletedTag)
                        removedPage.await()
                        settled(presenter)
                        assertThat(presenter.snapshot().items).isEmpty()
                        assertThat(deleted.await().summary).isEqualTo(RecordFilterSummary(0, 0, 0))
                        val insertedPage = async(start = CoroutineStart.UNDISPATCHED) {
                            presenter.onPagesUpdatedFlow.first { presenter.snapshot().items.map { it.id } == listOf(2L) }
                        }
                        put(2, amount = 200, remark = "组合")
                        attachTags(2, listOf(2))
                        insertedPage.await()
                        settled(presenter)
                        assertThat(presenter.snapshot().items.map { it.id }).containsExactly(2L)
                        val refreshed = inserted.await()
                        assertThat(refreshed.summary).isEqualTo(RecordFilterSummary(1, 0, 200))
                        assertThat(refreshed.categories.associate { it.typeId to it.amount }).containsExactly(10L, 200L)
                    } finally {
                        pagingJob.cancelAndJoin()
                        statisticsJob.cancelAndJoin()
                    }
                }
            }
        }
    }

    /** 分页读取旧 DAO，避免把只读首屏的集合当作完整关键词集合。 */
    private suspend fun oldKeywordIds(query: RecordKeywordQuery): Set<Long> = withTimeout(20000) {
        val ids = mutableSetOf<Long>()
        var offset = 0
        while (true) {
            check(offset < 2000) { "Old keyword DAO did not finish within 100 pages" }
            val page = database.recordDao().queryRecordByKeyword(query.bookId, query.keyword, query.amountCent, offset, 20)
            ids += page.map { it.id!! }
            if (page.size < 20) break
            offset += 20
        }
        ids
    }

    private fun presenter() = object : PagingDataPresenter<RecordViewsModel>(Dispatchers.Main) {
        override suspend fun presentPagingDataEvent(event: PagingDataEvent<RecordViewsModel>) = Unit
    }

    private suspend fun settled(presenter: PagingDataPresenter<RecordViewsModel>) {
        val states = presenter.loadStateFlow.filterNotNull().first {
            it.refresh !is LoadState.Loading && it.append !is LoadState.Loading
        }
        listOf(states.refresh, states.append).filterIsInstance<LoadState.Error>().firstOrNull()?.let { throw it.error }
    }

    /** 沿用真实 Room 测试的公共 Paging 展示器；事件等待和最大页数同时有界。 */
    private suspend fun records(request: RecordFilterRequest, keyword: RecordKeywordQuery? = null): List<RecordViewsModel> =
        withContext(Dispatchers.Default) {
            withTimeout(20000) {
                withContext(Dispatchers.Main) {
                    val presenter = presenter()
                    val job = launch {
                        val paging = if (keyword == null) repository.observeRecords(request) else repository.observeRecords(request, keyword)
                        paging.collectLatest { presenter.collectFrom(it) }
                    }
                    try {
                        settled(presenter)
                        var loads = 0
                        while ((presenter.loadStateFlow.value?.append as? LoadState.NotLoading)?.endOfPaginationReached != true) {
                            check(++loads <= 100) { "Paging did not finish within 100 page events" }
                            val update = async(start = CoroutineStart.UNDISPATCHED) { presenter.onPagesUpdatedFlow.first() }
                            presenter[presenter.size - 1]
                            update.await()
                            settled(presenter)
                        }
                        presenter.snapshot().items
                    } finally {
                        job.cancelAndJoin()
                    }
                }
            }
        }

    private suspend fun attachTags(recordId: Long, tagIds: List<Long>) {
        database.transactionDao().insertRelatedTags(tagIds.map { TagWithRecordTable(null, recordId, it) })
    }

    private fun request() = RecordFilterRequest(1, 0, 100000, emptySet(), CategoryFilterSelection(), emptySet())
    private fun type(id: Long, parent: Long = -1, category: Int = 0) =
        TypeTable(id, parent, "同名", "vector_type_three_meals_24", if (parent == -1L) 0 else 1, category, 0, 0)
    private suspend fun put(id: Long, type: Long = 11, amount: Long = 100, finalAmount: Long = amount, charge: Long = 0, concessions: Long = 0, book: Long = 1, remark: String = "匹配", time: Long = 1000 + id) {
        database.transactionDao().insertRecord(RecordTable(id, type, NO_ASSET_ID, NO_ASSET_ID, book, amount, finalAmount, concessions, charge, remark, 0, time))
    }
}
