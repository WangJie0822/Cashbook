package cn.wj.android.cashbook.di

import androidx.room.Room
import cn.wj.android.cashbook.BuildConfig
import cn.wj.android.cashbook.base.tools.funLogger
import cn.wj.android.cashbook.base.tools.jsonDefault
import cn.wj.android.cashbook.data.constants.DB_FILE_NAME
import cn.wj.android.cashbook.data.database.CashbookDatabase
import cn.wj.android.cashbook.data.database.CashbookDatabase.Companion.MIGRATION_LIST
import cn.wj.android.cashbook.data.net.UrlDefinition
import cn.wj.android.cashbook.data.net.WebService
import cn.wj.android.cashbook.data.repository.asset.AssetRepository
import cn.wj.android.cashbook.data.repository.books.BooksRepository
import cn.wj.android.cashbook.data.repository.main.MainRepository
import cn.wj.android.cashbook.data.repository.record.RecordRepository
import cn.wj.android.cashbook.data.repository.type.TypeRepository
import cn.wj.android.cashbook.third.okhttp.InterceptorLogger
import cn.wj.android.cashbook.third.okhttp.LoggerInterceptor
import cn.wj.android.cashbook.ui.asset.viewmodel.*
import cn.wj.android.cashbook.ui.books.viewmodel.EditBooksViewModel
import cn.wj.android.cashbook.ui.books.viewmodel.MyBooksViewModel
import cn.wj.android.cashbook.ui.general.viewmodel.GeneralViewModel
import cn.wj.android.cashbook.ui.general.viewmodel.ProgressViewModel
import cn.wj.android.cashbook.ui.main.viewmodel.*
import cn.wj.android.cashbook.ui.record.viewmodel.*
import cn.wj.android.cashbook.ui.type.viewmodel.*
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.ExperimentalSerializationApi
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import retrofit2.Retrofit

/**
 * 依赖注入模块
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 20201/5/11
 */

@OptIn(ExperimentalSerializationApi::class)
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
                LoggerInterceptor(
                    logger,
                    if (BuildConfig.DEBUG) LoggerInterceptor.LEVEL_BODY else LoggerInterceptor.LEVEL_NONE
                )
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

/** 数据库相关依赖注入 */
val dbModule = module {
    single {
        Room.databaseBuilder(
            androidContext(),
            CashbookDatabase::class.java,
            DB_FILE_NAME
        )
            .addMigrations(*MIGRATION_LIST)
            .build()
    }
}

/** 输出仓库相关依赖注入 */
val repositoryModule = module {
    factory {
        MainRepository(get(), get())
    }
    factory {
        AssetRepository(get())
    }
    factory {
        BooksRepository(get())
    }
    factory {
        RecordRepository(get())
    }
    factory {
        TypeRepository(get())
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
    viewModel {
        EditTypeListViewModel()
    }
    viewModel {
        TypeListViewModel(get())
    }
    viewModel {
        EditTypeViewModel(get())
    }
    viewModel {
        ReplaceTypeViewModel(get())
    }
    viewModel {
        CalendarViewModel(get())
    }
    viewModel {
        SearchRecordViewModel(get())
    }
    viewModel {
        SelectFirstTypeViewModel(get())
    }
    viewModel {
        BackupViewModel(get())
    }
    viewModel {
        TypeRecordViewModel(get())
    }
    viewModel {
        TagManagerViewModel(get())
    }
    viewModel {
        TagRecordViewModel(get())
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
        EditTagViewModel(get())
    }
    viewModel {
        EditTypeMenuViewModel()
    }
    viewModel {
        SelectYearMonthViewModel()
    }
    viewModel {
        ProgressViewModel()
    }
    viewModel {
        AssetMoreInfoViewModel()
    }
    viewModel {
        EditPasswordViewModel()
    }
    viewModel {
        VerifyPasswordViewModel()
    }
}