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

package cn.wj.android.cashbook.core.datastore.datasource

import androidx.datastore.core.DataStore
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import cn.wj.android.cashbook.core.common.ext.logger
import cn.wj.android.cashbook.core.datastore.AppPreferences
import cn.wj.android.cashbook.core.datastore.AppSettings
import cn.wj.android.cashbook.core.datastore.GitInfos
import cn.wj.android.cashbook.core.datastore.RecordSettings
import cn.wj.android.cashbook.core.datastore.SearchHistory
import cn.wj.android.cashbook.core.datastore.TempKeys
import cn.wj.android.cashbook.core.datastore.copy
import cn.wj.android.cashbook.core.model.enums.AutoBackupModeEnum
import cn.wj.android.cashbook.core.model.enums.DarkModeEnum
import cn.wj.android.cashbook.core.model.enums.ImageQualityEnum
import cn.wj.android.cashbook.core.model.enums.VerificationModeEnum
import cn.wj.android.cashbook.core.model.model.AppSettingsModel
import cn.wj.android.cashbook.core.model.model.GitDataModel
import cn.wj.android.cashbook.core.model.model.RecordSettingsModel
import cn.wj.android.cashbook.core.model.model.SearchHistoryModel
import cn.wj.android.cashbook.core.model.model.TempKeysModel
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.IvParameterSpec
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Proto 组合数据源
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2025/1/20
 */
