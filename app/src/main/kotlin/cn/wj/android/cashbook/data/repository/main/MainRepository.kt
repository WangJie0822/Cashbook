package cn.wj.android.cashbook.data.repository.main

import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.base.ext.base.orElse
import cn.wj.android.cashbook.base.ext.base.string
import cn.wj.android.cashbook.base.tools.DATE_FORMAT_DATE
import cn.wj.android.cashbook.base.tools.DATE_FORMAT_MONTH_DAY
import cn.wj.android.cashbook.base.tools.DATE_FORMAT_NO_SECONDS
import cn.wj.android.cashbook.base.tools.DATE_FORMAT_YEAR_MONTH
import cn.wj.android.cashbook.base.tools.createFileIfNotExists
import cn.wj.android.cashbook.base.tools.dateFormat
import cn.wj.android.cashbook.base.tools.deleteFiles
import cn.wj.android.cashbook.base.tools.toJsonString
import cn.wj.android.cashbook.base.tools.toLongTime
import cn.wj.android.cashbook.base.tools.zipToFile
import cn.wj.android.cashbook.data.config.AppConfigs
import cn.wj.android.cashbook.data.constants.BACKUP_ASSET_FILE_NAME
import cn.wj.android.cashbook.data.constants.BACKUP_BOOKS_FILE_NAME
import cn.wj.android.cashbook.data.constants.BACKUP_CACHE_FILE_NAME
import cn.wj.android.cashbook.data.constants.BACKUP_RECORD_FILE_NAME
import cn.wj.android.cashbook.data.constants.BACKUP_TAG_FILE_NAME
import cn.wj.android.cashbook.data.constants.BACKUP_TYPE_FILE_NAME
import cn.wj.android.cashbook.data.constants.BACKUP_ZIPPED_FILE_NAME
import cn.wj.android.cashbook.data.constants.GITEE_OWNER
import cn.wj.android.cashbook.data.constants.GITHUB_OWNER
import cn.wj.android.cashbook.data.constants.REPO_NAME
import cn.wj.android.cashbook.data.constants.SWITCH_INT_ON
import cn.wj.android.cashbook.data.database.CashbookDatabase
import cn.wj.android.cashbook.data.entity.DateRecordEntity
import cn.wj.android.cashbook.data.entity.RecordEntity
import cn.wj.android.cashbook.data.entity.UpdateInfoEntity
import cn.wj.android.cashbook.data.live.CurrentBooksLiveData
import cn.wj.android.cashbook.data.net.WebService
import cn.wj.android.cashbook.data.repository.Repository
import cn.wj.android.cashbook.data.transform.toUpdateInfoEntity
import cn.wj.android.cashbook.manager.AppManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*

/**
 * 主逻辑相关数据仓库
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/7/28
 */
class MainRepository(database: CashbookDatabase, private val service: WebService) : Repository(database) {

    /** 获取首页数据 */
    suspend fun getHomepageList(): List<DateRecordEntity> = withContext(Dispatchers.IO) {
        // 首页显示一周内数据
        val result = arrayListOf<DateRecordEntity>()
        val calendar = Calendar.getInstance()
        val today = calendar.get(Calendar.DAY_OF_MONTH)
        val month = calendar.get(Calendar.MONTH) + 1
        calendar.add(Calendar.DAY_OF_MONTH, -7)
        val recordTime = "${calendar.timeInMillis.dateFormat(DATE_FORMAT_DATE)} 00:00:00".toLongTime(DATE_FORMAT_NO_SECONDS) ?: return@withContext result
        val list = recordDao.queryAfterRecordTimeByBooksId(CurrentBooksLiveData.booksId, recordTime).filter {
            it.system != SWITCH_INT_ON
        }
        val map = hashMapOf<String, MutableList<RecordEntity>>()
        for (item in list) {
            val dateKey = item.recordTime.dateFormat(DATE_FORMAT_MONTH_DAY)
            val dayInt = dateKey.split(".").lastOrNull()?.toIntOrNull().orElse(-1)
            val monthInt = dateKey.split(".").firstOrNull()?.toIntOrNull().orElse(-1)
            val key = dateKey + if (monthInt == month) {
                when (dayInt) {
                    today -> {
                        // 今天
                        " ${R.string.today.string}"
                    }
                    today - 1 -> {
                        // 昨天
                        " ${R.string.yesterday.string}"
                    }
                    today - 2 -> {
                        // 前天
                        " ${R.string.the_day_before_yesterday.string}"
                    }
                    else -> {
                        ""
                    }
                }
            } else {
                ""
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

    /** 获取当前月所有记录 */
    suspend fun getCurrentMonthRecord(): List<RecordEntity> = withContext(Dispatchers.IO) {
        val result = arrayListOf<RecordEntity>()
        // 获取当前月开始时间
        val calendar = Calendar.getInstance()
        val monthStartDate = "${calendar.timeInMillis.dateFormat(DATE_FORMAT_YEAR_MONTH)}-01 00:00:00".toLongTime() ?: return@withContext result
        recordDao.queryAfterRecordTimeByBooksId(CurrentBooksLiveData.booksId, monthStartDate).forEach { item ->
            val record = loadRecordEntityFromTable(item, false)
            if (null != record) {
                result.add(record)
            }
        }
        result
    }

    /** 获取最新 Release 信息 */
    suspend fun queryLatestRelease(useGitee: Boolean): UpdateInfoEntity = withContext(Dispatchers.IO) {
        if (useGitee) {
            service.giteeQueryRelease(GITEE_OWNER, REPO_NAME, "latest")
        } else {
            service.githubQueryRelease(GITHUB_OWNER, REPO_NAME, "latest")
        }.toUpdateInfoEntity()
    }

    /** 获取修改日志信息 */
    suspend fun getChangelog(useGitee: Boolean): String = withContext(Dispatchers.IO) {
        @Suppress("BlockingMethodInNonBlockingContext")
        if (useGitee) {
            service.giteeRaw(GITEE_OWNER, REPO_NAME, "CHANGELOG.md")
        } else {
            service.githubRaw(GITHUB_OWNER, REPO_NAME, "CHANGELOG.md")
        }.string()
    }

    /** 获取用户协议及隐私政策信息 */
    suspend fun getPrivacyPolicy(useGitee: Boolean): String = withContext(Dispatchers.IO) {
        @Suppress("BlockingMethodInNonBlockingContext")
        if (useGitee) {
            service.giteeRaw(GITEE_OWNER, REPO_NAME, "PRIVACY_POLICY.md")
        } else {
            service.githubRaw(GITHUB_OWNER, REPO_NAME, "PRIVACY_POLICY.md")
        }.string()
    }

    /** 备份到指定路径 [path] */
    suspend fun backup(path: String) = withContext(Dispatchers.IO) {
        // 保存备份时间
        AppConfigs.lastBackupMs = System.currentTimeMillis()
        // 获取缓存路径
        val cachePath = File(AppManager.getContext().filesDir, BACKUP_CACHE_FILE_NAME).absolutePath
        // 清空缓存路径下文件
        cachePath.deleteFiles()
        val cacheFiles = arrayListOf<String>()
        // 备份数据库数据到文件
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
        // 压缩包路径
        val zippedPath = cachePath + File.separator + BACKUP_ZIPPED_FILE_NAME
        // 将缓存压缩
        cacheFiles.zipToFile(zippedPath)
    }
}