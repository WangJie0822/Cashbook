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

import cn.wj.android.cashbook.core.network.datasource.OfflineDataSource
import cn.wj.android.cashbook.core.network.datasource.RemoteDataSource
import cn.wj.android.cashbook.core.network.util.OfflineWebDAVHandler
import cn.wj.android.cashbook.core.network.util.WebDAVHandler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Suppress("unused")
@Module
@InstallIn(SingletonComponent::class)
interface DataSourceModule {

    @Binds
    fun bindRemoteDataSource(
        offlineDataSource: OfflineDataSource,
    ): RemoteDataSource

    @Binds
    fun bindWebDAVHandler(
        offlineWebDAVHandler: OfflineWebDAVHandler,
    ): WebDAVHandler
}
