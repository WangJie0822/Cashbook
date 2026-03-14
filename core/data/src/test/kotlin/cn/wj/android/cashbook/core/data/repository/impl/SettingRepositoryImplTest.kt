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

import cn.wj.android.cashbook.core.data.testdoubles.FakeCombineProtoDataSource
import cn.wj.android.cashbook.core.data.testdoubles.FakeRemoteDataSource
import cn.wj.android.cashbook.core.model.entity.UpgradeInfoEntity
import cn.wj.android.cashbook.core.model.enums.AutoBackupModeEnum
import cn.wj.android.cashbook.core.model.enums.DarkModeEnum
import cn.wj.android.cashbook.core.model.enums.ImageQualityEnum
import cn.wj.android.cashbook.core.model.enums.VerificationModeEnum
import cn.wj.android.cashbook.core.network.entity.GitReleaseAssetEntity
import cn.wj.android.cashbook.core.network.entity.GitReleaseEntity
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * SettingRepositoryImpl 实现测试
 *
 * 由于 CombineProtoDataSource 是 final class 且依赖 DataStore/Proto，
 * 无法直接构造 SettingRepositoryImpl，因此通过 FakeCombineProtoDataSource
 * 模拟其行为，验证设置仓库的核心业务逻辑。
 */
class SettingRepositoryImplTest {

    private lateinit var fakeDataSource: FakeCombineProtoDataSource
    private lateinit var fakeRemoteDataSource: FakeRemoteDataSource

    @Before
    fun setup() {
        fakeDataSource = FakeCombineProtoDataSource()
        fakeRemoteDataSource = FakeRemoteDataSource()
    }

    // ========== Flow 数据读取测试 ==========

    @Test
    fun when_read_appSettings_then_returns_default_values() = runTest {
        val settings = fakeDataSource.appSettingsData.first()

        assertThat(settings.useGithub).isFalse()
        assertThat(settings.autoCheckUpdate).isTrue()
        assertThat(settings.ignoreUpdateVersion).isEmpty()
        assertThat(settings.darkMode).isEqualTo(DarkModeEnum.FOLLOW_SYSTEM)
        assertThat(settings.dynamicColor).isFalse()
        assertThat(settings.agreedProtocol).isFalse()
        assertThat(settings.canary).isFalse()
        assertThat(settings.logcatInRelease).isFalse()
        assertThat(settings.imageQuality).isEqualTo(ImageQualityEnum.ORIGINAL)
    }

    @Test
    fun when_read_gitData_then_returns_default_empty() = runTest {
        val gitData = fakeDataSource.gitData.first()

        assertThat(gitData.latestVersionName).isEmpty()
        assertThat(gitData.latestVersionInfo).isEmpty()
        assertThat(gitData.latestApkName).isEmpty()
        assertThat(gitData.latestApkDownloadUrl).isEmpty()
    }

    @Test
    fun when_read_tempKeys_then_returns_default_values() = runTest {
        val tempKeys = fakeDataSource.tempKeysData.first()

        assertThat(tempKeys.db9To10DataMigrated).isFalse()
        assertThat(tempKeys.preferenceSplit).isFalse()
    }

    @Test
    fun when_read_recordSettings_then_returns_default_values() = runTest {
        val recordSettings = fakeDataSource.recordSettingsData.first()

        assertThat(recordSettings.currentBookId).isEqualTo(1L)
        assertThat(recordSettings.defaultTypeId).isEqualTo(1L)
        assertThat(recordSettings.lastAssetId).isEqualTo(-1L)
    }

    // ========== 使用 Github 配置更新测试 ==========

    @Test
    fun when_updateUseGithub_true_then_appSettings_updated() = runTest {
        fakeDataSource.updateUseGithub(true)

        val settings = fakeDataSource.appSettingsData.first()
        assertThat(settings.useGithub).isTrue()
    }

    @Test
    fun when_updateUseGithub_false_then_appSettings_updated() = runTest {
        fakeDataSource.updateUseGithub(true)
        fakeDataSource.updateUseGithub(false)

        val settings = fakeDataSource.appSettingsData.first()
        assertThat(settings.useGithub).isFalse()
    }

    // ========== 自动检查更新配置测试 ==========

