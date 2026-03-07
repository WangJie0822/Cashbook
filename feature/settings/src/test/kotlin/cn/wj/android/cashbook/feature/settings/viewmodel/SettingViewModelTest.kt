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

import cn.wj.android.cashbook.core.model.enums.DarkModeEnum
import cn.wj.android.cashbook.core.model.enums.ImageQualityEnum
import cn.wj.android.cashbook.core.model.enums.VerificationModeEnum
import cn.wj.android.cashbook.core.testing.repository.FakeSettingRepository
import cn.wj.android.cashbook.core.testing.util.TestDispatcherRule
import cn.wj.android.cashbook.core.ui.DialogState
import cn.wj.android.cashbook.feature.settings.enums.SettingDialogEnum
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class SettingViewModelTest {

    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    private lateinit var settingRepository: FakeSettingRepository
    private lateinit var viewModel: SettingViewModel

    @Before
    fun setup() {
        settingRepository = FakeSettingRepository()
        viewModel = SettingViewModel(settingRepository)
    }

    @Test
    fun when_initial_state_then_loading() {
        assertThat(viewModel.uiState.value).isEqualTo(SettingUiState.Loading)
    }

    @Test
    fun when_settings_loaded_then_state_is_success() = runTest {
        val collectJob = launch(UnconfinedTestDispatcher()) {
            viewModel.uiState.collect()
        }

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(SettingUiState.Success::class.java)

        collectJob.cancel()
    }

    @Test
    fun when_mobile_network_download_changed_then_setting_updated() = runTest {
        val collectJob = launch(UnconfinedTestDispatcher()) {
            viewModel.uiState.collect()
        }

        viewModel.onMobileNetworkDownloadEnableChanged(true)

        val settings = settingRepository.appSettingsModel.first()
        assertThat(settings.mobileNetworkDownloadEnable).isTrue()
        val state = viewModel.uiState.value as SettingUiState.Success
        assertThat(state.mobileNetworkDownloadEnable).isTrue()

        collectJob.cancel()
    }

    @Test
    fun when_image_quality_click_then_dialog_shown() {
        viewModel.onImageQualityClick()

        assertThat(viewModel.dialogState).isInstanceOf(DialogState.Shown::class.java)
        val data = (viewModel.dialogState as DialogState.Shown<*>).data
        assertThat(data).isEqualTo(SettingDialogEnum.IMAGE_QUALITY)
    }

    @Test
    fun when_image_quality_selected_then_setting_updated() = runTest {
        val collectJob = launch(UnconfinedTestDispatcher()) {
            viewModel.uiState.collect()
        }

        viewModel.onImageQualitySelected(ImageQualityEnum.MEDIUM)

        val settings = settingRepository.appSettingsModel.first()
        assertThat(settings.imageQuality).isEqualTo(ImageQualityEnum.MEDIUM)

        collectJob.cancel()
    }

    @Test
    fun when_security_verification_disabled_then_fingerprint_also_disabled() = runTest {
        // 先设置已开启的状态
        settingRepository.updateNeedSecurityVerificationWhenLaunch(true)
        settingRepository.updateEnableFingerprintVerification(true)

        viewModel.onNeedSecurityVerificationWhenLaunchChanged(false)

        val settings = settingRepository.appSettingsModel.first()
        assertThat(settings.needSecurityVerificationWhenLaunch).isFalse()
        assertThat(settings.enableFingerprintVerification).isFalse()
    }

    @Test
    fun given_has_password_when_enable_security_then_switch_enabled() = runTest {
        // 设置已有密码
        settingRepository.updatePasswordInfo("some_encrypted_password")

        viewModel.onNeedSecurityVerificationWhenLaunchChanged(true)

        val settings = settingRepository.appSettingsModel.first()
        assertThat(settings.needSecurityVerificationWhenLaunch).isTrue()
    }

    @Test
    fun given_no_password_when_enable_security_then_show_create_password_dialog() = runTest {
        // 确保没有密码（默认为空字符串）
        viewModel.onNeedSecurityVerificationWhenLaunchChanged(true)

        assertThat(viewModel.dialogState).isInstanceOf(DialogState.Shown::class.java)
        val data = (viewModel.dialogState as DialogState.Shown<*>).data
        assertThat(data).isEqualTo(SettingDialogEnum.CREATE_PASSWORD)
    }

    @Test
    fun when_enable_fingerprint_then_show_verify_password_dialog() {
        viewModel.onEnableFingerprintVerificationChanged(true)

        assertThat(viewModel.dialogState).isInstanceOf(DialogState.Shown::class.java)
        val data = (viewModel.dialogState as DialogState.Shown<*>).data
        assertThat(data).isEqualTo(SettingDialogEnum.VERIFY_PASSWORD)
    }

    @Test
    fun when_disable_fingerprint_then_setting_updated() = runTest {
        settingRepository.updateEnableFingerprintVerification(true)

        viewModel.onEnableFingerprintVerificationChanged(false)

        val settings = settingRepository.appSettingsModel.first()
        assertThat(settings.enableFingerprintVerification).isFalse()
    }

    @Test
    fun given_has_password_when_password_click_then_show_modify_dialog() = runTest {
        settingRepository.updatePasswordInfo("encrypted_pwd")

        viewModel.onPasswordClick()

        assertThat(viewModel.dialogState).isInstanceOf(DialogState.Shown::class.java)
        val data = (viewModel.dialogState as DialogState.Shown<*>).data
        assertThat(data).isEqualTo(SettingDialogEnum.MODIFY_PASSWORD)
    }

    @Test
    fun given_no_password_when_password_click_then_show_create_dialog() = runTest {
        viewModel.onPasswordClick()

        assertThat(viewModel.dialogState).isInstanceOf(DialogState.Shown::class.java)
        val data = (viewModel.dialogState as DialogState.Shown<*>).data
        assertThat(data).isEqualTo(SettingDialogEnum.CREATE_PASSWORD)
    }

    @Test
    fun when_dark_mode_click_then_dialog_shown() {
        viewModel.onDarkModeClick()

        assertThat(viewModel.dialogState).isInstanceOf(DialogState.Shown::class.java)
        val data = (viewModel.dialogState as DialogState.Shown<*>).data
        assertThat(data).isEqualTo(SettingDialogEnum.DARK_MODE)
    }

    @Test
    fun when_dark_mode_selected_then_setting_updated() = runTest {
        viewModel.onDarkModeSelected(DarkModeEnum.DARK)

        val settings = settingRepository.appSettingsModel.first()
        assertThat(settings.darkMode).isEqualTo(DarkModeEnum.DARK)
    }

    @Test
    fun when_dynamic_color_click_then_dialog_shown() {
        viewModel.onDynamicColorClick()

        assertThat(viewModel.dialogState).isInstanceOf(DialogState.Shown::class.java)
        val data = (viewModel.dialogState as DialogState.Shown<*>).data
        assertThat(data).isEqualTo(SettingDialogEnum.DYNAMIC_COLOR)
    }

    @Test
    fun when_dynamic_color_selected_then_setting_updated() = runTest {
        viewModel.onDynamicColorSelected(true)

        val settings = settingRepository.appSettingsModel.first()
        assertThat(settings.dynamicColor).isTrue()
    }

    @Test
    fun when_verification_mode_click_then_dialog_shown() {
        viewModel.onVerificationModeClick()

        assertThat(viewModel.dialogState).isInstanceOf(DialogState.Shown::class.java)
        val data = (viewModel.dialogState as DialogState.Shown<*>).data
        assertThat(data).isEqualTo(SettingDialogEnum.VERIFICATION_MODE)
    }

    @Test
    fun when_verification_mode_selected_then_setting_updated() = runTest {
        viewModel.onVerificationModeSelected(VerificationModeEnum.WHEN_FOREGROUND)

        val settings = settingRepository.appSettingsModel.first()
        assertThat(settings.verificationMode).isEqualTo(VerificationModeEnum.WHEN_FOREGROUND)
    }

    @Test
    fun when_clear_password_click_then_dialog_shown() {
        viewModel.onClearPasswordClick()

        assertThat(viewModel.dialogState).isInstanceOf(DialogState.Shown::class.java)
        val data = (viewModel.dialogState as DialogState.Shown<*>).data
        assertThat(data).isEqualTo(SettingDialogEnum.CLEAR_PASSWORD)
    }

    @Test
    fun when_dismiss_dialog_then_state_is_dismiss() {
        viewModel.onImageQualityClick()
        assertThat(viewModel.dialogState).isInstanceOf(DialogState.Shown::class.java)

        viewModel.dismissDialog()

        assertThat(viewModel.dialogState).isEqualTo(DialogState.Dismiss)
    }

    @Test
    fun when_dismiss_bookmark_then_state_is_empty() {
        viewModel.onFingerprintVerifyError(1, "测试错误")
        assertThat(viewModel.shouldDisplayBookmark).isNotEmpty()

        viewModel.dismissBookmark()

        assertThat(viewModel.shouldDisplayBookmark).isEmpty()
    }

    @Test
    fun when_fingerprint_verify_error_then_bookmark_shown() {
        viewModel.onFingerprintVerifyError(1, "指纹验证失败")

        assertThat(viewModel.shouldDisplayBookmark).isEqualTo("指纹验证失败")
        assertThat(viewModel.dialogState).isEqualTo(DialogState.Dismiss)
    }

    @Test
    fun when_ui_state_reflects_settings_then_all_fields_mapped() = runTest {
        settingRepository.updateMobileNetworkDownloadEnable(true)
        settingRepository.updateDarkMode(DarkModeEnum.DARK)
        settingRepository.updateDynamicColor(true)
        settingRepository.updatePasswordInfo("some_pwd")

        val collectJob = launch(UnconfinedTestDispatcher()) {
            viewModel.uiState.collect()
        }

        val state = viewModel.uiState.value as SettingUiState.Success
        assertThat(state.mobileNetworkDownloadEnable).isTrue()
        assertThat(state.darkMode).isEqualTo(DarkModeEnum.DARK)
        assertThat(state.dynamicColor).isTrue()
        assertThat(state.hasPassword).isTrue()

        collectJob.cancel()
    }
}
