package cn.wj.android.cashbook.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import cn.wj.android.cashbook.core.database.table.RecordTable

/**
 * 记录数据库操作类
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/2/20
 */
@Dao
interface RecordDao {

    @Query(
        value = """
        SELECT * FROM db_record WHERE id=:recordId
    """
    )
    suspend fun queryById(recordId: Long): RecordTable?

    @Query(
        value = """
        SELECT * FROM db_record WHERE books_id=:booksId AND record_time>=:dateTime
    """
    )
    suspend fun queryByBooksIdAfterDate(booksId: Long, dateTime: Long): List<RecordTable>
}