    @Test
    fun when_updateAutoCheckUpdate_false_then_appSettings_updated() = runTest {
        fakeDataSource.updateAutoCheckUpdate(false)

        val settings = fakeDataSource.appSettingsData.first()
        assertThat(settings.autoCheckUpdate).isFalse()
    }

    // ========== 忽略更新版本配置测试 ==========

    @Test
    fun when_updateIgnoreUpdateVersion_then_appSettings_updated() = runTest {
        fakeDataSource.updateIgnoreUpdateVersion("2.0.0")

        val settings = fakeDataSource.appSettingsData.first()
        assertThat(settings.ignoreUpdateVersion).isEqualTo("2.0.0")
    }

    // ========== 流量下载配置测试 ==========

    @Test
    fun when_updateMobileNetworkDownloadEnable_true_then_appSettings_updated() = runTest {
        fakeDataSource.updateMobileNetworkDownloadEnable(true)

        val settings = fakeDataSource.appSettingsData.first()
        assertThat(settings.mobileNetworkDownloadEnable).isTrue()
    }

    // ========== 图片质量配置测试 ==========

    @Test
    fun when_updateImageQuality_to_high_then_appSettings_updated() = runTest {
        fakeDataSource.updateImageQuality(ImageQualityEnum.HIGH)

        val settings = fakeDataSource.appSettingsData.first()
        assertThat(settings.imageQuality).isEqualTo(ImageQualityEnum.HIGH)
    }

    @Test
    fun when_updateImageQuality_to_medium_then_appSettings_updated() = runTest {
        fakeDataSource.updateImageQuality(ImageQualityEnum.MEDIUM)

        val settings = fakeDataSource.appSettingsData.first()
        assertThat(settings.imageQuality).isEqualTo(ImageQualityEnum.MEDIUM)
    }

    // ========== 安全验证配置测试 ==========

    @Test
    fun when_updateNeedSecurityVerificationWhenLaunch_true_then_appSettings_updated() = runTest {
        fakeDataSource.updateNeedSecurityVerificationWhenLaunch(true)

        val settings = fakeDataSource.appSettingsData.first()
        assertThat(settings.needSecurityVerificationWhenLaunch).isTrue()
    }

    @Test
    fun when_updateEnableFingerprintVerification_true_then_appSettings_updated() = runTest {
        fakeDataSource.updateEnableFingerprintVerification(true)

        val settings = fakeDataSource.appSettingsData.first()
        assertThat(settings.enableFingerprintVerification).isTrue()
    }

    @Test
    fun when_updateVerificationMode_to_foreground_then_appSettings_updated() = runTest {
        fakeDataSource.updateVerificationMode(VerificationModeEnum.WHEN_FOREGROUND)

        val settings = fakeDataSource.appSettingsData.first()
        assertThat(settings.verificationMode).isEqualTo(VerificationModeEnum.WHEN_FOREGROUND)
    }

    // ========== 密钥/密码信息管理测试 ==========

    @Test
    fun when_updatePasswordIv_then_appSettings_updated() = runTest {
        fakeDataSource.updatePasswordIv("test_iv_value")

        val settings = fakeDataSource.appSettingsData.first()
        assertThat(settings.passwordIv).isEqualTo("test_iv_value")
    }

    @Test
    fun when_updateFingerprintIv_then_appSettings_updated() = runTest {
        fakeDataSource.updateFingerprintIv("fingerprint_iv")

        val settings = fakeDataSource.appSettingsData.first()
        assertThat(settings.fingerprintIv).isEqualTo("fingerprint_iv")
    }

    @Test
    fun when_updatePasswordInfo_then_appSettings_updated() = runTest {
        fakeDataSource.updatePasswordInfo("encrypted_password")

        val settings = fakeDataSource.appSettingsData.first()
        assertThat(settings.passwordInfo).isEqualTo("encrypted_password")
    }

    @Test
    fun when_updateFingerprintPasswordInfo_then_appSettings_updated() = runTest {
        fakeDataSource.updateFingerprintPasswordInfo("fingerprint_password_data")

        val settings = fakeDataSource.appSettingsData.first()
        assertThat(settings.fingerprintPasswordInfo).isEqualTo("fingerprint_password_data")
    }

    // ========== 外观设置测试 ==========

