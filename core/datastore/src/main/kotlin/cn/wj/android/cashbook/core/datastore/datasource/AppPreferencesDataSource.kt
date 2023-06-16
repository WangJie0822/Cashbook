package cn.wj.android.cashbook.core.datastore.datasource

import androidx.datastore.core.DataStore
import cn.wj.android.cashbook.core.datastore.AppPreferences
import cn.wj.android.cashbook.core.datastore.copy
import cn.wj.android.cashbook.core.model.model.AppDataModel
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * 应用配置数据源
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/2/21
 */
@Singleton
class AppPreferencesDataSource @Inject constructor(
    private val appPreferences: DataStore<AppPreferences>
) {

    val appData = appPreferences.data
        .map {
            AppDataModel(
                currentBookId = it.currentBookId,
                defaultTypeId = it.defaultTypeId,
                lastAssetId = it.lastAssetId,
                refundTypeId = it.refundTypeId,
                reimburseTypeId = it.refundTypeId,
                useGithub = it.useGithub,
                autoCheckUpdate = it.autoCheckUpdate,
                ignoreUpdateVersion = it.ignoreUpdateVersion,
                mobileNetworkDownloadEnable = it.mobileNetworkDownloadEnable,
            )
        }

    suspend fun updateCurrentBookId(bookId: Long) {
        appPreferences.updateData { it.copy { this.currentBookId = bookId } }
    }

    suspend fun updateLastAssetId(lastAssetId: Long) {
        appPreferences.updateData { it.copy { this.lastAssetId = lastAssetId } }
    }

    suspend fun updateUseGithub(useGithub: Boolean) {
        appPreferences.updateData { it.copy { this.useGithub = useGithub } }
    }

    suspend fun updateAutoCheckUpdate(autoCheckUpdate: Boolean) {
        appPreferences.updateData { it.copy { this.autoCheckUpdate = autoCheckUpdate } }
    }

    suspend fun updateIgnoreUpdateVersion(ignoreUpdateVersion: String) {
        appPreferences.updateData { it.copy { this.ignoreUpdateVersion = ignoreUpdateVersion } }
    }

    suspend fun updateMobileNetworkDownloadEnable(mobileNetworkDownloadEnable: Boolean) {
        appPreferences.updateData {
            it.copy {
                this.mobileNetworkDownloadEnable = mobileNetworkDownloadEnable
            }
        }
    }

    suspend fun needRelated(typeId: Long): Boolean {
        val appDataModel = appData.first()
        return typeId == appDataModel.reimburseTypeId || typeId == appDataModel.refundTypeId
    }
}