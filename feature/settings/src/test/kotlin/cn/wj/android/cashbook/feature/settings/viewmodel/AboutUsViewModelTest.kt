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

import cn.wj.android.cashbook.core.model.enums.LogcatState
import cn.wj.android.cashbook.core.testing.repository.FakeSettingRepository
import cn.wj.android.cashbook.core.testing.util.TestDispatcherRule
import cn.wj.android.cashbook.core.ui.DialogState
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class AboutUsViewModelTest {

    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    private lateinit var settingRepository: FakeSettingRepository
    private lateinit var viewModel: AboutUsViewModel

    @Before
    fun setup() {
        settingRepository = FakeSettingRepository()
        viewModel = AboutUsViewModel(settingRepository)
    }

    @Test
    fun when_initial_state_then_loading() {
        assertThat(viewModel.uiState.value).isEqualTo(AboutUsUiState.Loading)
    }

    @Test
    fun when_settings_loaded_then_state_is_success() = runTest {
        val collectJob = launch(UnconfinedTestDispatcher()) {
            viewModel.uiState.collect()
        }

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(AboutUsUiState.Success::class.java)

        collectJob.cancel()
    }

    @Test
    fun when_settings_use_github_false_then_use_gitee_true() = runTest {
        // 默认 useGithub = false，所以 useGitee = true
        val collectJob = launch(UnconfinedTestDispatcher()) {
            viewModel.uiState.collect()
        }

        val state = viewModel.uiState.value as AboutUsUiState.Success
        assertThat(state.useGitee).isTrue()

        collectJob.cancel()
    }

    @Test
    fun when_update_use_gitee_true_then_use_github_false() = runTest {
        viewModel.updateUseGitee(true)

        val settings = settingRepository.appSettingsModel.first()
        assertThat(settings.useGithub).isFalse()
    }

    @Test
    fun when_update_use_gitee_false_then_use_github_true() = runTest {
        viewModel.updateUseGitee(false)

        val settings = settingRepository.appSettingsModel.first()
        assertThat(settings.useGithub).isTrue()
    }

    @Test
    fun when_update_canary_then_setting_updated() = runTest {
        viewModel.updateCanary(true)

        val settings = settingRepository.appSettingsModel.first()
        assertThat(settings.canary).isTrue()
    }

    @Test
    fun when_update_auto_check_update_then_setting_updated() = runTest {
        viewModel.updateAutoCheckUpdate(true)

        val settings = settingRepository.appSettingsModel.first()
        assertThat(settings.autoCheckUpdate).isTrue()
    }

    @Test
    fun when_update_logcat_state_always_then_logcat_in_release_true() = runTest {
        viewModel.updateLogcatState(LogcatState.ALWAYS)

        val settings = settingRepository.appSettingsModel.first()
        assertThat(settings.logcatInRelease).isTrue()
    }

    @Test
    fun when_update_logcat_state_none_then_logcat_in_release_false() = runTest {
        // 先设置为 ALWAYS
        settingRepository.updateLogcatInRelease(true)

        viewModel.updateLogcatState(LogcatState.NONE)

        val settings = settingRepository.appSettingsModel.first()
        assertThat(settings.logcatInRelease).isFalse()
    }

    @Test
    fun when_update_logcat_state_once_then_logcat_in_release_false() = runTest {
        viewModel.updateLogcatState(LogcatState.ONCE)

        val settings = settingRepository.appSettingsModel.first()
        assertThat(settings.logcatInRelease).isFalse()
    }

    @Test
    fun when_update_logcat_state_then_dialog_dismissed() = runTest {
        viewModel.logcatDialogState = DialogState.Shown(LogcatState.NONE)

        viewModel.updateLogcatState(LogcatState.NONE)

        assertThat(viewModel.logcatDialogState).isEqualTo(DialogState.Dismiss)
    }

    @Test
    fun when_dismiss_dialog_then_state_is_dismiss() {
        viewModel.logcatDialogState = DialogState.Shown(LogcatState.NONE)

        viewModel.dismissDialog()

        assertThat(viewModel.logcatDialogState).isEqualTo(DialogState.Dismiss)
    }

    @Test
    fun when_count_clicks_less_than_10_then_no_dialog() {
        // 点击 9 次不应显示弹窗
        repeat(9) {
            viewModel.countNameClicks()
        }

        assertThat(viewModel.logcatDialogState).isEqualTo(DialogState.Dismiss)
    }

    @Test
    fun when_count_clicks_reaches_10_then_dialog_shown() = runTest {
        // 点击 10 次应显示弹窗
        repeat(10) {
            viewModel.countNameClicks()
        }

        assertThat(viewModel.logcatDialogState).isInstanceOf(DialogState.Shown::class.java)
    }

    @Test
    fun when_ui_state_reflects_canary_setting() = runTest {
        settingRepository.updateCanary(true)

        val collectJob = launch(UnconfinedTestDispatcher()) {
            viewModel.uiState.collect()
        }

        val state = viewModel.uiState.value as AboutUsUiState.Success
        assertThat(state.canary).isTrue()

        collectJob.cancel()
    }

    @Test
    fun when_ui_state_reflects_auto_check_update_setting() = runTest {
        settingRepository.updateAutoCheckUpdate(true)

        val collectJob = launch(UnconfinedTestDispatcher()) {
            viewModel.uiState.collect()
        }

        val state = viewModel.uiState.value as AboutUsUiState.Success
        assertThat(state.autoCheckUpdate).isTrue()

        collectJob.cancel()
    }
}
