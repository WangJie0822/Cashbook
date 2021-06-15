package cn.wj.android.cashbook.data.entity

import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.base.ext.base.formatToNumber
import cn.wj.android.cashbook.base.ext.base.orElse
import cn.wj.android.cashbook.base.ext.base.string
import cn.wj.android.cashbook.data.enums.RecordTypeEnum
import cn.wj.android.cashbook.data.live.CurrentBooksLiveData

/**
 * 首页数据实体类
 *
 * @param date 时间
 * @param list 记录列表
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/11
 */
data class HomepageEntity(
    val date: String,
    val list: List<RecordEntity>
) {

    val listInfo: String
        get() {
            var totalExpend = "0".toBigDecimal()
            var totalIncome = "0".toBigDecimal()
            list.forEach {
                when (it.type) {
                    RecordTypeEnum.EXPENDITURE -> {
                        // 支出
                        totalExpend += it.amount.toBigDecimal()
                    }
                    RecordTypeEnum.INCOME -> {
                        // 收入
                        totalIncome += it.amount.toBigDecimal()
                    }
                    RecordTypeEnum.TRANSFER -> {
                        // 转账
                        if (it.charge.toFloatOrNull().orElse(0f) > 0) {
                            totalExpend += it.amount.toBigDecimal()
                        }
                    }
                    else -> {
                    }
                }
            }
            val symbol = CurrentBooksLiveData.currency.symbol
            return with(StringBuilder()) {
                if (totalIncome.toFloat() > 0) {
                    append(R.string.income_with_colon.string + symbol + totalIncome.formatToNumber())
                }
                if (isNotBlank()) {
                    append(R.string.symbol_comma.string)
                }
                if (totalExpend.toFloat() > 0) {
                    append(R.string.expenditure_with_colon.string + symbol + totalExpend.formatToNumber())
                }
                toString()
            }
        }
}