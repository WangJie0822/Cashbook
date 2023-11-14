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

package cn.wj.android.cashbook.core.data.di

import cn.wj.android.cashbook.core.data.repository.AssetRepository
import cn.wj.android.cashbook.core.data.repository.BooksRepository
import cn.wj.android.cashbook.core.data.repository.RecordRepository
import cn.wj.android.cashbook.core.data.repository.SettingRepository
import cn.wj.android.cashbook.core.data.repository.TagRepository
import cn.wj.android.cashbook.core.data.repository.TypeRepository
import cn.wj.android.cashbook.core.data.repository.impl.AssetRepositoryImpl
import cn.wj.android.cashbook.core.data.repository.impl.BooksRepositoryImpl
import cn.wj.android.cashbook.core.data.repository.impl.RecordRepositoryImpl
import cn.wj.android.cashbook.core.data.repository.impl.SettingRepositoryImpl
import cn.wj.android.cashbook.core.data.repository.impl.TagRepositoryImpl
import cn.wj.android.cashbook.core.data.repository.impl.TypeRepositoryImpl
import cn.wj.android.cashbook.core.data.uitl.NetworkMonitor
import cn.wj.android.cashbook.core.data.uitl.impl.ConnectivityManagerNetworkMonitor
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Suppress("unused")
@Module
@InstallIn(SingletonComponent::class)
interface DataModule {

    @Binds
    fun bindNetworkMonitor(
        networkMonitor: ConnectivityManagerNetworkMonitor,
    ): NetworkMonitor

    @Binds
    fun bindTypeRepository(
        repository: TypeRepositoryImpl,
    ): TypeRepository

    @Binds
    fun bindTagRepository(
        repository: TagRepositoryImpl,
    ): TagRepository

    @Binds
    fun bindAssetRepository(
        repository: AssetRepositoryImpl,
    ): AssetRepository

    @Binds
    fun bindRecordRepository(
        repository: RecordRepositoryImpl,
    ): RecordRepository

    @Binds
    fun bindBooksRepository(
        repository: BooksRepositoryImpl,
    ): BooksRepository

    @Binds
    fun bindSettingRepository(
        repository: SettingRepositoryImpl,
    ): SettingRepository
}
