package cn.wj.android.cashbook.core.data.repository

import cn.wj.android.cashbook.core.database.dao.AssetDao
import cn.wj.android.cashbook.core.database.table.AssetTable
import cn.wj.android.cashbook.core.model.model.AssetModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 资产类型数据仓库
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/2/20
 */
class AssetRepository @Inject constructor(
    private val assetDao: AssetDao,
) {

    suspend fun getAssetById(assetId: Long): AssetModel? = withContext(Dispatchers.IO){
        assetDao.queryAssetById(assetId)?.asModel()
    }

    private fun AssetTable.asModel(): AssetModel {
        return AssetModel()
    }

}