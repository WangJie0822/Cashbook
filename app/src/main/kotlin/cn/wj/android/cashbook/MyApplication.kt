@file:Suppress("unused")

package cn.wj.android.cashbook

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import cn.wj.android.cashbook.core.common.ApplicationInfo
import cn.wj.android.cashbook.core.common.NOTIFICATION_CHANNEL_APP
import cn.wj.android.cashbook.core.common.NOTIFICATION_CHANNEL_UPDATE
import cn.wj.android.cashbook.core.common.ext.logger
import cn.wj.android.cashbook.core.common.ext.string
import cn.wj.android.cashbook.core.common.manager.AppManager
import cn.wj.android.cashbook.core.common.third.MyFormatStrategy
import cn.wj.android.cashbook.core.ui.R
import cn.wj.android.cashbook.sync.initializers.Sync
import com.orhanobut.logger.AndroidLogAdapter
import com.orhanobut.logger.Logger
import dagger.hilt.android.HiltAndroidApp

/**
 * 全局应用
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/5/11
 */
@HiltAndroidApp
class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // 初始化应用信息
        ApplicationInfo.setFlavor(BuildConfig.FLAVOR)
        ApplicationInfo.applicationId = BuildConfig.APPLICATION_ID
        ApplicationInfo.versionName = BuildConfig.VERSION_NAME

        // 注册应用管理
        AppManager.register(this)

        // 初始化 Logger 日志打印
        val strategy = MyFormatStrategy.newBuilder()
            .borderPriority(Log.WARN)
            .headerPriority(Log.WARN)
            .tag("CASHBOOK")
            .build()
        Logger.addLogAdapter(object : AndroidLogAdapter(strategy) {
            override fun isLoggable(priority: Int, tag: String?): Boolean {
                return BuildConfig.DEBUG
            }
        })
        logger().d("MyApplication onCreate ${BuildConfig.VERSION_NAME}")

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            logger("UncaughtException").e(throwable, "UncaughtException $thread")
            AppManager.finishAllActivity()
        }

        // 初始化同步服务
        Sync.initialize(this)

        // 初始化通知渠道
        initNotificationChannel()
    }

    /** 初始化通知渠道 */
    private fun initNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            // 应用通知
            val appChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_APP,
                R.string.channel_app_name.string,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = R.string.channel_app_description.string
            }
            // 更新通知
            val updateChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_UPDATE,
                R.string.channel_update_name.string,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = R.string.channel_update_description.string
            }
            nm.createNotificationChannels(mutableListOf(appChannel, updateChannel))
        }
    }
}