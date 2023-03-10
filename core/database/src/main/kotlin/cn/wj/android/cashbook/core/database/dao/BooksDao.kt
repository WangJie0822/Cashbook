package cn.wj.android.cashbook.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import cn.wj.android.cashbook.core.database.table.BooksTable

/**
 * 账本类型数据库操作类
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/3/9
 */
@Dao
interface BooksDao {

    @Query(
        value = """
        SELECT * FROM db_books
    """
    )
    suspend fun queryAll(): List<BooksTable>
}