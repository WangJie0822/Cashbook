package cn.wj.android.cashbook.feature.settings.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import cn.wj.android.cashbook.core.common.ApplicationInfo
import cn.wj.android.cashbook.core.common.INTENT_KEY_CANCEL_DOWNLOAD
import cn.wj.android.cashbook.core.common.INTENT_KEY_RETRY_DOWNLOAD
import cn.wj.android.cashbook.core.common.ext.logger
import cn.wj.android.cashbook.feature.settings.manager.UpdateManager

/**
 * 事件广播接收者
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/24
 */
class EventReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        logger().d("onReceive ${intent?.extras?.toString()}")
        if (null == intent) {
            return
        }
        if (intent.hasExtra(INTENT_KEY_CANCEL_DOWNLOAD)) {
            // 取消下载
            UpdateManager.cancelDownload()
        }
        if (intent.hasExtra(INTENT_KEY_RETRY_DOWNLOAD)) {
            // 重试下载
            UpdateManager.retry()
        }
    }

    companion object {

        val ACTION = "${ApplicationInfo.applicationId}.EventReceiver"

        /**
         * 注册广播监听
         */
        fun register(context: Context) {
            context.registerReceiver(
                EventReceiver(),
                IntentFilter(ACTION)
            )
        }
    }
}