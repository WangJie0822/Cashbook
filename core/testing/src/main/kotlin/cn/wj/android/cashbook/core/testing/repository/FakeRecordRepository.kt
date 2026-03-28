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

package cn.wj.android.cashbook.core.testing.repository

import androidx.paging.PagingData
import cn.wj.android.cashbook.core.data.repository.RecordRepository
import cn.wj.android.cashbook.core.database.table.RecordTable
import cn.wj.android.cashbook.core.model.model.ExportRecordModel
import cn.wj.android.cashbook.core.model.model.ImageModel
import cn.wj.android.cashbook.core.model.model.RecordModel
import cn.wj.android.cashbook.core.model.model.RecordViewSummaryModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf

class FakeRecordRepository : RecordRepository {

    private val records = mutableListOf<RecordModel>()
    private val relatedMap = mutableMapOf<Long, List<Long>>()
    private val relatedFromMap = mutableMapOf<Long, List<Long>>()
    private val imageMap = mutableMapOf<Long, List<ImageModel>>()
    private val _searchHistoryListData = MutableStateFlow<List<String>>(emptyList())

    /** 用于测试验证的记录 */
    var lastUpdatedRecord: RecordModel? = null
        private set
    var lastUpdatedTagIdList: List<Long> = emptyList()
        private set
    var lastUpdatedNeedRelated: Boolean = false
        private set
    var lastDeletedRecordId: Long = -1L
        private set

    override val searchHistoryListData: Flow<List<String>> = _searchHistoryListData

    fun addRecord(record: RecordModel) {
        records.add(record)
    }

    fun setRelatedIds(recordId: Long, relatedIds: List<Long>) {
        relatedMap[recordId] = relatedIds
    }

    fun setRelatedFromIds(recordId: Long, relatedFromIds: List<Long>) {
        relatedFromMap[recordId] = relatedFromIds
    }

    fun setImages(recordId: Long, images: List<ImageModel>) {
        imageMap[recordId] = images
    }

    override suspend fun queryById(recordId: Long): RecordModel? {
        return records.find { it.id == recordId }
    }

    override suspend fun queryByTypeId(id: Long): List<RecordModel> {
        return records.filter { it.typeId == id }
    }

    override suspend fun queryRelatedById(recordId: Long): List<RecordModel> {
        val relatedIds = relatedMap[recordId] ?: emptyList()
        return records.filter { it.id in relatedIds }
    }

    override suspend fun updateRecord(
        record: RecordModel,
        tagIdList: List<Long>,
        needRelated: Boolean,
        relatedRecordIdList: List<Long>,
        relatedImageList: List<ImageModel>,
    ) {
        lastUpdatedRecord = record
        lastUpdatedTagIdList = tagIdList
        lastUpdatedNeedRelated = needRelated
        val existing = records.indexOfFirst { it.id == record.id }
        if (existing >= 0) {
            records[existing] = record
        } else {
            records.add(record)
        }
    }

    override suspend fun deleteRecord(recordId: Long) {
        lastDeletedRecordId = recordId
        records.removeAll { it.id == recordId }
    }

    override suspend fun queryPagingRecordListByAssetId(
        assetId: Long,
        page: Int,
        pageSize: Int,
    ): List<RecordModel> {
        return records.filter { it.assetId == assetId }
            .drop(page * pageSize)
            .take(pageSize)
    }

    override suspend fun queryPagingRecordListByTypeId(
        typeId: Long,
        page: Int,
        pageSize: Int,
        includeChildTypes: Boolean,
    ): List<RecordModel> {
        return records.filter { it.typeId == typeId }
            .drop(page * pageSize)
            .take(pageSize)
    }

    override suspend fun queryPagingRecordListByTypeIdBetweenDate(
        typeId: Long,
        dateRange: String,
        page: Int,
        pageSize: Int,
        includeChildTypes: Boolean,
    ): List<RecordModel> {
        return records.filter { it.typeId == typeId }
            .drop(page * pageSize)
            .take(pageSize)
    }

    override suspend fun queryPagingRecordListByTagId(
        tagId: Long,
        page: Int,
        pageSize: Int,
    ): List<RecordModel> {
        return records.drop(page * pageSize).take(pageSize)
    }

