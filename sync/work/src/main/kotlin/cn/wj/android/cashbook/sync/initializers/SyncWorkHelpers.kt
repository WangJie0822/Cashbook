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

package cn.wj.android.cashbook.sync.initializers

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Constraints
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import cn.wj.android.cashbook.core.common.SERVICE_ACTION_CANCEL_DOWNLOAD
import cn.wj.android.cashbook.core.common.ext.string
import cn.wj.android.cashbook.sync.R

internal const val SyncNotificationId = 20013
internal const val SyncNotificationChannelID = "SyncNotificationChannel"

/** 需要网络连接 */
internal val NetworkConstraints
    get() = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

/** 同步任务前台信息 */
internal fun Context.syncForegroundInfo() = ForegroundInfo(
    SyncNotificationId,
    syncWorkNotification(),
)

/**
 * 同步任务通知
 */
internal fun Context.syncWorkNotification(): Notification {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            SyncNotificationChannelID,
            getString(R.string.sync_notification_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = getString(R.string.sync_notification_channel_description)
        }
        // 注册通知通道
        val notificationManager: NotificationManager? =
            getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.createNotificationChannel(channel)
    }

    return NotificationCompat.Builder(
        this,
        SyncNotificationChannelID,
    )
        .setSmallIcon(
            R.drawable.ic_notification,
        )
        .setContentTitle(getString(R.string.sync_notification_title))
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .build()
}

internal const val NoticeNotificationId = 20014
internal const val NoticeNotificationChannelID = "NoticeNotificationChannel"

/**
 * 提示任务通知
 */
internal fun Context.noticeNotificationBuilder(): NotificationCompat.Builder {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            NoticeNotificationChannelID,
            getString(R.string.notice_notification_channel_app_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = getString(R.string.notice_notification_channel_app_description)
        }
        // 注册通知通道
        val notificationManager: NotificationManager? =
            getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.createNotificationChannel(channel)
    }

    return NotificationCompat.Builder(
        this,
        NoticeNotificationChannelID,
    )
        .setSmallIcon(
            R.drawable.ic_notification,
        )
        .setWhen(System.currentTimeMillis())
        .setCategory(Notification.CATEGORY_RECOMMENDATION)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
}

internal const val UpgradeNotificationId = 20015
internal const val UpgradeNotificationChannelID = "UpgradeNotificationChannel"

/**
 * 更新任务通知
 */
internal fun Context.upgradeNotificationBuilder(): NotificationCompat.Builder {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            UpgradeNotificationChannelID,
            getString(R.string.upgrade_notification_channel_update_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = getString(R.string.upgrade_notification_channel_update_description)
        }
        // 注册通知通道
        val notificationManager: NotificationManager? =
            getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.createNotificationChannel(channel)
    }

    val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        PendingIntent.getForegroundService(
            this,
            0,
            Intent(SERVICE_ACTION_CANCEL_DOWNLOAD),
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    } else {
        PendingIntent.getService(
            this,
            0,
            Intent(SERVICE_ACTION_CANCEL_DOWNLOAD),
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
    return NotificationCompat.Builder(
        this,
        UpgradeNotificationChannelID,
    )
        .setContentTitle(R.string.update_progress_title.string(this))
        .setSmallIcon(
            R.drawable.ic_notification,
        )
        .setWhen(System.currentTimeMillis())
        .setSilent(true)
        .setCategory(Notification.CATEGORY_PROGRESS)
        .setOngoing(true)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .addAction(
            0,
            R.string.cancel.string(this),
            pendingIntent,
        )
}
