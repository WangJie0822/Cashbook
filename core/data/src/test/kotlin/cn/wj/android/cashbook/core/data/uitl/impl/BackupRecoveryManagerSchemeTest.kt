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

package cn.wj.android.cashbook.core.data.uitl.impl

import cn.wj.android.cashbook.core.data.uitl.impl.BackupRecoveryManagerImpl.Companion.normalizeWebDAVScheme
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * [BackupRecoveryManagerImpl.normalizeWebDAVScheme] 纯函数单测：
 * 校验 dav/davs 映射到 https、明文 http 被拒绝、非法/空 host 被拒绝。
 */
class BackupRecoveryManagerSchemeTest {

    // region dav/davs -> https 映射

    @Test
    fun when_dav_scheme_then_mapped_to_https() {
        assertThat(normalizeWebDAVScheme("dav://dav.jianguoyun.com/dav/"))
            .isEqualTo("https://dav.jianguoyun.com/dav/")
    }

    @Test
    fun when_davs_scheme_then_mapped_to_https() {
        assertThat(normalizeWebDAVScheme("davs://dav.jianguoyun.com/dav/"))
            .isEqualTo("https://dav.jianguoyun.com/dav/")
    }

    @Test
    fun when_https_scheme_then_kept_as_is() {
        assertThat(normalizeWebDAVScheme("https://dav.jianguoyun.com/dav/"))
            .isEqualTo("https://dav.jianguoyun.com/dav/")
    }

    // endregion

    // region 明文 http 拒绝

    @Test
    fun when_http_scheme_then_rejected() {
        // 明文 http 必须被拒绝，避免携带 Basic 凭据走明文通道
        assertThat(normalizeWebDAVScheme("http://dav.jianguoyun.com/dav/")).isNull()
    }

    // endregion

    // region 非法输入拒绝

    @Test
    fun when_blank_then_rejected() {
        assertThat(normalizeWebDAVScheme("")).isNull()
        assertThat(normalizeWebDAVScheme("   ")).isNull()
        assertThat(normalizeWebDAVScheme(null)).isNull()
    }

    @Test
    fun when_no_scheme_then_rejected() {
        assertThat(normalizeWebDAVScheme("dav.jianguoyun.com/dav/")).isNull()
    }

    @Test
    fun when_unsupported_scheme_then_rejected() {
        assertThat(normalizeWebDAVScheme("ftp://dav.jianguoyun.com/")).isNull()
    }

    @Test
    fun when_scheme_only_without_host_then_rejected() {
        // 仅有 scheme 而无 host
        assertThat(normalizeWebDAVScheme("https://")).isNull()
        assertThat(normalizeWebDAVScheme("dav://")).isNull()
        assertThat(normalizeWebDAVScheme("davs://")).isNull()
    }

    @Test
    fun when_surrounding_whitespace_then_trimmed_and_mapped() {
        assertThat(normalizeWebDAVScheme("  davs://host.com/  "))
            .isEqualTo("https://host.com/")
    }

    // endregion
}