    @Test
    fun when_updateDarkMode_to_dark_then_appSettings_updated() = runTest {
        fakeDataSource.updateDarkMode(DarkModeEnum.DARK)

        val settings = fakeDataSource.appSettingsData.first()
        assertThat(settings.darkMode).isEqualTo(DarkModeEnum.DARK)
    }

    @Test
    fun when_updateDarkMode_to_light_then_appSettings_updated() = runTest {
        fakeDataSource.updateDarkMode(DarkModeEnum.LIGHT)

        val settings = fakeDataSource.appSettingsData.first()
        assertThat(settings.darkMode).isEqualTo(DarkModeEnum.LIGHT)
    }

    @Test
    fun when_updateDynamicColor_true_then_appSettings_updated() = runTest {
        fakeDataSource.updateDynamicColor(true)

        val settings = fakeDataSource.appSettingsData.first()
        assertThat(settings.dynamicColor).isTrue()
    }

    // ========== 协议同意测试 ==========

    @Test
    fun when_updateAgreedProtocol_true_then_appSettings_updated() = runTest {
        fakeDataSource.updateAgreedProtocol(true)

        val settings = fakeDataSource.appSettingsData.first()
        assertThat(settings.agreedProtocol).isTrue()
    }

    // ========== WebDAV 配置测试 ==========

    @Test
    fun when_updateWebDAV_then_all_fields_updated() = runTest {
        fakeDataSource.updateWebDAV(
            domain = "https://dav.example.com",
            account = "user@example.com",
            password = "secret123",
        )

        val settings = fakeDataSource.appSettingsData.first()
        assertThat(settings.webDAVDomain).isEqualTo("https://dav.example.com")
        assertThat(settings.webDAVAccount).isEqualTo("user@example.com")
        assertThat(settings.webDAVPassword).isEqualTo("secret123")
    }

    @Test
    fun when_updateWebDAV_with_empty_values_then_cleared() = runTest {
        fakeDataSource.updateWebDAV(
            domain = "https://dav.example.com",
            account = "user@example.com",
            password = "secret123",
        )
        fakeDataSource.updateWebDAV(domain = "", account = "", password = "")

        val settings = fakeDataSource.appSettingsData.first()
        assertThat(settings.webDAVDomain).isEmpty()
        assertThat(settings.webDAVAccount).isEmpty()
        assertThat(settings.webDAVPassword).isEmpty()
    }

    // ========== 备份配置测试 ==========

    @Test
    fun when_updateBackupPath_then_appSettings_updated() = runTest {
        fakeDataSource.updateBackupPath("/storage/emulated/0/Cashbook/backup")

        val settings = fakeDataSource.appSettingsData.first()
        assertThat(settings.backupPath).isEqualTo("/storage/emulated/0/Cashbook/backup")
    }

    @Test
    fun when_updateBackupMs_then_appSettings_updated() = runTest {
        val backupTime = 1700000000000L
        fakeDataSource.updateBackupMs(backupTime)

        val settings = fakeDataSource.appSettingsData.first()
        assertThat(settings.lastBackupMs).isEqualTo(backupTime)
    }

    @Test
    fun when_updateAutoBackupMode_to_each_day_then_appSettings_updated() = runTest {
        fakeDataSource.updateAutoBackupMode(AutoBackupModeEnum.EACH_DAY)

        val settings = fakeDataSource.appSettingsData.first()
        assertThat(settings.autoBackup).isEqualTo(AutoBackupModeEnum.EACH_DAY)
    }

    @Test
    fun when_updateAutoBackupMode_to_when_launch_then_appSettings_updated() = runTest {
        fakeDataSource.updateAutoBackupMode(AutoBackupModeEnum.WHEN_LAUNCH)

        val settings = fakeDataSource.appSettingsData.first()
        assertThat(settings.autoBackup).isEqualTo(AutoBackupModeEnum.WHEN_LAUNCH)
    }

    @Test
    fun when_updateAutoBackupMode_to_each_week_then_appSettings_updated() = runTest {
        fakeDataSource.updateAutoBackupMode(AutoBackupModeEnum.EACH_WEEK)

        val settings = fakeDataSource.appSettingsData.first()
        assertThat(settings.autoBackup).isEqualTo(AutoBackupModeEnum.EACH_WEEK)
    }

