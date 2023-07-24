package cn.wj.android.cashbook.sync.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.core.net.toFile
import androidx.documentfile.provider.DocumentFile
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import cn.wj.android.cashbook.core.common.BACKUP_DIR_NAME
import cn.wj.android.cashbook.core.common.annotation.CashbookDispatchers
import cn.wj.android.cashbook.core.common.annotation.Dispatcher
import cn.wj.android.cashbook.core.common.ext.logger
import cn.wj.android.cashbook.core.common.model.dataVersion
import cn.wj.android.cashbook.core.common.model.updateVersion
import cn.wj.android.cashbook.core.data.repository.SettingRepository
import cn.wj.android.cashbook.core.data.uitl.BackupRecoveryManager
import cn.wj.android.cashbook.core.data.uitl.BackupRecoveryState
import cn.wj.android.cashbook.core.data.uitl.BackupRecoveryState.None
import cn.wj.android.cashbook.core.network.util.OkHttpWebDAVHandler
import cn.wj.android.cashbook.sync.initializers.BackupWorkName
import cn.wj.android.cashbook.sync.workers.BackupWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import java.net.URL
import java.net.URLEncoder
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * 备份恢复管理
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/7/21
 */
class WorkManagerBackupRecoveryManager @Inject constructor(
    private val settingRepository: SettingRepository,
    private val okHttpWebDAVHandler: OkHttpWebDAVHandler,
    @ApplicationContext private val context: Context,
    @Dispatcher(CashbookDispatchers.IO) private val coroutineContext: CoroutineContext,
) : BackupRecoveryManager {

    private val dataVersion = dataVersion()

    override val isWebDAVConnected: Flow<Boolean> =
        combine(dataVersion, settingRepository.appDataMode) { _, _ ->
            refreshConnectedStatus()
        }

    override val backupState: MutableStateFlow<BackupRecoveryState> = MutableStateFlow(None)

    override val recoveryState: MutableStateFlow<BackupRecoveryState> = MutableStateFlow(None)

    override fun refreshWebDAVConnected() {
        dataVersion.updateVersion()
    }

    override suspend fun upload(backupFileUri: Uri): Boolean = withContext(coroutineContext) {
        this@WorkManagerBackupRecoveryManager.logger()
            .i("upload(backupFileUri = <$backupFileUri>)")
        withCredentials { root ->
            if (backupFileUri.scheme == "content") {
                val backupFileName = DocumentFile.fromSingleUri(context, backupFileUri)?.name
                    ?: throw RuntimeException("get file name failed")
                context.contentResolver.openInputStream(backupFileUri)!!.use { bis ->
                    put(root + backupFileName, bis, "application/octet-stream")
                }
            } else {
                val backupFile = backupFileUri.toFile()
                put(root + backupFile.name, backupFile, "application/octet-stream")
            }
        }
    }

    override suspend fun requestBackup(): Unit = withContext(coroutineContext) {
        updateBackupState(BackupRecoveryState.InProgress)
        val appDataModel = settingRepository.appDataMode.first()
        val backupPath = appDataModel.backupPath
        logger().i("requestBackup(), backupPath = <$backupPath>")
        if (backupPath.isBlank()) {
            logger().i("requestBackup(), blank backupPath")
            updateBackupState(BackupRecoveryState.Failed(BackupRecoveryState.FAILED_BLANK_BACKUP_PATH))
        } else {
            if (grantedPermissions(backupPath)) {
                // 有权限，开始备份
                logger().i("requestBackup(), startBackup")
                val workManager = WorkManager.getInstance(context)
                workManager.enqueueUniqueWork(
                    BackupWorkName,
                    ExistingWorkPolicy.KEEP,
                    BackupWorker.startUpBackupWork(backupPath),
                )
            } else {
                logger().i("requestBackup(), unauthorized")
                updateBackupState(BackupRecoveryState.Failed(BackupRecoveryState.FAILED_BACKUP_PATH_UNAUTHORIZED))
            }
        }
    }

    override fun updateBackupState(state: BackupRecoveryState) {
        backupState.tryEmit(state)
    }

    override suspend fun requestRecovery(onlyLocal: Boolean) {

    }

    override fun updateRecoveryState(state: BackupRecoveryState) {

    }

    private suspend fun refreshConnectedStatus(): Boolean = withContext(coroutineContext) {
        withCredentials { root ->
            exists(root) || createDirectory(root)
        }
    }

    private fun grantedPermissions(backupPath: String): Boolean {
        return if (backupPath.startsWith("content://")) {
            DocumentFile.fromTreeUri(context, Uri.parse(backupPath))?.canRead() == true
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
                    && ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private val String.backupPath: String
        get() {
            val url = URL("$this${if (this.endsWith("/")) "" else "/"}$BACKUP_DIR_NAME/")
            val raw = url.toString().replace("davs://", "https://").replace("dav://", "http://")
            return URLEncoder.encode(raw, "UTF-8")
                .replace("\\+".toRegex(), "%20")
                .replace("%3A".toRegex(), ":")
                .replace("%2F".toRegex(), "/")
        }

    private suspend fun <T> withCredentials(
        block: suspend OkHttpWebDAVHandler.(String) -> T
    ): T = withContext(coroutineContext) {
        val appDataMode = settingRepository.appDataMode.first()
        okHttpWebDAVHandler.setCredentials(appDataMode.webDAVAccount, appDataMode.webDAVPassword)
        okHttpWebDAVHandler.block(appDataMode.webDAVDomain.backupPath)
    }

}