package cn.wj.android.cashbook.core.data.repository.impl

import cn.wj.android.cashbook.core.data.repository.RecordRepository
import cn.wj.android.cashbook.core.data.repository.asModel
import cn.wj.android.cashbook.core.data.repository.asTable
import cn.wj.android.cashbook.core.database.dao.RecordDao
import cn.wj.android.cashbook.core.database.dao.TransactionDao
import cn.wj.android.cashbook.core.model.model.RecordModel
import cn.wj.android.cashbook.core.model.model.TagModel
import javax.inject.Inject

class RecordRepositoryImpl @Inject constructor(
    private val recordDao: RecordDao,
    private val transactionDao: TransactionDao,
) : RecordRepository {

    override suspend fun queryById(recordId: Long): RecordModel? {
        val queryById = recordDao.queryById(recordId)
        return queryById?.asModel()
    }

    override suspend fun updateRecord(record: RecordModel, tags: List<TagModel>) {
        transactionDao.updateRecord(record.asTable(), tags.map { it.id })
    }
}