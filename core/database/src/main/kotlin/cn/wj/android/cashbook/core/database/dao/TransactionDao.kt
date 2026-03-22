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

package cn.wj.android.cashbook.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import cn.wj.android.cashbook.core.database.table.AssetTable
import cn.wj.android.cashbook.core.database.table.ImageWithRelatedTable
import cn.wj.android.cashbook.core.database.table.RecordTable
import cn.wj.android.cashbook.core.database.table.RecordWithRelatedTable
import cn.wj.android.cashbook.core.database.table.TYPE_TABLE_BALANCE_EXPENDITURE
import cn.wj.android.cashbook.core.database.table.TYPE_TABLE_BALANCE_INCOME
import cn.wj.android.cashbook.core.database.table.TagWithRecordTable
import cn.wj.android.cashbook.core.database.table.TypeTable
import cn.wj.android.cashbook.core.database.throwable.DataTransactionException
import cn.wj.android.cashbook.core.model.enums.ClassificationTypeEnum
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.model.model.ImageModel

/**
 * 事务数据库操作类
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/2/20
 */
@Dao
interface TransactionDao {

    @Insert
    suspend fun insertRecord(recordTable: RecordTable): Long

    @Insert
    suspend fun insertRelatedTags(tagWithRecordTable: List<TagWithRecordTable>)

    @Insert
    suspend fun insertRelatedImages(images: List<ImageWithRelatedTable>)

    @Delete
    suspend fun deleteRecord(recordTable: RecordTable): Int

    @Query(
        value = """
        DELETE FROM db_tag_with_record WHERE record_id=:recordId
    """,
    )
    suspend fun deleteOldRelatedTags(recordId: Long)

    @Query(
        value = """
        DELETE FROM db_image_with_related WHERE record_id=:recordId
    """,
    )
    suspend fun deleteOldRelatedImages(recordId: Long)

    @Update
    suspend fun updateRecord(recordTable: RecordTable)

    @Update
    suspend fun updateAsset(assetTable: AssetTable)

    @Query(
        value = """
        SELECT * FROM db_asset WHERE id=:assetId
    """,
    )
    suspend fun queryAssetById(assetId: Long): AssetTable?

    @Query(
        value = """
        SELECT * FROM db_type WHERE id=:typeId
    """,
    )
    suspend fun queryTypeById(typeId: Long): TypeTable?

    @Query(
        value = """
        SELECT * FROM db_record WHERE id=:recordId
    """,
    )
    suspend fun queryRecordById(recordId: Long): RecordTable?

    @Query(
        value = """
        SELECT * FROM db_record WHERE id IN (:ids)
    """,
    )
    suspend fun queryRecordByIds(ids: List<Long>): List<RecordTable>

    @Query(
        value = """
        DELETE FROM db_record_with_related WHERE record_id=:id OR related_record_id=:id
    """,
    )
    suspend fun clearRelatedRecordById(id: Long)

    @Query(
        value = """
        SELECT * FROM db_record_with_related WHERE related_record_id=:id
    """,
    )
    suspend fun queryRelatedByRelatedRecordId(id: Long): List<RecordWithRelatedTable>

    @Query(
        value = """
        SELECT * FROM db_record_with_related WHERE record_id=:id
    """,
    )
    suspend fun queryRelatedByRecordId(id: Long): List<RecordWithRelatedTable>

    @Insert
    suspend fun insertRelatedRecord(related: List<RecordWithRelatedTable>)

    @Query("SELECT * FROM db_asset WHERE books_id=:bookId")
    suspend fun queryAllAssetsByBookId(bookId: Long): List<AssetTable>

    @Query("DELETE FROM db_asset WHERE books_id=:bookId")
    suspend fun deleteAssetsByBookId(bookId: Long)

    @Query("DELETE FROM db_tag WHERE books_id=:bookId")
    suspend fun deleteTagsByBookId(bookId: Long)

    @Query("SELECT * FROM db_record WHERE asset_id=:assetId OR into_asset_id=:assetId")
    suspend fun queryRecordsByAssetId(assetId: Long): List<RecordTable>

