@file:Suppress("unused")

package cn.wj.android.cashbook

import android.app.Application
import android.content.Context
import androidx.multidex.MultiDex
import cn.wj.android.cashbook.base.ext.base.logger
import cn.wj.android.cashbook.di.dataStoreModule
import cn.wj.android.cashbook.di.dbModule
import cn.wj.android.cashbook.di.viewModelModule
import cn.wj.android.cashbook.manager.AppManager
import cn.wj.android.cashbook.third.logger.MyFormatStrategy
import com.alibaba.android.arouter.launcher.ARouter
import com.orhanobut.logger.AndroidLogAdapter
import com.orhanobut.logger.Logger
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

/**
 * 全局应用
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/5/11
 */
class MyApplication : Application() {

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)

        // 安装 dex
        MultiDex.install(this)
    }

    override fun onCreate() {
        super.onCreate()

        // 注册应用管理
        AppManager.register(this)

        // 初始化 Logger 日志打印
        val strategy = MyFormatStrategy.newBuilder()
            .tag("CASHBOOK")
            .build()
        Logger.addLogAdapter(object : AndroidLogAdapter(strategy) {
            override fun isLoggable(priority: Int, tag: String?): Boolean {
                return true
            }
        })
        logger().d("MyApplication onCreate ${BuildConfig.VERSION_NAME}")

        // 初始化 Koin
        startKoin {
            androidLogger(Level.NONE)
            androidContext(this@MyApplication)
            modules(listOf(dbModule, dataStoreModule, viewModelModule))
        }

        // 初始化 ARouter
        if (BuildConfig.DEBUG) {
            // Debug 模式或非线上模式
            ARouter.openLog()
            ARouter.openDebug()
        }
        ARouter.init(this)
    }
}