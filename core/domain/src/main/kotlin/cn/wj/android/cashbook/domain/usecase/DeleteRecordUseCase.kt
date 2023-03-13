package cn.wj.android.cashbook.domain.usecase

import cn.wj.android.cashbook.core.data.repository.RecordRepository
import cn.wj.android.cashbook.core.datastore.datasource.AppPreferencesDataSource
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DeleteRecordUseCase @Inject constructor(
    private val recordRepository: RecordRepository,
) {

    suspend operator fun invoke(recordId: Long) =
        withContext(Dispatchers.IO) {
            // 从数据库中删除记录及相关信息
            recordRepository.deleteRecord(recordId)
        }
}