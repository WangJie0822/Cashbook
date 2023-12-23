/*
 * Copyright 2021 The Cashbook Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.wj.android.cashbook.core.data.repository.impl

import android.content.Context
import cn.wj.android.cashbook.core.common.annotation.CashbookDispatchers
import cn.wj.android.cashbook.core.common.annotation.Dispatcher
import cn.wj.android.cashbook.core.common.ext.logger
import cn.wj.android.cashbook.core.data.repository.SettingRepository
import cn.wj.android.cashbook.core.datastore.datasource.AppPreferencesDataSource
import cn.wj.android.cashbook.core.datastore.datasource.GitInfosDataSource
import cn.wj.android.cashbook.core.model.entity.UpgradeInfoEntity
import cn.wj.android.cashbook.core.model.enums.AutoBackupModeEnum
import cn.wj.android.cashbook.core.model.enums.DarkModeEnum
import cn.wj.android.cashbook.core.model.enums.MarkdownTypeEnum
import cn.wj.android.cashbook.core.model.enums.VerificationModeEnum
import cn.wj.android.cashbook.core.model.model.AppDataModel
import cn.wj.android.cashbook.core.model.model.GitDataModel
import cn.wj.android.cashbook.core.network.datasource.RemoteDataSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.InputStreamReader
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

/**
 * 设置相关数据仓库
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/6/14
 */
class SettingRepositoryImpl @Inject constructor(
    private val appPreferencesDataSource: AppPreferencesDataSource,
    private val gitInfosDataSource: GitInfosDataSource,
    private val remoteDataSource: RemoteDataSource,
    @ApplicationContext private val context: Context,
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
                needSecurityVerificationWhenLaunch,
            )
        }

    override suspend fun updateEnableFingerprintVerification(enableFingerprintVerification: Boolean) =
        withContext(coroutineContext) {
            appPreferencesDataSource.updateEnableFingerprintVerification(
                enableFingerprintVerification,
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

    override suspend fun getLatestUpdateInfo(): UpgradeInfoEntity = withContext(coroutineContext) {
        val gitDataModel = gitDataModel.first()
        UpgradeInfoEntity(
            versionName = gitDataModel.latestVersionName,
            versionInfo = gitDataModel.latestVersionInfo,
            apkName = gitDataModel.latestApkName,
            downloadUrl = gitDataModel.latestApkDownloadUrl,
        )
    }

    override suspend fun syncLatestVersion(): Boolean = withContext(coroutineContext) {
        try {
            val appData = appPreferencesDataSource.appData.first()
            val release = remoteDataSource.checkUpdate(!appData.useGithub, appData.canary)
            val asset = release?.assets?.firstOrNull {
                val assetName = it.name.orEmpty()
                assetName.endsWith(".apk") && (assetName.contains("_online") || assetName.contains("_canary"))
            }
            gitInfosDataSource.updateLatestVersionData(
                release?.name.orEmpty(),
                release?.body.orEmpty(),
                asset?.name.orEmpty(),
                asset?.downloadUrl.orEmpty(),
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
                password = password,
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

    override suspend fun updateKeepLatestBackup(keepLatestBackup: Boolean) =
        withContext(coroutineContext) {
            appPreferencesDataSource.updateKeepLatestBackup(keepLatestBackup)
        }

    override suspend fun updateCanary(canary: Boolean) =
        withContext(coroutineContext) {
            appPreferencesDataSource.updateCanary(canary)
        }

    override suspend fun getContentByMarkdownType(type: MarkdownTypeEnum?): String =
        withContext(coroutineContext) {
            when (type) {
                MarkdownTypeEnum.CHANGELOG -> {
                    context.assets.open("CHANGELOG.md").use { `is` ->
                        InputStreamReader(`is`).use { isr ->
                            isr.readText()
                        }
                    }
                }

                MarkdownTypeEnum.PRIVACY_POLICY -> {
                    context.assets.open("PRIVACY_POLICY.md").use { `is` ->
                        InputStreamReader(`is`).use { isr ->
                            isr.readText()
                        }
                    }
                }

                else -> {
                    "No selected type"
                }
            }
        }
}
