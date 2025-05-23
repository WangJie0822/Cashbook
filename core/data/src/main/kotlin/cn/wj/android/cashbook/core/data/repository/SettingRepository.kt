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

package cn.wj.android.cashbook.core.data.repository

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

/**
 * 设置相关数据仓库
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/6/14
 */
interface SettingRepository {

    /** 应用设置数据源 */
    val appSettingsModel: Flow<AppSettingsModel>

    /** 记录设置数据源 */
    val recordSettingsModel: Flow<RecordSettingsModel>

    /** 远程仓库数据源 */
    val gitDataModel: Flow<GitDataModel>

    /** 临时开关数据 */
    val tempKeysModel: Flow<TempKeysModel>

    /** 拆分 AppPreferences 数据 */
    suspend fun splitAppPreferences()

    /** 更新使用 github 配置 */
    suspend fun updateUseGithub(useGithub: Boolean)

    /** 更新自动检查更新配置 */
    suspend fun updateAutoCheckUpdate(autoCheckUpdate: Boolean)

    /** 更新忽略更新版本配置 */
    suspend fun updateIgnoreUpdateVersion(ignoreUpdateVersion: String)

    /** 更新是否允许流量下载配置 */
    suspend fun updateMobileNetworkDownloadEnable(mobileNetworkDownloadEnable: Boolean)

    /** 更新图片质量 */
    suspend fun updateImageQuality(imageQuality: ImageQualityEnum)

    /** 更新启动时是否需要安全验证配置 */
    suspend fun updateNeedSecurityVerificationWhenLaunch(needSecurityVerificationWhenLaunch: Boolean)

    /** 更新是否允许指纹验证配置 */
    suspend fun updateEnableFingerprintVerification(enableFingerprintVerification: Boolean)

    /** 更新密码加密向量 */
    suspend fun updatePasswordIv(iv: String)

    /** 更新指纹加密向量 */
    suspend fun updateFingerprintIv(iv: String)

    suspend fun updatePasswordInfo(passwordInfo: String)

    suspend fun updateFingerprintPasswordInfo(fingerprintPasswordInfo: String)

    suspend fun updateDarkMode(darkModeEnum: DarkModeEnum)

    suspend fun updateDynamicColor(dynamicColor: Boolean)

    suspend fun updateVerificationMode(verificationMode: VerificationModeEnum)

    suspend fun updateAgreedProtocol(agreedProtocol: Boolean)

    /** 检查更新 */
    suspend fun getLatestUpdateInfo(): UpgradeInfoEntity

    suspend fun syncLatestVersion(): Boolean

    suspend fun updateWebDAV(domain: String, account: String, password: String)

    suspend fun updateBackupPath(path: String)

    suspend fun updateBackupMs(ms: Long)

    suspend fun updateAutoBackupMode(autoBackupMode: AutoBackupModeEnum)

    suspend fun updateKeepLatestBackup(keepLatestBackup: Boolean)

    suspend fun updateMobileNetworkBackupEnable(enable: Boolean)

    suspend fun updateCanary(canary: Boolean)

    suspend fun getContentByMarkdownType(type: MarkdownTypeEnum?): String

    suspend fun updateLogcatInRelease(logcatInRelease: Boolean)
}
