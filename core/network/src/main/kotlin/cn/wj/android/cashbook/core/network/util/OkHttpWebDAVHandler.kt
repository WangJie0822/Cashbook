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

import androidx.annotation.WorkerThread
import cn.wj.android.cashbook.core.common.BACKUP_FILE_EXT
import cn.wj.android.cashbook.core.common.annotation.CashbookDispatchers
import cn.wj.android.cashbook.core.common.annotation.Dispatcher
import cn.wj.android.cashbook.core.common.ext.logger
import cn.wj.android.cashbook.core.model.model.BackupModel
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.intellij.lang.annotations.Language
import org.jsoup.Jsoup
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

/**
 * 使用 OkHttp 实现的 WebDAV 操作
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/7/24
 */
class OkHttpWebDAVHandler @Inject constructor(
    private val callFactory: Call.Factory,
    @Dispatcher(CashbookDispatchers.IO) private val ioCoroutineContext: CoroutineContext,
) : WebDAVHandler {

    private var account = ""
    private var password = ""

    override fun setCredentials(
        account: String,
        password: String,
    ) {
        this.account = account
        this.password = password
    }

    @WorkerThread
    override fun exists(url: String): Boolean {
        if (url.isBlank()) {
            return false
        }
        return runCatching {
            callFactory.newCall(
                Request.Builder()
                    .url(url)
                    .addHeader("Authorization", Credentials.basic(account, password))
                    .method("HEAD", null)
                    .build(),
            ).execute().use { response ->
                logger().i("exists(url = <$url>), response = <$response>")
                response.isSuccessful
            }
        }.getOrElse { throwable ->
            logger().e(throwable, "exists(url = <$url>)")
            false
        }
    }

    @WorkerThread
    override fun createDirectory(url: String): Boolean {
        if (url.isBlank()) {
            return false
        }
        return runCatching {
            callFactory.newCall(
                Request.Builder()
                    .url(url)
                    .addHeader("Authorization", Credentials.basic(account, password))
                    .method("MKCOL", null)
                    .build(),
            ).execute().use { response ->
                logger().i("createDirectory(url = <$url>), response = <$response>")
                response.isSuccessful
            }
        }.getOrElse { throwable ->
            logger().e(throwable, "createDirectory(url = <$url>)")
            false
        }
    }

    /**
     * InputStream 上传保留：当前无生产调用方（备份上传已改走 [put] 的 File 重载，asRequestBody 天然
     * 流式 + repeatable + 有 Content-Length）。若未来重新启用 InputStream 上传，须先解决：
     * - LoggerInterceptor（network interceptor + debug LEVEL_BODY）无 isOneShot 守卫，会消耗流；
     * - 当前实现 dataStream.readBytes() 全量入堆——若改流式包 InputStream 需自定义 RequestBody
     *   并显式 override contentLength()（否则退化 chunked，坚果云等 WebDAV 服务端可能拒 411/400）
     *   与 isOneShot() = true（防 OkHttp retry/redirect 重发耗尽流）。
     * 详见 docs/superpowers/specs/2026-07-10-backup-streaming-design.md 节点1四维评审。
     */
    @WorkerThread
    override fun put(url: String, dataStream: InputStream, contentType: String): Boolean {
        if (url.isBlank()) {
            return false
        }
        val readBytes = dataStream.readBytes()
        val requestBody =
            readBytes.toRequestBody(contentType.toMediaTypeOrNull(), 0, readBytes.size)
        return runCatching {
            callFactory.newCall(
                Request.Builder()
                    .url(url)
                    .addHeader("Authorization", Credentials.basic(account, password))
                    .put(requestBody)
                    .build(),
            ).execute().use { response ->
                logger().i("put(url = <$url>, dataStream = <$dataStream>, contentType = <$contentType>), response = <$response>")
                response.isSuccessful
            }
        }.getOrElse { throwable ->
            logger().e(
                throwable,
                "put(url = <$url>, dataStream = <$dataStream>, contentType = <$contentType>)",
            )
            false
        }
    }

    @WorkerThread
    override fun put(url: String, file: File, contentType: String): Boolean {
        if (url.isBlank()) {
            return false
        }
        return runCatching {
            callFactory.newCall(
                Request.Builder()
                    .url(url)
                    .addHeader("Authorization", Credentials.basic(account, password))
                    .put(file.asRequestBody(contentType.toMediaTypeOrNull()))
                    .build(),
            ).execute().use { response ->
                logger().i("put(url = <$url>, file = <$file>, contentType = <$contentType>), response = <$response>")
                response.isSuccessful
            }
        }.getOrElse { throwable ->
            logger().e(throwable, "put(url = <$url>, file = <$file>, contentType = <$contentType>)")
            false
        }
    }

    @WorkerThread
    override suspend fun list(
        url: String,
        propsList: List<String>,
    ): List<BackupModel> = withContext(ioCoroutineContext) {
        if (url.isBlank()) {
            return@withContext emptyList()
        }
        val requestPropText = if (propsList.isEmpty()) {
            DAV_PROP.replace("%s", "")
        } else {
            DAV_PROP.format(
                with(StringBuilder()) {
                    propsList.forEach {
                        appendLine("<a:$it/>")
                    }
                    toString()
                },
            )
        }
        val responseString = runCatching {
            callFactory.newCall(
                Request.Builder()
                    .url(url)
                    .addHeader("Authorization", Credentials.basic(account, password))
                    .addHeader("Depth", "1")
                    .method(
                        "PROPFIND",
                        requestPropText.toRequestBody("text/plain".toMediaTypeOrNull()),
                    )
                    .build(),
            ).execute().use { response ->
                logger().i("list(url = <$url>, propsList = <$propsList>), response = <${response.code}>")
                response.body?.string()
            }
        }.getOrElse { throwable ->
            logger().e(throwable, "list(url = <$url>, propsList = <$propsList>)")
            null
        } ?: return@withContext emptyList()
        val result = arrayListOf<BackupModel>()
        Jsoup.parse(responseString).getElementsByTag("d:response").forEach {
            val href = it.getElementsByTag("d:href")[0].text()
            if (!href.endsWith("/")) {
                val fileName = href.split("/").last()
                if (fileName.endsWith(BACKUP_FILE_EXT)) {
                    result.add(BackupModel(fileName, href))
                }
            }
        }
        logger().i("list(url, propsList), result = <$result>")
        result
    }

    @WorkerThread
    override suspend fun get(url: String, dest: File, maxBytes: Long): Boolean = withContext(ioCoroutineContext) {
        if (url.isBlank()) {
            return@withContext false
        }
        val ok = runCatching {
            callFactory.newCall(
                Request.Builder()
                    .url(url)
                    .addHeader("Authorization", Credentials.basic(account, password))
                    .build(),
            ).execute().use { response ->
                logger().i("get(url = <$url>), response = <${response.code}>")
                if (!response.isSuccessful) return@use false // 非 2xx 不写 dest
                val body = response.body ?: return@use false
                body.byteStream().use { input -> // 嵌套双 use：input+output 都关闭
                    dest.outputStream().use { output ->
                        input.copyToCapped(output, maxBytes) // 流式 + 大小上限
                    }
                }
                true
            }
        }.getOrElse { throwable ->
            logger().e(throwable, "get(url = <$url>)")
            false
        }
        if (!ok) dest.delete() // 失败清半截 dest（含空 url 早退）
        ok
    }

    companion object {
        /** 指定文件属性 */
        @Language("xml")
        private const val DAV_PROP =
            """<?xml version="1.0"?>
            <a:propfind xmlns:a="DAV:">
                <a:prop>
                    <a:displayname/>
                    <a:resourcetype/>
                    <a:getcontentlength/>
                    <a:creationdate/>
                    <a:getlastmodified/>
                    %s
                </a:prop>
            </a:propfind>"""
    }
}

/**
 * 流式把 receiver 输入流内容写入 [out]，8KB 缓冲、O(1) 堆内存；
 * 累计写入超过 [maxBytes] 立即抛 IOException（防不可信来源的磁盘耗尽 DoS）。返回已 copy 字节数。
 *
 * 抽为顶层 internal fun 便于单测。调用方负责各自 use 输入/输出流。
 */
internal fun InputStream.copyToCapped(out: OutputStream, maxBytes: Long): Long {
    val buffer = ByteArray(8 * 1024)
    var total = 0L
    while (true) {
        val read = read(buffer)
        if (read < 0) break
        total += read
        if (total > maxBytes) throw IOException("download exceeds cap: $maxBytes bytes")
        out.write(buffer, 0, read)
    }
    return total
}