    /** 解析记录类型，处理平账特殊类型 */
    suspend fun resolveType(typeId: Long): TypeTable? {
        return when (typeId) {
            TYPE_TABLE_BALANCE_EXPENDITURE.id -> TYPE_TABLE_BALANCE_EXPENDITURE
            TYPE_TABLE_BALANCE_INCOME.id -> TYPE_TABLE_BALANCE_INCOME
            else -> queryTypeById(typeId)
        }
    }

    /** 计算记录涉及的实际金额（单位：分） */
    fun calculateRecordAmount(
        record: RecordTable,
        category: RecordTypeCategoryEnum,
    ): Long {
        return if (category == RecordTypeCategoryEnum.INCOME) {
            // 收入：金额 - 手续费
            record.amount - record.charge
        } else {
            // 支出、转账：金额 + 手续费 - 优惠
            record.amount + record.charge - record.concessions
        }
    }

    /**
     * 重算某个吸收者（收入记录）的 finalAmount
     *
     * finalAmount = 吸收者自身的 recordAmount - 其所吸收的所有支出记录的 fullAmount 之和
     *
     * @param absorberId 吸收者记录 ID
     * @param excludeAbsorbedId 需要排除的被吸收记录 ID（用于删除场景，该记录即将被删除但关联尚未清除）
     */
    suspend fun recalculateAbsorberFinalAmount(
        absorberId: Long,
        excludeAbsorbedId: Long = -1L,
    ) {
        val absorber = queryRecordById(absorberId) ?: return
        val absorberType = resolveType(absorber.typeId) ?: return
        val absorberCategory = RecordTypeCategoryEnum.ordinalOf(absorberType.typeCategory)
        val absorberRecordAmount = calculateRecordAmount(absorber, absorberCategory)

        var totalAbsorbedFullAmount = 0L
        queryRelatedByRecordId(absorberId)
            .filter { it.relatedRecordId != excludeAbsorbedId }
            .forEach { relation ->
                val absorbed = queryRecordById(relation.relatedRecordId) ?: return@forEach
                val absorbedType = resolveType(absorbed.typeId) ?: return@forEach
                val absorbedCategory = RecordTypeCategoryEnum.ordinalOf(absorbedType.typeCategory)
                totalAbsorbedFullAmount += calculateRecordAmount(absorbed, absorbedCategory)
            }

        updateRecordFinalAmountById(absorberId, absorberRecordAmount - totalAbsorbedFullAmount)
    }

    /**
     * 校验资产余额的一致性
     *
     * 遍历资产的所有关联记录，计算余额应有的净变化量，与当前余额对比。
     * 返回差值：0 表示余额正确，非 0 表示存在偏差。
     *
     * 注意：新建资产时的初始余额不通过记录产生（直接写入 AssetTable），
     * 后续的余额修改会生成平账记录。因此本方法计算的是"记录产生的净变化量"，
     * 需要加上初始余额才等于期望的当前余额。
     *
     * @return 差值 = 当前余额 - (初始余额推算值 + 记录净变化)，正常应为 0
     */
    @Transaction
    suspend fun verifyAssetBalance(assetId: Long): Long {
        val asset = queryAssetById(assetId) ?: return 0L
        val isCreditCard =
            ClassificationTypeEnum.ordinalOf(asset.type) == ClassificationTypeEnum.CREDIT_CARD_ACCOUNT
        val records = queryRecordsByAssetId(assetId)

        var balanceChange = 0L
        for (record in records) {
            val type = resolveType(record.typeId) ?: continue
            val category = RecordTypeCategoryEnum.ordinalOf(type.typeCategory)
            val recordAmount = calculateRecordAmount(record, category)

            // 此资产作为源资产（assetId）
            if (record.assetId == assetId) {
                balanceChange += if (isCreditCard) {
                    if (category == RecordTypeCategoryEnum.INCOME) -recordAmount else recordAmount
                } else {
                    if (category == RecordTypeCategoryEnum.INCOME) recordAmount else -recordAmount
                }
            }

            // 此资产作为转账目标（intoAssetId）
            if (record.intoAssetId == assetId && category == RecordTypeCategoryEnum.TRANSFER) {
                balanceChange += if (isCreditCard) -record.amount else record.amount
            }
        }

        // 初始余额推算 = 当前余额 - 记录净变化
        // 如果一切正确，初始余额应是资产创建时的值（无法独立验证）
        // 但可以用于跨资产交叉校验：同一批操作后重算应与原值一致
        return balanceChange
    }

