package cn.wj.android.cashbook.ui.record.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.base.ext.base.orElse
import cn.wj.android.cashbook.base.ext.base.string
import cn.wj.android.cashbook.base.ui.BaseViewModel
import cn.wj.android.cashbook.data.event.LifecycleEvent
import java.util.Calendar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 选择年、月弹窗
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/7/22
 */
class SelectYearMonthViewModel : BaseViewModel() {

    private val yearStr: String by lazy {
        R.string.year.string
    }

    /** 月份选择事件 */
    val monthSelectedEvent: LifecycleEvent<String> = LifecycleEvent()

    /** 记录年份 */
    var tempYear: Int = 2021
        set(value) {
            field = value
            val tabsList = tabs.value
            if (null == tabsList) {
                viewModelScope.launch {
                    delay(220L)
                    yearCurrentItem.value = tabs.value?.indexOf("$value$yearStr").orElse(0)
                }
            } else {
                yearCurrentItem.value = tabsList.indexOf("$value$yearStr")
            }
        }

    /** 选中年份下标 */
    val yearCurrentItem: MutableLiveData<Int> = MutableLiveData()

    /** 已选中月份 */
    val selectedMonth: MutableLiveData<Int> = MutableLiveData()

    /** 标签列表 */
    val tabs: MutableLiveData<List<String>> = object : MutableLiveData<List<String>>() {
        override fun onActive() {
            // 获取标签列表数据
            value = arrayListOf<String>().apply {
                val startYear = Calendar.getInstance().get(Calendar.YEAR) + 1
                for (i in 0..30) {
                    add("${startYear - i}$yearStr")
                }
            }
        }
    }

    /** 月份点击 */
    val onMonthClick: (Int) -> Unit = { month ->
        val year = tabs.value?.get(yearCurrentItem.value.orElse(0)).orElse(tempYear.toString()).replace(yearStr, "")
        val monthStr = if (month < 10) "0$month" else month.toString()
        monthSelectedEvent.value = "$year-$monthStr"
    }
}