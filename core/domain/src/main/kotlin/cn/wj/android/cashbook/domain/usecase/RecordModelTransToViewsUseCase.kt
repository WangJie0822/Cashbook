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
import cn.wj.android.cashbook.core.common.ext.toBigDecimalOrZero
import cn.wj.android.cashbook.core.data.repository.AssetRepository
import cn.wj.android.cashbook.core.data.repository.RecordRepository
import cn.wj.android.cashbook.core.data.repository.TagRepository
import cn.wj.android.cashbook.core.data.repository.TypeRepository
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.model.model.RECORD_TYPE_BALANCE_EXPENDITURE
import cn.wj.android.cashbook.core.model.model.RECORD_TYPE_BALANCE_INCOME
import cn.wj.android.cashbook.core.model.model.RecordModel
import cn.wj.android.cashbook.core.model.model.RecordViewsModel
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

/**
 * 记录数据转换为记录显示数据用例
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/9/21
 */
class RecordModelTransToViewsUseCase @Inject constructor(
    private val recordRepository: RecordRepository,
    private val typeRepository: TypeRepository,
    private val assetRepository: AssetRepository,
    private val tagRepository: TagRepository,
    @Dispatcher(CashbookDispatchers.IO) private val coroutineContext: CoroutineContext,
) {
    suspend operator fun invoke(recordModel: RecordModel): RecordViewsModel =
        withContext(coroutineContext) {
            val type = when (recordModel.typeId) {
                RECORD_TYPE_BALANCE_INCOME.id -> RECORD_TYPE_BALANCE_INCOME
                RECORD_TYPE_BALANCE_EXPENDITURE.id -> RECORD_TYPE_BALANCE_EXPENDITURE
                else -> typeRepository.getNoNullRecordTypeById(recordModel.typeId)
            }
            val relatedRecord = if (type.typeCategory == RecordTypeCategoryEnum.INCOME) {
                recordRepository.getRelatedIdListById(recordModel.id).mapNotNull { id ->
                    recordRepository.queryById(id)
                }
            } else {
                recordRepository.getRecordIdListFromRelatedId(recordModel.id).mapNotNull { id ->
                    recordRepository.queryById(id)
                }
            }
            var totalRelated = BigDecimal.ZERO
            relatedRecord.forEach { record ->
                if (type.typeCategory == RecordTypeCategoryEnum.EXPENDITURE) {
                    // 支出类型，关联的是收入
                    totalRelated += record.amount.toBigDecimalOrZero()
                } else if (type.typeCategory == RecordTypeCategoryEnum.INCOME) {
                    // 收入类型，关联的是支出
                    totalRelated += (record.amount.toBigDecimalOrZero() + record.charges.toBigDecimalOrZero() - record.concessions.toBigDecimalOrZero())
                }
            }
            RecordViewsModel(
                id = recordModel.id,
                booksId = recordModel.booksId,
                type = type,
                asset = assetRepository.getAssetById(recordModel.assetId),
                relatedAsset = assetRepository.getAssetById(recordModel.relatedAssetId),
                amount = recordModel.amount,
                finalAmount = recordModel.finalAmount,
                charges = recordModel.charges,
                concessions = recordModel.concessions,
                remark = recordModel.remark,
                reimbursable = recordModel.reimbursable,
                relatedTags = tagRepository.getRelatedTag(recordModel.id),
                relatedImage = recordRepository.queryImagesByRecordId(recordModel.id),
                relatedRecord = relatedRecord,
                relatedAmount = totalRelated.decimalFormat(),
                recordTime = recordModel.recordTime,
            )
        }
}
