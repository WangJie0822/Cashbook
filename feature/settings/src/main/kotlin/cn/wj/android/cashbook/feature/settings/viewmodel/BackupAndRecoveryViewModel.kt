package cn.wj.android.cashbook.feature.settings.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.wj.android.cashbook.core.data.repository.SettingRepository
import cn.wj.android.cashbook.core.model.enums.AutoBackupModeEnum
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn

/**
 * 备份与恢复 ViewModel
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/7/18
 */
@HiltViewModel
class BackupAndRecoveryViewModel @Inject constructor(
    settingRepository: SettingRepository,
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


}

data class BackupAndRecoveryUiState(
    val webDAVDomain: String = "",
    val webDAVAccount: String = "",
    val webDAVPassword: String = "",
    val backupPath: String = "",
    val autoBackup: AutoBackupModeEnum = AutoBackupModeEnum.CLOSE,
)