package cn.wj.android.cashbook.domain.usecase

import cn.wj.android.cashbook.core.common.annotation.CashbookDispatchers
import cn.wj.android.cashbook.core.common.annotation.Dispatcher
import cn.wj.android.cashbook.core.common.ext.decimalFormat
import cn.wj.android.cashbook.core.common.ext.logger
import cn.wj.android.cashbook.core.common.ext.toBigDecimalOrZero
import cn.wj.android.cashbook.core.data.repository.TypeRepository
import cn.wj.android.cashbook.core.model.entity.AnalyticsRecordPieEntity
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.model.enums.TypeLevelEnum
import cn.wj.android.cashbook.core.model.model.RecordTypeModel
import cn.wj.android.cashbook.core.model.model.RecordViewsModel
import java.math.BigDecimal
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.withContext

class TransRecordViewsToAnalyticsPieUseCase @Inject constructor(
    private val typeRepository: TypeRepository,
    @Dispatcher(CashbookDispatchers.IO) private val coroutineContext: CoroutineContext,
) {

    suspend operator fun invoke(
        typeCategory: RecordTypeCategoryEnum,
        recordViewsList: List<RecordViewsModel>
    ): List<AnalyticsRecordPieEntity> = withContext(coroutineContext) {
        val result = mutableListOf<AnalyticsRecordPieEntity>()
        val categoryList = recordViewsList.filter { it.type.typeCategory == typeCategory }
        var total = BigDecimal.ZERO
        val typeList = mutableListOf<RecordTypeModel>()
        categoryList.forEach { record ->
            // 计算总金额
            total += if (typeCategory == RecordTypeCategoryEnum.EXPENDITURE) {
                (record.amount.toBigDecimalOrZero() + record.charges.toBigDecimalOrZero() - record.concessions.toBigDecimalOrZero())
            } else {
                (record.amount.toBigDecimalOrZero() - record.charges.toBigDecimalOrZero())
            }
            // 统计一级分类
            val type = record.type
            if (type.typeLevel == TypeLevelEnum.FIRST) {
                if (typeList.count { it.id == type.id } <= 0) {
                    typeList.add(type)
                }
            } else {
                if (typeList.count { it.id == type.parentId } <= 0) {
                    typeRepository.getRecordTypeById(type.parentId)?.let { parentType ->
                        typeList.add(parentType)
                    }
                }
            }
        }

        typeList.forEach { type ->
            if (result.count { it.typeId == type.id } <= 0) {
                var typeTotal = BigDecimal.ZERO
                categoryList.filter { it.type.id == type.id || it.type.parentId == type.id }
                    .forEach {
                        typeTotal += if (typeCategory == RecordTypeCategoryEnum.EXPENDITURE) {
                            (it.amount.toBigDecimalOrZero() + it.charges.toBigDecimalOrZero() - it.concessions.toBigDecimalOrZero())
                        } else {
                            (it.amount.toBigDecimalOrZero() - it.charges.toBigDecimalOrZero())
                        }
                    }
                result.add(
                    AnalyticsRecordPieEntity(
                        typeId = type.id,
                        typeName = type.name,
                        typeIconResName = type.iconName,
                        typeCategory = type.typeCategory,
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