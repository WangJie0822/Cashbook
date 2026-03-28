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

package cn.wj.android.cashbook.core.data.testdoubles

import cn.wj.android.cashbook.core.model.enums.AutoBackupModeEnum
import cn.wj.android.cashbook.core.model.enums.DarkModeEnum
import cn.wj.android.cashbook.core.model.enums.ImageQualityEnum
import cn.wj.android.cashbook.core.model.enums.VerificationModeEnum
import cn.wj.android.cashbook.core.model.model.AppSettingsModel
import cn.wj.android.cashbook.core.model.model.GitDataModel
import cn.wj.android.cashbook.core.model.model.RecordSettingsModel
import cn.wj.android.cashbook.core.model.model.SearchHistoryModel
import cn.wj.android.cashbook.core.model.model.TempKeysModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * CombineProtoDataSource 的测试替身
 *
 * 由于 CombineProtoDataSource 依赖 DataStore/Proto，测试中不便直接使用，
 * 这里提供一个接口相似的 Fake 实现，供 Repository 测试使用。
 *
 * 注意：实际的 CombineProtoDataSource 是一个 final class 且构造函数需要多个 DataStore，
 * 所以不能直接继承。Repository 测试中需要通过调整构造方式来使用此 Fake。
 */
class FakeCombineProtoDataSource {

    private val _appSettings = MutableStateFlow(
        AppSettingsModel(
            useGithub = false,
            autoCheckUpdate = true,
            ignoreUpdateVersion = "",
            mobileNetworkDownloadEnable = false,
            needSecurityVerificationWhenLaunch = false,
            enableFingerprintVerification = false,
            passwordIv = "",
            fingerprintIv = "",
            passwordInfo = "",
            fingerprintPasswordInfo = "",
            darkMode = DarkModeEnum.FOLLOW_SYSTEM,
            dynamicColor = false,
            verificationMode = VerificationModeEnum.WHEN_LAUNCH,
            agreedProtocol = false,
            webDAVDomain = "",
            webDAVAccount = "",
            webDAVPassword = "",
            backupPath = "",
            autoBackup = AutoBackupModeEnum.CLOSE,
            lastBackupMs = 0L,
            keepLatestBackup = false,
            canary = false,
            logcatInRelease = false,
            mobileNetworkBackupEnable = false,
            imageQuality = ImageQualityEnum.ORIGINAL,
        ),
    )

    private val _recordSettings = MutableStateFlow(
        RecordSettingsModel(
            currentBookId = 1L,
            defaultTypeId = 1L,
            lastAssetId = -1L,
            refundTypeId = 0L,
            reimburseTypeId = 0L,
            creditCardPaymentTypeId = 0L,
            topUpInTotal = false,
        ),
    )

    private val _gitData = MutableStateFlow(
        GitDataModel(
            latestVersionName = "",
            latestVersionInfo = "",
            latestApkName = "",
            latestApkDownloadUrl = "",
        ),
    )

    private val _searchHistory = MutableStateFlow(
        SearchHistoryModel(keywords = emptyList()),
    )

    private val _tempKeys = MutableStateFlow(
        TempKeysModel(
            db9To10DataMigrated = false,
            preferenceSplit = false,
        ),
    )

    /** 应用设置数据 */
    val appSettingsData: Flow<AppSettingsModel> = _appSettings

    /** 记录设置数据 */
    val recordSettingsData: Flow<RecordSettingsModel> = _recordSettings

    /** git 仓库更新数据 */
    val gitData: Flow<GitDataModel> = _gitData

    /** 搜索历史数据 */
    val searchHistoryData: Flow<SearchHistoryModel> = _searchHistory

    /** 临时数据 */
    val tempKeysData: Flow<TempKeysModel> = _tempKeys

    /** 充值计入总额数据 */
    val topUpInTotalData: Flow<Boolean> = _recordSettings.map { it.topUpInTotal }

    // ========== 应用设置相关方法 ==========

    suspend fun splitAppPreferences() {
        _tempKeys.update { it.copy(preferenceSplit = true) }
    }

    suspend fun updateUseGithub(useGithub: Boolean) {
        _appSettings.update { it.copy(useGithub = useGithub) }
    }

    suspend fun updateAutoCheckUpdate(autoCheckUpdate: Boolean) {
        _appSettings.update { it.copy(autoCheckUpdate = autoCheckUpdate) }
    }

    suspend fun updateIgnoreUpdateVersion(ignoreUpdateVersion: String) {
        _appSettings.update { it.copy(ignoreUpdateVersion = ignoreUpdateVersion) }
    }

    suspend fun updateMobileNetworkDownloadEnable(mobileNetworkDownloadEnable: Boolean) {
        _appSettings.update { it.copy(mobileNetworkDownloadEnable = mobileNetworkDownloadEnable) }
    }

    suspend fun updateImageQuality(imageQuality: ImageQualityEnum) {
        _appSettings.update { it.copy(imageQuality = imageQuality) }
    }

