package cn.wj.android.cashbook.data.enums

import androidx.appcompat.app.AppCompatDelegate
import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.base.ext.base.ifCondition
import cn.wj.android.cashbook.base.ext.base.orElse
import cn.wj.android.cashbook.base.ext.base.string
import cn.wj.android.cashbook.data.config.AppConfigs

/**
 * 白天黑夜模式枚举
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/22
 */
enum class DayNightEnum(val typeStrResId: Int, val mode: Int) {

    // 浅色主题
    LIGHT(R.string.day_night_light, AppCompatDelegate.MODE_NIGHT_NO),

    // 深色主题
    DARK(R.string.day_night_night, AppCompatDelegate.MODE_NIGHT_YES),

    // 跟随系统
    FOLLOW_SYSTEM(R.string.day_night_follow_system, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);

    companion object {
        fun currentDayNight(): DayNightEnum {
            val mode = AppConfigs.datNightMode
            return values().firstOrNull { it.mode == mode }.orElse(FOLLOW_SYSTEM)
        }

        fun getSelectItems(): Array<String> {
            return arrayOf(LIGHT.typeStrResId.string, DARK.typeStrResId.string, FOLLOW_SYSTEM.typeStrResId.string)
        }

        fun indexOf(theme: DayNightEnum): Int {
            val indexOf = values().indexOf(theme)
            return indexOf.ifCondition(indexOf < 0) { values().indexOf(FOLLOW_SYSTEM) }
        }

        fun fromIndex(index: Int): DayNightEnum {
            return if (index >= values().size) {
                FOLLOW_SYSTEM
            } else {
                values()[index]
            }
        }
    }
}