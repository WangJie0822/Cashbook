package cn.wj.android.cashbook.core.network.di

import cn.wj.android.cashbook.core.common.ApplicationInfo
import cn.wj.android.cashbook.core.common.tools.funLogger
import cn.wj.android.cashbook.core.network.okhttp.LoggerInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.Call
import okhttp3.OkHttpClient

/**
 * 网络模块
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/6/14
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun providesNetworkJson(): Json = Json {
        ignoreUnknownKeys = true
    }

    @Provides
    @Singleton
    fun okHttpCallFactory(): Call.Factory = OkHttpClient.Builder()
        .addNetworkInterceptor(
            LoggerInterceptor(
                { message ->
                    funLogger("NET").d(message)
                },
                if (ApplicationInfo.isDev) LoggerInterceptor.LEVEL_BODY else LoggerInterceptor.LEVEL_NONE
            )
        )
        .build()
}