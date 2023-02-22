package cn.wj.android.cashbook.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import cn.wj.android.cashbook.core.database.table.AssetTable
import cn.wj.android.cashbook.core.database.table.TypeTable
import kotlinx.coroutines.flow.Flow

/**
 * 资产数据库操作类
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/2/20
 */
@Dao
interface AssetDao {

    @Query("SELECT * FROM db_asset WHERE id=:assetId")
    suspend fun queryAssetById(assetId:Long):AssetTable?
}