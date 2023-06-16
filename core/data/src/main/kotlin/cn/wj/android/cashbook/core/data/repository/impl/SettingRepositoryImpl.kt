package cn.wj.android.cashbook.core.data.repository.impl

import cn.wj.android.cashbook.core.data.repository.SettingRepository
import cn.wj.android.cashbook.core.datastore.datasource.AppPreferencesDataSource
import cn.wj.android.cashbook.core.model.entity.UpdateInfoEntity
import cn.wj.android.cashbook.core.model.model.AppDataModel
import cn.wj.android.cashbook.core.network.datasource.NetworkDataSource
import cn.wj.android.cashbook.core.network.entity.GitReleaseEntity
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * 设置相关数据仓库
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/6/14
 */
class SettingRepositoryImpl @Inject constructor(
    private val appPreferencesDataSource: AppPreferencesDataSource,
    private val networkDataSource: NetworkDataSource,
) : SettingRepository {

    override val appDataMode: Flow<AppDataModel> = appPreferencesDataSource.appData

    override suspend fun updateUseGithub(
        useGithub: Boolean,
        coroutineContext: CoroutineContext
    ) = withContext(coroutineContext) {
        appPreferencesDataSource.updateUseGithub(useGithub)
    }

    override suspend fun updateAutoCheckUpdate(
        autoCheckUpdate: Boolean,
        coroutineContext: CoroutineContext
    ) = withContext(coroutineContext) {
        appPreferencesDataSource.updateAutoCheckUpdate(autoCheckUpdate)
    }

    override suspend fun updateIgnoreUpdateVersion(
        ignoreUpdateVersion: String,
        coroutineContext: CoroutineContext
    ) = withContext(coroutineContext) {
        appPreferencesDataSource.updateIgnoreUpdateVersion(ignoreUpdateVersion)
    }

    override suspend fun updateMobileNetworkDownloadEnable(
        mobileNetworkDownloadEnable: Boolean,
        coroutineContext: CoroutineContext
    ) = withContext(coroutineContext) {
        appPreferencesDataSource.updateMobileNetworkDownloadEnable(mobileNetworkDownloadEnable)
    }

    override suspend fun checkUpdate(
        coroutineContext: CoroutineContext
    ): UpdateInfoEntity = withContext(coroutineContext) {
        networkDataSource.checkUpdate(!appPreferencesDataSource.appData.first().useGithub)
            .toUpdateInfoEntity()
    }
}

private fun GitReleaseEntity.toUpdateInfoEntity(): UpdateInfoEntity {
    val asset = assets?.firstOrNull {
        it.name?.endsWith(".apk") ?: false
    }
    return UpdateInfoEntity(
        versionName = name.orEmpty(),
        versionInfo = body.orEmpty(),
        apkName = asset?.name.orEmpty(),
        downloadUrl = asset?.downloadUrl.orEmpty()
    )
}