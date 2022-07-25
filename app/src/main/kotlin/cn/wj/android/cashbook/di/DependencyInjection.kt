package cn.wj.android.cashbook.di

import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import cn.wj.android.cashbook.BuildConfig
import cn.wj.android.cashbook.base.tools.funLogger
import cn.wj.android.cashbook.base.tools.jsonDefault
import cn.wj.android.cashbook.data.constants.DB_FILE_NAME
import cn.wj.android.cashbook.data.constants.SWITCH_INT_ON
import cn.wj.android.cashbook.data.database.CashbookDatabase
import cn.wj.android.cashbook.data.net.UrlDefinition
import cn.wj.android.cashbook.data.net.WebService
import cn.wj.android.cashbook.data.repository.asset.AssetRepository
import cn.wj.android.cashbook.data.repository.books.BooksRepository
import cn.wj.android.cashbook.data.repository.main.MainRepository
import cn.wj.android.cashbook.data.repository.record.RecordRepository
import cn.wj.android.cashbook.data.repository.type.TypeRepository
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
import cn.wj.android.cashbook.ui.general.viewmodel.ProgressViewModel
import cn.wj.android.cashbook.ui.main.viewmodel.AboutUsViewModel
import cn.wj.android.cashbook.ui.main.viewmodel.BackupViewModel
import cn.wj.android.cashbook.ui.main.viewmodel.MainViewModel
import cn.wj.android.cashbook.ui.main.viewmodel.MarkdownViewModel
import cn.wj.android.cashbook.ui.main.viewmodel.SettingViewModel
import cn.wj.android.cashbook.ui.main.viewmodel.SplashViewModel
import cn.wj.android.cashbook.ui.record.viewmodel.CalculatorViewModel
import cn.wj.android.cashbook.ui.record.viewmodel.CalendarViewModel
import cn.wj.android.cashbook.ui.record.viewmodel.DateTimePickerViewModel
import cn.wj.android.cashbook.ui.record.viewmodel.EditRecordViewModel
import cn.wj.android.cashbook.ui.record.viewmodel.EditTagViewModel
import cn.wj.android.cashbook.ui.record.viewmodel.RecordInfoViewModel
import cn.wj.android.cashbook.ui.record.viewmodel.SearchRecordViewModel
import cn.wj.android.cashbook.ui.record.viewmodel.SelectAssociatedRecordViewModel
import cn.wj.android.cashbook.ui.record.viewmodel.SelectTagViewModel
import cn.wj.android.cashbook.ui.record.viewmodel.SelectYearMonthViewModel
import cn.wj.android.cashbook.ui.record.viewmodel.TypeRecordViewModel
import cn.wj.android.cashbook.ui.type.viewmodel.ConsumptionTypeViewModel
import cn.wj.android.cashbook.ui.type.viewmodel.EditTypeListViewModel
import cn.wj.android.cashbook.ui.type.viewmodel.EditTypeMenuViewModel
import cn.wj.android.cashbook.ui.type.viewmodel.EditTypeViewModel
import cn.wj.android.cashbook.ui.type.viewmodel.ReplaceTypeViewModel
import cn.wj.android.cashbook.ui.type.viewmodel.SelectFirstTypeViewModel
import cn.wj.android.cashbook.ui.type.viewmodel.TypeListViewModel
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidContext
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

/** 数据库升级 1 -> 2 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // 新增 标签 表
        database.execSQL("CREATE TABLE IF NOT EXISTS `db_tag` (`id` INTEGER PRIMARY KEY AUTOINCREMENT, `name` TEXT NOT NULL)")
    }
}

/** 数据库升级 2 -> 3 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // 更新记录表数据
        // 将旧表重命名
        database.execSQL("ALTER TABLE `db_record` RENAME TO `db_record_temp`")
        // 新建新表
        database.execSQL("CREATE TABLE IF NOT EXISTS `db_record` (`id` INTEGER PRIMARY KEY AUTOINCREMENT, `type_enum` TEXT NOT NULL, `type_id` INTEGER NOT NULL, `asset_id` INTEGER NOT NULL, `into_asset_id` INTEGER NOT NULL, `books_id` INTEGER NOT NULL, `record_id` INTEGER NOT NULL, `amount` REAL NOT NULL, `charge` REAL NOT NULL, `remark` TEXT NOT NULL, `tag_ids` TEXT NOT NULL, `reimbursable` INTEGER NOT NULL, `system` INTEGER NOT NULL, `record_time` INTEGER NOT NULL, `create_time` INTEGER NOT NULL, `modify_time` INTEGER NOT NULL)")
        // 从旧表中查询数据插入新表
        database.execSQL("INSERT INTO `db_record` SELECT `id`, `type`, `first_type_id`, `asset_id`, `into_asset_id`, `books_id`, `record_id`, `amount`, `charge`, `remark`, `tag_ids`, `reimbursable`, `system`, `record_time`, `create_time`, `modify_time` FROM `db_record_temp`")
        // 删除旧表
        database.execSQL("DROP TABLE `db_record_temp`")

        // 更新分类表数据
        // 将旧表重命名
        database.execSQL("ALTER TABLE `db_type` RENAME TO `db_type_temp`")
        // 新建新表
        database.execSQL("CREATE TABLE IF NOT EXISTS `db_type` (`id` INTEGER PRIMARY KEY AUTOINCREMENT, `parent_id` INTEGER NOT NULL, `name` TEXT NOT NULL, `icon_res_name` TEXT NOT NULL, `type` TEXT NOT NULL, `record_type` INTEGER NOT NULL, `child_enable` INTEGER NOT NULL, `refund` INTEGER NOT NULL, `reimburse` INTEGER NOT NULL, `sort` INTEGER NOT NULL)")
        // 从旧表中查询数据插入新表
        database.execSQL("INSERT INTO `db_type` SELECT `id`, `parent_id`, `name`, `icon_res_name`, `type`, `record_type`, `child_enable`, `refund`, `reimburse`, `sort` FROM `db_type_temp`")
        // 删除旧表
        database.execSQL("DROP TABLE `db_type_temp`")
    }
}

/** 数据库升级 3 -> 4 */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // 标签表新增所属账本主键
        database.execSQL("ALTER TABLE `db_tag` ADD `books_id` INTEGER DEFAULT -1 NOT NULL")
        database.execSQL("ALTER TABLE `db_tag` ADD `shared` INTEGER DEFAULT $SWITCH_INT_ON NOT NULL")
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
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
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
}