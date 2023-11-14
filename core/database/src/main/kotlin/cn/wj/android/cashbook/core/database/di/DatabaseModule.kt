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

package cn.wj.android.cashbook.core.database.di

import android.content.Context
import androidx.room.Room
import cn.wj.android.cashbook.core.common.DB_FILE_NAME
import cn.wj.android.cashbook.core.common.DB_INIT_FILE_NAME
import cn.wj.android.cashbook.core.database.CashbookDatabase
import cn.wj.android.cashbook.core.database.dao.AssetDao
import cn.wj.android.cashbook.core.database.dao.BooksDao
import cn.wj.android.cashbook.core.database.dao.RecordDao
import cn.wj.android.cashbook.core.database.dao.TagDao
import cn.wj.android.cashbook.core.database.dao.TransactionDao
import cn.wj.android.cashbook.core.database.dao.TypeDao
import cn.wj.android.cashbook.core.database.migration.DatabaseMigrations
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Database 数据库依赖注入模块
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/2/17
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun providesDatabase(
        @ApplicationContext context: Context,
    ): CashbookDatabase = Room
        .databaseBuilder(
            context,
            CashbookDatabase::class.java,
            DB_FILE_NAME,
        )
        .createFromAsset(DB_INIT_FILE_NAME)
        .addMigrations(*DatabaseMigrations.MIGRATION_LIST)
        .build()

    @Provides
    @Singleton
    fun providesTypeDao(
        database: CashbookDatabase,
    ): TypeDao = database.typeDao()

    @Provides
    @Singleton
    fun providesAssetDao(
        database: CashbookDatabase,
    ): AssetDao = database.assetDao()

    @Provides
    @Singleton
    fun providesTagDao(
        database: CashbookDatabase,
    ): TagDao = database.tagDao()

    @Provides
    @Singleton
    fun providesRecordDao(
        database: CashbookDatabase,
    ): RecordDao = database.recordDao()

    @Provides
    @Singleton
    fun providesBooksDao(
        database: CashbookDatabase,
    ): BooksDao = database.booksDao()

    @Provides
    @Singleton
    fun providesTransactionDao(
        database: CashbookDatabase,
    ): TransactionDao = database.transactionDao()
}
