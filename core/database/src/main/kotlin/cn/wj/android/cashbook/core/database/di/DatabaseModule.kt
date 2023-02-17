package cn.wj.android.cashbook.core.database.di

import android.content.Context
import androidx.room.Room
import cn.wj.android.cashbook.core.database.CashbookDatabase
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
    fun providesNiaDatabase(
        @ApplicationContext context: Context,
    ): CashbookDatabase = Room.databaseBuilder(
        context,
        CashbookDatabase::class.java,
        "cashbook.db",
    ).build()

}