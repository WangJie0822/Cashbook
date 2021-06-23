package cn.wj.android.cashbook.di

import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import cn.wj.android.cashbook.BuildConfig
import cn.wj.android.cashbook.base.tools.funLogger
import cn.wj.android.cashbook.base.tools.jsonDefault
import cn.wj.android.cashbook.data.constants.DB_FILE_NAME
import cn.wj.android.cashbook.data.database.CashbookDatabase
import cn.wj.android.cashbook.data.net.UrlDefinition
import cn.wj.android.cashbook.data.net.WebService
import cn.wj.android.cashbook.data.store.LocalDataStore
import cn.wj.android.cashbook.data.store.WebDataStore
import cn.wj.android.cashbook.manager.AppManager
import cn.wj.android.cashbook.third.okhttp.InterceptorLogger
import cn.wj.android.cashbook.third.okhttp.LoggerInterceptor
import cn.wj.android.cashbook.ui.asset.viewmodel.AssetInfoViewModel
import cn.wj.android.cashbook.ui.asset.viewmodel.AssetLongClickMenuViewModel
import cn.wj.android.cashbook.ui.asset.viewmodel.AssetMoreMenuViewModel
import cn.wj.android.cashbook.ui.asset.viewmodel.EditAssetViewModel
import cn.wj.android.cashbook.ui.asset.viewmodel.InvisibleAssetLongClickMenuViewModel
import cn.wj.android.cashbook.ui.asset.viewmodel.InvisibleAssetViewModel
import cn.wj.android.cashbook.ui.asset.viewmodel.MyAssetViewModel
import cn.wj.android.cashbook.ui.asset.viewmodel.SelectAssetClassificationViewModel
import cn.wj.android.cashbook.ui.asset.viewmodel.SelectAssetViewModel
import cn.wj.android.cashbook.ui.asset.viewmodel.SelectDayViewModel
import cn.wj.android.cashbook.ui.books.viewmodel.EditBooksViewModel
import cn.wj.android.cashbook.ui.books.viewmodel.MyBooksViewModel
import cn.wj.android.cashbook.ui.general.viewmodel.GeneralViewModel
import cn.wj.android.cashbook.ui.main.viewmodel.AboutUsViewModel
import cn.wj.android.cashbook.ui.main.viewmodel.MainViewModel
import cn.wj.android.cashbook.ui.main.viewmodel.MarkdownViewModel
import cn.wj.android.cashbook.ui.main.viewmodel.SettingViewModel
import cn.wj.android.cashbook.ui.main.viewmodel.SplashViewModel
import cn.wj.android.cashbook.ui.record.viewmodel.CalculatorViewModel
import cn.wj.android.cashbook.ui.record.viewmodel.ConsumptionTypeViewModel
import cn.wj.android.cashbook.ui.record.viewmodel.CreateTagViewModel
import cn.wj.android.cashbook.ui.record.viewmodel.DateTimePickerViewModel
import cn.wj.android.cashbook.ui.record.viewmodel.EditRecordViewModel
import cn.wj.android.cashbook.ui.record.viewmodel.RecordInfoViewModel
import cn.wj.android.cashbook.ui.record.viewmodel.SelectAssociatedRecordViewModel
import cn.wj.android.cashbook.ui.record.viewmodel.SelectTagViewModel
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import retrofit2.Retrofit

/**
 * 依赖注入模块
 *
 * > [jiewang41](mailto:jiewang41@iflytek.com) 创建于 20201/5/11
 */

val netModule = module {
    single {
        // 日志打印
        val logger = object : InterceptorLogger {
            override fun invoke(msg: String) {
                funLogger("NET").d(msg)
            }
        }
        OkHttpClient.Builder()
            .retryOnConnectionFailure(true)
            .addNetworkInterceptor(
                LoggerInterceptor(logger, if (BuildConfig.DEBUG) LoggerInterceptor.LEVEL_BODY else LoggerInterceptor.LEVEL_NONE)
            )
            .build()
    }

    single {
        Retrofit.Builder()
            .baseUrl(UrlDefinition.BASE_URL)
            .addConverterFactory(jsonDefault.asConverterFactory("application/json; charset=UTF-8".toMediaType()))
            .client(get())
            .build()
    }

    single {
        get<Retrofit>().create(WebService::class.java)
    }
}

/** 数据库升级 1 -> 2 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("CREATE TABLE IF NOT EXISTS `db_tag` (`id` INTEGER PRIMARY KEY AUTOINCREMENT, `name` TEXT NOT NULL)")
    }
}

/** 数据库相关依赖注入 */
val dbModule = module {
    single {
        Room.databaseBuilder(
            AppManager.getContext().applicationContext,
            CashbookDatabase::class.java,
            DB_FILE_NAME
        )
            .addMigrations(MIGRATION_1_2)
            .build()
    }
}

/** 数据存储相关依赖注入 */
val dataStoreModule = module {
    factory {
        LocalDataStore(get())
    }
    factory {
        WebDataStore(get())
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
    viewModel {
        AboutUsViewModel(get())
    }
    viewModel {
        MarkdownViewModel()
    }
    viewModel {
        SettingViewModel()
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
    viewModel {
        SelectDayViewModel()
    }
    viewModel {
        SelectTagViewModel(get())
    }
    viewModel {
        CreateTagViewModel(get())
    }
}