package cn.wj.android.cashbook.di

import cn.wj.android.cashbook.ui.viewmodel.MainViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module


/**
 * 依赖注入模块
 *
 * > [jiewang41](mailto:jiewang41@iflytek.com) 创建于 20201/5/11
 */

/** ViewModel 相关依赖注入 */
val viewModelModule = module {
    viewModel {
        MainViewModel()
    }
}