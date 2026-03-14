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
        val type = when (record.typeId) {
            TYPE_TABLE_BALANCE_EXPENDITURE.id -> {
                TYPE_TABLE_BALANCE_EXPENDITURE
            }

            TYPE_TABLE_BALANCE_INCOME.id -> {
                TYPE_TABLE_BALANCE_INCOME
            }

            else -> {
                queryTypeById(record.typeId)
            }
        } ?: throw DataTransactionException("Type must not be null")

        val category = RecordTypeCategoryEnum.ordinalOf(type.typeCategory)
        // 计算记录涉及金额（单位：分）
        val recordAmount = if (category == RecordTypeCategoryEnum.INCOME) {
            // 收入，金额 - 手续费
            record.amount - record.charge
        } else {
            // 支出、转账，金额 + 手续费 - 优惠
            record.amount + record.charge - record.concessions
        }
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
        val type = when (record.typeId) {
            TYPE_TABLE_BALANCE_EXPENDITURE.id -> {
                TYPE_TABLE_BALANCE_EXPENDITURE
            }

            TYPE_TABLE_BALANCE_INCOME.id -> {
                TYPE_TABLE_BALANCE_INCOME
            }

            else -> {
                queryTypeById(record.typeId)
            }
        } ?: throw DataTransactionException("Type must not be null")

        val category = RecordTypeCategoryEnum.ordinalOf(type.typeCategory)
        // 计算之前记录涉及金额（单位：分）
        val oldRecordAmount = if (category == RecordTypeCategoryEnum.INCOME) {
            // 收入，金额 - 手续费
            record.amount - record.charge
        } else {
            // 支出、转账，金额 + 手续费 - 优惠
            record.amount + record.charge - record.concessions
        }
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
            // 删除支出记录，需要更新报销、退款记录，最终金额为 关联最终金额 + 支出记录的finalAmount
            queryRelatedByRelatedRecordId(recordId).mapNotNull { related ->
                queryRecordById(related.recordId)
            }.forEach { relatedRecord ->
                val relatedRecordId = relatedRecord.id
                if (null != relatedRecordId) {
                    updateRecordFinalAmountById(
                        relatedRecordId,
                        relatedRecord.finalAmount + oldRecordAmount,
                    )
                }
            }
        } else if (category == RecordTypeCategoryEnum.INCOME) {
            // 删除收入记录，需要更新被关联的支出记录，最终金额需重新计算
            queryRelatedByRecordId(recordId).mapNotNull { related ->
                queryRecordById(related.relatedRecordId)
            }.forEach { relatedRecord ->
                val relatedRecordId = relatedRecord.id
                if (null != relatedRecordId) {
                    updateRecordFinalAmountById(
                        relatedRecordId,
                        relatedRecord.amount - relatedRecord.concessions + relatedRecord.charge,
                    )
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

    @Throws(DataTransactionException::class)
    @Transaction
    suspend fun deleteBookTransaction(bookId: Long) {
        // 批量删除当前账本下的标签关联
        deleteTagRelationsByBookId(bookId)
        // 批量删除当前账本下的记录关联
        deleteRecordRelationsByBookId(bookId)
        // 批量删除当前账本下的图片关联
        deleteImageRelationsByBookId(bookId)
        // 批量删除当前账本下的记录
        deleteRecordsByBookId(bookId)
        // 删除账本
        deleteBookById(bookId)
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
     * 按顺序删除：标签关联 -> 记录关联 -> 图片关联 -> 记录
     */
    @Transaction
    suspend fun deleteAssetRelatedData(assetId: Long) {
        deleteTagRelationsByAssetId(assetId)
        deleteRecordRelationsByAssetId(assetId)
        deleteImageRelationsByAssetId(assetId)
        deleteRecordsByAssetId(assetId)
    }
}
