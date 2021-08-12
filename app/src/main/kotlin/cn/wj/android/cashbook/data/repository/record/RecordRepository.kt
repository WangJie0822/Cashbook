package cn.wj.android.cashbook.data.repository.record

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.liveData
import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.base.ext.base.*
import cn.wj.android.cashbook.base.tools.DATE_FORMAT_DATE
import cn.wj.android.cashbook.base.tools.DATE_FORMAT_MONTH_DAY
import cn.wj.android.cashbook.base.tools.dateFormat
import cn.wj.android.cashbook.base.tools.toLongTime
import cn.wj.android.cashbook.data.constants.DEFAULT_PAGE_SIZE
import cn.wj.android.cashbook.data.constants.SWITCH_INT_ON
import cn.wj.android.cashbook.data.database.CashbookDatabase
import cn.wj.android.cashbook.data.entity.AssetEntity
import cn.wj.android.cashbook.data.entity.DateRecordEntity
import cn.wj.android.cashbook.data.entity.RecordEntity
import cn.wj.android.cashbook.data.entity.TagEntity
import cn.wj.android.cashbook.data.enums.ClassificationTypeEnum
import cn.wj.android.cashbook.data.enums.RecordTypeEnum
import cn.wj.android.cashbook.data.live.CurrentBooksLiveData
import cn.wj.android.cashbook.data.repository.Repository
import cn.wj.android.cashbook.data.source.SearchRecordPagingSource
import cn.wj.android.cashbook.data.transform.toAssetEntity
import cn.wj.android.cashbook.data.transform.toRecordTable
import cn.wj.android.cashbook.data.transform.toTagEntity
import cn.wj.android.cashbook.data.transform.toTagTable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

/**
 * 记录相关数据仓库
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/7/28
 */
class RecordRepository(database: CashbookDatabase) : Repository(database) {

    /** 根据资产id [keywords] 获取记录 Pager 数据 */
    fun getRecordListByKeywordsPagerData(keywords: String) = Pager(
        config = PagingConfig(DEFAULT_PAGE_SIZE),
        pagingSourceFactory = { SearchRecordPagingSource(keywords, this) }
    ).liveData

    /** 根据关键字 [keywords] 搜索账单记录 */
    suspend fun getRecordByKeywords(keywords: String, pageNum: Int, pageSize: Int = DEFAULT_PAGE_SIZE): List<RecordEntity> = withContext(Dispatchers.IO) {
        val result = arrayListOf<RecordEntity>()
        recordDao.queryRecordByKeywords("%$keywords%", pageNum, pageSize)
            .forEach { item ->
                val record = loadRecordEntityFromTable(item, true)
                if (null != record) {
                    result.add(record)
                }
            }
        result
    }

    /** 根据 [id] 获取记录数据 */
    suspend fun getRecordById(id: Long): RecordEntity? = withContext(Dispatchers.IO) {
        loadRecordEntityFromTable(recordDao.queryById(id), false)
    }

