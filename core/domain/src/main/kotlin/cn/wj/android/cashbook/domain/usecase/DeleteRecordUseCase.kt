package cn.wj.android.cashbook.domain.usecase

import cn.wj.android.cashbook.core.common.annotation.CashbookDispatchers
import cn.wj.android.cashbook.core.common.annotation.Dispatcher
import cn.wj.android.cashbook.core.data.repository.RecordRepository
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.withContext

class DeleteRecordUseCase @Inject constructor(
    private val recordRepository: RecordRepository,
    @Dispatcher(CashbookDispatchers.IO) private val coroutineContext: CoroutineContext,
) {

    suspend operator fun invoke(recordId: Long) = withContext(coroutineContext) {
        // 从数据库中删除记录及相关信息
        recordRepository.deleteRecord(recordId)
    }
}