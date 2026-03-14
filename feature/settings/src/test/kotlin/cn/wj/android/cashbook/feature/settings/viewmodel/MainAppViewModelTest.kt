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

import cn.wj.android.cashbook.core.common.ApplicationInfo
import cn.wj.android.cashbook.core.data.uitl.AppUpgradeManager
import cn.wj.android.cashbook.core.data.uitl.NetworkMonitor
import cn.wj.android.cashbook.core.model.entity.UpgradeInfoEntity
import cn.wj.android.cashbook.core.model.enums.AppUpgradeStateEnum
import cn.wj.android.cashbook.core.model.enums.VerificationModeEnum
import cn.wj.android.cashbook.core.testing.repository.FakeBooksRepository
import cn.wj.android.cashbook.core.testing.repository.FakeSettingRepository
import cn.wj.android.cashbook.core.testing.util.TestDispatcherRule
import cn.wj.android.cashbook.core.ui.DialogState
import cn.wj.android.cashbook.feature.settings.enums.MainAppBookmarkEnum
import cn.wj.android.cashbook.feature.settings.enums.SettingPasswordStateEnum
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File

class MainAppViewModelTest {

    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    private lateinit var settingRepository: FakeSettingRepository
    private lateinit var booksRepository: FakeBooksRepository
    private lateinit var fakeNetworkMonitor: FakeNetworkMonitor
    private lateinit var fakeAppUpgradeManager: FakeAppUpgradeManager
    private lateinit var viewModel: MainAppViewModel

    @Before
    fun setup() {
        settingRepository = FakeSettingRepository()
        booksRepository = FakeBooksRepository()
        fakeNetworkMonitor = FakeNetworkMonitor()
        fakeAppUpgradeManager = FakeAppUpgradeManager()

        // 设置默认版本信息用于测试
        ApplicationInfo.versionName = "v1.0.0"

        viewModel = MainAppViewModel(
            settingRepository = settingRepository,
            booksRepository = booksRepository,
            networkMonitor = fakeNetworkMonitor,
            appUpgradeManager = fakeAppUpgradeManager,
        )
    }

    // region 初始状态

    @Test
    fun when_initialized_then_uiState_is_loading() {
        assertThat(viewModel.uiState.value).isEqualTo(MainAppUiState.Loading)
    }

    @Test
    fun when_settings_loaded_then_uiState_is_success() = runTest {
        val collectJob = launch(UnconfinedTestDispatcher()) {
            viewModel.uiState.collect()
        }

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(MainAppUiState.Success::class.java)

        collectJob.cancel()
    }

    @Test
    fun when_initialized_then_verifyState_is_success() {
        assertThat(viewModel.verifyState).isEqualTo(SettingPasswordStateEnum.SUCCESS)
    }

    @Test
    fun when_initialized_then_firstOpen_is_true() {
        assertThat(viewModel.firstOpen).isTrue()
    }

    @Test
    fun when_initialized_then_shouldDisplayBookmark_is_none() {
        assertThat(viewModel.shouldDisplayBookmark).isEqualTo(MainAppBookmarkEnum.NONE)
    }

    @Test
    fun when_initialized_then_dialogState_is_dismiss() {
        assertThat(viewModel.dialogState).isEqualTo(DialogState.Dismiss)
    }

    @Test
    fun when_initialized_then_inRequestUpdateData_is_false() {
        assertThat(viewModel.inRequestUpdateData).isFalse()
    }

    @Test
    fun when_initialized_then_updateInfoData_is_null() {
        assertThat(viewModel.updateInfoData.value).isNull()
    }

    // endregion

    // region 协议相关

    @Test
    fun given_protocol_not_agreed_when_loaded_then_needRequestProtocol_is_true() = runTest {
        // 默认 agreedProtocol = false
        val collectJob = launch(UnconfinedTestDispatcher()) {
            viewModel.uiState.collect()
        }

        val state = viewModel.uiState.value as MainAppUiState.Success
        assertThat(state.needRequestProtocol).isTrue()

        collectJob.cancel()
    }

    @Test
    fun given_protocol_agreed_when_loaded_then_needRequestProtocol_is_false() = runTest {
        settingRepository.updateAgreedProtocol(true)

        val collectJob = launch(UnconfinedTestDispatcher()) {
            viewModel.uiState.collect()
        }

        val state = viewModel.uiState.value as MainAppUiState.Success
        assertThat(state.needRequestProtocol).isFalse()

        collectJob.cancel()
    }

    @Test
    fun when_agreeProtocol_then_setting_updated() = runTest {
        viewModel.agreeProtocol()

        val settings = settingRepository.appSettingsModel.first()
        assertThat(settings.agreedProtocol).isTrue()
    }

    // endregion

    // region 安全认证

    @Test
    fun given_security_disabled_when_loaded_then_needVerity_is_false() = runTest {
        // 默认 needSecurityVerificationWhenLaunch = false
        val collectJob = launch(UnconfinedTestDispatcher()) {
            viewModel.uiState.collect()
        }

        val state = viewModel.uiState.value as MainAppUiState.Success
        assertThat(state.needVerity).isFalse()

        collectJob.cancel()
    }