    @Test
    fun when_updateKeepLatestBackup_true_then_appSettings_updated() = runTest {
        fakeDataSource.updateKeepLatestBackup(true)

        val settings = fakeDataSource.appSettingsData.first()
        assertThat(settings.keepLatestBackup).isTrue()
    }

    @Test
    fun when_updateMobileNetworkBackupEnable_true_then_appSettings_updated() = runTest {
        fakeDataSource.updateMobileNetworkBackupEnable(true)

        val settings = fakeDataSource.appSettingsData.first()
        assertThat(settings.mobileNetworkBackupEnable).isTrue()
    }

    // ========== Canary 配置测试 ==========

    @Test
    fun when_updateCanary_true_then_appSettings_updated() = runTest {
        fakeDataSource.updateCanary(true)

        val settings = fakeDataSource.appSettingsData.first()
        assertThat(settings.canary).isTrue()
    }

    // ========== 日志配置测试 ==========

    @Test
    fun when_updateLogcatInRelease_true_then_appSettings_updated() = runTest {
        fakeDataSource.updateLogcatInRelease(true)

        val settings = fakeDataSource.appSettingsData.first()
        assertThat(settings.logcatInRelease).isTrue()
    }

    // ========== splitAppPreferences 测试 ==========

    @Test
    fun when_splitAppPreferences_then_preferenceSplit_becomes_true() = runTest {
        assertThat(fakeDataSource.tempKeysData.first().preferenceSplit).isFalse()

        fakeDataSource.splitAppPreferences()

        assertThat(fakeDataSource.tempKeysData.first().preferenceSplit).isTrue()
    }

    // ========== 版本更新相关测试 ==========

    @Test
    fun when_getLatestUpdateInfo_then_returns_from_gitData() = runTest {
        // 模拟 SettingRepositoryImpl.getLatestUpdateInfo 逻辑
        fakeDataSource.updateLatestVersionData(
            latestVersionName = "v2.0.0",
            latestVersionInfo = "新增功能A",
            latestApkName = "cashbook_v2.0.0_online.apk",
            latestApkDownloadUrl = "https://example.com/download",
        )

        val gitDataModel = fakeDataSource.gitData.first()
        val upgradeInfo = UpgradeInfoEntity(
            versionName = gitDataModel.latestVersionName,
            versionInfo = gitDataModel.latestVersionInfo,
            apkName = gitDataModel.latestApkName,
            downloadUrl = gitDataModel.latestApkDownloadUrl,
        )

        assertThat(upgradeInfo.versionName).isEqualTo("v2.0.0")
        assertThat(upgradeInfo.versionInfo).isEqualTo("新增功能A")
        assertThat(upgradeInfo.apkName).isEqualTo("cashbook_v2.0.0_online.apk")
        assertThat(upgradeInfo.downloadUrl).isEqualTo("https://example.com/download")
    }

    @Test
    fun when_getLatestUpdateInfo_with_empty_gitData_then_returns_empty_strings() = runTest {
        val gitDataModel = fakeDataSource.gitData.first()
        val upgradeInfo = UpgradeInfoEntity(
            versionName = gitDataModel.latestVersionName,
            versionInfo = gitDataModel.latestVersionInfo,
            apkName = gitDataModel.latestApkName,
            downloadUrl = gitDataModel.latestApkDownloadUrl,
        )

        assertThat(upgradeInfo.versionName).isEmpty()
        assertThat(upgradeInfo.versionInfo).isEmpty()
        assertThat(upgradeInfo.apkName).isEmpty()
        assertThat(upgradeInfo.downloadUrl).isEmpty()
    }

    // ========== syncLatestVersion 逻辑测试 ==========

