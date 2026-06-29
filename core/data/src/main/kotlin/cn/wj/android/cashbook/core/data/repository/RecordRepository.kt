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

package cn.wj.android.cashbook.core.data.repository

import androidx.paging.PagingData
import cn.wj.android.cashbook.core.common.SWITCH_INT_OFF
import cn.wj.android.cashbook.core.common.SWITCH_INT_ON
import cn.wj.android.cashbook.core.database.relation.RecordViewsRelation
import cn.wj.android.cashbook.core.database.table.ImageWithRelatedTable
import cn.wj.android.cashbook.core.database.table.RecordTable
import cn.wj.android.cashbook.core.model.model.ExportRecordModel
import cn.wj.android.cashbook.core.model.model.ImageModel
import cn.wj.android.cashbook.core.model.model.RecordModel
import cn.wj.android.cashbook.core.model.model.RecordViewSummaryModel
import kotlinx.coroutines.flow.Flow

interface RecordRepository {

    val searchHistoryListData: Flow<List<String>>

    suspend fun queryById(recordId: Long): RecordModel?

    suspend fun queryByTypeId(id: Long): List<RecordModel>

    suspend fun queryRelatedById(recordId: Long): List<RecordModel>

    suspend fun updateRecord(
        record: RecordModel,
        tagIdList: List<Long>,
        needRelated: Boolean,
        relatedRecordIdList: List<Long>,
        relatedImageList: List<ImageModel>,
    )

    suspend fun deleteRecord(recordId: Long)

    suspend fun queryPagingRecordListByAssetId(
        assetId: Long,
        page: Int,
        pageSize: Int,
    ): List<RecordModel>

    suspend fun queryPagingRecordListByTypeId(
        typeId: Long,
        page: Int,
        pageSize: Int,
        includeChildTypes: Boolean = true,
    ): List<RecordModel>

    /** 类型 [typeId]（按 [includeChildTypes]）在 [[startDate], [endDate]) 半开区间的第 [page] 页 [pageSize] 条（分页，按时间倒序） */
    suspend fun queryPagingRecordListByTypeIdInRange(
        typeId: Long,
        startDate: Long,
        endDate: Long,
        page: Int,
        pageSize: Int,
        includeChildTypes: Boolean = true,
    ): List<RecordModel>

    /** 资产 [assetId] 在 [startDate,endDate) 的分页记录（含转入） */
    suspend fun queryPagingRecordListByAssetIdBetweenDate(
        assetId: Long,
        startDate: Long,
        endDate: Long,
        page: Int,
        pageSize: Int,
    ): List<RecordModel>

    /** 资产 [assetId] 在 [startDate,endDate) 的全量记录（用于余额口径汇总），响应式 */
    fun queryAssetRecordsBetweenDateFlow(
        assetId: Long,
        startDate: Long,
        endDate: Long,
    ): Flow<List<RecordModel>>

    suspend fun queryPagingRecordListByTagId(
        tagId: Long,
        page: Int,
        pageSize: Int,
    ): List<RecordModel>

    /** 标签 [tagId] 在 [[startDate], [endDate]) 的分页记录 */
    suspend fun queryPagingRecordListByTagIdBetween(
        tagId: Long,
        startDate: Long,
        endDate: Long,
        page: Int,
        pageSize: Int,
    ): List<RecordModel>

    /** 类型 [typeId]（按 [includeChildTypes]）在 [[startDate], [endDate]) 的全量记录（汇总用，非分页） */
    suspend fun queryRecordsByTypeIdInRange(
        typeId: Long,
        startDate: Long,
        endDate: Long,
        includeChildTypes: Boolean,
    ): List<RecordModel>

    /** 标签 [tagId] 在 [[startDate], [endDate]) 的全量记录（汇总用，非分页） */
    suspend fun queryRecordsByTagIdInRange(
        tagId: Long,
        startDate: Long,
        endDate: Long,
    ): List<RecordModel>

