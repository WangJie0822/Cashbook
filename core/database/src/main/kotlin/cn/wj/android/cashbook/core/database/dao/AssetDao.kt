package cn.wj.android.cashbook.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import cn.wj.android.cashbook.core.database.table.AssetTable
import kotlinx.coroutines.flow.Flow

/**
 * 资产数据库操作类
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/2/20
 */
@Dao
interface AssetDao {

    @Query("SELECT * FROM db_asset WHERE id=:assetId")
    suspend fun queryAssetById(assetId: Long): AssetTable?

    @Query("SELECT * FROM db_asset WHERE books_id=:bookId")
    suspend fun queryVisibleAssetByBookId(bookId: Long): List<AssetTable>
}