package cn.wj.android.cashbook.core.model.enums

/**
 * 自动备份
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/7/18
 */
enum class AutoBackupModeEnum {

    /** 关闭 */
    CLOSE,

    /** 每次启动 */
    WHEN_LAUNCH,

    /** 每天 */
    EACH_DAY,

    /** 每周 */
    EACH_WEEK,
    ;

    companion object {
        fun ordinalOf(ordinal: Int): AutoBackupModeEnum {
            return entries.first { it.ordinal == ordinal }
        }
    }
}