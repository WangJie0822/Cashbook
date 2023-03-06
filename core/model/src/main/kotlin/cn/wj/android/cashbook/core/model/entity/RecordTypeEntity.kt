package cn.wj.android.cashbook.core.model.entity

import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum

/**
 * 记录类型数据实体
 *
 * @param id 类型 id
 * @param parentId 二级分类父类型 id，一级类型默认为 -1L
 * @param name 类型名称
 * @param iconResId 类型图标资源 id
 * @param sort 排序关键字
 * @param child 一级分类的二级分类列表
 * @param selected 是否是选中状态
 * @param shapeType 形状类型，二级分类使用 -1：第一条 0：中间部分 1：最后一条
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/2/23
 */
data class RecordTypeEntity(
    val id: Long,
    val parentId: Long,
    val name: String,
    val iconResId: Int,
    val typeCategory: RecordTypeCategoryEnum,
    val sort: Int,
    val child: List<RecordTypeEntity>,
    val selected: Boolean,
    val shapeType: Int,
)

/** 固定类型 - 设置 */
val RECORD_TYPE_SETTINGS: RecordTypeEntity
    get() = RecordTypeEntity(
        id = -1001L,
        parentId = -1L,
        name = "",
        iconResId = 0,
        sort = 0,
        child = listOf(),
        selected = true,
        typeCategory = RecordTypeCategoryEnum.EXPENDITURE,
        shapeType = 0,
    )

