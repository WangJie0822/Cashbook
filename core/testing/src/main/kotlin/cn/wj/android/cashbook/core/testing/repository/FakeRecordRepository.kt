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
import cn.wj.android.cashbook.core.common.ext.toAmountCent
import cn.wj.android.cashbook.core.data.repository.RecordRepository
import cn.wj.android.cashbook.core.database.table.RecordTable
import cn.wj.android.cashbook.core.model.model.ExportRecordModel
import cn.wj.android.cashbook.core.model.model.ImageModel
import cn.wj.android.cashbook.core.model.model.RecordModel
import cn.wj.android.cashbook.core.model.model.RecordViewSummaryModel
import cn.wj.android.cashbook.core.model.model.RecordViewsModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf

class FakeRecordRepository : RecordRepository {

    private val records = mutableListOf<RecordModel>()
    private val relatedMap = mutableMapOf<Long, List<Long>>()
    private val relatedFromMap = mutableMapOf<Long, List<Long>>()
    private val imageMap = mutableMapOf<Long, List<ImageModel>>()
    private val tagRecordIds = mutableMapOf<Long, MutableSet<Long>>()
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

    /** 最近一次 [updateRecordReimbursed] 入参，供测试断言 */
    var lastReimbursedRecordId: Long = -1L
        private set
    var lastReimbursedValue: Boolean? = null
        private set

    /** 可配置的导出记录列表 */
    var exportRecordsList: List<ExportRecordModel> = emptyList()

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

    /** 建立标签-记录关联（测试辅助，对应 db_tag_with_record） */
    fun addTagRelation(tagId: Long, recordId: Long) {
        tagRecordIds.getOrPut(tagId) { mutableSetOf() }.add(recordId)
    }

    private fun recordsOfTag(tagId: Long): List<RecordModel> {
        val ids = tagRecordIds[tagId].orEmpty()
        return records.filter { it.id in ids }
    }

