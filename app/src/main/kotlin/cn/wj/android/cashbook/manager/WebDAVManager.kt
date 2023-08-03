package cn.wj.android.cashbook.manager

import cn.wj.android.cashbook.base.ext.base.logger
import cn.wj.android.cashbook.base.tools.funLogger
import cn.wj.android.cashbook.base.tools.isNetAvailable
import cn.wj.android.cashbook.data.config.AppConfigs
import cn.wj.android.cashbook.data.constants.BACKUP_DIR_NAME
import cn.wj.android.cashbook.data.constants.BACKUP_FILE_EXT
import cn.wj.android.cashbook.data.entity.BackupEntity
import cn.wj.android.cashbook.third.encode.EncodingDetect
import cn.wj.android.cashbook.third.encode.UTF8BOMFighter
import cn.wj.android.cashbook.third.okhttp.InterceptorLogger
import cn.wj.android.cashbook.third.okhttp.LoggerInterceptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import org.intellij.lang.annotations.Language
import org.jsoup.Jsoup
import java.io.File
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.Charset

/**
 * WebDAV 操作管理类
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/8/17
 */
object WebDAVManager {

    /** 指定文件属性 */
    @Language("xml")
    private const val DIR =
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

    private val okHttpClient: OkHttpClient by lazy {
        // 日志打印
        val logger = object : InterceptorLogger {
            override fun invoke(msg: String) {
                funLogger("WebDAV").d(msg)
            }
        }
        OkHttpClient.Builder()
            .retryOnConnectionFailure(true)
            .addNetworkInterceptor(
                LoggerInterceptor(logger,  LoggerInterceptor.LEVEL_BODY )
            )
            .build()
    }

    /** 获取时间存储链接 */
    private fun getUrl(): String? {
        val webUrl = AppConfigs.webDAVWebUrl
        val url = URL("$webUrl${if (webUrl.endsWith("/")) "" else "/"}$BACKUP_DIR_NAME/")
        val raw = url.toString().replace("davs://", "https://").replace("dav://", "http://")
        return kotlin.runCatching {
            URLEncoder.encode(raw, "UTF-8")
                .replace("\\+".toRegex(), "%20")
                .replace("%3A".toRegex(), ":")
                .replace("%2F".toRegex(), "/")
        }.getOrNull()
    }

    /** 根据链接创建文件 */
    private suspend fun createDir() = withContext(Dispatchers.IO) {
        val url = getUrl()
        if (url.isNullOrBlank()) {
            return@withContext
        }
        @Suppress("BlockingMethodInNonBlockingContext")
        okHttpClient.newCall(
            Request.Builder()
                .url(url)
                .method("MKCOL", null)
                .addHeader("Authorization", Credentials.basic(AppConfigs.webDAVAccount, AppConfigs.webDAVPassword))
                .build()
        ).execute()
    }

    /** 判断 WebDAV 是否可用 */
    suspend fun available(): Boolean {
        if (isNetAvailable() && AppConfigs.webDAVWebUrl.isNotBlank() && AppConfigs.webDAVAccount.isNotBlank() && AppConfigs.webDAVPassword.isNotBlank()) {
            // 已配置 WebDAV
            createDir()
            return true
        }
        return false
    }

    /** 将 [path] 路径的文件备份到 WebDAV 并返回结果 */
    suspend fun backup(path: String): Boolean = withContext(Dispatchers.IO) {
        if (!available()) {
            logger().d("WebDAV not available")
            return@withContext false
        }
        val local = File(path)
        if (!local.exists()) {
            logger().d("file not exists: $path")
            return@withContext false
        }
        val uploadUrl = getUrl() + local.name
        val fileBody = local.asRequestBody("application/octet-stream".toMediaType())
        @Suppress("BlockingMethodInNonBlockingContext")
        kotlin.runCatching {
            okHttpClient.newCall(
                Request.Builder()
                    .url(uploadUrl)
                    .put(fileBody)
                    .addHeader("Authorization", Credentials.basic(AppConfigs.webDAVAccount, AppConfigs.webDAVPassword))
                    .build()
            ).execute()
        }.isSuccess
    }

    /** 获取备份文件列表 */
    suspend fun getBackupFileList(): List<BackupEntity> = withContext(Dispatchers.IO) {
        val ls = arrayListOf<BackupEntity>()
        if (!available()) {
            logger().d("WebDAV not available")
            return@withContext ls
        }
        // 获取 WebDAV 信息
        val info = getWebDAVInfo() ?: return@withContext ls
        // 解析数据并获取备份列表
        val baseUrl = getUrl()
        Jsoup.parse(info).getElementsByTag("d:response").forEach {
            val href = it.getElementsByTag("d:href")[0].text()
            if (!href.endsWith("/")) {
                val fileName = href.split("/").last()
                if (fileName.endsWith(BACKUP_FILE_EXT)) {
                    ls.add(BackupEntity(fileName, baseUrl + fileName, true))
                }
            }
        }
        ls
    }

    /** 将 [url] 文件下载到 [saved] */
    suspend fun downloadTo(url: String, saved: File): Boolean = withContext(Dispatchers.IO) {
        val inputStream = kotlin.runCatching {
            okHttpClient.newCall(
                Request.Builder()
                    .url(url)
                    .addHeader("Authorization", Credentials.basic(AppConfigs.webDAVAccount, AppConfigs.webDAVPassword))
                    .build()
            ).execute().body?.byteStream()
        }.getOrNull() ?: return@withContext false
        saved.writeBytes(inputStream.readBytes())
        true
    }

    /** 获取 WebDAV 信息 */
    private suspend fun getWebDAVInfo(propsList: List<String> = emptyList()): String? = withContext(Dispatchers.IO) {
        val requestProps = StringBuilder()
        for (p in propsList) {
            requestProps.append("<a:").append(p).append("/>\n")
        }
        val requestPropsStr: String = if (requestProps.toString().isEmpty()) {
            DIR.replace("%s", "")
        } else {
            String.format(DIR, requestProps.toString() + "\n")
        }
        val url = getUrl() ?: return@withContext null
        kotlin.runCatching {
            okHttpClient.newCall(
                Request.Builder()
                    .url(url)
                    .addHeader("Authorization", Credentials.basic(AppConfigs.webDAVAccount, AppConfigs.webDAVPassword))
                    .addHeader("Depth", "1")
                    .method("PROPFIND", requestPropsStr.toRequestBody("text/plain".toMediaType()))
                    .build()
            ).execute().body?.text()
        }.onFailure {
            logger().e(it, "propFindResponse")
        }.getOrNull()
    }

}

fun ResponseBody.text(encode: String? = null): String {
    val responseBytes = UTF8BOMFighter.removeUTF8BOM(bytes())
    var charsetName: String? = encode

    charsetName?.let {
        return String(responseBytes, Charset.forName(charsetName))
    }

    //根据http头判断
    contentType()?.charset()?.let {
        return String(responseBytes, it)
    }

    //根据内容判断
    charsetName = EncodingDetect.getHtmlEncode(responseBytes)
    return String(responseBytes, Charset.forName(charsetName))
}