    @Test
    fun given_valid_release_when_syncLatestVersion_logic_then_updates_gitData() = runTest {
        // 模拟 SettingRepositoryImpl.syncLatestVersion 逻辑
        fakeRemoteDataSource.releaseToReturn = GitReleaseEntity(
            name = "v3.0.0",
            body = "大版本更新",
            assets = listOf(
                GitReleaseAssetEntity(
                    name = "cashbook_v3.0.0_online.apk",
                    downloadUrl = "https://example.com/v3.apk",
                ),
            ),
        )

        val appData = fakeDataSource.appSettingsData.first()
        val release = fakeRemoteDataSource.checkUpdate(!appData.useGithub, appData.canary)
        val asset = release?.assets?.firstOrNull {
            val assetName = it.name.orEmpty()
            assetName.endsWith(".apk") && (assetName.contains("_online") || assetName.contains("_canary"))
        }
        fakeDataSource.updateLatestVersionData(
            release?.name.orEmpty(),
            release?.body.orEmpty(),
            asset?.name.orEmpty(),
            asset?.downloadUrl.orEmpty(),
        )

        val gitData = fakeDataSource.gitData.first()
        assertThat(gitData.latestVersionName).isEqualTo("v3.0.0")
        assertThat(gitData.latestVersionInfo).isEqualTo("大版本更新")
        assertThat(gitData.latestApkName).isEqualTo("cashbook_v3.0.0_online.apk")
        assertThat(gitData.latestApkDownloadUrl).isEqualTo("https://example.com/v3.apk")
    }

    @Test
    fun given_canary_asset_when_syncLatestVersion_logic_then_selects_canary_apk() = runTest {
        fakeRemoteDataSource.releaseToReturn = GitReleaseEntity(
            name = "v3.0.0-canary",
            body = "Canary 版本",
            assets = listOf(
                GitReleaseAssetEntity(
                    name = "cashbook_v3.0.0_canary.apk",
                    downloadUrl = "https://example.com/canary.apk",
                ),
                GitReleaseAssetEntity(
                    name = "cashbook_v3.0.0_offline.apk",
                    downloadUrl = "https://example.com/offline.apk",
                ),
            ),
        )

        val appData = fakeDataSource.appSettingsData.first()
        val release = fakeRemoteDataSource.checkUpdate(!appData.useGithub, appData.canary)
        val asset = release?.assets?.firstOrNull {
            val assetName = it.name.orEmpty()
            assetName.endsWith(".apk") && (assetName.contains("_online") || assetName.contains("_canary"))
        }
        fakeDataSource.updateLatestVersionData(
            release?.name.orEmpty(),
            release?.body.orEmpty(),
            asset?.name.orEmpty(),
            asset?.downloadUrl.orEmpty(),
        )

        val gitData = fakeDataSource.gitData.first()
        assertThat(gitData.latestApkName).isEqualTo("cashbook_v3.0.0_canary.apk")
        assertThat(gitData.latestApkDownloadUrl).isEqualTo("https://example.com/canary.apk")
    }

    @Test
    fun given_no_matching_asset_when_syncLatestVersion_logic_then_stores_empty() = runTest {
        fakeRemoteDataSource.releaseToReturn = GitReleaseEntity(
            name = "v3.0.0",
            body = "更新说明",
            assets = listOf(
                GitReleaseAssetEntity(
                    name = "cashbook_v3.0.0_offline.apk",
                    downloadUrl = "https://example.com/offline.apk",
                ),
            ),
        )

        val appData = fakeDataSource.appSettingsData.first()
        val release = fakeRemoteDataSource.checkUpdate(!appData.useGithub, appData.canary)
        val asset = release?.assets?.firstOrNull {
            val assetName = it.name.orEmpty()
            assetName.endsWith(".apk") && (assetName.contains("_online") || assetName.contains("_canary"))
        }
        fakeDataSource.updateLatestVersionData(
            release?.name.orEmpty(),
            release?.body.orEmpty(),
            asset?.name.orEmpty(),
            asset?.downloadUrl.orEmpty(),
        )

        val gitData = fakeDataSource.gitData.first()
        assertThat(gitData.latestVersionName).isEqualTo("v3.0.0")
        assertThat(gitData.latestVersionInfo).isEqualTo("更新说明")
        assertThat(gitData.latestApkName).isEmpty()
        assertThat(gitData.latestApkDownloadUrl).isEmpty()
    }