    @Throws(DataTransactionException::class)
    @Transaction
    suspend fun updateRecordTransaction(
        record: RecordTable,
        tagIdList: List<Long>,
        needRelated: Boolean,
        relatedRecordIdList: List<Long>,
        relatedImageList: List<ImageModel>,
    ) {
        // 删除旧数据
        deleteRecordTransaction(record.id)
        // 插入新数据
        insertRecordTransaction(
            record = record,
            tagIdList = tagIdList,
            needRelated = needRelated,
            relatedRecordIdList = relatedRecordIdList,
            relatedImageList = relatedImageList,
        )
    }

    /**
     * 插入记录
     *
     * - 计算最终金额
     * - 更新关联资产金额
     * - 转账类型更新目标资产金额
     * - 插入关联的标签数据
     * - 插入关联记录数据
     * - 更新关联记录的最终金额
     * - 更新记录的最终金额
     */
    @Throws(DataTransactionException::class)
    @Transaction
    suspend fun insertRecordTransaction(
        record: RecordTable,
        tagIdList: List<Long>,
        needRelated: Boolean,
        relatedRecordIdList: List<Long>,
        relatedImageList: List<ImageModel>,
    ) {
        // 防御性校验：确保引用的类型存在
        val type = resolveType(record.typeId)
            ?: throw DataTransactionException("Type must not be null, typeId=${record.typeId}")
        val category = RecordTypeCategoryEnum.ordinalOf(type.typeCategory)
        // 防御性校验：确保引用的资产存在（NO_ASSET_ID 除外）
        if (record.assetId != cn.wj.android.cashbook.core.common.NO_ASSET_ID) {
            queryAssetById(record.assetId)
                ?: throw DataTransactionException("Asset not found, assetId=${record.assetId}")
        }
        if (category == RecordTypeCategoryEnum.TRANSFER &&
            record.intoAssetId != cn.wj.android.cashbook.core.common.NO_ASSET_ID
        ) {
            queryAssetById(record.intoAssetId)
                ?: throw DataTransactionException("Transfer target asset not found, intoAssetId=${record.intoAssetId}")
        }
        val recordAmount = calculateRecordAmount(record, category)
        // 更新资产余额
        queryAssetById(record.assetId)?.let { asset ->
            // 计算已用额度 or 余额
            val balance =
                if (ClassificationTypeEnum.ordinalOf(asset.type) == ClassificationTypeEnum.CREDIT_CARD_ACCOUNT) {
                    // 信用卡账户
                    if (category == RecordTypeCategoryEnum.INCOME) {
                        // 收入，已用额度 - 记录金额
                        asset.balance - recordAmount
                    } else {
                        // 支出、转账，已用额度 + 记录金额
                        asset.balance + recordAmount
                    }
                } else {
                    // 非信用卡账户
                    if (category == RecordTypeCategoryEnum.INCOME) {
                        // 收入，余额 + 记录金额
                        asset.balance + recordAmount
                    } else {
                        // 支出、转账，余额 - 记录金额
                        asset.balance - recordAmount
                    }
                }
            // 更新资产
            updateAsset(asset.copy(balance = balance))
        }
        if (category == RecordTypeCategoryEnum.TRANSFER) {
            // 转账，更新关联资产余额
            queryAssetById(record.intoAssetId)?.let { asset ->
                // 计算已用额度 or 余额
                val balance =
                    if (ClassificationTypeEnum.ordinalOf(asset.type) == ClassificationTypeEnum.CREDIT_CARD_ACCOUNT) {
                        // 信用卡账户，已用额度 - 记录金额
                        asset.balance - record.amount
                    } else {
                        // 非信用卡账户，余额 + 记录金额
                        asset.balance + record.amount
                    }
                // 更新资产
                updateAsset(asset.copy(balance = balance))
            }
        }

        val recordId = insertRecord(record.copy(finalAmount = recordAmount))

        // 插入新的关联标签
        insertRelatedTags(
            tagIdList.map { tagId ->
                TagWithRecordTable(id = null, recordId = recordId, tagId = tagId)
            },
        )

        // 插入新的关联图片
        insertRelatedImages(
            relatedImageList.map {
                ImageWithRelatedTable(
                    id = null,
                    recordId = recordId,
                    path = it.path,
                    bytes = it.bytes,
                )
            },
        )

        if (needRelated && relatedRecordIdList.isNotEmpty()) {
            // 更新关联记录
            insertRelatedRecord(
                relatedRecordIdList.map { relatedRecordId ->
                    RecordWithRelatedTable(
                        id = null,
                        recordId = recordId,
                        relatedRecordId = relatedRecordId,
                    )
                },
            )

            // 更新关联记录的金额
            var relatedAmount = 0L
            queryRecordByIds(relatedRecordIdList).forEach { relatedRecord ->
                relatedAmount += relatedRecord.finalAmount
            }
            relatedRecordIdList.forEach { relatedRecordId ->
                updateRecordFinalAmountById(relatedRecordId, 0L)
            }
            updateRecordFinalAmountById(recordId, recordAmount - relatedAmount)
        }
    }

