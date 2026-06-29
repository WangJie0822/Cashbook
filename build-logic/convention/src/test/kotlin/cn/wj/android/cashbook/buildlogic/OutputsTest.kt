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

package cn.wj.android.cashbook.buildlogic

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * [buildReleaseApkName] 命名契约测试。
 *
 * 守护两个核心约束：
 * 1. AGP `variant.flavorName` 实际返回枚举常量名（首字母大写 `Online`/`Offline`/`Canary`/`Dev`），
 *    命名必须强制小写以对齐历史发布命名（`_online`/`_offline`/...）。
 * 2. 应用内"检查更新"在 `SettingRepositoryImpl.syncLatestVersion()` 用**大小写敏感**的
 *    `contains("_online")` / `contains("_canary")` 从 Release 资产挑选要下载的 APK；
 *    渠道段一旦大写（`_Online`）会让更新检查静默失效。
 */
class OutputsTest {

    @Test
    fun buildReleaseApkName_online_lowercasesFlavorToHistoricalFormat() {
        assertThat(buildReleaseApkName("v1.3.0_26062916", "Online"))
            .isEqualTo("Cashbook_v1.3.0_26062916_online.apk")
    }

    @Test
    fun buildReleaseApkName_offline_lowercasesFlavor() {
        assertThat(buildReleaseApkName("v1.3.0_26062916", "Offline"))
            .isEqualTo("Cashbook_v1.3.0_26062916_offline.apk")
    }

    @Test
    fun buildReleaseApkName_online_matchesInAppUpdaterCaseSensitiveContract() {
        assertThat(buildReleaseApkName("v1.3.0_26062916", "Online")).contains("_online")
    }

    @Test
    fun buildReleaseApkName_canary_matchesInAppUpdaterCaseSensitiveContract() {
        assertThat(buildReleaseApkName("v1.3.0_26062916", "Canary")).contains("_canary")
    }

    @Test
    fun buildReleaseApkName_canary_lowercasesFlavorToHistoricalFormat() {
        assertThat(buildReleaseApkName("v1.3.0_26062916", "Canary"))
            .isEqualTo("Cashbook_v1.3.0_26062916_canary.apk")
    }

    @Test
    fun buildReleaseApkName_dev_lowercasesFlavor() {
        assertThat(buildReleaseApkName("v1.3.0_26062916", "Dev"))
            .isEqualTo("Cashbook_v1.3.0_26062916_dev.apk")
    }

    @Test
    fun buildReleaseApkName_onlineAndOffline_produceDistinctNames() {
        // 本次修复的核心退化点：渠道段缺失会致 online/offline 同名互相覆盖（2 个 APK 退化为 1 个）
        val version = "v1.3.0_26062916"
        assertThat(buildReleaseApkName(version, "Online"))
            .isNotEqualTo(buildReleaseApkName(version, "Offline"))
    }
}
