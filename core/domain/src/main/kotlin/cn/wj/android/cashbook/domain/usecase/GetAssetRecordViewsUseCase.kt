package cn.wj.android.cashbook.domain.usecase

import cn.wj.android.cashbook.core.common.annotation.CashbookDispatchers
import cn.wj.android.cashbook.core.common.annotation.Dispatcher
import cn.wj.android.cashbook.core.data.repository.RecordRepository
import cn.wj.android.cashbook.core.model.entity.RecordViewsEntity
import cn.wj.android.cashbook.core.model.transfer.asEntity
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.withContext

/**
 * 获取当前资产记录显示数据用例
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/7/3
 */
class GetAssetRecordViewsUseCase @Inject constructor(
    private val recordRepository: RecordRepository,
    private val recordModelTransToViewsUseCase: RecordModelTransToViewsUseCase,
    @Dispatcher(CashbookDispatchers.IO) private val coroutineContext: CoroutineContext,
) {

    suspend operator fun invoke(
        assetId: Long,
        pageNum: Int,
        pageSize: Int,
    ): List<RecordViewsEntity> = withContext(coroutineContext) {
        if (assetId == -1L) {
            return@withContext emptyList()
        }
        recordRepository.queryPagingRecordListByAssetId(assetId, pageNum, pageSize)
            .sortedBy { it.recordTime }
            .reversed()
            .map {
                recordModelTransToViewsUseCase(it).asEntity()
            }
    }
}