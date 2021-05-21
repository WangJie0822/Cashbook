package cn.wj.android.cashbook.di

import androidx.room.Room
import cn.wj.android.cashbook.data.constants.DB_FILE_NAME
import cn.wj.android.cashbook.data.database.CashbookDatabase
import cn.wj.android.cashbook.data.store.LocalDataStore
import cn.wj.android.cashbook.manager.AppManager
import cn.wj.android.cashbook.ui.viewmodel.MainViewModel
import cn.wj.android.cashbook.ui.viewmodel.SplashViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module


/**
 * 依赖注入模块
 *
 * > [jiewang41](mailto:jiewang41@iflytek.com) 创建于 20201/5/11
 */

/** 数据库相关依赖注入 */
val dbModule = module {
    single<CashbookDatabase> {
        Room.databaseBuilder(
            AppManager.getContext().applicationContext,
            CashbookDatabase::class.java,
            DB_FILE_NAME
        ).build()
    }
}

/** 数据存储相关依赖注入 */
val dataStoreModule = module {
    factory {
        LocalDataStore(get())
    }
}

/** ViewModel 相关依赖注入 */
val viewModelModule = module {
    viewModel {
        SplashViewModel(get())
    }
    viewModel {
        MainViewModel()
    }
}