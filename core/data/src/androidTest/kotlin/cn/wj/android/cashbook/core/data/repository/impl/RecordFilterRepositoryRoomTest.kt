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
import cn.wj.android.cashbook.core.database.table.TYPE_TABLE_REIMBURSE
import cn.wj.android.cashbook.core.database.table.TagTable
import cn.wj.android.cashbook.core.database.table.TagWithRecordTable
import cn.wj.android.cashbook.core.database.table.TypeTable
import cn.wj.android.cashbook.core.model.entity.DateSelectionEntity
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.model.enums.TypeLevelEnum
import cn.wj.android.cashbook.core.model.model.CategoryFilterSelection
import cn.wj.android.cashbook.core.model.model.RECORD_TYPE_BALANCE_EXPENDITURE
import cn.wj.android.cashbook.core.model.model.RECORD_TYPE_BALANCE_INCOME
import cn.wj.android.cashbook.core.model.model.RecordFilter
import cn.wj.android.cashbook.core.model.model.RecordFilterRequest
import cn.wj.android.cashbook.core.model.model.RecordFilterSummary
import cn.wj.android.cashbook.core.model.model.RecordViewsModel
import cn.wj.android.cashbook.core.model.model.toRequest
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
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/** 从真实仓库、SQL builder、Room 和 Paging 展示器取值，不另写一份 SQL 充当预言机。 */
@RunWith(AndroidJUnit4::class)
class RecordFilterRepositoryRoomTest {
    private lateinit var database: CashbookDatabase
    private lateinit var repository: RecordFilterRepositoryImpl

