package cn.wj.android.cashbook.data.entity

import cn.wj.android.cashbook.data.enums.TypeEnum

/**
 * 类型数据实体类
 *
 * @param id 主键自增长
 * @param parentId 子类时父类id
 * @param name 类型名称
 * @param iconResName 图标资源名称
 * @param type 类型类别
 * @param childList 子类型列表
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/8
 */
data class TypeEntity(
    val id: Long,
    val parentId: Long,
    val name: String,
    val iconResName: String,
    val type: TypeEnum,
    val childList: ArrayList<TypeEntity>
) {

    companion object {
        fun newFirst(name: String, iconResName: String): TypeEntity {
            return TypeEntity(
                id = -1L,
                parentId = -1L,
                name = name,
                iconResName = iconResName,
                type = TypeEnum.FIRST,
                childList = arrayListOf()
            )
        }

        fun newSecond(parentId: Long, name: String, iconResName: String): TypeEntity {
            return TypeEntity(
                id = -1L,
                parentId = parentId,
                name = name,
                iconResName = iconResName,
                type = TypeEnum.SECOND,
                childList = arrayListOf()
            )
        }
    }
}