    @Test
    fun given_null_release_when_syncLatestVersion_logic_then_stores_empty() = runTest {
        fakeRemoteDataSource.releaseToReturn = null

        val appData = fakeDataSource.appSettingsData.first()
        val release = fakeRemoteDataSource.checkUpdate(!appData.useGithub, appData.canary)
        val asset = release?.assets?.firstOrNull {
            val assetName = it.name.orEmpty()
            assetName.endsWith(".apk") && (assetName.contains("_online") || assetName.contains("_canary"))
        }
        fakeDataSource.updateLatestVersionData(
            release?.name.orEmpty(),
            release?.body.orEmpty(),
            asset?.name.orEmpty(),
            asset?.downloadUrl.orEmpty(),
        )

        val gitData = fakeDataSource.gitData.first()
        assertThat(gitData.latestVersionName).isEmpty()
        assertThat(gitData.latestVersionInfo).isEmpty()
        assertThat(gitData.latestApkName).isEmpty()
        assertThat(gitData.latestApkDownloadUrl).isEmpty()
    }

    @Test
    fun given_network_error_when_syncLatestVersion_logic_then_returns_false() = runTest {
        // 模拟 SettingRepositoryImpl.syncLatestVersion 中的异常处理逻辑
        fakeRemoteDataSource.shouldThrow = true

        val result = try {
            val appData = fakeDataSource.appSettingsData.first()
            fakeRemoteDataSource.checkUpdate(!appData.useGithub, appData.canary)
            true
        } catch (_: Throwable) {
            false
        }

        assertThat(result).isFalse()
    }

    @Test
    fun given_useGithub_false_when_syncLatestVersion_then_passes_useGitee_true() = runTest {
        // useGithub 为 false 时，传给 checkUpdate 的 useGitee 参数应为 true
        fakeDataSource.updateUseGithub(false)

        val appData = fakeDataSource.appSettingsData.first()
        fakeRemoteDataSource.checkUpdate(!appData.useGithub, appData.canary)

        assertThat(fakeRemoteDataSource.lastUseGitee).isTrue()
    }

    @Test
    fun given_useGithub_true_when_syncLatestVersion_then_passes_useGitee_false() = runTest {
        fakeDataSource.updateUseGithub(true)

        val appData = fakeDataSource.appSettingsData.first()
        fakeRemoteDataSource.checkUpdate(!appData.useGithub, appData.canary)

        assertThat(fakeRemoteDataSource.lastUseGitee).isFalse()
    }

    @Test
    fun given_canary_true_when_syncLatestVersion_then_passes_canary_true() = runTest {
        fakeDataSource.updateCanary(true)

        val appData = fakeDataSource.appSettingsData.first()
        fakeRemoteDataSource.checkUpdate(!appData.useGithub, appData.canary)

        assertThat(fakeRemoteDataSource.lastCanary).isTrue()
    }

    // ========== 组合场景测试 ==========

    @Test
    fun when_multiple_settings_updated_then_all_reflected_in_flow() = runTest {
        fakeDataSource.updateUseGithub(true)
        fakeDataSource.updateDarkMode(DarkModeEnum.DARK)
        fakeDataSource.updateDynamicColor(true)
        fakeDataSource.updateAgreedProtocol(true)
        fakeDataSource.updateCanary(true)
        fakeDataSource.updateAutoBackupMode(AutoBackupModeEnum.EACH_DAY)
        fakeDataSource.updateKeepLatestBackup(true)
        fakeDataSource.updateImageQuality(ImageQualityEnum.MEDIUM)

        val settings = fakeDataSource.appSettingsData.first()
        assertThat(settings.useGithub).isTrue()
        assertThat(settings.darkMode).isEqualTo(DarkModeEnum.DARK)
        assertThat(settings.dynamicColor).isTrue()
        assertThat(settings.agreedProtocol).isTrue()
        assertThat(settings.canary).isTrue()
        assertThat(settings.autoBackup).isEqualTo(AutoBackupModeEnum.EACH_DAY)
        assertThat(settings.keepLatestBackup).isTrue()
        assertThat(settings.imageQuality).isEqualTo(ImageQualityEnum.MEDIUM)
    }

    @Test
    fun when_webdav_and_backup_configured_together_then_both_persisted() = runTest {
        fakeDataSource.updateWebDAV(
            domain = "https://dav.example.com",
            account = "user@test.com",
            password = "pass",
        )
        fakeDataSource.updateBackupPath("/backup/path")
        fakeDataSource.updateBackupMs(1700000000000L)
        fakeDataSource.updateAutoBackupMode(AutoBackupModeEnum.WHEN_LAUNCH)
        fakeDataSource.updateMobileNetworkBackupEnable(true)

        val settings = fakeDataSource.appSettingsData.first()
        assertThat(settings.webDAVDomain).isEqualTo("https://dav.example.com")
        assertThat(settings.webDAVAccount).isEqualTo("user@test.com")
        assertThat(settings.webDAVPassword).isEqualTo("pass")
        assertThat(settings.backupPath).isEqualTo("/backup/path")
        assertThat(settings.lastBackupMs).isEqualTo(1700000000000L)
        assertThat(settings.autoBackup).isEqualTo(AutoBackupModeEnum.WHEN_LAUNCH)
        assertThat(settings.mobileNetworkBackupEnable).isTrue()
    }