    override suspend fun queryPagingRecordListByKeyword(
        keyword: String,
        page: Int,
        pageSize: Int,
    ): List<RecordModel> {
        return records.filter { it.remark.contains(keyword) }
            .drop(page * pageSize)
            .take(pageSize)
    }

    override suspend fun queryRecordListBetweenDate(
        from: Long,
        to: Long,
    ): List<RecordModel> {
        return records
    }

    override fun queryRecordByYearMonth(
        year: String,
        month: String,
    ): Flow<List<RecordModel>> {
        return MutableStateFlow(records.toList())
    }

    override fun getRecordPagingData(
        startDate: Long,
        endDate: Long,
    ): Flow<PagingData<RecordModel>> {
        return flowOf(PagingData.from(records.toList()))
    }

    private val _summaryData = MutableStateFlow<List<RecordViewSummaryModel>>(emptyList())

    fun setSummaryData(data: List<RecordViewSummaryModel>) {
        _summaryData.value = data
    }

    override fun queryRecordViewSummariesFlow(
        startDate: Long,
        endDate: Long,
    ): Flow<List<RecordViewSummaryModel>> {
        return _summaryData
    }

    override suspend fun getDefaultRecord(typeId: Long): RecordModel {
        return RecordModel(
            id = -1L,
            booksId = 1L,
            typeId = typeId,
            assetId = -1L,
            relatedAssetId = -1L,
            amount = 0L,
            finalAmount = 0L,
            charges = 0L,
            concessions = 0L,
            remark = "",
            reimbursable = false,
            recordTime = 1704067200000L, // 2024-01-01 00:00:00 UTC+8
        )
    }

    override suspend fun changeRecordTypeBeforeDeleteType(fromId: Long, toId: Long) {
        // no-op
    }

    override suspend fun getRelatedIdListById(id: Long): List<Long> {
        return relatedMap[id] ?: emptyList()
    }

    override suspend fun getRecordIdListFromRelatedId(id: Long): List<Long> {
        return relatedFromMap[id] ?: emptyList()
    }

    override suspend fun getLastThreeMonthReimbursableRecordList(): List<RecordModel> {
        return records.filter { it.reimbursable }
    }

    override suspend fun getLastThreeMonthRefundableRecordList(): List<RecordModel> {
        return records
    }

    override suspend fun getLastThreeMonthReimbursableRecordListByKeyword(
        keyword: String,
    ): List<RecordModel> {
        return records.filter { it.reimbursable && it.remark.contains(keyword) }
    }

    override suspend fun getLastThreeMonthRefundableRecordListByKeyword(
        keyword: String,
    ): List<RecordModel> {
        return records.filter { it.remark.contains(keyword) }
    }

    override suspend fun getLastThreeMonthRecordCountByAssetId(assetId: Long): Int {
        return records.count { it.assetId == assetId }
    }

    override suspend fun deleteRecordsWithAsset(assetId: Long) {
        records.removeAll { it.assetId == assetId }
    }

    override suspend fun deleteRecordRelatedWithAsset(assetId: Long) {
        // no-op
    }

    override suspend fun addSearchHistory(keyword: String) {
        if (keyword.isNotBlank()) {
            val current = _searchHistoryListData.value.toMutableList()
            current.remove(keyword)
            current.add(0, keyword)
            _searchHistoryListData.value = current.take(10)
        }
    }

    override suspend fun clearSearchHistory() {
        _searchHistoryListData.value = emptyList()
    }

    override suspend fun migrateAfter9To10() {
        // no-op
    }

    override suspend fun queryRelatedRecordCountById(id: Long): Int {
        return relatedMap[id]?.size ?: 0
    }

    override suspend fun queryImagesByRecordId(id: Long): List<ImageModel> {
        return imageMap[id] ?: emptyList()
    }

    override suspend fun queryByWechatTransactionId(
        booksId: Long,
        transactionId: String,
    ): List<RecordModel> {
        return emptyList()
    }

    override suspend fun queryByTimeAndAmount(
        booksId: Long,
        startTime: Long,
        endTime: Long,
        amount: Double,
    ): List<RecordModel> {
        return emptyList()
    }

    override suspend fun batchImportRecords(records: List<RecordTable>): List<Long> {
        return emptyList()
    }

    override suspend fun queryExportRecords(
        booksId: Long,
        startDate: Long,
        endDate: Long,
    ): List<ExportRecordModel> {
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
            .minByOrNull { it.recordTime }
            ?.recordTime
    }
}
