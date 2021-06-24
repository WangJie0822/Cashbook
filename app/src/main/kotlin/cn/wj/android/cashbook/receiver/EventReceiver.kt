package cn.wj.android.cashbook.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import cn.wj.android.cashbook.BuildConfig
import cn.wj.android.cashbook.base.ext.base.logger
import cn.wj.android.cashbook.data.constants.INTENT_KEY_CANCEL_DOWNLOAD
import cn.wj.android.cashbook.data.constants.INTENT_KEY_RETRY_DOWNLOAD
import cn.wj.android.cashbook.manager.UpdateManager

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

        const val ACTION = "${BuildConfig.APPLICATION_ID}.EventReceiver"

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