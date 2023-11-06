package cn.wj.android.cashbook.sync.service

import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import cn.wj.android.cashbook.core.common.SERVICE_ACTION_RETRY
import cn.wj.android.cashbook.core.data.uitl.SyncManager
import cn.wj.android.cashbook.sync.initializers.SyncNotificationId
import cn.wj.android.cashbook.sync.initializers.syncWorkNotification
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * 同步服务
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/7/14
 */
@AndroidEntryPoint
class SyncService : Service() {

    @Inject
    lateinit var syncManager: SyncManager

    override fun onCreate() {
        super.onCreate()
        startForeground(SyncNotificationId, syncWorkNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            SERVICE_ACTION_RETRY -> {
                syncManager.requestSync()
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_DETACH)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(false)
        }
    }
}