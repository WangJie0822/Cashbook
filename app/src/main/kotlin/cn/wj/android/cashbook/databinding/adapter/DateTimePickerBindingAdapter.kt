@file:Suppress("unused")

package cn.wj.android.cashbook.databinding.adapter

import android.os.Build
import android.widget.DatePicker
import android.widget.TimePicker
import androidx.databinding.BindingAdapter
import androidx.databinding.InverseBindingAdapter
import androidx.databinding.InverseBindingListener

/**
 * DatePicker TimePicker DataBinding 适配器
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/1
 */

/** 当前日期 */
@set:BindingAdapter("android:bind_datePicker_currentDate")
@get:InverseBindingAdapter(
    attribute = "android:bind_datePicker_currentDate",
    event = "android:bind_datePicker_currentDateAttrChanged"
)
var DatePicker.currentDate: String
    get() {
        val realMonth = month + 1
        return "$year-${
            if (realMonth < 10) {
                "0$realMonth"
            } else {
                "$realMonth"
            }
        }-${
            if (dayOfMonth < 10) {
                "0$dayOfMonth"
            } else {
                "$dayOfMonth"
            }
        }"
    }
    set(value) {
        val splits = value.split("-")
        if (splits.size == 3) {
            updateDate(splits[0].toInt(), splits[1].toInt() - 1, splits[2].toInt())
        }
    }

/** 设置日期变化监听 */
@BindingAdapter(
    "android:bind_datePicker_currentDate",
    "android:bind_datePicker_currentDateAttrChanged",
    requireAll = false
)
fun DatePicker.setOnDateChangedListener(
    onChanged: ((String) -> Unit)?,
    listener: InverseBindingListener?
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        setOnDateChangedListener { _, year, monthOfYear, dayOfMonth ->
            onChanged?.invoke("$year-$monthOfYear-$dayOfMonth")
            listener?.onChange()
        }
    } else {
        init(year, month, dayOfMonth) { _, year, monthOfYear, dayOfMonth ->
            onChanged?.invoke("$year-$monthOfYear-$dayOfMonth")
            listener?.onChange()
        }
    }
}

/** 当前时间 */
@Suppress("DEPRECATION")
@set:BindingAdapter("android:bind_timePicker_currentTime")
@get:InverseBindingAdapter(
    attribute = "android:bind_timePicker_currentTime",
    event = "android:bind_timePicker_currentTimeAttrChanged"
)
var TimePicker.currentTime: String
    get() {
        val realHour = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            hour
        } else {
            currentHour
        }
        val realMinute = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            minute
        } else {
            currentMinute
        }
        return "${
            if (realHour < 10) {
                "0$realHour"
            } else {
                "$realHour"
            }
        }:${
            if (realMinute < 10) {
                "0$realMinute"
            } else {
                "$realMinute"
            }
        }"
    }
    set(value) {
        val splits = value.split(":")
        if (splits.size == 2) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                hour = splits[0].toInt()
                minute = splits[1].toInt()
            } else {
                currentHour = splits[0].toInt()
                currentMinute = splits[1].toInt()
            }
        }
    }

/** 设置日期变化监听 */
@BindingAdapter(
    "android:bind_timePicker_currentTime",
    "android:bind_timePicker_currentTimeAttrChanged",
    requireAll = false
)
fun TimePicker.setOnDateChangedListener(
    onChanged: ((String) -> Unit)?,
    listener: InverseBindingListener?
) {
    setOnTimeChangedListener { _, hourOfDay, minute ->
        onChanged?.invoke("${hourOfDay}:${minute}")
        listener?.onChange()
    }
}

/** 设置是否是 24 小时制 */
@BindingAdapter("android:bind_timePicker_24Hours")
fun TimePicker.set24Hours(is24Hours: Boolean?) {
    if (null == is24Hours) {
        return
    }
    setIs24HourView(is24Hours)
}