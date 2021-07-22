@file:Suppress("unused")

package cn.wj.android.cashbook.databinding.adapter

import androidx.databinding.BindingAdapter
import androidx.databinding.InverseBindingAdapter
import androidx.databinding.InverseBindingListener
import com.haibin.calendarview.Calendar
import com.haibin.calendarview.CalendarView

/*
 * CalendarView DataBinding 适配器
 */

/** 用于处理手动设置日期导致多次回调问题 */
private var setSelectedDate: Boolean = false

/** 当前日历选择日期 */
@set:BindingAdapter("android:bind_calendar_selectedDate")
@get:InverseBindingAdapter(
    attribute = "android:bind_calendar_selectedDate",
    event = "android:bind_calendar_selectedDateAttrChanged"
)
var CalendarView.selectedDate: Calendar?
    get() = this.selectedCalendar
    set(value) {
        if (null != value) {
            if (this.selectedCalendar != value) {
                setSelectedDate = true
                scrollToCalendar(value.year, value.month, value.day)
            }
        }
    }

/** 设置日历变化回调 [onDaySelected] */
@BindingAdapter("android_bind_calendar_onDateSelected", "android:bind_calendar_selectedDateAttrChanged", requireAll = false)
fun CalendarView.setOnDateSelectedListener(onDaySelected: ((Calendar) -> Unit)?, listener: InverseBindingListener?) {
    this.setOnCalendarSelectListener(object : CalendarView.OnCalendarSelectListener {
        override fun onCalendarOutOfRange(calendar: Calendar) {
        }

        override fun onCalendarSelect(calendar: Calendar, isClick: Boolean) {
            onDaySelected?.invoke(calendar)
            if (setSelectedDate) {
                setSelectedDate = false
                return
            }
            listener?.onChange()
        }
    })
}

/** 设置标签列表 [data] */
@BindingAdapter("android:bind_calendar_schemes")
fun CalendarView.setSchemes(data: Map<String, Calendar>?) {
    this.setSchemeDate(data)
}