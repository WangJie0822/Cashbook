package cn.wj.android.cashbook.core.model.enums

/**
 * 认证模式
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/6/20
 */
enum class VerificationModeEnum(val type: Int) {

    /** 启动时 */
    WHEN_LAUNCH(0),

    /** 每次进入前台 */
    WHEN_FOREGROUND(1);

    companion object {
        fun typeOf(type: Int): VerificationModeEnum {
            return values().first { it.type == type }
        }
    }
}