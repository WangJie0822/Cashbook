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
import cn.wj.android.cashbook.core.common.BACKUP_CACHE_FILE_NAME
import cn.wj.android.cashbook.core.common.BACKUP_DIR_NAME
import cn.wj.android.cashbook.core.common.BACKUP_FILE_EXT
import cn.wj.android.cashbook.core.common.BACKUP_FILE_NAME
import cn.wj.android.cashbook.core.common.annotation.CashbookDispatchers
import cn.wj.android.cashbook.core.common.annotation.Dispatcher
import cn.wj.android.cashbook.core.common.ext.deleteAllFiles
import cn.wj.android.cashbook.core.common.ext.logger
import cn.wj.android.cashbook.core.common.model.dataVersion
import cn.wj.android.cashbook.core.common.model.updateVersion
import cn.wj.android.cashbook.core.data.repository.SettingRepository
import cn.wj.android.cashbook.core.data.uitl.BackupRecoveryManager
import cn.wj.android.cashbook.core.data.uitl.BackupRecoveryState
import cn.wj.android.cashbook.core.data.uitl.BackupRecoveryState.None
import cn.wj.android.cashbook.core.model.model.BackupModel
import cn.wj.android.cashbook.core.network.util.OkHttpWebDAVHandler
import cn.wj.android.cashbook.sync.initializers.BackupWorkName
import cn.wj.android.cashbook.sync.workers.BackupWorker
import cn.wj.android.cashbook.sync.workers.RecoveryWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileFilter
import java.net.URL
import java.net.URLEncoder
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapLatest
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

    private val connectedDataVersion = dataVersion()
    private val backupDataVersion = dataVersion()

    override val isWebDAVConnected: Flow<Boolean> =
        combine(connectedDataVersion, settingRepository.appDataMode) { _, _ ->
            refreshConnectedStatus()
        }

    override val backupState: StateFlow<BackupRecoveryState> = _backupState

    override val recoveryState: StateFlow<BackupRecoveryState> = _recoveryState

    override val onlineBackupListData = combine(connectedDataVersion, backupDataVersion) { _, _ ->
        val appDataMode = settingRepository.appDataMode.first()
        okHttpWebDAVHandler.list(appDataMode.webDAVDomain.backupPath)
    }

    override val localBackupListData = settingRepository.appDataMode.mapLatest { appDataModel ->
        getLocalBackupList(appDataModel.backupPath)
    }

    private val customPath = MutableStateFlow("")

    override val localCustomBackupListData: Flow<List<BackupModel>> = customPath.mapLatest {
        getLocalBackupList(it)
    }

    override fun refreshWebDAVConnected() {
        connectedDataVersion.updateVersion()
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
        _backupState.tryEmit(state)
    }

    override fun refreshLocalPath(localPath: String) {
        customPath.tryEmit(localPath)
    }

    override suspend fun requestRecovery(path: String): Unit = withContext(coroutineContext) {
        updateRecoveryState(BackupRecoveryState.InProgress)
        logger().i("requestRecovery(), path = <$path>")
        when {
            path.isBlank() -> {
                logger().i("requestRecovery(), blank path")
                updateBackupState(BackupRecoveryState.Failed(BackupRecoveryState.FAILED_BLANK_BACKUP_PATH))
            }

            path.startsWith("https://") || path.startsWith("http://") || grantedPermissions(path) -> {
                logger().i("requestRecovery(), startBackup")
                val workManager = WorkManager.getInstance(context)
                workManager.enqueueUniqueWork(
                    BackupWorkName,
                    ExistingWorkPolicy.KEEP,
                    RecoveryWorker.startUpBackupWork(path),
                )
            }

            else -> {
                logger().i("requestRecovery(), unauthorized")
                updateBackupState(BackupRecoveryState.Failed(BackupRecoveryState.FAILED_BACKUP_PATH_UNAUTHORIZED))
            }
        }
    }

    override fun updateRecoveryState(state: BackupRecoveryState) {
        _recoveryState.tryEmit(state)
    }

    override suspend fun getWebFile(url: String): String = withContext(coroutineContext) {
        // 获取文件名
        val fileName = url.split("/").last()
        if (!fileName.startsWith(BACKUP_FILE_NAME) || !fileName.endsWith(BACKUP_FILE_EXT)) {
            // 不满足格式
            return@withContext ""
        }
        // 创建本地缓存文件
        val cacheDir = File(context.cacheDir, BACKUP_CACHE_FILE_NAME)
        cacheDir.deleteAllFiles()
        cacheDir.mkdirs()
        val backupCacheFile = File(cacheDir, fileName)
        backupCacheFile.createNewFile()

        withCredentials {
            val inputStream = get(url) ?: return@withCredentials ""
            backupCacheFile.writeBytes(inputStream.readBytes())
            backupCacheFile.path
        }
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

    private suspend fun getLocalBackupList(
        path: String
    ): List<BackupModel> = withContext(coroutineContext) {
        if (!grantedPermissions(path)) {
            return@withContext emptyList()
        }
        val result = arrayListOf<BackupModel>()
        if (path.startsWith("content://")) {
            DocumentFile.fromTreeUri(context, Uri.parse(path))?.let { df ->
                if (df.name == BACKUP_DIR_NAME || null == df.findFile(BACKUP_DIR_NAME)) {
                    // 备份路径为指定名称或路径下找不到指定名称，使用当前路径
                    df
                } else {
                    df.findFile(BACKUP_DIR_NAME) ?: df
                }.listFiles().forEach {
                    it.name?.let { name ->
                        if (name.startsWith(BACKUP_FILE_NAME) && name.endsWith(BACKUP_FILE_EXT)) {
                            result.add(BackupModel(name, it.uri.toString()))
                        }
                    }
                }
            }
        } else {
            val file = File(path)
            if (file.exists()) {
                if (file.name == BACKUP_DIR_NAME) {
                    file
                } else {
                    file.listFiles(FileFilter { it.name == BACKUP_DIR_NAME })?.firstOrNull() ?: file
                }.listFiles(FileFilter {
                    it.name.startsWith(BACKUP_FILE_NAME) && it.name.endsWith(BACKUP_FILE_EXT)
                })?.forEach {
                    result.add(BackupModel(it.name, it.path))
                }
            }
        }
        result
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

private val _backupState: MutableStateFlow<BackupRecoveryState> = MutableStateFlow(None)
private val _recoveryState: MutableStateFlow<BackupRecoveryState> = MutableStateFlow(None)