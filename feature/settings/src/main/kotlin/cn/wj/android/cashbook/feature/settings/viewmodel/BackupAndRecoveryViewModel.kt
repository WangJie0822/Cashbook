package cn.wj.android.cashbook.feature.settings.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.wj.android.cashbook.core.data.repository.SettingRepository
import cn.wj.android.cashbook.core.data.uitl.BackupRecoveryManager
import cn.wj.android.cashbook.core.data.uitl.BackupRecoveryState
import cn.wj.android.cashbook.core.data.uitl.NetworkMonitor
import cn.wj.android.cashbook.core.model.enums.AutoBackupModeEnum
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 备份与恢复 ViewModel
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/7/18
 */
@HiltViewModel
class BackupAndRecoveryViewModel @Inject constructor(
    private val settingRepository: SettingRepository,
    private val backupRecoveryManager: BackupRecoveryManager,
    networkMonitor: NetworkMonitor,
) : ViewModel() {

    val uiState = settingRepository.appDataMode
        .mapLatest {
            BackupAndRecoveryUiState(
                webDAVDomain = it.webDAVDomain,
                webDAVAccount = it.webDAVAccount,
                webDAVPassword = it.webDAVPassword,
                backupPath = it.backupPath,
                autoBackup = it.autoBackup,
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000L),
            initialValue = BackupAndRecoveryUiState(),
        )

    val isConnected =
        combine(
            networkMonitor.isOnline,
            backupRecoveryManager.isWebDAVConnected
        ) { isOnline, isConnected ->
            isOnline && isConnected
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000L),
                initialValue = false,
            )

    // FIXME 数据变化无法触发
    val shouldDisplayBookmark =
        combine(
            backupRecoveryManager.backupState,
            backupRecoveryManager.recoveryState
        ) { backup, recovery ->
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

    fun saveWebDAV(domain: String, account: String, password: String) {
        viewModelScope.launch {
            val state = uiState.first()
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

    fun saveBackupPath(path: String) {
        viewModelScope.launch {
            settingRepository.updateBackupPath(path)
        }
    }

    fun backup() {
        viewModelScope.launch {
            backupRecoveryManager.requestBackup()
        }
    }

    fun recovery(onlyLocal: Boolean) {
        viewModelScope.launch {
            backupRecoveryManager.requestRecovery(onlyLocal)
        }
    }

    fun showSelectAutoBackupDialog() {

    }

    fun dismissBookmark() {
        viewModelScope.launch {
            backupRecoveryManager.updateBackupState(BackupRecoveryState.None)
            backupRecoveryManager.updateRecoveryState(BackupRecoveryState.None)
        }
    }
}

data class BackupAndRecoveryUiState(
    val webDAVDomain: String = "",
    val webDAVAccount: String = "",
    val webDAVPassword: String = "",
    val backupPath: String = "",
    val autoBackup: AutoBackupModeEnum = AutoBackupModeEnum.CLOSE,
)