/*
 * Copyright 2021 The Cashbook Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.wj.android.cashbook.domain.usecase

import cn.wj.android.cashbook.core.common.annotation.CashbookDispatchers
import cn.wj.android.cashbook.core.common.annotation.Dispatcher
import cn.wj.android.cashbook.core.common.ext.decimalFormat
import cn.wj.android.cashbook.core.common.ext.logger
import cn.wj.android.cashbook.core.common.ext.toBigDecimalOrZero
import cn.wj.android.cashbook.core.model.entity.AnalyticsRecordPieEntity
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.model.model.RecordTypeModel
import cn.wj.android.cashbook.core.model.model.RecordViewsModel
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class TransRecordViewsToAnalyticsPieSecondUseCase @Inject constructor(
    @Dispatcher(CashbookDispatchers.IO) private val coroutineContext: CoroutineContext,
) {

    suspend operator fun invoke(
        typeId: Long,
        recordViewsList: List<RecordViewsModel>,
    ): List<AnalyticsRecordPieEntity> = withContext(coroutineContext) {
        val result = mutableListOf<AnalyticsRecordPieEntity>()
        val categoryList =
            recordViewsList.filter { it.type.id == typeId || it.type.parentId == typeId }
        var total = BigDecimal.ZERO
        val typeList = mutableListOf<RecordTypeModel>()
        val typeCategory =
            categoryList.firstOrNull()?.type?.typeCategory ?: return@withContext emptyList()
        categoryList.forEach { record ->
            // 计算总金额
            total += if (typeCategory == RecordTypeCategoryEnum.EXPENDITURE) {
                (record.amount.toBigDecimalOrZero() + record.charges.toBigDecimalOrZero() - record.concessions.toBigDecimalOrZero())
            } else {
                (record.amount.toBigDecimalOrZero() - record.charges.toBigDecimalOrZero())
            }
            // 统计所有分类
            val type = record.type
            if (typeList.count { it.id == type.id } <= 0) {
                typeList.add(type)
            }
        }

        typeList.forEach { type ->
            if (result.count { it.typeId == type.id } <= 0) {
                var typeTotal = BigDecimal.ZERO
                categoryList.filter { it.type.id == type.id }
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
                        percent = (typeTotal / total).toFloat(),
                    ),
                )
            }
        }
        result.sortBy { it.percent }
        result.reverse()
        this@TransRecordViewsToAnalyticsPieSecondUseCase.logger().i("result = <$result>")
        result
    }
}
