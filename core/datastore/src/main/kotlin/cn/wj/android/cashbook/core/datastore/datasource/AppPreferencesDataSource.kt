package cn.wj.android.cashbook.core.datastore.datasource

import androidx.datastore.core.DataStore
import cn.wj.android.cashbook.core.datastore.AppPreferences
import cn.wj.android.cashbook.core.model.model.AppDataModel
import javax.inject.Inject
import kotlinx.coroutines.flow.map

/**
 * 应用配置数据源
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/2/21
 */
class AppPreferencesDataSource @Inject constructor(
    private val appPreferences: DataStore<AppPreferences>
) {

    val appData = appPreferences.data
        .map {
            AppDataModel(
                currentBookId = it.currentBookId,
                defaultTypeId = it.defaultTypeId,
                lastAssetId = it.lastAssetId,
            )
        }

}