package cn.wj.android.cashbook.di

import androidx.room.Room
import cn.wj.android.cashbook.contants.DB_FILE_NAME
import cn.wj.android.cashbook.db.CashbookDatabase
import cn.wj.android.cashbook.manager.AppManager
import cn.wj.android.cashbook.ui.viewmodel.MainViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module


/**
 * 依赖注入模块
 *
 * > [jiewang41](mailto:jiewang41@iflytek.com) 创建于 20201/5/11
 */

/** 数据库相关依赖注入 */
val dbModule = module {
    single {
        Room.databaseBuilder(
            AppManager.getContext().applicationContext,
            CashbookDatabase::class.java,
            DB_FILE_NAME
        ).build()
    }
}

/** ViewModel 相关依赖注入 */
val viewModelModule = module {
    viewModel {
        MainViewModel()
    }
}