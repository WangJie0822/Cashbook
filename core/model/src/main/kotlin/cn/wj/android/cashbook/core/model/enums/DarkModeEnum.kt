package cn.wj.android.cashbook.core.model.enums

/**
 * 黑夜模式
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/6/20
 */
enum class DarkModeEnum {

    /** 跟随系统 */
    FOLLOW_SYSTEM,

    /** 白天模式 */
    LIGHT,

    /** 黑夜模式 */
    DARK,
    ;

    companion object {
        fun ordinalOf(ordinal: Int): DarkModeEnum {
            return entries.first { it.ordinal == ordinal }
        }
    }
}