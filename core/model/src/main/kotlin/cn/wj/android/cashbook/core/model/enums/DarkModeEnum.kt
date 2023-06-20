package cn.wj.android.cashbook.core.model.enums

/**
 * 黑夜模式
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/6/20
 */
enum class DarkModeEnum(val type: Int) {

    /** 跟随系统 */
    FOLLOW_SYSTEM(0),

    /** 白天模式 */
    LIGHT(1),

    /** 黑夜模式 */
    DARK(2);

    companion object {
        fun typeOf(type: Int): DarkModeEnum {
            return values().first { it.type == type }
        }
    }
}