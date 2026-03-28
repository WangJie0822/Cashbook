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

import cn.wj.android.cashbook.core.data.uitl.BackupRecoveryState
import cn.wj.android.cashbook.core.model.enums.AutoBackupModeEnum
import cn.wj.android.cashbook.core.model.model.BackupModel
import cn.wj.android.cashbook.core.testing.repository.FakeBackupRecoveryManager
import cn.wj.android.cashbook.core.testing.repository.FakeRecordRepository
import cn.wj.android.cashbook.core.testing.repository.FakeSettingRepository
import cn.wj.android.cashbook.core.testing.util.FakeNetworkMonitor
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

class BackupAndRecoveryViewModelTest {

    @get:Rule
    val testDispatcher = TestDispatcherRule()

    private lateinit var settingRepository: FakeSettingRepository
    private lateinit var recordRepository: FakeRecordRepository
    private lateinit var backupRecoveryManager: FakeBackupRecoveryManager
    private lateinit var networkMonitor: FakeNetworkMonitor
    private lateinit var viewModel: BackupAndRecoveryViewModel

    @Before
    fun setup() {
        settingRepository = FakeSettingRepository()
        recordRepository = FakeRecordRepository()
        backupRecoveryManager = FakeBackupRecoveryManager()
        networkMonitor = FakeNetworkMonitor()
        viewModel = BackupAndRecoveryViewModel(
            settingRepository = settingRepository,
            recordRepository = recordRepository,
            backupRecoveryManager = backupRecoveryManager,
            networkMonitor = networkMonitor,
        )
    }

    // region 初始状态

    @Test
    fun when_initial_state_then_loading() {
        assertThat(viewModel.uiState.value).isEqualTo(BackupAndRecoveryUiState.Loading)
    }

    @Test
    fun when_initial_state_then_dialog_dismissed() {
        assertThat(viewModel.dialogState).isEqualTo(DialogState.Dismiss)
    }

    @Test
    fun when_initial_state_then_not_connected() {
        assertThat(viewModel.isConnected.value).isFalse()
    }

    @Test
    fun when_settings_loaded_then_state_is_success() = runTest {
        val collectJob = launch(UnconfinedTestDispatcher()) {
            viewModel.uiState.collect()
        }

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(BackupAndRecoveryUiState.Success::class.java)

        collectJob.cancel()
    }

    @Test
    fun when_settings_loaded_with_blank_domain_then_default_domain_used() = runTest {
        // 默认 webDAVDomain 为空字符串
        val collectJob = launch(UnconfinedTestDispatcher()) {
            viewModel.uiState.collect()
        }

        val state = viewModel.uiState.value as BackupAndRecoveryUiState.Success
        assertThat(state.webDAVDomain).isEqualTo("https://dav.jianguoyun.com/dav/")

        collectJob.cancel()
    }

    @Test
    fun when_settings_loaded_with_custom_domain_then_custom_domain_used() = runTest {
        settingRepository.updateWebDAV(
            domain = "https://custom.webdav.com/",
            account = "user",
            password = "pass",
        )

        val collectJob = launch(UnconfinedTestDispatcher()) {
            viewModel.uiState.collect()
        }

        val state = viewModel.uiState.value as BackupAndRecoveryUiState.Success
        assertThat(state.webDAVDomain).isEqualTo("https://custom.webdav.com/")
        assertThat(state.webDAVAccount).isEqualTo("user")
        assertThat(state.webDAVPassword).isEqualTo("pass")

        collectJob.cancel()
    }

    @Test
    fun when_settings_loaded_then_all_fields_mapped() = runTest {
        settingRepository.updateBackupPath("/backup/path")
        settingRepository.updateAutoBackupMode(AutoBackupModeEnum.WHEN_LAUNCH)
        settingRepository.updateKeepLatestBackup(true)
        settingRepository.updateMobileNetworkBackupEnable(true)

        val collectJob = launch(UnconfinedTestDispatcher()) {
            viewModel.uiState.collect()
        }

        val state = viewModel.uiState.value as BackupAndRecoveryUiState.Success
        assertThat(state.backupPath).isEqualTo("/backup/path")
        assertThat(state.autoBackup).isEqualTo(AutoBackupModeEnum.WHEN_LAUNCH)
        assertThat(state.keepLatestBackup).isTrue()
        assertThat(state.mobileNetworkBackupEnable).isTrue()

        collectJob.cancel()
    }

