package cn.wj.android.cashbook.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import cn.wj.android.cashbook.data.database.table.TagTable
import cn.wj.android.cashbook.data.live.CurrentBooksLiveData

/**
 * 标签数据库访问接口
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/23
 */
@Dao
interface TagDao {

    /** 将 [tag] 数据插入数据库并返回主键 id */
    @Insert
    fun insert(tag: TagTable): Long

    /** 插入或替换标签 */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrReplace(vararg tags: TagTable)

    /** 删除 [tag] 数据 */
    @Delete
    fun delete(tag: TagTable)

    /** 更新 [tag] 数据 */
    @Update
    fun update(tag: TagTable)

    /** 从数据库中获取所有标签数据并返回 */
    @Query("SELECT * FROM db_tag")
    fun queryAll(): List<TagTable>

    /** 从数据库中获取账本 id 为 [booksId] 的标签数据并返回 */
    @Query("SELECT * FROM db_tag WHERE books_id=:booksId OR shared=1")
    fun getTagListByBooksId(booksId: Long = CurrentBooksLiveData.booksId): List<TagTable>

    /** 根据 [id] 查询并返回标签数据 */
    @Query("SELECT * FROM db_tag WHERE id=:id")
    fun queryById(id: Long): TagTable?

    /** 根据 [name] 查询并返回标签数据 */
    @Query("SELECT * FROM db_tag WHERE name=:name")
    fun queryByName(name: String): List<TagTable>
}