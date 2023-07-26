package cn.wj.android.cashbook.core.database.table

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 账本数据表格
 *
 * @param id 账本 id 主键自增长
 * @param name 账本名
 * @param description 描述
 * @param modifyTime 修改时间
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/5/15
 */
@Entity(tableName = TABLE_BOOKS)
data class BooksTable(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = TABLE_BOOKS_ID) val id: Long?,
    @ColumnInfo(name = TABLE_BOOKS_NAME) val name: String,
    @ColumnInfo(name = TABLE_BOOKS_DESCRIPTION) val description: String,
    @ColumnInfo(name = TABLE_BOOKS_MODIFY_TIME) val modifyTime: Long
)