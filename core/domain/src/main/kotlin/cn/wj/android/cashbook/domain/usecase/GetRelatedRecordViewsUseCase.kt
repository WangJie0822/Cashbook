package cn.wj.android.cashbook.domain.usecase

import cn.wj.android.cashbook.core.common.annotation.CashbookDispatchers
import cn.wj.android.cashbook.core.common.annotation.Dispatcher
import cn.wj.android.cashbook.core.data.repository.RecordRepository
import cn.wj.android.cashbook.core.data.repository.TypeRepository
import cn.wj.android.cashbook.core.model.entity.RecordViewsEntity
import cn.wj.android.cashbook.core.model.model.RecordTypeModel
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.withContext

/**
 * 获取关联记录数据用例
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/2/22
 */
class GetRelatedRecordViewsUseCase @Inject constructor(
    private val recordRepository: RecordRepository,
    private val typeRepository: TypeRepository,
    private val recordModelTransToViewsUseCase: RecordModelTransToViewsUseCase,
    @Dispatcher(CashbookDispatchers.IO) private val coroutineContext: CoroutineContext,
) {

    suspend operator fun invoke(
        keyword: String,
        recordTypeModel: RecordTypeModel?
    ): List<RecordViewsEntity> = withContext(coroutineContext) {
        if (recordTypeModel == null) {
            emptyList()
        } else {
            val reimburse = typeRepository.isReimburseType(recordTypeModel.id)
            if (keyword.isBlank()) {
                // 没有关键字，获取最近三个月的数据
                if (reimburse) {
                    recordRepository.getLastThreeMonthReimbursableRecordList()
                } else {
                    recordRepository.getLastThreeMonthRefundableRecordList()
                }
            } else {
                if (reimburse) {
                    recordRepository.getLastThreeMonthReimbursableRecordListByKeyword("%$keyword%")
                } else {
                    recordRepository.getLastThreeMonthRefundableRecordListByKeyword("%$keyword%")
                }
            }.map {
                recordModelTransToViewsUseCase(it)
            }
        }
    }

}