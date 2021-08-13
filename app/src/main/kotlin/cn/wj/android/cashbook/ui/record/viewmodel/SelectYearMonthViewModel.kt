package cn.wj.android.cashbook.ui.record.viewmodel

import androidx.lifecycle.MutableLiveData
import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.base.ext.base.orElse
import cn.wj.android.cashbook.base.ext.base.string
import cn.wj.android.cashbook.base.ui.BaseViewModel
import cn.wj.android.cashbook.data.event.LifecycleEvent
import java.util.Calendar

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
            yearCurrentItem.value = tabs.indexOf("$value$yearStr")
        }

    /** 选中年份下标 */
    val yearCurrentItem: MutableLiveData<Int> = MutableLiveData()

    /** 已选中月份 */
    val selectedMonth: MutableLiveData<Int> = MutableLiveData()

    /** 标签列表 */
    val tabs: List<String> by lazy {
        arrayListOf<String>().apply {
            val startYear = Calendar.getInstance().get(Calendar.YEAR) + 1
            for (i in 0..10) {
                add("${startYear - i}$yearStr")
            }
        }
    }

    /** 月份点击 */
    val onMonthClick: (Int) -> Unit = { month ->
        val year = tabs[yearCurrentItem.value.orElse(0)].replace(yearStr, "")
        val monthStr = if (month < 10) "0$month" else month.toString()
        monthSelectedEvent.value = "$year-$monthStr"
    }
}