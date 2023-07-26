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
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/23
 */
@Entity(tableName = TABLE_TAG)
data class TagTable(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = TABLE_TAG_ID) val id: Long?,
    @ColumnInfo(name = TABLE_TAG_NAME) val name: String,
    @ColumnInfo(name = TABLE_TAG_BOOKS_ID) val booksId: Long,
)