package cn.wj.android.cashbook.domain.usecase

import cn.wj.android.cashbook.core.common.annotation.CashbookDispatchers
import cn.wj.android.cashbook.core.common.annotation.Dispatcher
import cn.wj.android.cashbook.core.common.ext.decimalFormat
import cn.wj.android.cashbook.core.common.ext.logger
import cn.wj.android.cashbook.core.common.ext.toBigDecimalOrZero
import cn.wj.android.cashbook.core.model.entity.AnalyticsRecordPieEntity
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.model.enums.TypeLevelEnum
import cn.wj.android.cashbook.core.model.model.RecordViewsModel
import java.math.BigDecimal
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.withContext

class TransRecordViewsToAnalyticsPieUseCase @Inject constructor(
    @Dispatcher(CashbookDispatchers.IO) private val coroutineContext: CoroutineContext,
) {

    suspend operator fun invoke(
        typeCategory: RecordTypeCategoryEnum,
        recordViewsList: List<RecordViewsModel>
    ): List<AnalyticsRecordPieEntity> = withContext(coroutineContext) {
        val result = mutableListOf<AnalyticsRecordPieEntity>()
        val categoryList = recordViewsList.filter { it.type.typeCategory == typeCategory }
        var total = BigDecimal.ZERO
        categoryList.forEach {
            total += if (typeCategory == RecordTypeCategoryEnum.EXPENDITURE) {
                (it.amount.toBigDecimalOrZero() + it.charges.toBigDecimalOrZero() - it.concessions.toBigDecimalOrZero())
            } else {
                (it.amount.toBigDecimalOrZero() - it.charges.toBigDecimalOrZero())
            }
        }

        categoryList.filter { it.type.typeLevel == TypeLevelEnum.FIRST }
            .forEach { firstTypeRecord ->
                if (result.count { it.typeId == firstTypeRecord.type.id } <= 0) {
                    var typeTotal = BigDecimal.ZERO
                    categoryList.filter { it.type.id == firstTypeRecord.type.id || it.type.parentId == firstTypeRecord.type.id }
                        .forEach {
                            typeTotal += if (typeCategory == RecordTypeCategoryEnum.EXPENDITURE) {
                                (it.amount.toBigDecimalOrZero() + it.charges.toBigDecimalOrZero() - it.concessions.toBigDecimalOrZero())
                            } else {
                                (it.amount.toBigDecimalOrZero() - it.charges.toBigDecimalOrZero())
                            }
                        }
                    result.add(
                        AnalyticsRecordPieEntity(
                            typeId = firstTypeRecord.type.id,
                            typeName = firstTypeRecord.type.name,
                            typeIconResName = firstTypeRecord.type.iconName,
                            typeCategory = firstTypeRecord.type.typeCategory,
                            totalAmount = typeTotal.decimalFormat(),
                            percent = (typeTotal / total).toFloat()
                        )
                    )
                }
            }
        result.sortBy { it.percent }
        result.reverse()
        this@TransRecordViewsToAnalyticsPieUseCase.logger().i("result = <$result>")
        result
    }
}