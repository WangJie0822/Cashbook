package cn.wj.android.cashbook.core.model.model

import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.model.enums.TypeLevelEnum

/**
 * 记录类型数据模型
 *
 * @param id 主键自增长
 * @param parentId 子类时父类 id
 * @param name 类型名称
 * @param iconName 图标资源信息
 * @param typeLevel 类型等级
 * @param typeCategory 类型分类
 * @param protected 是否受保护
 * @param sort 排序
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/8
 */
data class RecordTypeModel(
    val id: Long,
    val parentId: Long,
    val name: String,
    val iconName: String,
    val typeLevel: TypeLevelEnum,
    val typeCategory: RecordTypeCategoryEnum,
    val protected: Boolean,
    val sort: Int,
)
