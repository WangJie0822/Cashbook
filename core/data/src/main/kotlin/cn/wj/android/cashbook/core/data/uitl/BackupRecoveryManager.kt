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

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.core.net.toFile
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import cn.wj.android.cashbook.core.common.ApplicationInfo
import cn.wj.android.cashbook.core.common.BACKUP_CACHE_FILE_NAME
import cn.wj.android.cashbook.core.common.BACKUP_DIR_NAME
import cn.wj.android.cashbook.core.common.BACKUP_FILE_EXT
import cn.wj.android.cashbook.core.common.BACKUP_FILE_NAME
import cn.wj.android.cashbook.core.common.DB_FILE_NAME
import cn.wj.android.cashbook.core.common.annotation.CashbookDispatchers
import cn.wj.android.cashbook.core.common.annotation.Dispatcher
import cn.wj.android.cashbook.core.common.ext.SCHEME_DAV
import cn.wj.android.cashbook.core.common.ext.SCHEME_DAVS
import cn.wj.android.cashbook.core.common.ext.SCHEME_HTTP
import cn.wj.android.cashbook.core.common.ext.SCHEME_HTTPS
import cn.wj.android.cashbook.core.common.ext.deleteAllFiles
import cn.wj.android.cashbook.core.common.ext.isContentUri
import cn.wj.android.cashbook.core.common.ext.isWebUri
import cn.wj.android.cashbook.core.common.ext.logger
import cn.wj.android.cashbook.core.common.model.dataVersion
import cn.wj.android.cashbook.core.common.model.updateVersion
import cn.wj.android.cashbook.core.common.tools.DATE_FORMAT_BACKUP
import cn.wj.android.cashbook.core.common.tools.dateFormat
import cn.wj.android.cashbook.core.data.repository.SettingRepository
import cn.wj.android.cashbook.core.data.uitl.BackupRecoveryState.Companion.FAILED_BACKUP_PATH_EMPTY
import cn.wj.android.cashbook.core.database.CashbookDatabase
import cn.wj.android.cashbook.core.database.migration.DatabaseMigrations
import cn.wj.android.cashbook.core.database.util.DelegateSQLiteDatabase
import cn.wj.android.cashbook.core.model.model.BackupModel
import cn.wj.android.cashbook.core.network.util.WebDAVHandler
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileFilter
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.URL
import java.net.URLEncoder
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

/**
 * 备份恢复管理
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/7/21
 */
