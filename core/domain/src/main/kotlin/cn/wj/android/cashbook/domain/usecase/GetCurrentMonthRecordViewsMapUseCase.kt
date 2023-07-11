package cn.wj.android.cashbook.domain.usecase

import android.util.ArrayMap
import cn.wj.android.cashbook.core.common.ext.decimalFormat
import cn.wj.android.cashbook.core.common.ext.toBigDecimalOrZero
import cn.wj.android.cashbook.core.model.entity.RecordDayEntity
import cn.wj.android.cashbook.core.model.entity.RecordViewsEntity
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import java.math.BigDecimal
import javax.inject.Inject

/**
 * 获取当前月显示数据用例
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/2/22
 */
class GetCurrentMonthRecordViewsMapUseCase @Inject constructor(
) {

    operator fun invoke(list: List<RecordViewsEntity>): Map<RecordDayEntity, List<RecordViewsEntity>> {
        val map = ArrayMap<String, ArrayList<RecordViewsEntity>>()
        list.sortedBy { it.recordTime }
            .reversed()
            .forEach {
                val key = it.recordTime.split(" ").firstOrNull()?.split("-")?.lastOrNull()
                    ?.toIntOrNull()?.toString()
                if (map.containsKey(key)) {
                    map[key]?.add(it)
                } else {
                    map[key] = arrayListOf(it)
                }
            }
        return map.mapKeys {
            var totalExpenditure = BigDecimal.ZERO
            var totalIncome = BigDecimal.ZERO
            it.value.forEach { record ->
                when (record.typeCategory) {
                    RecordTypeCategoryEnum.EXPENDITURE -> {
                        // 支出
                        totalExpenditure += (record.amount.toBigDecimalOrZero() + record.charges.toBigDecimalOrZero() - record.concessions.toBigDecimalOrZero())
                    }

                    RecordTypeCategoryEnum.INCOME -> {
                        // 收入
                        totalIncome += (record.amount.toBigDecimalOrZero() - record.charges.toBigDecimalOrZero())
                    }

                    RecordTypeCategoryEnum.TRANSFER -> {
                        // 转账
                        totalExpenditure += record.charges.toBigDecimalOrZero()
                        totalIncome += record.concessions.toBigDecimalOrZero()
                    }
                }
            }
            RecordDayEntity(
                day = it.key,
                dayIncome = totalIncome.decimalFormat(),
                dayExpand = totalExpenditure.decimalFormat(),
            )
        }
    }

}