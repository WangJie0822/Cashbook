package cn.wj.android.cashbook.core.database

import androidx.room.Room
import cn.wj.android.cashbook.core.model.DB_FILE_NAME
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/** 数据库依赖注入模块 */
val DatabaseModule = module {
    single {
        Room.databaseBuilder(
            androidContext(),
            CashbookDatabase::class.java,
            DB_FILE_NAME
        )
            .addMigrations(*DatabaseMigrations.MIGRATION_LIST)
            .build()
    }
}