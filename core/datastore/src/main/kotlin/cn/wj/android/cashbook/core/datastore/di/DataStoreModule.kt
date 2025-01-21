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

package cn.wj.android.cashbook.core.datastore.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.dataStoreFile
import cn.wj.android.cashbook.core.common.annotation.CashbookDispatchers
import cn.wj.android.cashbook.core.common.annotation.Dispatcher
import cn.wj.android.cashbook.core.datastore.AppPreferences
import cn.wj.android.cashbook.core.datastore.GitInfos
import cn.wj.android.cashbook.core.datastore.SearchHistory
import cn.wj.android.cashbook.core.datastore.TempKeys
import cn.wj.android.cashbook.core.datastore.serializer.AppPreferencesSerializer
import cn.wj.android.cashbook.core.datastore.serializer.GitInfosSerializer
import cn.wj.android.cashbook.core.datastore.serializer.SearchHistorySerializer
import cn.wj.android.cashbook.core.datastore.serializer.TempKeysSerializer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    @Provides
    @Singleton
    fun providesAppPreferencesDataStore(
        @ApplicationContext context: Context,
        @Dispatcher(CashbookDispatchers.IO) ioDispatcher: CoroutineContext,
        appPreferencesSerializer: AppPreferencesSerializer,
    ): DataStore<AppPreferences> =
        DataStoreFactory.create(
            serializer = appPreferencesSerializer,
            scope = CoroutineScope(ioDispatcher + SupervisorJob()),
        ) {
            context.dataStoreFile("app_preferences.pb")
        }

    @Provides
    @Singleton
    fun providesGitInfosDataStore(
        @ApplicationContext context: Context,
        @Dispatcher(CashbookDispatchers.IO) ioDispatcher: CoroutineContext,
        gitInfosSerializer: GitInfosSerializer,
    ): DataStore<GitInfos> =
        DataStoreFactory.create(
            serializer = gitInfosSerializer,
            scope = CoroutineScope(ioDispatcher + SupervisorJob()),
        ) {
            context.dataStoreFile("git_infos.pb")
        }

    @Provides
    @Singleton
    fun providesSearchHistoryDataStore(
        @ApplicationContext context: Context,
        @Dispatcher(CashbookDispatchers.IO) ioDispatcher: CoroutineContext,
        searchHistorySerializer: SearchHistorySerializer,
    ): DataStore<SearchHistory> =
        DataStoreFactory.create(
            serializer = searchHistorySerializer,
            scope = CoroutineScope(ioDispatcher + SupervisorJob()),
        ) {
            context.dataStoreFile("search_history.pb")
        }

    @Provides
    @Singleton
    fun providesTempKeysDataStore(
        @ApplicationContext context: Context,
        @Dispatcher(CashbookDispatchers.IO) ioDispatcher: CoroutineContext,
        tempKeysSerializer: TempKeysSerializer,
    ): DataStore<TempKeys> =
        DataStoreFactory.create(
            serializer = tempKeysSerializer,
            scope = CoroutineScope(ioDispatcher + SupervisorJob()),
        ) {
            context.dataStoreFile("temp_keys.pb")
        }
}
