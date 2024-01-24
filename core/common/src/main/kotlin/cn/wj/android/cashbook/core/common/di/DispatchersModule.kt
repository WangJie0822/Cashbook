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

package cn.wj.android.cashbook.core.common.di

import cn.wj.android.cashbook.core.common.ApplicationCoroutineScope
import cn.wj.android.cashbook.core.common.annotation.CashbookDispatchers
import cn.wj.android.cashbook.core.common.annotation.Dispatcher
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

@Module
@InstallIn(SingletonComponent::class)
object DispatchersModule {

    @Provides
    @Dispatcher(CashbookDispatchers.Default)
    fun providesDefaultCoroutineContext(): CoroutineContext = Dispatchers.Default

    @Provides
    @Dispatcher(CashbookDispatchers.Main)
    fun providesMainCoroutineContext(): CoroutineContext = Dispatchers.Main

    @Provides
    @Dispatcher(CashbookDispatchers.IO)
    fun providesIOCoroutineContext(): CoroutineContext = Dispatchers.IO

    @Provides
    @Singleton
    fun providesApplicationCoroutineScope(): ApplicationCoroutineScope = ApplicationCoroutineScope()
}
