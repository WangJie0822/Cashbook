package cn.wj.android.cashbook

import android.app.Application
import android.content.Context
import androidx.multidex.MultiDex
import cn.wj.android.cashbook.BuildConfig
import cn.wj.android.cashbook.di.viewModelModule
import cn.wj.android.cashbook.ext.base.logger
import cn.wj.android.cashbook.manager.AppManager
import cn.wj.android.cashbook.manager.SkinManager
import cn.wj.android.cashbook.third.logger.MyFormatStrategy
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
        logger().i("MyApplication onCreate ${BuildConfig.VERSION_NAME}")

        // 初始化 Koin
        startKoin {
            androidLogger(Level.NONE)
            androidContext(this@MyApplication)
            modules(listOf(viewModelModule))
        }

        // 初始化换肤框架
        SkinManager.init(this)
    }
}