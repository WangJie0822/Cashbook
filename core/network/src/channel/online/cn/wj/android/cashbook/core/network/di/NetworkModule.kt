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
import cn.wj.android.cashbook.core.common.tools.funLogger
import cn.wj.android.cashbook.core.network.okhttp.LoggerInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.Call
import okhttp3.OkHttpClient
import javax.inject.Singleton

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
                if (ApplicationInfo.debug) {
                    LoggerInterceptor.LEVEL_BODY
                } else {
                    LoggerInterceptor.LEVEL_NONE
                },
            ),
        )
        .build()
}
