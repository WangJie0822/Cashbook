package cn.wj.android.cashbook.domain.usecase

import cn.wj.android.cashbook.core.common.annotation.CashbookDispatchers
import cn.wj.android.cashbook.core.common.annotation.Dispatcher
import cn.wj.android.cashbook.core.data.repository.RecordRepository
import cn.wj.android.cashbook.core.data.repository.TypeRepository
import cn.wj.android.cashbook.core.model.model.RecordModel
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.withContext

class SaveRecordUseCase @Inject constructor(
    private val recordRepository: RecordRepository,
    private val typeRepository: TypeRepository,
    @Dispatcher(CashbookDispatchers.IO) private val coroutineContext: CoroutineContext
) {

    suspend operator fun invoke(
        recordModel: RecordModel,
        tagIdList: List<Long>,
        relatedRecordIdList: List<Long>,
    ) = withContext(coroutineContext) {
        // 向数据库内更新最新记录信息及关联信息
        val needRelated = typeRepository.needRelated(recordModel.typeId)
        recordRepository.updateRecord(
            record = recordModel,
            tagIdList = tagIdList,
            needRelated = needRelated,
            relatedRecordIdList = relatedRecordIdList
        )
    }
}