    /** 获取指定日期的记录数据 */
    suspend fun getRecordListByDate(calendar: com.haibin.calendarview.Calendar): List<DateRecordEntity> = withContext(Dispatchers.IO) {
        // 首页显示一周内数据
        val result = arrayListOf<DateRecordEntity>()
        val year = calendar.year
        val month = calendar.month
        val day = calendar.day
        val monthStr = if (month < 10) "0$month" else "$month"
        val dayStr = if (day < 10) "0$day" else "$day"
        val startTime = "$year-$monthStr-$dayStr 00:00:00".toLongTime() ?: return@withContext result
        val endTime = "$year-$monthStr-$dayStr 23:59:59".toLongTime() ?: return@withContext result
        val list = recordDao.queryRecordBetweenTimeByBooksId(CurrentBooksLiveData.booksId, startTime, endTime).filter {
            it.system != SWITCH_INT_ON
        }
        val map = hashMapOf<String, MutableList<RecordEntity>>()
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1
        val today = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
        for (item in list) {
            val dateKey = item.recordTime.dateFormat(DATE_FORMAT_MONTH_DAY)
            val yearInt = item.recordTime.dateFormat().split("-").firstOrNull()?.toIntOrNull().orElse(-1)
            val monthInt = dateKey.split(".").firstOrNull()?.toIntOrNull().orElse(-1)
            val dayInt = dateKey.split(".").lastOrNull()?.toIntOrNull().orElse(-1)
            val key = dateKey + if (yearInt == currentYear && monthInt == currentMonth) {
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

    /** 根据日期获取当月每天数据结余 */
    suspend fun getCalendarSchemesByDate(calendar: com.haibin.calendarview.Calendar): Map<String, com.haibin.calendarview.Calendar> = withContext(Dispatchers.IO) {
        val result = hashMapOf<String, com.haibin.calendarview.Calendar>()
        val year = calendar.year
        val month = calendar.month
        val monthStr = if (month < 10) "0$month" else "$month"
        val cal = Calendar.getInstance()
        cal.set(year, month - 1, 1)
        cal.add(Calendar.MONTH, 1)
        cal.add(Calendar.DAY_OF_YEAR, -1)
        val endDay = cal.get(Calendar.DAY_OF_MONTH)
        val endDayStr = if (endDay < 10) "0$endDay" else "$endDay"
        val startTime = "$year-$monthStr-01 00:00:00".toLongTime() ?: return@withContext result
        val endTime = "$year-$monthStr-$endDayStr 23:59:59".toLongTime() ?: return@withContext result
        val map = recordDao.queryRecordBetweenTimeByBooksId(CurrentBooksLiveData.booksId, startTime, endTime).filter {
            it.system != SWITCH_INT_ON
        }.mapNotNull {
            loadRecordEntityFromTable(it, false)
        }.groupBy {
            it.recordTime.dateFormat(DATE_FORMAT_DATE)
        }
        for ((date, list) in map) {
            var amount = "0".toBigDecimal()
            list.forEach {
                when (it.typeEnum) {
                    RecordTypeEnum.EXPENDITURE -> {
                        // 支出
                        amount -= it.amount.toBigDecimal()
                    }
                    RecordTypeEnum.INCOME -> {
                        // 收入
                        amount += it.amount.toBigDecimal()
                    }
                    RecordTypeEnum.TRANSFER -> {
                        // 转账
                        if (it.charge.toFloatOrNull().orElse(0f) > 0f) {
                            // 有手续费
                            amount -= it.charge.toBigDecimal()
                        }
                    }
                    else -> {
                    }
                }
            }
            val amountStr = amount.decimalFormat()
            val value = com.haibin.calendarview.Calendar().apply {
                this.year = year
                this.month = month
                this.day = date.split("-").last().toInt()
                this.scheme = amountStr
            }
            result[value.toString()] = value
        }
        result
    }

    /** 根据资产 id [assetId] 获取资产数据 */
    suspend fun findAssetById(assetId: Long): AssetEntity? = withContext(Dispatchers.IO) {
        if (assetId < 0) {
            return@withContext null
        }
        val queryById = assetDao.queryById(assetId)
        queryById?.toAssetEntity(getAssetBalanceById(assetId, queryById.type == ClassificationTypeEnum.CREDIT_CARD_ACCOUNT.name))
    }

    /** 将记录 [record] 插入到数据库并返回生成的主键 id */
    suspend fun insertRecord(record: RecordEntity): Long = withContext(Dispatchers.IO) {
        recordDao.insert(record.toRecordTable())
    }

    /** 更新 [record] 记录 */
    suspend fun updateRecord(record: RecordEntity) = withContext(Dispatchers.IO) {
        recordDao.update(record.toRecordTable())
    }

    /** 删除记录数据 [record] */
    suspend fun deleteRecord(record: RecordEntity) = withContext(Dispatchers.IO) {
        recordDao.delete(record.toRecordTable())
    }

    /** 获取所有标签数据并返回 */
    suspend fun getTagList(): List<TagEntity> = withContext(Dispatchers.IO) {
        tagDao.queryAll().map {
            it.toTagEntity()
        }
    }

    /** 根据标签名 [name] 查询并返回标签 */
    suspend fun queryTagByName(name: String): List<TagEntity> = withContext(Dispatchers.IO) {
        tagDao.queryByName(name).map {
            it.toTagEntity()
        }
    }

    /** 将 [tag] 插入数据库并返回主键 */
    suspend fun insertTag(tag: TagEntity): Long = withContext(Dispatchers.IO) {
        tagDao.insert(tag.toTagTable())
    }

    /** 更新 [tag] 数据 */
    suspend fun updateTag(tag: TagEntity) = withContext(Dispatchers.IO) {
        tagDao.update(tag.toTagTable())
    }

    /** 删除 [tag] 数据*/
    suspend fun deleteTag(tag: TagEntity) = withContext(Dispatchers.IO) {
        tagDao.delete(tag.toTagTable())
    }

    /** 获取最近三个月金额小于等于 [amount] 的所有支出记录 */
    suspend fun getLastThreeMonthExpenditureRecordLargerThanAmount(amount: String): List<RecordEntity> = withContext(Dispatchers.IO) {
        val result = arrayListOf<RecordEntity>()
        // 获取最近三个月开始时间
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, -90)
        val startDate = "${calendar.timeInMillis.dateFormat(DATE_FORMAT_DATE)} 00:00:00".toLongTime() ?: return@withContext result
        recordDao.queryExpenditureRecordAfterDateLargerThanAmount(CurrentBooksLiveData.booksId, amount.toFloatOrZero(), startDate).forEach { item ->
            val record = loadRecordEntityFromTable(item, true)
            if (null != record) {
                result.add(record)
            }
        }
        result.filter {
            // 排除已关联的以及标记为可报销的
            null == it.beAssociated && !it.reimbursable
        }
    }

    /** 获取最近三个月标记为可报销的支出记录 */
    suspend fun getLastThreeMonthReimburseExpenditureRecord(): List<RecordEntity> = withContext(Dispatchers.IO) {
        val result = arrayListOf<RecordEntity>()
        // 获取最近三个月开始时间
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, -90)
        val startDate = "${calendar.timeInMillis.dateFormat(DATE_FORMAT_DATE)} 00:00:00".toLongTime() ?: return@withContext result
        recordDao.queryReimburseExpenditureRecordAfterDate(CurrentBooksLiveData.booksId, startDate).forEach { item ->
            val record = loadRecordEntityFromTable(item, true)
            if (null != record) {
                result.add(record)
            }
        }
        result.filter {
            // 排除已关联的
            null == it.beAssociated
        }
    }

    /** 根据关键字 [keywords] 搜索没有标记为可报销的支出记录 */
    suspend fun getExpenditureRecordByKeywords(keywords: String): List<RecordEntity> = withContext(Dispatchers.IO) {
        val result = arrayListOf<RecordEntity>()
        recordDao.queryExpenditureRecordByKeywords("%$keywords%")
            .forEach { item ->
                val record = loadRecordEntityFromTable(item, true)
                if (null != record) {
                    result.add(record)
                }
            }
        result.filter {
            // 排除已关联
            null == it.beAssociated
        }
    }

    /** 根据关键字 [keywords] 搜索可报销的支出记录 */
    suspend fun getReimburseExpenditureRecordByKeywords(keywords: String): List<RecordEntity> = withContext(Dispatchers.IO) {
        val result = arrayListOf<RecordEntity>()
        recordDao.queryReimburseExpenditureRecordByKeywords("%$keywords%")
            .forEach { item ->
                val record = loadRecordEntityFromTable(item, true)
                if (null != record) {
                    result.add(record)
                }
            }
        result.filter {
            // 排除已关联的
            null == it.beAssociated
        }
    }
}