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
import cn.wj.android.cashbook.core.database.table.TagWithRecordTable
import cn.wj.android.cashbook.core.database.table.TypeTable
import cn.wj.android.cashbook.core.database.throwable.DataTransactionException
import cn.wj.android.cashbook.core.model.enums.ClassificationTypeEnum
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum

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
    """
    )
    suspend fun deleteOldRelatedTags(recordId: Long)

    @Update
    suspend fun updateRecord(recordTable: RecordTable)

    @Update
    suspend fun updateAsset(assetTable: AssetTable)

    @Query(
        value = """
        SELECT * FROM db_asset WHERE id=:assetId
    """
    )
    suspend fun queryAssetById(assetId: Long): AssetTable?

    @Query(
        value = """
        SELECT * FROM db_type WHERE id=:typeId
    """
    )
    suspend fun queryTypeById(typeId: Long): TypeTable?

    @Query(
        value = """
        SELECT * FROM db_record WHERE id=:recordId
    """
    )
    suspend fun queryRecordById(recordId: Long): RecordTable?

    @Query(
        value = """
        DELETE FROM db_record_with_related WHERE record_id=:id
    """
    )
    suspend fun clearRelatedRecordById(id: Long)

    @Insert
    suspend fun insertRelatedRecord(related: List<RecordWithRelatedTable>)

    @Throws(DataTransactionException::class)
    @Transaction
    suspend fun updateRecordTransaction(
        recordTable: RecordTable,
        tagIdList: List<Long>,
        needRelated: Boolean,
        relatedRecordIdList: List<Long>,
    ) {
        if (null != recordTable.id) {
            // 修改记录，获取之前记录信息
            val oldRecord = queryRecordById(recordTable.id)
                ?: throw DataTransactionException("Record id not found")
            val oldType = queryTypeById(oldRecord.typeId)
                ?: throw DataTransactionException("Type must not be null")
            val oldCategory = RecordTypeCategoryEnum.ordinalOf(oldType.typeCategory)
            // 计算之前记录涉及金额
            val oldRecordAmount = if (oldCategory == RecordTypeCategoryEnum.INCOME) {
                // 收入，金额 - 手续费
                oldRecord.amount.toBigDecimalOrZero() - oldRecord.charge.toBigDecimalOrZero()
            } else {
                // 支出、转账，金额 + 手续费 - 优惠
                oldRecord.amount.toBigDecimal() + oldRecord.charge.toBigDecimalOrZero() - oldRecord.concessions.toBigDecimalOrZero()
            }
            queryAssetById(oldRecord.assetId)?.let { asset ->
                // 计算回退已用额度 or 余额
                val balance =
                    if (ClassificationTypeEnum.ordinalOf(asset.type) == ClassificationTypeEnum.CREDIT_CARD_ACCOUNT) {
                        // 信用卡账户，已用额度 - 记录金额
                        asset.balance.toBigDecimalOrZero() - oldRecordAmount
                    } else {
                        // 非信用卡账户，余额 + 记录金额
                        asset.balance.toBigDecimalOrZero() + oldRecordAmount
                    }
                // 更新资产
                updateAsset(asset.copy(balance = balance.decimalFormat().toDoubleOrZero()))
            }
            if (oldCategory == RecordTypeCategoryEnum.TRANSFER) {
                // 转账类型，更新关联资产信息
                queryAssetById(oldRecord.intoAssetId)?.let { asset ->
                    // 计算回退已用额度 or 余额
                    val balance =
                        if (ClassificationTypeEnum.ordinalOf(asset.type) == ClassificationTypeEnum.CREDIT_CARD_ACCOUNT) {
                            // 信用卡账户，已用额度 + 记录金额
                            asset.balance.toBigDecimalOrZero() + oldRecordAmount
                        } else {
                            // 非信用卡账户，余额 - 记录金额
                            asset.balance.toBigDecimalOrZero() - oldRecordAmount
                        }
                    // 更新资产
                    updateAsset(asset.copy(balance = balance.decimalFormat().toDoubleOrZero()))
                }
            }
        }

        // 更新或插入记录
        val id = if (recordTable.id == null) {
            insertRecord(recordTable)
        } else {
            updateRecord(recordTable)
            recordTable.id
        }

        // 获取类型信息
        val type = queryTypeById(recordTable.typeId)
            ?: throw DataTransactionException("Type must not be null")
        val category = RecordTypeCategoryEnum.ordinalOf(type.typeCategory)

        // 计算此次记录涉及金额
        val recordAmount = if (category == RecordTypeCategoryEnum.INCOME) {
            // 收入，金额 - 手续费
            recordTable.amount.toBigDecimalOrZero() - recordTable.charge.toBigDecimalOrZero()
        } else {
            // 支出、转账，金额 + 手续费 - 优惠
            recordTable.amount.toBigDecimal() + recordTable.charge.toBigDecimalOrZero() - recordTable.concessions.toBigDecimalOrZero()
        }

        // 更新相关资产信息
        queryAssetById(recordTable.assetId)?.let { asset ->
            // 计算已用额度 or 余额
            val balance =
                if (ClassificationTypeEnum.ordinalOf(asset.type) == ClassificationTypeEnum.CREDIT_CARD_ACCOUNT) {
                    // 信用卡账户，已用额度 + 记录金额
                    asset.balance.toBigDecimalOrZero() + recordAmount
                } else {
                    // 非信用卡账户，余额 - 记录金额
                    asset.balance.toBigDecimalOrZero() - recordAmount
                }
            // 更新资产
            updateAsset(asset.copy(balance = balance.decimalFormat().toDoubleOrZero()))
        }

        if (category == RecordTypeCategoryEnum.TRANSFER) {
            // 转账类型，更新关联资产信息
            queryAssetById(recordTable.intoAssetId)?.let { asset ->
                // 计算已用额度 or 余额
                val balance =
                    if (ClassificationTypeEnum.ordinalOf(asset.type) == ClassificationTypeEnum.CREDIT_CARD_ACCOUNT) {
                        // 信用卡账户，已用额度 - 记录金额
                        asset.balance.toBigDecimalOrZero() - recordAmount
                    } else {
                        // 非信用卡账户，余额 + 记录金额
                        asset.balance.toBigDecimalOrZero() + recordAmount
                    }
                // 更新资产
                updateAsset(asset.copy(balance = balance.decimalFormat().toDoubleOrZero()))
            }
        }

        if (null != recordTable.id) {
            // 更新数据，移除旧的关联标签
            deleteOldRelatedTags(id)
        }
        // 插入新的关联标签
        val insertTags = arrayListOf<TagWithRecordTable>()
        tagIdList.forEach {
            insertTags.add(TagWithRecordTable(id = null, recordId = id, tagId = it))
        }
        insertRelatedTags(insertTags)

        if (needRelated) {
            // 更新关联记录
            clearRelatedRecordById(id = id)
            insertRelatedRecord(relatedRecordIdList.map {
                RecordWithRelatedTable(
                    id = null,
                    recordId = id,
                    relatedRecordId = it,
                )
            })
        }
    }

    @Throws(DataTransactionException::class)
    @Transaction
    suspend fun deleteRecordTransaction(recordId: Long) {
        // 获取记录信息
        val record =
            queryRecordById(recordId) ?: throw DataTransactionException("Record id not found")
        val type = queryTypeById(record.typeId)
            ?: throw DataTransactionException("Type must not be null")
        val category = RecordTypeCategoryEnum.ordinalOf(type.typeCategory)
        // 计算之前记录涉及金额
        val oldRecordAmount = if (category == RecordTypeCategoryEnum.INCOME) {
            // 收入，金额 - 手续费
            record.amount.toBigDecimalOrZero() - record.charge.toBigDecimalOrZero()
        } else {
            // 支出、转账，金额 + 手续费 - 优惠
            record.amount.toBigDecimal() + record.charge.toBigDecimalOrZero() - record.concessions.toBigDecimalOrZero()
        }
        queryAssetById(record.assetId)?.let { asset ->
            // 计算回退已用额度 or 余额
            val balance =
                if (ClassificationTypeEnum.ordinalOf(asset.type) == ClassificationTypeEnum.CREDIT_CARD_ACCOUNT) {
                    // 信用卡账户，已用额度 - 记录金额
                    asset.balance.toBigDecimalOrZero() - oldRecordAmount
                } else {
                    // 非信用卡账户，余额 + 记录金额
                    asset.balance.toBigDecimalOrZero() + oldRecordAmount
                }
            // 更新资产
            updateAsset(asset.copy(balance = balance.decimalFormat().toDoubleOrZero()))
        }

        // 移除关联标签
        deleteOldRelatedTags(recordId)

        // TODO 移除关联记录

        // 删除当前记录
        val result = deleteRecord(record)
        if (result <= 0) {
            throw DataTransactionException("Record delete failed!")
        }
    }

    @Query(
        value = """
        SELECT * FROM db_record WHERE books_id=:bookId
    """
    )
    suspend fun queryRecordListByBookId(bookId: Long): List<RecordTable>

    @Query(
        """
        DELETE FROM db_record_with_related
        WHERE record_id=:recordId OR related_record_id=:recordId
    """
    )
    suspend fun deleteRecordRelationByRecordId(recordId: Long)

    @Query(
        value = """
        DELETE FROM db_tag_with_record
        WHERE record_id=:recordId
    """
    )
    suspend fun deleteTagRelationByRecordId(recordId: Long)

    @Query(
        value = """
       DELETE FROM db_books
        WHERE id=:bookId
    """
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
                deleteRecordRelationByRecordId(id)
                deleteTagRelationByRecordId(id)
            }
            deleteRecord(record)
        }
        // 删除账本
        deleteBookById(bookId)
    }

}