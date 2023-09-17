package cn.wj.android.cashbook.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import cn.wj.android.cashbook.core.common.SWITCH_INT_ON
import cn.wj.android.cashbook.core.database.relation.RecordViewsRelation
import cn.wj.android.cashbook.core.database.table.RecordTable

/**
 * 记录数据库操作类
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/2/20
 */
@Dao
interface RecordDao {

    @Query(
        value = """
        SELECT * FROM db_record WHERE id=:recordId
    """
    )
    suspend fun queryById(recordId: Long): RecordTable?

    @Query(
        value = """
        SELECT * FROM db_record WHERE id IN (SELECT record_id FROM db_record_with_related WHERE record_id=:recordId)
    """
    )
    suspend fun queryRelatedById(recordId: Long): List<RecordTable>

    @Query(
        value = """
        SELECT * FROM db_record WHERE books_id=:booksId AND record_time>=:dateTime
    """
    )
    suspend fun queryByBooksIdAfterDate(booksId: Long, dateTime: Long): List<RecordTable>

    @Query(
        value = """
            SELECT * FROM db_record WHERE books_id=:booksId AND record_time>=:startDate AND record_time<:endDate
        """
    )
    suspend fun queryByBooksIdBetweenDate(
        booksId: Long,
        startDate: Long,
        endDate: Long
    ): List<RecordTable>

    @Query(
        value = """
        SELECT * FROM db_record WHERE books_id=:booksId AND reimbursable=$SWITCH_INT_ON AND record_time>=:dateTime
    """
    )
    suspend fun queryReimburseByBooksIdAfterDate(booksId: Long, dateTime: Long): List<RecordTable>

    @Query(
        value = """
        SELECT db_record.id as id, db_record.amount as amount, db_record.charge as charges, db_record.concessions as  concessions, 
        db_record.remark as remark, db_record.reimbursable as reimbursable, db_record.record_time as recordTime,
        db_type.type_category as typeCategory,db_type.name as typeName, db_type.icon_name as typeIconResName,
        db_asset.name as assetName, db_asset.classification as assetClassification,
        related.name as relatedAssetName, related.classification as relatedAssetClassification
        FROM db_record
        JOIN db_type ON db_type.id = db_record.type_id
        LEFT JOIN db_asset ON db_asset.id = db_record.asset_id
        LEFT JOIN db_asset AS related ON related.id = db_record.into_asset_id
        WHERE db_record.books_id = :booksId
    """
    )
    suspend fun query(booksId: Long): List<RecordViewsRelation>

    /** 资产 id 为 [assetId] 的第 [pageNum] 页 [pageSize] 条记录 */
    @Query("SELECT * FROM db_record WHERE books_id=:booksId AND asset_id=:assetId ORDER BY record_time DESC LIMIT :pageSize OFFSET :pageNum")
    suspend  fun queryRecordByAssetId(
        booksId: Long,
        assetId: Long,
        pageNum: Int,
        pageSize: Int,
    ): List<RecordTable>

    @Query("SELECT * FROM db_record WHERE type_id=:id")
    fun queryByTypeId(id: Long): List<RecordTable>

    @Query("UPDATE db_record SET type_id=:toId WHERE type_id=:fromId")
    suspend fun changeRecordTypeBeforeDeleteType(fromId: Long, toId: Long)
}