@Singleton
class CombineProtoDataSource @Inject constructor(
    @Deprecated("data split to appSettings and recordSettings")
    private val appPreferences: DataStore<AppPreferences>,
    private val appSettings: DataStore<AppSettings>,
    private val recordSettings: DataStore<RecordSettings>,
    private val gitInfos: DataStore<GitInfos>,
    private val searchHistory: DataStore<SearchHistory>,
    private val tempKeys: DataStore<TempKeys>,
) {

    /** 应用设置数据 */
    val appSettingsData = appSettings.data.map {
        AppSettingsModel(
            useGithub = it.useGithub,
            autoCheckUpdate = it.autoCheckUpdate,
            ignoreUpdateVersion = it.ignoreUpdateVersion,
            mobileNetworkDownloadEnable = it.mobileNetworkDownloadEnable,
            needSecurityVerificationWhenLaunch = it.needSecurityVerificationWhenLaunch,
            enableFingerprintVerification = it.enableFingerprintVerification,
            passwordIv = it.passwordIv,
            fingerprintIv = it.fingerprintIv,
            passwordInfo = it.passwordInfo,
            fingerprintPasswordInfo = it.fingerprintPasswordInfo,
            darkMode = DarkModeEnum.ordinalOf(it.darkMode),
            dynamicColor = it.dynamicColor,
            verificationMode = VerificationModeEnum.ordinalOf(it.verificationMode),
            agreedProtocol = it.agreedProtocol,
            webDAVDomain = it.webDAVDomain,
            webDAVAccount = it.webDAVAccount,
            webDAVPassword = decryptWebDAVPassword(it.webDAVPassword),
            backupPath = it.backupPath,
            autoBackup = AutoBackupModeEnum.ordinalOf(it.autoBackup),
            lastBackupMs = it.lastBackupMs,
            keepLatestBackup = it.keepLatestBackup,
            canary = it.canary,
            logcatInRelease = it.logcatInRelease,
            mobileNetworkBackupEnable = it.mobileNetworkBackupEnable,
            imageQuality = ImageQualityEnum.ordinalOf(it.imageQuality),
        )
    }

    /** 记录设置数据 */
    val recordSettingsData = recordSettings.data
        .map {
            RecordSettingsModel(
                currentBookId = it.currentBookId,
                defaultTypeId = it.defaultTypeId,
                lastAssetId = it.lastAssetId,
                refundTypeId = it.refundTypeId,
                reimburseTypeId = it.reimburseTypeId,
                creditCardPaymentTypeId = it.creditCardPaymentTypeId,
                topUpInTotal = it.topUpInTotal,
            )
        }

    /** git 仓库更新数据 */
    val gitData = gitInfos.data
        .map {
            GitDataModel(
                latestVersionName = it.latestVersionName,
                latestVersionInfo = it.latestVersionInfo,
                latestApkName = it.latestApkName,
                latestApkDownloadUrl = it.latestApkDownloadUrl,
            )
        }

    /** 搜索历史数据 */
    val searchHistoryData = searchHistory.data
        .map {
            SearchHistoryModel(
                keywords = it.keywordsList,
            )
        }

    /** 临时数据 */
    val tempKeysData = tempKeys.data
        .map {
            TempKeysModel(
                db9To10DataMigrated = it.db9To10DataMigrated,
                preferenceSplit = it.preferenceSplit,
            )
        }

    /** AppPreferences 数据拆分 */
    suspend fun splitAppPreferences() {
        if (tempKeysData.first().preferenceSplit) {
            logger().i("splitAppPreferences(), split already")
            return
        }
        logger().i("splitAppPreferences(), start split")
        @Suppress("DEPRECATION")
        val appDataModel = appPreferences.data.first()
        appSettings.updateData {
            it.copy {
                this.useGithub = appDataModel.useGithub
                this.autoCheckUpdate = appDataModel.autoCheckUpdate
                this.ignoreUpdateVersion = appDataModel.ignoreUpdateVersion
                this.mobileNetworkDownloadEnable = appDataModel.mobileNetworkDownloadEnable
                this.needSecurityVerificationWhenLaunch =
                    appDataModel.needSecurityVerificationWhenLaunch
                this.enableFingerprintVerification = appDataModel.enableFingerprintVerification
                this.passwordIv = appDataModel.passwordIv
                this.fingerprintIv = appDataModel.fingerprintIv
                this.passwordInfo = appDataModel.passwordInfo
                this.fingerprintPasswordInfo = appDataModel.fingerprintPasswordInfo
                this.darkMode = appDataModel.darkMode
                this.dynamicColor = appDataModel.dynamicColor
                this.verificationMode = appDataModel.verificationMode
                this.agreedProtocol = appDataModel.agreedProtocol
                this.webDAVDomain = appDataModel.webDAVDomain
                this.webDAVAccount = appDataModel.webDAVAccount
                this.webDAVPassword = appDataModel.webDAVPassword
                this.backupPath = appDataModel.backupPath
                this.autoBackup = appDataModel.autoBackup
                this.lastBackupMs = appDataModel.lastBackupMs
                this.keepLatestBackup = appDataModel.keepLatestBackup
                this.canary = appDataModel.canary
                this.logcatInRelease = appDataModel.logcatInRelease
            }
        }
        recordSettings.updateData {
            it.copy {
                this.currentBookId = appDataModel.currentBookId
                this.defaultTypeId = appDataModel.defaultTypeId
                this.lastAssetId = appDataModel.lastAssetId
                this.refundTypeId = appDataModel.refundTypeId
                this.reimburseTypeId = appDataModel.reimburseTypeId
                this.creditCardPaymentTypeId = appDataModel.creditCardPaymentTypeId
                this.topUpInTotal = appDataModel.topUpInTotal
            }
        }
        tempKeys.updateData { it.copy { this.preferenceSplit = true } }
    }

    suspend fun updateDb9To10DataMigrated(db9To10DataMigrated: Boolean) {
        tempKeys.updateData { it.copy { this.db9To10DataMigrated = db9To10DataMigrated } }
    }

    suspend fun updateKeywords(keywords: List<String>) {
        searchHistory.updateData {
            it.copy {
                this.keywords.clear()
                this.keywords.addAll(keywords)
            }
        }
    }

    suspend fun updateLatestVersionData(
        latestVersionName: String,
        latestVersionInfo: String,
        latestApkName: String,
        latestApkDownloadUrl: String,
    ) {
        gitInfos.updateData {
            it.copy {
                this.latestVersionName = latestVersionName
                this.latestVersionInfo = latestVersionInfo
                this.latestApkName = latestApkName
                this.latestApkDownloadUrl = latestApkDownloadUrl
            }
        }
    }

    suspend fun updateCurrentBookId(bookId: Long) {
        recordSettings.updateData { it.copy { this.currentBookId = bookId } }
    }

    suspend fun updateLastAssetId(lastAssetId: Long) {
        recordSettings.updateData { it.copy { this.lastAssetId = lastAssetId } }
    }

    suspend fun updateUseGithub(useGithub: Boolean) {
        appSettings.updateData { it.copy { this.useGithub = useGithub } }
    }

    suspend fun updateAutoCheckUpdate(autoCheckUpdate: Boolean) {
        appSettings.updateData { it.copy { this.autoCheckUpdate = autoCheckUpdate } }
    }

    suspend fun updateIgnoreUpdateVersion(ignoreUpdateVersion: String) {
        appSettings.updateData { it.copy { this.ignoreUpdateVersion = ignoreUpdateVersion } }
    }

    suspend fun updateMobileNetworkDownloadEnable(mobileNetworkDownloadEnable: Boolean) {
        appSettings.updateData {
            it.copy {
                this.mobileNetworkDownloadEnable = mobileNetworkDownloadEnable
            }
        }
    }

    suspend fun updateImageQuality(imageQuality: ImageQualityEnum) {
        appSettings.updateData {
            it.copy {
                this.imageQuality = imageQuality.ordinal
            }
        }
    }

    suspend fun updateNeedSecurityVerificationWhenLaunch(needSecurityVerificationWhenLaunch: Boolean) {
        appSettings.updateData {
            it.copy {
                this.needSecurityVerificationWhenLaunch = needSecurityVerificationWhenLaunch
            }
        }
    }

    suspend fun updateEnableFingerprintVerification(enableFingerprintVerification: Boolean) {
        appSettings.updateData {
            it.copy {
                this.enableFingerprintVerification = enableFingerprintVerification
            }
        }
    }

    suspend fun updatePasswordIv(iv: String) {
        logger().i("updatePasswordIv()")
        appSettings.updateData { it.copy { this.passwordIv = iv } }
    }

    suspend fun updateFingerprintIv(iv: String) {
        logger().i("updateFingerprintIv()")
        appSettings.updateData { it.copy { this.fingerprintIv = iv } }
    }

    suspend fun updatePasswordInfo(passwordInfo: String) {
        logger().i("updatePasswordInfo()")
        appSettings.updateData { it.copy { this.passwordInfo = passwordInfo } }
    }

    suspend fun updateFingerprintPasswordInfo(fingerprintPasswordInfo: String) {
        logger().i("updateFingerprintPasswordInfo()")
        appSettings.updateData {
            it.copy {
                this.fingerprintPasswordInfo = fingerprintPasswordInfo
            }
        }
    }

    suspend fun updateDarkMode(darkModeEnum: DarkModeEnum) {
        appSettings.updateData { it.copy { this.darkMode = darkModeEnum.ordinal } }
    }

    suspend fun updateDynamicColor(dynamicColor: Boolean) {
        appSettings.updateData { it.copy { this.dynamicColor = dynamicColor } }
    }

    suspend fun updateVerificationMode(verificationMode: VerificationModeEnum) {
        appSettings.updateData { it.copy { this.verificationMode = verificationMode.ordinal } }
    }

    suspend fun updateAgreedProtocol(agreedProtocol: Boolean) {
        appSettings.updateData { it.copy { this.agreedProtocol = agreedProtocol } }
    }

    suspend fun updateWebDAV(domain: String, account: String, password: String) {
        appSettings.updateData {
            it.copy {
                this.webDAVDomain = domain
                this.webDAVAccount = account
                this.webDAVPassword = encryptWebDAVPassword(password)
            }
        }
    }

    suspend fun updateAutoBackupMode(autoBackupMode: AutoBackupModeEnum) {
        appSettings.updateData { it.copy { this.autoBackup = autoBackupMode.ordinal } }
    }

    suspend fun updateBackupPath(path: String) {
        appSettings.updateData { it.copy { this.backupPath = path } }
    }

    suspend fun updateBackupMs(ms: Long) {
        appSettings.updateData { it.copy { this.lastBackupMs = ms } }
    }

    suspend fun updateRefundTypeId(id: Long) {
        recordSettings.updateData { it.copy { this.refundTypeId = id } }
    }

    suspend fun updateReimburseTypeId(id: Long) {
        recordSettings.updateData { it.copy { this.reimburseTypeId = id } }
    }

    suspend fun updateCreditCardPaymentTypeId(id: Long) {
        recordSettings.updateData { it.copy { this.creditCardPaymentTypeId = id } }
    }

    suspend fun updateKeepLatestBackup(keepLatestBackup: Boolean) {
        appSettings.updateData { it.copy { this.keepLatestBackup = keepLatestBackup } }
    }

    suspend fun updateMobileNetworkBackupEnable(enable: Boolean) {
        appSettings.updateData { it.copy { this.mobileNetworkBackupEnable = enable } }
    }

    suspend fun updateCanary(canary: Boolean) {
        appSettings.updateData { it.copy { this.canary = canary } }
    }

    suspend fun needRelated(typeId: Long): Boolean {
        val recordSettingsModel = recordSettingsData.first()
        return typeId == recordSettingsModel.reimburseTypeId || typeId == recordSettingsModel.refundTypeId
    }

    suspend fun updateTopUpInTotal(topUpInTotal: Boolean) {
        recordSettings.updateData { it.copy { this.topUpInTotal = topUpInTotal } }
    }

    suspend fun updateLogcatInRelease(logcatInRelease: Boolean) {
        appSettings.updateData { it.copy { this.logcatInRelease = logcatInRelease } }
    }

    companion object {

        private const val ANDROID_KEY_STORE = "AndroidKeyStore"
        private const val WEBDAV_KEY_ALIAS = "CashbookWebDAVKey"
        private const val AES_CBC_TRANSFORMATION =
            KeyProperties.KEY_ALGORITHM_AES + "/" +
                KeyProperties.BLOCK_MODE_CBC + "/" +
                KeyProperties.ENCRYPTION_PADDING_PKCS7

        /** 确保 WebDAV 密钥存在，不存在则生成 */
        private fun ensureWebDAVKey(): java.security.Key {
            val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE)
            keyStore.load(null)
            if (!keyStore.containsAlias(WEBDAV_KEY_ALIAS)) {
                val keyGenerator =
                    KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE)
                val builder = KeyGenParameterSpec.Builder(
                    WEBDAV_KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                    .setUserAuthenticationRequired(false)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                keyGenerator.init(builder.build())
                keyGenerator.generateKey()
            }
            return keyStore.getKey(WEBDAV_KEY_ALIAS, null)
        }

        /**
         * 使用 AndroidKeyStore 加密 WebDAV 密码
         *
         * 返回格式: Base64(iv):Base64(encrypted)
         */
        fun encryptWebDAVPassword(plainText: String): String {
            if (plainText.isBlank()) return plainText
            val key = ensureWebDAVKey()
            val cipher = Cipher.getInstance(AES_CBC_TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, key)
            val iv = cipher.iv
            val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            return Base64.encodeToString(iv, Base64.NO_WRAP) + ":" +
                Base64.encodeToString(encrypted, Base64.NO_WRAP)
        }

        /**
         * 使用 AndroidKeyStore 解密 WebDAV 密码
         *
         * 如果传入的字符串不含 ":" 分隔符（旧的明文数据），直接返回原文，保证向后兼容
         */
        fun decryptWebDAVPassword(encryptedText: String): String {
            if (encryptedText.isBlank() || !encryptedText.contains(":")) return encryptedText
            return try {
                val parts = encryptedText.split(":")
                val iv = Base64.decode(parts[0], Base64.NO_WRAP)
                val encrypted = Base64.decode(parts[1], Base64.NO_WRAP)
                val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE)
                keyStore.load(null)
                val key = keyStore.getKey(WEBDAV_KEY_ALIAS, null)
                val cipher = Cipher.getInstance(AES_CBC_TRANSFORMATION)
                cipher.init(Cipher.DECRYPT_MODE, key, IvParameterSpec(iv))
                String(cipher.doFinal(encrypted), Charsets.UTF_8)
            } catch (_: Exception) {
                // 解密失败，可能是旧的明文数据，直接返回原文
                encryptedText
            }
        }
    }
}
