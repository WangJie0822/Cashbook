package cn.wj.android.cashbook.sync.workers

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkerParameters
import cn.wj.android.cashbook.core.common.BACKUP_CACHE_FILE_NAME
import cn.wj.android.cashbook.core.common.BACKUP_FILE_EXT
import cn.wj.android.cashbook.core.common.BACKUP_FILE_NAME
import cn.wj.android.cashbook.core.common.DB_FILE_NAME
import cn.wj.android.cashbook.core.common.annotation.CashbookDispatchers
import cn.wj.android.cashbook.core.common.annotation.Dispatcher
import cn.wj.android.cashbook.core.common.ext.deleteAllFiles
import cn.wj.android.cashbook.core.common.ext.logger
import cn.wj.android.cashbook.core.data.uitl.BackupRecoveryManager
import cn.wj.android.cashbook.core.data.uitl.BackupRecoveryState
import cn.wj.android.cashbook.core.data.uitl.BackupRecoveryState.Companion.FAILED_BACKUP_PATH_UNAUTHORIZED
import cn.wj.android.cashbook.core.data.uitl.BackupRecoveryState.Companion.FAILED_BLANK_BACKUP_PATH
import cn.wj.android.cashbook.core.data.uitl.BackupRecoveryState.Companion.FAILED_FILE_FORMAT_ERROR
import cn.wj.android.cashbook.core.data.uitl.BackupRecoveryState.Companion.SUCCESS_RECOVERY
import cn.wj.android.cashbook.core.database.DatabaseMigrations
import cn.wj.android.cashbook.core.database.util.DelegateSQLiteDatabase
import cn.wj.android.cashbook.sync.initializers.BackupRecoveryConstraints
import cn.wj.android.cashbook.sync.initializers.syncForegroundInfo
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.withContext

/**
 * 数据恢复 Worker
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/7/25
 */
@HiltWorker
class RecoveryWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val backupRecoveryManager: BackupRecoveryManager,
    @Dispatcher(CashbookDispatchers.IO) private val ioDispatcher: CoroutineContext,
) : CoroutineWorker(appContext, workerParams) {

    private val path = workerParams.inputData.getString(DATA_KEY_PATH).orEmpty()

    override suspend fun getForegroundInfo(): ForegroundInfo =
        appContext.syncForegroundInfo()

    override suspend fun doWork(): Result = withContext(ioDispatcher) {
        if (path.isBlank()) {
            this@RecoveryWorker.logger()
                .i("doWork(), backupPath is blank")
            backupRecoveryManager.updateRecoveryState(
                BackupRecoveryState.Failed(
                    FAILED_BLANK_BACKUP_PATH
                )
            )
            return@withContext Result.failure()
        }

        // 恢复缓存路径
        val cacheDir = File(appContext.cacheDir, BACKUP_CACHE_FILE_NAME)
        cacheDir.deleteAllFiles()
        cacheDir.mkdirs()

        val result = runCatching {
            val localPath = if (path.startsWith("https://") || path.startsWith("http://")) {
                // 云端备份，下载
                backupRecoveryManager.getWebFile(path)
            } else {
                path
            }

            this@RecoveryWorker.logger().i("doWork(), localPath = <$localPath>")

            val backupZippedCacheFile: File
            if (localPath.startsWith("content://")) {
                DocumentFile.fromSingleUri(appContext, Uri.parse(localPath))!!.let { df ->
                    val name = df.name ?: return@runCatching FAILED_BACKUP_PATH_UNAUTHORIZED
                    if (name.startsWith(BACKUP_FILE_NAME) && name.endsWith(BACKUP_FILE_EXT)) {
                        backupZippedCacheFile = File(cacheDir, name)
                        if (!backupZippedCacheFile.exists()) {
                            backupZippedCacheFile.createNewFile()
                        }
                        appContext.contentResolver.openInputStream(Uri.parse(localPath))!!.use {
                            backupZippedCacheFile.writeBytes(it.readBytes())
                        }
                    } else {
                        return@runCatching FAILED_BACKUP_PATH_UNAUTHORIZED
                    }
                }
            } else {
                val localFile = File(localPath)
                backupZippedCacheFile = File(cacheDir, localFile.name)
                if (localFile.absolutePath != backupZippedCacheFile.absolutePath) {
                    // 非相同文件，需要复制
                    if (!backupZippedCacheFile.exists()) {
                        backupZippedCacheFile.createNewFile()
                    }
                    localFile.copyTo(backupZippedCacheFile)
                }
            }

            this@RecoveryWorker.logger()
                .i("doWork(), backupZippedCacheFile = <$backupZippedCacheFile>")
            val destFiles = arrayListOf<File>()
            // 解压缩
            ZipFile(backupZippedCacheFile.absolutePath).use { zipFile ->
                val entries = zipFile.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement() as ZipEntry
                    val comment = entry.comment
                    this@RecoveryWorker.logger().i("doWork(), comment = <$comment>")
                    if (comment.isNullOrBlank()) {
                        continue
                    }
                    val entryName = entry.name
                    if (entry.isDirectory || entryName.contains("../")) {
                        continue
                    }
                    val destFile = File(cacheDir, entryName)
                    destFile.deleteAllFiles()
                    if (!destFile.exists()) {
                        destFile.createNewFile()
                    }
                    BufferedInputStream(zipFile.getInputStream(entry)).use { bis ->
                        BufferedOutputStream(FileOutputStream(destFile)).use { bos ->
                            bos.write(bis.readBytes())
                        }
                    }
                    destFiles.add(destFile)
                }
            }

            val dbFile = destFiles.firstOrNull { it.name == DB_FILE_NAME }
                ?: return@runCatching FAILED_FILE_FORMAT_ERROR

            val backupDatabase = DelegateSQLiteDatabase(
                appContext.openOrCreateDatabase(
                    dbFile.absolutePath,
                    Context.MODE_PRIVATE,
                    null
                )
            )
            val currentDatabase = DelegateSQLiteDatabase(
                appContext.openOrCreateDatabase(
                    DB_FILE_NAME,
                    Context.MODE_PRIVATE,
                    null
                )
            )
            if (DatabaseMigrations.recoveryFromDb(backupDatabase, currentDatabase)) {
                SUCCESS_RECOVERY
            } else {
                FAILED_BACKUP_PATH_UNAUTHORIZED
            }
        }.getOrElse { throwable ->
            this@RecoveryWorker.logger().e(throwable, "doWork(), backup")
            FAILED_BACKUP_PATH_UNAUTHORIZED
        }

        this@RecoveryWorker.logger().i("doWork(), result = <$result>")

        // 删除缓存路径
        cacheDir.deleteAllFiles()

        if (result == SUCCESS_RECOVERY) {
            backupRecoveryManager.updateRecoveryState(BackupRecoveryState.Success(SUCCESS_RECOVERY))
            Result.success()
        } else {
            backupRecoveryManager.updateRecoveryState(BackupRecoveryState.Failed(result))
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
                RecoveryWorker::class.delegatedData(
                    data = Data.Builder().putString(DATA_KEY_PATH, path).build()
                )
            )
            .build()
    }
}