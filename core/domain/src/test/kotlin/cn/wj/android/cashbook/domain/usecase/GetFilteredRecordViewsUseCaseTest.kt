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

package cn.wj.android.cashbook.domain.usecase

import androidx.paging.PagingDataEvent
import androidx.paging.PagingDataPresenter
import cn.wj.android.cashbook.core.model.entity.RecordViewsEntity
import cn.wj.android.cashbook.core.model.model.CategoryFilterSelection
import cn.wj.android.cashbook.core.model.model.RecordFilter
import cn.wj.android.cashbook.core.model.model.toRequest
import cn.wj.android.cashbook.core.testing.data.createRecordTypeModel
import cn.wj.android.cashbook.core.testing.data.createRecordViewsModel
import cn.wj.android.cashbook.core.testing.repository.FakeRecordFilterRepository
import cn.wj.android.cashbook.core.testing.util.TestDispatcherRule
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GetFilteredRecordViewsUseCaseTest {
    @get:Rule val dispatcherRule = TestDispatcherRule()

    @Test
    fun mapsFilteredModelsToEntitiesWithoutLosingNetAmount() = runTest {
        val type = createRecordTypeModel(id = 11L)
        val repository = FakeRecordFilterRepository(flowOf(listOf(type)), flowOf(emptyList())).apply {
            setRecords(listOf(createRecordViewsModel(id = 7L, type = type, amount = 100000L, finalAmount = 20000L)))
        }
        val request = RecordFilter(categories = CategoryFilterSelection(exactTypeIds = setOf(11L))).toRequest(1L, 1)
        val result = CompletableDeferred<List<RecordViewsEntity>>()
        val presenter = object : PagingDataPresenter<RecordViewsEntity>(dispatcherRule.testDispatcher) {
            override suspend fun presentPagingDataEvent(event: PagingDataEvent<RecordViewsEntity>) = Unit
        }
        presenter.addOnPagesUpdatedListener { result.complete(presenter.snapshot().items) }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            GetFilteredRecordViewsUseCase(repository)(request).collectLatest { presenter.collectFrom(it) }
        }
        val items = result.await()
        assertThat(items).hasSize(1)
        assertThat(items.single().id).isEqualTo(7L)
        assertThat(items.single().finalAmount).isEqualTo(20000L)
        assertThat(items.single().amount).isEqualTo(100000L)
        assertThat(repository.recordRequests.last()).isEqualTo(request)
    }
}
