package cn.wj.android.cashbook.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import cn.wj.android.cashbook.data.constants.SWITCH_INT_ON
import cn.wj.android.cashbook.data.database.table.AssetTable

/**
 * 资产相关数据库操作接口
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/3
 */
@Dao
interface AssetDao {

    /** 插入新的资产 [asset] 到数据库并返回主键id */
    @Insert
    suspend fun insert(asset: AssetTable): Long

    /** 更新资产 [asset] 到数据库 */
    @Update
    suspend fun update(asset: AssetTable)

    /** 根据 [booksId] 从数据库中查询所有资产数据并返回 */
    @Query("SELECT * FROM db_asset WHERE books_id=:booksId")
    suspend fun queryByBooksId(booksId: Long): List<AssetTable>

    /** 根据 [booksId] 从数据库中查询隐藏资产数据并返回 */
    @Query("SELECT * FROM db_asset WHERE books_id=:booksId AND invisible=${SWITCH_INT_ON}")
    suspend fun queryInvisibleByBooksId(booksId: Long): List<AssetTable>
}