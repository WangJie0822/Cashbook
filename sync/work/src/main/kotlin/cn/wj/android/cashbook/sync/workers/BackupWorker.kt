package cn.wj.android.cashbook.sync.workers

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkerParameters
import cn.wj.android.cashbook.core.common.ApplicationInfo
import cn.wj.android.cashbook.core.common.BACKUP_CACHE_FILE_NAME
import cn.wj.android.cashbook.core.common.BACKUP_FILE_EXT
import cn.wj.android.cashbook.core.common.BACKUP_FILE_NAME
import cn.wj.android.cashbook.core.common.DB_FILE_NAME
import cn.wj.android.cashbook.core.common.annotation.CashbookDispatchers
import cn.wj.android.cashbook.core.common.annotation.Dispatcher
import cn.wj.android.cashbook.core.common.ext.deleteAllFiles
import cn.wj.android.cashbook.core.common.ext.logger
import cn.wj.android.cashbook.core.common.tools.DATE_FORMAT_BACKUP
import cn.wj.android.cashbook.core.common.tools.dateFormat
import cn.wj.android.cashbook.core.data.repository.SettingRepository
import cn.wj.android.cashbook.core.data.uitl.BackupRecoveryManager
import cn.wj.android.cashbook.core.data.uitl.BackupRecoveryState
import cn.wj.android.cashbook.core.data.uitl.BackupRecoveryState.Companion.FAILED_BACKUP_PATH_UNAUTHORIZED
import cn.wj.android.cashbook.core.data.uitl.BackupRecoveryState.Companion.FAILED_BACKUP_WEBDAV
import cn.wj.android.cashbook.core.data.uitl.BackupRecoveryState.Companion.FAILED_BLANK_BACKUP_PATH
import cn.wj.android.cashbook.core.data.uitl.BackupRecoveryState.Companion.SUCCESS_BACKUP
import cn.wj.android.cashbook.sync.initializers.BackupRecoveryConstraints
import cn.wj.android.cashbook.sync.initializers.syncForegroundInfo
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * 数据备份 Worker
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/7/18
 */
@HiltWorker
class BackupWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val settingRepository: SettingRepository,
    private val backupRecoveryManager: BackupRecoveryManager,
    @Dispatcher(CashbookDispatchers.IO) private val ioDispatcher: CoroutineContext,
) : CoroutineWorker(appContext, workerParams) {

    private val backupPath = workerParams.inputData.getString(DATA_KEY_PATH).orEmpty()

    override suspend fun getForegroundInfo(): ForegroundInfo =
        appContext.syncForegroundInfo()

    override suspend fun doWork(): Result = withContext(ioDispatcher) {
        if (backupPath.isBlank()) {
            this@BackupWorker.logger()
                .i("doWork(), backupPath is blank")
            backupRecoveryManager.updateBackupState(
                BackupRecoveryState.Failed(
                    FAILED_BLANK_BACKUP_PATH
                )
            )
            return@withContext Result.failure()
        }

        // 备份到本地
        val result = runCatching {
            val databaseFile = appContext.getDatabasePath(DB_FILE_NAME)
            val currentMs = System.currentTimeMillis()
            val dateFormat = currentMs.dateFormat(DATE_FORMAT_BACKUP)
            this@BackupWorker.logger()
                .i("doWork(), backupPath = <$backupPath>, databaseFile = <$databaseFile>, dateFormat = <$dateFormat>")
            // 备份缓存目录
            val backupCacheDir = File(appContext.cacheDir, BACKUP_CACHE_FILE_NAME)
            backupCacheDir.deleteAllFiles()
            backupCacheDir.mkdirs()
            // 备份缓存文件
            val databaseCacheFile = File(backupCacheDir, DB_FILE_NAME)
            databaseFile.copyTo(databaseCacheFile)
            // 压缩备份文件
            val zippedPath =
                backupCacheDir.absolutePath + File.separator + BACKUP_FILE_NAME + dateFormat + BACKUP_FILE_EXT
            ZipOutputStream(FileOutputStream(zippedPath)).use { zos ->
                BufferedInputStream(FileInputStream(databaseCacheFile)).use { bis ->
                    val entry = ZipEntry(databaseCacheFile.name)
                    entry.comment = ApplicationInfo.infos
                    zos.putNextEntry(entry)
                    zos.write(bis.readBytes())
                    zos.closeEntry()
                }
            }
            // 将备份文件复制到备份路径
            val zippedFile = File(zippedPath)
            if (!zippedFile.exists()) {
                return@runCatching FAILED_BACKUP_PATH_UNAUTHORIZED
            }
            val zippedFileName = zippedFile.name
            val backupFileUri = if (backupPath.startsWith("content://")) {
                val documentFile = DocumentFile.fromTreeUri(appContext, Uri.parse(backupPath))
                    ?: return@runCatching FAILED_BACKUP_PATH_UNAUTHORIZED
                documentFile.findFile(zippedFileName)?.delete()
                val backupFile = documentFile.createFile("application/zip", zippedFileName)
                    ?: return@runCatching FAILED_BACKUP_PATH_UNAUTHORIZED
                appContext.contentResolver.openOutputStream(backupFile.uri)?.use {
                    it.write(zippedFile.readBytes())
                }
                backupFile.uri
            } else {
                val backupFile = File(backupPath, zippedFileName)
                if (!backupFile.exists()) {
                    backupFile.createNewFile()
                }
                zippedFile.copyTo(backupFile)
                backupFile.toUri()
            }

            // 更新备份时间
            settingRepository.updateBackupMs(currentMs)

            val innerResult = if (backupRecoveryManager.isWebDAVConnected.first()) {
                // WebDAV 已连接，上传备份文件到云端
                runCatching {
                    this@BackupWorker.logger().i("doWork(), upload to WebDAV")
                    if (backupRecoveryManager.upload(backupFileUri)) {
                        SUCCESS_BACKUP
                    } else {
                        FAILED_BACKUP_WEBDAV
                    }
                }.getOrElse { throwable ->
                    this@BackupWorker.logger().e(throwable, "doWork(), upload")
                    FAILED_BACKUP_WEBDAV
                }
            } else {
                SUCCESS_BACKUP
            }

            // 删除缓存文件
            backupCacheDir.deleteAllFiles()

            innerResult
        }.getOrElse { throwable ->
            this@BackupWorker.logger().e(throwable, "doWork(), backup")
            FAILED_BACKUP_PATH_UNAUTHORIZED
        }
        val state = if (result == SUCCESS_BACKUP) {
            BackupRecoveryState.Success(SUCCESS_BACKUP)
        } else {
            BackupRecoveryState.Failed(result)
        }
        backupRecoveryManager.updateBackupState(state)
        this@BackupWorker.logger().i("doWork(), result = <$result>")
        if (result == SUCCESS_BACKUP) {
            Result.success()
        } else {
            Result.failure()
        }
    }

    companion object {

        private const val DATA_KEY_PATH = "data_key_path"

        /** 使用代理任务启动同步任务，以支持依赖注入 */
        fun startUpBackupWork(path: String) = OneTimeWorkRequestBuilder<DelegatingWorker>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .setConstraints(BackupRecoveryConstraints)
            .setInputData(
                BackupWorker::class.delegatedData(
                    data = Data.Builder().putString(DATA_KEY_PATH, path).build()
                )
            )
            .build()
    }
}