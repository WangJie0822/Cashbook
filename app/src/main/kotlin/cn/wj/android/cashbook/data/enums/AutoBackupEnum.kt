package cn.wj.android.cashbook.data.enums

import androidx.annotation.StringRes
import cn.wj.android.cashbook.R

/**
 * 自动备份枚举
 *
 * @param value 值
 * @param textResId 对应文本资源 id
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/8/16
 */
enum class AutoBackupEnum(
    val value: Int,
    @StringRes val textResId: Int
) {
    // 关闭
    CLOSED(0, R.string.closed),

    // 每次开启
    WHEN_OPEN(1, R.string.when_open),

    // 每天
    EVERY_DAY(2, R.string.every_day),

    // 每周
    EVERY_WEEK(3, R.string.every_week);

    companion object {

        fun fromValue(value: Int): AutoBackupEnum {
            return values().firstOrNull { it.value == value } ?: CLOSED
        }
    }
}