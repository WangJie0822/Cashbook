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

import cn.wj.android.cashbook.core.common.FIXED_TYPE_ID_REFUND
import cn.wj.android.cashbook.core.common.FIXED_TYPE_ID_REIMBURSE
import cn.wj.android.cashbook.core.common.annotation.CashbookDispatchers
import cn.wj.android.cashbook.core.common.annotation.Dispatcher
import cn.wj.android.cashbook.core.data.repository.AssetRepository
import cn.wj.android.cashbook.core.data.repository.RecordRepository
import cn.wj.android.cashbook.core.data.repository.TagRepository
import cn.wj.android.cashbook.core.data.repository.TypeRepository
import cn.wj.android.cashbook.core.model.enums.RecordRelatedNatureEnum
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.model.model.AssetModel
import cn.wj.android.cashbook.core.model.model.RECORD_TYPE_BALANCE_EXPENDITURE
import cn.wj.android.cashbook.core.model.model.RECORD_TYPE_BALANCE_INCOME
import cn.wj.android.cashbook.core.model.model.RecordModel
import cn.wj.android.cashbook.core.model.model.RecordTypeModel
import cn.wj.android.cashbook.core.model.model.RecordViewsModel
import cn.wj.android.cashbook.core.model.model.recordAmount
import kotlinx.coroutines.withContext
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
            val totalRelated = sumRelatedAmount(type.typeCategory, relatedRecord)
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
                relatedAmount = totalRelated,
                relatedNature = computeRelatedNature(type.typeCategory, relatedRecord),
                recordTime = recordModel.recordTime,
            )
        }

    /**
     * 批量转换：与单条 [invoke] 产出逐字段等价，但通过 IN 批量查询 + 去重 + 内存 Map 组装，
     * 避免对每条记录单独发起 type / asset / 关联记录 / 图片 / 标签 查询，消除 N+1。
     *
     * 标签经 [TagRepository.getRelatedTags] 批量 JOIN 查询（F-4），按 recordId 建 Map 后内存 lookup，
     * 已消除原逐条 [TagRepository.getRelatedTag] 的 1-per-record 调用。
     */
    suspend operator fun invoke(records: List<RecordModel>): List<RecordViewsModel> =
        transBatch(records)

    suspend fun transBatch(records: List<RecordModel>): List<RecordViewsModel> =
        withContext(coroutineContext) {
            if (records.isEmpty()) {
                return@withContext emptyList()
            }

            // 1. 批量解析类型：去重 typeId，逐个解析后建 Map（平账合成类型走特判，不查库）
            val typeMap: Map<Long, RecordTypeModel> = records
                .map { it.typeId }
                .distinct()
                .associateWith { typeId ->
                    when (typeId) {
                        RECORD_TYPE_BALANCE_INCOME.id -> RECORD_TYPE_BALANCE_INCOME
                        RECORD_TYPE_BALANCE_EXPENDITURE.id -> RECORD_TYPE_BALANCE_EXPENDITURE
                        else -> typeRepository.getNoNullRecordTypeById(typeId)
                    }
                }

            // 2. 批量解析资产：去重 assetId / relatedAssetId，逐个解析后建 Map（与单条版 getAssetById 一致，-1 返回 null）
            val assetIds = records
                .flatMap { listOf(it.assetId, it.relatedAssetId) }
                .distinct()
            val assetMap: Map<Long, AssetModel?> = assetIds.associateWith { id ->
                assetRepository.getAssetById(id)
            }

            // 3. 按类型分组解析关联关系：收入侧用 getRelatedIdMapByIds，其余用 getRecordIdFromRelatedMapByIds
            val incomeRecordIds = records
                .filter { typeMap.getValue(it.typeId).typeCategory == RecordTypeCategoryEnum.INCOME }
                .map { it.id }
            val nonIncomeRecordIds = records
                .filter { typeMap.getValue(it.typeId).typeCategory != RecordTypeCategoryEnum.INCOME }
                .map { it.id }
            val incomeRelatedIdMap = recordRepository.getRelatedIdMapByIds(incomeRecordIds)
            val nonIncomeRelatedIdMap = recordRepository.getRecordIdFromRelatedMapByIds(nonIncomeRecordIds)

            // 4. 批量取回所有被关联的记录，建 id -> RecordModel Map
            val relatedRecordIds = (
                incomeRelatedIdMap.values.flatten() + nonIncomeRelatedIdMap.values.flatten()
                ).distinct()
            val relatedRecordMap: Map<Long, RecordModel> = recordRepository
                .queryByIds(relatedRecordIds)
                .associateBy { it.id }

            // 5. 批量取回图片
            val allRecordIds = records.map { it.id }
            val imageMap = recordRepository.queryImagesByRecordIds(allRecordIds)

            // 5b. 批量取回标签（F-4：消除逐条 getRelatedTag 的 1-per-record 调用）
            val tagMap = tagRepository.getRelatedTags(allRecordIds)

            // 6. 逐条组装（所有关联数据已批量预取，此处仅内存 Map lookup）
            records.map { recordModel ->
                val type = typeMap.getValue(recordModel.typeId)
                val relatedIdList = if (type.typeCategory == RecordTypeCategoryEnum.INCOME) {
                    incomeRelatedIdMap[recordModel.id].orEmpty()
                } else {
                    nonIncomeRelatedIdMap[recordModel.id].orEmpty()
                }
                // mapNotNull 复刻单条版 queryById 返回 null 时丢弃的语义
                val relatedRecord = relatedIdList.mapNotNull { id -> relatedRecordMap[id] }
                val totalRelated = sumRelatedAmount(type.typeCategory, relatedRecord)
                RecordViewsModel(
                    id = recordModel.id,
                    booksId = recordModel.booksId,
                    type = type,
                    asset = assetMap[recordModel.assetId],
                    relatedAsset = assetMap[recordModel.relatedAssetId],
                    amount = recordModel.amount,
                    finalAmount = recordModel.finalAmount,
                    charges = recordModel.charges,
                    concessions = recordModel.concessions,
                    remark = recordModel.remark,
                    reimbursable = recordModel.reimbursable,
                    relatedTags = tagMap[recordModel.id].orEmpty(),
                    relatedImage = imageMap[recordModel.id].orEmpty(),
                    relatedRecord = relatedRecord,
                    relatedAmount = totalRelated,
                    relatedNature = computeRelatedNature(type.typeCategory, relatedRecord),
                    recordTime = recordModel.recordTime,
                )
            }
        }

    /**
     * 计算关联金额，单条与批量共用同一口径，保证两者逐字段等价。
     * 关联 category 由主 category 取反推断（零额外查询）：
     * - 主支出：关联收入，recordAmount(INCOME)=amount−charges
     * - 主收入：关联支出，recordAmount(EXPENDITURE)=amount+charges−concessions
     * - 其它（TRANSFER 等）：不累加
     */
    private fun sumRelatedAmount(
        typeCategory: RecordTypeCategoryEnum,
        relatedRecord: List<RecordModel>,
    ): Long {
        val relatedCategory = when (typeCategory) {
            RecordTypeCategoryEnum.EXPENDITURE -> RecordTypeCategoryEnum.INCOME
            RecordTypeCategoryEnum.INCOME -> RecordTypeCategoryEnum.EXPENDITURE
            else -> return 0L
        }
        return relatedRecord.sumOf { record ->
            recordAmount(relatedCategory, record.amount, record.charges, record.concessions)
        }
    }

    /**
     * 计算被吸收支出的关联性质（在已物化 relatedRecord 上判定，零额外查询）。
     * 仅 EXPENDITURE 主记录有性质；relatedRecord 为吸收它的收入（报销/退款款）。
     * 标准链路下关联收入 typeId 经 migrateSpecialTypes 为固定负 ID（REIMBURSE/REFUND）；
     * 若出现其它 typeId（未迁移/历史导入等），归 MIXED 兜底。
     */
    private fun computeRelatedNature(
        typeCategory: RecordTypeCategoryEnum,
        relatedRecord: List<RecordModel>,
    ): RecordRelatedNatureEnum {
        if (typeCategory != RecordTypeCategoryEnum.EXPENDITURE || relatedRecord.isEmpty()) {
            return RecordRelatedNatureEnum.NONE
        }
        val allReimburse = relatedRecord.all { it.typeId == FIXED_TYPE_ID_REIMBURSE }
        val allRefund = relatedRecord.all { it.typeId == FIXED_TYPE_ID_REFUND }
        return when {
            allReimburse -> RecordRelatedNatureEnum.REIMBURSED
            allRefund -> RecordRelatedNatureEnum.REFUNDED
            else -> RecordRelatedNatureEnum.MIXED
        }
    }
}