class BackupRecoveryManager @Inject constructor(
    networkMonitor: NetworkMonitor,
    private val settingRepository: SettingRepository,
    private val webDAVHandler: WebDAVHandler,
    private val database: CashbookDatabase,
    @ApplicationContext private val context: Context,
    @Dispatcher(CashbookDispatchers.IO) private val ioCoroutineContext: CoroutineContext,
) {

    private val connectedDataVersion = dataVersion()
    private val backupDataVersion = dataVersion()

    private val _backupState: MutableStateFlow<BackupRecoveryState> = MutableStateFlow(
        BackupRecoveryState.None,
    )
    private val _recoveryState: MutableStateFlow<BackupRecoveryState> = MutableStateFlow(
        BackupRecoveryState.None,
    )

    val isWebDAVConnected: Flow<Boolean> =
        combine(
            networkMonitor.isOnline,
            connectedDataVersion,
            settingRepository.appDataMode,
        ) { isOnline, _, _ ->
            isOnline && refreshConnectedStatus()
        }

    val backupState: Flow<BackupRecoveryState> = _backupState

    val recoveryState: Flow<BackupRecoveryState> = _recoveryState

    private val onlineBackupListData = combine(connectedDataVersion, backupDataVersion) { _, _ ->
        val appDataMode = settingRepository.appDataMode.first()
        webDAVHandler.list(appDataMode.webDAVDomain.backupPath)
    }

    private val localBackupListData = settingRepository.appDataMode.mapLatest { appDataModel ->
        getLocalBackupList(appDataModel.backupPath)
    }

    fun refreshWebDAVConnected() {
        connectedDataVersion.updateVersion()
    }

    private suspend fun upload(backupFileUri: Uri): Boolean = withContext(ioCoroutineContext) {
        this@BackupRecoveryManager.logger()
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

    suspend fun requestBackup(): Unit = withContext(ioCoroutineContext) {
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
                startBackup(backupPath)
            } else {
                logger().i("requestBackup(), unauthorized")
                updateBackupState(BackupRecoveryState.Failed(BackupRecoveryState.FAILED_BACKUP_PATH_UNAUTHORIZED))
            }
        }
    }

    fun updateBackupState(state: BackupRecoveryState) {
        _backupState.tryEmit(state)
    }

    suspend fun requestRecovery(path: String): Unit = withContext(ioCoroutineContext) {
        updateRecoveryState(BackupRecoveryState.InProgress)
        logger().i("requestRecovery(), path = <$path>")
        when {
            path.isBlank() -> {
                logger().i("requestRecovery(), blank path")
                updateBackupState(BackupRecoveryState.Failed(BackupRecoveryState.FAILED_BLANK_BACKUP_PATH))
            }

            path.isWebUri || grantedPermissions(path) -> {
                logger().i("requestRecovery(), startBackup")
                startRecovery(path)
            }

            else -> {
                logger().i("requestRecovery(), unauthorized")
                updateBackupState(BackupRecoveryState.Failed(BackupRecoveryState.FAILED_BACKUP_PATH_UNAUTHORIZED))
            }
        }
    }

    fun updateRecoveryState(state: BackupRecoveryState) {
        _recoveryState.tryEmit(state)
    }

    private suspend fun getWebFile(url: String): String = withContext(ioCoroutineContext) {
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

    private suspend fun refreshConnectedStatus(): Boolean = withContext(ioCoroutineContext) {
        withCredentials { root ->
            exists(root) || createDirectory(root)
        }
    }

    private fun grantedPermissions(backupPath: String): Boolean {
        return if (backupPath.isContentUri) {
            DocumentFile.fromTreeUri(context, backupPath.toUri())?.canRead() == true
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE,
            ) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private suspend fun getLocalBackupList(
        path: String,
    ): List<BackupModel> = withContext(ioCoroutineContext) {
        if (!grantedPermissions(path)) {
            return@withContext emptyList()
        }
        val result = arrayListOf<BackupModel>()
        if (path.isContentUri) {
            DocumentFile.fromTreeUri(context, path.toUri())?.let { df ->
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
                }.listFiles(
                    FileFilter {
                        it.name.startsWith(BACKUP_FILE_NAME) && it.name.endsWith(BACKUP_FILE_EXT)
                    },
                )?.forEach {
                    result.add(BackupModel(it.name, it.path))
                }
            }
        }
        result
    }

    suspend fun getRecoveryList(
        onlyLocal: Boolean,
        localPath: String,
    ): List<BackupModel> = withContext(ioCoroutineContext) {
        val list = if (!onlyLocal && isWebDAVConnected.first()) {
            // 使用云端数据
            val webDAVDomain = settingRepository.appDataMode.first().webDAVDomain
            onlineBackupListData.first().map {
                // 云端路径不包含服务器地址，完善地址
                if (!it.path.startsWith(webDAVDomain)) {
                    it.copy(path = webDAVDomain.fixedDavDomain + it.path.fixedDavPath)
                } else {
                    it
                }
            }
        } else {
            if (localPath.isNotBlank()) {
                getLocalBackupList(localPath)
            } else {
                localBackupListData.first()
            }
        }
        if (list.isEmpty()) {
            updateRecoveryState(BackupRecoveryState.Failed(FAILED_BACKUP_PATH_EMPTY))
        }
        list.sortedBy { it.name }.reversed()
    }

    /** 修复 dav 路径，坚果云新版本路径中会添加 /dav */
    private val String.fixedDavDomain: String
        get() = if (this.endsWith("/")) {
            this.dropLast(1)
        } else {
            this
        }

    /** 修复 dav 路径，坚果云新版本路径中会添加 /dav */
    private val String.fixedDavPath: String
        get() = if (this.startsWith("/dav")) {
            this.drop(4)
        } else {
            this
        }

    private val String.backupPath: String
        get() = runCatching {
            val url = URL("$this${if (this.endsWith("/")) "" else "/"}$BACKUP_DIR_NAME/")
            val raw =
                url.toString().replace(SCHEME_DAVS, SCHEME_HTTPS).replace(SCHEME_DAV, SCHEME_HTTP)
            URLEncoder.encode(raw, "UTF-8")
                .replace("\\+".toRegex(), "%20")
                .replace("%3A".toRegex(), ":")
                .replace("%2F".toRegex(), "/")
        }
            .getOrElse { throwable ->
                this@BackupRecoveryManager.logger().e(throwable, "backupPath")
                ""
            }

    private suspend fun <T> withCredentials(
        block: suspend WebDAVHandler.(String) -> T,
    ): T = withContext(ioCoroutineContext) {
        val appDataMode = settingRepository.appDataMode.first()
        webDAVHandler.setCredentials(appDataMode.webDAVAccount, appDataMode.webDAVPassword)
        webDAVHandler.block(appDataMode.webDAVDomain.backupPath)
    }

    private suspend fun startBackup(backupPath: String) {
        if (backupPath.isBlank()) {
            this@BackupRecoveryManager.logger().i("startBackup(), backupPath is blank")
            updateBackupState(BackupRecoveryState.Failed(BackupRecoveryState.FAILED_BLANK_BACKUP_PATH))
            return
        }

        // 备份缓存目录
        val backupCacheDir = File(context.cacheDir, BACKUP_CACHE_FILE_NAME)
        backupCacheDir.deleteAllFiles()
        backupCacheDir.mkdirs()

        // 备份到本地
        val result = runCatching {
            val databaseFile = context.getDatabasePath(DB_FILE_NAME)
            val currentMs = System.currentTimeMillis()
            val dateFormat = currentMs.dateFormat(DATE_FORMAT_BACKUP)

            // 备份缓存文件
            val databaseCacheFile = File(backupCacheDir, DB_FILE_NAME)

            this@BackupRecoveryManager.logger()
                .i("startBackup(), backupPath = <$backupPath>, databaseFile = <$databaseFile>, databaseCacheFile = <$databaseCacheFile>, dateFormat = <$dateFormat>")

            // 复制数据库文件到缓存路径
            database.close()
            databaseFile.copyTo(databaseCacheFile)
            // 压缩备份文件
            val zippedPath =
                backupCacheDir.absolutePath + File.separator + BACKUP_FILE_NAME + dateFormat + BACKUP_FILE_EXT
            ZipOutputStream(FileOutputStream(zippedPath)).use { zos ->
                BufferedInputStream(FileInputStream(databaseCacheFile)).use { bis ->
                    val entry = ZipEntry(databaseCacheFile.name)
                    entry.comment = ApplicationInfo.applicationInfo
                    zos.putNextEntry(entry)
                    zos.write(bis.readBytes())
                    zos.closeEntry()
                }
            }
            // 将备份文件复制到备份路径
            val zippedFile = File(zippedPath)
            if (!zippedFile.exists()) {
                return@runCatching BackupRecoveryState.FAILED_BACKUP_PATH_UNAUTHORIZED
            }
            val zippedFileName = zippedFile.name
            val keepLatest = settingRepository.appDataMode.first().keepLatestBackup
            val backupFileUri = if (backupPath.startsWith("content://")) {
                val documentFile = DocumentFile.fromTreeUri(context, backupPath.toUri())
                    ?: return@runCatching BackupRecoveryState.FAILED_BACKUP_PATH_UNAUTHORIZED
                documentFile.findFile(zippedFileName)?.delete()
                if (keepLatest) {
                    documentFile.listFiles()
                        .filter {
                            // 仅删除备份文件
                            val name = it.name.orEmpty()
                            name.contains(BACKUP_FILE_NAME) && name.endsWith(BACKUP_FILE_EXT)
                        }
                        .forEach { it.delete() }
                }
                val backupFile = documentFile.createFile("application/zip", zippedFileName)
                    ?: return@runCatching BackupRecoveryState.FAILED_BACKUP_PATH_UNAUTHORIZED
                context.contentResolver.openOutputStream(backupFile.uri)?.use {
                    it.write(zippedFile.readBytes())
                }
                backupFile.uri
            } else {
                val backupDir = File(backupPath)
                if (keepLatest) {
                    backupDir.listFiles { _, name ->
                        // 仅删除备份文件
                        name.contains(BACKUP_FILE_NAME) && name.endsWith(BACKUP_FILE_EXT)
                    }?.forEach { it.delete() }
                }
                if (!backupDir.exists()) {
                    backupDir.mkdirs()
                }
                val backupFile = File(backupDir, zippedFileName)
                if (!backupFile.exists()) {
                    backupFile.createNewFile()
                }
                zippedFile.copyTo(backupFile)
                backupFile.toUri()
            }

            // 更新备份时间
            settingRepository.updateBackupMs(currentMs)

            val innerResult = if (isWebDAVConnected.first()) {
                // WebDAV 已连接，上传备份文件到云端
                runCatching {
                    this@BackupRecoveryManager.logger()
                        .i("startBackup(), upload to WebDAV")
                    if (upload(backupFileUri)) {
                        BackupRecoveryState.SUCCESS_BACKUP
                    } else {
                        BackupRecoveryState.FAILED_BACKUP_WEBDAV
                    }
                }.getOrElse { throwable ->
                    this@BackupRecoveryManager.logger()
                        .e(throwable, "startBackup(), upload")
                    BackupRecoveryState.FAILED_BACKUP_WEBDAV
                }
            } else {
                BackupRecoveryState.SUCCESS_BACKUP
            }

            innerResult
        }.getOrElse { throwable ->
            this@BackupRecoveryManager.logger().e(throwable, "startBackup(), backup")
            BackupRecoveryState.FAILED_BACKUP_PATH_UNAUTHORIZED
        }

        if (!ApplicationInfo.isDev) {
            // 删除缓存文件
            backupCacheDir.deleteAllFiles()
        }

        val state = if (result == BackupRecoveryState.SUCCESS_BACKUP) {
            BackupRecoveryState.Success(BackupRecoveryState.SUCCESS_BACKUP)
        } else {
            BackupRecoveryState.Failed(result)
        }
        updateBackupState(state)
        this@BackupRecoveryManager.logger().i("startBackup(), result = <$result>")
    }

    private suspend fun startRecovery(path: String) {
        if (path.isBlank()) {
            this@BackupRecoveryManager.logger()
                .i("startRecovery(), backupPath is blank")
            updateRecoveryState(
                BackupRecoveryState.Failed(
                    BackupRecoveryState.FAILED_BLANK_BACKUP_PATH,
                ),
            )
            return
        }

        // 恢复缓存路径
        val cacheDir = File(context.cacheDir, BACKUP_CACHE_FILE_NAME)
        cacheDir.deleteAllFiles()
        cacheDir.mkdirs()

        val result = runCatching {
            val localPath = if (path.startsWith("https://") || path.startsWith("http://")) {
                // 云端备份，下载
                getWebFile(path)
            } else {
                path
            }

            this@BackupRecoveryManager.logger()
                .i("startRecovery(), localPath = <$localPath>")

            val backupZippedCacheFile: File
            if (localPath.startsWith("content://")) {
                DocumentFile.fromSingleUri(context, localPath.toUri())!!.let { df ->
                    val name = df.name
                        ?: return@runCatching BackupRecoveryState.FAILED_BACKUP_PATH_UNAUTHORIZED
                    if (name.startsWith(BACKUP_FILE_NAME) && name.endsWith(BACKUP_FILE_EXT)) {
                        backupZippedCacheFile = File(cacheDir, name)
                        if (!backupZippedCacheFile.exists()) {
                            backupZippedCacheFile.createNewFile()
                        }
                        context.contentResolver.openInputStream(localPath.toUri())!!.use {
                            backupZippedCacheFile.writeBytes(it.readBytes())
                        }
                    } else {
                        return@runCatching BackupRecoveryState.FAILED_BACKUP_PATH_UNAUTHORIZED
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

            this@BackupRecoveryManager.logger()
                .i("startRecovery(), backupZippedCacheFile = <$backupZippedCacheFile>")
            val destFiles = arrayListOf<File>()
            // 解压缩
            ZipFile(backupZippedCacheFile.absolutePath).use { zipFile ->
                val entries = zipFile.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement() as ZipEntry
                    val comment = entry.comment
                    this@BackupRecoveryManager.logger()
                        .i("startRecovery(), comment = <$comment>")
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
                ?: return@runCatching BackupRecoveryState.FAILED_FILE_FORMAT_ERROR

            val backupDatabase = DelegateSQLiteDatabase(
                context.openOrCreateDatabase(
                    dbFile.absolutePath,
                    Context.MODE_PRIVATE,
                    null,
                ),
            )
            val currentDatabase = database.openHelper.writableDatabase
            if (DatabaseMigrations.recoveryFromDb(backupDatabase, currentDatabase)) {
                BackupRecoveryState.SUCCESS_RECOVERY
            } else {
                BackupRecoveryState.FAILED_BACKUP_PATH_UNAUTHORIZED
            }
        }.getOrElse { throwable ->
            this@BackupRecoveryManager.logger().e(throwable, "startRecovery(), backup")
            BackupRecoveryState.FAILED_BACKUP_PATH_UNAUTHORIZED
        }

        this@BackupRecoveryManager.logger().i("startRecovery(), result = <$result>")

        if (!ApplicationInfo.isDev) {
            // 删除缓存路径
            cacheDir.deleteAllFiles()
        }

        val state = if (result == BackupRecoveryState.SUCCESS_RECOVERY) {
            BackupRecoveryState.Success(BackupRecoveryState.SUCCESS_RECOVERY)
        } else {
            BackupRecoveryState.Failed(result)
        }
        updateRecoveryState(state)
    }
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
