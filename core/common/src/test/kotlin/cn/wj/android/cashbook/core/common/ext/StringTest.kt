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

package cn.wj.android.cashbook.core.common.ext

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * String.kt 字符串工具方法单元测试
 */
class StringTest {

    // ========== isWebUri ==========

    @Test
    fun when_https_url_then_isWebUri_true() {
        assertThat("https://example.com".isWebUri).isTrue()
    }

    @Test
    fun when_http_url_then_isWebUri_true() {
        assertThat("http://example.com".isWebUri).isTrue()
    }

    @Test
    fun when_ftp_url_then_isWebUri_false() {
        assertThat("ftp://example.com".isWebUri).isFalse()
    }

    @Test
    fun when_empty_string_then_isWebUri_false() {
        assertThat("".isWebUri).isFalse()
    }

    @Test
    fun when_plain_text_then_isWebUri_false() {
        assertThat("hello world".isWebUri).isFalse()
    }

    @Test
    fun when_dav_url_then_isWebUri_false() {
        assertThat("dav://server.com".isWebUri).isFalse()
    }

    // ========== isContentUri ==========

    @Test
    fun when_content_uri_then_isContentUri_true() {
        assertThat("content://com.example.provider/data".isContentUri).isTrue()
    }

    @Test
    fun when_http_uri_then_isContentUri_false() {
        assertThat("http://example.com".isContentUri).isFalse()
    }

    @Test
    fun when_empty_string_then_isContentUri_false() {
        assertThat("".isContentUri).isFalse()
    }

    // ========== withCNY() ==========

    @Test
    fun when_positive_amount_withCNY_then_adds_prefix() {
        assertThat("19.99".withCNY()).isEqualTo("¥19.99")
    }

    @Test
    fun when_negative_amount_withCNY_then_negative_before_prefix() {
        assertThat("-19.99".withCNY()).isEqualTo("-¥19.99")
    }

    @Test
    fun when_already_has_cny_withCNY_then_no_duplicate() {
        assertThat("¥19.99".withCNY()).isEqualTo("¥19.99")
    }

    @Test
    fun when_negative_with_cny_withCNY_then_no_duplicate() {
        assertThat("-¥19.99".withCNY()).isEqualTo("-¥19.99")
    }

    @Test
    fun when_zero_withCNY_then_adds_prefix() {
        assertThat("0".withCNY()).isEqualTo("¥0")
    }
}
