package cn.wj.android.cashbook.domain.usecase

import cn.wj.android.cashbook.core.data.repository.AssetRepository
import cn.wj.android.cashbook.core.data.repository.RecordRepository
import cn.wj.android.cashbook.core.data.repository.TagRepository
import cn.wj.android.cashbook.core.data.repository.TypeRepository
import cn.wj.android.cashbook.core.model.entity.RecordTypeEntity
import cn.wj.android.cashbook.core.model.entity.RecordViewsEntity
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi

/**
 * 获取关联记录数据用例
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/2/22
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GetRelatedRecordViewsUseCase @Inject constructor(
    private val recordRepository: RecordRepository,
    private val typeRepository: TypeRepository,
    private val assetRepository: AssetRepository,
    private val tagRepository: TagRepository,
) {

    suspend operator fun invoke(
        keyword: String,
        recordTypeEntity: RecordTypeEntity?
    ): List<RecordViewsEntity> {
        return listOf()
    }

}