package cn.wj.android.cashbook.data.repository.main

import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import cn.wj.android.cashbook.BuildConfig
import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.base.ext.base.isContentScheme
import cn.wj.android.cashbook.base.ext.base.logger
import cn.wj.android.cashbook.base.ext.base.orElse
import cn.wj.android.cashbook.base.ext.base.string
import cn.wj.android.cashbook.base.tools.DATE_FORMAT_BACKUP
import cn.wj.android.cashbook.base.tools.DATE_FORMAT_MONTH_DAY
import cn.wj.android.cashbook.base.tools.DATE_FORMAT_NO_SECONDS
import cn.wj.android.cashbook.base.tools.DATE_FORMAT_YEAR_MONTH
import cn.wj.android.cashbook.base.tools.copyToPath
import cn.wj.android.cashbook.base.tools.createFileIfNotExists
import cn.wj.android.cashbook.base.tools.dateFormat
import cn.wj.android.cashbook.base.tools.deleteFiles
import cn.wj.android.cashbook.base.tools.readBytes
import cn.wj.android.cashbook.base.tools.toJsonString
import cn.wj.android.cashbook.base.tools.toLongTime
import cn.wj.android.cashbook.base.tools.toTypeEntity
import cn.wj.android.cashbook.base.tools.unzipToDir
import cn.wj.android.cashbook.base.tools.zipToFile
import cn.wj.android.cashbook.data.config.AppConfigs
import cn.wj.android.cashbook.data.constants.BACKUP_ASSET_FILE_NAME
import cn.wj.android.cashbook.data.constants.BACKUP_BOOKS_FILE_NAME
import cn.wj.android.cashbook.data.constants.BACKUP_CACHE_FILE_NAME
import cn.wj.android.cashbook.data.constants.BACKUP_DIR_NAME
import cn.wj.android.cashbook.data.constants.BACKUP_FILE_EXT
import cn.wj.android.cashbook.data.constants.BACKUP_FILE_NAME
import cn.wj.android.cashbook.data.constants.BACKUP_INFO_NAME
import cn.wj.android.cashbook.data.constants.BACKUP_RECORD_FILE_NAME
import cn.wj.android.cashbook.data.constants.BACKUP_TAG_FILE_NAME
import cn.wj.android.cashbook.data.constants.BACKUP_TYPE_FILE_NAME
import cn.wj.android.cashbook.data.constants.GITEE_OWNER
import cn.wj.android.cashbook.data.constants.GITHUB_OWNER
import cn.wj.android.cashbook.data.constants.MIME_TYPE_ZIP
import cn.wj.android.cashbook.data.constants.REPO_NAME
import cn.wj.android.cashbook.data.constants.SWITCH_INT_ON
import cn.wj.android.cashbook.data.database.CashbookDatabase
import cn.wj.android.cashbook.data.database.table.AssetTable
import cn.wj.android.cashbook.data.database.table.BooksTable
import cn.wj.android.cashbook.data.database.table.RecordTable
import cn.wj.android.cashbook.data.database.table.TagTable
import cn.wj.android.cashbook.data.database.table.TypeTable
import cn.wj.android.cashbook.data.entity.BackupEntity
import cn.wj.android.cashbook.data.entity.BackupVersionEntity
import cn.wj.android.cashbook.data.entity.DataResult
import cn.wj.android.cashbook.data.entity.DateRecordEntity
import cn.wj.android.cashbook.data.entity.RESULT_CODE_RECOVERY_CHANNEL_ERROR
import cn.wj.android.cashbook.data.entity.RESULT_CODE_RECOVERY_PATH_ERROR
import cn.wj.android.cashbook.data.entity.RESULT_CODE_RECOVERY_UNKNOWN_FILE
import cn.wj.android.cashbook.data.entity.RESULT_CODE_SUCCESS
import cn.wj.android.cashbook.data.entity.RESULT_CODE_WEBDAV_FAILED
import cn.wj.android.cashbook.data.entity.RecordEntity
import cn.wj.android.cashbook.data.entity.UpdateInfoEntity
import cn.wj.android.cashbook.data.net.WebService
import cn.wj.android.cashbook.data.repository.Repository
import cn.wj.android.cashbook.data.transform.toUpdateInfoEntity
import cn.wj.android.cashbook.manager.AppManager
import cn.wj.android.cashbook.manager.WebDAVManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*

/**
 * ???????????????????????????
 *
 * > [??????](mailto:15555650921@163.com) ????????? 2021/7/28
 */
