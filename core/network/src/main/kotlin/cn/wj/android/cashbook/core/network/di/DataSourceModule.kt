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

package cn.wj.android.cashbook.core.network.di

import cn.wj.android.cashbook.core.common.ApplicationInfo
import cn.wj.android.cashbook.core.common.annotation.CashbookDispatchers
import cn.wj.android.cashbook.core.common.annotation.Dispatcher
import cn.wj.android.cashbook.core.common.tools.funLogger
import cn.wj.android.cashbook.core.network.datasource.NetworkDataSource
import cn.wj.android.cashbook.core.network.datasource.OfflineDataSource
import cn.wj.android.cashbook.core.network.datasource.RemoteDataSource
import cn.wj.android.cashbook.core.network.okhttp.LoggerInterceptor
import cn.wj.android.cashbook.core.network.util.OfflineWebDAVHandler
import cn.wj.android.cashbook.core.network.util.OkHttpWebDAVHandler
import cn.wj.android.cashbook.core.network.util.WebDAVHandler
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.Call
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

@Module
@InstallIn(SingletonComponent::class)
object DataSourceModule {

    @Provides
    @Singleton
    fun providesNetworkJson(): Json = Json {
        ignoreUnknownKeys = true
    }

    @Provides
    @Singleton
    fun okHttpCallFactory(): Call.Factory {
        // 构造日志拦截器：debug 渠道打印完整 body，其余渠道关闭日志
        val loggerInterceptor = LoggerInterceptor(
            { message ->
                funLogger("NET").d(message)
            },
            if (ApplicationInfo.debug) {
                LoggerInterceptor.LEVEL_BODY
            } else {
                LoggerInterceptor.LEVEL_NONE
            },
        ).apply {
            // 对敏感请求头脱敏，避免 WebDAV Basic 凭据等明文进入日志（redactHeader 返回 Unit，就地修改）
            redactHeader("Authorization")
            redactHeader("Cookie")
            redactHeader("Proxy-Authorization")
        }
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addNetworkInterceptor(loggerInterceptor)
            .build()
    }

    @Provides
    fun providesNetworkMonitor(
        networkJson: Json,
        okhttpCallFactory: Call.Factory,
    ): RemoteDataSource {
        return if (ApplicationInfo.isOffline) {
            OfflineDataSource()
        } else {
            NetworkDataSource(networkJson, okhttpCallFactory)
        }
    }

    @Provides
    fun providesWebDAVHandler(
        callFactory: Call.Factory,
        @Dispatcher(CashbookDispatchers.IO) ioCoroutineContext: CoroutineContext,
    ): WebDAVHandler {
        return if (ApplicationInfo.isOffline) {
            OfflineWebDAVHandler()
        } else {
            OkHttpWebDAVHandler(callFactory, ioCoroutineContext)
        }
    }
}
