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

package cn.wj.android.cashbook.feature.records.viewmodel

import cn.wj.android.cashbook.core.testing.repository.FakeRecordRepository
import cn.wj.android.cashbook.core.testing.util.TestDispatcherRule
import cn.wj.android.cashbook.domain.usecase.UpdateRecordReimbursedUseCase
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class RecordDetailsSheetViewModelTest {

    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    private val repository = FakeRecordRepository()
    private val viewModel = RecordDetailsSheetViewModel(
        UpdateRecordReimbursedUseCase(repository, Dispatchers.Unconfined),
    )

    @Test
    fun markReimbursed_true_delegates() = runTest {
        viewModel.markReimbursed(recordId = 5L, reimbursed = true)
        advanceUntilIdle()
        assertThat(repository.lastReimbursedRecordId).isEqualTo(5L)
        assertThat(repository.lastReimbursedValue).isTrue()
    }

    @Test
    fun markReimbursed_false_delegates() = runTest {
        viewModel.markReimbursed(recordId = 9L, reimbursed = false)
        advanceUntilIdle()
        assertThat(repository.lastReimbursedRecordId).isEqualTo(9L)
        assertThat(repository.lastReimbursedValue).isFalse()
    }
}