    suspend fun updateNeedSecurityVerificationWhenLaunch(needSecurityVerificationWhenLaunch: Boolean) {
        _appSettings.update {
            it.copy(needSecurityVerificationWhenLaunch = needSecurityVerificationWhenLaunch)
        }
    }

    suspend fun updateEnableFingerprintVerification(enableFingerprintVerification: Boolean) {
        _appSettings.update {
            it.copy(enableFingerprintVerification = enableFingerprintVerification)
        }
    }

    suspend fun updatePasswordIv(iv: String) {
        _appSettings.update { it.copy(passwordIv = iv) }
    }

    suspend fun updateFingerprintIv(iv: String) {
        _appSettings.update { it.copy(fingerprintIv = iv) }
    }

    suspend fun updatePasswordInfo(passwordInfo: String) {
        _appSettings.update { it.copy(passwordInfo = passwordInfo) }
    }

    suspend fun updateFingerprintPasswordInfo(fingerprintPasswordInfo: String) {
        _appSettings.update { it.copy(fingerprintPasswordInfo = fingerprintPasswordInfo) }
    }

    suspend fun updateDarkMode(darkModeEnum: DarkModeEnum) {
        _appSettings.update { it.copy(darkMode = darkModeEnum) }
    }

    suspend fun updateDynamicColor(dynamicColor: Boolean) {
        _appSettings.update { it.copy(dynamicColor = dynamicColor) }
    }

    suspend fun updateVerificationMode(verificationMode: VerificationModeEnum) {
        _appSettings.update { it.copy(verificationMode = verificationMode) }
    }

    suspend fun updateAgreedProtocol(agreedProtocol: Boolean) {
        _appSettings.update { it.copy(agreedProtocol = agreedProtocol) }
    }

    suspend fun updateWebDAV(domain: String, account: String, password: String) {
        _appSettings.update {
            it.copy(webDAVDomain = domain, webDAVAccount = account, webDAVPassword = password)
        }
    }

    suspend fun updateBackupPath(path: String) {
        _appSettings.update { it.copy(backupPath = path) }
    }

    suspend fun updateBackupMs(ms: Long) {
        _appSettings.update { it.copy(lastBackupMs = ms) }
    }

    suspend fun updateAutoBackupMode(autoBackupMode: AutoBackupModeEnum) {
        _appSettings.update { it.copy(autoBackup = autoBackupMode) }
    }

    suspend fun updateKeepLatestBackup(keepLatestBackup: Boolean) {
        _appSettings.update { it.copy(keepLatestBackup = keepLatestBackup) }
    }

    suspend fun updateMobileNetworkBackupEnable(enable: Boolean) {
        _appSettings.update { it.copy(mobileNetworkBackupEnable = enable) }
    }

    suspend fun updateCanary(canary: Boolean) {
        _appSettings.update { it.copy(canary = canary) }
    }

    suspend fun updateLogcatInRelease(logcatInRelease: Boolean) {
        _appSettings.update { it.copy(logcatInRelease = logcatInRelease) }
    }

    suspend fun updateLatestVersionData(
        latestVersionName: String,
        latestVersionInfo: String,
        latestApkName: String,
        latestApkDownloadUrl: String,
    ) {
        _gitData.update {
            GitDataModel(
                latestVersionName = latestVersionName,
                latestVersionInfo = latestVersionInfo,
                latestApkName = latestApkName,
                latestApkDownloadUrl = latestApkDownloadUrl,
            )
        }
    }

    // ========== 记录设置相关方法 ==========

    suspend fun updateCurrentBookId(bookId: Long) {
        _recordSettings.update { it.copy(currentBookId = bookId) }
    }

    suspend fun updateLastAssetId(lastAssetId: Long) {
        _recordSettings.update { it.copy(lastAssetId = lastAssetId) }
    }

    suspend fun updateRefundTypeId(id: Long) {
        _recordSettings.update { it.copy(refundTypeId = id) }
    }

    suspend fun updateReimburseTypeId(id: Long) {
        _recordSettings.update { it.copy(reimburseTypeId = id) }
    }

    suspend fun updateCreditCardPaymentTypeId(id: Long) {
        _recordSettings.update { it.copy(creditCardPaymentTypeId = id) }
    }

    suspend fun updateKeywords(keywords: List<String>) {
        _searchHistory.update { SearchHistoryModel(keywords = keywords) }
    }

    suspend fun updateTopUpInTotal(topUpInTotal: Boolean) {
        _recordSettings.update { it.copy(topUpInTotal = topUpInTotal) }
    }

    suspend fun updateDb9To10DataMigrated(migrated: Boolean) {
        _tempKeys.update { it.copy(db9To10DataMigrated = migrated) }
    }

    suspend fun needRelated(typeId: Long): Boolean {
        val settings = _recordSettings.value
        return typeId == settings.reimburseTypeId || typeId == settings.refundTypeId
    }
}
