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
 * @param type 类型类别
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/8
 */
@Entity(tableName = "db_type")
data class TypeTable(
    @PrimaryKey(autoGenerate = true) val id: Long?,
    @ColumnInfo(name = "parent_id") val parentId: Long,
    val name: String,
    @ColumnInfo(name = "icon_res_name") val iconResName: String,
    val type: String
)