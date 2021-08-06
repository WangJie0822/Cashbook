package cn.wj.android.cashbook.manager

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import cn.wj.android.cashbook.BuildConfig
import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.base.ext.base.logger
import cn.wj.android.cashbook.base.ext.base.string
import cn.wj.android.cashbook.data.constants.*
import cn.wj.android.cashbook.data.entity.UpdateInfoEntity
import cn.wj.android.cashbook.receiver.EventReceiver
import cn.wj.android.cashbook.work.ApkDownloadWorker
import java.io.File

/**
 * 更新管理类
 *
 * - 创建时间：2021/6/20
 *
 * @author 王杰
 */
object UpdateManager {

    private const val downloadWorkerTag = "ApkDownloadWorker"

    private val manager: WorkManager by lazy {
        WorkManager.getInstance(AppManager.getContext())
    }

    private var download: UpdateInfoEntity? = null

    fun checkFromInfo(info: UpdateInfoEntity, need: () -> Unit, noNeed: () -> Unit) {
        logger().d("checkFromInfo info: $info")
        if (BuildConfig.DEBUG) {
            // Debug 环境，永远需要更新
            need.invoke()
            return
        }
        if (!needUpdate(info.versionName)) {
            // 已是最新版本
            noNeed.invoke()
            return
        }
        // 不是最新版本
        if (info.downloadUrl.isBlank()) {
            // 没有下载资源
            noNeed.invoke()
            return
        }
        need.invoke()
    }

    /** 根据网络返回的版本信息判断是否需要更新 */
    private fun needUpdate(versionName: String?): Boolean {
        if (versionName.isNullOrBlank()) {
            return false
        }
        val localSplits = BuildConfig.VERSION_NAME.split("_")
        val splits = versionName.split("_")
        val localVersions = localSplits.first().replace("v", "").split(".")
        val versions = splits.first().replace("v", "").split(".")
        if (localSplits.first() == splits.first()) {
            return splits[1].toInt() > localSplits[1].toInt()
        }
        for (i in localVersions.indices) {
            if (versions[i] > localVersions[i]) {
                return true
            }
        }
        return false
    }

    fun startDownload(info: UpdateInfoEntity) {
        download = info
        manager.enqueue(
            OneTimeWorkRequestBuilder<ApkDownloadWorker>()
                .setInputData(
                    Data.Builder()
                        .putString(ACTION_APK_NAME, info.apkName)
                        .putString(ACTION_DOWNLOAD_URL, info.downloadUrl)
                        .build()
                )
                .addTag(downloadWorkerTag)
                .build()
        )
    }

    fun retry() {
        download?.run {
            startDownload(this)
        }
    }

    fun install(file: File) {
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

    private val nm: NotificationManager by lazy {
        val context = AppManager.getContext()
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    private val builder: NotificationCompat.Builder by lazy {
        val context = AppManager.getContext()
        NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_UPDATE)
            .setContentTitle(R.string.update_progress_title.string)
            .setContentText(R.string.update_progress_format.string.format(0))
            .setWhen(System.currentTimeMillis())
            .setNotificationSilent()
            .setCategory(Notification.CATEGORY_PROGRESS)
            .setOngoing(true)
            .setProgress(100, 0, false)
            .addAction(0, R.string.cancel.string, PendingIntent.getBroadcast(context, 0, Intent(EventReceiver.ACTION).apply {
                putExtra(INTENT_KEY_CANCEL_DOWNLOAD, "")
            }, if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE else PendingIntent.FLAG_CANCEL_CURRENT))
            .setSmallIcon(R.mipmap.ic_launcher)
            .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher))
    }

    fun cancelDownload() {
        manager.cancelAllWorkByTag(downloadWorkerTag)
        hideNotification()
    }

    fun showNotification() {
        hideNotification()
        nm.notify(
            NOTIFICATION_ID_UPDATE,
            builder
                .setContentText(R.string.update_progress_format.string.format(0))
                .setProgress(100, 0, false)
                .build()
        )
    }

    private var progressTemp = -1

    fun updateProgress(progress: Int) {
        if (progress == progressTemp) {
            return
        }
        logger().d("updateProgress $progress")
        progressTemp = progress
        nm.notify(
            NOTIFICATION_ID_UPDATE,
            builder
                .setContentText(R.string.update_progress_format.string.format(progress))
                .setProgress(100, progress, false)
                .build()
        )
    }

    fun hideNotification() {
        nm.cancel(NOTIFICATION_ID_UPDATE)
        nm.cancel(NOTIFICATION_ID_UPDATE_ERROR)
    }

    fun showDownloadError() {
        hideNotification()
        val context = AppManager.getContext()
        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_UPDATE)
            .setContentTitle(R.string.update_error_title.string)
            .setWhen(System.currentTimeMillis())
            .setCategory(Notification.CATEGORY_RECOMMENDATION)
            .addAction(0, R.string.retry.string, PendingIntent.getBroadcast(context, 0, Intent(EventReceiver.ACTION).apply {
                putExtra(INTENT_KEY_RETRY_DOWNLOAD, "")
            }, if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE else PendingIntent.FLAG_CANCEL_CURRENT))
            .setSmallIcon(R.mipmap.ic_launcher)
            .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher))
            .build()
        nm.notify(
            NOTIFICATION_ID_UPDATE_ERROR,
            notification
        )
    }
}