    suspend fun queryPagingRecordListByKeyword(
        keyword: String,
        page: Int,
        pageSize: Int,
    ): List<RecordModel>

    suspend fun queryRecordListBetweenDate(from: Long, to: Long): List<RecordModel>

    fun queryRecordByYearMonth(year: String, month: String): Flow<List<RecordModel>>

    /** 获取分页记录数据 */
    fun getRecordPagingData(startDate: Long, endDate: Long): Flow<PagingData<RecordModel>>

    /** 获取轻量记录汇总数据（响应式，数据变更时重新发射） */
    fun queryRecordViewSummariesFlow(startDate: Long, endDate: Long): Flow<List<RecordViewSummaryModel>>

    suspend fun getDefaultRecord(typeId: Long): RecordModel

    suspend fun changeRecordTypeBeforeDeleteType(fromId: Long, toId: Long)

    suspend fun getRelatedIdListById(id: Long): List<Long>

    suspend fun getRecordIdListFromRelatedId(id: Long): List<Long>

    suspend fun getLastThreeMonthReimbursableRecordList(): List<RecordModel>

    /** 当前账本全部「可报销且未关联任何报销/退款款」的支出记录（待报销管理界面用） */
    suspend fun getReimbursableUnrelatedRecordList(): List<RecordModel>

    /** 手动设置/清除「已报销」标记（按当前账本守护，写后 bump recordDataVersion） */
    suspend fun updateRecordReimbursed(recordId: Long, reimbursed: Boolean)

    suspend fun getLastThreeMonthRefundableRecordList(): List<RecordModel>

    suspend fun getLastThreeMonthReimbursableRecordListByKeyword(keyword: String): List<RecordModel>

    suspend fun getLastThreeMonthRefundableRecordListByKeyword(keyword: String): List<RecordModel>

    suspend fun getLastThreeMonthRecordCountByAssetId(assetId: Long): Int

    suspend fun deleteRecordsWithAsset(assetId: Long)

    suspend fun deleteRecordRelatedWithAsset(assetId: Long)

    suspend fun addSearchHistory(keyword: String)

    suspend fun clearSearchHistory()

    suspend fun migrateAfter9To10()

    /** 全表净自付重算（迁移 gate / 备份恢复复用），完成后置 finalAmountNetRecalcDone 标记 */
    suspend fun recalculateAllFinalAmount()

    suspend fun queryRelatedRecordCountById(id: Long): Int

    suspend fun queryImagesByRecordId(id: Long): List<ImageModel>

    /**
     * 批量按 id 查询记录（IN 查询），用于批量转换时解析关联记录，消除逐条 [queryById] 的 N+1。
     */
    suspend fun queryByIds(ids: List<Long>): List<RecordModel>

    /**
     * 批量查询多条记录的图片（IN 查询），消除逐条 [queryImagesByRecordId] 的 N+1。
     * @return recordId -> 图片列表 的映射；无图片的记录不在结果中（调用方按需兜底空列表）
     */
    suspend fun queryImagesByRecordIds(ids: List<Long>): Map<Long, List<ImageModel>>

    /** 存量图片 BLOB → 文件系统 backfill（逐行幂等，崩溃可重入；成功后置位迁移标志） */
    suspend fun backfillImagesToFiles()

    /**
     * 清理 record_images 目录下不被 DB 引用的孤儿图片文件（grace window 保护新写文件）。
     * 内部按 7 天节流：距上次扫描未达窗口直接返回（删资产/账本/编辑路径已各自删文件，本扫描退为纯兜底）。
     */
    suspend fun cleanupOrphanImageFiles(graceWindowMs: Long = 60_000L)

    /** backfill 完成后一次性 live DB VACUUM 回收空闲页（C-robust：StatFs 预检 + 仅真成功置位、失败下次重试） */
    suspend fun compactDatabaseIfNeeded()

