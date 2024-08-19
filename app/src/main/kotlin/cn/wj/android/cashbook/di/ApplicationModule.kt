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

package cn.wj.android.cashbook.di

import android.content.Context
import cn.wj.android.cashbook.BuildConfig
import cn.wj.android.cashbook.core.data.uitl.AppUpgradeManager
import cn.wj.android.cashbook.sync.util.OfflineAppUpgradeManager
import cn.wj.android.cashbook.sync.util.WorkManagerAppUpgradeManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 应用全局依赖注入
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2024/8/19
 */
@Module
@InstallIn(SingletonComponent::class)
object ApplicationModule {

    @Provides
    @Singleton
    fun providesAppUpgradeManager(
        @ApplicationContext context: Context,
    ): AppUpgradeManager = if (BuildConfig.FLAVOR.contains("offline", true)) {
        OfflineAppUpgradeManager()
    } else {
        WorkManagerAppUpgradeManager(context)
    }
}
