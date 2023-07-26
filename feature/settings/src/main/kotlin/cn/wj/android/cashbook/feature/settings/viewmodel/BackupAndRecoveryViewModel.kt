package cn.wj.android.cashbook.feature.settings.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.wj.android.cashbook.core.data.repository.SettingRepository
import cn.wj.android.cashbook.core.data.uitl.BackupRecoveryManager
import cn.wj.android.cashbook.core.data.uitl.BackupRecoveryState
import cn.wj.android.cashbook.core.data.uitl.NetworkMonitor
import cn.wj.android.cashbook.core.model.enums.AutoBackupModeEnum
import cn.wj.android.cashbook.core.ui.DialogState
import cn.wj.android.cashbook.feature.settings.enums.BackupListEnum
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
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

    var dialogState by mutableStateOf<DialogState>(DialogState.Dismiss)
        private set

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

    private val backupListType = MutableStateFlow(BackupListEnum.NONE)
    private val webBackupListData = backupRecoveryManager.onlineBackupListData.mapLatest { list ->
        val webDAVDomain = settingRepository.appDataMode.first().webDAVDomain
        list.map {
            if (!it.path.startsWith(webDAVDomain)) {
                it.copy(path = webDAVDomain + it.path)
            } else {
                it
            }
        }
    }
    private val localBackupListData = backupRecoveryManager.localBackupListData
    private val localCustomBackupList = backupRecoveryManager.localCustomBackupListData
    val backupListData = backupListType.flatMapLatest {
        when (it) {
            BackupListEnum.WEB -> webBackupListData
            BackupListEnum.LOCAL -> localBackupListData
            BackupListEnum.CUSTOM -> localCustomBackupList
            BackupListEnum.NONE -> flowOf(emptyList())
        }
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000L),
            initialValue = emptyList(),
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

    fun getRecoveryList(onlyLocal: Boolean, localPath: String) {
        viewModelScope.launch {
            if (!onlyLocal && isConnected.first()) {
                // 使用云端数据
                backupListType.tryEmit(BackupListEnum.WEB)
            } else {
                if (localPath.isNotBlank()) {
                    backupRecoveryManager.refreshLocalPath(localPath)
                    backupListType.tryEmit(BackupListEnum.CUSTOM)
                } else {
                    backupListType.tryEmit(BackupListEnum.LOCAL)
                }
            }
            backupListData.first()
            delay(500L)
            dialogState = DialogState.Shown(0)
        }
    }

    fun tryRecovery(backupPath: String) {
        viewModelScope.launch {
            backupRecoveryManager.requestRecovery(backupPath)
            dismissDialog()
        }
    }

    fun showSelectAutoBackupDialog() {
        // TODO
    }

    fun dismissBookmark() {
        backupRecoveryManager.updateBackupState(BackupRecoveryState.None)
        backupRecoveryManager.updateRecoveryState(BackupRecoveryState.None)
    }

    fun dismissDialog() {
        dialogState = DialogState.Dismiss
    }
}

data class BackupAndRecoveryUiState(
    val webDAVDomain: String = "",
    val webDAVAccount: String = "",
    val webDAVPassword: String = "",
    val backupPath: String = "",
    val autoBackup: AutoBackupModeEnum = AutoBackupModeEnum.CLOSE,
)