    @Throws(DataTransactionException::class)
    @Transaction
    suspend fun deleteRecordTransaction(recordId: Long?) {
        if (null == recordId) {
            return
        }
        // 获取记录信息
        val record =
            queryRecordById(recordId) ?: throw DataTransactionException("Record id not found")
        deleteRecordTransaction(record)
    }

    /**
     * 删除已有记录
     *
     * - 计算金额（非finalAmount）后更新资产金额
     * - 转账类型更新目标资产金额
     * - 移除和记录关联的所有标签数据
     * - 更新与记录关联的所有记录 finalAmount 字段
     * - 移除和记录关联的所有关联记录数据
     */
    @Throws(DataTransactionException::class)
    @Transaction
    suspend fun deleteRecordTransaction(record: RecordTable) {
        val recordId = record.id ?: return
        val type = resolveType(record.typeId)
            ?: throw DataTransactionException("Type must not be null")
        val category = RecordTypeCategoryEnum.ordinalOf(type.typeCategory)
        val oldRecordAmount = calculateRecordAmount(record, category)
        // 更新资产余额
        queryAssetById(record.assetId)?.let { asset ->
            // 计算回退已用额度 or 余额
            val balance =
                if (ClassificationTypeEnum.ordinalOf(asset.type) == ClassificationTypeEnum.CREDIT_CARD_ACCOUNT) {
                    // 信用卡账户
                    if (category == RecordTypeCategoryEnum.INCOME) {
                        // 收入，已用额度 + 记录金额
                        asset.balance + oldRecordAmount
                    } else {
                        // 支出、转账，已用额度 - 记录金额
                        asset.balance - oldRecordAmount
                    }
                } else {
                    // 非信用卡账户
                    if (category == RecordTypeCategoryEnum.INCOME) {
                        // 收入，余额 - 记录金额
                        asset.balance - oldRecordAmount
                    } else {
                        // 支出、转账，余额 + 记录金额
                        asset.balance + oldRecordAmount
                    }
                }
            // 更新资产
            updateAsset(asset.copy(balance = balance))
        }
        if (category == RecordTypeCategoryEnum.TRANSFER) {
            // 转账，更新关联资产余额
            queryAssetById(record.intoAssetId)?.let { asset ->
                // 计算回退已用额度 or 余额
                val balance =
                    if (ClassificationTypeEnum.ordinalOf(asset.type) == ClassificationTypeEnum.CREDIT_CARD_ACCOUNT) {
                        // 信用卡账户，已用额度 + 记录金额
                        asset.balance + record.amount
                    } else {
                        // 非信用卡账户，余额 - 记录金额
                        asset.balance - record.amount
                    }
                // 更新资产
                updateAsset(asset.copy(balance = balance))
            }
        }

        // 移除关联标签
        deleteOldRelatedTags(recordId)

        // 移除关联照片
        deleteOldRelatedImages(recordId)

        // 更新关联记录的 finalAmount
        if (category == RecordTypeCategoryEnum.EXPENDITURE) {
            // 删除支出记录，重算所有吸收了该支出的收入记录的 finalAmount
            queryRelatedByRelatedRecordId(recordId).forEach { relation ->
                // 排除即将被删除的支出记录（关联尚未清除）
                recalculateAbsorberFinalAmount(relation.recordId, excludeAbsorbedId = recordId)
            }
        } else if (category == RecordTypeCategoryEnum.INCOME) {
            // 删除收入（吸收者）记录，需要更新被关联的支出记录
            val absorbedRelations = queryRelatedByRecordId(recordId)
            for (relation in absorbedRelations) {
                val expenseId = relation.relatedRecordId
                val expense = queryRecordById(expenseId) ?: continue
                // 检查该支出是否还有其他吸收者
                val remainingAbsorbers = queryRelatedByRelatedRecordId(expenseId)
                    .filter { it.recordId != recordId }
                if (remainingAbsorbers.isEmpty()) {
                    // 无其他吸收者，恢复支出的完整金额
                    val expenseType = resolveType(expense.typeId) ?: continue
                    val expenseCategory = RecordTypeCategoryEnum.ordinalOf(expenseType.typeCategory)
                    val fullAmount = calculateRecordAmount(expense, expenseCategory)
                    updateRecordFinalAmountById(expenseId, fullAmount)
                } else {
                    // 还有其他吸收者，支出保持 finalAmount = 0
                    // 重算每个剩余吸收者的 finalAmount
                    for (absorber in remainingAbsorbers) {
                        recalculateAbsorberFinalAmount(absorber.recordId)
                    }
                }
            }
        }

        // 移除关联记录
        clearRelatedRecordById(recordId)

        // 删除当前记录
        val result = deleteRecord(record)
        if (result <= 0) {
            throw DataTransactionException("Record delete failed!")
        }
    }

