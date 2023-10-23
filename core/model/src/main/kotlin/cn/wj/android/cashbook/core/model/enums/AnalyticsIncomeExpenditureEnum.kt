package cn.wj.android.cashbook.core.model.enums

enum class AnalyticsIncomeExpenditureEnum {
    EXPENDITURE,
    INCOME,
    ALL,
    ;

    companion object {
        fun ordinalOf(ordinal: Int): AnalyticsIncomeExpenditureEnum? {
            return entries.firstOrNull { it.ordinal == ordinal }
        }
    }
}