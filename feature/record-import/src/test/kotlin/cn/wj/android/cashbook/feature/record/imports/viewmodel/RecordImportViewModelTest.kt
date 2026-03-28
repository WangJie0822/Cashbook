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

package cn.wj.android.cashbook.feature.record.imports.viewmodel

import androidx.lifecycle.SavedStateHandle
import cn.wj.android.cashbook.core.testing.repository.FakeAssetRepository
import cn.wj.android.cashbook.core.testing.repository.FakeBooksRepository
import cn.wj.android.cashbook.core.testing.repository.FakeRecordRepository
import cn.wj.android.cashbook.core.testing.repository.FakeTypeRepository
import cn.wj.android.cashbook.core.testing.util.TestDispatcherRule
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class RecordImportViewModelTest {

    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    private fun createViewModel(filePath: String = ""): RecordImportViewModel {
        return RecordImportViewModel(
            savedStateHandle = SavedStateHandle(mapOf("fileUri" to filePath)),
            recordRepository = FakeRecordRepository(),
            typeRepository = FakeTypeRepository(),
            assetRepository = FakeAssetRepository(),
            booksRepository = FakeBooksRepository(),
            coroutineContext = UnconfinedTestDispatcher(),
        )
    }

    @Test
    fun when_file_not_found_then_ui_state_error() = runTest {
        // 给定一个不存在的文件路径，解析后应进入 Error 状态
        val viewModel = createViewModel(filePath = "/nonexistent/path/bill.csv")
        advanceUntilIdle()
        assertThat(viewModel.uiState.value).isInstanceOf(RecordImportUiState.Error::class.java)
    }

    @Test
    fun when_empty_file_path_then_ui_state_error() = runTest {
        // 空路径对应的 File 不存在，解析后应进入 Error 状态
        val viewModel = createViewModel(filePath = "")
        advanceUntilIdle()
        assertThat(viewModel.uiState.value).isInstanceOf(RecordImportUiState.Error::class.java)
    }

    @Test
    fun when_toggle_on_non_ready_state_then_no_crash() = runTest {
        // 在 Error 状态下调用 toggleItemSelection 应安全退出不崩溃
        val viewModel = createViewModel(filePath = "/nonexistent/path/bill.csv")
        advanceUntilIdle()
        assertThat(viewModel.uiState.value).isInstanceOf(RecordImportUiState.Error::class.java)

        // 调用不应抛出异常
        viewModel.toggleItemSelection(0)

        // 状态保持不变
        assertThat(viewModel.uiState.value).isInstanceOf(RecordImportUiState.Error::class.java)
    }

    @Test
    fun when_select_all_on_non_ready_state_then_no_crash() = runTest {
        // 在 Error 状态下调用 selectAll 应安全退出不崩溃
        val viewModel = createViewModel(filePath = "/nonexistent/path/bill.csv")
        advanceUntilIdle()
        assertThat(viewModel.uiState.value).isInstanceOf(RecordImportUiState.Error::class.java)

        // 调用不应抛出异常
        viewModel.selectAll(true)

        // 状态保持不变
        assertThat(viewModel.uiState.value).isInstanceOf(RecordImportUiState.Error::class.java)
    }

    @Test
    fun when_confirm_import_on_non_ready_state_then_no_crash() = runTest {
        // 在 Error 状态下调用 confirmImport 应安全退出不崩溃
        val viewModel = createViewModel(filePath = "/nonexistent/path/bill.csv")
        advanceUntilIdle()
        assertThat(viewModel.uiState.value).isInstanceOf(RecordImportUiState.Error::class.java)

        // 调用不应抛出异常
        viewModel.confirmImport()

        // 状态保持不变
        assertThat(viewModel.uiState.value).isInstanceOf(RecordImportUiState.Error::class.java)
    }
}
