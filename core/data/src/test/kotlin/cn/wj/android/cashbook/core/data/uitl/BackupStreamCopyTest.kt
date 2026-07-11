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

import cn.wj.android.cashbook.core.data.uitl.impl.writeFileToStream
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FilterOutputStream
import java.io.OutputStream

/**
 * 导出侧流式 copy 单测，守护：
 * - 大文件流式 copy 内容一致（不整流物化）
 * - 输出流由 writeFileToStream 关闭（copyTo 不 flush/close 目标流，需嵌套双 use）
 */
class BackupStreamCopyTest {

    @Test
    fun writeFileToStream_copies_full_content() {
        val src = File.createTempFile("src", ".bin")
        val payload = ByteArray(200_000) { (it % 256).toByte() }
        src.writeBytes(payload)
        val out = ByteArrayOutputStream()

        writeFileToStream(src, out)

        assertThat(out.toByteArray()).isEqualTo(payload)
        src.delete()
    }

    @Test
    fun writeFileToStream_closes_output_stream() {
        val src = File.createTempFile("src", ".bin")
        src.writeBytes("hello".toByteArray())
        var closed = false
        val out: OutputStream = object : FilterOutputStream(ByteArrayOutputStream()) {
            override fun close() {
                closed = true
                super.close()
            }
        }

        writeFileToStream(src, out)

        assertThat(closed).isTrue()
        src.delete()
    }
}
