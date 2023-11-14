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

package cn.wj.android.cashbook.sync.util

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import cn.wj.android.cashbook.core.common.ApplicationInfo
import cn.wj.android.cashbook.core.common.SERVICE_ACTION_RETRY_DOWNLOAD
import cn.wj.android.cashbook.core.common.ext.logger
import cn.wj.android.cashbook.core.common.ext.string
import cn.wj.android.cashbook.core.data.uitl.AppUpgradeManager
import cn.wj.android.cashbook.core.model.entity.UpgradeInfoEntity
import cn.wj.android.cashbook.sync.R
import cn.wj.android.cashbook.sync.initializers.ApkDownloadWorkName
import cn.wj.android.cashbook.sync.initializers.NoticeNotificationId
import cn.wj.android.cashbook.sync.initializers.UpgradeNotificationId
import cn.wj.android.cashbook.sync.initializers.noticeNotificationBuilder
import cn.wj.android.cashbook.sync.initializers.upgradeNotificationBuilder
import cn.wj.android.cashbook.sync.workers.ApkDownloadWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkManagerAppUpgradeManager @Inject constructor(
    @ApplicationContext private val context: Context,
) : AppUpgradeManager {

    private var upgradeInfo: UpgradeInfoEntity? = null

    private val _isDownloading = MutableStateFlow(false)

    override val isDownloading: Flow<Boolean> = _isDownloading

    override suspend fun startDownload(info: UpgradeInfoEntity) {
        upgradeInfo = info
        _isDownloading.tryEmit(true)
        showNotification()
        WorkManager.getInstance(context).apply {
            enqueueUniqueWork(
                ApkDownloadWorkName,
                ExistingWorkPolicy.KEEP,
                ApkDownloadWorker.startUpApkDownloadWork(
                    apkName = info.apkName,
                    downloadUrl = info.downloadUrl,
                ),
            )
        }
    }

    override suspend fun updateDownloadProgress(progress: Int) {
        updateProgress(progress)
    }

    override suspend fun downloadComplete(apkFile: File) {
        _isDownloading.tryEmit(false)
        hideNotification()
        install(apkFile)
    }

    override suspend fun downloadFailed() {
        hideNotification()
        val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            PendingIntent.getForegroundService(
                context,
                0,
                Intent(SERVICE_ACTION_RETRY_DOWNLOAD),
                PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        } else {
            PendingIntent.getService(
                context,
                0,
                Intent(SERVICE_ACTION_RETRY_DOWNLOAD),
                PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        nm.notify(
            NoticeNotificationId,
            context.noticeNotificationBuilder()
                .setContentTitle(R.string.update_error_title.string(context))
                .addAction(
                    0,
                    R.string.retry.string(context),
                    pendingIntent,
                )
                .build(),
        )
    }

    override suspend fun downloadStopped() {
        hideNotification()
    }

    override suspend fun cancelDownload() {
        _isDownloading.tryEmit(false)
        hideNotification()
        WorkManager.getInstance(context).cancelAllWorkByTag(ApkDownloadWorkName)
    }

    override suspend fun retry() {
        upgradeInfo?.let { startDownload(it) }
    }

    private val nm: NotificationManager by lazy {
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    private fun hideNotification() {
        progressTemp = -1
        nm.cancel(NoticeNotificationId)
        nm.cancel(UpgradeNotificationId)
    }

    private fun showNotification() {
        hideNotification()
        updateProgress(0)
    }

    private var progressTemp = -1

    private fun updateProgress(progress: Int) {
        if (progress == progressTemp) {
            return
        }
        logger().d("updateProgress $progress")
        progressTemp = progress
        nm.notify(
            UpgradeNotificationId,
            context.upgradeNotificationBuilder()
                .setContentText(R.string.update_progress_format.string(context).format(progress))
                .setProgress(100, progress, false)
                .build(),
        )
    }

    private fun install(file: File) {
        logger().d("install file: ${file.path}")
        try {
            context.startActivity(
                Intent(Intent.ACTION_VIEW).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        // 如果SDK版本>=24，即：Build.VERSION.SDK_INT >= 24
                        val authority = "${ApplicationInfo.applicationId}.FileProvider"
                        FileProvider.getUriForFile(context, authority, file)
                    } else {
                        Uri.fromFile(file)
                    }
                    setDataAndType(uri, "application/vnd.android.package-archive")
                },
            )
        } catch (throwable: Throwable) {
            logger().e(throwable, "install")
        }
    }
}
