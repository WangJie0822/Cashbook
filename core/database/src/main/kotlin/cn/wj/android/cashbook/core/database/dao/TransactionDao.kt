package cn.wj.android.cashbook.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import cn.wj.android.cashbook.core.common.ext.decimalFormat
import cn.wj.android.cashbook.core.common.ext.toBigDecimalOrZero
import cn.wj.android.cashbook.core.common.ext.toDoubleOrZero
import cn.wj.android.cashbook.core.database.table.AssetTable
import cn.wj.android.cashbook.core.database.table.RecordTable
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
    fun insertRecord(recordTable: RecordTable): Long

    @Insert
    fun insertRelatedTags(tagWithRecordTable: List<TagWithRecordTable>)

    @Query(
        value = """
        DELETE FROM db_tag_with_record WHERE record_id=:recordId
    """
    )
    fun deleteOldRelatedTags(recordId: Long)

    @Update
    fun updateRecord(recordTable: RecordTable)

    @Update
    fun updateAsset(assetTable: AssetTable)

    @Query(
        value = """
        SELECT * FROM db_record WHERE id=:assetId
    """
    )
    fun queryAssetById(assetId: Long): AssetTable?

    @Query(
        value = """
        SELECT * FROM db_type WHERE id=:typeId
    """
    )
    fun queryTypeById(typeId: Long): TypeTable?

    @Throws(DataTransactionException::class)
    @Transaction
    fun updateRecord(recordTable: RecordTable, tags: List<Long>) {
        // 获取类型信息
        val type = queryTypeById(recordTable.typeId)
            ?: throw DataTransactionException("Type must not be null")
        val category = RecordTypeCategoryEnum.valueOf(type.typeCategory)
        val insert = null == recordTable.id
        // 更新或插入记录
        val id = if (recordTable.id == null) {
            insertRecord(recordTable)
        } else {
            updateRecord(recordTable)
            recordTable.id
        }

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
                if (ClassificationTypeEnum.valueOf(asset.type) == ClassificationTypeEnum.CREDIT_CARD_ACCOUNT) {
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
                    if (ClassificationTypeEnum.valueOf(asset.type) == ClassificationTypeEnum.CREDIT_CARD_ACCOUNT) {
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

        if (!insert) {
            // 更新数据，移除旧的关联标签
            deleteOldRelatedTags(id)
        }
        // 插入新的关联标签
        val insertTags = arrayListOf<TagWithRecordTable>()
        tags.forEach {
            insertTags.add(TagWithRecordTable(id = null, recordId = id, tagId = it))
        }
        insertRelatedTags(insertTags)
    }
}