class MainRepository(database: CashbookDatabase, private val service: WebService) : Repository(database) {

    /** ?????????????????? */
    suspend fun getHomepageList(): List<DateRecordEntity> = withContext(Dispatchers.IO) {
        // ??????????????????????????????
        val result = arrayListOf<DateRecordEntity>()
        val calendar = Calendar.getInstance()
        val today = calendar.get(Calendar.DAY_OF_MONTH)
        val recordTime = "${calendar.timeInMillis.dateFormat(DATE_FORMAT_YEAR_MONTH)}-01 00:00:00".toLongTime(DATE_FORMAT_NO_SECONDS) ?: return@withContext result
        val list = recordDao.queryAfterRecordTimeByBooksId(recordTime).filter {
            it.system != SWITCH_INT_ON
        }
        val map = hashMapOf<String, MutableList<RecordEntity>>()
        for (item in list) {
            val dateKey = item.recordTime.dateFormat(DATE_FORMAT_MONTH_DAY)
            val dayInt = dateKey.split(".").lastOrNull()?.toIntOrNull().orElse(-1)
            val key = dateKey + when (dayInt) {
                today -> {
                    // ??????
                    " ${R.string.today.string}"
                }
                today - 1 -> {
                    // ??????
                    " ${R.string.yesterday.string}"
                }
                today - 2 -> {
                    // ??????
                    " ${R.string.the_day_before_yesterday.string}"
                }
                else -> {
                    ""
                }
            }
            val value = loadRecordEntityFromTable(item, false) ?: continue
            if (key.isNotBlank()) {
                if (map.containsKey(key)) {
                    map[key]!!.add(value)
                } else {
                    map[key] = arrayListOf(value)
                }
            }
        }
        map.keys.forEach { key ->
            result.add(
                DateRecordEntity(
                    date = key,
                    list = map[key].orEmpty().sortedBy { it.recordTime }.reversed()
                )
            )
        }
        result.sortedBy { it.date.toLongTime(DATE_FORMAT_MONTH_DAY) }.reversed()
    }

    /** ??????????????????????????? */
    suspend fun getCurrentMonthRecord(): List<RecordEntity> = withContext(Dispatchers.IO) {
        val result = arrayListOf<RecordEntity>()
        // ???????????????????????????
        val calendar = Calendar.getInstance()
        val monthStartDate = "${calendar.timeInMillis.dateFormat(DATE_FORMAT_YEAR_MONTH)}-01 00:00:00".toLongTime() ?: return@withContext result
        recordDao.queryAfterRecordTimeByBooksId(monthStartDate).forEach { item ->
            val record = loadRecordEntityFromTable(item, false)
            if (null != record) {
                result.add(record)
            }
        }
        result
    }

    /** ???????????? Release ?????? */
    suspend fun queryLatestRelease(useGitee: Boolean): UpdateInfoEntity = withContext(Dispatchers.IO) {
        if (useGitee) {
            service.giteeQueryRelease(GITEE_OWNER, REPO_NAME, "latest")
        } else {
            service.githubQueryRelease(GITHUB_OWNER, REPO_NAME, "latest")
        }.toUpdateInfoEntity()
    }

    /** ???????????????????????? */
    suspend fun getChangelog(useGitee: Boolean): String = withContext(Dispatchers.IO) {
        @Suppress("BlockingMethodInNonBlockingContext")
        if (useGitee) {
            service.giteeRaw(GITEE_OWNER, REPO_NAME, "CHANGELOG.md")
        } else {
            service.githubRaw(GITHUB_OWNER, REPO_NAME, "CHANGELOG.md")
        }.string()
    }

    /** ??????????????????????????????????????? */
    suspend fun getPrivacyPolicy(useGitee: Boolean): String = withContext(Dispatchers.IO) {
        @Suppress("BlockingMethodInNonBlockingContext")
        if (useGitee) {
            service.giteeRaw(GITEE_OWNER, REPO_NAME, "PRIVACY_POLICY.md")
        } else {
            service.githubRaw(GITHUB_OWNER, REPO_NAME, "PRIVACY_POLICY.md")
        }.string()
    }

