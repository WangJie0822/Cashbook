package cn.wj.android.cashbook.domain.usecase

import cn.wj.android.cashbook.core.common.RECORD_TYPE_COLUMNS
import cn.wj.android.cashbook.core.common.tools.getIdByString
import cn.wj.android.cashbook.core.data.repository.TypeRepository
import cn.wj.android.cashbook.core.model.entity.RecordEntity
import cn.wj.android.cashbook.core.model.entity.RecordTypeEntity
import cn.wj.android.cashbook.core.model.model.RecordTypeModel
import javax.inject.Inject

/**
 * 获取分类列表数据用例
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/2/22
 */
class GetRecordTypeListUseCase @Inject constructor(
    private val typeRepository: TypeRepository
) {

    suspend operator fun invoke(
        recordEntity: RecordEntity
    ): List<RecordTypeEntity> {
        val selectedType = recordEntity.type
        // 获取一级分类列表
        val firstList: List<RecordTypeEntity> =
            typeRepository.getFirstRecordTypeListByCategory(recordEntity.typeCategory)
                .map { model ->
                    model.asEntity(
                        typeRepository.getSecondRecordTypeListByParentId(model.id)
                            .map { it.asEntity() }
                            .sortedBy { it.sort }
                    )
                }
                .sortedBy { it.sort }

        // 最终输出结果
        val result = arrayListOf<RecordTypeEntity>()
        // 二级分类插入位置
        var secondStartIndex = -1
        // 二级分类数据
        val secondList = arrayListOf<RecordTypeEntity>()
        // 更新选中状态
        firstList.forEachIndexed { index, first ->
            if (index == secondStartIndex) {
                // 到达二级分类插入位置
                result.addAll(secondList)
                // 补齐二级分类末尾
                repeat((0 until RECORD_TYPE_COLUMNS - secondList.size % RECORD_TYPE_COLUMNS).count()) {
                    result.add(
                        RecordTypeEntity(
                            id = -1L,
                            parentId = 0L,
                            name = "",
                            iconResId = 0,
                            sort = 0,
                            child = listOf(),
                            selected = false
                        )
                    )
                }
            }
            if (selectedType.parentId == -1L) {
                // 选中一级分类
                val selected = first.id == selectedType.id
                // 添加更新了选中状态的数据
                result.add(first.copy(selected = selected))
                if (selected) {
                    // 当前数据被选中，记录二级分类插入位置为下一行
                    secondStartIndex = index - index % RECORD_TYPE_COLUMNS + RECORD_TYPE_COLUMNS
                    // 记录需要插入的二级分类数据
                    secondList.clear()
                    secondList.addAll(first.child)
                }
            } else {
                // 选中二级分类
                val selected = first.id == selectedType.parentId
                // 添加更新了选中状态的数据
                result.add(first.copy(selected = selected))
                if (selected) {
                    // 当前数据被选中，记录二级分类插入位置为下一行
                    secondStartIndex = index - index % RECORD_TYPE_COLUMNS + RECORD_TYPE_COLUMNS
                    // 记录需要插入的二级分类数据
                    secondList.clear()
                    first.child.forEach {
                        secondList.add(it.copy(selected = it.id == selectedType.id))
                    }
                }
            }
        }
        if (secondStartIndex >= firstList.size) {
            // 一级分类在最后一行，二级分类需要另起一行
            // 补齐一级分类末尾
            repeat((0 until RECORD_TYPE_COLUMNS - firstList.size % RECORD_TYPE_COLUMNS).count()) {
                result.add(
                    RecordTypeEntity(
                        id = -1L,
                        parentId = -1L,
                        name = "",
                        iconResId = 0,
                        sort = 0,
                        child = listOf(),
                        selected = false
                    )
                )
            }
            // 添加二级分类
            result.addAll(secondList)
        }
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
        )
    }
}