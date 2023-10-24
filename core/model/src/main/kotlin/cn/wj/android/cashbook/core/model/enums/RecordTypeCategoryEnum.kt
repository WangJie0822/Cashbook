package cn.wj.android.cashbook.core.model.enums

/**
 * 收支类型
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/2/17
 */
enum class RecordTypeCategoryEnum {

    /** 支出 */
    EXPENDITURE,

    /** 收入 */
    INCOME,

    /** 转账 */
    TRANSFER,
    ;

    companion object {

        val size: Int
            get() = entries.size

        fun ordinalOf(ordinal: Int): RecordTypeCategoryEnum {
            return entries.first { it.ordinal == ordinal }
        }
    }
}