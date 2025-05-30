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

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.wj.android.cashbook.core.common.tools.dateFormat
import cn.wj.android.cashbook.core.data.repository.RecordRepository
import cn.wj.android.cashbook.core.data.repository.SettingRepository
import cn.wj.android.cashbook.core.data.uitl.BackupRecoveryManager
import cn.wj.android.cashbook.core.data.uitl.BackupRecoveryState
import cn.wj.android.cashbook.core.data.uitl.NetworkMonitor
import cn.wj.android.cashbook.core.model.enums.AutoBackupModeEnum
import cn.wj.android.cashbook.core.ui.DialogState
import cn.wj.android.cashbook.core.ui.ProgressDialogManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 备份与恢复 ViewModel
 *
 * @param settingRepository 设置相关数据仓库
 * @param backupRecoveryManager 备份与恢复管理器
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/7/18
 */
@HiltViewModel
class BackupAndRecoveryViewModel @Inject constructor(
    private val settingRepository: SettingRepository,
    private val recordRepository: RecordRepository,
    private val backupRecoveryManager: BackupRecoveryManager,
    private val networkMonitor: NetworkMonitor,
) : ViewModel() {

    /** 弹窗状态 */
    var dialogState by mutableStateOf<DialogState>(DialogState.Dismiss)
        private set

    /** 界面 UI 状态 */
    val uiState = settingRepository.appSettingsModel
        .mapLatest {
            BackupAndRecoveryUiState.Success(
                webDAVDomain = it.webDAVDomain.ifBlank {
                    "https://dav.jianguoyun.com/dav/"
                },
                webDAVAccount = it.webDAVAccount,
                webDAVPassword = it.webDAVPassword,
                backupPath = it.backupPath,
                lastBackupTime = if (it.lastBackupMs <= 0L) "" else it.lastBackupMs.dateFormat(),
                autoBackup = it.autoBackup,
                keepLatestBackup = it.keepLatestBackup,
                mobileNetworkBackupEnable = it.mobileNetworkBackupEnable,
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000L),
            initialValue = BackupAndRecoveryUiState.Loading,
        )

    /** WebDAV 连接状态 */
    val isConnected = backupRecoveryManager.isWebDAVConnected
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000L),
            initialValue = false,
        )

    /** 显示提示数据 */
    val shouldDisplayBookmark =
        combine(
            backupRecoveryManager.backupState,
            backupRecoveryManager.recoveryState,
        ) { backup, recovery ->
            if (recovery == BackupRecoveryState.InProgress || backup == BackupRecoveryState.InProgress) {
                ProgressDialogManager.show(cancelable = false)
            } else {
                ProgressDialogManager.dismiss()
            }
            if (backup.code != 0) {
                backup.code
            } else {
                recovery.code
            }
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000L),
                initialValue = 0,
            )

    /** 保存 WebDAV 配置 */
    fun saveWebDAV(domain: String, account: String, password: String) {
        viewModelScope.launch {
            val state = uiState.first()
            if (state is BackupAndRecoveryUiState.Success) {
                if (state.webDAVDomain == domain && state.webDAVAccount == account && state.webDAVPassword == password) {
                    // 未做修改，尝试重连
                    backupRecoveryManager.refreshWebDAVConnected()
                } else {
                    // 更新配置数据
                    settingRepository.updateWebDAV(
                        domain = domain,
                        account = account,
                        password = password,
                    )
                }
            }
        }
    }

    /** 保持本地备份路径 [path] */
    fun saveBackupPath(path: String) {
        viewModelScope.launch {
            settingRepository.updateBackupPath(path)
        }
    }

    /** 开始备份 */
    fun backup() {
        viewModelScope.launch {
            if (backupRecoveryManager.isWebDAVConnected.first() &&
                !settingRepository.appSettingsModel.first().mobileNetworkBackupEnable &&
                !networkMonitor.isWifi.first()
            ) {
                // WebDAV已连接，不允许移动数据备份，不是Wi-Fi，提示
                dialogState = DialogState.Shown(1)
                return@launch
            }
            backupRecoveryManager.requestBackup()
        }
    }

    /**
     * 获取备份数据列表
     * @param onlyLocal 仅从本地恢复
     * @param localPath 本地自定义备份路径
     */
    fun getRecoveryList(onlyLocal: Boolean, localPath: String) {
        viewModelScope.launch {
            val list = backupRecoveryManager.getRecoveryList(onlyLocal, localPath)
            if (list.isNotEmpty()) {
                dialogState = DialogState.Shown(list)
            }
        }
    }

    /** 从 [backupPath] 路径恢复备份 */
    fun tryRecovery(backupPath: String) {
        viewModelScope.launch {
            backupRecoveryManager.requestRecovery(backupPath)
            dismissDialog()
        }
    }

    /** 显示选择自动备份类型弹窗 */
    fun displaySelectAutoBackupDialog() {
        dialogState = DialogState.Shown(0)
    }

    /** 更新自动备份类型 */
    fun onAutoBackupModeSelected(autoBackup: AutoBackupModeEnum) {
        viewModelScope.launch {
            settingRepository.updateAutoBackupMode(autoBackup)
        }
    }

    fun changeKeepLatestBackup(keep: Boolean) {
        viewModelScope.launch {
            settingRepository.updateKeepLatestBackup(keep)
        }
    }

    fun onMobileNetworkBackupEnableChanged(keep: Boolean) {
        viewModelScope.launch {
            settingRepository.updateMobileNetworkBackupEnable(keep)
        }
    }

    fun onNoWifiConfirmBackupClick(noMorePrompt: Boolean) {
        dismissDialog()
        viewModelScope.launch {
            if (noMorePrompt) {
                settingRepository.updateMobileNetworkBackupEnable(true)
            }
            backupRecoveryManager.requestBackup()
        }
    }

    fun refreshDbMigrate() {
        viewModelScope.launch {
            ProgressDialogManager.show()
            recordRepository.migrateAfter9To10()
            ProgressDialogManager.dismiss()
        }
    }

    /** 隐藏提示 */
    fun dismissBookmark() {
        backupRecoveryManager.updateBackupState(BackupRecoveryState.None)
        backupRecoveryManager.updateRecoveryState(BackupRecoveryState.None)
    }

    /** 隐藏弹窗 */
    fun dismissDialog() {
        dialogState = DialogState.Dismiss
    }
}

sealed interface BackupAndRecoveryUiState {
    data object Loading : BackupAndRecoveryUiState
    data class Success(
        val webDAVDomain: String,
        val webDAVAccount: String,
        val webDAVPassword: String,
        val backupPath: String,
        val lastBackupTime: String,
        val autoBackup: AutoBackupModeEnum,
        val keepLatestBackup: Boolean,
        val mobileNetworkBackupEnable: Boolean,
    ) : BackupAndRecoveryUiState
}
