package cn.wj.android.cashbook.data.database.table

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 类型数据表
 *
 * @param id 主键自增长
 * @param parentId 子类时父类id
 * @param name 类型名称
 * @param iconResName 图标资源名称
 * @param type 类型类别 一级类别、二级类别
 * @param recordType 记录类型 收入、支出、转账
 * @param childEnable 是否允许子类型
 * @param refund 是否是退款
 * @param reimburse 是否是报销
 * @param sort 排序
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/8
 */
@Entity(tableName = "db_type")
data class TypeTable(
    @PrimaryKey(autoGenerate = true) val id: Long?,
    @ColumnInfo(name = "parent_id") val parentId: Long,
    val name: String,
    @ColumnInfo(name = "icon_res_name") val iconResName: String,
    val type: String,
    @ColumnInfo(name = "record_type") val recordType: Int,
    @ColumnInfo(name = "child_enable") val childEnable: Int,
    val refund: Int,
    val reimburse: Int,
    val sort: Int
)