package cn.wj.android.cashbook.core.data.repository.impl

import cn.wj.android.cashbook.core.common.annotation.CashbookDispatchers
import cn.wj.android.cashbook.core.common.annotation.Dispatcher
import cn.wj.android.cashbook.core.common.ext.completeZero
import cn.wj.android.cashbook.core.common.ext.logger
import cn.wj.android.cashbook.core.common.model.recordDataVersion
import cn.wj.android.cashbook.core.common.model.updateVersion
import cn.wj.android.cashbook.core.common.tools.DATE_FORMAT_DATE
import cn.wj.android.cashbook.core.common.tools.DATE_FORMAT_NO_SECONDS
import cn.wj.android.cashbook.core.common.tools.dateFormat
import cn.wj.android.cashbook.core.common.tools.parseDateLong
import cn.wj.android.cashbook.core.common.tools.toLongTime
import cn.wj.android.cashbook.core.data.repository.RecordRepository
import cn.wj.android.cashbook.core.data.repository.asModel
import cn.wj.android.cashbook.core.data.repository.asTable
import cn.wj.android.cashbook.core.database.dao.RecordDao
import cn.wj.android.cashbook.core.database.dao.TransactionDao
import cn.wj.android.cashbook.core.datastore.datasource.AppPreferencesDataSource
import cn.wj.android.cashbook.core.model.model.RecordModel
import java.util.Calendar
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class RecordRepositoryImpl @Inject constructor(
    private val recordDao: RecordDao,
    private val transactionDao: TransactionDao,
    private val appPreferencesDataSource: AppPreferencesDataSource,
    @Dispatcher(CashbookDispatchers.IO) private val coroutineContext: CoroutineContext,
) : RecordRepository {

    override suspend fun queryById(recordId: Long): RecordModel? = withContext(coroutineContext) {
        recordDao.queryById(recordId)?.asModel()
    }

    override suspend fun queryByTypeId(id: Long): List<RecordModel> =
        withContext(coroutineContext) {
            recordDao.queryByTypeId(id)
                .map { it.asModel() }
        }

    override suspend fun queryRelatedById(recordId: Long): List<RecordModel> =
        withContext(coroutineContext) {
            recordDao.queryRelatedById(recordId)
                .map { it.asModel() }
        }

    override suspend fun updateRecord(
        record: RecordModel,
        tagIdList: List<Long>,
        needRelated: Boolean,
        relatedRecordIdList: List<Long>,
    ) = withContext(coroutineContext) {
        logger().i("updateRecord(record = <$record>, tagIdList = <$tagIdList>")
        transactionDao.updateRecordTransaction(
            recordTable = record.asTable(),
            tagIdList = tagIdList,
            needRelated = needRelated,
            relatedRecordIdList = relatedRecordIdList,
        )
        recordDataVersion.updateVersion()
        // 更新上次使用的默认数据
        appPreferencesDataSource.updateLastAssetId(record.assetId)
    }

    override suspend fun deleteRecord(recordId: Long) = withContext(coroutineContext) {
        transactionDao.deleteRecordTransaction(recordId)
        recordDataVersion.updateVersion()
    }

    override suspend fun queryPagingRecordListByAssetId(
        assetId: Long,
        page: Int,
        pageSize: Int
    ): List<RecordModel> = withContext(coroutineContext) {
        recordDao.queryRecordByAssetId(
            booksId = appPreferencesDataSource.appData.first().currentBookId,
            assetId = assetId,
            pageNum = page * pageSize,
            pageSize = pageSize,
        ).map {
            it.asModel()
        }
    }

    override fun queryRecordByYearMonth(
        year: String,
        month: String
    ): Flow<List<RecordModel>> {
        val monthInt = month.toInt()
        val yearInt = year.toInt()
        var nextYearInt = yearInt
        val nextMonthInt = if (monthInt >= 12) {
            nextYearInt++
            1
        } else {
            monthInt + 1
        }
        val startDate = "${yearInt.completeZero()}-${monthInt.completeZero()}-01 00:00:00"
        val endDate = "${nextYearInt.completeZero()}-${nextMonthInt.completeZero()}-01 00:00:00"
        return combine(recordDataVersion, appPreferencesDataSource.appData) { _, appData ->
            recordDao.queryByBooksIdBetweenDate(
                appData.currentBookId,
                startDate.parseDateLong(),
                endDate.parseDateLong()
            ).map {
                it.asModel()
            }
        }
    }

    override suspend fun getDefaultRecord(typeId: Long): RecordModel =
        withContext(coroutineContext) {
            val appDataModel = appPreferencesDataSource.appData.first()
            RecordModel(
                id = -1L,
                booksId = appDataModel.currentBookId,
                typeId = typeId,
                assetId = appDataModel.lastAssetId,
                relatedAssetId = -1L,
                amount = "0",
                charges = "",
                concessions = "",
                remark = "",
                reimbursable = false,
                recordTime = System.currentTimeMillis().dateFormat(DATE_FORMAT_NO_SECONDS),
            )
        }

    override suspend fun changeRecordTypeBeforeDeleteType(fromId: Long, toId: Long): Unit =
        withContext(coroutineContext) {
            recordDao.changeRecordTypeBeforeDeleteType(fromId = fromId, toId = toId)
            recordDataVersion.updateVersion()
        }

    override suspend fun getRelatedIdListById(id: Long): List<Long> =
        withContext(coroutineContext) {
            recordDao.getRelatedIdListById(id)
        }

    override suspend fun getRecordIdListFromRelatedId(id: Long): List<Long> =
        withContext(coroutineContext) {
            recordDao.getRecordIdListFromRelatedId(id)
        }

    override suspend fun getLastThreeMonthRefundableRecordList(): List<RecordModel> =
        withContext(coroutineContext) {
            // 获取最近三个月开始时间
            val calendar = Calendar.getInstance()
            calendar[Calendar.DAY_OF_MONTH] = -90
            val startDate =
                "${calendar.timeInMillis.dateFormat(DATE_FORMAT_DATE)} 00:00:00".toLongTime()!!
            val appDataModel = appPreferencesDataSource.appData.first()
            recordDao.getLastThreeMonthExpenditureRecordList(
                booksId = appDataModel.currentBookId,
                recordTime = startDate,
            ).map { it.asModel() }
        }

    override suspend fun getLastThreeMonthReimbursableRecordList(): List<RecordModel> =
        withContext(coroutineContext) {
            // 获取最近三个月开始时间
            val calendar = Calendar.getInstance()
            calendar[Calendar.DAY_OF_MONTH] = -90
            val startDate =
                "${calendar.timeInMillis.dateFormat(DATE_FORMAT_DATE)} 00:00:00".toLongTime()!!
            val appDataModel = appPreferencesDataSource.appData.first()
            recordDao.getLastThreeMonthExpenditureReimburseRecordList(
                booksId = appDataModel.currentBookId,
                recordTime = startDate,
            ).map { it.asModel() }
        }

    override suspend fun getLastThreeMonthRefundableRecordListByKeyword(keyword: String): List<RecordModel> =
        withContext(coroutineContext) {
            // 获取最近三个月开始时间
            val calendar = Calendar.getInstance()
            calendar[Calendar.DAY_OF_MONTH] = -90
            val startDate =
                "${calendar.timeInMillis.dateFormat(DATE_FORMAT_DATE)} 00:00:00".toLongTime()!!
            val appDataModel = appPreferencesDataSource.appData.first()
            recordDao.getLastThreeMonthExpenditureRecordListByKeyword(
                keyword = keyword,
                booksId = appDataModel.currentBookId,
                recordTime = startDate,
            ).map { it.asModel() }
        }

    override suspend fun getLastThreeMonthReimbursableRecordListByKeyword(keyword: String): List<RecordModel> =
        withContext(coroutineContext) {
            // 获取最近三个月开始时间
            val calendar = Calendar.getInstance()
            calendar[Calendar.DAY_OF_MONTH] = -90
            val startDate =
                "${calendar.timeInMillis.dateFormat(DATE_FORMAT_DATE)} 00:00:00".toLongTime()!!
            val appDataModel = appPreferencesDataSource.appData.first()
            recordDao.getLastThreeMonthExpenditureReimburseRecordListByKeyword(
                keyword = keyword,
                booksId = appDataModel.currentBookId,
                recordTime = startDate,
            ).map { it.asModel() }
        }
}