package cn.wj.android.cashbook.ui.asset.viewmodel

import androidx.lifecycle.MutableLiveData
import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.base.ext.base.string
import cn.wj.android.cashbook.base.ui.BaseViewModel
import cn.wj.android.cashbook.data.event.LifecycleEvent

/**
 * 选择日期 ViewModel
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/23
 */
class SelectDayViewModel : BaseViewModel() {

    /** 确认点击事件 */
    val confirmClickEvent: LifecycleEvent<Int> = LifecycleEvent()

    /** 日期数据 */
    val dayData: MutableLiveData<List<String>> = MutableLiveData(arrayListOf<String>().apply {
        for (i in 0 until 30) {
            add("${i + 1}${R.string.day.string}")
        }
    })

    /** 确认点击 */
    val onConfirmClick: () -> Unit = {
        confirmClickEvent.value = 0
    }
}