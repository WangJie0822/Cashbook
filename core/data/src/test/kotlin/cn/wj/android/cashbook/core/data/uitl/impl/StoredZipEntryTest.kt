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

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.util.zip.CRC32

/** [computeCrcAndSize] STORED zip entry 的流式 CRC32/size 计算单测。 */
class StoredZipEntryTest {

    @Test
    fun computeCrcAndSize_matchesKnownBytes() {
        val bytes = "hello world".toByteArray()
        val expectedCrc = CRC32().apply { update(bytes) }.value
        val (crc, size) = computeCrcAndSize(bytes.inputStream())
        assertThat(crc).isEqualTo(expectedCrc)
        assertThat(size).isEqualTo(bytes.size.toLong())
    }

    @Test
    fun computeCrcAndSize_emptyStream() {
        val (crc, size) = computeCrcAndSize(ByteArray(0).inputStream())
        assertThat(size).isEqualTo(0L)
        assertThat(crc).isEqualTo(CRC32().value) // 空流 CRC32 = 0
    }
}
