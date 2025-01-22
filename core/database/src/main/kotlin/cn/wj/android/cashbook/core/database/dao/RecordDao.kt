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
import androidx.room.Query
import androidx.room.Update
import cn.wj.android.cashbook.core.common.SWITCH_INT_ON
import cn.wj.android.cashbook.core.database.relation.RecordViewsRelation
import cn.wj.android.cashbook.core.database.table.RecordTable
import cn.wj.android.cashbook.core.database.table.RecordWithRelatedTable
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum

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
    """,
    )
    suspend fun queryById(recordId: Long): RecordTable?

    @Query(
        value = """
        SELECT * FROM db_record WHERE id IN (SELECT record_id FROM db_record_with_related WHERE record_id=:recordId)
    """,
    )
    suspend fun queryRelatedById(recordId: Long): List<RecordTable>

    @Query(
        value = """
        SELECT * FROM db_record WHERE books_id=:booksId AND record_time>=:dateTime
    """,
    )
    suspend fun queryByBooksIdAfterDate(booksId: Long, dateTime: Long): List<RecordTable>

    @Query(
        value = """
            SELECT * FROM db_record 
            WHERE record_time>=:startDate 
            AND record_time<:endDate
            AND books_id=:booksId 
        """,
    )
    suspend fun queryByBooksIdBetweenDate(
        booksId: Long,
        startDate: Long,
        endDate: Long,
    ): List<RecordTable>

    @Query(
        value = """
        SELECT * FROM db_record WHERE books_id=:booksId AND reimbursable=$SWITCH_INT_ON AND record_time>=:dateTime
    """,
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
    """,
    )
    suspend fun query(booksId: Long): List<RecordViewsRelation>

    /** 资产 id 为 [assetId] 的第 [pageNum] 页 [pageSize] 条记录 */
    @Query("SELECT * FROM db_record WHERE books_id=:booksId AND asset_id=:assetId ORDER BY record_time DESC LIMIT :pageSize OFFSET :pageNum")
    suspend fun queryRecordByAssetId(
        booksId: Long,
        assetId: Long,
        pageNum: Int,
        pageSize: Int,
    ): List<RecordTable>

    /** 类型 id 为 [typeId] 的第 [pageNum] 页 [pageSize] 条记录 */
    @Query(
        """
        SELECT * FROM db_record 
        WHERE books_id=:booksId 
        AND (type_id=:typeId 
        OR type_id IN (SELECT id FROM db_type WHERE parent_id=:typeId))
        ORDER BY record_time DESC LIMIT :pageSize OFFSET :pageNum
    """,
    )
    suspend fun queryRecordByTypeId(
        booksId: Long,
        typeId: Long,
        pageNum: Int,
        pageSize: Int,
    ): List<RecordTable>

    /** 标签 id 为 [tagId] 的第 [pageNum] 页 [pageSize] 条记录 */
    @Query(
        """
        SELECT * FROM db_record 
        WHERE books_id=:booksId
        AND id IN (
            SELECT record_id FROM db_tag_with_record
            WHERE tag_id=:tagId
        ) 
        ORDER BY record_time 
        DESC LIMIT :pageSize 
        OFFSET :pageNum
    """,
    )
    suspend fun queryRecordByTagId(
        booksId: Long,
        tagId: Long,
        pageNum: Int,
        pageSize: Int,
    ): List<RecordTable>

    @Query(
        value = """
        SELECT * FROM db_record 
        WHERE books_id=:booksId 
        AND (remark LIKE '%'||:keyword||'%'
        OR amount LIKE '%'||:keyword||'%'
        OR charge  LIKE '%'||:keyword||'%'
        OR concessions LIKE '%'||:keyword||'%') 
        ORDER BY record_time 
        DESC LIMIT :pageSize 
        OFFSET :pageNum
    """,
    )
    suspend fun queryRecordByKeyword(
        booksId: Long,
        keyword: String,
        pageNum: Int,
        pageSize: Int,
    ): List<RecordTable>

    @Query("SELECT * FROM db_record WHERE type_id=:id")
    fun queryByTypeId(id: Long): List<RecordTable>

    @Query(
        """
        SELECT * 
        FROM db_record 
        WHERE type_id IN (
            SELECT id 
            FROM db_type 
            WHERE type_category=:typeCategoryId
        )
    """,
    )
    fun queryByTypeCategory(
        typeCategoryId: Int,
    ): List<RecordTable>

    @Query("SELECT * FROM db_record_with_related")
    fun queryRelatedRecord(): List<RecordWithRelatedTable>

    @Query("SELECT COUNT(*) FROM db_record_with_related WHERE related_record_id=:id OR record_id=:id")
    fun queryRelatedRecordCountByID(id: Long): Int

    @Update
    suspend fun updateRecord(list: List<RecordTable>): Int

    @Query("UPDATE db_record SET type_id=:toId WHERE type_id=:fromId")
    suspend fun changeRecordTypeBeforeDeleteType(fromId: Long, toId: Long)

    @Query("SELECT related_record_id FROM db_record_with_related WHERE record_id=:id")
    suspend fun getRelatedIdListById(id: Long): List<Long>

    @Query("SELECT record_id FROM db_record_with_related WHERE related_record_id=:id")
    suspend fun getRecordIdListFromRelatedId(id: Long): List<Long>

    @Query(
        """
        SELECT * FROM db_record 
        WHERE record_time>=:recordTime 
        AND type_id IN (SELECT id FROM db_type WHERE type_category=:incomeCategory)
        AND books_id=:booksId 
        ORDER BY record_time DESC LIMIT 50
    """,
    )
    fun getExpenditureRecordListAfterTime(
        booksId: Long,
        recordTime: Long,
        incomeCategory: Int = RecordTypeCategoryEnum.EXPENDITURE.ordinal,
    ): List<RecordTable>

    @Query(
        """
        SELECT * FROM db_record 
        WHERE record_time>=:recordTime 
        AND reimbursable=$SWITCH_INT_ON
        AND books_id=:booksId 
        ORDER BY record_time DESC LIMIT 50
    """,
    )
    fun getExpenditureReimburseRecordListAfterTime(
        booksId: Long,
        recordTime: Long,
    ): List<RecordTable>

    @Query(
        """
        SELECT * FROM db_record 
        WHERE record_time>=:recordTime 
        AND type_id IN (SELECT id FROM db_type WHERE type_category=:incomeCategory)
        AND books_id=:booksId
        AND (remark LIKE :keyword OR amount LIKE :keyword)
        ORDER BY record_time DESC LIMIT 50
    """,
    )
    fun getExpenditureRecordListByKeywordAfterTime(
        keyword: String,
        booksId: Long,
        recordTime: Long,
        incomeCategory: Int = RecordTypeCategoryEnum.EXPENDITURE.ordinal,
    ): List<RecordTable>

    @Query(
        """
        SELECT COUNT(*) FROM db_record 
        WHERE record_time>=:recordTime 
        AND asset_id=:assetId
    """,
    )
    fun getRecordCountByAssetIdAfterTime(
        assetId: Long,
        recordTime: Long,
    ): Int

    @Query(
        """
        SELECT * FROM db_record 
        WHERE record_time>=:recordTime 
        AND reimbursable=$SWITCH_INT_ON
        AND books_id=:booksId 
        AND (remark LIKE :keyword OR amount LIKE :keyword)
        ORDER BY record_time DESC LIMIT 50
    """,
    )
    fun getLastThreeMonthExpenditureReimburseRecordListByKeyword(
        keyword: String,
        booksId: Long,
        recordTime: Long,
    ): List<RecordTable>

    @Query(
        value = """
        DELETE FROM db_record
        WHERE asset_id=:assetId OR into_asset_id=:assetId
    """,
    )
    fun deleteWithAsset(assetId: Long)

    @Query(
        value = """
        DELETE FROM db_record_with_related
        WHERE record_id IN (SELECT id FROM db_record WHERE asset_id=:assetId OR into_asset_id=:assetId)
        OR related_record_id IN (SELECT id FROM db_record WHERE asset_id=:assetId OR into_asset_id=:assetId)
    """,
    )
    fun deleteRelatedWithAsset(assetId: Long)
}
