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

package cn.wj.android.cashbook.core.data.uitl.impl

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
import cn.wj.android.cashbook.core.data.repository.RecordRepository
import cn.wj.android.cashbook.core.data.repository.SettingRepository
import cn.wj.android.cashbook.core.data.uitl.BACKUP_FORMAT_VERSION
import cn.wj.android.cashbook.core.data.uitl.BackupRecoveryManager
import cn.wj.android.cashbook.core.data.uitl.BackupRecoveryState
import cn.wj.android.cashbook.core.data.uitl.MANIFEST_ENTRY
import cn.wj.android.cashbook.core.data.uitl.NetworkMonitor
import cn.wj.android.cashbook.core.data.uitl.RECORD_IMAGES_ENTRY_PREFIX
import cn.wj.android.cashbook.core.data.uitl.RecordImageFileStorage
import cn.wj.android.cashbook.core.data.uitl.SETTINGS_ENTRY
import cn.wj.android.cashbook.core.data.uitl.buildManifestJson
import cn.wj.android.cashbook.core.database.CashbookDatabase
import cn.wj.android.cashbook.core.database.migration.DatabaseMigrations
import cn.wj.android.cashbook.core.database.util.DelegateSQLiteDatabase
import cn.wj.android.cashbook.core.datastore.datasource.CombineProtoDataSource
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
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

/**
 * 备份恢复管理实现
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/7/21
 */
