@file:Suppress("unused")

package cn.wj.android.cashbook

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import cn.wj.android.cashbook.base.ext.base.logger
import cn.wj.android.cashbook.base.ext.base.string
import cn.wj.android.cashbook.base.tools.jumpAppDetails
import cn.wj.android.cashbook.data.constants.NOTIFICATION_CHANNEL_APP
import cn.wj.android.cashbook.data.constants.NOTIFICATION_CHANNEL_UPDATE
import cn.wj.android.cashbook.data.live.CurrentDayNightLiveData
import cn.wj.android.cashbook.di.dbModule
import cn.wj.android.cashbook.di.netModule
import cn.wj.android.cashbook.di.repositoryModule
import cn.wj.android.cashbook.di.viewModelModule
import cn.wj.android.cashbook.manager.AppManager
import cn.wj.android.cashbook.third.logger.MyFormatStrategy
import com.didichuxing.doraemonkit.DoraemonKit
import com.didichuxing.doraemonkit.kit.IKit
import com.orhanobut.logger.AndroidLogAdapter
import com.orhanobut.logger.Logger
import dagger.hilt.android.HiltAndroidApp
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

/**
 * 全局应用
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/5/11
 */
@HiltAndroidApp
class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()

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

        // 初始化 Koin
        startKoin {
            androidLogger(Level.NONE)
            androidContext(this@MyApplication)
            modules(listOf(netModule, dbModule, repositoryModule, viewModelModule))
        }

        // 应用主题
        CurrentDayNightLiveData.applyTheme()

        // 初始化通知渠道
        initNotificationChannel()

        // 初始化 DoraemonKit
        initDoraemon()


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

    /** 初始化 DoraemonKit */
    private fun initDoraemon() {

        fun createKit(name: Int, onClick: (Context) -> Unit): IKit {
            return object : IKit {
                override fun getCategory() = 4

                override fun getName() = name

                override fun getIcon() = R.drawable.ic_launcher

                override fun onClick(context: Context) {
                    onClick.invoke(context)
                }

                override fun onAppInit(context: Context?) {
                }
            }
        }

        DoraemonKit.install(
            this, arrayListOf(
                createKit(R.string.application_details) {
                    jumpAppDetails()
                }
            )
        )
    }
}