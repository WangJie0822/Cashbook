package cn.wj.android.cashbook.core.data.repository.impl

import cn.wj.android.cashbook.core.common.model.DataVersion
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine

class RecordRepositoryImpl @Inject constructor(
    private val recordDao: RecordDao,
    private val transactionDao: TransactionDao,
    appPreferencesDataSource: AppPreferencesDataSource
) : RecordRepository {

    private val dataVersion: DataVersion = MutableStateFlow(0)

    override val currentMonthRecordListData: Flow<List<RecordModel>> =
        combine(dataVersion, appPreferencesDataSource.appData) { _, appData ->
            queryCurrentMonthRecordByBooksId(appData.currentBookId)
        }

    override suspend fun queryById(recordId: Long): RecordModel? {
        val queryById = recordDao.queryById(recordId)
        return queryById?.asModel()
    }

    private suspend fun queryCurrentMonthRecordByBooksId(booksId: Long): List<RecordModel> {
        return recordDao.queryByBooksIdAfterDate(
            booksId,
            "${Calendar.getInstance().timeInMillis.dateFormat(DATE_FORMAT_YEAR_MONTH)}-01 00:00:00".parseDateLong()
        ).map { it.asModel() }
    }

    override suspend fun updateRecord(record: RecordModel, tags: List<TagModel>) {
        transactionDao.updateRecordTransaction(record.asTable(), tags.map { it.id })
        dataVersion.updateVersion()
    }
}