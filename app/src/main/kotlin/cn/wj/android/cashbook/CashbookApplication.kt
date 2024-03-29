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

package cn.wj.android.cashbook

import android.app.Application
import android.util.Log
import cn.wj.android.cashbook.core.common.ApplicationCoroutineScope
import cn.wj.android.cashbook.core.common.ApplicationInfo
import cn.wj.android.cashbook.core.common.ext.logger
import cn.wj.android.cashbook.core.common.manager.AppManager
import cn.wj.android.cashbook.core.common.third.MyFormatStrategy
import cn.wj.android.cashbook.core.data.repository.SettingRepository
import cn.wj.android.cashbook.sync.initializers.Sync
import com.orhanobut.logger.AndroidLogAdapter
import com.orhanobut.logger.Logger
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 全局应用
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/5/11
 */
@HiltAndroidApp
class CashbookApplication : Application() {

    @Inject
    lateinit var applicationScope: ApplicationCoroutineScope

    @Inject
    lateinit var settingsRepository: SettingRepository

    private var logcatEnable = false

    override fun onCreate() {
        super.onCreate()

        applicationScope.launch {
            settingsRepository.appDataMode.collectLatest {
                logcatEnable = it.logcatInRelease
            }
        }

        // 初始化应用信息
        with(ApplicationInfo) {
            setFlavor(BuildConfig.FLAVOR)
            applicationId = BuildConfig.APPLICATION_ID
            versionName = BuildConfig.VERSION_NAME
            debug = BuildConfig.DEBUG
        }

        // 注册应用管理
        AppManager.register(this)

        // 初始化 Logger 日志打印
        val strategy = MyFormatStrategy.newBuilder()
            .borderPriority(Log.WARN)
            .headerPriority(Log.WARN)
            .tag("CASHBOOK")
            .build()
        Logger.addLogAdapter(
            object : AndroidLogAdapter(strategy) {
                override fun isLoggable(priority: Int, tag: String?): Boolean {
                    // debug版本、非产线版本或通过后门开启日志，有日志输出
                    return BuildConfig.DEBUG || !ApplicationInfo.isProduction || ApplicationInfo.logcatEnable || logcatEnable
                }
            },
        )
        logger().d("Application onCreate ${BuildConfig.VERSION_NAME}")

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            logger("UncaughtException").e(throwable, "UncaughtException $thread")
            AppManager.finishAllActivity()
        }

        // 初始化同步服务
        Sync.initialize(this)
    }
}
