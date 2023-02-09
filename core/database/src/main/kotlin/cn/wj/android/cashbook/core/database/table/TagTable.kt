package cn.wj.android.cashbook.core.database.table

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 标签数据表
 *
 * @param id 主键自增长
 * @param name 标签名称
 * @param booksId 所属账本主键
 * @param shared 是否是共享标签
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/23
 */
@Entity(tableName = "db_tag")
data class TagTable(
    @PrimaryKey(autoGenerate = true) val id: Long?,
    val name: String,
    @ColumnInfo(name = "books_id") val booksId: Long,
    val shared: Int
)