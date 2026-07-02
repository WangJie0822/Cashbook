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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * String.kt 字符串工具方法单元测试
 */
class StringTest {

    // ========== isWebUri ==========

    @Test
    fun when_https_url_then_isWebUri_true() {
        assertTrue("https://example.com".isWebUri)
    }

    @Test
    fun when_http_url_then_isWebUri_true() {
        assertTrue("http://example.com".isWebUri)
    }

    @Test
    fun when_ftp_url_then_isWebUri_false() {
        assertFalse("ftp://example.com".isWebUri)
    }

    @Test
    fun when_empty_string_then_isWebUri_false() {
        assertFalse("".isWebUri)
    }

    @Test
    fun when_plain_text_then_isWebUri_false() {
        assertFalse("hello world".isWebUri)
    }

    @Test
    fun when_dav_url_then_isWebUri_false() {
        assertFalse("dav://server.com".isWebUri)
    }

    // ========== isContentUri ==========

    @Test
    fun when_content_uri_then_isContentUri_true() {
        assertTrue("content://com.example.provider/data".isContentUri)
    }

    @Test
    fun when_http_uri_then_isContentUri_false() {
        assertFalse("http://example.com".isContentUri)
    }

    @Test
    fun when_empty_string_then_isContentUri_false() {
        assertFalse("".isContentUri)
    }

    // ========== withCNY() ==========

    @Test
    fun when_positive_amount_withCNY_then_adds_prefix() {
        assertEquals("¥19.99", "19.99".withCNY())
    }

    @Test
    fun when_negative_amount_withCNY_then_negative_before_prefix() {
        assertEquals("-¥19.99", "-19.99".withCNY())
    }

    @Test
    fun when_already_has_cny_withCNY_then_no_duplicate() {
        assertEquals("¥19.99", "¥19.99".withCNY())
    }

    @Test
    fun when_negative_with_cny_withCNY_then_no_duplicate() {
        assertEquals("-¥19.99", "-¥19.99".withCNY())
    }

    @Test
    fun when_zero_withCNY_then_adds_prefix() {
        assertEquals("¥0", "0".withCNY())
    }
}
