package cn.wj.android.cashbook.core.network.util

import androidx.annotation.WorkerThread
import cn.wj.android.cashbook.core.common.ext.logger
import java.io.File
import java.io.InputStream
import javax.inject.Inject
import okhttp3.Call
import okhttp3.Credentials
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import okio.BufferedSink
import okio.Okio

/**
 * 使用 OkHttp 实现的 WebDAV 操作
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/7/24
 */
class OkHttpWebDAVHandler @Inject constructor(
    private val callFactory: Call.Factory,
) {

    private var account = ""
    private var password = ""

    fun setCredentials(
        account: String,
        password: String,
    ) {
        this.account = account
        this.password = password
    }

    @WorkerThread
    fun exists(url: String): Boolean {
        val response = callFactory.newCall(
            Request.Builder()
                .url(url)
                .addHeader("Authorization", Credentials.basic(account, password))
                .method("HEAD", null)
                .build()
        ).execute()
        logger().i("exists(url = <$url>), response = <$response>")
        return response.isSuccessful
    }

    @WorkerThread
    fun createDirectory(url: String): Boolean {
        val response = callFactory.newCall(
            Request.Builder()
                .url(url)
                .addHeader("Authorization", Credentials.basic(account, password))
                .method("MKCOL", null)
                .build()
        ).execute()
        logger().i("createDirectory(url = <$url>), response = <$response>")
        return response.isSuccessful
    }

    @WorkerThread
    fun put(url: String, dataStream: InputStream, contentType: String): Boolean {
        val requestBody = RequestBody.create(MediaType.parse(contentType),dataStream.readBytes())
        val response = callFactory.newCall(
            Request.Builder()
                .url(url)
                .addHeader("Authorization", Credentials.basic(account, password))
                .put(requestBody)
                .build()
        ).execute()
        logger().i("put(url = <$url>, dataStream = <$dataStream>, contentType = <$contentType>), response = <$response>")
        return response.isSuccessful
    }

    @WorkerThread
    fun put(url: String, file: File, contentType: String): Boolean {
        val response = callFactory.newCall(
            Request.Builder()
                .url(url)
                .addHeader("Authorization", Credentials.basic(account, password))
                .put(RequestBody.create(MediaType.parse(contentType), file))
                .build()
        ).execute()
        logger().i("put(url = <$url>, file = <$file>, contentType = <$contentType>), response = <$response>")
        return response.isSuccessful
    }
}