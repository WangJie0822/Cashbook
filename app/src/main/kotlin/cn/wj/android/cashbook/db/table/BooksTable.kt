package cn.wj.android.cashbook.db.table

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 账本数据表格
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/5/15
 */
@Entity(tableName = "db_books")
data class BooksTable(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    val name: String
)