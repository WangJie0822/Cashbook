package cn.wj.android.cashbook.domain.usecase

import android.util.ArrayMap
import cn.wj.android.cashbook.core.data.repository.AssetRepository
import cn.wj.android.cashbook.core.data.repository.RecordRepository
import cn.wj.android.cashbook.core.data.repository.TagRepository
import cn.wj.android.cashbook.core.data.repository.TypeRepository
import cn.wj.android.cashbook.core.model.entity.RecordViewsEntity
import cn.wj.android.cashbook.core.model.model.RecordViewsModel
import cn.wj.android.cashbook.core.model.transfer.asEntity
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapLatest

/**
 * 获取当前月显示数据用例
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/2/22
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GetCurrentMonthRecordViewsUseCase @Inject constructor(
    private val recordRepository: RecordRepository,
    private val typeRepository: TypeRepository,
    private val assetRepository: AssetRepository,
    private val tagRepository: TagRepository,
) {

    operator fun invoke(): Flow<Map<String, List<RecordViewsEntity>>> =
        recordRepository.currentMonthRecordListData.mapLatest { list ->
            val map = ArrayMap<String, ArrayList<RecordViewsEntity>>()
            list.sortedBy { it.recordTime }
                .reversed()
                .forEach {
                    val key = it.recordTime.split(" ").firstOrNull()?.split("-")?.lastOrNull()
                        ?.toIntOrNull()?.toString()
                    val entity = RecordViewsModel(
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
                        it.recordTime
                    ).asEntity()
                    if (map.containsKey(key)) {
                        map[key]!!.add(entity)
                    } else {
                        map[key] = arrayListOf(entity)
                    }
                }
            map
        }

    private fun RecordViewsModel.asEntity(): RecordViewsEntity {
        return RecordViewsEntity(
            this.id,
            this.booksId,
            this.type.asEntity(),
            this.asset?.asEntity(),
            this.relatedAsset?.asEntity(),
            this.amount,
            this.charges,
            this.concessions,
            this.remark,
            this.reimbursable,
            this.relatedTags.map { it.asEntity() },
            listOf(), // TODO
            this.recordTime,
        )
    }
}