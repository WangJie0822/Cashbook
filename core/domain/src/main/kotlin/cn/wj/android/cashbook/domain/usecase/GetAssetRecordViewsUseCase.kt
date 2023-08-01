package cn.wj.android.cashbook.domain.usecase

import cn.wj.android.cashbook.core.data.repository.AssetRepository
import cn.wj.android.cashbook.core.data.repository.RecordRepository
import cn.wj.android.cashbook.core.data.repository.TagRepository
import cn.wj.android.cashbook.core.data.repository.TypeRepository
import cn.wj.android.cashbook.core.model.entity.RecordViewsEntity
import cn.wj.android.cashbook.core.model.model.RecordViewsModel
import cn.wj.android.cashbook.core.model.transfer.asEntity
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 获取当前资产记录显示数据用例
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/7/3
 */
class GetAssetRecordViewsUseCase @Inject constructor(
    private val recordRepository: RecordRepository,
    private val typeRepository: TypeRepository,
    private val assetRepository: AssetRepository,
    private val tagRepository: TagRepository,
) {

    suspend operator fun invoke(
        assetId: Long,
        pageNum: Int,
        pageSize: Int,
        coroutineContext: CoroutineContext = Dispatchers.IO
    ): List<RecordViewsEntity> = withContext(coroutineContext) {
        if (assetId == -1L) {
            return@withContext emptyList()
        }
        recordRepository.queryPagingRecordListByAssetId(assetId, pageNum, pageSize)
            .sortedBy { it.recordTime }
            .reversed()
            .map {
                RecordViewsModel(
                    it.id,
                    it.booksId,
                    typeRepository.getNoNullRecordTypeById(it.typeId),
                    assetRepository.getAssetById(it.assetId),
                    assetRepository.getAssetById(it.relatedAssetId),
                    it.amount,
                    it.charges,
                    it.concessions,
                    it.remark,
                    it.reimbursable,
                    tagRepository.getRelatedTag(it.id),
                    listOf(), // TODO
                    it.recordTime,
                ).asEntity()
            }
    }


    private fun RecordViewsModel.asEntity(): RecordViewsEntity {
        return RecordViewsEntity(
            this.id,
            this.type.typeCategory,
            this.type.name,
            this.type.iconName,
            this.asset?.name,
            this.asset?.iconResId,
            this.relatedAsset?.name,
            this.relatedAsset?.iconResId,
            this.amount,
            this.charges,
            this.concessions,
            this.remark,
            this.reimbursable,
            this.relatedTags,
            listOf(), // TODO
            this.recordTime,
        )
    }
}