    @Test
    fun given_security_enabled_when_loaded_then_needVerity_is_true() = runTest {
        settingRepository.updateNeedSecurityVerificationWhenLaunch(true)

        // 需要重新创建 ViewModel 以获取新的初始状态
        viewModel = MainAppViewModel(
            settingRepository = settingRepository,
            booksRepository = booksRepository,
            networkMonitor = fakeNetworkMonitor,
            appUpgradeManager = fakeAppUpgradeManager,
        )

        val collectJob = launch(UnconfinedTestDispatcher()) {
            viewModel.uiState.collect()
        }

        val state = viewModel.uiState.value as MainAppUiState.Success
        assertThat(state.needVerity).isTrue()

        collectJob.cancel()
    }

    @Test
    fun given_fingerprint_enabled_when_loaded_then_supportFingerprint_is_true() = runTest {
        settingRepository.updateEnableFingerprintVerification(true)

        val collectJob = launch(UnconfinedTestDispatcher()) {
            viewModel.uiState.collect()
        }

        val state = viewModel.uiState.value as MainAppUiState.Success
        assertThat(state.supportFingerprint).isTrue()

        collectJob.cancel()
    }

    @Test
    fun when_refreshVerifyState_with_when_foreground_mode_then_verified_reset() = runTest {
        settingRepository.updateVerificationMode(VerificationModeEnum.WHEN_FOREGROUND)
        settingRepository.updateNeedSecurityVerificationWhenLaunch(true)

        // 重新创建 ViewModel
        viewModel = MainAppViewModel(
            settingRepository = settingRepository,
            booksRepository = booksRepository,
            networkMonitor = fakeNetworkMonitor,
            appUpgradeManager = fakeAppUpgradeManager,
        )

        val collectJob = launch(UnconfinedTestDispatcher()) {
            viewModel.uiState.collect()
        }

        // 刷新认证状态
        viewModel.refreshVerifyState()

        val state = viewModel.uiState.value as MainAppUiState.Success
        assertThat(state.needVerity).isTrue()
        assertThat(viewModel.firstOpen).isTrue()

        collectJob.cancel()
    }

    @Test
    fun when_refreshVerifyState_with_when_launch_mode_then_verified_not_reset() = runTest {
        // 默认 verificationMode = WHEN_LAUNCH
        settingRepository.updateNeedSecurityVerificationWhenLaunch(false)

        val collectJob = launch(UnconfinedTestDispatcher()) {
            viewModel.uiState.collect()
        }

        viewModel.refreshVerifyState()

        // WHEN_LAUNCH 模式下不重置，verified 保持不变
        val state = viewModel.uiState.value as MainAppUiState.Success
        assertThat(state.needVerity).isFalse()

        collectJob.cancel()
    }

    // endregion

    // region 指纹认证错误

    @Test
    fun when_onFingerprintVerifyError_then_bookmark_is_error() {
        viewModel.onFingerprintVerifyError(1, "指纹识别失败")

        assertThat(viewModel.shouldDisplayBookmark).isEqualTo(MainAppBookmarkEnum.ERROR)
        assertThat(viewModel.errorText).isEqualTo("指纹识别失败")
        assertThat(viewModel.dialogState).isEqualTo(DialogState.Dismiss)
    }

    @Test
    fun when_showFingerprintVerify_then_firstOpen_is_false() {
        // showFingerprintVerify 内部调用了 loadDecryptCipher，会因为 KeyStore 不可用而提前返回
        // 但 firstOpen 在调用时会被设为 false
        viewModel.showFingerprintVerify()

        assertThat(viewModel.firstOpen).isFalse()
    }

    // endregion

    // region 提示操作

    @Test
    fun when_dismissBookmark_then_bookmark_is_none() {
        viewModel.onFingerprintVerifyError(1, "错误")
        assertThat(viewModel.shouldDisplayBookmark).isNotEqualTo(MainAppBookmarkEnum.NONE)

        viewModel.dismissBookmark()

        assertThat(viewModel.shouldDisplayBookmark).isEqualTo(MainAppBookmarkEnum.NONE)
    }

    // endregion

    // region 更新检查

    @Test
    fun given_auto_check_disabled_when_checkUpdateAuto_then_no_update() = runTest {
        // 默认 autoCheckUpdate = false
        fakeNetworkMonitor.setOnline(true)

        viewModel.checkUpdateAuto()

        assertThat(viewModel.updateInfoData.value).isNull()
    }

    @Test
    fun given_no_network_when_checkUpdateAuto_then_no_update() = runTest {
        settingRepository.updateAutoCheckUpdate(true)
        fakeNetworkMonitor.setOnline(false)

        viewModel.checkUpdateAuto()

        assertThat(viewModel.updateInfoData.value).isNull()
    }

    @Test
    fun given_downloading_when_checkUpdate_then_show_downloading_bookmark() = runTest {
        fakeAppUpgradeManager.setUpgradeState(AppUpgradeStateEnum.DOWNLOADING)

        viewModel.checkUpdate()

        assertThat(viewModel.shouldDisplayBookmark).isEqualTo(MainAppBookmarkEnum.UPDATE_DOWNLOADING)
    }

