package cn.wj.android.cashbook.sync.service

import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import cn.wj.android.cashbook.core.common.SERVICE_ACTION_CANCEL_DOWNLOAD
import cn.wj.android.cashbook.core.common.SERVICE_ACTION_RETRY_DOWNLOAD
import cn.wj.android.cashbook.core.data.uitl.AppUpgradeManager
import cn.wj.android.cashbook.sync.initializers.SyncNotificationId
import cn.wj.android.cashbook.sync.initializers.syncWorkNotification
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * 升级服务
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/10/20
 */
@AndroidEntryPoint
class UpgradeService : Service() {

    private val coroutineScope: CoroutineScope = object : CoroutineScope {
        override val coroutineContext: CoroutineContext
            get() = SupervisorJob() + Dispatchers.Main.immediate
    }

    @Inject
    lateinit var appUpgradeManager: AppUpgradeManager

    override fun onCreate() {
        super.onCreate()
        startForeground(SyncNotificationId, syncWorkNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            SERVICE_ACTION_CANCEL_DOWNLOAD -> {
                coroutineScope.launch {
                    appUpgradeManager.cancelDownload()
                }
            }

            SERVICE_ACTION_RETRY_DOWNLOAD -> {
                coroutineScope.launch {
                    appUpgradeManager.retry()
                }
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
        coroutineScope.cancel()
    }
}