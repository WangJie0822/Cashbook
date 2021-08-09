package cn.wj.android.cashbook.data.database.table

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * 标签数据表
 *
 * @param id 主键自增长
 * @param name 标签名称
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/23
 */
@Serializable
@Entity(tableName = "db_tag")
data class TagTable(
    @PrimaryKey(autoGenerate = true) val id: Long?,
    val name: String,
)