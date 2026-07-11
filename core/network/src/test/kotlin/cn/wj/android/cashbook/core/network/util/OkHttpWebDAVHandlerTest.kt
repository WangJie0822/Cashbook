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

package cn.wj.android.cashbook.core.network.util

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException

/**
 * OkHttpWebDAVHandler 单元测试
 *
 * 使用 MockWebServer 验证备份/恢复 WebDAV 网络底座：
 * - exists(HEAD) / put(PUT) 对 2xx/非 2xx/空 url 的布尔映射
 * - list(PROPFIND) 对 207 XML 的解析（剔目录与非 .zip，仅留 .zip 备份）
 * - get(GET) 流式下载到 dest 文件 + 大小上限 + 失败清理
 */
class OkHttpWebDAVHandlerTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var handler: OkHttpWebDAVHandler

    @Before
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
        handler = OkHttpWebDAVHandler(OkHttpClient(), Dispatchers.IO)
        handler.setCredentials("user", "password")
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    private fun url(path: String = "/dav") = mockWebServer.url(path).toString()

    // ---------------- exists (HEAD) ----------------

    @Test
    fun when_exists_blank_url_then_false_without_request() {
        assertThat(handler.exists("")).isFalse()
        // 空 url 应短路，不发起任何请求
        assertThat(mockWebServer.requestCount).isEqualTo(0)
    }

    @Test
    fun when_exists_success_response_then_true() {
        mockWebServer.enqueue(MockResponse().setResponseCode(200))
        assertThat(handler.exists(url())).isTrue()
    }

    @Test
    fun when_exists_error_response_then_false() {
        mockWebServer.enqueue(MockResponse().setResponseCode(404))
        assertThat(handler.exists(url())).isFalse()
    }

    // ---------------- put (PUT) ----------------

    @Test
    fun when_put_blank_url_then_false() {
        assertThat(
            handler.put("", ByteArrayInputStream("data".toByteArray()), "application/zip"),
        ).isFalse()
    }

    @Test
    fun when_put_success_response_then_true() {
        mockWebServer.enqueue(MockResponse().setResponseCode(201))
        assertThat(
            handler.put(url(), ByteArrayInputStream("data".toByteArray()), "application/zip"),
        ).isTrue()
    }

    @Test
    fun when_put_error_response_then_false() {
        mockWebServer.enqueue(MockResponse().setResponseCode(500))
        assertThat(
            handler.put(url(), ByteArrayInputStream("data".toByteArray()), "application/zip"),
        ).isFalse()
    }

    /**
     * 回归护栏：备份上传由 upload 走 [put] 的 File 重载（asRequestBody 天然流式 + repeatable +
     * 带 Content-Length），非 chunked。守护 Task 2 上传流式化的前提；若哪天 File 重载被误改为流式包 InputStream
     * 致 Content-Length 消失、退化 chunked，此测试立刻红。
     */
    @Test
    fun when_put_file_then_request_has_content_length_not_chunked() = runTest {
        val file = File.createTempFile("backup", ".zip")
        file.writeBytes("zip-binary-body".toByteArray())
        mockWebServer.enqueue(MockResponse().setResponseCode(201))

        val ok = handler.put(url(), file, "application/octet-stream")

        assertThat(ok).isTrue()
        val recorded = mockWebServer.takeRequest()
        assertThat(recorded.getHeader("Content-Length")).isNotNull()
        assertThat(recorded.getHeader("Transfer-Encoding")).isNull()
        assertThat(recorded.body.readUtf8()).isEqualTo("zip-binary-body")
        file.delete()
    }

    // ---------------- list (PROPFIND) ----------------

    @Test
    fun when_list_blank_url_then_empty() = runTest {
        assertThat(handler.list("", emptyList())).isEmpty()
    }

    @Test
    fun when_list_propfind_then_only_zip_files_parsed() = runTest {
        // 目录（href 以 / 结尾）与非 .zip 文件应被剔除，只保留 .zip 备份
        val propfindXml = """
            <?xml version="1.0" encoding="utf-8"?>
            <d:multistatus xmlns:d="DAV:">
                <d:response><d:href>/dav/cashbook/</d:href></d:response>
                <d:response><d:href>/dav/cashbook/backup_20260101.zip</d:href></d:response>
                <d:response><d:href>/dav/cashbook/notes.txt</d:href></d:response>
                <d:response><d:href>/dav/cashbook/backup_20260201.zip</d:href></d:response>
            </d:multistatus>
        """.trimIndent()
        mockWebServer.enqueue(MockResponse().setResponseCode(207).setBody(propfindXml))

        val result = handler.list(url(), emptyList())

        assertThat(result.map { it.name })
            .containsExactly("backup_20260101.zip", "backup_20260201.zip")
        assertThat(result.map { it.path })
            .containsExactly(
                "/dav/cashbook/backup_20260101.zip",
                "/dav/cashbook/backup_20260201.zip",
            )
    }

    // ---------------- copyToCapped ----------------

    @Test
    fun copyToCapped_within_cap_copies_all_bytes() {
        val input = ByteArrayInputStream(ByteArray(500) { 1 })
        val out = ByteArrayOutputStream()
        val copied = input.copyToCapped(out, maxBytes = 1000)
        assertThat(copied).isEqualTo(500L)
        assertThat(out.size()).isEqualTo(500)
    }

    @Test
    fun copyToCapped_exceeds_cap_throws() {
        val input = ByteArrayInputStream(ByteArray(2000))
        val out = ByteArrayOutputStream()
        assertThrows(IOException::class.java) {
            input.copyToCapped(out, maxBytes = 1000)
        }
    }

    @Test
    fun copyToCapped_at_exact_cap_does_not_throw() {
        // 恰好等于 cap 不抛：copyToCapped 用 total > maxBytes 判断（cap 含界）。
        // 守护未来退化为 >= 会静默拒绝合法的 at-cap 下载。
        val input = ByteArrayInputStream(ByteArray(500) { 1 })
        val out = ByteArrayOutputStream()
        val copied = input.copyToCapped(out, maxBytes = 500)
        assertThat(copied).isEqualTo(500L)
        assertThat(out.size()).isEqualTo(500)
    }

    @Test
    fun copyToCapped_exceeds_cap_partial_output_within_cap() {
        // 输入 > 8KB buffer 且 cap 落在块边界之间：超限时已落盘部分 <= cap（部分写契约，超限块不落盘）。
        val input = ByteArrayInputStream(ByteArray(20_000))
        val out = ByteArrayOutputStream()
        assertThrows(IOException::class.java) {
            input.copyToCapped(out, maxBytes = 10_000)
        }
        assertThat(out.size().toLong()).isAtMost(10_000L)
    }

    // ---------------- get (GET) 流式下载到 dest ----------------

    @Test
    fun when_get_blank_url_then_false_and_no_request() = runTest {
        val dest = File.createTempFile("dltest", ".zip").also { it.delete() }
        assertThat(handler.get("", dest, maxBytes = 1024)).isFalse()
        assertThat(mockWebServer.requestCount).isEqualTo(0)
        assertThat(dest.exists()).isFalse()
    }

    @Test
    fun when_get_success_then_writes_dest_and_true() = runTest {
        val body = "backup-binary-content"
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(body))
        val dest = File.createTempFile("dltest", ".zip")

        val ok = handler.get(url(), dest, maxBytes = 1_000_000)

        assertThat(ok).isTrue()
        assertThat(dest.readText()).isEqualTo(body)
        dest.delete()
    }

    @Test
    fun when_get_error_response_then_false_and_dest_deleted() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(404))
        val dest = File.createTempFile("dltest", ".zip")

        val ok = handler.get(url(), dest, maxBytes = 1_000_000)

        assertThat(ok).isFalse()
        assertThat(dest.exists()).isFalse()
    }

    @Test
    fun when_get_exceeds_cap_then_false_and_dest_deleted() = runTest {
        val body = "x".repeat(10_000)
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(body))
        val dest = File.createTempFile("dltest", ".zip")

        val ok = handler.get(url(), dest, maxBytes = 100)

        assertThat(ok).isFalse()
        assertThat(dest.exists()).isFalse()
    }
}
