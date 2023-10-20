package cn.wj.android.cashbook.core.data.repository.impl

import cn.wj.android.cashbook.core.common.annotation.CashbookDispatchers
import cn.wj.android.cashbook.core.common.annotation.Dispatcher
import cn.wj.android.cashbook.core.common.ext.logger
import cn.wj.android.cashbook.core.data.repository.SettingRepository
import cn.wj.android.cashbook.core.datastore.datasource.AppPreferencesDataSource
import cn.wj.android.cashbook.core.datastore.datasource.GitInfosDataSource
import cn.wj.android.cashbook.core.model.entity.UpgradeInfoEntity
import cn.wj.android.cashbook.core.model.enums.AutoBackupModeEnum
import cn.wj.android.cashbook.core.model.enums.DarkModeEnum
import cn.wj.android.cashbook.core.model.enums.VerificationModeEnum
import cn.wj.android.cashbook.core.model.model.AppDataModel
import cn.wj.android.cashbook.core.model.model.GitDataModel
import cn.wj.android.cashbook.core.network.datasource.NetworkDataSource
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
    private val gitInfosDataSource: GitInfosDataSource,
    private val networkDataSource: NetworkDataSource,
    @Dispatcher(CashbookDispatchers.IO) private val coroutineContext: CoroutineContext,
) : SettingRepository {

    override val appDataMode: Flow<AppDataModel> = appPreferencesDataSource.appData

    override val gitDataModel: Flow<GitDataModel> = gitInfosDataSource.gitData

    override suspend fun updateUseGithub(useGithub: Boolean) = withContext(coroutineContext) {
        appPreferencesDataSource.updateUseGithub(useGithub)
    }

    override suspend fun updateAutoCheckUpdate(autoCheckUpdate: Boolean) =
        withContext(coroutineContext) {
            appPreferencesDataSource.updateAutoCheckUpdate(autoCheckUpdate)
        }

    override suspend fun updateIgnoreUpdateVersion(ignoreUpdateVersion: String) =
        withContext(coroutineContext) {
            appPreferencesDataSource.updateIgnoreUpdateVersion(ignoreUpdateVersion)
        }

    override suspend fun updateMobileNetworkDownloadEnable(mobileNetworkDownloadEnable: Boolean) =
        withContext(coroutineContext) {
            appPreferencesDataSource.updateMobileNetworkDownloadEnable(mobileNetworkDownloadEnable)
        }

    override suspend fun updateNeedSecurityVerificationWhenLaunch(needSecurityVerificationWhenLaunch: Boolean) =
        withContext(coroutineContext) {
            appPreferencesDataSource.updateNeedSecurityVerificationWhenLaunch(
                needSecurityVerificationWhenLaunch
            )
        }

    override suspend fun updateEnableFingerprintVerification(enableFingerprintVerification: Boolean) =
        withContext(coroutineContext) {
            appPreferencesDataSource.updateEnableFingerprintVerification(
                enableFingerprintVerification
            )
        }

    override suspend fun updatePasswordIv(iv: String) =
        withContext(coroutineContext) {
            appPreferencesDataSource.updatePasswordIv(iv)
        }

    override suspend fun updateFingerprintIv(iv: String) =
        withContext(coroutineContext) {
            appPreferencesDataSource.updateFingerprintIv(iv)
        }

    override suspend fun updatePasswordInfo(passwordInfo: String) = withContext(coroutineContext) {
        appPreferencesDataSource.updatePasswordInfo(passwordInfo)
    }

    override suspend fun updateFingerprintPasswordInfo(fingerprintPasswordInfo: String) =
        withContext(coroutineContext) {
            appPreferencesDataSource.updateFingerprintPasswordInfo(fingerprintPasswordInfo)
        }

    override suspend fun updateDarkMode(darkModeEnum: DarkModeEnum) =
        withContext(coroutineContext) {
            appPreferencesDataSource.updateDarkMode(darkModeEnum)
        }

    override suspend fun updateDynamicColor(dynamicColor: Boolean) = withContext(coroutineContext) {
        appPreferencesDataSource.updateDynamicColor(dynamicColor)
    }

    override suspend fun updateVerificationMode(verificationMode: VerificationModeEnum) =
        withContext(coroutineContext) {
            appPreferencesDataSource.updateVerificationMode(verificationMode)
        }

    override suspend fun updateAgreedProtocol(agreedProtocol: Boolean) =
        withContext(coroutineContext) {
            appPreferencesDataSource.updateAgreedProtocol(agreedProtocol)
        }

    override suspend fun checkUpdate(): UpgradeInfoEntity = withContext(coroutineContext) {
        val gitDataModel = gitDataModel.first()
        UpgradeInfoEntity(
            versionName = gitDataModel.latestVersionName,
            versionInfo = gitDataModel.latestVersionInfo,
            apkName = gitDataModel.latestApkName,
            downloadUrl = gitDataModel.latestApkDownloadUrl,
        )
    }

    override suspend fun syncChangelog(): Boolean = withContext(coroutineContext) {
        try {
            val content =
                networkDataSource.getChangelog(!appPreferencesDataSource.appData.first().useGithub)
            gitInfosDataSource.updateChangelogData(content.content)
            true
        } catch (throwable: Throwable) {
            this@SettingRepositoryImpl.logger().e(throwable, "syncChangelog()")
            false
        }
    }

    override suspend fun syncPrivacyPolicy(): Boolean = withContext(coroutineContext) {
        try {
            val content =
                networkDataSource.getPrivacyPolicy(!appPreferencesDataSource.appData.first().useGithub)
            gitInfosDataSource.updatePrivacyPolicyData(content.content)
            true
        } catch (throwable: Throwable) {
            this@SettingRepositoryImpl.logger().e(throwable, "syncPrivacyPolicy()")
            false
        }
    }

    override suspend fun syncLatestVersion(): Boolean = withContext(coroutineContext) {
        try {
            val release =
                networkDataSource.checkUpdate(!appPreferencesDataSource.appData.first().useGithub)
            val asset = release.assets?.firstOrNull {
                it.name?.endsWith(".apk") ?: false
            }
            gitInfosDataSource.updateLatestVersionData(
                release.name.orEmpty(),
                release.body.orEmpty(),
                asset?.name.orEmpty(),
                asset?.downloadUrl.orEmpty()
            )
            true
        } catch (throwable: Throwable) {
            this@SettingRepositoryImpl.logger().e(throwable, "syncLatestVersion()")
            false
        }
    }

    override suspend fun updateWebDAV(domain: String, account: String, password: String) =
        withContext(coroutineContext) {
            appPreferencesDataSource.updateWebDAV(
                domain = domain,
                account = account,
                password = password
            )
        }

    override suspend fun updateBackupPath(path: String) = withContext(coroutineContext) {
        appPreferencesDataSource.updateBackupPath(path)
    }

    override suspend fun updateBackupMs(ms: Long) = withContext(coroutineContext) {
        appPreferencesDataSource.updateBackupMs(ms)
    }

    override suspend fun updateAutoBackupMode(autoBackupMode: AutoBackupModeEnum) =
        withContext(coroutineContext) {
            appPreferencesDataSource.updateAutoBackupMode(autoBackupMode)
        }
}