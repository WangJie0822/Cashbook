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

package cn.wj.android.cashbook.feature.books.viewmodel

import cn.wj.android.cashbook.core.testing.data.createBooksModel
import cn.wj.android.cashbook.core.testing.repository.FakeBooksRepository
import cn.wj.android.cashbook.core.testing.util.TestDispatcherRule
import cn.wj.android.cashbook.core.ui.DialogState
import cn.wj.android.cashbook.domain.usecase.GetSelectableBooksListUseCase
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class MyBooksViewModelTest {

    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    private lateinit var booksRepository: FakeBooksRepository
    private lateinit var viewModel: MyBooksViewModel

    @Before
    fun setup() {
        booksRepository = FakeBooksRepository()
        viewModel = MyBooksViewModel(
            getSelectableBooksListUseCase = GetSelectableBooksListUseCase(booksRepository),
            booksRepository = booksRepository,
        )
    }

    @Test
    fun when_initial_state_then_loading() {
        assertThat(viewModel.uiState.value).isEqualTo(MyBooksUiState.Loading)
    }

    @Test
    fun when_delete_book_click_then_dialog_shown() {
        viewModel.onDeleteBookClick(1L)

        val state = viewModel.dialogState
        assertThat(state).isInstanceOf(DialogState.Shown::class.java)
        assertThat((state as DialogState.Shown<*>).data).isEqualTo(1L)
    }

    @Test
    fun when_confirm_delete_book_then_dialog_dismissed() = runTest {
        booksRepository.addBook(createBooksModel(id = 1L, name = "测试账本"))

        viewModel.onDeleteBookClick(1L)
        assertThat(viewModel.dialogState).isInstanceOf(DialogState.Shown::class.java)

        viewModel.confirmDeleteBook(1L)

        assertThat(viewModel.dialogState).isEqualTo(DialogState.Dismiss)
    }

    @Test
    fun when_dismiss_dialog_then_state_dismiss() {
        viewModel.onDeleteBookClick(1L)
        assertThat(viewModel.dialogState).isInstanceOf(DialogState.Shown::class.java)

        viewModel.onDismissDialog()

        assertThat(viewModel.dialogState).isEqualTo(DialogState.Dismiss)
    }

    @Test
    fun when_book_selected_then_repository_updated() = runTest {
        val book1 = createBooksModel(id = 1L, name = "账本1")
        val book2 = createBooksModel(id = 2L, name = "账本2")
        booksRepository.addBook(book1)
        booksRepository.addBook(book2)
        booksRepository.setCurrentBook(book1)

        viewModel.onBookSelected(2L)

        // 验证 repository 的当前账本已更新
        assertThat(booksRepository.currentBook.first().id).isEqualTo(2L)
    }
}
