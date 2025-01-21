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
import cn.wj.android.cashbook.core.common.ext.decimalFormat
import cn.wj.android.cashbook.core.common.ext.toBigDecimalOrZero
import cn.wj.android.cashbook.core.common.ext.toDoubleOrZero
import cn.wj.android.cashbook.core.database.table.AssetTable
import cn.wj.android.cashbook.core.database.table.RecordTable
import cn.wj.android.cashbook.core.database.table.RecordWithRelatedTable
import cn.wj.android.cashbook.core.database.table.TYPE_TABLE_BALANCE_EXPENDITURE
import cn.wj.android.cashbook.core.database.table.TYPE_TABLE_BALANCE_INCOME
import cn.wj.android.cashbook.core.database.table.TagWithRecordTable
import cn.wj.android.cashbook.core.database.table.TypeTable
import cn.wj.android.cashbook.core.database.throwable.DataTransactionException
import cn.wj.android.cashbook.core.model.enums.ClassificationTypeEnum
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import java.math.BigDecimal

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

    @Delete
    suspend fun deleteRecord(recordTable: RecordTable): Int

    @Query(
        value = """
        DELETE FROM db_tag_with_record WHERE record_id=:recordId
    """,
    )
    suspend fun deleteOldRelatedTags(recordId: Long)

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
    ) {
        // 删除旧数据
        deleteRecordTransaction(record.id)
        // 插入新数据
        insertRecordTransaction(
            record = record,
            tagIdList = tagIdList,
            needRelated = needRelated,
            relatedRecordIdList = relatedRecordIdList,
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
        // 计算记录涉及金额
        val recordAmount = if (category == RecordTypeCategoryEnum.INCOME) {
            // 收入，金额 - 手续费
            record.amount.toBigDecimalOrZero() - record.charge.toBigDecimalOrZero()
        } else {
            // 支出、转账，金额 + 手续费 - 优惠
            record.amount.toBigDecimal() + record.charge.toBigDecimalOrZero() - record.concessions.toBigDecimalOrZero()
        }
        // 更新资产余额
        queryAssetById(record.assetId)?.let { asset ->
            // 计算已用额度 or 余额
            val balance =
                if (ClassificationTypeEnum.ordinalOf(asset.type) == ClassificationTypeEnum.CREDIT_CARD_ACCOUNT) {
                    // 信用卡账户
                    if (category == RecordTypeCategoryEnum.INCOME) {
                        // 收入，已用额度 - 记录金额
                        asset.balance.toBigDecimalOrZero() - recordAmount
                    } else {
                        // 支出、转账，已用额度 + 记录金额
                        asset.balance.toBigDecimalOrZero() + recordAmount
                    }
                } else {
                    // 非信用卡账户
                    if (category == RecordTypeCategoryEnum.INCOME) {
                        // 收入，余额 + 记录金额
                        asset.balance.toBigDecimalOrZero() + recordAmount
                    } else {
                        // 支出、转账，余额 - 记录金额
                        asset.balance.toBigDecimalOrZero() - recordAmount
                    }
                }
            // 更新资产
            updateAsset(asset.copy(balance = balance.decimalFormat().toDoubleOrZero()))
        }
        if (category == RecordTypeCategoryEnum.TRANSFER) {
            // 转账，更新关联资产余额
            queryAssetById(record.intoAssetId)?.let { asset ->
                // 计算已用额度 or 余额
                val balance =
                    if (ClassificationTypeEnum.ordinalOf(asset.type) == ClassificationTypeEnum.CREDIT_CARD_ACCOUNT) {
                        // 信用卡账户，已用额度 - 记录金额
                        asset.balance.toBigDecimalOrZero() - record.amount.toBigDecimalOrZero()
                    } else {
                        // 非信用卡账户，余额 + 记录金额
                        asset.balance.toBigDecimalOrZero() + record.amount.toBigDecimalOrZero()
                    }
                // 更新资产
                updateAsset(asset.copy(balance = balance.decimalFormat().toDoubleOrZero()))
            }
        }

        val recordId = insertRecord(record.copy(finalAmount = recordAmount.toDouble()))

        // 插入新的关联标签
        insertRelatedTags(
            tagIdList.map { tagId ->
                TagWithRecordTable(id = null, recordId = recordId, tagId = tagId)
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
            var relatedAmount = BigDecimal.ZERO
            relatedRecordIdList.mapNotNull { relatedRecordId ->
                queryRecordById(relatedRecordId)
            }.forEach { relatedRecord ->
                relatedAmount += relatedRecord.finalAmount.toBigDecimalOrZero()
            }
            relatedRecordIdList.forEach { relatedRecordId ->
                updateRecordFinalAmountById(relatedRecordId, 0.0)
            }
            updateRecordFinalAmountById(recordId, (recordAmount - relatedAmount).toDouble())
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
        // 计算之前记录涉及金额
        val oldRecordAmount = if (category == RecordTypeCategoryEnum.INCOME) {
            // 收入，金额 - 手续费
            record.amount.toBigDecimalOrZero() - record.charge.toBigDecimalOrZero()
        } else {
            // 支出、转账，金额 + 手续费 - 优惠
            record.amount.toBigDecimal() + record.charge.toBigDecimalOrZero() - record.concessions.toBigDecimalOrZero()
        }
        // 更新资产余额
        queryAssetById(record.assetId)?.let { asset ->
            // 计算回退已用额度 or 余额
            val balance =
                if (ClassificationTypeEnum.ordinalOf(asset.type) == ClassificationTypeEnum.CREDIT_CARD_ACCOUNT) {
                    // 信用卡账户
                    if (category == RecordTypeCategoryEnum.INCOME) {
                        // 收入，已用额度 + 记录金额
                        asset.balance.toBigDecimalOrZero() + oldRecordAmount
                    } else {
                        // 支出、转账，已用额度 - 记录金额
                        asset.balance.toBigDecimalOrZero() - oldRecordAmount
                    }
                } else {
                    // 非信用卡账户
                    if (category == RecordTypeCategoryEnum.INCOME) {
                        // 收入，余额 - 记录金额
                        asset.balance.toBigDecimalOrZero() - oldRecordAmount
                    } else {
                        // 支出、转账，余额 + 记录金额
                        asset.balance.toBigDecimalOrZero() + oldRecordAmount
                    }
                }
            // 更新资产
            updateAsset(asset.copy(balance = balance.decimalFormat().toDoubleOrZero()))
        }
        if (category == RecordTypeCategoryEnum.TRANSFER) {
            // 转账，更新关联资产余额
            queryAssetById(record.intoAssetId)?.let { asset ->
                // 计算回退已用额度 or 余额
                val balance =
                    if (ClassificationTypeEnum.ordinalOf(asset.type) == ClassificationTypeEnum.CREDIT_CARD_ACCOUNT) {
                        // 信用卡账户，已用额度 + 记录金额
                        asset.balance.toBigDecimalOrZero() + record.amount.toBigDecimalOrZero()
                    } else {
                        // 非信用卡账户，余额 - 记录金额
                        asset.balance.toBigDecimalOrZero() - record.amount.toBigDecimalOrZero()
                    }
                // 更新资产
                updateAsset(asset.copy(balance = balance.decimalFormat().toDoubleOrZero()))
            }
        }

        // 移除关联标签
        deleteOldRelatedTags(recordId)

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
                        (relatedRecord.finalAmount.toBigDecimalOrZero() + oldRecordAmount).decimalFormat()
                            .toDoubleOrZero(),
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
                        (relatedRecord.amount.toBigDecimalOrZero() - relatedRecord.concessions.toBigDecimalOrZero() + relatedRecord.charge.toBigDecimalOrZero()).decimalFormat()
                            .toDoubleOrZero(),
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
    suspend fun updateRecordFinalAmountById(id: Long, finalAmount: Double)

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

    @Throws(DataTransactionException::class)
    @Transaction
    suspend fun deleteBookTransaction(bookId: Long) {
        // 查询当前账本下的记录
        val recordList = queryRecordListByBookId(bookId)
        // 从记录关系表和标签关系表中删除对应记录
        recordList.forEach { record ->
            val id = record.id
            if (null != id) {
                deleteTagRelationByRecordId(id)
                deleteRecordRelationByRecordId(id)
            }
            deleteRecord(record)
        }
        // 删除账本
        deleteBookById(bookId)
    }

    @Transaction
    suspend fun deleteTag(id: Long) {
        deleteTagRelationByTagId(id)
        deleteTagById(id)
    }
}