class BackupRecoveryManagerImpl @Inject constructor(
    private val networkMonitor: NetworkMonitor,
    private val settingRepository: SettingRepository,
    private val recordRepository: RecordRepository,
    private val webDAVHandler: WebDAVHandler,
    private val database: CashbookDatabase,
    private val combineProtoDataSource: CombineProtoDataSource,
    private val recordImageFileStorage: RecordImageFileStorage,
    @ApplicationContext private val context: Context,
    @Dispatcher(CashbookDispatchers.IO) private val ioCoroutineContext: CoroutineContext,
) : BackupRecoveryManager {

    private val connectedDataVersion = dataVersion()
    private val backupDataVersion = dataVersion()

    private val _backupState: MutableStateFlow<BackupRecoveryState> = MutableStateFlow(
        BackupRecoveryState.None,
    )
    private val _recoveryState: MutableStateFlow<BackupRecoveryState> = MutableStateFlow(
        BackupRecoveryState.None,
    )

    override val isWebDAVConnected: Flow<Boolean> =
        combine(
            networkMonitor.isOnline,
            connectedDataVersion,
            settingRepository.appSettingsModel,
        ) { isOnline, _, _ ->
            isOnline && refreshConnectedStatus()
        }

    override val backupState: Flow<BackupRecoveryState> = _backupState

    override val recoveryState: Flow<BackupRecoveryState> = _recoveryState

    private val onlineBackupListData = combine(connectedDataVersion, backupDataVersion) { _, _ ->
        val appDataMode = settingRepository.appSettingsModel.first()
        webDAVHandler.list(appDataMode.webDAVDomain.backupPath)
    }

    private val localBackupListData = settingRepository.appSettingsModel.mapLatest { appDataModel ->
        getLocalBackupList(appDataModel.backupPath)
    }

    override fun refreshWebDAVConnected() {
        connectedDataVersion.updateVersion()
    }

    private suspend fun upload(backupFileUri: Uri): Boolean = withContext(ioCoroutineContext) {
        this@BackupRecoveryManagerImpl.logger()
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

    override suspend fun requestAutoBackup(): BackupRecoveryState = withContext(ioCoroutineContext) {
        requestBackup(
            onlyLocal = !settingRepository.appSettingsModel.first().mobileNetworkBackupEnable &&
                !networkMonitor.isWifi.first(),
        )
    }

    override suspend fun requestBackup(onlyLocal: Boolean): BackupRecoveryState =
        withContext(ioCoroutineContext) {
            updateBackupState(BackupRecoveryState.InProgress)
            val appDataModel = settingRepository.appSettingsModel.first()
            val backupPath = appDataModel.backupPath
            logger().i("requestBackup(onlyLocal = <$onlyLocal>), backupPath = <$backupPath>")
            if (backupPath.isBlank()) {
                logger().i("requestBackup(onlyLocal), blank backupPath")
                val state = BackupRecoveryState.Failed(BackupRecoveryState.FAILED_BLANK_BACKUP_PATH)
                updateBackupState(state)
                state
            } else {
                if (grantedPermissions(backupPath)) {
                    // 有权限，开始备份
                    logger().i("requestBackup(onlyLocal), startBackup")
                    startBackup(backupPath, onlyLocal)
                } else {
                    logger().i("requestBackup(onlyLocal), unauthorized")
                    val state = BackupRecoveryState.Failed(BackupRecoveryState.FAILED_BACKUP_PATH_UNAUTHORIZED)
                    updateBackupState(state)
                    state
                }
            }
        }

    override fun updateBackupState(state: BackupRecoveryState) {
        _backupState.tryEmit(state)
    }

    override suspend fun requestRecovery(path: String): Unit = withContext(ioCoroutineContext) {
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

    override fun updateRecoveryState(state: BackupRecoveryState) {
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
            val bytes = get(url) ?: return@withCredentials ""
            backupCacheFile.writeBytes(bytes)
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

    override suspend fun getRecoveryList(
        onlyLocal: Boolean,
        localPath: String,
    ): List<BackupModel> = withContext(ioCoroutineContext) {
        val list = if (!onlyLocal && isWebDAVConnected.first()) {
            // 使用云端数据
            val webDAVDomain = settingRepository.appSettingsModel.first().webDAVDomain
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
            updateRecoveryState(BackupRecoveryState.Failed(BackupRecoveryState.FAILED_BACKUP_PATH_EMPTY))
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
            // 先规范化并校验 scheme：dav/davs 统一映射到 https，明文 http 直接拒绝
            val normalized = normalizeWebDAVScheme(this)
            if (normalized == null) {
                // 明文 http 或非法 scheme，拒绝构建 URL，避免携带 Basic 凭据走明文通道
                this@BackupRecoveryManagerImpl.logger()
                    .e("backupPath, rejected plaintext/illegal scheme: <$this>")
                return@runCatching ""
            }
            val url =
                URL("$normalized${if (normalized.endsWith("/")) "" else "/"}$BACKUP_DIR_NAME/")
            val raw = url.toString()
            URLEncoder.encode(raw, "UTF-8")
                .replace("\\+".toRegex(), "%20")
                .replace("%3A".toRegex(), ":")
                .replace("%2F".toRegex(), "/")
        }
            .getOrElse { throwable ->
                this@BackupRecoveryManagerImpl.logger().e(throwable, "backupPath")
                ""
            }

    private suspend fun <T> withCredentials(
        block: suspend WebDAVHandler.(String) -> T,
    ): T = withContext(ioCoroutineContext) {
        val appDataMode = settingRepository.appSettingsModel.first()
        webDAVHandler.setCredentials(appDataMode.webDAVAccount, appDataMode.webDAVPassword)
        webDAVHandler.block(appDataMode.webDAVDomain.backupPath)
    }

    private suspend fun startBackup(backupPath: String, onlyLocal: Boolean): BackupRecoveryState {
        this@BackupRecoveryManagerImpl.logger()
            .i("startBackup(backupPath = <$backupPath>, onlyLocal = <$onlyLocal>)")
        if (backupPath.isBlank()) {
            this@BackupRecoveryManagerImpl.logger().i("startBackup(), backupPath is blank")
            val state = BackupRecoveryState.Failed(BackupRecoveryState.FAILED_BLANK_BACKUP_PATH)
            updateBackupState(state)
            return state
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

            this@BackupRecoveryManagerImpl.logger()
                .i("startBackup(), backupPath = <$backupPath>, databaseFile = <$databaseFile>, databaseCacheFile = <$databaseCacheFile>, dateFormat = <$dateFormat>")

            // 执行 checkpoint 确保所有数据写入主数据库文件，然后复制
            database.openHelper.writableDatabase.run {
                query("PRAGMA wal_checkpoint(TRUNCATE)").use { cursor ->
                    cursor.moveToFirst()
                    val busy = cursor.getInt(0)
                    if (busy != 0) {
                        this@BackupRecoveryManagerImpl.logger()
                            .w("WAL checkpoint busy, backup may be incomplete")
                    }
                }
            }
            databaseFile.copyTo(databaseCacheFile)
            // 对备份副本 in-place VACUUM 压实（minSdk24 兼容；失败/ENOSPC 回退未 VACUUM 的 checkpointed 副本，不让备份失败）
            runCatching {
                context.openOrCreateDatabase(databaseCacheFile.absolutePath, Context.MODE_PRIVATE, null)
                    .use { db ->
                        db.execSQL("VACUUM")
                        db.rawQuery("PRAGMA integrity_check", null).use { c ->
                            if (c.moveToFirst()) {
                                val ok = c.getString(0)
                                if (!"ok".equals(ok, ignoreCase = true)) {
                                    this@BackupRecoveryManagerImpl.logger()
                                        .w("startBackup(), VACUUM integrity_check = <$ok>, keep copy")
                                }
                            }
                        }
                    }
            }.onFailure {
                this@BackupRecoveryManagerImpl.logger()
                    .w("startBackup(), VACUUM failed, fallback to checkpointed copy: ${it.message}")
            }
            // 多 entry 打包：db(comment 仅此项，兼容旧 app 仅恢复 db 不崩) + record_images/* + settings.json + manifest.json
            val settingsJson = settingRepository.exportSettings()
            val manifestJson = buildManifestJson(BACKUP_FORMAT_VERSION, ApplicationInfo.versionName)
            val imagesDir = recordImageFileStorage.baseDir()
            val zippedPath =
                backupCacheDir.absolutePath + File.separator + BACKUP_FILE_NAME + dateFormat + BACKUP_FILE_EXT
            ZipOutputStream(FileOutputStream(zippedPath)).use { zos ->
                zos.setLevel(Deflater.BEST_COMPRESSION)
                putZipFileEntry(zos, databaseCacheFile, databaseCacheFile.name, ApplicationInfo.applicationInfo)
                imagesDir.listFiles()?.filter { it.isFile }?.forEach { img ->
                    putZipFileEntry(zos, img, RECORD_IMAGES_ENTRY_PREFIX + img.name, comment = null)
                }
                putZipBytesEntry(zos, SETTINGS_ENTRY, settingsJson.toByteArray(), comment = null)
                putZipBytesEntry(zos, MANIFEST_ENTRY, manifestJson.toByteArray(), comment = null)
            }
            // 将备份文件复制到备份路径
            val zippedFile = File(zippedPath)
            if (!zippedFile.exists()) {
                return@runCatching BackupRecoveryState.FAILED_BACKUP_PATH_UNAUTHORIZED
            }
            val zippedFileName = zippedFile.name
            val keepLatest = settingRepository.appSettingsModel.first().keepLatestBackup
            val backupFileUri = if (backupPath.startsWith("content://")) {
                val documentFile = DocumentFile.fromTreeUri(context, backupPath.toUri())
                    ?: return@runCatching BackupRecoveryState.FAILED_BACKUP_PATH_UNAUTHORIZED
                // 先删除同名文件（避免重名冲突）
                documentFile.findFile(zippedFileName)?.delete()
                // 先写入新备份文件
                val backupFile = documentFile.createFile("application/zip", zippedFileName)
                    ?: return@runCatching BackupRecoveryState.FAILED_BACKUP_PATH_UNAUTHORIZED
                val outputStream = context.contentResolver.openOutputStream(backupFile.uri)
                if (outputStream == null) {
                    backupFile.delete()
                    return@runCatching BackupRecoveryState.FAILED_BACKUP_PATH_UNAUTHORIZED
                }
                outputStream.use {
                    it.write(zippedFile.readBytes())
                }
                // 写入成功后再删除旧备份
                if (keepLatest) {
                    documentFile.listFiles()
                        .filter {
                            val name = it.name.orEmpty()
                            name.contains(BACKUP_FILE_NAME) && name.endsWith(BACKUP_FILE_EXT) &&
                                it.uri != backupFile.uri
                        }
                        .forEach { it.delete() }
                }
                backupFile.uri
            } else {
                val backupDir = File(backupPath)
                if (!backupDir.exists()) {
                    backupDir.mkdirs()
                }
                // 先写入新备份文件
                val backupFile = File(backupDir, zippedFileName)
                if (!backupFile.exists()) {
                    backupFile.createNewFile()
                }
                zippedFile.copyTo(backupFile, overwrite = true)
                // 写入成功后再删除旧备份
                if (keepLatest) {
                    backupDir.listFiles { _, name ->
                        name.contains(BACKUP_FILE_NAME) && name.endsWith(BACKUP_FILE_EXT) &&
                            name != zippedFileName
                    }?.forEach { it.delete() }
                }
                backupFile.toUri()
            }

            // 更新备份时间
            settingRepository.updateBackupMs(currentMs)

            val innerResult = if (!onlyLocal && isWebDAVConnected.first()) {
                // 非仅本地且 WebDAV 已连接，上传备份文件到云端
                runCatching {
                    this@BackupRecoveryManagerImpl.logger()
                        .i("startBackup(), upload to WebDAV")
                    if (upload(backupFileUri)) {
                        BackupRecoveryState.SUCCESS_BACKUP
                    } else {
                        BackupRecoveryState.FAILED_BACKUP_WEBDAV
                    }
                }.getOrElse { throwable ->
                    this@BackupRecoveryManagerImpl.logger()
                        .e(throwable, "startBackup(), upload")
                    BackupRecoveryState.FAILED_BACKUP_WEBDAV
                }
            } else {
                BackupRecoveryState.SUCCESS_BACKUP
            }

            innerResult
        }.getOrElse { throwable ->
            this@BackupRecoveryManagerImpl.logger().e(throwable, "startBackup(), backup")
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
        this@BackupRecoveryManagerImpl.logger().i("startBackup(), result = <$result>")
        return state
    }

    /** 写一个文件 entry 到 zip；comment 仅 db entry 传非空（旧 app 据 comment 仅恢复 db、不崩） */
    private fun putZipFileEntry(zos: ZipOutputStream, file: File, entryName: String, comment: String?) {
        val entry = ZipEntry(entryName)
        if (comment != null) entry.comment = comment
        zos.putNextEntry(entry)
        BufferedInputStream(FileInputStream(file)).use { zos.write(it.readBytes()) }
        zos.closeEntry()
    }

    /** 写一段字节 entry 到 zip（settings.json / manifest.json） */
    private fun putZipBytesEntry(zos: ZipOutputStream, entryName: String, bytes: ByteArray, comment: String?) {
        val entry = ZipEntry(entryName)
        if (comment != null) entry.comment = comment
        zos.putNextEntry(entry)
        zos.write(bytes)
        zos.closeEntry()
    }

    /**
     * 恢复前安全快照：在改动当前库之前，把"当前库"完整复制到本地一个安全副本，
     * 复用与 [startBackup] 相同的 WAL checkpoint + 文件复制能力（不发明高风险 IO）。
     *
     * 安全副本带 [PRE_RESTORE_PREFIX] 标记，文件名形如 `pre-restore-yyyyMMddHHmmss.db`，
     * 落在 [PRE_RESTORE_DIR_NAME] 目录下（与恢复缓存目录解耦，避免恢复结束清理缓存时被删除），
     * 供恢复失败/异常时人工回退兜底。每次仅保留最近一次快照。
     *
     * 当前库不存在（首次安装等）时无需保护，视为成功。
     *
     * @return `true` 表示已成功快照或无需快照，可安全继续恢复；
     *         `false` 表示快照失败，调用方须中止恢复，不在无保护下覆盖当前库
     */
    private fun createPreRestoreBackup(): Boolean = runCatching {
        val databaseFile = context.getDatabasePath(DB_FILE_NAME)
        if (!databaseFile.exists()) {
            // 当前库不存在（首次安装等），无需保护，视为快照成功
            this@BackupRecoveryManagerImpl.logger()
                .i("createPreRestoreBackup(), current db not exists, skip")
            return@runCatching true
        }
        // 安全副本目录（独立于恢复缓存目录，恢复结束清理 cacheDir 时不会被删除）
        val preRestoreDir = File(context.cacheDir, PRE_RESTORE_DIR_NAME)
        // 仅保留最近一次快照，先清空旧快照再写入
        preRestoreDir.deleteAllFiles()
        preRestoreDir.mkdirs()
        // 执行 checkpoint 确保所有 WAL 数据写入主库文件，再复制（与 startBackup 一致）
        database.openHelper.writableDatabase.run {
            query("PRAGMA wal_checkpoint(TRUNCATE)").use { cursor ->
                cursor.moveToFirst()
                val busy = cursor.getInt(0)
                if (busy != 0) {
                    this@BackupRecoveryManagerImpl.logger()
                        .w("createPreRestoreBackup(), WAL checkpoint busy, snapshot may be incomplete")
                }
            }
        }
        val dateFormat = System.currentTimeMillis().dateFormat(DATE_FORMAT_BACKUP)
        val preRestoreFile = File(preRestoreDir, "$PRE_RESTORE_PREFIX$dateFormat.db")
        databaseFile.copyTo(preRestoreFile, overwrite = true)
        this@BackupRecoveryManagerImpl.logger()
            .i("createPreRestoreBackup(), snapshot saved to <${preRestoreFile.absolutePath}>")
        true
    }.getOrElse { throwable ->
        this@BackupRecoveryManagerImpl.logger().e(throwable, "createPreRestoreBackup()")
        false
    }

    private suspend fun startRecovery(path: String) {
        if (path.isBlank()) {
            this@BackupRecoveryManagerImpl.logger()
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
                // 云端备份，下载——拒绝明文 http 携凭据下载（与备份上传侧 normalizeWebDAVScheme 对称，
                // 防中间人在恢复时嗅探 Basic 凭据；存量 http 配置用户需改用 https 后重试）
                if (path.startsWith(SCHEME_HTTP)) {
                    this@BackupRecoveryManagerImpl.logger()
                        .e("startRecovery(), reject cleartext http recovery download: <$path>")
                    return@runCatching BackupRecoveryState.FAILED_RECOVERY_CLEARTEXT
                }
                getWebFile(path)
            } else {
                path
            }

            this@BackupRecoveryManagerImpl.logger()
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

            this@BackupRecoveryManagerImpl.logger()
                .i("startRecovery(), backupZippedCacheFile = <$backupZippedCacheFile>")
            val destFiles = arrayListOf<File>()
            // 解压缩
            ZipFile(backupZippedCacheFile.absolutePath).use { zipFile ->
                val entries = zipFile.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement() as ZipEntry
                    val comment = entry.comment
                    this@BackupRecoveryManagerImpl.logger()
                        .i("startRecovery(), comment = <$comment>")
                    if (comment.isNullOrBlank()) {
                        continue
                    }
                    val entryName = entry.name
                    if (entry.isDirectory) {
                        continue
                    }
                    val destFile = File(cacheDir, entryName)
                    // Zip Slip 防护：校验解压目标路径是否在缓存目录内
                    if (!destFile.canonicalPath.startsWith(cacheDir.canonicalPath + File.separator)) {
                        continue
                    }
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

            // 恢复前安全快照：在真正改动当前库（copyData）之前，先把当前库备份到本地安全副本。
            // 由于恢复语义为"保守修正"（合并、非清空替换），此快照用于恢复异常时人工回退兜底。
            if (!createPreRestoreBackup()) {
                // 安全备份失败，中止恢复，避免在无保护的情况下覆盖当前库
                this@BackupRecoveryManagerImpl.logger()
                    .e("startRecovery(), pre-restore backup failed, abort recovery")
                return@runCatching BackupRecoveryState.FAILED_PRE_RESTORE_SNAPSHOT
            }

            val currentDatabase = database.openHelper.writableDatabase
            if (DatabaseMigrations.recoveryFromDb(backupDatabase, currentDatabase)) {
                // 确保固定类型行存在（旧版备份可能不包含）
                currentDatabase.execSQL(
                    "INSERT OR IGNORE INTO db_type (id, parent_id, name, icon_name, type_level, type_category, protected, sort) VALUES (-2001, -1, '退款', 'vector_type_refund_24', 0, 1, 1, 0)",
                )
                currentDatabase.execSQL(
                    "INSERT OR IGNORE INTO db_type (id, parent_id, name, icon_name, type_level, type_category, protected, sort) VALUES (-2002, -1, '报销', 'vector_type_reimburse_24', 0, 1, 1, 0)",
                )
                currentDatabase.execSQL(
                    "INSERT OR IGNORE INTO db_type (id, parent_id, name, icon_name, type_level, type_category, protected, sort) VALUES (-2003, -1, '信用卡还款', 'vector_type_credit_card_payment_24', 0, 2, 1, 0)",
                )
                // 重置迁移标志，下次启动时自动触发应用层迁移
                combineProtoDataSource.updateRefundTypeId(0L)
                combineProtoDataSource.updateReimburseTypeId(0L)
                combineProtoDataSource.updateCreditCardPaymentTypeId(0L)
                // 净自付（H3）：恢复是 CONFLICT_REPLACE 合并、恢复后无进程重启，
                // 合并后 finalAmount 为「备份旧语义行 + 当前库新语义行」混合，须同步对全表重算覆盖
                // F2：走 Repository 统一副作用（重算 + 置 finalAmountNetRecalcDone + bump recordDataVersion）
                recordRepository.recalculateAllFinalAmount()
                BackupRecoveryState.SUCCESS_RECOVERY
            } else {
                // recoveryFromDb 返回 false 表示数据库版本不兼容或迁移路径缺失，
                // 与"路径未授权"无关，使用语义更准确的文件格式/版本错误码
                BackupRecoveryState.FAILED_FILE_FORMAT_ERROR
            }
        }.getOrElse { throwable ->
            this@BackupRecoveryManagerImpl.logger().e(throwable, "startRecovery(), backup")
            BackupRecoveryState.FAILED_BACKUP_PATH_UNAUTHORIZED
        }

        this@BackupRecoveryManagerImpl.logger().i("startRecovery(), result = <$result>")

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

    companion object {

        /** 恢复前安全快照子目录名 */
        private const val PRE_RESTORE_DIR_NAME = "pre-restore"

        /** 恢复前安全快照文件名前缀 */
        private const val PRE_RESTORE_PREFIX = "pre-restore-"

        /**
         * 规范化并校验 WebDAV 地址的 scheme（纯函数，便于单测）。
         *
         * - `dav://`、`davs://` 统一映射到 `https://`（加密通道）
         * - `https://` 原样保留
         * - `http://`（明文）显式拒绝，返回 `null`，避免携带 Basic 凭据走明文通道
         * - 其它非 WebDAV scheme（空字符串、无 scheme 等）一律拒绝，返回 `null`
         *
         * @param raw 用户填写的原始 WebDAV 地址
         * @return 规范化后以 `https://` 开头的地址；非法/明文输入返回 `null` 表示拒绝
         */
        fun normalizeWebDAVScheme(raw: String?): String? {
            if (raw.isNullOrBlank()) {
                return null
            }
            val trimmed = raw.trim()
            val normalized = when {
                // davs:// 与 dav:// 均映射到 https://（强制加密）
                trimmed.startsWith(SCHEME_DAVS) ->
                    SCHEME_HTTPS + trimmed.removePrefix(SCHEME_DAVS)

                trimmed.startsWith(SCHEME_DAV) ->
                    SCHEME_HTTPS + trimmed.removePrefix(SCHEME_DAV)

                // 已经是 https，原样保留
                trimmed.startsWith(SCHEME_HTTPS) -> trimmed

                // 明文 http 显式拒绝
                trimmed.startsWith(SCHEME_HTTP) -> null

                // 其它（无 scheme 或非法 scheme）拒绝
                else -> null
            } ?: return null
            // 校验映射后 host 非空（仅有 scheme 而无 host 视为非法）
            return if (normalized.removePrefix(SCHEME_HTTPS).isBlank()) {
                null
            } else {
                normalized
            }
        }
    }
}
