package cn.wj.android.cashbook.di

import androidx.room.Room
import cn.wj.android.cashbook.data.constants.DB_FILE_NAME
import cn.wj.android.cashbook.data.database.CashbookDatabase
import cn.wj.android.cashbook.data.store.LocalDataStore
import cn.wj.android.cashbook.manager.AppManager
import cn.wj.android.cashbook.ui.asset.viewmodel.AssetInfoViewModel
import cn.wj.android.cashbook.ui.asset.viewmodel.AssetLongClickMenuViewModel
import cn.wj.android.cashbook.ui.asset.viewmodel.AssetMoreMenuViewModel
import cn.wj.android.cashbook.ui.asset.viewmodel.EditAssetViewModel
import cn.wj.android.cashbook.ui.asset.viewmodel.InvisibleAssetLongClickMenuViewModel
import cn.wj.android.cashbook.ui.asset.viewmodel.InvisibleAssetViewModel
import cn.wj.android.cashbook.ui.asset.viewmodel.MyAssetViewModel
import cn.wj.android.cashbook.ui.asset.viewmodel.SelectAssetClassificationViewModel
import cn.wj.android.cashbook.ui.asset.viewmodel.SelectAssetViewModel
import cn.wj.android.cashbook.ui.books.viewmodel.EditBooksViewModel
import cn.wj.android.cashbook.ui.books.viewmodel.MyBooksViewModel
import cn.wj.android.cashbook.ui.general.viewmodel.GeneralViewModel
import cn.wj.android.cashbook.ui.main.viewmodel.MainViewModel
import cn.wj.android.cashbook.ui.main.viewmodel.SplashViewModel
import cn.wj.android.cashbook.ui.record.viewmodel.CalculatorViewModel
import cn.wj.android.cashbook.ui.record.viewmodel.DateTimePickerViewModel
import cn.wj.android.cashbook.ui.record.viewmodel.EditRecordViewModel
import cn.wj.android.cashbook.ui.record.viewmodel.RecordInfoViewModel
import cn.wj.android.cashbook.ui.record.viewmodel.SelectAssociatedRecordViewModel
import cn.wj.android.cashbook.ui.type.viewmodel.ConsumptionTypeViewModel
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

/** 数据存储相关依赖注入 */
val dataStoreModule = module {
    factory {
        LocalDataStore(get())
    }
}

/** ViewModel 相关依赖注入 */
val viewModelModule = module {
    // Activity
    viewModel {
        SplashViewModel(get())
    }
    viewModel {
        MainViewModel(get())
    }
    viewModel {
        MyBooksViewModel(get())
    }
    viewModel {
        EditBooksViewModel(get())
    }
    viewModel {
        EditRecordViewModel(get())
    }
    viewModel {
        EditAssetViewModel(get())
    }
    viewModel {
        MyAssetViewModel(get())
    }
    viewModel {
        InvisibleAssetViewModel(get())
    }
    viewModel {
        AssetInfoViewModel(get())
    }
    viewModel {
        SelectAssociatedRecordViewModel(get())
    }

    // Fragment
    viewModel {
        ConsumptionTypeViewModel(get())
    }

    // Dialog
    viewModel {
        GeneralViewModel()
    }
    viewModel {
        DateTimePickerViewModel()
    }
    viewModel {
        SelectAssetViewModel(get())
    }
    viewModel {
        SelectAssetClassificationViewModel(get())
    }
    viewModel {
        AssetLongClickMenuViewModel()
    }
    viewModel {
        AssetMoreMenuViewModel()
    }
    viewModel {
        InvisibleAssetLongClickMenuViewModel()
    }
    viewModel {
        CalculatorViewModel()
    }
    viewModel {
        RecordInfoViewModel(get())
    }
}