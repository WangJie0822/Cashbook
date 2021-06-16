package cn.wj.android.cashbook.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import cn.wj.android.cashbook.data.database.table.RecordTable
import cn.wj.android.cashbook.data.enums.RecordTypeEnum

/**
 * 记录相关数据库操作接口
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/10
 */
@Dao
interface RecordDao {

    /** 插入记录数据 [record] 并返回生成的主键 id */
    @Insert
    suspend fun insert(record: RecordTable): Long

    /** 删除记录数据 [record] */
    @Delete
    suspend fun delete(record: RecordTable)

    /** 删除与资产 id 为 [assetId] 相关联的所有转账、修改余额记录  */
    @Query("DELETE FROM db_record WHERE (asset_id=:assetId OR into_asset_id=:assetId) AND (type=:modify OR type=:transfer)")
    suspend fun deleteModifyAndTransferByAssetId(assetId: Long, modify: String = RecordTypeEnum.MODIFY_BALANCE.name, transfer: String = RecordTypeEnum.TRANSFER.name)

    /** 更新 [record] 数据 */
    @Update
    suspend fun update(record: RecordTable)

    /** 根据 id 获取对应记录数据 */
    @Query("SELECT * FROM db_record WHERE id=:recordId")
    suspend fun queryById(recordId: Long): RecordTable?

    /** 查询最后一条修改记录 */
    @Query("SELECT * FROM db_record WHERE asset_id=:assetId AND type=:type ORDER BY record_time DESC LIMIT 1")
    suspend fun queryLastModifyRecord(assetId: Long, type: String = RecordTypeEnum.MODIFY_BALANCE.name): List<RecordTable>

    /** 查询记录时间在 [recordTime] 之后的所有记录 */
    @Query("SELECT * FROM db_record WHERE asset_id=:assetId AND record_time>=:recordTime")
    suspend fun queryAfterRecordTime(assetId: Long, recordTime: Long): List<RecordTable>

    /** 查询记录时间在 [recordTime] 之后的所有 转入资产id 为 [assetId] 的转账记录 */
    @Query("SELECT * FROM db_record WHERE into_asset_id=:assetId AND type=:type AND record_time>=:recordTime")
    suspend fun queryByIntoAssetIdAfterRecordTime(assetId: Long, recordTime: Long, type: String = RecordTypeEnum.TRANSFER.name): List<RecordTable>

    /** 查询记录时间在 [recordTime] 之后且属于 id 为 [booksId] 的账本的记录 */
    @Query("SELECT * FROM db_record WHERE record_time>=:recordTime AND books_id=:booksId ORDER BY record_time DESC")
    suspend fun queryAfterRecordTimeByBooksId(booksId: Long, recordTime: Long): List<RecordTable>

    /** 获取与资产有关联的所有记录 */
    @Query("SELECT * FROM db_record WHERE (asset_id=:assetId OR into_asset_id=:assetId) ORDER BY record_time DESC LIMIT :pageSize OFFSET :pageNum")
    suspend fun queryRecordByAssetId(assetId: Long, pageNum: Int, pageSize: Int): List<RecordTable>
}