package cn.wj.android.cashbook.core.data.repository.impl

import cn.wj.android.cashbook.core.data.repository.RecordRepository
import cn.wj.android.cashbook.core.data.repository.TypeRepository
import cn.wj.android.cashbook.core.data.repository.asModel
import cn.wj.android.cashbook.core.database.dao.RecordDao
import cn.wj.android.cashbook.core.model.model.RecordModel
import javax.inject.Inject

class RecordRepositoryImpl @Inject constructor(
    private val recordDao: RecordDao,
) : RecordRepository {

    override suspend fun queryById(recordId: Long): RecordModel? {
        val queryById = recordDao.queryById(recordId)
        return queryById?.asModel()
    }
}