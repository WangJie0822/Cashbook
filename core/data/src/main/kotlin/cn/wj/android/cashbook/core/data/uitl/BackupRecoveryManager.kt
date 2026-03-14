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

package cn.wj.android.cashbook.core.data.uitl

import cn.wj.android.cashbook.core.model.model.BackupModel
import kotlinx.coroutines.flow.Flow

/**
 * 备份恢复管理接口
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/7/21
 */
interface BackupRecoveryManager {

    val isWebDAVConnected: Flow<Boolean>

    val backupState: Flow<BackupRecoveryState>

    val recoveryState: Flow<BackupRecoveryState>

    fun refreshWebDAVConnected()

    fun updateBackupState(state: BackupRecoveryState)

    fun updateRecoveryState(state: BackupRecoveryState)

    suspend fun requestAutoBackup()

    suspend fun requestBackup(onlyLocal: Boolean = false)

    suspend fun requestRecovery(path: String)

    suspend fun getRecoveryList(onlyLocal: Boolean, localPath: String): List<BackupModel>
}

sealed class BackupRecoveryState(open val code: Int = 0) {

    companion object {
        const val FAILED_BLANK_BACKUP_PATH = -3012
        const val FAILED_BACKUP_PATH_UNAUTHORIZED = -3013
        const val FAILED_BACKUP_WEBDAV = -3014
        const val FAILED_FILE_FORMAT_ERROR = -3015
        const val FAILED_BACKUP_PATH_EMPTY = -3016

        const val SUCCESS_BACKUP = 2001
        const val SUCCESS_RECOVERY = 2002
    }

    data object None : BackupRecoveryState()
    data object InProgress : BackupRecoveryState()
    data class Failed(override val code: Int) : BackupRecoveryState()
    data class Success(override val code: Int) : BackupRecoveryState()
}
