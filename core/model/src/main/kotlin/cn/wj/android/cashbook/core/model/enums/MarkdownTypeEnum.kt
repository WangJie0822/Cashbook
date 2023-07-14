package cn.wj.android.cashbook.core.model.enums

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