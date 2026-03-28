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
import cn.wj.android.cashbook.core.common.ext.logger
import cn.wj.android.cashbook.core.data.repository.TypeRepository
import cn.wj.android.cashbook.core.model.entity.AnalyticsRecordPieEntity
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.model.enums.TypeLevelEnum
import cn.wj.android.cashbook.core.model.model.RecordTypeModel
import cn.wj.android.cashbook.core.model.model.RecordViewsModel
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class TransRecordViewsToAnalyticsPieUseCase @Inject constructor(
    private val typeRepository: TypeRepository,
    @Dispatcher(CashbookDispatchers.IO) private val coroutineContext: CoroutineContext,
) {

    suspend operator fun invoke(
        typeCategory: RecordTypeCategoryEnum,
        recordViewsList: List<RecordViewsModel>,
    ): List<AnalyticsRecordPieEntity> = withContext(coroutineContext) {
        val result = mutableListOf<AnalyticsRecordPieEntity>()
        val categoryList = recordViewsList.filter { !it.isBalanceRecord && it.type.typeCategory == typeCategory }
        var total = 0L
        val typeList = mutableListOf<RecordTypeModel>()
        val addedTypeIds = mutableSetOf<Long>()
        categoryList.forEach { record ->
            // 计算总金额
            total += if (typeCategory == RecordTypeCategoryEnum.EXPENDITURE) {
                record.amount + record.charges - record.concessions
            } else {
                record.amount - record.charges
            }
            // 统计一级分类
            val type = record.type
            if (type.typeLevel == TypeLevelEnum.FIRST) {
                if (addedTypeIds.add(type.id)) {
                    typeList.add(type)
                }
            } else {
                if (addedTypeIds.add(type.parentId)) {
                    typeRepository.getRecordTypeById(type.parentId)?.let { parentType ->
                        typeList.add(parentType)
                    }
                }
            }
        }

        val addedResultTypeIds = mutableSetOf<Long>()
        typeList.forEach { type ->
            if (addedResultTypeIds.add(type.id)) {
                var typeTotal = 0L
                categoryList.filter { it.type.id == type.id || it.type.parentId == type.id }
                    .forEach {
                        typeTotal += if (typeCategory == RecordTypeCategoryEnum.EXPENDITURE) {
                            it.amount + it.charges - it.concessions
                        } else {
                            it.amount - it.charges
                        }
                    }
                result.add(
                    AnalyticsRecordPieEntity(
                        typeId = type.id,
                        typeName = type.name,
                        typeIconResName = type.iconName,
                        typeCategory = type.typeCategory,
                        totalAmount = typeTotal,
                        percent = if (total != 0L) (typeTotal.toDouble() / total.toDouble()).toFloat() else 0f,
                    ),
                )
            }
        }
        result.sortBy { it.percent }
        result.reverse()
        this@TransRecordViewsToAnalyticsPieUseCase.logger().i("result = <$result>")
        result
    }
}
