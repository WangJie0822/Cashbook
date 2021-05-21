package cn.wj.android.cashbook.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import cn.wj.android.cashbook.base.ui.BaseViewModel
import cn.wj.android.cashbook.data.live.CurrentBooksLiveData

/**
 * 主界面 ViewModel
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/5/11
 */
class MainViewModel : BaseViewModel() {

    /** 账本名称 */
    val booksName: LiveData<String> = CurrentBooksLiveData.map { it.name }
}