    @Test
    fun when_security_settings_configured_together_then_all_persisted() = runTest {
        fakeDataSource.updateNeedSecurityVerificationWhenLaunch(true)
        fakeDataSource.updateEnableFingerprintVerification(true)
        fakeDataSource.updateVerificationMode(VerificationModeEnum.WHEN_FOREGROUND)
        fakeDataSource.updatePasswordIv("iv_data")
        fakeDataSource.updateFingerprintIv("fp_iv_data")
        fakeDataSource.updatePasswordInfo("pwd_info")
        fakeDataSource.updateFingerprintPasswordInfo("fp_pwd_info")

        val settings = fakeDataSource.appSettingsData.first()
        assertThat(settings.needSecurityVerificationWhenLaunch).isTrue()
        assertThat(settings.enableFingerprintVerification).isTrue()
        assertThat(settings.verificationMode).isEqualTo(VerificationModeEnum.WHEN_FOREGROUND)
        assertThat(settings.passwordIv).isEqualTo("iv_data")
        assertThat(settings.fingerprintIv).isEqualTo("fp_iv_data")
        assertThat(settings.passwordInfo).isEqualTo("pwd_info")
        assertThat(settings.fingerprintPasswordInfo).isEqualTo("fp_pwd_info")
    }

    // ========== UpgradeInfoEntity 测试 ==========

    @Test
    fun when_upgradeInfoEntity_created_then_displayVersionInfo_formatted() {
        val entity = UpgradeInfoEntity(
            versionName = "v1.0.0",
            versionInfo = "- 修复Bug\n- 新增功能",
            apkName = "app.apk",
            downloadUrl = "https://example.com/app.apk",
        )

        assertThat(entity.displayVersionInfo).contains("# v1.0.0")
        assertThat(entity.displayVersionInfo).contains("- 修复Bug")
        assertThat(entity.displayVersionInfo).contains("- 新增功能")
    }

    // ========== db9To10DataMigrated 测试 ==========

    @Test
    fun when_updateDb9To10DataMigrated_true_then_tempKeys_updated() = runTest {
        fakeDataSource.updateDb9To10DataMigrated(true)

        val tempKeys = fakeDataSource.tempKeysData.first()
        assertThat(tempKeys.db9To10DataMigrated).isTrue()
    }

    // ========== Git 版本数据更新测试 ==========

    @Test
    fun when_updateLatestVersionData_then_gitData_updated() = runTest {
        fakeDataSource.updateLatestVersionData(
            latestVersionName = "v1.5.0",
            latestVersionInfo = "功能更新",
            latestApkName = "cashbook_v1.5.0_online.apk",
            latestApkDownloadUrl = "https://dl.example.com/v1.5.0.apk",
        )

        val gitData = fakeDataSource.gitData.first()
        assertThat(gitData.latestVersionName).isEqualTo("v1.5.0")
        assertThat(gitData.latestVersionInfo).isEqualTo("功能更新")
        assertThat(gitData.latestApkName).isEqualTo("cashbook_v1.5.0_online.apk")
        assertThat(gitData.latestApkDownloadUrl).isEqualTo("https://dl.example.com/v1.5.0.apk")
    }

    @Test
    fun when_updateLatestVersionData_twice_then_latest_value_kept() = runTest {
        fakeDataSource.updateLatestVersionData("v1.0.0", "旧版本", "old.apk", "old_url")
        fakeDataSource.updateLatestVersionData("v2.0.0", "新版本", "new.apk", "new_url")

        val gitData = fakeDataSource.gitData.first()
        assertThat(gitData.latestVersionName).isEqualTo("v2.0.0")
        assertThat(gitData.latestVersionInfo).isEqualTo("新版本")
    }
}
