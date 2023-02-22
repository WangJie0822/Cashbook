package cn.wj.android.cashbook.core.model.model

/**
 * 分类图标分组数据实体类
 *
 * @param name 分组名称
 * @param icons 图标数据列表
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/30
 */
data class TypeIconGroupEntity(
    val name: Int,
    val icons: List<TypeIconModel>
)

/**
 * 图标数据实体类
 *
 * @param name 名称
 * @param iconResIdStr 图标资源id
 */
data class TypeIconModel(
    val name: Int,
    val iconResIdStr: String
)