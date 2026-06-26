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

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** [encodeSettingsBackup] / [decodeSettingsBackup] 白名单 + 排除 + 严格校验单测 */
class SettingsBackupCodecTest {

    private fun sampleBackup() = SettingsBackup(
        useGithub = true,
        autoCheckUpdate = false,
        ignoreUpdateVersion = "v1.0",
        mobileNetworkDownloadEnable = true,
        mobileNetworkBackupEnable = false,
        darkMode = 2,
        dynamicColor = true,
        imageQuality = 1,
        canary = false,
        logcatInRelease = true,
        monthStartDay = 5,
    )

    @Test
    fun encodeDecode_roundTrip() {
        val decoded = decodeSettingsBackup(encodeSettingsBackup(sampleBackup()))
        assertThat(decoded).isEqualTo(sampleBackup())
    }

    @Test
    fun encode_excludesCredentialAndDeviceFields() {
        val json = encodeSettingsBackup(sampleBackup())
        listOf(
            "passwordIv", "fingerprintIv", "passwordInfo", "fingerprintPasswordInfo",
            "verificationMode", "needSecurityVerificationWhenLaunch", "enableFingerprintVerification",
            "webDAVDomain", "webDAVAccount", "webDAVPassword",
            "backupPath", "autoBackup", "lastBackupMs", "agreedProtocol",
            "currentBookId", "defaultTypeId",
        ).forEach { excluded ->
            assertThat(json).doesNotContain(excluded)
        }
    }

    @Test
    fun decode_rejectsDarkModeOutOfRange() {
        val json = encodeSettingsBackup(sampleBackup()).replace("\"darkMode\":2", "\"darkMode\":9")
        assertThat(decodeSettingsBackup(json)).isNull()
    }

    @Test
    fun decode_rejectsImageQualityOutOfRange() {
        val json = encodeSettingsBackup(sampleBackup()).replace("\"imageQuality\":1", "\"imageQuality\":7")
        assertThat(decodeSettingsBackup(json)).isNull()
    }

    @Test
    fun decode_rejectsMonthStartDayOutOfRange() {
        val json = encodeSettingsBackup(sampleBackup()).replace("\"monthStartDay\":5", "\"monthStartDay\":40")
        assertThat(decodeSettingsBackup(json)).isNull()
    }

    @Test
    fun decode_malformedJson_returnsNull() {
        assertThat(decodeSettingsBackup("not json")).isNull()
    }

    @Test
    fun decode_missingField_returnsNull() {
        assertThat(decodeSettingsBackup("{\"darkMode\":1}")).isNull()
    }
}