    override suspend fun queryById(recordId: Long): RecordModel? {
        queryByIdCount++
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

    /** [backfillImagesToFiles] 调用次数，供首屏 gate 测试断言 */
    var backfillImagesToFilesCount = 0
        private set

    /** 非 null 时 [backfillImagesToFiles] 在 count++ 后抛此异常（测试后台维护失败隔离路径） */
    var backfillThrowable: Throwable? = null

    override suspend fun backfillImagesToFiles() {
        backfillImagesToFilesCount++
        backfillThrowable?.let { throw it }
    }

    /** [cleanupOrphanImageFiles] 调用次数，供首屏 gate 测试断言 */
    var cleanupOrphanImageFilesCount = 0
        private set

    /** 非 null 时 [cleanupOrphanImageFiles] 在 count++ 后抛此异常 */
    var orphanThrowable: Throwable? = null

    override suspend fun cleanupOrphanImageFiles(graceWindowMs: Long) {
        cleanupOrphanImageFilesCount++
        orphanThrowable?.let { throw it }
    }

    /** [compactDatabaseIfNeeded] 调用次数，供首屏 gate 测试断言 */
    var compactDatabaseIfNeededCount = 0
        private set

    /** 非 null 时 [compactDatabaseIfNeeded] 在 count++ 后抛此异常 */
    var compactThrowable: Throwable? = null

    override suspend fun compactDatabaseIfNeeded() {
        compactDatabaseIfNeededCount++
        compactThrowable?.let { throw it }
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

    override suspend fun queryPagingRecordListByTypeIdInRange(
        typeId: Long,
        startDate: Long,
        endDate: Long,
        page: Int,
        pageSize: Int,
        includeChildTypes: Boolean,
    ): List<RecordModel> {
        // 忠实复刻半开区间 [start,end)（Fake 不建模父子类型，按精确 typeId 过滤）
        return records.filter { it.typeId == typeId && it.recordTime >= startDate && it.recordTime < endDate }
            .sortedByDescending { it.recordTime }
            .drop(page * pageSize)
            .take(pageSize)
    }

    override suspend fun queryPagingRecordListByAssetIdBetweenDate(
        assetId: Long,
        startDate: Long,
        endDate: Long,
        page: Int,
        pageSize: Int,
    ): List<RecordModel> {
        return records.filter {
            (it.assetId == assetId || it.relatedAssetId == assetId) &&
                it.recordTime >= startDate && it.recordTime < endDate
        }
            .sortedByDescending { it.recordTime }
            .drop(page * pageSize)
            .take(pageSize)
    }

    override fun queryAssetRecordsBetweenDateFlow(
        assetId: Long,
        startDate: Long,
        endDate: Long,
    ): Flow<List<RecordModel>> = MutableStateFlow(
        records.filter {
            (it.assetId == assetId || it.relatedAssetId == assetId) &&
                it.recordTime >= startDate && it.recordTime < endDate
        },
    )

    override suspend fun queryPagingRecordListByTagId(
        tagId: Long,
        page: Int,
        pageSize: Int,
    ): List<RecordModel> {
        return recordsOfTag(tagId)
            .sortedByDescending { it.recordTime }
            .drop(page * pageSize)
            .take(pageSize)
    }

    override suspend fun queryPagingRecordListByTagIdBetween(
        tagId: Long,
        startDate: Long,
        endDate: Long,
        page: Int,
        pageSize: Int,
    ): List<RecordModel> {
        return recordsOfTag(tagId)
            .filter { it.recordTime >= startDate && it.recordTime < endDate }
            .sortedByDescending { it.recordTime }
            .drop(page * pageSize)
            .take(pageSize)
    }

    override suspend fun queryRecordsByTypeIdInRange(
        typeId: Long,
        startDate: Long,
        endDate: Long,
        includeChildTypes: Boolean,
    ): List<RecordModel> {
        // Fake 不建模父子类型，按精确 typeId + 半开区间过滤（测试用直接 typeId）
        return records.filter { it.typeId == typeId && it.recordTime >= startDate && it.recordTime < endDate }
    }

    override suspend fun queryRecordsByTagIdInRange(
        tagId: Long,
        startDate: Long,
        endDate: Long,
    ): List<RecordModel> {
        return recordsOfTag(tagId).filter { it.recordTime >= startDate && it.recordTime < endDate }
    }

    override suspend fun queryPagingRecordListByKeyword(
        keyword: String,
        page: Int,
        pageSize: Int,
    ): List<RecordModel> {
        val amountCent = if (keyword.toBigDecimalOrNull() != null) keyword.toAmountCent() else -1L
        return records.filter {
            it.remark.contains(keyword) ||
                (amountCent != -1L && (it.amount == amountCent || it.finalAmount == amountCent))
        }
            .drop(page * pageSize)
            .take(pageSize)
    }

    override suspend fun queryRecordListBetweenDate(
        from: Long,
        to: Long,
    ): List<RecordModel> {
        // 忠实复刻真实 SQL（RecordDao.queryByBooksIdBetweenDate）日期语义：
        // record_time >= from AND record_time < to（左闭右开）。
        // 注：真实实现另按 currentBookId 过滤，Fake 默认单账本不模拟账本维度。
        return records.filter { it.recordTime in from until to }
    }

    override fun queryRecordByYearMonth(
        year: String,
        month: String,
    ): Flow<List<RecordModel>> {
        return MutableStateFlow(records.toList())
    }

    /** 首页分页视图桩（测试按需 recordViews.add(...) 注入；接口现返回 @Relation 视图 RecordViewsModel） */
    val recordViews = mutableListOf<RecordViewsModel>()

    override fun getRecordPagingData(
        startDate: Long,
        endDate: Long,
    ): Flow<PagingData<RecordViewsModel>> {
        return flowOf(PagingData.from(recordViews.toList()))
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

    override suspend fun getReimbursableUnrelatedRecordList(): List<RecordModel> {
        // 忠实桩：可报销 + 未手动标记已报销 + 双向（relatedMap 吸收者侧 / relatedFromMap 被吸收侧）都空才算未关联
        return records.filter { record ->
            record.reimbursable &&
                !record.reimbursed &&
                relatedMap[record.id].isNullOrEmpty() &&
                relatedFromMap[record.id].isNullOrEmpty()
        }
    }

    override suspend fun updateRecordReimbursed(recordId: Long, reimbursed: Boolean) {
        lastReimbursedRecordId = recordId
        lastReimbursedValue = reimbursed
        val index = records.indexOfFirst { it.id == recordId }
        if (index >= 0) {
            records[index] = records[index].copy(reimbursed = reimbursed)
        }
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

    /** db9To10 迁移调用次数（供 gate 测试断言） */
    var migrateAfter9To10Count: Int = 0

    /** 非 null 时 [migrateAfter9To10] 挂起在此 deferred；默认 null=不挂起立即返回（保持 32 个复用测试不破） */
    var migrateSuspendGate: CompletableDeferred<Unit>? = null

    /** [migrateAfter9To10] 进入信号：入口 complete，供测试确认协程已停在挂起点 */
    var migrateStartedSignal: CompletableDeferred<Unit>? = null

    /** 非 null 时 [migrateAfter9To10] 在 count++ + gate 放行后抛此异常（测试迁移失败逃逸全局 handler 路径） */
    var migrateThrowable: Throwable? = null

    override suspend fun migrateAfter9To10() {
        migrateAfter9To10Count++
        migrateStartedSignal?.complete(Unit)
        migrateSuspendGate?.await()
        migrateThrowable?.let { throw it }
    }

    /** 净自付重算调用次数（供 gate 触发测试断言） */
    var recalculateAllFinalAmountCount: Int = 0

    /** 非 null 时 [recalculateAllFinalAmount] 挂起在此 deferred；默认 null=不挂起立即返回 */
    var recalcSuspendGate: CompletableDeferred<Unit>? = null

    /** [recalculateAllFinalAmount] 进入信号：入口 complete，供测试确认协程已停在挂起点 */
    var recalcStartedSignal: CompletableDeferred<Unit>? = null

    /** 非 null 时 [recalculateAllFinalAmount] 在进入信号 + 放行后抛此异常（测试后台重算失败路径） */
    var recalcThrowable: Throwable? = null

    override suspend fun recalculateAllFinalAmount() {
        recalculateAllFinalAmountCount++
        recalcStartedSignal?.complete(Unit)
        recalcSuspendGate?.await()
        recalcThrowable?.let { throw it }
    }

    override suspend fun queryRelatedRecordCountById(id: Long): Int {
        return relatedMap[id]?.size ?: 0
    }

    override suspend fun queryImagesByRecordId(id: Long): List<ImageModel> {
        queryImagesByRecordIdCount++
        return imageMap[id] ?: emptyList()
    }

    /** 逐条 [queryImagesByRecordId] 的调用次数，供测试断言批量路径未触发 N+1 */
    var queryImagesByRecordIdCount: Int = 0
        private set

    /** 逐条 [queryById] 的调用次数，供测试断言批量路径未触发 N+1 */
    var queryByIdCount: Int = 0
        private set

    /** 批量 [queryByIds] 的调用次数，供测试断言批量路径被使用 */
    var queryByIdsCount: Int = 0
        private set

    /** 批量 [queryImagesByRecordIds] 的调用次数，供测试断言批量路径被使用 */
    var queryImagesByRecordIdsCount: Int = 0
        private set

    override suspend fun queryByIds(ids: List<Long>): List<RecordModel> {
        queryByIdsCount++
        return records.filter { it.id in ids }
    }

    override suspend fun queryImagesByRecordIds(ids: List<Long>): Map<Long, List<ImageModel>> {
        queryImagesByRecordIdsCount++
        return ids.mapNotNull { id ->
            imageMap[id]?.let { id to it }
        }.toMap()
    }

    override suspend fun getRelatedIdMapByIds(ids: List<Long>): Map<Long, List<Long>> {
        return ids.mapNotNull { id ->
            relatedMap[id]?.takeIf { it.isNotEmpty() }?.let { id to it }
        }.toMap()
    }

    override suspend fun getRecordIdFromRelatedMapByIds(ids: List<Long>): Map<Long, List<Long>> {
        return ids.mapNotNull { id ->
            relatedFromMap[id]?.takeIf { it.isNotEmpty() }?.let { id to it }
        }.toMap()
    }

    override suspend fun queryByWechatTransactionId(
        booksId: Long,
        transactionId: String,
    ): List<RecordModel> {
        // 忠实复刻 DAO 的 remark LIKE '%[微信单号:<id>]%'（方括号定界，同 booksId）语义，
        // 使 EXACT 去重路径可被单测覆盖（此前为 emptyList 桩，精确单号匹配从未被测试覆盖）。
        // 注意:真实写入(RecordImportViewModel)与 DAO SQL 均用方括号定界,不可用裸 contains。
        if (transactionId.isBlank()) return emptyList()
        val marker = "[微信单号:$transactionId]"
        return records.filter { it.booksId == booksId && it.remark.contains(marker) }
    }

    override suspend fun queryByTimeAndAmount(
        booksId: Long,
        startTime: Long,
        endTime: Long,
        amount: Long,
    ): List<RecordModel> {
        // 复刻真实 DAO 语义：同账本 + 时间区间 [startTime,endTime] + 金额（分，Long）精确相等
        return records.filter {
            it.booksId == booksId &&
                it.recordTime in startTime..endTime &&
                it.amount == amount
        }
    }

    /** 最近一次 [batchImportRecords] 传入的记录（供导入路径测试断言 remark/金额/finalAmount 等） */
    var lastImportedRecords: List<RecordTable> = emptyList()
        private set

    override suspend fun batchImportRecords(records: List<RecordTable>): List<Long> {
        // 忠实复刻真实 DAO：插入 N 条返回 N 个行 id（旧空桩返 emptyList 会致 Done.imported 恒 0 假阳性）
        lastImportedRecords = records
        return records.indices.map { (it + 1).toLong() }
    }

    override suspend fun queryExportRecords(
        booksId: Long,
        startDate: Long,
        endDate: Long,
    ): List<ExportRecordModel> {
        return exportRecordsList
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
