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

import androidx.test.core.app.ApplicationProvider
import cn.wj.android.cashbook.core.common.manager.DocumentOperationManager
import cn.wj.android.cashbook.core.testing.data.createBooksModel
import cn.wj.android.cashbook.core.testing.repository.FakeBooksRepository
import cn.wj.android.cashbook.core.testing.util.TestDispatcherRule
import cn.wj.android.cashbook.feature.books.enums.EditBookBookmarkEnum
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class EditBookViewModelTest {

    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    private lateinit var booksRepository: FakeBooksRepository
    private lateinit var viewModel: EditBookViewModel

    @Before
    fun setup() {
        booksRepository = FakeBooksRepository()
        val dom = DocumentOperationManager(
            context = ApplicationProvider.getApplicationContext(),
            ioCoroutineContext = UnconfinedTestDispatcher(),
        )
        viewModel = EditBookViewModel(
            booksRepository = booksRepository,
            dom = dom,
        )
    }

    @Test
    fun when_update_book_id_then_ui_state_updates() = runTest {
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        val book = createBooksModel(id = 1L, name = "测试账本", description = "描述")
        booksRepository.addBook(book)

        viewModel.updateBookId(1L)

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(EditBookUiState.Success::class.java)
        val successState = state as EditBookUiState.Success
        assertThat(successState.data.id).isEqualTo(1L)
        assertThat(successState.data.name).isEqualTo("测试账本")
        assertThat(successState.data.description).isEqualTo("描述")

        job.cancel()
    }

    @Test
    fun when_save_with_duplicated_name_then_bookmark_name_duplicated() = runTest {
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        val book1 = createBooksModel(id = 1L, name = "已有账本")
        val book2 = createBooksModel(id = 2L, name = "待编辑账本")
        booksRepository.addBook(book1)
        booksRepository.addBook(book2)

        viewModel.updateBookId(2L)

        var successCalled = false
        viewModel.onSaveClick(
            name = "已有账本",
            description = "",
            bgUri = null,
            onSuccess = { successCalled = true },
        )

        assertThat(viewModel.shouldDisplayBookmark).isEqualTo(EditBookBookmarkEnum.NAME_DUPLICATED)
        assertThat(successCalled).isFalse()

        job.cancel()
    }

    @Test
    fun when_dismiss_bookmark_then_bookmark_none() {
        viewModel.onDismissBookmark()

        assertThat(viewModel.shouldDisplayBookmark).isEqualTo(EditBookBookmarkEnum.NONE)
    }
}
