package cn.wj.android.cashbook.domain.usecase

import cn.wj.android.cashbook.core.data.repository.TagRepository
import cn.wj.android.cashbook.core.model.entity.TagEntity
import cn.wj.android.cashbook.core.model.transfer.asEntity
import javax.inject.Inject

class GetDefaultTagListUseCase @Inject constructor(
    private val tagRepository: TagRepository
) {

    suspend operator fun invoke(recordId: Long): List<TagEntity> {
        return tagRepository.getRelatedTag(recordId)
            .map { it.asEntity() }
    }
}