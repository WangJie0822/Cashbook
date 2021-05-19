package cn.wj.android.cashbook.data.database.table

import androidx.room.Entity
import androidx.room.PrimaryKey
import cn.wj.android.cashbook.base.ext.base.orElse
import cn.wj.android.cashbook.data.contants.SWITCH_INT_OFF
import cn.wj.android.cashbook.data.contants.SWITCH_INT_ON
import cn.wj.android.cashbook.data.entity.BooksEntity

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

/** 将数据库数据转换为对应数据实体类 */
internal fun BooksTable.toBooksEntity(): BooksEntity {
    return BooksEntity(
        id = id.orElse(-1L),
        name = name,
        imageUrl = imageUrl,
        amount = amount,
        sort = sort,
        selected = selected == SWITCH_INT_ON
    )
}

/** 将数据实体类转换为对应数据库数据 */
internal fun BooksEntity.toBooksTable(): BooksTable {
    return BooksTable(
        id = if (-1L == id) null else id,
        name = name,
        imageUrl = imageUrl,
        amount = amount,
        sort = sort,
        selected = if (selected) SWITCH_INT_ON else SWITCH_INT_OFF
    )
}