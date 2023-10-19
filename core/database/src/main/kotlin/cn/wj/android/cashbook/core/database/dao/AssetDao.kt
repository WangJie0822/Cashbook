package cn.wj.android.cashbook.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import cn.wj.android.cashbook.core.common.SWITCH_INT_OFF
import cn.wj.android.cashbook.core.common.SWITCH_INT_ON
import cn.wj.android.cashbook.core.database.table.AssetTable
import kotlinx.coroutines.flow.Flow

/**
 * 资产数据库操作类
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/2/20
 */
@Dao
interface AssetDao {

    @Insert
    suspend fun insert(assetTable: AssetTable)

    @Update
    suspend fun update(assetTable: AssetTable)

    @Query("SELECT * FROM db_asset WHERE id=:assetId")
    suspend fun queryAssetById(assetId: Long): AssetTable?

    @Query("SELECT * FROM db_asset WHERE books_id=:bookId AND invisible=$SWITCH_INT_OFF")
    suspend fun queryVisibleAssetByBookId(bookId: Long): List<AssetTable>

    @Query("SELECT * FROM db_asset WHERE books_id=:bookId AND invisible=$SWITCH_INT_ON")
    suspend fun queryInvisibleAssetByBookId(bookId: Long): List<AssetTable>

    @Query(value = """
        DELETE FROM db_asset WHERE id=:assetId
    """)
    suspend fun deleteById(assetId: Long)

    @Query(value = """
        UPDATE db_asset SET invisible=$SWITCH_INT_OFF WHERE id=:id
    """)
    suspend fun visibleById(id:Long)
}