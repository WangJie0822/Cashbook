package cn.wj.android.cashbook.domain.usecase

import android.util.Log
import cn.wj.android.cashbook.core.common.ext.logger
import cn.wj.android.cashbook.core.common.tools.getIdByString
import cn.wj.android.cashbook.core.data.repository.TypeRepository
import cn.wj.android.cashbook.core.model.entity.RECORD_TYPE_SETTINGS
import cn.wj.android.cashbook.core.model.entity.RecordTypeEntity
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.model.model.RecordTypeModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
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
    private val typeRepository: TypeRepository
) {

    suspend operator fun invoke(
        typeCategory: RecordTypeCategoryEnum,
        selectedType: RecordTypeEntity?
    ): List<RecordTypeEntity> = withContext(Dispatchers.IO) {
        when (typeCategory) {
            RecordTypeCategoryEnum.EXPENDITURE -> typeRepository.firstExpenditureTypeListData
            RecordTypeCategoryEnum.INCOME -> typeRepository.firstIncomeTypeListData
            RecordTypeCategoryEnum.TRANSFER -> typeRepository.firstTransferTypeListData
        }
            .map { list ->
                logger("111").d(list.toString())
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
                logger("222").d(selectedType.toString() + "  ---   " + list.toString())
                val selectedEntity = selectedType ?: list.first().child.first()
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
                            logger("444").d(second)
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
                logger("333").d(result.toString())
                result.toList()
            }
            .first()
    }
}

fun RecordTypeModel.asEntity(
    child: List<RecordTypeEntity> = listOf()
): RecordTypeEntity {
    return RecordTypeEntity(
        id = this.id,
        parentId = this.parentId,
        name = this.name,
        iconResId = getIdByString(this.iconName, "drawable"),
        sort = this.sort,
        typeCategory = this.typeCategory,
        child = child,
        selected = false,
        shapeType = -1,
    )
}