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

package cn.wj.android.cashbook.core.network.entity

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.json.Json
import org.junit.Test

/**
 * GitReleaseEntity / GitReleaseAssetEntity JSON 反序列化单元测试
 */
class GitReleaseEntitySerializationTest {

    private val json = Json { ignoreUnknownKeys = true }

    // ========== when_valid_json_then_deserializes_correctly ==========

    @Test
    fun when_valid_json_then_deserializes_correctly() {
        val jsonStr = """
            {
              "id": 123,
              "name": "Release v1.0.0",
              "body": "changelog",
              "assets": [
                {
                  "name": "app.apk",
                  "browser_download_url": "https://example.com/app.apk"
                }
              ]
            }
        """.trimIndent()

        val entity = json.decodeFromString<GitReleaseEntity>(jsonStr)

        assertThat(entity.id).isEqualTo(123L)
        assertThat(entity.name).isEqualTo("Release v1.0.0")
        assertThat(entity.body).isEqualTo("changelog")
        assertThat(entity.assets).hasSize(1)
        assertThat(entity.assets!![0].name).isEqualTo("app.apk")
        assertThat(entity.assets!![0].downloadUrl).isEqualTo("https://example.com/app.apk")
    }

    // ========== when_browser_download_url_field_then_maps_to_downloadUrl ==========

    @Test
    fun when_browser_download_url_field_then_maps_to_downloadUrl() {
        val jsonStr = """
            {
              "name": "release.apk",
              "browser_download_url": "https://github.com/releases/download/v1.0.0/release.apk"
            }
        """.trimIndent()

        val entity = json.decodeFromString<GitReleaseAssetEntity>(jsonStr)

        assertThat(entity.name).isEqualTo("release.apk")
        assertThat(entity.downloadUrl).isEqualTo("https://github.com/releases/download/v1.0.0/release.apk")
    }

    // ========== when_unknown_fields_then_ignores ==========

    @Test
    fun when_unknown_fields_then_ignores() {
        val jsonStr = """
            {
              "id": 456,
              "name": "v2.0.0",
              "body": "desc",
              "tag_name": "v2.0.0",
              "prerelease": false,
              "draft": false,
              "published_at": "2024-01-01T00:00:00Z",
              "assets": []
            }
        """.trimIndent()

        // 含有未知字段，不应抛出异常
        val entity = json.decodeFromString<GitReleaseEntity>(jsonStr)

        assertThat(entity.id).isEqualTo(456L)
        assertThat(entity.name).isEqualTo("v2.0.0")
        assertThat(entity.assets).isEmpty()
    }

    // ========== when_null_fields_then_handles_gracefully ==========

    @Test
    fun when_null_fields_then_handles_gracefully() {
        val jsonStr = """
            {
              "id": null,
              "name": null,
              "body": null,
              "assets": null
            }
        """.trimIndent()

        val entity = json.decodeFromString<GitReleaseEntity>(jsonStr)

        assertThat(entity.id).isNull()
        assertThat(entity.name).isNull()
        assertThat(entity.body).isNull()
        assertThat(entity.assets).isNull()
    }

    // ========== when_empty_assets_list_then_deserializes_empty ==========

    @Test
    fun when_empty_assets_list_then_deserializes_empty() {
        val jsonStr = """
            {
              "id": 789,
              "name": "v3.0.0",
              "body": "no assets",
              "assets": []
            }
        """.trimIndent()

        val entity = json.decodeFromString<GitReleaseEntity>(jsonStr)

        assertThat(entity.assets).isNotNull()
        assertThat(entity.assets).isEmpty()
    }
}
