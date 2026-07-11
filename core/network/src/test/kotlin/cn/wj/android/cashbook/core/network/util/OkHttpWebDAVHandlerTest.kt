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
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.File

/**
 * OkHttpWebDAVHandler 单元测试
 *
 * 使用 MockWebServer 验证备份/恢复 WebDAV 网络底座：
 * - exists(HEAD) / put(PUT) 对 2xx/非 2xx/空 url 的布尔映射
 * - list(PROPFIND) 对 207 XML 的解析（剔目录与非 .zip，仅留 .zip 备份）
 * - get(GET) 对 200/404/空 url 的字节流映射
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

    // ---------------- get (GET) ----------------

    @Test
    fun when_get_blank_url_then_null() = runTest {
        assertThat(handler.get("")).isNull()
    }

    @Test
    fun when_get_success_response_then_bytes() = runTest {
        val body = "backup-binary-content"
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(body))

        val result = handler.get(url())

        assertThat(result).isNotNull()
        assertThat(String(result!!)).isEqualTo(body)
    }

    @Test
    fun when_get_error_response_then_null() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(404))
        assertThat(handler.get(url())).isNull()
    }
}
