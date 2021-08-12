package cn.wj.android.cashbook.data.entity

import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.base.ext.base.decimalFormat
import cn.wj.android.cashbook.base.ext.base.moneyFormat
import cn.wj.android.cashbook.base.ext.base.string
import cn.wj.android.cashbook.base.ext.base.toBigDecimalOrZero
import cn.wj.android.cashbook.base.ext.base.toFloatOrZero
import cn.wj.android.cashbook.data.enums.RecordTypeEnum

/**
 * 日期相关记录数据实体类
 *
 * @param date 时间
 * @param list 记录列表
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/11
 */
data class DateRecordEntity(
    val date: String,
    val list: List<RecordEntity>
) {

    val listInfo: String
        get() {
            var totalExpend = "0".toBigDecimal()
            var totalIncome = "0".toBigDecimal()
            list.forEach {
                when (it.typeEnum) {
                    RecordTypeEnum.EXPENDITURE -> {
                        // 支出
                        totalExpend += it.amount.toBigDecimalOrZero()
                    }
                    RecordTypeEnum.INCOME -> {
                        // 收入
                        totalIncome += it.amount.toBigDecimalOrZero()
                    }
                    RecordTypeEnum.TRANSFER -> {
                        // 转账
                        if (it.charge.toFloatOrZero() > 0f) {
                            totalExpend += it.charge.toBigDecimalOrZero()
                        } else if (it.charge.toFloatOrZero() < 0f) {
                            totalIncome -= it.charge.toBigDecimalOrZero()
                        }
                    }
                    else -> {
                    }
                }
            }
            return with(StringBuilder()) {
                if (totalIncome.toFloat() > 0) {
                    append(R.string.income_with_colon.string + totalIncome.decimalFormat().moneyFormat())
                }
                if (totalExpend.toFloat() > 0) {
                    if (isNotBlank()) {
                        append(R.string.symbol_comma.string)
                    }
                    append(R.string.expenditure_with_colon.string + totalExpend.decimalFormat().moneyFormat())
                }
                toString()
            }
        }
}