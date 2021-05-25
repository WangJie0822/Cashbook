package cn.wj.android.cashbook.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import cn.wj.android.cashbook.base.ui.BaseViewModel
import cn.wj.android.cashbook.data.live.CurrentBooksLiveData
import cn.wj.android.cashbook.data.observable.ObservableMoney
import cn.wj.android.cashbook.data.store.LocalDataStore

/**
 * 主界面 ViewModel
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/5/11
 */
class MainViewModel(private val local: LocalDataStore) : BaseViewModel() {

    /** 账本名称 */
    val booksName: LiveData<String> = CurrentBooksLiveData.map { it.name }

    /** 顶部背景图片 */
    val topBgImage: LiveData<String> = CurrentBooksLiveData.map { it.imageUrl }

    /** 列表刷新状态 */
    val refreshing: MutableLiveData<Boolean> = MutableLiveData()

    /** 账单数据 */
    val billListData: LiveData<PagingData<String>>
        get() = local.getBillList().cachedIn(viewModelScope)

    /** 本月支出 */
    val spending: ObservableMoney = ObservableMoney()

    /** 本月收入 */
    val income: ObservableMoney = ObservableMoney()

    /** 本月结余 */
    val balance: ObservableMoney = object : ObservableMoney(spending, income) {
        override fun get(): String {
            return (income.bigDecimalVal - spending.bigDecimalVal).toPlainString()
        }
    }
}