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

package cn.wj.android.cashbook.core.data.testdoubles

import androidx.paging.PagingSource
import androidx.paging.PagingState
import cn.wj.android.cashbook.core.common.SWITCH_INT_ON
import cn.wj.android.cashbook.core.database.dao.RecordDao
import cn.wj.android.cashbook.core.database.relation.RecordViewsRelation
import cn.wj.android.cashbook.core.database.table.ImageWithRelatedTable
import cn.wj.android.cashbook.core.database.table.RecordTable
import cn.wj.android.cashbook.core.database.table.RecordWithRelatedTable
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum

/**
 * RecordDao 的测试替身，使用内存列表存储数据
 */
class FakeRecordDao : RecordDao {

    /** 记录数据列表 */
    val records = mutableListOf<RecordTable>()

    /** 关联记录数据列表 */
    val relatedRecords = mutableListOf<RecordWithRelatedTable>()

    /** 图片关联数据列表 */
    val images = mutableListOf<ImageWithRelatedTable>()

    /** 类型数据列表，用于类型相关查询 */
    val types = mutableListOf<FakeTypeEntry>()

    /** 标签关联数据列表 */
    val tagWithRecords = mutableListOf<FakeTagWithRecordEntry>()

    /** 自增主键计数器 */
    private var nextId = 1L

    override suspend fun queryById(recordId: Long): RecordTable? {
        return records.firstOrNull { it.id == recordId }
    }

    override suspend fun queryRelatedById(recordId: Long): List<RecordTable> {
        // 查询和 recordId 关联的记录
        val relatedIds = relatedRecords
            .filter { it.recordId == recordId }
            .map { it.relatedRecordId }
        return records.filter { it.id in relatedIds }
    }

    override suspend fun queryByBooksIdAfterDate(booksId: Long, dateTime: Long): List<RecordTable> {
        return records.filter { it.booksId == booksId && it.recordTime >= dateTime }
    }

    override suspend fun queryByBooksIdBetweenDate(
        booksId: Long,
        startDate: Long,
        endDate: Long,
    ): List<RecordTable> {
        return records.filter {
            it.booksId == booksId && it.recordTime >= startDate && it.recordTime < endDate
        }
    }

    override suspend fun queryReimburseByBooksIdAfterDate(
        booksId: Long,
        dateTime: Long,
    ): List<RecordTable> {
        return records.filter {
            it.booksId == booksId && it.reimbursable == SWITCH_INT_ON && it.recordTime >= dateTime
        }
    }

    override suspend fun query(booksId: Long): List<RecordViewsRelation> {
        // 简化实现，返回空列表
        return emptyList()
    }

    override suspend fun queryRecordByAssetId(
        booksId: Long,
        assetId: Long,
        pageNum: Int,
        pageSize: Int,
    ): List<RecordTable> {
        return records.filter { it.booksId == booksId && it.assetId == assetId }
            .sortedByDescending { it.recordTime }
            .drop(pageNum)
            .take(pageSize)
    }

    override suspend fun queryRecordByTypeId(
        booksId: Long,
        typeId: Long,
        pageNum: Int,
        pageSize: Int,
    ): List<RecordTable> {
        // 查找 typeId 或 parentId 为 typeId 的子类型 id
        val childTypeIds = types.filter { it.parentId == typeId }.mapNotNull { it.id }
        return records.filter {
            it.booksId == booksId && (it.typeId == typeId || it.typeId in childTypeIds)
        }
            .sortedByDescending { it.recordTime }
            .drop(pageNum)
            .take(pageSize)
    }

    override suspend fun queryRecordByTypeIdBetween(
        booksId: Long,
        typeId: Long,
        startDate: Long,
        endDate: Long,
        pageNum: Int,
        pageSize: Int,
    ): List<RecordTable> {
        val childTypeIds = types.filter { it.parentId == typeId }.mapNotNull { it.id }
        return records.filter {
            it.booksId == booksId &&
                it.recordTime >= startDate &&
                it.recordTime < endDate &&
                (it.typeId == typeId || it.typeId in childTypeIds)
        }
            .sortedByDescending { it.recordTime }
            .drop(pageNum)
            .take(pageSize)
    }

