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

import org.json.JSONException
import org.json.JSONObject

/**
 * 备份的设置项白名单（仅设备无关偏好）。
 * 显式排除：所有安全凭据（password/fingerprint/verification）、WebDAV 全部、设备绑定项
 * （backupPath/autoBackup/lastBackupMs/agreedProtocol）、DB 耦合的 recordSettings ids。
 */
data class SettingsBackup(
    val useGithub: Boolean,
    val autoCheckUpdate: Boolean,
    val ignoreUpdateVersion: String,
    val mobileNetworkDownloadEnable: Boolean,
    val mobileNetworkBackupEnable: Boolean,
    val darkMode: Int, // DarkModeEnum ordinal 0..2
    val dynamicColor: Boolean,
    val imageQuality: Int, // ImageQualityEnum ordinal 0..2
    val canary: Boolean,
    val logcatInRelease: Boolean,
    val monthStartDay: Int, // 1..28
)

/** 白名单序列化为 JSON（org.json，零依赖）。天然只含白名单键，不含任何凭据/设备项。 */
fun encodeSettingsBackup(backup: SettingsBackup): String =
    JSONObject()
        .put("useGithub", backup.useGithub)
        .put("autoCheckUpdate", backup.autoCheckUpdate)
        .put("ignoreUpdateVersion", backup.ignoreUpdateVersion)
        .put("mobileNetworkDownloadEnable", backup.mobileNetworkDownloadEnable)
        .put("mobileNetworkBackupEnable", backup.mobileNetworkBackupEnable)
        .put("darkMode", backup.darkMode)
        .put("dynamicColor", backup.dynamicColor)
        .put("imageQuality", backup.imageQuality)
        .put("canary", backup.canary)
        .put("logcatInRelease", backup.logcatInRelease)
        .put("monthStartDay", backup.monthStartDay)
        .toString()

/**
 * 严格校验解析：缺字段 / 枚举越界 / monthStartDay 越界 / 非法 JSON 一律返回 null
 * （调用方据此整体跳过设置恢复，绝不部分应用、绝不恢复凭据/设备项）。
 */
fun decodeSettingsBackup(json: String): SettingsBackup? = try {
    val obj = JSONObject(json)
    val darkMode = obj.getInt("darkMode")
    val imageQuality = obj.getInt("imageQuality")
    val monthStartDay = obj.getInt("monthStartDay")
    if (darkMode !in 0..2 || imageQuality !in 0..2 || monthStartDay !in 1..28) {
        null
    } else {
        SettingsBackup(
            useGithub = obj.getBoolean("useGithub"),
            autoCheckUpdate = obj.getBoolean("autoCheckUpdate"),
            ignoreUpdateVersion = obj.getString("ignoreUpdateVersion"),
            mobileNetworkDownloadEnable = obj.getBoolean("mobileNetworkDownloadEnable"),
            mobileNetworkBackupEnable = obj.getBoolean("mobileNetworkBackupEnable"),
            darkMode = darkMode,
            dynamicColor = obj.getBoolean("dynamicColor"),
            imageQuality = imageQuality,
            canary = obj.getBoolean("canary"),
            logcatInRelease = obj.getBoolean("logcatInRelease"),
            monthStartDay = monthStartDay,
        )
    }
} catch (e: JSONException) {
    null
}
