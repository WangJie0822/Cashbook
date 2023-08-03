package cn.wj.android.cashbook.domain.usecase

import cn.wj.android.cashbook.core.data.repository.RecordRepository
import cn.wj.android.cashbook.core.model.model.RecordModel
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SaveRecordUseCase @Inject constructor(
    private val recordRepository: RecordRepository,
) {

    suspend operator fun invoke(
        recordModel: RecordModel,
        tagIdList: List<Long>,
        coroutineContext: CoroutineContext = Dispatchers.IO
    ) = withContext(coroutineContext) {
        // 向数据库内更新最新记录信息及关联信息
        recordRepository.updateRecord(recordModel, tagIdList)
    }
}