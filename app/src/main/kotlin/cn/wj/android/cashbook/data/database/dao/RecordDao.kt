package cn.wj.android.cashbook.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import cn.wj.android.cashbook.data.constants.SWITCH_INT_OFF
import cn.wj.android.cashbook.data.constants.SWITCH_INT_ON
import cn.wj.android.cashbook.data.database.table.RecordTable
import cn.wj.android.cashbook.data.enums.RecordTypeEnum
import cn.wj.android.cashbook.data.live.CurrentBooksLiveData

/**
 * 记录相关数据库操作接口
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/10
 */
@Dao
interface RecordDao {

    /** 插入记录数据 [record] 并返回生成的主键 id */
    @Insert
    fun insert(record: RecordTable): Long

    /** 插入或替换记录 */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrReplace(vararg records: RecordTable)

    /** 删除记录数据 [record] */
    @Delete
    fun delete(record: RecordTable)

    /** 删除与资产 id 为 [assetId] 相关联的所有转账、修改余额记录  */
    @Query("DELETE FROM db_record WHERE (asset_id=:assetId OR into_asset_id=:assetId) AND (type_enum=:modify OR type_enum=:transfer)")
    fun deleteModifyAndTransferByAssetId(
        assetId: Long,
        modify: String = RecordTypeEnum.MODIFY_BALANCE.name,
        transfer: String = RecordTypeEnum.TRANSFER.name
    )

    /** 更新 [record] 数据 */
    @Update
    fun update(record: RecordTable)

    /** 将类型为 [oldId] 的记录类型修改为 [newId] */
    @Query("UPDATE db_record SET type_id=:newId WHERE type_id=:oldId")
    fun updateTypeId(oldId: Long, newId: Long)

    /** 查询所有数据 */
    @Query("SELECT * FROM db_record")
    fun queryAll(): List<RecordTable>

    /** 根据 id 获取对应记录数据 */
    @Query("SELECT * FROM db_record WHERE id=:recordId")
    fun queryById(recordId: Long): RecordTable?

    /** 获取被关联的记录数据 */
    @Query("SELECT * FROM db_record WHERE record_id=:recordId")
    fun queryAssociatedById(recordId: Long): RecordTable?

    /** 查询最后一条修改记录 */
    @Query("SELECT * FROM db_record WHERE asset_id=:assetId AND type_enum=:type ORDER BY record_time DESC LIMIT 1")
    fun queryLastModifyRecord(
        assetId: Long,
        type: String = RecordTypeEnum.MODIFY_BALANCE.name
    ): List<RecordTable>

    /** 查询记录时间在 [recordTime] 之后的所有记录 */
    @Query("SELECT * FROM db_record WHERE asset_id=:assetId AND record_time>=:recordTime")
    fun queryAfterRecordTime(assetId: Long, recordTime: Long): List<RecordTable>

    /** 查询记录时间在 [recordTime] 之后的所有 转入资产id 为 [assetId] 的转账记录 */
    @Query("SELECT * FROM db_record WHERE into_asset_id=:assetId AND type_enum=:type AND record_time>=:recordTime")
    fun queryByIntoAssetIdAfterRecordTime(
        assetId: Long,
        recordTime: Long,
        type: String = RecordTypeEnum.TRANSFER.name
    ): List<RecordTable>

    /** 查询记录时间在 [recordTime] 之后且属于 id 为 [booksId] 的账本的记录 */
    @Query("SELECT * FROM db_record WHERE record_time>=:recordTime AND books_id=:booksId ORDER BY record_time DESC")
    fun queryAfterRecordTimeByBooksId(
        recordTime: Long,
        booksId: Long = CurrentBooksLiveData.booksId
    ): List<RecordTable>

    @Query(
        value = """
            SELECT * FROM db_record WHERE books_id=:booksId AND record_time>=:startDate AND record_time<:endDate
        """
    )
    suspend fun queryByBooksIdBetweenDate(
        startDate: Long,
        endDate: Long,
        booksId: Long = CurrentBooksLiveData.booksId
    ): List<RecordTable>

    /** 查询记录时间在 [startTime] 与 [endTime] 之间的记录 */
    @Query("SELECT * FROM db_record WHERE record_time>=:startTime AND record_time<=:endTime AND books_id=:booksId ORDER BY record_time DESC")
    fun queryRecordBetweenTime(
        startTime: Long,
        endTime: Long,
        booksId: Long = CurrentBooksLiveData.booksId
    ): List<RecordTable>

