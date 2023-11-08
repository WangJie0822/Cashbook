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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

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