    @Test
    fun when_no_backup_time_then_last_backup_time_empty() = runTest {
        val collectJob = launch(UnconfinedTestDispatcher()) {
            viewModel.uiState.collect()
        }

        val state = viewModel.uiState.value as BackupAndRecoveryUiState.Success
        assertThat(state.lastBackupTime).isEmpty()

        collectJob.cancel()
    }

    @Test
    fun when_has_backup_time_then_last_backup_time_formatted() = runTest {
        settingRepository.updateBackupMs(1704067200000L) // 2024-01-01

        val collectJob = launch(UnconfinedTestDispatcher()) {
            viewModel.uiState.collect()
        }

        val state = viewModel.uiState.value as BackupAndRecoveryUiState.Success
        assertThat(state.lastBackupTime).isNotEmpty()

        collectJob.cancel()
    }

    // endregion

    // region WebDAV 连接配置

    @Test
    fun when_save_webdav_with_new_config_then_setting_updated() = runTest {
        val collectJob = launch(UnconfinedTestDispatcher()) {
            viewModel.uiState.collect()
        }

        viewModel.saveWebDAV(
            domain = "https://new.webdav.com/",
            account = "newuser",
            password = "newpass",
        )

        val settings = settingRepository.appSettingsModel.first()
        assertThat(settings.webDAVDomain).isEqualTo("https://new.webdav.com/")
        assertThat(settings.webDAVAccount).isEqualTo("newuser")
        assertThat(settings.webDAVPassword).isEqualTo("newpass")

        collectJob.cancel()
    }

    @Test
    fun when_save_webdav_with_same_config_then_refresh_connection() = runTest {
        // 先设置 WebDAV 配置
        settingRepository.updateWebDAV(
            domain = "https://existing.webdav.com/",
            account = "user",
            password = "pass",
        )

        val collectJob = launch(UnconfinedTestDispatcher()) {
            viewModel.uiState.collect()
        }

        // 使用相同配置调用 saveWebDAV
        viewModel.saveWebDAV(
            domain = "https://existing.webdav.com/",
            account = "user",
            password = "pass",
        )

        assertThat(backupRecoveryManager.refreshWebDAVConnectedCalled).isTrue()

        collectJob.cancel()
    }

    @Test
    fun when_webdav_connected_then_is_connected_true() = runTest {
        val collectJob = launch(UnconfinedTestDispatcher()) {
            viewModel.isConnected.collect()
        }

        backupRecoveryManager.setWebDAVConnected(true)

        assertThat(viewModel.isConnected.value).isTrue()

        collectJob.cancel()
    }

    // endregion

    // region 备份流程

    @Test
    fun when_backup_with_wifi_then_request_backup() = runTest {
        networkMonitor.setWifi(true)

        viewModel.backup()

        assertThat(backupRecoveryManager.requestBackupCalled).isTrue()
    }

    @Test
    fun given_webdav_connected_and_no_mobile_backup_and_no_wifi_when_backup_then_show_dialog() = runTest {
        backupRecoveryManager.setWebDAVConnected(true)
        networkMonitor.setWifi(false)
        // mobileNetworkBackupEnable 默认为 false

        val collectJob = launch(UnconfinedTestDispatcher()) {
            viewModel.isConnected.collect()
        }

        viewModel.backup()

        assertThat(viewModel.dialogState).isInstanceOf(DialogState.Shown::class.java)
        val data = (viewModel.dialogState as DialogState.Shown<*>).data
        assertThat(data).isEqualTo(1)

        collectJob.cancel()
    }

    @Test
    fun given_webdav_connected_and_mobile_backup_enabled_when_backup_then_request_backup() = runTest {
        backupRecoveryManager.setWebDAVConnected(true)
        settingRepository.updateMobileNetworkBackupEnable(true)
        networkMonitor.setWifi(false)

        val collectJob = launch(UnconfinedTestDispatcher()) {
            viewModel.isConnected.collect()
        }

        viewModel.backup()

        assertThat(backupRecoveryManager.requestBackupCalled).isTrue()

        collectJob.cancel()
    }