    override suspend fun queryRecordByTagId(
        booksId: Long,
        tagId: Long,
        pageNum: Int,
        pageSize: Int,
    ): List<RecordTable> {
        val recordIds = tagWithRecords.filter { it.tagId == tagId }.map { it.recordId }
        return records.filter { it.booksId == booksId && it.id in recordIds }
            .sortedByDescending { it.recordTime }
            .drop(pageNum)
            .take(pageSize)
    }

    override suspend fun queryRecordByKeyword(
        booksId: Long,
        keyword: String,
        pageNum: Int,
        pageSize: Int,
    ): List<RecordTable> {
        return records.filter {
            it.booksId == booksId && (
                it.remark.contains(keyword) ||
                    it.amount.toString().contains(keyword) ||
                    it.charge.toString().contains(keyword) ||
                    it.concessions.toString().contains(keyword)
                )
        }
            .sortedByDescending { it.recordTime }
            .drop(pageNum)
            .take(pageSize)
    }

    override suspend fun queryByIds(ids: List<Long>): List<RecordTable> {
        return records.filter { it.id in ids }
    }

    override fun queryByTypeId(id: Long): List<RecordTable> {
        return records.filter { it.typeId == id }
    }

    override fun queryByTypeCategory(typeCategoryId: Int): List<RecordTable> {
        val typeIds = types.filter { it.typeCategory == typeCategoryId }.mapNotNull { it.id }
        return records.filter { it.typeId in typeIds }
    }

    override fun queryRelatedRecord(): List<RecordWithRelatedTable> {
        return relatedRecords.toList()
    }

    override fun queryRelatedRecordCountByID(id: Long): Int {
        return relatedRecords.count { it.relatedRecordId == id || it.recordId == id }
    }

    override suspend fun updateRecord(list: List<RecordTable>): Int {
        var count = 0
        list.forEach { updated ->
            val index = records.indexOfFirst { it.id == updated.id }
            if (index >= 0) {
                records[index] = updated
                count++
            }
        }
        return count
    }

    override suspend fun changeRecordTypeBeforeDeleteType(fromId: Long, toId: Long) {
        for (i in records.indices) {
            if (records[i].typeId == fromId) {
                records[i] = records[i].copy(typeId = toId)
            }
        }
    }

    override suspend fun getRelatedIdListById(id: Long): List<Long> {
        return relatedRecords.filter { it.recordId == id }.map { it.relatedRecordId }
    }

    override suspend fun getRecordIdListFromRelatedId(id: Long): List<Long> {
        return relatedRecords.filter { it.relatedRecordId == id }.map { it.recordId }
    }

    override fun getExpenditureRecordListAfterTime(
        booksId: Long,
        recordTime: Long,
        incomeCategory: Int,
    ): List<RecordTable> {
        val expenditureTypeIds = types
            .filter { it.typeCategory == RecordTypeCategoryEnum.EXPENDITURE.ordinal }
            .mapNotNull { it.id }
        return records.filter {
            it.booksId == booksId &&
                it.recordTime >= recordTime &&
                it.typeId in expenditureTypeIds
        }.sortedByDescending { it.recordTime }.take(50)
    }

    override fun getExpenditureReimburseRecordListAfterTime(
        booksId: Long,
        recordTime: Long,
    ): List<RecordTable> {
        return records.filter {
            it.booksId == booksId &&
                it.recordTime >= recordTime &&
                it.reimbursable == SWITCH_INT_ON
        }.sortedByDescending { it.recordTime }.take(50)
    }

