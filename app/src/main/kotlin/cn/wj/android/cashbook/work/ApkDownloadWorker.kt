package cn.wj.android.cashbook.work

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import cn.wj.android.cashbook.BuildConfig
import cn.wj.android.cashbook.base.ext.base.logger
import cn.wj.android.cashbook.data.constants.ACTION_APK_NAME
import cn.wj.android.cashbook.data.constants.ACTION_DOWNLOAD_URL
import cn.wj.android.cashbook.manager.AppManager
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * 升级包下载
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/21
 */
class ApkDownloadWorker(context: Context, workerParameters: WorkerParameters) : CoroutineWorker(context, workerParameters) {

    private val okhttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder().build()
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    override suspend fun doWork(): Result = coroutineScope {
        withContext(Dispatchers.IO) {
            val apkName = inputData.getString(ACTION_APK_NAME).orEmpty()
            val downloadUrl = inputData.getString(ACTION_DOWNLOAD_URL).orEmpty()
            logger().d("doWork apkName: $apkName downloadUrl: $downloadUrl")

            val request = Request.Builder().url(downloadUrl).build()
            try {
                okhttpClient.newCall(request).execute().use { response ->
                    val downloads = AppManager.getContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                    val saveFile = File(downloads, apkName)
                    logger().d("doWork save path: ${saveFile.path}")

                    // 文件总大小
                    val total = response.body!!.contentLength()

                    if (saveFile.exists()) {
                        // 文件已存在
                        FileInputStream(saveFile).use {
                            if (it.available().toLong() == total) {
                                // 且大小相同，已下载
                                logger().d("doWork downloaded")
                                install(saveFile)
                                return@withContext Result.success()
                            }
                        }
                    }

                    val buf = ByteArray(2048)
                    var len: Int
                    var sum = 0L

                    var fos: FileOutputStream? = FileOutputStream(saveFile)

                    response.body!!.byteStream().use { inputStream ->
                        while (inputStream.read(buf).also { len = it } != -1) {
                            fos?.write(buf, 0, len)
                            sum += len.toLong()
                            val progress = (sum * 1f / total * 100).toInt()
                            // TODO 下载中
                            logger().d("doWork downloading: $progress")
                        }
                        fos?.flush()
                        // 下载完成
                        logger().d("doWork download finish")
                        // 安装 APK
                        install(saveFile)
                    }
                    try {
                        fos?.close()
                        fos = null
                    } catch (throwable: Throwable) {
                    }
                }
            } catch (throwable: Throwable) {
                logger().e(throwable, "doWork")
                return@withContext Result.failure()
            }

            return@withContext Result.success()
        }
    }

    private fun install(file: File) {
        logger().d("install file: ${file.path}")
        try {
            val context = AppManager.getContext()
            context.startActivity(Intent(Intent.ACTION_VIEW).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    //如果SDK版本>=24，即：Build.VERSION.SDK_INT >= 24
                    val authority = "${BuildConfig.APPLICATION_ID}.FileProvider"
                    val uri = FileProvider.getUriForFile(context, authority, file)
                    setDataAndType(uri, "application/vnd.android.package-archive")
                } else {
                    val uri = Uri.fromFile(file)
                    setDataAndType(uri, "application/vnd.android.package-archive")
                }
            })
        } catch (throwable: Throwable) {
            logger().e(throwable, "install")
        }
    }
}