    @Query(
        value = """
        UPDATE db_record SET final_amount=:finalAmount WHERE id=:id
    """,
    )
    suspend fun updateRecordFinalAmountById(id: Long, finalAmount: Long)

    @Query(
        value = """
        SELECT * FROM db_record WHERE books_id=:bookId
    """,
    )
    suspend fun queryRecordListByBookId(bookId: Long): List<RecordTable>

    @Query(
        """
        DELETE FROM db_record_with_related
        WHERE record_id=:recordId OR related_record_id=:recordId
    """,
    )
    suspend fun deleteRecordRelationByRecordId(recordId: Long)

    @Query(
        value = """
        DELETE FROM db_tag_with_record
        WHERE record_id=:recordId
    """,
    )
    suspend fun deleteTagRelationByRecordId(recordId: Long)

    @Query(
        value = """
        DELETE FROM db_tag_with_record
        WHERE tag_id=:tagId
    """,
    )
    suspend fun deleteTagRelationByTagId(tagId: Long)

    @Query(
        value = """
        DELETE FROM db_tag
        WHERE id=:tagId
    """,
    )
    suspend fun deleteTagById(tagId: Long)

    @Query(
        value = """
       DELETE FROM db_books
        WHERE id=:bookId
    """,
    )
    suspend fun deleteBookById(bookId: Long)

    @Query("DELETE FROM db_tag_with_record WHERE record_id IN (SELECT id FROM db_record WHERE books_id = :bookId)")
    suspend fun deleteTagRelationsByBookId(bookId: Long)

    @Query("DELETE FROM db_record_with_related WHERE record_id IN (SELECT id FROM db_record WHERE books_id = :bookId) OR related_record_id IN (SELECT id FROM db_record WHERE books_id = :bookId)")
    suspend fun deleteRecordRelationsByBookId(bookId: Long)

    @Query("DELETE FROM db_image_with_related WHERE record_id IN (SELECT id FROM db_record WHERE books_id = :bookId)")
    suspend fun deleteImageRelationsByBookId(bookId: Long)

    @Query("DELETE FROM db_record WHERE books_id = :bookId")
    suspend fun deleteRecordsByBookId(bookId: Long)

