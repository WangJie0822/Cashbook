package cn.wj.android.cashbook.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import cn.wj.android.cashbook.data.constants.SWITCH_INT_OFF
import cn.wj.android.cashbook.data.constants.SWITCH_INT_ON
import cn.wj.android.cashbook.data.database.table.AssetTable
import cn.wj.android.cashbook.data.live.CurrentBooksLiveData

/**
 * 资产相关数据库操作接口
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/3
 */
@Dao
interface AssetDao {

    /** 插入新的资产 [asset] 到数据库并返回主键id */
    @Insert
    fun insert(asset: AssetTable): Long

    /** 插入或替换资产 */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrReplace(vararg assets: AssetTable)

    /** 删除资产 [asset] */
    @Delete
    fun delete(asset: AssetTable)

    /** 更新资产 [asset] 到数据库 */
    @Update
    fun update(asset: AssetTable)

    /** 更新资产 [assets] */
    @Update
    fun update(vararg assets: AssetTable)

    /** 查询所有数据 */
    @Query("SELECT * FROM db_asset")
    fun queryAll(): List<AssetTable>

    /** 根据 [booksId] 从数据库中查询所有资产数据并返回 */
    @Query("SELECT * FROM db_asset WHERE books_id=:booksId")
    fun queryByBooksId(booksId: Long = CurrentBooksLiveData.booksId): List<AssetTable>

    /** 根据 [booksId] 从数据库中查询隐藏资产数据并返回 */
    @Query("SELECT * FROM db_asset WHERE books_id=:booksId AND invisible=${SWITCH_INT_ON}")
    fun queryInvisibleByBooksId(booksId: Long = CurrentBooksLiveData.booksId): List<AssetTable>

    /** 根据 [booksId] 从数据库中查询未隐藏资产数据并返回 */
    @Query("SELECT * FROM db_asset WHERE books_id=:booksId AND invisible=${SWITCH_INT_OFF}")
    fun queryVisibleByBooksId(booksId: Long = CurrentBooksLiveData.booksId): List<AssetTable>

    /** 从数据库中查询账本id为 [booksId] 的资产中最大的排序 */
    @Query("SELECT MAX(sort) FROM db_asset WHERE books_id=:booksId")
    fun queryMaxSortByBooksId(booksId: Long = CurrentBooksLiveData.booksId): Int?

    /** 获取 id 为 [id] 的资产 */
    @Query("SELECT * FROM db_asset WHERE id=:id AND books_id=:booksId")
    fun queryById(id: Long, booksId: Long = CurrentBooksLiveData.booksId): AssetTable?
}