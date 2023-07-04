package cn.wj.android.cashbook.domain.usecase

import cn.wj.android.cashbook.core.data.repository.RecordRepository
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DeleteRecordUseCase @Inject constructor(
    private val recordRepository: RecordRepository,
) {

    suspend operator fun invoke(
        recordId: Long,
        coroutineContext: CoroutineContext = Dispatchers.IO
    ) = withContext(coroutineContext) {
        // 从数据库中删除记录及相关信息
        recordRepository.deleteRecord(recordId)
        // TODO 删除关联信息
    }
}