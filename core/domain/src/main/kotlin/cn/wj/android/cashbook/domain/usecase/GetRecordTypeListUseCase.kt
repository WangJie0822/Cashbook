package cn.wj.android.cashbook.domain.usecase

import cn.wj.android.cashbook.core.common.annotation.CashbookDispatchers
import cn.wj.android.cashbook.core.common.annotation.Dispatcher
import cn.wj.android.cashbook.core.data.repository.TypeRepository
import cn.wj.android.cashbook.core.model.entity.RECORD_TYPE_SETTINGS
import cn.wj.android.cashbook.core.model.entity.RecordTypeEntity
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.model.transfer.asEntity
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * 获取记录类型列表数据用例
 *
 * @param typeRepository 类型数据仓库
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/2/22
 */
class GetRecordTypeListUseCase @Inject constructor(
    private val typeRepository: TypeRepository,
    @Dispatcher(CashbookDispatchers.IO) private val ioCoroutineContext: CoroutineContext,
) {

    suspend operator fun invoke(
        typeCategory: RecordTypeCategoryEnum,
        selectedTypeId: Long,
    ): List<RecordTypeEntity> = withContext(ioCoroutineContext) {
        when (typeCategory) {
            RecordTypeCategoryEnum.EXPENDITURE -> typeRepository.firstExpenditureTypeListData
            RecordTypeCategoryEnum.INCOME -> typeRepository.firstIncomeTypeListData
            RecordTypeCategoryEnum.TRANSFER -> typeRepository.firstTransferTypeListData
        }
            .map { list ->
                list.map { model ->
                    model.asEntity(
                        child = typeRepository.getSecondRecordTypeListByParentId(model.id)
                            .map { it.asEntity() }
                            .sortedBy { it.sort }
                    )
                }
                    .sortedBy { it.sort }
            }
            .map { list ->
                var selectedEntity = typeRepository.getRecordTypeById(selectedTypeId)?.asEntity()
                if (null == selectedEntity || selectedEntity.typeCategory != typeCategory) {
                    selectedEntity = list.first()
                }
                // 最终输出结果
                val result = arrayListOf<RecordTypeEntity>()
                // 是否选中一级类型
                val selectFirst = selectedEntity.parentId == -1L
                // 更新选中状态
                list.forEach { first ->
                    // 判断当前类型是否选中
                    val selected = if (selectFirst) {
                        first.id == selectedEntity.id
                    } else {
                        first.id == selectedEntity.parentId
                    }
                    // 更新类型选中状态并添加到结果中
                    result.add(first.copy(selected = selected))
                    if (selected) {
                        // 如果一级类型选中，向后面添加它的二级类型
                        val childCount = first.child.size
                        first.child.forEachIndexed { index, second ->
                            // 二级分类是否选中
                            val secondSelected = if (selectFirst) {
                                false
                            } else {
                                second.id == selectedEntity.id
                            }
                            // 判断是第一个还是最后一个
                            val shapeType = when (index) {
                                0 -> -1
                                childCount - 1 -> 1
                                else -> 0
                            }
                            result.add(
                                second.copy(
                                    selected = secondSelected,
                                    shapeType = shapeType
                                )
                            )
                        }
                    }
                }
                // 在末尾添加设置数据
                result.add(RECORD_TYPE_SETTINGS)
                if (typeCategory == RecordTypeCategoryEnum.INCOME) {
                    // 更新退款、报销类型标记
                    result.map {
                        it.copy(needRelated = typeRepository.needRelated(it.id))
                    }
                } else {
                    result
                }
            }
            .first()
    }
}