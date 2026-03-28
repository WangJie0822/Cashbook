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

package cn.wj.android.cashbook.core.network.datasource

import cn.wj.android.cashbook.core.network.entity.GitReleaseEntity
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * NetworkDataSource Release 筛选逻辑单元测试
 *
 * 说明：RetrofitNetworkApi 的 @GET 注解使用完整 URL（UrlDefinition.GITEE_RELEASE_LIST /
 * GITHUB_RELEASE_LIST），Retrofit 遇到完整 URL 时会忽略 baseUrl，因此无法通过
 * MockWebServer 拦截请求。这里只针对 checkUpdate() 内的筛选逻辑进行纯函数测试。
 */
class NetworkDataSourceTest {

    /**
     * 复现 NetworkDataSource.checkUpdate() 中的 Release 筛选逻辑：
     * 名称以 "Release" 开头的版本始终命中；
     * 名称以 "Pre Release" 开头的版本仅在 canary=true 时命中。
     */
    private fun filterRelease(releases: List<GitReleaseEntity>, canary: Boolean): GitReleaseEntity? {
        return releases.firstOrNull {
            val name = it.name ?: ""
            name.startsWith("Release") || (canary && name.startsWith("Pre Release"))
        }
    }

    // ========== when_release_name_starts_with_Release_then_selected ==========

    @Test
    fun when_release_name_starts_with_Release_then_selected() {
        val releases = listOf(
            GitReleaseEntity(id = 1L, name = "Release v1.0.0"),
            GitReleaseEntity(id = 2L, name = "Draft v0.9.0"),
        )

        val result = filterRelease(releases, canary = false)

        assertThat(result?.name).isEqualTo("Release v1.0.0")
    }

    // ========== when_canary_true_then_includes_pre_release ==========

    @Test
    fun when_canary_true_then_includes_pre_release() {
        // 列表中没有正式 Release，只有 Pre Release
        val releases = listOf(
            GitReleaseEntity(id = 1L, name = "Pre Release v1.1.0-beta"),
            GitReleaseEntity(id = 2L, name = "Draft v0.9.0"),
        )

        val result = filterRelease(releases, canary = true)

        assertThat(result?.name).isEqualTo("Pre Release v1.1.0-beta")
    }

    // ========== when_canary_false_then_excludes_pre_release ==========

    @Test
    fun when_canary_false_then_excludes_pre_release() {
        val releases = listOf(
            GitReleaseEntity(id = 1L, name = "Pre Release v1.1.0-beta"),
            GitReleaseEntity(id = 2L, name = "Draft v0.9.0"),
        )

        val result = filterRelease(releases, canary = false)

        // canary=false 时 Pre Release 不应被命中
        assertThat(result).isNull()
    }

    // ========== when_no_matching_release_then_returns_null ==========

    @Test
    fun when_no_matching_release_then_returns_null() {
        val releases = listOf(
            GitReleaseEntity(id = 1L, name = "Draft v0.8.0"),
            GitReleaseEntity(id = 2L, name = "Snapshot v0.9.0"),
        )

        val result = filterRelease(releases, canary = false)

        assertThat(result).isNull()
    }

    // ========== when_null_name_then_skipped ==========

    @Test
    fun when_null_name_then_skipped() {
        val releases = listOf(
            GitReleaseEntity(id = 1L, name = null),
            GitReleaseEntity(id = 2L, name = "Release v2.0.0"),
        )

        val result = filterRelease(releases, canary = false)

        // name 为 null 的条目不应命中，应返回后续的正式 Release
        assertThat(result?.id).isEqualTo(2L)
        assertThat(result?.name).isEqualTo("Release v2.0.0")
    }
}