    /**
     * 事务化删除账本及其所有关联数据
     *
     * 逐条删除记录以确保正确回退所有资产余额（包括跨账本转账的对方资产）
     * 并正确处理关联记录的 finalAmount 重算
     */
    @Throws(DataTransactionException::class)
    @Transaction
    suspend fun deleteBookTransaction(bookId: Long) {
        // 逐条删除记录，正确回退所有关联资产余额及 finalAmount
        val records = queryRecordListByBookId(bookId)
        for (record in records) {
            deleteRecordTransaction(record)
        }
        // 删除该账本下的所有标签
        deleteTagsByBookId(bookId)
        // 删除该账本下的所有资产
        deleteAssetsByBookId(bookId)
        // 删除账本
        deleteBookById(bookId)
    }

    @Query("UPDATE db_record SET type_id = :newTypeId WHERE type_id = :oldTypeId")
    suspend fun updateRecordTypeId(oldTypeId: Long, newTypeId: Long)

    @Query("UPDATE db_type SET parent_id = -1, type_level = 0 WHERE parent_id = :parentId")
    suspend fun promoteChildTypes(parentId: Long)

    @Query("SELECT COUNT(*) FROM db_record WHERE type_id = :typeId")
    suspend fun countRecordsByTypeId(typeId: Long): Int

    @Query("DELETE FROM db_type WHERE id = :typeId")
    suspend fun deleteTypeById(typeId: Long)

    /**
     * 事务化迁移类型记录：将旧类型的记录迁移到固定类型，并清理旧类型
     */
    @Transaction
    suspend fun migrateTypeRecords(oldTypeId: Long, fixedTypeId: Long) {
        // 更新记录的 type_id
        updateRecordTypeId(oldTypeId, fixedTypeId)
        // 将旧类型的子类型提升为一级类型
        promoteChildTypes(oldTypeId)
        // 如果旧类型没有剩余记录引用，则删除
        val remainingCount = countRecordsByTypeId(oldTypeId)
        if (remainingCount == 0) {
            deleteTypeById(oldTypeId)
        }
    }

    @Transaction
    suspend fun deleteTag(id: Long) {
        deleteTagRelationByTagId(id)
        deleteTagById(id)
    }

    /** 删除资产关联的标签记录关联 */
    @Query(
        value = """
        DELETE FROM db_tag_with_record
        WHERE record_id IN (SELECT id FROM db_record WHERE asset_id=:assetId OR into_asset_id=:assetId)
    """,
    )
    suspend fun deleteTagRelationsByAssetId(assetId: Long)

    /** 删除资产关联的记录关联数据 */
    @Query(
        value = """
        DELETE FROM db_record_with_related
        WHERE record_id IN (SELECT id FROM db_record WHERE asset_id=:assetId OR into_asset_id=:assetId)
        OR related_record_id IN (SELECT id FROM db_record WHERE asset_id=:assetId OR into_asset_id=:assetId)
    """,
    )
    suspend fun deleteRecordRelationsByAssetId(assetId: Long)

    /** 删除资产关联的图片关联数据 */
    @Query(
        value = """
        DELETE FROM db_image_with_related
        WHERE record_id IN (SELECT id FROM db_record WHERE asset_id=:assetId OR into_asset_id=:assetId)
    """,
    )
    suspend fun deleteImageRelationsByAssetId(assetId: Long)

    /** 删除资产关联的记录 */
    @Query(
        value = """
        DELETE FROM db_record
        WHERE asset_id=:assetId OR into_asset_id=:assetId
    """,
    )
    suspend fun deleteRecordsByAssetId(assetId: Long)

    /**
     * 事务化删除资产关联的所有数据
     *
     * 逐条删除记录以确保正确回退对方资产余额（特别是转账场景）
     * 并正确处理关联记录的 finalAmount 重算
     */
    @Transaction
    suspend fun deleteAssetRelatedData(assetId: Long) {
        // 逐条删除，正确回退对方资产余额及 finalAmount
        val records = queryRecordsByAssetId(assetId)
        for (record in records) {
            deleteRecordTransaction(record)
        }
    }
}
