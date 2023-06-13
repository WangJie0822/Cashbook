package cn.wj.android.cashbook.domain.usecase

import cn.wj.android.cashbook.core.data.repository.RecordRepository
import cn.wj.android.cashbook.core.model.entity.RecordEntity
import cn.wj.android.cashbook.core.model.transfer.asEntity
import javax.inject.Inject

class GetDefaultRelatedRecordListUseCase @Inject constructor(
    private val recordRepository: RecordRepository,
) {

    suspend operator fun invoke(recordId: Long): List<RecordEntity> {
        return recordRepository.queryRelatedById(recordId)
            .map { it.asEntity() }
    }
}