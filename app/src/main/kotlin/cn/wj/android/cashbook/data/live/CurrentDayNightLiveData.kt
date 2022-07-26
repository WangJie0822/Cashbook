package cn.wj.android.cashbook.data.live

import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.MutableLiveData
import cn.wj.android.cashbook.base.ext.base.orElse
import cn.wj.android.cashbook.data.config.AppConfigs
import cn.wj.android.cashbook.data.enums.DayNightEnum

/**
 * 当前白天黑夜模式数据
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/22
 */
object CurrentDayNightLiveData : MutableLiveData<DayNightEnum>(DayNightEnum.currentDayNight()) {

    val currentDayNight: DayNightEnum
        get() = value.orElse(DayNightEnum.currentDayNight())

    fun applyTheme() {
        value?.run {
            AppCompatDelegate.setDefaultNightMode(mode)
        }
    }

    override fun setValue(value: DayNightEnum?) {
        super.setValue(value)

        if (null != value) {
            AppCompatDelegate.setDefaultNightMode(value.mode)
            AppConfigs.datNightMode = value.mode
        }
    }
}