    @Test
    fun given_webdav_not_connected_and_no_wifi_when_backup_then_request_backup() = runTest {
        backupRecoveryManager.setWebDAVConnected(false)
        networkMonitor.setWifi(false)

        val collectJob = launch(UnconfinedTestDispatcher()) {
            viewModel.isConnected.collect()
        }

        viewModel.backup()

        // WebDAV 未连接时不检查网络状态，直接备份
        assertThat(backupRecoveryManager.requestBackupCalled).isTrue()

        collectJob.cancel()
    }

    @Test
    fun when_no_wifi_confirm_backup_then_request_backup() = runTest {
        viewModel.onNoWifiConfirmBackupClick(noMorePrompt = false)

        assertThat(backupRecoveryManager.requestBackupCalled).isTrue()
        assertThat(viewModel.dialogState).isEqualTo(DialogState.Dismiss)
    }

    @Test
    fun when_no_wifi_confirm_backup_with_no_more_prompt_then_enable_mobile_backup() = runTest {
        viewModel.onNoWifiConfirmBackupClick(noMorePrompt = true)

        val settings = settingRepository.appSettingsModel.first()
        assertThat(settings.mobileNetworkBackupEnable).isTrue()
        assertThat(backupRecoveryManager.requestBackupCalled).isTrue()
    }

    // endregion

    // region 恢复流程

    @Test
    fun when_try_recovery_then_request_recovery_and_dismiss_dialog() = runTest {
        viewModel.tryRecovery("/backup/file.zip")

        assertThat(backupRecoveryManager.lastRecoveryPath).isEqualTo("/backup/file.zip")
        assertThat(viewModel.dialogState).isEqualTo(DialogState.Dismiss)
    }

    @Test
    fun when_get_recovery_list_with_results_then_show_dialog() = runTest {
        val backupList = listOf(
            BackupModel("backup_20240101.zip", "/path/backup_20240101.zip"),
            BackupModel("backup_20240102.zip", "/path/backup_20240102.zip"),
        )
        backupRecoveryManager.setRecoveryList(backupList)

        viewModel.getRecoveryList(onlyLocal = true, localPath = "/backup")

        assertThat(viewModel.dialogState).isInstanceOf(DialogState.Shown::class.java)
        @Suppress("UNCHECKED_CAST")
        val data = (viewModel.dialogState as DialogState.Shown<List<BackupModel>>).data
        assertThat(data).hasSize(2)
    }

    @Test
    fun when_get_recovery_list_empty_then_no_dialog() = runTest {
        backupRecoveryManager.setRecoveryList(emptyList())

        viewModel.getRecoveryList(onlyLocal = true, localPath = "/backup")

        assertThat(viewModel.dialogState).isEqualTo(DialogState.Dismiss)
    }

    @Test
    fun when_get_recovery_list_then_params_passed_correctly() = runTest {
        backupRecoveryManager.setRecoveryList(emptyList())

        viewModel.getRecoveryList(onlyLocal = false, localPath = "/custom/path")

        assertThat(backupRecoveryManager.lastGetRecoveryListOnlyLocal).isFalse()
        assertThat(backupRecoveryManager.lastGetRecoveryListLocalPath).isEqualTo("/custom/path")
    }

    // endregion

    // region 自动备份配置

    @Test
    fun when_display_select_auto_backup_dialog_then_dialog_shown() {
        viewModel.displaySelectAutoBackupDialog()

        assertThat(viewModel.dialogState).isInstanceOf(DialogState.Shown::class.java)
        val data = (viewModel.dialogState as DialogState.Shown<*>).data
        assertThat(data).isEqualTo(0)
    }

    @Test
    fun when_auto_backup_mode_selected_then_setting_updated() = runTest {
        viewModel.onAutoBackupModeSelected(AutoBackupModeEnum.WHEN_LAUNCH)

        val settings = settingRepository.appSettingsModel.first()
        assertThat(settings.autoBackup).isEqualTo(AutoBackupModeEnum.WHEN_LAUNCH)
    }

