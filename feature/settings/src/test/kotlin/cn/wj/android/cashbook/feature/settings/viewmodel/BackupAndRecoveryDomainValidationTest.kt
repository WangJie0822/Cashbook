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

package cn.wj.android.cashbook.feature.settings.viewmodel

import cn.wj.android.cashbook.feature.settings.viewmodel.BackupAndRecoveryViewModel.Companion.isWebDAVDomainValid
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * [BackupAndRecoveryViewModel.isWebDAVDomainValid] 纯函数单测：
 * 校验仅接受 https://、davs:// 且 host 非空，拒绝 http://、空 host、非法 scheme。
 */
class BackupAndRecoveryDomainValidationTest {

    // region 合法地址

    @Test
    fun when_https_with_host_then_valid() {
        assertThat(isWebDAVDomainValid("https://dav.jianguoyun.com/dav/")).isTrue()
    }

    @Test
    fun when_davs_with_host_then_valid() {
        assertThat(isWebDAVDomainValid("davs://dav.jianguoyun.com/dav/")).isTrue()
    }

    @Test
    fun when_surrounding_whitespace_then_trimmed_and_valid() {
        assertThat(isWebDAVDomainValid("  https://host.com/  ")).isTrue()
    }

    // endregion

    // region 非法地址（明文 http）

    @Test
    fun when_http_plaintext_then_invalid() {
        // 不得静默接受明文 http://
        assertThat(isWebDAVDomainValid("http://dav.jianguoyun.com/dav/")).isFalse()
    }

    // endregion

    // region 非法地址（空/无 host/非法 scheme）

    @Test
    fun when_blank_then_invalid() {
        assertThat(isWebDAVDomainValid("")).isFalse()
        assertThat(isWebDAVDomainValid("   ")).isFalse()
        assertThat(isWebDAVDomainValid(null)).isFalse()
    }

    @Test
    fun when_no_scheme_then_invalid() {
        assertThat(isWebDAVDomainValid("dav.jianguoyun.com/dav/")).isFalse()
    }

    @Test
    fun when_scheme_only_without_host_then_invalid() {
        assertThat(isWebDAVDomainValid("https://")).isFalse()
        assertThat(isWebDAVDomainValid("davs://")).isFalse()
    }

    @Test
    fun when_unsupported_scheme_then_invalid() {
        // dav:// 单独作为输入校验时不视为合法地址（需用户填 davs:// 或 https://）
        assertThat(isWebDAVDomainValid("dav://dav.jianguoyun.com/dav/")).isFalse()
        assertThat(isWebDAVDomainValid("ftp://host.com/")).isFalse()
    }

    // endregion
}
