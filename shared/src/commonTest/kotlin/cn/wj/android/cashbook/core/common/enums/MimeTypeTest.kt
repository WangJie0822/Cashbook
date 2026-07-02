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

package cn.wj.android.cashbook.core.common.enums

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * MimeType.kt 媒体类型解析单元测试
 */
class MimeTypeTest {

    @Test
    fun parse_null_returns_null() = assertNull(MimeType.parse(null))

    @Test
    fun parse_image_jpeg() = assertEquals(MimeType.Image.JPEG, MimeType.parse("image/jpeg"))

    @Test
    fun parse_image_png() = assertEquals(MimeType.Image.PNG, MimeType.parse("image/png"))

    @Test
    fun parse_image_webp() = assertEquals(MimeType.Image.WEBP, MimeType.parse("image/webp"))

    @Test
    fun parse_unknown_image_subtype_returns_null() = assertNull(MimeType.parse("image/gif"))

    @Test
    fun parse_non_image_returns_null() = assertNull(MimeType.parse("text/plain"))

    @Test
    fun parse_no_slash_returns_null() = assertNull(MimeType.parse("invalid"))

    @Test
    fun parse_too_many_parts_returns_null() = assertNull(MimeType.parse("a/b/c"))

    @Test
    fun isImage_true_for_image_type() = assertTrue(MimeType.Image.JPEG.isImage)

    @Test
    fun format_returns_type_slash_subtype() = assertEquals("image/png", MimeType.Image.PNG.format)
}