    @Before
    fun setup(): Unit = kotlinx.coroutines.runBlocking {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), CashbookDatabase::class.java)
            .allowMainThreadQueries().build()
        repository = RecordFilterRepositoryImpl(database.recordFilterDao(), Dispatchers.IO)
        database.typeDao().insertTypes(type(10L), type(11L, 10L), type(12L, 10L), type(20L, category = 1), type(30L, category = 2), type(40L), TYPE_TABLE_REIMBURSE)
        database.tagDao().insert(TagTable(41L, "旅行", -1L, 0))
        database.tagDao().insert(TagTable(42L, "已隐藏", -1L, 1))
    }

    @After
    fun tearDown() { if (::database.isInitialized) database.close() }

    @Test
    fun tagsAny_deduplicatesAndScopesBook() = runTest {
        put(1L, tags = listOf(41L, 42L))
        put(2L, amount = 200L, tags = listOf(42L))
        put(3L, bookId = 2L, tags = listOf(41L))
        val request = RecordFilter(tagIds = setOf(41L, 42L)).toRequest(1L, 1)
        assertThat(records(request).map { it.id }).containsExactly(2L, 1L).inOrder()
        assertThat(repository.observeSummary(request).first()).isEqualTo(RecordFilterSummary(2L, 0L, 300L))
    }

    @Test
    fun dateRange_includesStartAndExcludesEnd() = runTest {
        put(1L, time = 999L)
        put(2L, time = 1000L)
        put(3L, time = 1999L)
        put(4L, time = 2000L)
        val request = RecordFilter().toRequest(1L, 1).copy(fromInclusive = 1000L, toExclusive = 2000L)
        assertThat(records(request).map { it.id }).containsExactly(3L, 2L).inOrder()
        assertThat(repository.observeSummary(request).first().totalCount).isEqualTo(2L)
    }

    @Test
    fun wholeGroup_newChildAndMovedChild_followCurrentTree() = runTest {
        put(1L, typeId = 11L)
        put(2L, typeId = 12L)
        val request = RecordFilter(categories = CategoryFilterSelection(wholeGroupIds = setOf(10L), excludedTypeIds = setOf(11L))).toRequest(1L, 1)
        assertThat(records(request).map { it.id }).containsExactly(2L)
        database.typeDao().insertType(type(13L, 10L))
        put(3L, typeId = 13L)
        assertThat(records(request).map { it.id }).containsExactly(3L, 2L).inOrder()
        database.typeDao().updateTypeLevel(13L, 40L, TypeLevelEnum.SECOND.ordinal)
        assertThat(records(request).map { it.id }).containsExactly(2L)
    }

    @Test
    fun deletedSelectedTag_neverBecomesUnrestricted() = runTest {
        put(1L, tags = listOf(42L))
        val request = RecordFilter(tagIds = setOf(42L)).toRequest(1L, 1)
        assertThat(records(request)).hasSize(1)
        database.tagDao().delete(TagTable(42L, "已隐藏", -1L, 1))
        assertThat(records(request)).isEmpty()
        assertThat(repository.observeSummary(request).first()).isEqualTo(RecordFilterSummary(0L, 0L, 0L))
    }

    @Test
    fun deletedSelections_neverBecomeUnrestricted() = runTest {
        put(1L)
        val request = RecordFilter(categories = CategoryFilterSelection(exactTypeIds = setOf(11L))).toRequest(1L, 1)
        assertThat(records(request)).hasSize(1)
        database.typeDao().deleteById(11L)
        assertThat(records(request)).isEmpty()
        assertThat(repository.observeSummary(request).first()).isEqualTo(RecordFilterSummary(0L, 0L, 0L))
        assertThat(records(RecordFilter().toRequest(1L, 1))).hasSize(1)
    }

    @Test
    fun deletedWholeGroup_doesNotMatchDanglingChildren() = runTest {
        put(1L)
        val request = RecordFilter(categories = CategoryFilterSelection(wholeGroupIds = setOf(10L))).toRequest(1L, 1)
        assertThat(records(request)).hasSize(1)
        database.typeDao().deleteById(10L)
        assertThat(records(request)).isEmpty()
        assertThat(records(RecordFilter().toRequest(1L, 1))).hasSize(1)
    }

    @Test
    fun combinedTypesCategoriesAndTags_doNotBypassBookOrOtherDimensions() = runTest {
        put(1L, tags = listOf(41L))
        put(2L, typeId = 20L, tags = listOf(41L))
        put(3L, typeId = 12L, tags = listOf(42L))
        put(4L, bookId = 2L, tags = listOf(41L))
        put(5L, typeId = 20L, tags = listOf(42L))
        val request = RecordFilter(
            typeCategories = setOf(RecordTypeCategoryEnum.EXPENDITURE, RecordTypeCategoryEnum.INCOME),
            categories = CategoryFilterSelection(exactTypeIds = setOf(20L), wholeGroupIds = setOf(10L), excludedTypeIds = setOf(12L)),
            tagIds = setOf(41L),
        ).toRequest(1L, 1)
        assertThat(records(request).map { it.id }).containsExactly(2L, 1L).inOrder()
        assertThat(repository.observeSummary(request).first()).isEqualTo(RecordFilterSummary(2L, 100L, 100L))
        val expenditureOnly = request.copy(typeCategories = setOf(RecordTypeCategoryEnum.EXPENDITURE))
        assertThat(records(expenditureOnly).map { it.id }).containsExactly(1L)
    }

    @Test
    fun syntheticBalance_countedButNotSummed() = runTest {
        put(1L, typeId = RECORD_TYPE_BALANCE_EXPENDITURE.id, amount = 20000L)
        put(2L, typeId = RECORD_TYPE_BALANCE_INCOME.id, amount = 10000L)
        put(3L, amount = 100L)
        val request = RecordFilter().toRequest(1L, 1)
        assertThat(records(request)).hasSize(3)
        assertThat(repository.observeSummary(request).first()).isEqualTo(RecordFilterSummary(3L, 0L, 100L))
        val income = request.copy(typeCategories = setOf(RecordTypeCategoryEnum.INCOME))
        assertThat(records(income).map { it.id }).containsExactly(2L)
        assertThat(repository.observeSummary(income).first()).isEqualTo(RecordFilterSummary(1L, 0L, 0L))
    }

    @Test
    fun transfer_onlyFeeMinusConcessionsContributes() = runTest {
        put(1L, typeId = 30L, amount = 500000L, charge = 200L)
        put(2L, typeId = 30L, amount = 500000L, charge = 200L, concessions = 300L)
        val request = RecordFilter(typeCategories = setOf(RecordTypeCategoryEnum.TRANSFER)).toRequest(1L, 1)
        assertThat(repository.observeSummary(request).first()).isEqualTo(RecordFilterSummary(2L, 0L, 100L))
    }

    @Test
    fun aggregate_coversAllPagesAndSameTimestampIsStable() = runTest {
        for (id in 1L..61L) put(id, tags = listOf(41L, 42L))
        val request = RecordFilter(tagIds = setOf(41L, 42L)).toRequest(1L, 1)
        assertThat(repository.observeSummary(request).first()).isEqualTo(RecordFilterSummary(61L, 0L, 6100L))
        assertThat(records(request).map { it.id }).containsExactlyElementsIn((61L downTo 1L).toList()).inOrder()
    }

    @Test
    fun wholeGroup_handlesMoreThanNineHundredChildren() = runTest {
        database.typeDao().insertTypes(*(0L..900L).map { type(10000L + it, 10L) }.toTypedArray())
        for (id in 1L..61L) put(id, typeId = 10000L + id)
        put(62L, typeId = 10900L)
        val request = RecordFilter(categories = CategoryFilterSelection(wholeGroupIds = setOf(10L))).toRequest(1L, 1)
        assertThat(records(request)).hasSize(62)
        assertThat(repository.observeSummary(request).first()).isEqualTo(RecordFilterSummary(62L, 0L, 6200L))
    }

    @Test
    fun outsideRangeReimbursement_updatesHistoricNet() = runTest {
        val october = LocalDate.of(2026, 10, 15)
        val november = LocalDate.of(2026, 11, 15)
        database.transactionDao().insertRecordTransaction(table(1L, amount = 100000L, time = DateSelectionEntity.ByDay(october).toDateRange().first), emptyList(), false, emptyList(), emptyList())
        database.transactionDao().insertRecordTransaction(table(2L, typeId = TYPE_TABLE_REIMBURSE.id!!, amount = 80000L, time = DateSelectionEntity.ByDay(november).toDateRange().first), emptyList(), true, listOf(1L), emptyList())
        val request = RecordFilter(dateSelection = DateSelectionEntity.DateRange(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31))).toRequest(1L, 1)
        assertThat(repository.observeSummary(request).first()).isEqualTo(RecordFilterSummary(1L, 0L, 20000L))
        assertThat(records(request).single().finalAmount).isEqualTo(20000L)
    }

    @Test
    fun typeOnlyWrite_refreshesLiveSummary() = runTest {
        put(1L)
        val request = RecordFilter(categories = CategoryFilterSelection(wholeGroupIds = setOf(10L))).toRequest(1L, 1)
        withContext(Dispatchers.Default) {
            withTimeout(20000L) {
                val initial = CompletableDeferred<RecordFilterSummary>()
                val updated = CompletableDeferred<RecordFilterSummary>()
                val job = launch {
                    repository.observeSummary(request).collect { summary ->
                        if (!initial.isCompleted) initial.complete(summary) else if (summary.totalCount == 0L) updated.complete(summary)
                    }
                }
                try {
                    assertThat(initial.await().totalCount).isEqualTo(1L)
                    database.typeDao().updateTypeLevel(11L, 40L, TypeLevelEnum.SECOND.ordinal)
                    assertThat(updated.await()).isEqualTo(RecordFilterSummary(0L, 0L, 0L))
                } finally {
                    job.cancelAndJoin()
                }
            }
        }
    }

    @Test
    fun typeOnlyWrite_invalidatesLivePaging() = runTest {
        put(1L)
        val request = RecordFilter(categories = CategoryFilterSelection(wholeGroupIds = setOf(10L))).toRequest(1L, 1)
        withContext(Dispatchers.Default) {
            withTimeout(20000L) {
                withContext(Dispatchers.Main) {
                    val presenter = presenter()
                    val job = launch { repository.observeRecords(request).collectLatest { presenter.collectFrom(it) } }
                    try {
                        settled(presenter)
                        assertThat(presenter.snapshot().items.map { it.id }).containsExactly(1L)
                        val update = async(start = CoroutineStart.UNDISPATCHED) {
                            presenter.onPagesUpdatedFlow.first { presenter.snapshot().items.isEmpty() }
                        }
                        database.typeDao().updateTypeLevel(11L, 40L, TypeLevelEnum.SECOND.ordinal)
                        update.await()
                        settled(presenter)
                        assertThat(presenter.snapshot().items).isEmpty()
                    } finally {
                        job.cancelAndJoin()
                    }
                }
            }
        }
    }

    private fun presenter() = object : PagingDataPresenter<RecordViewsModel>(Dispatchers.Main) {
        override suspend fun presentPagingDataEvent(event: PagingDataEvent<RecordViewsModel>) = Unit
    }

    private suspend fun settled(presenter: PagingDataPresenter<RecordViewsModel>) {
        val states = presenter.loadStateFlow.filterNotNull().first { it.refresh !is LoadState.Loading && it.append !is LoadState.Loading }
        listOf(states.refresh, states.append).filterIsInstance<LoadState.Error>().firstOrNull()?.let { throw it.error }
    }

    /** 使用项目已有 Paging 公共 API 消费真实仓库；每次等待页事件，禁止无限轮询。 */
    private suspend fun records(request: RecordFilterRequest): List<RecordViewsModel> = withContext(Dispatchers.Default) {
        withTimeout(20000L) {
            withContext(Dispatchers.Main) {
                val presenter = presenter()
                val job = launch { repository.observeRecords(request).collectLatest { presenter.collectFrom(it) } }
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

    private suspend fun put(id: Long, typeId: Long = 11L, bookId: Long = 1L, time: Long = 1000L, amount: Long = 100L, charge: Long = 0L, concessions: Long = 0L, tags: List<Long> = emptyList()) {
        val inserted = database.transactionDao().insertRecord(table(id, typeId, bookId, time, amount, charge, concessions))
        database.transactionDao().insertRelatedTags(tags.map { TagWithRecordTable(null, inserted, it) })
    }

    private fun table(id: Long, typeId: Long = 11L, bookId: Long = 1L, time: Long = 1000L, amount: Long = 100L, charge: Long = 0L, concessions: Long = 0L) =
        RecordTable(id, typeId, NO_ASSET_ID, NO_ASSET_ID, bookId, amount, amount, concessions, charge, "测试$id", 0, time)

    private fun type(id: Long, parentId: Long = -1L, category: Int = 0) =
        TypeTable(id, parentId, "分类$id", "vector_type_three_meals_24", if (parentId == -1L) 0 else 1, category, 0, 0)
}
