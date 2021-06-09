package cn.wj.android.cashbook.data.entity

import cn.wj.android.cashbook.base.tools.dateFormat
import cn.wj.android.cashbook.data.enums.RecordTypeEnum
import cn.wj.android.cashbook.data.enums.TypeEnum

/**
 * 类型数据实体类
 *
 * @param id 主键自增长
 * @param parentId 子类时父类id
 * @param name 类型名称
 * @param iconResName 图标资源名称
 * @param type 类型类别
 * @param recordType 记录类型
 * @param createTime 创建时间
 * @param modifyTime 修改时间
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
    val recordType: RecordTypeEnum,
    val childEnable: Boolean,
    val createTime: String,
    val modifyTime: String,
    val childList: List<TypeEntity>
) {

    /** 是否显示更多图标 */
    val showMore: Boolean
        get() = type == TypeEnum.FIRST && childEnable && childList.isNotEmpty()

    companion object {
        fun newFirstExpenditure(name: String, iconResName: String, childEnable: Boolean = true): TypeEntity {
            val time = System.currentTimeMillis().dateFormat()
            return TypeEntity(
                id = -1L,
                parentId = -1L,
                name = name,
                iconResName = iconResName,
                type = TypeEnum.FIRST,
                recordType = RecordTypeEnum.EXPENDITURE,
                childEnable = childEnable,
                createTime = time,
                modifyTime = time,
                childList = arrayListOf()
            )
        }

        fun newSecondExpenditure(parentId: Long, name: String, iconResName: String): TypeEntity {
            val time = System.currentTimeMillis().dateFormat()
            return TypeEntity(
                id = -1L,
                parentId = parentId,
                name = name,
                iconResName = iconResName,
                type = TypeEnum.SECOND,
                recordType = RecordTypeEnum.EXPENDITURE,
                childEnable = false,
                createTime = time,
                modifyTime = time,
                childList = arrayListOf()
            )
        }

        fun newFirstIncome(name: String, iconResName: String, childEnable: Boolean = true): TypeEntity {
            val time = System.currentTimeMillis().dateFormat()
            return TypeEntity(
                id = -1L,
                parentId = -1L,
                name = name,
                iconResName = iconResName,
                type = TypeEnum.FIRST,
                recordType = RecordTypeEnum.INCOME,
                childEnable = childEnable,
                createTime = time,
                modifyTime = time,
                childList = arrayListOf()
            )
        }

        fun newSecondIncome(parentId: Long, name: String, iconResName: String): TypeEntity {
            val time = System.currentTimeMillis().dateFormat()
            return TypeEntity(
                id = -1L,
                parentId = parentId,
                name = name,
                iconResName = iconResName,
                type = TypeEnum.SECOND,
                recordType = RecordTypeEnum.INCOME,
                childEnable = false,
                createTime = time,
                modifyTime = time,
                childList = arrayListOf()
            )
        }

        fun newFirstTransfer(name: String, iconResName: String, childEnable: Boolean = true): TypeEntity {
            val time = System.currentTimeMillis().dateFormat()
            return TypeEntity(
                id = -1L,
                parentId = -1L,
                name = name,
                iconResName = iconResName,
                type = TypeEnum.FIRST,
                recordType = RecordTypeEnum.TRANSFER,
                childEnable = childEnable,
                createTime = time,
                modifyTime = time,
                childList = arrayListOf()
            )
        }

        fun newSecondTransfer(parentId: Long, name: String, iconResName: String): TypeEntity {
            val time = System.currentTimeMillis().dateFormat()
            return TypeEntity(
                id = -1L,
                parentId = parentId,
                name = name,
                iconResName = iconResName,
                type = TypeEnum.SECOND,
                recordType = RecordTypeEnum.TRANSFER,
                childEnable = false,
                createTime = time,
                modifyTime = time,
                childList = arrayListOf()
            )
        }
    }
}