package cn.wj.android.cashbook.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import cn.wj.android.cashbook.data.database.table.BooksTable

/**
 * 账本数据库访问接口
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/5/15
 */
@Dao
interface BooksDao {

    /** 新增账本数据 [books] 到数据库 */
    @Insert
    suspend fun insert(vararg books: BooksTable)

    /** 新增账本数据 [books] 到数据库 */
    @Insert
    suspend fun insert(books: BooksTable): Long

    /** 从数据库中删除账本数据 [books]*/
    @Delete
    suspend fun delete(vararg books: BooksTable)

    /** 更新数据库中的 [books] 数据 */
    @Update
    suspend fun update(vararg books: BooksTable)

    /** 获取 name 是 [name] 的数据数量 */
    @Query("SELECT COUNT(*) FROM db_books WHERE name=:name")
    suspend fun getCountByName(name: String): Long

    /** 从数据库中查询所有账本数据并返回 */
    @Query("SELECT * FROM db_books")
    suspend fun queryAll(): List<BooksTable>

    @Query("SELECT * FROM db_books WHERE selected=1")
    suspend fun queryDefault(): List<BooksTable>
}