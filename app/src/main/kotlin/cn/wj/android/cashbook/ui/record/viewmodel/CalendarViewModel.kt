package cn.wj.android.cashbook.ui.record.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import cn.wj.android.cashbook.base.ext.base.logger
import cn.wj.android.cashbook.base.ui.BaseViewModel
import cn.wj.android.cashbook.data.constants.ROUTE_PATH_RECORD_SEARCH
import cn.wj.android.cashbook.data.entity.DateRecordEntity
import cn.wj.android.cashbook.data.entity.RecordEntity
import cn.wj.android.cashbook.data.event.LifecycleEvent
import cn.wj.android.cashbook.data.model.UiNavigationModel
import cn.wj.android.cashbook.data.store.LocalDataStore
import cn.wj.android.cashbook.interfaces.RecordListClickListener
import com.haibin.calendarview.Calendar
import kotlinx.coroutines.launch

/**
 * 日历界面 ViewModel
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/7/15
 */
class CalendarViewModel(private val local: LocalDataStore) : BaseViewModel(), RecordListClickListener {

    /** 旧的缓存数据，用于判断处理日期变换时标记数据 */
    private var oldCalendar: Calendar = Calendar()
    private var oldMap: Map<String, Calendar> = hashMapOf()

    /** 标记 - 是否是刷新 */
    private var refresh = false

    /** 显示记录详情弹窗事件 */
    val showRecordDetailsDialogEvent: LifecycleEvent<RecordEntity> = LifecycleEvent()

    /** 显示选择年月弹出事件 */
    val showSelectYearMonthDialogEvent: LifecycleEvent<(String) -> Unit> = LifecycleEvent()

    /** 选中日期 */
    val selectedDate: MutableLiveData<Calendar> = object : MutableLiveData<Calendar>() {
        override fun onActive() {
            val temp = value
            value = Calendar().apply {
                if (null == temp) {
                    // 绑定数据时如果为空，默认加载当天
                    val cal = java.util.Calendar.getInstance()
                    year = cal.get(java.util.Calendar.YEAR)
                    month = cal.get(java.util.Calendar.MONTH) + 1
                    day = cal.get(java.util.Calendar.DAY_OF_MONTH)
                } else {
                    // 已选择日期，刷新
                    year = temp.year
                    month = temp.month
                    day = temp.day
                }
            }
        }
    }

    /** 标题文本 */
    val titleStr: LiveData<String> = selectedDate.map {
        "${it.year}-${it.month}"
    }

    /** 列表数据 */
    val listData: LiveData<List<DateRecordEntity>> = selectedDate.switchMap {
        val result = MutableLiveData<List<DateRecordEntity>>()
        viewModelScope.launch {
            try {
                result.value = local.getRecordListByDate(it)
            } catch (throwable: Throwable) {
                logger().e(throwable, "getRecordListByDate")
            }
        }
        result
    }

    /** 标记数据 */
    val schemeData: LiveData<Map<String, Calendar>> = selectedDate.switchMap {
        val result = MutableLiveData<Map<String, Calendar>>()
        viewModelScope.launch {
            try {
                result.value = if (it.year == oldCalendar.year && it.month == oldCalendar.month && !refresh) {
                    // 月份未改变且不是刷新
                    oldMap
                } else {
                    // 月份已改变或者是刷新，获取数据
                    refresh = false
                    oldCalendar = it
                    oldMap = local.getCalendarSchemesByDate(it)
                    oldMap
                }
            } catch (throwable: Throwable) {
                logger().e(throwable, "")
            }
        }
        result
    }

    /** 返回点击 */
    val onBackClick: () -> Unit = {
        uiNavigationEvent.value = UiNavigationModel.builder {
            close()
        }
    }

    /** 搜索点击 */
    val onSearchClick: () -> Unit = {
        uiNavigationEvent.value = UiNavigationModel.builder {
            jump(ROUTE_PATH_RECORD_SEARCH)
        }
    }

    /** 标题点击 */
    val onTitleClick: () -> Unit = {
        // 显示弹窗
        showSelectYearMonthDialogEvent.value = { date ->
            // 选择回调，更新选中日期
            selectedDate.value = Calendar().apply {
                val splits = date.split("-")
                year = splits.first().toInt()
                month = splits.last().toInt()
                day = 1
            }
        }
    }

    /** 记录 Item 点击 */
    override val onRecordItemClick: (RecordEntity) -> Unit = { item ->
        showRecordDetailsDialogEvent.value = item
    }

    /** 刷新数据 */
    fun refresh() {
        val selectedCalendar = selectedDate.value ?: return
        refresh = true
        selectedDate.value = Calendar().apply {
            this.year = selectedCalendar.year
            this.month = selectedCalendar.month
            this.day = selectedCalendar.day
        }
    }
}