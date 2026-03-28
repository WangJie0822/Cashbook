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
import cn.wj.android.cashbook.core.data.repository.BooksRepository
import cn.wj.android.cashbook.core.data.repository.RecordRepository
import cn.wj.android.cashbook.core.data.repository.SettingRepository
import cn.wj.android.cashbook.core.data.uitl.BackupRecoveryManager
import cn.wj.android.cashbook.core.data.uitl.BackupRecoveryState
import cn.wj.android.cashbook.core.data.uitl.NetworkMonitor
import cn.wj.android.cashbook.core.model.enums.AutoBackupModeEnum
import cn.wj.android.cashbook.core.model.model.BooksModel
import cn.wj.android.cashbook.core.ui.DialogState
import cn.wj.android.cashbook.core.ui.ProgressDialogController
import cn.wj.android.cashbook.domain.usecase.ExportRecordUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
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
    private val booksRepository: BooksRepository,
    private val backupRecoveryManager: BackupRecoveryManager,
    private val networkMonitor: NetworkMonitor,
    private val exportRecordUseCase: ExportRecordUseCase,
) : ViewModel() {

    /** 进度弹窗控制器 */
    private var progressDialogController: ProgressDialogController? = null

    /** 设置进度弹窗控制器 */
    fun setProgressDialogController(controller: ProgressDialogController) {
        progressDialogController = controller
    }

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
                progressDialogController?.show(cancelable = false)
            } else {
                progressDialogController?.dismiss()
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

    fun refreshDbMigrate(controller: ProgressDialogController) {
        viewModelScope.launch {
            controller.show()
            recordRepository.migrateAfter9To10()
            controller.dismiss()
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

    /** 账本列表（用于导出 Bottom Sheet 的账本选择器） */
    val booksList: StateFlow<List<BooksModel>> = booksRepository.booksListData
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** 当前选中账本（用于导出的默认账本） */
    val currentBook: StateFlow<BooksModel?> = booksRepository.currentBook
        .map<BooksModel, BooksModel?> { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** 导出状态 */
    private val _exportState = MutableStateFlow<ExportState>(ExportState.Idle)
    val exportState: StateFlow<ExportState> = _exportState.asStateFlow()

    /** 查询指定账本最早的记录时间 */
    suspend fun getEarliestRecordTime(booksId: Long): Long? =
        recordRepository.queryEarliestRecordTime(booksId)

    /** 查询导出记录数量（endDate 为 exclusive） */
    suspend fun countExportRecords(booksId: Long, startDate: Long, endDate: Long): Int =
        recordRepository.countExportRecords(booksId, startDate, endDate)

    /**
     * 执行导出
     */
    fun exportRecords(
        booksId: Long,
        startDate: Long,
        endDate: Long,
        bookName: String,
        displayStartDate: Long,
        displayEndDate: Long,
        cacheDir: File,
    ) {
        viewModelScope.launch {
            _exportState.value = ExportState.Exporting
            try {
                val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
                val fileName = "一日记账_${bookName}_${dateFormat.format(Date(displayStartDate))}_${dateFormat.format(Date(displayEndDate))}.csv"
                val outputFile = File(File(cacheDir, "export"), fileName)
                outputFile.parentFile?.mkdirs()
                val count = exportRecordUseCase(booksId, startDate, endDate, outputFile)
                _exportState.value = ExportState.Done(outputFile.absolutePath, count)
            } catch (e: Exception) {
                _exportState.value = ExportState.Error(e.message ?: "导出失败")
            }
        }
    }

    /** 重置导出状态 */
    fun resetExportState() {
        _exportState.value = ExportState.Idle
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

/** 导出状态 */
sealed interface ExportState {
    /** 空闲 */
    data object Idle : ExportState

    /** 导出中 */
    data object Exporting : ExportState

    /** 导出完成 */
    data class Done(val filePath: String, val count: Int) : ExportState

    /** 导出失败 */
    data class Error(val message: String) : ExportState
}