    @Test
    fun when_change_keep_latest_backup_then_setting_updated() = runTest {
        viewModel.changeKeepLatestBackup(true)

        val settings = settingRepository.appSettingsModel.first()
        assertThat(settings.keepLatestBackup).isTrue()
    }

    @Test
    fun when_mobile_network_backup_enable_changed_then_setting_updated() = runTest {
        viewModel.onMobileNetworkBackupEnableChanged(true)

        val settings = settingRepository.appSettingsModel.first()
        assertThat(settings.mobileNetworkBackupEnable).isTrue()
    }

    // endregion

    // region 本地备份路径管理

    @Test
    fun when_save_backup_path_then_setting_updated() = runTest {
        viewModel.saveBackupPath("/new/backup/path")

        val settings = settingRepository.appSettingsModel.first()
        assertThat(settings.backupPath).isEqualTo("/new/backup/path")
    }

    // endregion

    // region 提示状态管理

    @Test
    fun when_dismiss_bookmark_then_states_reset() = runTest {
        backupRecoveryManager.updateBackupState(
            BackupRecoveryState.Success(BackupRecoveryState.SUCCESS_BACKUP),
        )

        viewModel.dismissBookmark()

        val backupState = backupRecoveryManager.backupState.first()
        val recoveryState = backupRecoveryManager.recoveryState.first()
        assertThat(backupState).isEqualTo(BackupRecoveryState.None)
        assertThat(recoveryState).isEqualTo(BackupRecoveryState.None)
    }

    @Test
    fun when_dismiss_dialog_then_state_is_dismiss() {
        viewModel.displaySelectAutoBackupDialog()
        assertThat(viewModel.dialogState).isInstanceOf(DialogState.Shown::class.java)

        viewModel.dismissDialog()

        assertThat(viewModel.dialogState).isEqualTo(DialogState.Dismiss)
    }

    // endregion

    // region shouldDisplayBookmark

    @Test
    fun when_backup_and_recovery_none_then_bookmark_is_zero() = runTest {
        val collectJob = launch(UnconfinedTestDispatcher()) {
            viewModel.shouldDisplayBookmark.collect()
        }

        assertThat(viewModel.shouldDisplayBookmark.value).isEqualTo(0)

        collectJob.cancel()
    }

    @Test
    fun when_backup_failed_then_bookmark_shows_backup_code() = runTest {
        val collectJob = launch(UnconfinedTestDispatcher()) {
            viewModel.shouldDisplayBookmark.collect()
        }

        backupRecoveryManager.updateBackupState(
            BackupRecoveryState.Failed(BackupRecoveryState.FAILED_BLANK_BACKUP_PATH),
        )

        assertThat(viewModel.shouldDisplayBookmark.value)
            .isEqualTo(BackupRecoveryState.FAILED_BLANK_BACKUP_PATH)

        collectJob.cancel()
    }

    @Test
    fun when_recovery_failed_then_bookmark_shows_recovery_code() = runTest {
        val collectJob = launch(UnconfinedTestDispatcher()) {
            viewModel.shouldDisplayBookmark.collect()
        }

        backupRecoveryManager.updateRecoveryState(
            BackupRecoveryState.Failed(BackupRecoveryState.FAILED_FILE_FORMAT_ERROR),
        )

        assertThat(viewModel.shouldDisplayBookmark.value)
            .isEqualTo(BackupRecoveryState.FAILED_FILE_FORMAT_ERROR)

        collectJob.cancel()
    }

    @Test
    fun when_backup_success_then_bookmark_shows_success_code() = runTest {
        val collectJob = launch(UnconfinedTestDispatcher()) {
            viewModel.shouldDisplayBookmark.collect()
        }

        backupRecoveryManager.updateBackupState(
            BackupRecoveryState.Success(BackupRecoveryState.SUCCESS_BACKUP),
        )

        assertThat(viewModel.shouldDisplayBookmark.value)
            .isEqualTo(BackupRecoveryState.SUCCESS_BACKUP)

        collectJob.cancel()
    }

    // endregion

    // region 数据库迁移

    @Test
    fun when_refresh_db_migrate_then_no_crash() = runTest {
        // 验证 refreshDbMigrate 不会崩溃
        viewModel.refreshDbMigrate()
    }

    // endregion
}
