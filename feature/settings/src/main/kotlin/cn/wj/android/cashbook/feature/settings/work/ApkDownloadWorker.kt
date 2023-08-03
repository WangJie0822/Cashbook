package cn.wj.android.cashbook.feature.settings.work

import android.content.Context
import android.os.Environment
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import cn.wj.android.cashbook.core.common.ACTION_APK_NAME
import cn.wj.android.cashbook.core.common.ACTION_DOWNLOAD_URL
import cn.wj.android.cashbook.core.common.ext.logger
import cn.wj.android.cashbook.core.common.manager.AppManager
import cn.wj.android.cashbook.feature.settings.manager.UpdateManager
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * 升级包下载
 * - TODO 迁移到 `:sync:work`
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/21
 */
class ApkDownloadWorker(context: Context, workerParameters: WorkerParameters) :
    CoroutineWorker(context, workerParameters) {

    private val okhttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder().build()
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val apkName = inputData.getString(ACTION_APK_NAME).orEmpty()
        val downloadUrl = inputData.getString(ACTION_DOWNLOAD_URL).orEmpty()
        logger().d("doWork apkName: $apkName downloadUrl: $downloadUrl")

        val request = Request.Builder().url(downloadUrl).build()

        try {
            UpdateManager.showNotification()
            UpdateManager.downloading = true
            okhttpClient.newCall(request).execute().use { response ->
                val downloads =
                    AppManager.getContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                val saveFile = File(downloads, apkName)
                logger().d("doWork save path: ${saveFile.path}")

                // 文件总大小
                val total = response.body()!!.contentLength()

                if (saveFile.exists()) {
                    // 文件已存在
                    FileInputStream(saveFile).use {
                        if (it.available().toLong() == total) {
                            // 且大小相同，已下载
                            logger().d("doWork downloaded")
                            UpdateManager.hideNotification()
                            UpdateManager.install(saveFile)
                            return@withContext Result.success()
                        }
                    }
                }

                val buf = ByteArray(2048)
                var len: Int
                var sum = 0L

                var fos: FileOutputStream? = FileOutputStream(saveFile)
                response.body()!!.byteStream().use { inputStream ->
                    while (inputStream.read(buf).also { len = it } != -1) {
                        if (!isActive) {
                            // 已停止
                            logger().d("doWork stopped")
                            UpdateManager.hideNotification()
                            return@withContext Result.failure()
                        }
                        fos?.write(buf, 0, len)
                        sum += len.toLong()
                        val progress = (sum * 1f / total * 100).toInt()
                        UpdateManager.updateProgress(progress)
                    }
                    fos?.flush()
                    // 下载完成
                    logger().d("doWork download finish")
                    UpdateManager.hideNotification()
                    // 安装 APK
                    UpdateManager.install(saveFile)
                }
                try {
                    fos?.close()
                    fos = null
                } catch (throwable: Throwable) {
                    logger().e(throwable, "doWork fos close")
                }
            }
            return@withContext Result.success()
        } catch (throwable: Throwable) {
            logger().e(throwable, "doWork")
            UpdateManager.downloading = false
            UpdateManager.showDownloadError()
            return@withContext Result.failure()
        }
    }

}