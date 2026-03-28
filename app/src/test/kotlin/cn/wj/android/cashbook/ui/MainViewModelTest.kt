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

package cn.wj.android.cashbook.ui

import cn.wj.android.cashbook.core.model.enums.DarkModeEnum
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

class MainViewModelTest {

    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    private lateinit var settingRepository: FakeSettingRepository
    private lateinit var viewModel: MainViewModel

    @Before
    fun setup() {
        settingRepository = FakeSettingRepository()
        viewModel = MainViewModel(settingRepository = settingRepository)
    }

    @Test
    fun when_initial_then_ui_state_loading() {
        // 未收集 Flow 时，初始值为 Loading
        assertThat(viewModel.uiState.value).isEqualTo(ActivityUiState.Loading)
    }

    @Test
    fun when_settings_emitted_then_ui_state_success() = runTest {
        val collectJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        // FakeSettingRepository 默认 darkMode = FOLLOW_SYSTEM, dynamicColor = false
        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(ActivityUiState.Success::class.java)
        assertThat((state as ActivityUiState.Success).darkMode).isEqualTo(DarkModeEnum.FOLLOW_SYSTEM)
        assertThat(state.dynamicColor).isFalse()

        collectJob.cancel()
    }

    @Test
    fun when_dark_mode_changes_then_ui_state_reflects() = runTest {
        val collectJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        // 切换为深色模式
        settingRepository.updateDarkMode(DarkModeEnum.DARK)

        val state = viewModel.uiState.value as ActivityUiState.Success
        assertThat(state.darkMode).isEqualTo(DarkModeEnum.DARK)

        collectJob.cancel()
    }

    @Test
    fun when_dynamic_color_changes_then_ui_state_reflects() = runTest {
        val collectJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        // 开启动态颜色
        settingRepository.updateDynamicColor(true)

        val state = viewModel.uiState.value as ActivityUiState.Success
        assertThat(state.dynamicColor).isTrue()

        collectJob.cancel()
    }
}
