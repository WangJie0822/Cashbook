package cn.wj.android.cashbook.core.data.repository.impl

import cn.wj.android.cashbook.core.common.ext.logger
import cn.wj.android.cashbook.core.common.model.recordDataVersion
import cn.wj.android.cashbook.core.common.model.updateVersion
import cn.wj.android.cashbook.core.common.tools.DATE_FORMAT_YEAR_MONTH
import cn.wj.android.cashbook.core.common.tools.dateFormat
import cn.wj.android.cashbook.core.common.tools.parseDateLong
import cn.wj.android.cashbook.core.data.repository.RecordRepository
import cn.wj.android.cashbook.core.data.repository.asModel
import cn.wj.android.cashbook.core.data.repository.asTable
import cn.wj.android.cashbook.core.database.dao.RecordDao
import cn.wj.android.cashbook.core.database.dao.TransactionDao
import cn.wj.android.cashbook.core.datastore.datasource.AppPreferencesDataSource
import cn.wj.android.cashbook.core.model.model.RecordModel
import cn.wj.android.cashbook.core.model.model.TagModel
import java.util.Calendar
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class RecordRepositoryImpl @Inject constructor(
    private val recordDao: RecordDao,
    private val transactionDao: TransactionDao,
    appPreferencesDataSource: AppPreferencesDataSource
) : RecordRepository {

    override val currentMonthRecordListData: Flow<List<RecordModel>> =
        combine(recordDataVersion, appPreferencesDataSource.appData) { _, appData ->
            logger().i("currentMonthRecordListData update")
            queryCurrentMonthRecordByBooksId(appData.currentBookId)
        }

    override suspend fun queryById(recordId: Long): RecordModel? {
        val queryById = recordDao.queryById(recordId)
        return queryById?.asModel()
    }

    private suspend fun queryCurrentMonthRecordByBooksId(booksId: Long): List<RecordModel> {
        val monthFirst =
            "${Calendar.getInstance().timeInMillis.dateFormat(DATE_FORMAT_YEAR_MONTH)}-01 00:00:00"
        val result = recordDao.queryByBooksIdAfterDate(booksId, monthFirst.parseDateLong())
            .map { it.asModel() }
        logger().i("queryCurrentMonthRecordByBooksId(booksId = <$booksId>) monthFirst = <$monthFirst>, result = $result")
        return result
    }

    override suspend fun updateRecord(record: RecordModel, tags: List<TagModel>) {
        logger().i("updateRecord(record = <$record>, tags = <$tags>")
        transactionDao.updateRecordTransaction(record.asTable(), tags.map { it.id })
        recordDataVersion.updateVersion()
    }

    override suspend fun deleteRecord(recordId: Long) {
        transactionDao.deleteRecordTransaction(recordId)
        recordDataVersion.updateVersion()
    }
}