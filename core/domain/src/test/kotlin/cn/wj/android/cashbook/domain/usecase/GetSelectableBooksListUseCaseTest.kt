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

import cn.wj.android.cashbook.core.testing.data.createBooksModel
import cn.wj.android.cashbook.core.testing.repository.FakeBooksRepository
import cn.wj.android.cashbook.core.testing.util.TestDispatcherRule
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class GetSelectableBooksListUseCaseTest {

    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    private lateinit var booksRepository: FakeBooksRepository
    private lateinit var useCase: GetSelectableBooksListUseCase

    @Before
    fun setup() {
        booksRepository = FakeBooksRepository()
        useCase = GetSelectableBooksListUseCase(
            booksRepository = booksRepository,
        )
    }

    @Test
    fun when_current_book_matches_then_marked_as_selected() = runTest {
        val book1 = createBooksModel(id = 1L, name = "账本1")
        val book2 = createBooksModel(id = 2L, name = "账本2")
        booksRepository.addBook(book1)
        booksRepository.addBook(book2)
        booksRepository.setCurrentBook(book1)

        val result = useCase().first()

        assertThat(result).hasSize(2)
        assertThat(result.first { it.data.id == 1L }.selected).isTrue()
        assertThat(result.first { it.data.id == 2L }.selected).isFalse()
    }

    @Test
    fun when_switch_current_book_then_selection_updates() = runTest {
        val book1 = createBooksModel(id = 1L, name = "账本1")
        val book2 = createBooksModel(id = 2L, name = "账本2")
        booksRepository.addBook(book1)
        booksRepository.addBook(book2)
        booksRepository.setCurrentBook(book2)

        val result = useCase().first()

        assertThat(result.first { it.data.id == 2L }.selected).isTrue()
        assertThat(result.first { it.data.id == 1L }.selected).isFalse()
    }
}