    override fun getExpenditureRecordListByKeywordAfterTime(
        keyword: String,
        booksId: Long,
        recordTime: Long,
        incomeCategory: Int,
    ): List<RecordTable> {
        val expenditureTypeIds = types
            .filter { it.typeCategory == RecordTypeCategoryEnum.EXPENDITURE.ordinal }
            .mapNotNull { it.id }
        return records.filter {
            it.booksId == booksId &&
                it.recordTime >= recordTime &&
                it.typeId in expenditureTypeIds &&
                (it.remark.contains(keyword) || it.amount.toString().contains(keyword))
        }.sortedByDescending { it.recordTime }.take(50)
    }

    override fun getRecordCountByAssetIdAfterTime(assetId: Long, recordTime: Long): Int {
        return records.count { it.assetId == assetId && it.recordTime >= recordTime }
    }

    override fun getLastThreeMonthExpenditureReimburseRecordListByKeyword(
        keyword: String,
        booksId: Long,
        recordTime: Long,
    ): List<RecordTable> {
        return records.filter {
            it.booksId == booksId &&
                it.recordTime >= recordTime &&
                it.reimbursable == SWITCH_INT_ON &&
                (it.remark.contains(keyword) || it.amount.toString().contains(keyword))
        }.sortedByDescending { it.recordTime }.take(50)
    }

    override fun deleteWithAsset(assetId: Long) {
        records.removeAll { it.assetId == assetId || it.intoAssetId == assetId }
    }

    override fun deleteRelatedWithAsset(assetId: Long) {
        val recordIds = records
            .filter { it.assetId == assetId || it.intoAssetId == assetId }
            .mapNotNull { it.id }
        relatedRecords.removeAll { it.recordId in recordIds || it.relatedRecordId in recordIds }
    }

    override fun pagingQueryByBooksIdBetweenDate(
        booksId: Long,
        startDate: Long,
        endDate: Long,
    ): PagingSource<Int, RecordTable> {
        return object : PagingSource<Int, RecordTable>() {
            override suspend fun load(params: LoadParams<Int>): LoadResult<Int, RecordTable> {
                val data = records.filter {
                    it.booksId == booksId && it.recordTime >= startDate && it.recordTime < endDate
                }.sortedByDescending { it.recordTime }
                return LoadResult.Page(data = data, prevKey = null, nextKey = null)
            }

            override fun getRefreshKey(state: PagingState<Int, RecordTable>): Int? = null
        }
    }

    override suspend fun queryViewsBetweenDate(
        booksId: Long,
        startDate: Long,
        endDate: Long,
    ): List<RecordViewsRelation> {
        return records.filter {
            it.booksId == booksId && it.recordTime >= startDate && it.recordTime < endDate
        }.map { record ->
            val type = types.firstOrNull { it.id == record.typeId }
            RecordViewsRelation(
                id = record.id ?: -1L,
                typeCategory = type?.typeCategory ?: 0,
                typeName = "",
                typeIconResName = "",
                assetName = null,
                assetClassification = null,
                relatedAssetName = null,
                relatedAssetClassification = null,
                amount = record.amount,
                finalAmount = record.finalAmount,
                charges = record.charge,
                concessions = record.concessions,
                remark = record.remark,
                reimbursable = record.reimbursable,
                recordTime = record.recordTime,
            )
        }
    }

    override suspend fun queryImagesByRecordId(recordId: Long): List<ImageWithRelatedTable> {
        return images.filter { it.recordId == recordId }
    }

    /** 辅助方法：添加记录并自动分配 id */
    fun addRecord(record: RecordTable): RecordTable {
        val withId = if (record.id == null) record.copy(id = nextId++) else record
        records.add(withId)
        return withId
    }

    /** 类型简易数据，用于 Fake 中的类型查询 */
    data class FakeTypeEntry(
        val id: Long?,
        val parentId: Long,
        val typeCategory: Int,
    )

    /** 标签关联简易数据 */
    data class FakeTagWithRecordEntry(
        val recordId: Long,
        val tagId: Long,
    )
}
