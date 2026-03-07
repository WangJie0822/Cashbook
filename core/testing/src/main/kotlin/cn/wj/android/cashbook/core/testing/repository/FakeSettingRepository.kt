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

package cn.wj.android.cashbook.core.testing.repository

import cn.wj.android.cashbook.core.data.repository.SettingRepository
import cn.wj.android.cashbook.core.model.entity.UpgradeInfoEntity
import cn.wj.android.cashbook.core.model.enums.AutoBackupModeEnum
import cn.wj.android.cashbook.core.model.enums.DarkModeEnum
import cn.wj.android.cashbook.core.model.enums.ImageQualityEnum
import cn.wj.android.cashbook.core.model.enums.MarkdownTypeEnum
import cn.wj.android.cashbook.core.model.enums.VerificationModeEnum
import cn.wj.android.cashbook.core.model.model.AppSettingsModel
import cn.wj.android.cashbook.core.model.model.GitDataModel
import cn.wj.android.cashbook.core.model.model.RecordSettingsModel
import cn.wj.android.cashbook.core.model.model.TempKeysModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeSettingRepository : SettingRepository {

    private val _appSettingsModel = MutableStateFlow(
        AppSettingsModel(
            useGithub = false,
            autoCheckUpdate = false,
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
            imageQuality = ImageQualityEnum.HIGH,
        ),
    )

    private val _recordSettingsModel = MutableStateFlow(
        RecordSettingsModel(
            currentBookId = 1L,
            defaultTypeId = 1L,
            lastAssetId = -1L,
            refundTypeId = -1L,
            reimburseTypeId = -1L,
            creditCardPaymentTypeId = -1L,
            topUpInTotal = false,
        ),
    )

    private val _gitDataModel = MutableStateFlow(
        GitDataModel(
            latestVersionName = "",
            latestVersionInfo = "",
            latestApkName = "",
            latestApkDownloadUrl = "",
        ),
    )

    private val _tempKeysModel = MutableStateFlow(
        TempKeysModel(
            db9To10DataMigrated = false,
            preferenceSplit = false,
        ),
    )

    override val appSettingsModel: Flow<AppSettingsModel> = _appSettingsModel
    override val recordSettingsModel: Flow<RecordSettingsModel> = _recordSettingsModel
    override val gitDataModel: Flow<GitDataModel> = _gitDataModel
    override val tempKeysModel: Flow<TempKeysModel> = _tempKeysModel

    fun setAppSettings(settings: AppSettingsModel) {
        _appSettingsModel.value = settings
    }

    fun setRecordSettings(settings: RecordSettingsModel) {
        _recordSettingsModel.value = settings
    }

    override suspend fun splitAppPreferences() {
        // no-op
    }

    override suspend fun updateUseGithub(useGithub: Boolean) {
        _appSettingsModel.value = _appSettingsModel.value.copy(useGithub = useGithub)
    }

    override suspend fun updateAutoCheckUpdate(autoCheckUpdate: Boolean) {
        _appSettingsModel.value = _appSettingsModel.value.copy(autoCheckUpdate = autoCheckUpdate)
    }

    override suspend fun updateIgnoreUpdateVersion(ignoreUpdateVersion: String) {
        _appSettingsModel.value = _appSettingsModel.value.copy(ignoreUpdateVersion = ignoreUpdateVersion)
    }

    override suspend fun updateMobileNetworkDownloadEnable(mobileNetworkDownloadEnable: Boolean) {
        _appSettingsModel.value = _appSettingsModel.value.copy(mobileNetworkDownloadEnable = mobileNetworkDownloadEnable)
    }

    override suspend fun updateImageQuality(imageQuality: ImageQualityEnum) {
        _appSettingsModel.value = _appSettingsModel.value.copy(imageQuality = imageQuality)
    }

    override suspend fun updateNeedSecurityVerificationWhenLaunch(needSecurityVerificationWhenLaunch: Boolean) {
        _appSettingsModel.value = _appSettingsModel.value.copy(needSecurityVerificationWhenLaunch = needSecurityVerificationWhenLaunch)
    }

    override suspend fun updateEnableFingerprintVerification(enableFingerprintVerification: Boolean) {
        _appSettingsModel.value = _appSettingsModel.value.copy(enableFingerprintVerification = enableFingerprintVerification)
    }

    override suspend fun updatePasswordIv(iv: String) {
        _appSettingsModel.value = _appSettingsModel.value.copy(passwordIv = iv)
    }

    override suspend fun updateFingerprintIv(iv: String) {
        _appSettingsModel.value = _appSettingsModel.value.copy(fingerprintIv = iv)
    }

    override suspend fun updatePasswordInfo(passwordInfo: String) {
        _appSettingsModel.value = _appSettingsModel.value.copy(passwordInfo = passwordInfo)
    }

    override suspend fun updateFingerprintPasswordInfo(fingerprintPasswordInfo: String) {
        _appSettingsModel.value = _appSettingsModel.value.copy(fingerprintPasswordInfo = fingerprintPasswordInfo)
    }

    override suspend fun updateDarkMode(darkModeEnum: DarkModeEnum) {
        _appSettingsModel.value = _appSettingsModel.value.copy(darkMode = darkModeEnum)
    }

    override suspend fun updateDynamicColor(dynamicColor: Boolean) {
        _appSettingsModel.value = _appSettingsModel.value.copy(dynamicColor = dynamicColor)
    }

    override suspend fun updateVerificationMode(verificationMode: VerificationModeEnum) {
        _appSettingsModel.value = _appSettingsModel.value.copy(verificationMode = verificationMode)
    }

    override suspend fun updateAgreedProtocol(agreedProtocol: Boolean) {
        _appSettingsModel.value = _appSettingsModel.value.copy(agreedProtocol = agreedProtocol)
    }

    override suspend fun getLatestUpdateInfo(): UpgradeInfoEntity {
        return UpgradeInfoEntity(
            versionName = "",
            versionInfo = "",
            apkName = "",
            downloadUrl = "",
        )
    }

    override suspend fun syncLatestVersion(): Boolean = true

    override suspend fun updateWebDAV(domain: String, account: String, password: String) {
        _appSettingsModel.value = _appSettingsModel.value.copy(
            webDAVDomain = domain,
            webDAVAccount = account,
            webDAVPassword = password,
        )
    }

    override suspend fun updateBackupPath(path: String) {
        _appSettingsModel.value = _appSettingsModel.value.copy(backupPath = path)
    }

    override suspend fun updateBackupMs(ms: Long) {
        _appSettingsModel.value = _appSettingsModel.value.copy(lastBackupMs = ms)
    }

    override suspend fun updateAutoBackupMode(autoBackupMode: AutoBackupModeEnum) {
        _appSettingsModel.value = _appSettingsModel.value.copy(autoBackup = autoBackupMode)
    }

    override suspend fun updateKeepLatestBackup(keepLatestBackup: Boolean) {
        _appSettingsModel.value = _appSettingsModel.value.copy(keepLatestBackup = keepLatestBackup)
    }

    override suspend fun updateMobileNetworkBackupEnable(enable: Boolean) {
        _appSettingsModel.value = _appSettingsModel.value.copy(mobileNetworkBackupEnable = enable)
    }

    override suspend fun updateCanary(canary: Boolean) {
        _appSettingsModel.value = _appSettingsModel.value.copy(canary = canary)
    }

    override suspend fun getContentByMarkdownType(type: MarkdownTypeEnum?): String = ""

    override suspend fun updateLogcatInRelease(logcatInRelease: Boolean) {
        _appSettingsModel.value = _appSettingsModel.value.copy(logcatInRelease = logcatInRelease)
    }
}
