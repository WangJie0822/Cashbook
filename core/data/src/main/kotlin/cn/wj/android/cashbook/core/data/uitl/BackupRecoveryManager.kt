package cn.wj.android.cashbook.core.data.uitl

import android.net.Uri
import kotlinx.coroutines.flow.Flow

/**
 * 备份恢复管理
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/7/21
 */
interface BackupRecoveryManager {

    /** WebDAV 连接状态 */
    val isWebDAVConnected: Flow<Boolean>

    /** 备份状态 */
    val backupState: Flow<BackupRecoveryState>

    /** 恢复状态 */
    val recoveryState: Flow<BackupRecoveryState>

    /** 刷新 WebDAV 连接状态 */
    fun refreshWebDAVConnected()

    /** 上传文件 */
    suspend fun upload(backupFileUri: Uri): Boolean

    /** 请求备份 */
    suspend fun requestBackup()

    /** 更新备份状态 */
    fun updateBackupState(state: BackupRecoveryState)

    suspend fun requestRecovery(onlyLocal: Boolean = false)

    fun updateRecoveryState(state: BackupRecoveryState)
}

sealed class BackupRecoveryState(open val code: Int = 0) {

    companion object {
        const val FAILED_BLANK_BACKUP_PATH = -3012
        const val FAILED_BACKUP_PATH_UNAUTHORIZED = -3013
        const val FAILED_BACKUP_WEBDAV = -3014

        const val SUCCESS_BACKUP = 2001
    }

    object None : BackupRecoveryState()
    object InProgress : BackupRecoveryState()
    data class Failed(override val code: Int) : BackupRecoveryState()
    data class Success(override val code: Int) : BackupRecoveryState()
}