    /** 查询记录时间在 [startTime] 与 [endTime] 之间且类型 id 为 [typeId] 的记录 */
    @Query("SELECT * FROM db_record WHERE record_time>=:startTime AND record_time<=:endTime AND books_id=:booksId AND type_id=:typeId ORDER BY record_time DESC")
    fun queryRecordBetweenTimeByTypeId(
        typeId: Long,
        startTime: Long,
        endTime: Long,
        booksId: Long = CurrentBooksLiveData.booksId
    ): List<RecordTable>

    /** 获取与资产有关联的所有记录 */
    @Query("SELECT * FROM db_record WHERE (asset_id=:assetId OR into_asset_id=:assetId) ORDER BY record_time DESC LIMIT :pageSize OFFSET :pageNum")
    fun queryRecordByAssetId(assetId: Long, pageNum: Int, pageSize: Int): List<RecordTable>

    /** 获取关联资产 id 为 [assetId] 的记录数量 */
    @Query("SELECT COUNT(*) FROM db_record WHERE asset_id=:assetId")
    fun queryRecordCountByAssetId(assetId: Long): Int

    /** 获取分类 id 为 [typeId] 的记录数量 */
    @Query("SELECT COUNT(*) FROM db_record WHERE type_id=:typeId")
    fun queryRecordCountByTypeId(typeId: Long): Int

    /** 查询金额小于等于 [amount] 记录时间在 [recordTime] 之后的支出记录 */
    @Query("SELECT * FROM db_record WHERE record_time>=:recordTime AND type_enum=:type AND books_id=:booksId AND amount>=:amount ORDER BY record_time DESC")
    fun queryExpenditureRecordAfterDateLargerThanAmount(
        booksId: Long,
        amount: Float,
        recordTime: Long,
        type: String = RecordTypeEnum.EXPENDITURE.name
    ): List<RecordTable>

    /** 查询标记为可报销记录时间在 [recordTime] 之后的支出记录 */
    @Query("SELECT * FROM db_record WHERE record_time>=:recordTime AND type_enum=:type AND books_id=:booksId AND reimbursable=$SWITCH_INT_ON ORDER BY record_time DESC")
    fun queryReimburseExpenditureRecordAfterDate(
        booksId: Long,
        recordTime: Long,
        type: String = RecordTypeEnum.EXPENDITURE.name
    ): List<RecordTable>

    /** 查询金额或备注满足 [keywords] 条件的前 30 条支出记录 */
    @Query("SELECT * FROM db_record WHERE type_enum=:type AND books_id=:booksId AND reimbursable=$SWITCH_INT_OFF AND (remark LIKE :keywords OR amount LIKE :keywords) ORDER BY record_time DESC LIMIT 30")
    fun queryExpenditureRecordByKeywords(
        keywords: String,
        booksId: Long = CurrentBooksLiveData.booksId,
        type: String = RecordTypeEnum.EXPENDITURE.name
    ): List<RecordTable>

    /** 查询金额或备注满足 [keywords] 条件的前 30 条支出记录 */
    @Query("SELECT * FROM db_record WHERE type_enum=:type AND books_id=:booksId AND reimbursable=$SWITCH_INT_ON AND (remark LIKE :keywords OR amount LIKE :keywords) ORDER BY record_time DESC LIMIT 30")
    fun queryReimburseExpenditureRecordByKeywords(
        keywords: String,
        booksId: Long = CurrentBooksLiveData.booksId,
        type: String = RecordTypeEnum.EXPENDITURE.name
    ): List<RecordTable>

    /** 查询金额或备注满足 [keywords] 条件的第 [pageNum] 页 [pageSize] 条记录 */
    @Query("SELECT * FROM db_record WHERE type_enum!=:type AND books_id=:booksId AND (remark LIKE :keywords OR amount LIKE :keywords) ORDER BY record_time DESC LIMIT :pageSize OFFSET :pageNum")
    fun queryRecordByKeywords(
        keywords: String,
        pageNum: Int,
        pageSize: Int,
        booksId: Long = CurrentBooksLiveData.booksId,
        type: String = RecordTypeEnum.MODIFY_BALANCE.name
    ): List<RecordTable>
}