package cn.wj.android.cashbook.core.data.repository.impl

import cn.wj.android.cashbook.core.common.model.DataVersion
import cn.wj.android.cashbook.core.data.repository.AssetRepository
import cn.wj.android.cashbook.core.data.repository.asModel
import cn.wj.android.cashbook.core.database.dao.AssetDao
import cn.wj.android.cashbook.core.datastore.datasource.AppPreferencesDataSource
import cn.wj.android.cashbook.core.model.model.AssetModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext

/**
 * 资产类型数据仓库
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/2/20
 */
class AssetRepositoryImpl @Inject constructor(
    private val assetDao: AssetDao,
    appPreferencesDataSource: AppPreferencesDataSource,
) : AssetRepository {

    private val dataVersion: DataVersion = MutableStateFlow(0)

    override val currentVisibleAssetListData: Flow<List<AssetModel>> =
        combine(dataVersion, appPreferencesDataSource.appData) { _, appData ->
            getVisibleAssetsByBookId(appData.currentBookId)
        }

    override suspend fun getAssetById(assetId: Long): AssetModel? = withContext(Dispatchers.IO) {
        assetDao.queryAssetById(assetId)?.asModel()
    }

    override suspend fun getVisibleAssetsByBookId(bookId: Long): List<AssetModel> {
        return assetDao.queryVisibleAssetByBookId(bookId)
            .map { it.asModel() }
    }
}