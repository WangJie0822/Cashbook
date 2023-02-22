package cn.wj.android.cashbook.core.model.enums

/**
 * 收支类型
 *
 * @param position 类型对于下标
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/2/17
 */
enum class RecordTypeCategoryEnum(
    val position: Int
) {

    /** 支出 */
    EXPENDITURE(0),

    /** 收入 */
    INCOME(1),

    /** 转账 */
    TRANSFER(2),
}