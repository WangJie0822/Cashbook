package cn.wj.android.cashbook.data.live

import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.MutableLiveData
import cn.wj.android.cashbook.base.ext.base.orElse
import cn.wj.android.cashbook.data.config.AppConfigs
import cn.wj.android.cashbook.data.enums.ThemeEnum

/**
 * 当前主题数据
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/22
 */
object CurrentThemeLiveData : MutableLiveData<ThemeEnum>(ThemeEnum.currentTheme()) {

    val currentTheme: ThemeEnum
        get() = value.orElse(ThemeEnum.currentTheme())

    fun applyTheme() {
        value?.run {
            AppCompatDelegate.setDefaultNightMode(mode)
        }
    }

    override fun setValue(value: ThemeEnum?) {
        super.setValue(value)

        if (null != value) {
            AppCompatDelegate.setDefaultNightMode(value.mode)
            AppConfigs.themeMode = value.mode
        }
    }
}