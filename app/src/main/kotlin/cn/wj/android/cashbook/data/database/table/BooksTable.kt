package cn.wj.android.cashbook.data.database.table

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * 账本数据表格
 *
 * @param id 账本 id 主键自增长
 * @param name 账本名
 * @param imageUrl 账本封面地址
 * @param description 描述
 * @param currency 默认货币
 * @param selected 是否默认选中
 * @param createTime 创建时间
 * @param modifyTime 修改时间
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/5/15
 */
@Serializable
@Entity(tableName = "db_books")
data class BooksTable(
    @PrimaryKey(autoGenerate = true)
    val id: Long?,
    val name: String,
    val imageUrl: String,
    val description: String,
    val currency: String,
    val selected: Int,
    @ColumnInfo(name = "create_time") val createTime: Long,
    @ColumnInfo(name = "modify_time") val modifyTime: Long
)