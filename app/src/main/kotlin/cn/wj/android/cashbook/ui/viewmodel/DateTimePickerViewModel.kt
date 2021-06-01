package cn.wj.android.cashbook.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import cn.wj.android.cashbook.base.ext.base.condition
import cn.wj.android.cashbook.base.tools.DATE_FORMAT_DATE
import cn.wj.android.cashbook.base.tools.DATE_FORMAT_TIME
import cn.wj.android.cashbook.base.tools.dateFormat
import cn.wj.android.cashbook.base.ui.BaseViewModel
import cn.wj.android.cashbook.data.model.UiNavigationModel

/**
 * 日期时间选择 ViewModel
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/1
 */
class DateTimePickerViewModel : BaseViewModel() {

    /** 当前时间 */
    val currentMs = System.currentTimeMillis()

    /** 确认保存日期时间数据 */
    val confirmDateData: MutableLiveData<String> = MutableLiveData()

    /** 当前日期 */
    val date: MutableLiveData<String> = object : MutableLiveData<String>(currentMs.dateFormat(DATE_FORMAT_DATE)) {
        override fun setValue(value: String?) {
            super.setValue(value)
            // 更新数据同步更新按钮显示文本
            showDate.value = showDate.value
        }
    }

    /** 当前时间 */
    var time: MutableLiveData<String> = object : MutableLiveData<String>(currentMs.dateFormat(DATE_FORMAT_TIME)) {
        override fun setValue(value: String?) {
            super.setValue(value)
            // 更新数据同步更新按钮显示文本
            showDate.value = showDate.value
        }
    }

    /** 是否显示选择日期 */
    val showDate: MutableLiveData<Boolean> = MutableLiveData(true)

    /** 转换按钮显示文本 */
    val transStr: LiveData<String> = showDate.map {
        if (it) {
            // 当前显示选择日期，按钮显示时间
            time.value
        } else {
            // 当前显示选择时间，按钮显示日期
            date.value
        }.orEmpty()
    }

    /** 转换按钮点击 */
    val onTransClick: () -> Unit = {
        showDate.value = !showDate.value.condition
    }

    /** 今天点击 */
    val onTodayClick: () -> Unit = {
        date.value = currentMs.dateFormat(DATE_FORMAT_DATE)
    }

    /** 取消点击 */
    val onCancelClick: () -> Unit = {
        uiNavigationData.value = UiNavigationModel.builder {
            close()
        }
    }

    /** 确认点击 */
    val onConfirmClick: () -> Unit = {
        // 组装返回数据
        val selectedDate = "${date.value} ${time.value}"
        confirmDateData.value = selectedDate
    }
}