    /**
     * 批量查询「收入侧」关联关系：返回 recordId -> 关联记录 id 列表，
     * 等价于对每个 id 调用 [getRelatedIdListById] 后聚合，消除 N+1。
     */
    suspend fun getRelatedIdMapByIds(ids: List<Long>): Map<Long, List<Long>>

    /**
     * 批量查询「支出侧」关联关系：返回 recordId -> 命中其为 related 的记录 id 列表，
     * 等价于对每个 id 调用 [getRecordIdListFromRelatedId] 后聚合，消除 N+1。
     */
    suspend fun getRecordIdFromRelatedMapByIds(ids: List<Long>): Map<Long, List<Long>>

    /**
     * 查询指定账本中是否存在包含微信交易单号的记录
     */
    suspend fun queryByWechatTransactionId(booksId: Long, transactionId: String): List<RecordModel>

    /**
     * 查询指定账本中指定时间范围和金额的记录（用于模糊去重）
     */
    suspend fun queryByTimeAndAmount(
        booksId: Long,
        startTime: Long,
        endTime: Long,
        amount: Long,
    ): List<RecordModel>

    /**
     * 批量导入记录
     *
     * @param records 要导入的记录列表（RecordTable 格式）
     * @return 插入后的记录 ID 列表
     */
    suspend fun batchImportRecords(records: List<cn.wj.android.cashbook.core.database.table.RecordTable>): List<Long>

    suspend fun queryExportRecords(booksId: Long, startDate: Long, endDate: Long): List<ExportRecordModel>

    suspend fun countExportRecords(booksId: Long, startDate: Long, endDate: Long): Int

    suspend fun queryEarliestRecordTime(booksId: Long): Long?
}

internal fun RecordTable.asModel(): RecordModel {
    return RecordModel(
        id = this.id ?: -1L,
        booksId = this.booksId,
        typeId = this.typeId,
        assetId = this.assetId,
        relatedAssetId = this.intoAssetId,
        amount = this.amount,
        finalAmount = this.finalAmount,
        charges = this.charge,
        concessions = this.concessions,
        remark = this.remark,
        reimbursable = this.reimbursable == SWITCH_INT_ON,
        recordTime = this.recordTime,
        reimbursed = this.reimbursed == SWITCH_INT_ON,
    )
}

internal fun RecordModel.asTable(): RecordTable {
    return RecordTable(
        id = if (this.id == -1L) null else this.id,
        booksId = this.booksId,
        typeId = this.typeId,
        assetId = this.assetId,
        intoAssetId = this.relatedAssetId,
        amount = this.amount,
        finalAmount = this.finalAmount,
        charge = this.charges,
        concessions = this.concessions,
        remark = this.remark,
        reimbursable = if (this.reimbursable) SWITCH_INT_ON else SWITCH_INT_OFF,
        recordTime = this.recordTime,
        reimbursed = if (this.reimbursed) SWITCH_INT_ON else SWITCH_INT_OFF,
    )
}

internal fun ImageWithRelatedTable.asModel(): ImageModel {
    return ImageModel(
        id = this.id ?: -1L,
        recordId = this.recordId,
        path = this.path,
        bytes = this.bytes,
    )
}

internal fun ImageModel.asModel(): ImageWithRelatedTable {
    return ImageWithRelatedTable(
        id = if (this.id == -1L) null else this.id,
        recordId = this.recordId,
        path = this.path,
        bytes = this.bytes,
    )
}

internal fun RecordViewsRelation.asSummaryModel(): RecordViewSummaryModel {
    return RecordViewSummaryModel(
        id = this.id,
        typeId = this.typeId,
        typeCategory = this.typeCategory,
        typeName = this.typeName,
        amount = this.amount,
        finalAmount = this.finalAmount,
        charges = this.charges,
        concessions = this.concessions,
        recordTime = this.recordTime,
    )
}