    /** ????????????????????? */
    suspend fun backup(): DataResult<Any> = withContext(Dispatchers.IO) {
        val currentMs = System.currentTimeMillis()
        val dateFormat = currentMs.dateFormat(DATE_FORMAT_BACKUP)

        // ??????????????????
        val cachePath = File(AppManager.getContext().cacheDir, BACKUP_CACHE_FILE_NAME).absolutePath
        // ???????????????????????????
        cachePath.deleteFiles()

        val cacheFiles = arrayListOf<String>()
        // ??????????????????????????????
        cachePath.createFileIfNotExists(BACKUP_ASSET_FILE_NAME).run {
            cacheFiles.add(this.path)
            writeText(assetDao.queryAll().toJsonString())
        }
        cachePath.createFileIfNotExists(BACKUP_BOOKS_FILE_NAME).run {
            cacheFiles.add(this.path)
            writeText(booksDao.queryAll().toJsonString())
        }
        cachePath.createFileIfNotExists(BACKUP_RECORD_FILE_NAME).run {
            cacheFiles.add(this.path)
            writeText(recordDao.queryAll().toJsonString())
        }
        cachePath.createFileIfNotExists(BACKUP_TAG_FILE_NAME).run {
            cacheFiles.add(this.path)
            writeText(tagDao.queryAll().toJsonString())
        }
        cachePath.createFileIfNotExists(BACKUP_TYPE_FILE_NAME).run {
            cacheFiles.add(this.path)
            writeText(typeDao.queryAll().toJsonString())
        }
        // ??????????????????
        cachePath.createFileIfNotExists(BACKUP_INFO_NAME).run {
            cacheFiles.add(this.path)
            writeText(BackupVersionEntity(BuildConfig.BACKUP_VERSION, BuildConfig.FLAVOR, dateFormat).toJsonString())
        }

        // ???????????????
        val zippedPath = cachePath + File.separator + BACKUP_FILE_NAME + dateFormat + BACKUP_FILE_EXT
        // ???????????????
        cacheFiles.zipToFile(zippedPath)

        // ????????? WebDAV
        val result = WebDAVManager.backup(zippedPath)

        // ??????????????????????????????????????????
        zippedPath.copyToPath(AppConfigs.backupPath, MIME_TYPE_ZIP, BACKUP_DIR_NAME)
        // ??????????????????
        AppConfigs.lastBackupMs = currentMs
        DataResult(
            code = if (result) {
                RESULT_CODE_SUCCESS
            } else {
                RESULT_CODE_WEBDAV_FAILED
            }
        )
    }

    /** ???????????????????????? */
    suspend fun getWebBackupList(): List<BackupEntity> = withContext(Dispatchers.IO) {
        WebDAVManager.getBackupFileList().sortedBy { it.name }.reversed()
    }

    /** ?????? [path] ????????????????????? */
    suspend fun getLocalBackupList(path: String): List<BackupEntity> = withContext(Dispatchers.IO) {
        val result = arrayListOf<BackupEntity>()
        if (path.isContentScheme()) {
            DocumentFile.fromTreeUri(AppManager.getContext(), Uri.parse(path))?.let { df ->
                if (df.name == BACKUP_DIR_NAME) {
                    df
                } else {
                    df.findFile(BACKUP_DIR_NAME)
                }?.listFiles()?.filter {
                    it.name?.startsWith(BACKUP_FILE_NAME) == true && it.name?.endsWith(BACKUP_FILE_EXT) == true
                }?.forEach {
                    result.add(BackupEntity(it.name.orEmpty(), it.uri.toString(), false))
                }
            }
        } else {
            val file = File(path)
            if (!file.exists()) {
                return@withContext result
            }
            if (file.name == BACKUP_DIR_NAME) {
                file.listFiles()
            } else {
                file.listFiles()?.firstOrNull {
                    it.name == BACKUP_DIR_NAME
                }?.listFiles()
            }?.filter {
                it.name.startsWith(BACKUP_FILE_NAME) && it.name.endsWith(BACKUP_FILE_EXT)
            }?.forEach {
                result.add(BackupEntity(it.name, it.path, false))
            }
        }
        result.sortedBy { it.name }.reversed()
    }

    /** ?????? [url] ??????????????????????????? */
    suspend fun recoveryWeb(url: String): DataResult<Any> = withContext(Dispatchers.IO) {
        // ??????????????????
        val cachePath = File(AppManager.getContext().cacheDir, BACKUP_CACHE_FILE_NAME).absolutePath
        // ???????????????????????????
        cachePath.deleteFiles()

        // ??????????????????
        val zippedFile = cachePath.createFileIfNotExists(BACKUP_FILE_NAME + "Cache" + BACKUP_FILE_EXT)

        // ????????????
        WebDAVManager.downloadTo(url, zippedFile)

        // ?????????????????????????????????
        recoveryFromZipped(zippedFile.absolutePath.unzipToDir(cachePath))
    }

