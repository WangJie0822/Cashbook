package cn.wj.android.cashbook.core.model.enums

enum class MarkdownTypeEnum {

    /** 更新日志 */
    CHANGELOG,

    /** 隐私协议 */
    PRIVACY_POLICY,
    ;

    companion object {

        fun ordinalOf(ordinal: Int): MarkdownTypeEnum? {
            return MarkdownTypeEnum.values().firstOrNull { it.ordinal == ordinal }
        }
    }
}