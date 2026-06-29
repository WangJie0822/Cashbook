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
import cn.wj.android.cashbook.core.common.SWITCH_INT_OFF
import cn.wj.android.cashbook.core.common.SWITCH_INT_ON
import cn.wj.android.cashbook.core.database.dao.RecordDao
import cn.wj.android.cashbook.core.database.relation.ExportRecordRelation
import cn.wj.android.cashbook.core.database.relation.RecordViewsRelation
import cn.wj.android.cashbook.core.database.table.AssetTable
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

    /** 资产数据列表，用于 queryLastUsedAssetId 的资产归属/可见性子查询复刻 */
    val assets = mutableListOf<AssetTable>()

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

    override suspend fun queryRecordByAssetIdBetween(
        booksId: Long,
        assetId: Long,
        startDate: Long,
        endDate: Long,
        pageNum: Int,
        pageSize: Int,
    ): List<RecordTable> {
        return records.filter {
            it.booksId == booksId &&
                (it.assetId == assetId || it.intoAssetId == assetId) &&
                it.recordTime >= startDate && it.recordTime < endDate
        }
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

    override suspend fun queryRecordByTypeIdExact(
        booksId: Long,
        typeId: Long,
        pageNum: Int,
        pageSize: Int,
    ): List<RecordTable> {
        return records.filter {
            it.booksId == booksId && it.typeId == typeId
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

    override suspend fun queryRecordByTypeIdExactBetween(
        booksId: Long,
        typeId: Long,
        startDate: Long,
        endDate: Long,
        pageNum: Int,
        pageSize: Int,
    ): List<RecordTable> {
        return records.filter {
            it.booksId == booksId &&
                it.recordTime >= startDate &&
                it.recordTime < endDate &&
                it.typeId == typeId
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

    override suspend fun queryRecordByTagIdBetween(
        booksId: Long,
        tagId: Long,
        startDate: Long,
        endDate: Long,
        pageNum: Int,
        pageSize: Int,
    ): List<RecordTable> {
        val recordIds = tagWithRecords.filter { it.tagId == tagId }.map { it.recordId }
        return records.filter {
            it.booksId == booksId &&
                it.id in recordIds &&
                it.recordTime >= startDate &&
                it.recordTime < endDate
        }
            .sortedByDescending { it.recordTime }
            .drop(pageNum)
            .take(pageSize)
    }

    override suspend fun queryRecordByKeyword(
        booksId: Long,
        keyword: String,
        amountCent: Long,
        pageNum: Int,
        pageSize: Int,
    ): List<RecordTable> {
        return records.filter {
            it.booksId == booksId && (
                it.remark.contains(keyword) ||
                    (amountCent != -1L && (it.amount == amountCent || it.finalAmount == amountCent))
                )
        }
            .sortedByDescending { it.recordTime }
            .drop(pageNum)
            .take(pageSize)
    }

    override suspend fun queryByIds(ids: List<Long>): List<RecordTable> {
        return records.filter { it.id in ids }
    }

    override suspend fun queryByTypeId(id: Long): List<RecordTable> {
        return records.filter { it.typeId == id }
    }

    override suspend fun queryByTypeCategory(typeCategoryId: Int): List<RecordTable> {
        val typeIds = types.filter { it.typeCategory == typeCategoryId }.mapNotNull { it.id }
        return records.filter { it.typeId in typeIds }
    }

    override suspend fun queryRelatedRecordCountByID(id: Long): Int {
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

    override suspend fun updateRecordReimbursed(recordId: Long, booksId: Long, reimbursed: Int) {
        val index = records.indexOfFirst { it.id == recordId && it.booksId == booksId }
        if (index >= 0) {
            records[index] = records[index].copy(reimbursed = reimbursed)
        }
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

    override suspend fun getExpenditureRecordListAfterTime(
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

    override suspend fun getExpenditureReimburseRecordListAfterTime(
        booksId: Long,
        recordTime: Long,
    ): List<RecordTable> {
        return records.filter {
            it.booksId == booksId &&
                it.recordTime >= recordTime &&
                it.reimbursable == SWITCH_INT_ON &&
                it.reimbursed == SWITCH_INT_OFF
        }.sortedByDescending { it.recordTime }.take(50)
    }

    override suspend fun queryReimbursableUnrelated(
        booksId: Long,
        expenditureCategory: Int,
    ): List<RecordTable> {
        val expenditureTypeIds = types
            .filter { it.typeCategory == expenditureCategory }
            .mapNotNull { it.id }
        return records.filter { record ->
            record.booksId == booksId &&
                record.reimbursable == SWITCH_INT_ON &&
                record.reimbursed == SWITCH_INT_OFF &&
                record.typeId in expenditureTypeIds &&
                relatedRecords.none { it.recordId == record.id || it.relatedRecordId == record.id }
        }.sortedByDescending { it.recordTime }
    }

    override suspend fun getExpenditureRecordListByKeywordAfterTime(
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

    override suspend fun getRecordCountByAssetIdAfterTime(assetId: Long, recordTime: Long): Int {
        return records.count { it.assetId == assetId && it.recordTime >= recordTime }
    }

    override suspend fun getLastThreeMonthExpenditureReimburseRecordListByKeyword(
        keyword: String,
        booksId: Long,
        recordTime: Long,
    ): List<RecordTable> {
        return records.filter {
            it.booksId == booksId &&
                it.recordTime >= recordTime &&
                it.reimbursable == SWITCH_INT_ON &&
                it.reimbursed == SWITCH_INT_OFF &&
                (it.remark.contains(keyword) || it.amount.toString().contains(keyword))
        }.sortedByDescending { it.recordTime }.take(50)
    }

    override suspend fun deleteWithAsset(assetId: Long) {
        records.removeAll { it.assetId == assetId || it.intoAssetId == assetId }
    }

    override suspend fun deleteRelatedWithAsset(assetId: Long) {
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
                typeId = record.typeId,
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

    override suspend fun queryAllImages(): List<ImageWithRelatedTable> = images.toList()

    override suspend fun queryAllImagePaths(): List<String> = images.map { it.path }

    // 忠实复刻真实 SQL：先按谓词筛 record id 集（含 into_asset_id 转账入账侧），再投影命中记录的图片 path
    override suspend fun queryImagePathsByAssetId(assetId: Long): List<String> {
        val ids = records.filter { it.assetId == assetId || it.intoAssetId == assetId }
            .mapNotNull { it.id }.toSet()
        return images.filter { it.recordId in ids }.map { it.path }
    }

    override suspend fun queryImagePathsByBookId(bookId: Long): List<String> {
        val ids = records.filter { it.booksId == bookId }.mapNotNull { it.id }.toSet()
        return images.filter { it.recordId in ids }.map { it.path }
    }

    override suspend fun queryImagePathsByRecordId(recordId: Long): List<String> =
        images.filter { it.recordId == recordId }.map { it.path }

    override suspend fun queryUnmigratedImageIds(): List<Long> =
        images.filter { it.bytes.isNotEmpty() }.mapNotNull { it.id }

    override suspend fun queryImageBytesById(id: Long): ByteArray? =
        images.firstOrNull { it.id == id }?.bytes

    override suspend fun updateImagePathAndBytes(id: Long, path: String, bytes: ByteArray) {
        val idx = images.indexOfFirst { it.id == id }
        if (idx >= 0) images[idx] = images[idx].copy(path = path, bytes = bytes)
    }

    override suspend fun queryImagesByRecordIds(recordIds: List<Long>): List<ImageWithRelatedTable> {
        return images.filter { it.recordId in recordIds }
    }

    override suspend fun queryRelatedByRecordIds(recordIds: List<Long>): List<RecordWithRelatedTable> {
        return relatedRecords.filter { it.recordId in recordIds }
    }

    override suspend fun queryRelatedByRelatedRecordIds(recordIds: List<Long>): List<RecordWithRelatedTable> {
        return relatedRecords.filter { it.relatedRecordId in recordIds }
    }

    override suspend fun queryByWechatTransactionId(
        booksId: Long,
        transactionId: String,
    ): List<RecordTable> {
        return records.filter {
            it.booksId == booksId && it.remark.contains(transactionId)
        }
    }

    override suspend fun queryByTimeAndAmount(
        booksId: Long,
        startTime: Long,
        endTime: Long,
        amount: Long,
    ): List<RecordTable> {
        return records.filter {
            it.booksId == booksId &&
                it.recordTime in startTime..endTime &&
                // amount 列与入参都已是分（Long），按分精确相等比较
                it.amount == amount
        }
    }

    override suspend fun queryExportRecords(
        booksId: Long,
        startDate: Long,
        endDate: Long,
    ): List<ExportRecordRelation> {
        // 简化实现：返回空列表（测试中不依赖此方法）
        return emptyList()
    }

    override suspend fun countExportRecords(
        booksId: Long,
        startDate: Long,
        endDate: Long,
    ): Int {
        return 0
    }

    override suspend fun queryEarliestRecordTime(booksId: Long): Long? {
        return records.filter { it.booksId == booksId }
            .minOfOrNull { it.recordTime }
    }

    /** 辅助方法：添加记录并自动分配 id */
    fun addRecord(record: RecordTable): RecordTable {
        val withId = if (record.id == null) record.copy(id = nextId++) else record
        records.add(withId)
        return withId
    }

    /** 辅助方法：添加资产 */
    fun addAsset(asset: AssetTable) {
        assets.add(asset)
    }

    override suspend fun queryLastUsedAssetId(booksId: Long): Long? {
        // 忠实复刻真实 SQL：asset_id IN（本账本可见在册资产）+ ORDER BY id DESC LIMIT 1
        val visibleAssetIds = assets
            .filter { it.booksId == booksId && it.invisible == SWITCH_INT_OFF }
            .mapNotNull { it.id }
            .toSet()
        return records
            .filter { it.booksId == booksId && it.assetId in visibleAssetIds }
            .maxByOrNull { it.id ?: Long.MIN_VALUE }
            ?.assetId
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
