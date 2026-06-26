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

package cn.wj.android.cashbook.core.data.uitl

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** [buildManifestJson] / [parseManifestFormatVersion] manifest 版本戳单测 */
class BackupManifestTest {

    @Test
    fun buildManifestJson_containsFormatVersionAndAppVersion() {
        val json = buildManifestJson(formatVersion = 2, appVersion = "v1.2.0")
        assertThat(parseManifestFormatVersion(json)).isEqualTo(2)
        assertThat(json).contains("v1.2.0")
    }

    @Test
    fun parseManifestFormatVersion_missingField_returnsOne() {
        // 旧 db-only 备份无 manifest → 调用方按缺失视为版本 1
        assertThat(parseManifestFormatVersion("{}")).isEqualTo(1)
    }

    @Test
    fun parseManifestFormatVersion_malformed_returnsOne() {
        assertThat(parseManifestFormatVersion("not json")).isEqualTo(1)
    }
}