    @Test
    fun given_no_network_when_checkUpdate_then_no_update() = runTest {
        fakeNetworkMonitor.setOnline(false)

        viewModel.checkUpdate()

        assertThat(viewModel.updateInfoData.value).isNull()
    }

    // endregion

    // region 升级确认

    @Test
    fun given_no_update_info_when_confirmUpdate_then_nothing_happens() {
        // updateInfoData 为 null 时直接返回
        viewModel.confirmUpdate()

        assertThat(viewModel.shouldDisplayBookmark).isEqualTo(MainAppBookmarkEnum.NONE)
    }

    @Test
    fun when_dismissUpdateDialog_without_ignore_then_update_info_cleared() = runTest {
        viewModel.dismissUpdateDialog(ignore = false)

        assertThat(viewModel.updateInfoData.value).isNull()
        val settings = settingRepository.appSettingsModel.first()
        assertThat(settings.ignoreUpdateVersion).isEmpty()
    }

    @Test
    fun when_dismissNoWifiUpdateDialog_then_confirmUpdateInfoData_cleared() {
        viewModel.dismissNoWifiUpdateDialog()

        assertThat(viewModel.confirmUpdateInfoData.value).isNull()
    }

    // endregion

    // region 忽略更新版本

    @Test
    fun when_initialized_then_ignoreUpdateVersionData_is_false() = runTest {
        val collectJob = launch(UnconfinedTestDispatcher()) {
            viewModel.ignoreUpdateVersionData.collect()
        }

        assertThat(viewModel.ignoreUpdateVersionData.value).isFalse()

        collectJob.cancel()
    }

    // endregion

    // region 升级状态影响提示

    @Test
    fun when_upgradeState_is_download_failed_then_bookmark_shows_download_failed() = runTest {
        val collectJob = launch(UnconfinedTestDispatcher()) {
            viewModel.uiState.collect()
        }

        fakeAppUpgradeManager.setUpgradeState(AppUpgradeStateEnum.DOWNLOAD_FAILED)

        assertThat(viewModel.shouldDisplayBookmark).isEqualTo(MainAppBookmarkEnum.DOWNLOAD_FAILED)

        collectJob.cancel()
    }

    @Test
    fun when_upgradeState_is_install_failed_then_bookmark_shows_install_failed() = runTest {
        val collectJob = launch(UnconfinedTestDispatcher()) {
            viewModel.uiState.collect()
        }

        fakeAppUpgradeManager.setUpgradeState(AppUpgradeStateEnum.INSTALL_FAILED)

        assertThat(viewModel.shouldDisplayBookmark).isEqualTo(MainAppBookmarkEnum.INSTALL_FAILED)

        collectJob.cancel()
    }

    // endregion

    // region 当前账本名称

    @Test
    fun when_loaded_then_currentBookName_from_repository() = runTest {
        val collectJob = launch(UnconfinedTestDispatcher()) {
            viewModel.uiState.collect()
        }

        val state = viewModel.uiState.value as MainAppUiState.Success
        assertThat(state.currentBookName).isEqualTo("默认账本")

        collectJob.cancel()
    }

    // endregion

    // region 内联 Fake 实现

    /** 测试用的 NetworkMonitor 实现 */
    private class FakeNetworkMonitor : NetworkMonitor {
        private val _isOnline = MutableStateFlow(true)
        private val _isWifi = MutableStateFlow(true)

        override val isOnline = _isOnline
        override val isWifi = _isWifi

        fun setOnline(online: Boolean) {
            _isOnline.value = online
        }

        fun setWifi(wifi: Boolean) {
            _isWifi.value = wifi
        }
    }

    /** 测试用的 AppUpgradeManager 实现 */
    private class FakeAppUpgradeManager : AppUpgradeManager {
        private val _upgradeState = MutableStateFlow(AppUpgradeStateEnum.NONE)
        override val upgradeState = _upgradeState

        var lastDownloadInfo: UpgradeInfoEntity? = null
            private set

        fun setUpgradeState(state: AppUpgradeStateEnum) {
            _upgradeState.value = state
        }

        override suspend fun startDownload(info: UpgradeInfoEntity) {
            lastDownloadInfo = info
            _upgradeState.value = AppUpgradeStateEnum.DOWNLOADING
        }

        override suspend fun updateDownloadProgress(progress: Int) {
            // no-op
        }

        override suspend fun downloadComplete(apkFile: File) {
            _upgradeState.value = AppUpgradeStateEnum.DOWNLOAD_SUCCESS
        }

        override suspend fun downloadFailed() {
            _upgradeState.value = AppUpgradeStateEnum.DOWNLOAD_FAILED
        }

        override suspend fun downloadStopped() {
            _upgradeState.value = AppUpgradeStateEnum.NONE
        }

        override suspend fun cancelDownload() {
            _upgradeState.value = AppUpgradeStateEnum.NONE
        }

        override suspend fun retry() {
            // no-op
        }
    }

    // endregion
}
