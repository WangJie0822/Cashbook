package cn.wj.android.cashbook.data.database.table

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 账本数据表格
 *
 * @param id 账本 id 主键自增长
 * @param name 账本名
 * @param imageUrl 账本封面地址
 * @param amount 账本金额
 * @param sort 排序字段
 * @param selected 是否默认选中
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/5/15
 */
@Entity(tableName = "db_books")
data class BooksTable(
    @PrimaryKey(autoGenerate = true)
    val id: Long?,
    val name: String,
    val imageUrl: String,
    val amount: String,
    val sort: Int,
    val selected: Int
)