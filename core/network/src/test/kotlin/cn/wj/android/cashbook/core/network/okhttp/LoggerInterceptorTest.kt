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

package cn.wj.android.cashbook.core.network.okhttp

import com.google.common.truth.Truth.assertThat
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * LoggerInterceptor 单元测试
 *
 * 覆盖各日志级别（NONE / BASIC / BODY）的输出行为，以及 redactHeader 脱敏功能。
 */
class LoggerInterceptorTest {

    private lateinit var mockWebServer: MockWebServer
    private val logOutput = mutableListOf<String>()
    private val testLogger: InterceptorLogger = { message -> logOutput.add(message) }

    @Before
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
        logOutput.clear()
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    /** 构建携带指定级别 LoggerInterceptor 的 OkHttpClient */
    private fun buildClient(
        level: Int,
        configure: (LoggerInterceptor.() -> Unit)? = null,
    ): OkHttpClient {
        val interceptor = LoggerInterceptor(testLogger, level)
        configure?.invoke(interceptor)
        return OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .build()
    }

    // ========== when_level_none_then_no_log_output ==========

    @Test
    fun when_level_none_then_no_log_output() {
        mockWebServer.enqueue(MockResponse().setBody("ok"))

        buildClient(LoggerInterceptor.LEVEL_NONE)
            .newCall(Request.Builder().url(mockWebServer.url("/test")).build())
            .execute()

        // LEVEL_NONE 不应产生任何日志
        assertThat(logOutput).isEmpty()
    }

    // ========== when_level_basic_then_logs_method_and_url ==========

    @Test
    fun when_level_basic_then_logs_method_and_url() {
        mockWebServer.enqueue(MockResponse().setBody("ok"))

        buildClient(LoggerInterceptor.LEVEL_BASIC)
            .newCall(Request.Builder().url(mockWebServer.url("/test")).build())
            .execute()

        // LEVEL_BASIC 应输出日志
        assertThat(logOutput).isNotEmpty()
        val combined = logOutput.joinToString("\n")
        // 请求方法 GET 应出现在日志中
        assertThat(combined).contains("GET")
        // 请求路径 /test 应出现在日志中
        assertThat(combined).contains("/test")
    }

    // ========== when_level_body_then_logs_response_body ==========

    @Test
    fun when_level_body_then_logs_response_body() {
        // 使用纯文本响应体（不以 { 或 [ 开头），避免 JVM 单元测试中 org.json.JSONObject 未实现的问题
        // LoggerInterceptor 在 LEVEL_BODY 时会将原始响应体内容（第 222 行 logStr.appendLine(json)）写入日志
        val responseBody = "plain-text-response"
        mockWebServer.enqueue(
            MockResponse()
                .setBody(responseBody)
                .addHeader("Content-Type", "text/plain"),
        )

        buildClient(LoggerInterceptor.LEVEL_BODY)
            .newCall(Request.Builder().url(mockWebServer.url("/api")).build())
            .execute()

        // LEVEL_BODY 应输出日志且包含响应体内容
        assertThat(logOutput).isNotEmpty()
        val combined = logOutput.joinToString("\n")
        assertThat(combined).contains(responseBody)
    }

    // ========== when_redact_header_then_header_value_hidden ==========

    @Test
    fun when_redact_header_then_header_value_hidden() {
        mockWebServer.enqueue(MockResponse().setBody("ok"))

        buildClient(LoggerInterceptor.LEVEL_HEADERS) { redactHeader("Authorization") }
            .newCall(
                Request.Builder()
                    .url(mockWebServer.url("/secure"))
                    .header("Authorization", "secret-token")
                    .build(),
            )
            .execute()

        // 日志中不应出现原始 token 值
        val combined = logOutput.joinToString("\n")
        assertThat(combined).doesNotContain("secret-token")
        // 日志中应出现脱敏占位符 ██
        assertThat(combined).contains("██")
    }
}
