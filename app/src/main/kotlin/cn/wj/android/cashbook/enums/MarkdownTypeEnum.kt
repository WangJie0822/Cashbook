package cn.wj.android.cashbook.enums

enum class MarkdownTypeEnum {

    CHANGELOG,
    PRIVACY_POLICY,
    ;

    companion object {

        fun ordinalOf(ordinal: Int): MarkdownTypeEnum? {
            return MarkdownTypeEnum.values().firstOrNull { it.ordinal == ordinal }
        }
    }
}