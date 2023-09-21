package cn.wj.android.cashbook.domain.usecase

import cn.wj.android.cashbook.core.data.repository.RecordRepository
import cn.wj.android.cashbook.core.model.entity.RecordViewsEntity
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
    private val recordModelTransToViewsUseCase: RecordModelTransToViewsUseCase,
) {

    operator fun invoke(year: String, month: String): Flow<List<RecordViewsEntity>> =
        recordRepository.queryRecordByYearMonth(year, month).mapLatest { list ->
            list.map {
                recordModelTransToViewsUseCase(it)
            }
        }

}