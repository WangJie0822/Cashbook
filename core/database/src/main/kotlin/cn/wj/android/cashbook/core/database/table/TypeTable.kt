package cn.wj.android.cashbook.core.database.table

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 类型数据表
 *
 * @param id 主键自增长
 * @param parentId 子类时父类id
 * @param name 类型名称
 * @param iconName 图标名称
 * @param typeLevel 类型等级
 * @param typeCategory 类型分类 收入、支出、转账
 * @param protected 是否是受保护的
 * @param sort 排序
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/8
 */
@Entity(tableName = TABLE_TYPE)
data class TypeTable(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = TABLE_TYPE_ID) val id: Long?,
    @ColumnInfo(name = TABLE_TYPE_PARENT_ID) val parentId: Long,
    @ColumnInfo(name = TABLE_TYPE_NAME) val name: String,
    @ColumnInfo(name = TABLE_TYPE_ICON_NAME) val iconName: String,
    @ColumnInfo(name = TABLE_TYPE_TYPE_LEVEL) val typeLevel: Int,
    @ColumnInfo(name = TABLE_TYPE_TYPE_CATEGORY) val typeCategory: Int,
    @ColumnInfo(name = TABLE_TYPE_PROTECTED) val protected: Int,
    @ColumnInfo(name = TABLE_TYPE_SORT) val sort: Int
)