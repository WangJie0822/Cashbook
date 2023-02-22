package cn.wj.android.cashbook.core.database.di

import android.content.Context
import androidx.room.Room
import cn.wj.android.cashbook.core.database.CashbookDatabase
import cn.wj.android.cashbook.core.database.DatabaseMigrations
import cn.wj.android.cashbook.core.database.dao.AssetDao
import cn.wj.android.cashbook.core.database.dao.TypeDao
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
            "cashbook.db",
        )
        .createFromAsset("cashbook.db")
        .addMigrations(*DatabaseMigrations.MIGRATION_LIST)
        .build()

    @Provides
    @Singleton
    fun providesTypeDao(
        database: CashbookDatabase
    ): TypeDao = database.typeDao()

    @Provides
    @Singleton
    fun providesAssetDao(
        database: CashbookDatabase
    ): AssetDao = database.assetDao()

}