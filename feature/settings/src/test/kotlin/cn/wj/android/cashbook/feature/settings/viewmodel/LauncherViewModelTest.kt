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

package cn.wj.android.cashbook.feature.settings.viewmodel

import cn.wj.android.cashbook.core.testing.data.createBooksModel
import cn.wj.android.cashbook.core.testing.repository.FakeBooksRepository
import cn.wj.android.cashbook.core.testing.repository.FakeSettingRepository
import cn.wj.android.cashbook.core.testing.util.TestDispatcherRule
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class LauncherViewModelTest {

    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    private lateinit var settingRepository: FakeSettingRepository
    private lateinit var booksRepository: FakeBooksRepository
    private lateinit var viewModel: LauncherViewModel

    @Before
    fun setup() {
        settingRepository = FakeSettingRepository()
        booksRepository = FakeBooksRepository()
        viewModel = LauncherViewModel(
            settingRepository = settingRepository,
            booksRepository = booksRepository,
        )
    }

    @Test
    fun when_initial_state_then_ui_state_is_loading() {
        // 未收集 Flow 时，初始值为 Loading
        assertThat(viewModel.uiState.value).isEqualTo(LauncherUiState.Loading)
    }

    @Test
    fun when_current_book_emitted_then_ui_state_is_success() = runTest {
        val collectJob = launch(UnconfinedTestDispatcher()) {
            viewModel.uiState.collect()
        }

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(LauncherUiState.Success::class.java)
        // FakeBooksRepository 默认当前账本名称为 "默认账本"
        assertThat((state as LauncherUiState.Success).currentBookName).isEqualTo("默认账本")

        collectJob.cancel()
    }

    @Test
    fun when_current_book_changed_then_ui_state_updated() = runTest {
        val collectJob = launch(UnconfinedTestDispatcher()) {
            viewModel.uiState.collect()
        }

        // 切换当前账本
        val newBook = createBooksModel(id = 2L, name = "旅行账本")
        booksRepository.setCurrentBook(newBook)

        val state = viewModel.uiState.value as LauncherUiState.Success
        assertThat(state.currentBookName).isEqualTo("旅行账本")

        collectJob.cancel()
    }

    @Test
    fun when_initial_state_then_drawer_not_displayed() {
        assertThat(viewModel.shouldDisplayDrawerSheet).isFalse()
    }

    @Test
    fun when_display_drawer_then_state_is_true() {
        viewModel.displayDrawerSheet()

        assertThat(viewModel.shouldDisplayDrawerSheet).isTrue()
    }

    @Test
    fun when_dismiss_drawer_then_state_is_false() {
        // 先显示抽屉
        viewModel.displayDrawerSheet()
        assertThat(viewModel.shouldDisplayDrawerSheet).isTrue()

        // 隐藏抽屉
        viewModel.dismissDrawerSheet()

        assertThat(viewModel.shouldDisplayDrawerSheet).isFalse()
    }

    @Test
    fun when_display_and_dismiss_drawer_multiple_times_then_state_correct() {
        viewModel.displayDrawerSheet()
        assertThat(viewModel.shouldDisplayDrawerSheet).isTrue()

        viewModel.dismissDrawerSheet()
        assertThat(viewModel.shouldDisplayDrawerSheet).isFalse()

        viewModel.displayDrawerSheet()
        assertThat(viewModel.shouldDisplayDrawerSheet).isTrue()
    }
}
