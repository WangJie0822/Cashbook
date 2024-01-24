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
import cn.wj.android.cashbook.core.model.enums.VerificationModeEnum

/**
 * 应用配置数据
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/6/14
 */
data class AppDataModel(
    val currentBookId: Long,
    val defaultTypeId: Long,
    val lastAssetId: Long,
    val refundTypeId: Long,
    val reimburseTypeId: Long,
    val useGithub: Boolean,
    val autoCheckUpdate: Boolean,
    val ignoreUpdateVersion: String,
    val mobileNetworkDownloadEnable: Boolean,
    val needSecurityVerificationWhenLaunch: Boolean,
    val enableFingerprintVerification: Boolean,
    val passwordIv: String,
    val fingerprintIv: String,
    val passwordInfo: String,
    val fingerprintPasswordInfo: String,
    val darkMode: DarkModeEnum,
    val dynamicColor: Boolean,
    val verificationModel: VerificationModeEnum,
    val agreedProtocol: Boolean,
    val webDAVDomain: String,
    val webDAVAccount: String,
    val webDAVPassword: String,
    val backupPath: String,
    val autoBackup: AutoBackupModeEnum,
    val lastBackupMs: Long,
    val creditCardPaymentTypeId: Long,
    val keepLatestBackup: Boolean,
    val canary: Boolean,
    val topUpInTotal: Boolean,
    val logcatInRelease: Boolean,
)
