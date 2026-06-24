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

import cn.wj.android.cashbook.core.testing.repository.FakeRecordRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Test

class UpdateRecordReimbursedUseCaseTest {

    private val repository = FakeRecordRepository()
    private val useCase = UpdateRecordReimbursedUseCase(repository, Dispatchers.Unconfined)

    @Test
    fun invoke_true_delegates_to_repository() = runTest {
        useCase(recordId = 7L, reimbursed = true)
        assertThat(repository.lastReimbursedRecordId).isEqualTo(7L)
        assertThat(repository.lastReimbursedValue).isTrue()
    }

    @Test
    fun invoke_false_delegates_to_repository() = runTest {
        useCase(recordId = 9L, reimbursed = false)
        assertThat(repository.lastReimbursedRecordId).isEqualTo(9L)
        assertThat(repository.lastReimbursedValue).isFalse()
    }
}
