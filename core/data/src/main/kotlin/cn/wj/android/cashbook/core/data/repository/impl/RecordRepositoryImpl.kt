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

package cn.wj.android.cashbook.core.data.repository.impl

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import cn.wj.android.cashbook.core.common.annotation.CashbookDispatchers
import cn.wj.android.cashbook.core.common.annotation.Dispatcher
import cn.wj.android.cashbook.core.common.ext.completeZero
import cn.wj.android.cashbook.core.common.ext.logger
import cn.wj.android.cashbook.core.common.model.assetDataVersion
import cn.wj.android.cashbook.core.common.model.recordDataVersion
import cn.wj.android.cashbook.core.common.model.updateVersion
import cn.wj.android.cashbook.core.common.tools.parseDateLong
import cn.wj.android.cashbook.core.data.repository.RecordRepository
import cn.wj.android.cashbook.core.data.repository.asModel
import cn.wj.android.cashbook.core.data.repository.asSummaryModel
import cn.wj.android.cashbook.core.data.repository.asTable
import cn.wj.android.cashbook.core.database.dao.RecordDao
import cn.wj.android.cashbook.core.database.dao.TransactionDao
import cn.wj.android.cashbook.core.datastore.datasource.CombineProtoDataSource
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.model.model.ImageModel
import cn.wj.android.cashbook.core.model.model.RecordModel
import cn.wj.android.cashbook.core.model.model.RecordViewSummaryModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.withContext
import java.util.Calendar
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class RecordRepositoryImpl @Inject constructor(
    private val recordDao: RecordDao,
    private val transactionDao: TransactionDao,
    private val combineProtoDataSource: CombineProtoDataSource,
    @Dispatcher(CashbookDispatchers.IO) private val coroutineContext: CoroutineContext,
) : RecordRepository {

    override val searchHistoryListData: Flow<List<String>> =
        combineProtoDataSource.searchHistoryData.mapLatest { it.keywords }

    override suspend fun queryById(recordId: Long): RecordModel? = withContext(coroutineContext) {
        recordDao.queryById(recordId)?.asModel()
    }

    override suspend fun queryByTypeId(id: Long): List<RecordModel> =
        withContext(coroutineContext) {
            recordDao.queryByTypeId(id)
                .map { it.asModel() }
        }

    override suspend fun queryRelatedById(recordId: Long): List<RecordModel> =
        withContext(coroutineContext) {
            recordDao.queryRelatedById(recordId)
                .map { it.asModel() }
        }

    override suspend fun updateRecord(
        record: RecordModel,
        tagIdList: List<Long>,
        needRelated: Boolean,
        relatedRecordIdList: List<Long>,
        relatedImageList: List<ImageModel>,
    ) = withContext(coroutineContext) {
        logger().i("updateRecord(record = <$record>, tagIdList = <$tagIdList>")
        transactionDao.updateRecordTransaction(
            record = record.asTable(),
            tagIdList = tagIdList,
            needRelated = needRelated,
            relatedRecordIdList = relatedRecordIdList,
            relatedImageList = relatedImageList,
        )
        recordDataVersion.updateVersion()
        assetDataVersion.updateVersion()
        // 更新上次使用的默认数据
        combineProtoDataSource.updateLastAssetId(record.assetId)
    }

    override suspend fun deleteRecord(recordId: Long) = withContext(coroutineContext) {
        transactionDao.deleteRecordTransaction(recordId)
        recordDataVersion.updateVersion()
        assetDataVersion.updateVersion()
    }

    override suspend fun queryPagingRecordListByAssetId(
        assetId: Long,
        page: Int,
        pageSize: Int,
    ): List<RecordModel> = withContext(coroutineContext) {
        recordDao.queryRecordByAssetId(
            booksId = combineProtoDataSource.recordSettingsData.first().currentBookId,
            assetId = assetId,
            pageNum = page * pageSize,
            pageSize = pageSize,
        ).map {
            it.asModel()
        }
    }

    override suspend fun queryPagingRecordListByTypeId(
        typeId: Long,
        page: Int,
        pageSize: Int,
        includeChildTypes: Boolean,
    ): List<RecordModel> = withContext(coroutineContext) {
        val booksId = combineProtoDataSource.recordSettingsData.first().currentBookId
        if (includeChildTypes) {
            recordDao.queryRecordByTypeId(
                booksId = booksId,
                typeId = typeId,
                pageNum = page * pageSize,
                pageSize = pageSize,
            )
        } else {
            recordDao.queryRecordByTypeIdExact(
                booksId = booksId,
                typeId = typeId,
                pageNum = page * pageSize,
                pageSize = pageSize,
            )
        }.map {
            it.asModel()
        }
    }

    override suspend fun queryPagingRecordListByTypeIdBetweenDate(
        typeId: Long,
        dateRange: String,
        page: Int,
        pageSize: Int,
        includeChildTypes: Boolean,
    ): List<RecordModel> = withContext(coroutineContext) {
        if (dateRange.isNotBlank()) {
            val startDate: Long
            val endDate: Long
            if (dateRange.contains("~")) {
                val dates = dateRange.split("~")
                startDate = "${dates[0]} 00:00:00".parseDateLong()
                // 使用次日 00:00:00 作为结束时间（半开区间）
                endDate = with(Calendar.getInstance()) {
                    timeInMillis = "${dates[1]} 00:00:00".parseDateLong()
                    add(Calendar.DAY_OF_MONTH, 1)
                    timeInMillis
                }
            } else if (dateRange.contains("-")) {
                startDate = "$dateRange-01 00:00:00".parseDateLong()
                endDate = with(Calendar.getInstance()) {
                    timeInMillis = startDate
                    add(Calendar.MONTH, 1)
                    timeInMillis
                }
            } else {
                startDate = "$dateRange-01-01 00:00:00".parseDateLong()
                // 使用次年 1 月 1 日作为结束时间（半开区间）
                endDate = with(Calendar.getInstance()) {
                    timeInMillis = startDate
                    add(Calendar.YEAR, 1)
                    timeInMillis
                }
            }
            if (includeChildTypes) {
                recordDao.queryRecordByTypeIdBetween(
                    booksId = combineProtoDataSource.recordSettingsData.first().currentBookId,
                    typeId = typeId,
                    startDate = startDate,
                    endDate = endDate,
                    pageNum = page * pageSize,
                    pageSize = pageSize,
                )
            } else {
                recordDao.queryRecordByTypeIdExactBetween(
                    booksId = combineProtoDataSource.recordSettingsData.first().currentBookId,
                    typeId = typeId,
                    startDate = startDate,
                    endDate = endDate,
                    pageNum = page * pageSize,
                    pageSize = pageSize,
                )
            }
        } else {
            if (includeChildTypes) {
                recordDao.queryRecordByTypeId(
                    booksId = combineProtoDataSource.recordSettingsData.first().currentBookId,
                    typeId = typeId,
                    pageNum = page * pageSize,
                    pageSize = pageSize,
                )
            } else {
                recordDao.queryRecordByTypeIdExact(
                    booksId = combineProtoDataSource.recordSettingsData.first().currentBookId,
                    typeId = typeId,
                    pageNum = page * pageSize,
                    pageSize = pageSize,
                )
            }
        }.map {
            it.asModel()
        }
    }

    override suspend fun queryPagingRecordListByTagId(
        tagId: Long,
        page: Int,
        pageSize: Int,
    ): List<RecordModel> = withContext(coroutineContext) {
        recordDao.queryRecordByTagId(
            booksId = combineProtoDataSource.recordSettingsData.first().currentBookId,
            tagId = tagId,
            pageNum = page * pageSize,
            pageSize = pageSize,
        ).map {
            it.asModel()
        }
    }

    override suspend fun queryPagingRecordListByKeyword(
        keyword: String,
        page: Int,
        pageSize: Int,
    ): List<RecordModel> = withContext(coroutineContext) {
        recordDao.queryRecordByKeyword(
            booksId = combineProtoDataSource.recordSettingsData.first().currentBookId,
            keyword = keyword,
            pageNum = page * pageSize,
            pageSize = pageSize,
        ).map {
            it.asModel()
        }
    }

    override fun queryRecordByYearMonth(
        year: String,
        month: String,
    ): Flow<List<RecordModel>> {
        val monthInt = month.toInt()
        val yearInt = year.toInt()
        var nextYearInt = yearInt
        val nextMonthInt = if (monthInt >= 12) {
            nextYearInt++
            1
        } else {
            monthInt + 1
        }
        val startDate = "$yearInt-${monthInt.completeZero()}-01 00:00:00"
        val endDate = "$nextYearInt-${nextMonthInt.completeZero()}-01 00:00:00"
        return combine(recordDataVersion, combineProtoDataSource.recordSettingsData) { _, data ->
            recordDao.queryByBooksIdBetweenDate(
                data.currentBookId,
                startDate.parseDateLong(),
                endDate.parseDateLong(),
            ).map {
                it.asModel()
            }
        }
    }

    override suspend fun queryRecordListBetweenDate(from: Long, to: Long): List<RecordModel> =
        withContext(coroutineContext) {
            this@RecordRepositoryImpl.logger()
                .i("queryRecordListBetweenDate(from = <$from>, to = <$to>)")
            recordDao.queryByBooksIdBetweenDate(
                combineProtoDataSource.recordSettingsData.first().currentBookId,
                from,
                to,
            ).map {
                it.asModel()
            }
        }

    override fun getRecordPagingData(
        startDate: Long,
        endDate: Long,
    ): Flow<PagingData<RecordModel>> {
        return combineProtoDataSource.recordSettingsData
            .flatMapLatest { settings ->
                Pager(
                    config = PagingConfig(pageSize = 20),
                    pagingSourceFactory = {
                        recordDao.pagingQueryByBooksIdBetweenDate(
                            booksId = settings.currentBookId,
                            startDate = startDate,
                            endDate = endDate,
                        )
                    },
                ).flow.map { pagingData ->
                    pagingData.map { it.asModel() }
                }
            }
    }

    override fun queryRecordViewSummariesFlow(
        startDate: Long,
        endDate: Long,
    ): Flow<List<RecordViewSummaryModel>> {
        return combine(
            recordDataVersion,
            combineProtoDataSource.recordSettingsData,
        ) { _, settings ->
            recordDao.queryViewsBetweenDate(
                booksId = settings.currentBookId,
                startDate = startDate,
                endDate = endDate,
            ).map { it.asSummaryModel() }
        }
    }

    override suspend fun getDefaultRecord(typeId: Long): RecordModel =
        withContext(coroutineContext) {
            val appDataModel = combineProtoDataSource.recordSettingsData.first()
            RecordModel(
                id = -1L,
                booksId = appDataModel.currentBookId,
                typeId = typeId,
                assetId = appDataModel.lastAssetId,
                relatedAssetId = -1L,
                amount = 0L,
                finalAmount = 0L,
                charges = 0L,
                concessions = 0L,
                remark = "",
                reimbursable = false,
                recordTime = System.currentTimeMillis(),
            )
        }

    override suspend fun changeRecordTypeBeforeDeleteType(fromId: Long, toId: Long): Unit =
        withContext(coroutineContext) {
            recordDao.changeRecordTypeBeforeDeleteType(fromId = fromId, toId = toId)
            recordDataVersion.updateVersion()
        }

    override suspend fun getRelatedIdListById(id: Long): List<Long> =
        withContext(coroutineContext) {
            recordDao.getRelatedIdListById(id)
        }

    override suspend fun getRecordIdListFromRelatedId(id: Long): List<Long> =
        withContext(coroutineContext) {
            recordDao.getRecordIdListFromRelatedId(id)
        }

    override suspend fun getLastThreeMonthRefundableRecordList(): List<RecordModel> =
        withContext(coroutineContext) {
            // 获取最近三个月开始时间
            val startDate = getThreeMonthAgoStartTimeMs()
            val appDataModel = combineProtoDataSource.recordSettingsData.first()
            recordDao.getExpenditureRecordListAfterTime(
                booksId = appDataModel.currentBookId,
                recordTime = startDate,
            ).map { it.asModel() }
        }

    override suspend fun getLastThreeMonthReimbursableRecordList(): List<RecordModel> =
        withContext(coroutineContext) {
            // 获取最近三个月开始时间
            val startDate = getThreeMonthAgoStartTimeMs()
            val appDataModel = combineProtoDataSource.recordSettingsData.first()
            recordDao.getExpenditureReimburseRecordListAfterTime(
                booksId = appDataModel.currentBookId,
                recordTime = startDate,
            ).map { it.asModel() }
        }

    override suspend fun getLastThreeMonthRefundableRecordListByKeyword(keyword: String): List<RecordModel> =
        withContext(coroutineContext) {
            // 获取最近三个月开始时间
            val startDate = getThreeMonthAgoStartTimeMs()
            val appDataModel = combineProtoDataSource.recordSettingsData.first()
            recordDao.getExpenditureRecordListByKeywordAfterTime(
                keyword = keyword,
                booksId = appDataModel.currentBookId,
                recordTime = startDate,
            ).map { it.asModel() }
        }

    override suspend fun getLastThreeMonthRecordCountByAssetId(assetId: Long): Int =
        withContext(coroutineContext) {
            // 获取最近三个月开始时间
            val startDate = getThreeMonthAgoStartTimeMs()
            recordDao.getRecordCountByAssetIdAfterTime(assetId, startDate)
        }

    override suspend fun getLastThreeMonthReimbursableRecordListByKeyword(keyword: String): List<RecordModel> =
        withContext(coroutineContext) {
            // 获取最近三个月开始时间
            val startDate = getThreeMonthAgoStartTimeMs()
            val appDataModel = combineProtoDataSource.recordSettingsData.first()
            recordDao.getLastThreeMonthExpenditureReimburseRecordListByKeyword(
                keyword = keyword,
                booksId = appDataModel.currentBookId,
                recordTime = startDate,
            ).map { it.asModel() }
        }

    /** 获取三个月前的起始时间戳（毫秒） */
    private fun getThreeMonthAgoStartTimeMs(): Long {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -90)
        // 设置为当天零点
        calendar[Calendar.HOUR_OF_DAY] = 0
        calendar[Calendar.MINUTE] = 0
        calendar[Calendar.SECOND] = 0
        calendar[Calendar.MILLISECOND] = 0
        return calendar.timeInMillis
    }

    override suspend fun deleteRecordsWithAsset(assetId: Long): Unit =
        withContext(coroutineContext) {
            // 事务化删除：标签关联、记录关联、图片关联、记录
            transactionDao.deleteAssetRelatedData(assetId)
            recordDataVersion.updateVersion()
        }

    override suspend fun deleteRecordRelatedWithAsset(assetId: Long): Unit =
        withContext(coroutineContext) {
            // 已由 deleteRecordsWithAsset 中的事务方法统一处理，此处保留为空操作以兼容接口
        }

    override suspend fun addSearchHistory(keyword: String): Unit = withContext(coroutineContext) {
        if (keyword.isBlank()) {
            return@withContext
        }
        var currentList: MutableList<String> = ArrayList(searchHistoryListData.first())
        if (currentList.contains(keyword)) {
            return@withContext
        }
        currentList.add(0, keyword)
        if (currentList.size > 10) {
            currentList = currentList.subList(0, 10)
        }
        combineProtoDataSource.updateKeywords(currentList)
    }

    override suspend fun clearSearchHistory(): Unit = withContext(coroutineContext) {
        combineProtoDataSource.updateKeywords(emptyList())
    }

    override suspend fun migrateAfter9To10() = withContext(coroutineContext) {
        // 更新转账记录
        val transferCount = recordDao.updateRecord(
            recordDao.queryByTypeCategory(RecordTypeCategoryEnum.TRANSFER.ordinal).map {
                // 转账记录最终金额计算为 优惠 - 手续费
                it.copy(finalAmount = it.concessions - it.charge)
            },
        )
        this@RecordRepositoryImpl.logger().i("migrateAfter9To10(), transferCount $transferCount")
        val relatedRecordList = recordDao.queryRelatedRecord()
        val expandList =
            recordDao.queryByTypeCategory(RecordTypeCategoryEnum.EXPENDITURE.ordinal).map {
                if (relatedRecordList.count { related -> related.relatedRecordId == it.id } > 0) {
                    // 已退款、已报销记录，最终金额为 0
                    it.copy(finalAmount = 0L)
                } else {
                    // 普通支出，最终金额为 金额 + 手续费 - 优惠
                    it.copy(finalAmount = it.amount + it.charge - it.concessions)
                }
            }
        // 更新支出记录
        val expandCount = recordDao.updateRecord(expandList)
        this@RecordRepositoryImpl.logger().i("migrateAfter9To10(), expandCount $expandCount")
        val appData = combineProtoDataSource.recordSettingsData.first()
        val incomeCount = recordDao.updateRecord(
            recordDao.queryByTypeCategory(RecordTypeCategoryEnum.INCOME.ordinal).map {
                if (it.typeId == appData.refundTypeId || it.typeId == appData.reimburseTypeId) {
                    // 退款、报销类型
                    val relatedIdList =
                        relatedRecordList.filter { related -> related.recordId == it.id }
                            .map { related -> related.relatedRecordId }
                    if (relatedIdList.isNotEmpty()) {
                        // 已关联记录，最终金额为 金额 - 手续费 - 关联记录金额
                        var expandAmount = 0L
                        expandList.filter { expand -> expand.id in relatedIdList }
                            .forEach { expand -> expandAmount += (expand.amount + expand.charge - expand.concessions) }
                        it.copy(finalAmount = it.amount - it.charge - expandAmount)
                    } else {
                        // 未关联记录，最终金额计算为 金额 - 手续费
                        it.copy(finalAmount = it.amount - it.charge)
                    }
                } else {
                    // 其它收入记录，最终金额计算为 金额 - 手续费
                    it.copy(finalAmount = it.amount - it.charge)
                }
            },
        )
        this@RecordRepositoryImpl.logger().i("migrateAfter9To10(), incomeCount $incomeCount")
        // 标记已完成迁移
        combineProtoDataSource.updateDb9To10DataMigrated(true)
    }

    override suspend fun queryRelatedRecordCountById(id: Long): Int =
        withContext(coroutineContext) {
            recordDao.queryRelatedRecordCountByID(id)
        }

    override suspend fun queryImagesByRecordId(id: Long): List<ImageModel> =
        withContext(coroutineContext) {
            recordDao.queryImagesByRecordId(id).map { it.asModel() }
        }

    override suspend fun queryByWechatTransactionId(
        booksId: Long,
        transactionId: String,
    ): List<RecordModel> = withContext(coroutineContext) {
        recordDao.queryByWechatTransactionId(booksId, transactionId).map { it.asModel() }
    }

    override suspend fun queryByTimeAndAmount(
        booksId: Long,
        startTime: Long,
        endTime: Long,
        amount: Double,
    ): List<RecordModel> = withContext(coroutineContext) {
        recordDao.queryByTimeAndAmount(booksId, startTime, endTime, amount).map { it.asModel() }
    }

    override suspend fun batchImportRecords(
        records: List<cn.wj.android.cashbook.core.database.table.RecordTable>,
    ): List<Long> = withContext(coroutineContext) {
        val ids = transactionDao.batchImportRecordsTransaction(records)
        recordDataVersion.updateVersion()
        assetDataVersion.updateVersion()
        ids
    }
}
