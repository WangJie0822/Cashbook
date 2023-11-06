package cn.wj.android.cashbook.core.model.enums

/**
 * 认证模式
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/6/20
 */
enum class VerificationModeEnum {

    /** 启动时 */
    WHEN_LAUNCH,

    /** 每次进入前台 */
    WHEN_FOREGROUND,
    ;

    companion object {
        fun ordinalOf(ordinal: Int): VerificationModeEnum {
            return values().first { it.ordinal == ordinal }
        }
    }
}