    /** ??????????????? [path] ???????????? */
    suspend fun recoveryLocal(path: String): DataResult<Any> = withContext(Dispatchers.IO) {
        // ??????????????????
        val cachePath = File(AppManager.getContext().cacheDir, BACKUP_CACHE_FILE_NAME).absolutePath
        // ???????????????????????????
        cachePath.deleteFiles()

        // ??????????????????
        val zippedFile = cachePath.createFileIfNotExists(BACKUP_FILE_NAME + "Cache" + BACKUP_FILE_EXT)

        if (path.isContentScheme()) {
            DocumentFile.fromTreeUri(AppManager.getContext(), Uri.parse(path))?.let { df ->
                val backupFile = if (df.name == BACKUP_DIR_NAME) {
                    df
                } else {
                    df.findFile(BACKUP_DIR_NAME)
                }?.listFiles()?.sortedBy {
                    it.name
                }?.reversed()?.firstOrNull {
                    it.name?.startsWith(BACKUP_FILE_NAME) == true && it.name?.endsWith(BACKUP_FILE_EXT) == true
                } ?: return@withContext DataResult.failed(RESULT_CODE_RECOVERY_PATH_ERROR)
                logger().d("backupFile: ${backupFile.name}")
                // ???????????????????????????
                val bytes = backupFile.readBytes() ?: return@withContext DataResult.failed(RESULT_CODE_RECOVERY_PATH_ERROR)
                zippedFile.writeBytes(bytes)
            }
        } else {
            val file = File(path)
            if (!file.exists()) {
                return@withContext DataResult.failed(RESULT_CODE_RECOVERY_PATH_ERROR)
            }
            val backupFile = if (file.name == BACKUP_DIR_NAME) {
                file.listFiles()
            } else {
                file.listFiles()?.firstOrNull {
                    it.name == BACKUP_DIR_NAME
                }?.listFiles()
            }?.sortedBy {
                it.name
            }?.reversed()?.firstOrNull {
                it.name.startsWith(BACKUP_FILE_NAME) && it.name.endsWith(BACKUP_FILE_EXT)
            } ?: return@withContext DataResult.failed(RESULT_CODE_RECOVERY_PATH_ERROR)
            logger().d("backupFile: ${backupFile.name}")
            // ???????????????????????????
            backupFile.copyTo(zippedFile)
        }

        // ?????????????????????????????????
        recoveryFromZipped(zippedFile.absolutePath.unzipToDir(cachePath))
    }

    /** ?????????????????? [files] ??????????????? */
    private suspend fun recoveryFromZipped(files: List<File>): DataResult<Any> = withContext(Dispatchers.IO) {
        // ??????????????????
        val info = files.firstOrNull {
            it.name == BACKUP_INFO_NAME
        }?.readText().toTypeEntity<BackupVersionEntity>() ?: return@withContext DataResult.failed<Any>(RESULT_CODE_RECOVERY_UNKNOWN_FILE)
        if (info.channel != BuildConfig.FLAVOR) {
            return@withContext DataResult.failed(RESULT_CODE_RECOVERY_CHANNEL_ERROR)
        }
        files.forEach {
            when (it.name) {
                BACKUP_ASSET_FILE_NAME -> {
                    it.readText().toTypeEntity<List<AssetTable>>()?.let { list ->
                        assetDao.insertOrReplace(*list.toTypedArray())
                    }
                }
                BACKUP_BOOKS_FILE_NAME -> {
                    it.readText().toTypeEntity<List<BooksTable>>()?.let { list ->
                        booksDao.insertOrReplace(*list.toTypedArray())
                    }
                }
                BACKUP_RECORD_FILE_NAME -> {
                    it.readText().toTypeEntity<List<RecordTable>>()?.let { list ->
                        recordDao.insertOrReplace(*list.toTypedArray())
                    }
                }
                BACKUP_TAG_FILE_NAME -> {
                    it.readText().toTypeEntity<List<TagTable>>()?.let { list ->
                        tagDao.insertOrReplace(*list.toTypedArray())
                    }
                }
                BACKUP_TYPE_FILE_NAME -> {
                    it.readText().toTypeEntity<List<TypeTable>>()?.let { list ->
                        typeDao.insertOrReplace(*list.toTypedArray())
                    }
                }
            }
        }
        DataResult.success()
    }
}