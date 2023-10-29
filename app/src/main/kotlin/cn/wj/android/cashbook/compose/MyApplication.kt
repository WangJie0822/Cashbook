@file:Suppress("unused")

package cn.wj.android.cashbook.compose

import android.app.Application
import android.util.Log
import cn.wj.android.cashbook.compose.BuildConfig
import cn.wj.android.cashbook.core.common.ApplicationInfo
import cn.wj.android.cashbook.core.common.ext.logger
import cn.wj.android.cashbook.core.common.manager.AppManager
import cn.wj.android.cashbook.core.common.third.MyFormatStrategy
import cn.wj.android.cashbook.sync.initializers.Sync
import com.didichuxing.doraemonkit.DoKit
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

        // 初始化 didi 开发工具
        DoKit.Builder(this).build()
    }

}