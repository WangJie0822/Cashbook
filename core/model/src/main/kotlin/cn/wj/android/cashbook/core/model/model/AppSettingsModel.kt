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

package cn.wj.android.cashbook.core.model.model

import cn.wj.android.cashbook.core.model.enums.AutoBackupModeEnum
import cn.wj.android.cashbook.core.model.enums.DarkModeEnum
import cn.wj.android.cashbook.core.model.enums.ImageQualityEnum
import cn.wj.android.cashbook.core.model.enums.VerificationModeEnum

/**
 * 应用设置数据
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2025/4/26
 */
data class AppSettingsModel(
    /** 使用 github 源 */
    val useGithub: Boolean,
    /** 自动检查更新 */
    val autoCheckUpdate: Boolean,
    /** 忽略的更新版本 */
    val ignoreUpdateVersion: String,
    /** 是否允许使用数据流量下载 */
    val mobileNetworkDownloadEnable: Boolean,
    /** 启动时进行安全验证 */
    val needSecurityVerificationWhenLaunch: Boolean,
    /** 允许指纹验证 */
    val enableFingerprintVerification: Boolean,
    /** 密码加密向量 */
    val passwordIv: String,
    /** 指纹加密向量 */
    val fingerprintIv: String,
    /** 密码信息 */
    val passwordInfo: String,
    /** 指纹密码信息 */
    val fingerprintPasswordInfo: String,
    /** 黑夜模式 */
    val darkMode: DarkModeEnum,
    /** 动态配色 */
    val dynamicColor: Boolean,
    /** 安全验证类型 */
    val verificationMode: VerificationModeEnum,
    /** 已同意用户协议及隐私政策 */
    val agreedProtocol: Boolean,
    /** WebDAV 服务器地址 */
    val webDAVDomain: String,
    /** WebDAV 账户 */
    val webDAVAccount: String,
    /** WebDAV 密码 */
    val webDAVPassword: String,
    /** 备份路径 */
    val backupPath: String,
    /** 自动备份类型 */
    val autoBackup: AutoBackupModeEnum,
    /** 最后一次备份时间 */
    val lastBackupMs: Long,
    /** 是否仅保留本地最后一次备份 */
    val keepLatestBackup: Boolean,
    /** 是否支持实验版本 */
    val canary: Boolean,
    /** 在 Release 版本输出日志 */
    val logcatInRelease: Boolean,
    /** 是否允许使用数据流量备份 */
    val mobileNetworkBackupEnable: Boolean,
    /** 图片质量 */
    val imageQuality: ImageQualityEnum,
)
