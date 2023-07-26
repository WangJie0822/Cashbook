package cn.wj.android.cashbook.sync.initializers

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Constraints
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import cn.wj.android.cashbook.sync.R

const val SYNC_TOPIC = "sync"
internal  const val SyncNotificationId = 20013
internal  const val SyncNotificationChannelID = "SyncNotificationChannel"

// All sync work needs an internet connectionS
internal val SyncConstraints
    get() = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

internal val BackupRecoveryConstraints
    get() = Constraints.Builder()
        .build()

/**
 * Foreground information for sync on lower API levels when sync workers are being
 * run with a foreground service
 */
internal fun Context.syncForegroundInfo() = ForegroundInfo(
    SyncNotificationId,
    syncWorkNotification(),
)

/**
 * Notification displayed on lower API levels when sync workers are being
 * run with a foreground service
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
        // Register the channel with the system
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
