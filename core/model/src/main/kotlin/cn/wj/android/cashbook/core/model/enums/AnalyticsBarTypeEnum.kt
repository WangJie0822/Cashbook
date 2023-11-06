package cn.wj.android.cashbook.core.model.enums

enum class AnalyticsBarTypeEnum {
    EXPENDITURE,
    INCOME,
    BALANCE,
    ALL,
    ;

    companion object {
        val size: Int
            get() = entries.size

        fun ordinalOf(ordinal: Int): AnalyticsBarTypeEnum? {
            return entries.firstOrNull { it.ordinal == ordinal }
        }
    }
}