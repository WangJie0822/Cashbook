package cn.wj.android.cashbook.domain.usecase

import cn.wj.android.cashbook.core.common.tools.getIdByString
import cn.wj.android.cashbook.core.data.repository.TypeRepository
import cn.wj.android.cashbook.core.model.entity.RECORD_TYPE_SETTINGS
import cn.wj.android.cashbook.core.model.entity.RecordEntity
import cn.wj.android.cashbook.core.model.entity.RecordTypeEntity
import cn.wj.android.cashbook.core.model.model.RecordTypeModel
import javax.inject.Inject

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

    /** 更具当前记录数据 [recordEntity] 获取对应显示的类型列表并返回 */
    suspend operator fun invoke(
        recordEntity: RecordEntity
    ): List<RecordTypeEntity> {
        // 当前选择的类型
        val selectedType = recordEntity.type
        // 获取一级类型列表，补完对应的二级类型数据，并通过 sort 字段进行排序
        val firstList: List<RecordTypeEntity> =
            typeRepository.getFirstRecordTypeListByCategory(recordEntity.typeCategory)
                .map { model ->
                    model.asEntity(
                        child = typeRepository.getSecondRecordTypeListByParentId(model.id)
                            .map { it.asEntity() }
                            .sortedBy { it.sort }
                    )
                }
                .sortedBy { it.sort }

        // 最终输出结果
        val result = arrayListOf<RecordTypeEntity>()
        // 是否选中一级类型
        val selectFirst = selectedType.parentId == -1L
        // 更新选中状态
        firstList.forEach { first ->
            // 判断当前类型是否选中
            val selected = if (selectFirst) {
                first.id == selectedType.id
            } else {
                first.id == selectedType.parentId
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
                        second.id == selectedType.id
                    }
                    // 判断是第一个还是最后一个
                    val shapeType = when (index) {
                        0 -> -1
                        childCount - 1 -> 1
                        else -> 0
                    }
                    result.add(second.copy(selected = secondSelected, shapeType = shapeType))
                }
            }
        }
        // 在末尾添加设置数据
        result.add(RECORD_TYPE_SETTINGS)
        return result

    }

    private fun RecordTypeModel.asEntity(
        child: List<RecordTypeEntity> = listOf()
    ): RecordTypeEntity {
        return RecordTypeEntity(
            id = this.id,
            parentId = this.parentId,
            name = this.name,
            iconResId = getIdByString(this.iconName, "drawable"),
            sort = this.sort,
            child = child,
            selected = false,
            shapeType = -1,
        )
    }
}