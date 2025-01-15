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

import cn.wj.android.cashbook.core.common.SWITCH_INT_OFF
import cn.wj.android.cashbook.core.common.SWITCH_INT_ON
import cn.wj.android.cashbook.core.common.ext.toDoubleOrZero
import cn.wj.android.cashbook.core.common.tools.DATE_FORMAT_NO_SECONDS
import cn.wj.android.cashbook.core.common.tools.dateFormat
import cn.wj.android.cashbook.core.common.tools.parseDateLong
import cn.wj.android.cashbook.core.database.table.RecordTable
import cn.wj.android.cashbook.core.model.model.RecordModel
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
    ): List<RecordModel>

    suspend fun queryPagingRecordListByTagId(
        tagId: Long,
        page: Int,
        pageSize: Int,
    ): List<RecordModel>

    suspend fun queryPagingRecordListByKeyword(
        keyword: String,
        page: Int,
        pageSize: Int,
    ): List<RecordModel>

    suspend fun queryRecordListBetweenDate(from: String, to: String): List<RecordModel>

    fun queryRecordByYearMonth(year: String, month: String): Flow<List<RecordModel>>

    suspend fun getDefaultRecord(typeId: Long): RecordModel

    suspend fun changeRecordTypeBeforeDeleteType(fromId: Long, toId: Long)

    suspend fun getRelatedIdListById(id: Long): List<Long>

    suspend fun getRecordIdListFromRelatedId(id: Long): List<Long>

    suspend fun getLastThreeMonthReimbursableRecordList(): List<RecordModel>

    suspend fun getLastThreeMonthRefundableRecordList(): List<RecordModel>

    suspend fun getLastThreeMonthReimbursableRecordListByKeyword(keyword: String): List<RecordModel>

    suspend fun getLastThreeMonthRefundableRecordListByKeyword(keyword: String): List<RecordModel>

    suspend fun getLastThreeMonthRecordCountByAssetId(assetId: Long): Int

    suspend fun deleteRecordsWithAsset(assetId: Long)

    suspend fun deleteRecordRelatedWithAsset(assetId: Long)

    suspend fun addSearchHistory(keyword: String)

    suspend fun clearSearchHistory()
}

internal fun RecordTable.asModel(): RecordModel {
    return RecordModel(
        id = this.id ?: -1L,
        booksId = this.booksId,
        typeId = this.typeId,
        assetId = this.assetId,
        relatedAssetId = this.intoAssetId,
        amount = this.amount.toString(),
        finalAmount = this.finalAmount.toString(),
        charges = this.charge.toString(),
        concessions = this.concessions.toString(),
        remark = this.remark,
        reimbursable = this.reimbursable == SWITCH_INT_ON,
        recordTime = this.recordTime.dateFormat(DATE_FORMAT_NO_SECONDS),
    )
}

internal fun RecordModel.asTable(): RecordTable {
    return RecordTable(
        id = if (this.id == -1L) null else this.id,
        booksId = this.booksId,
        typeId = this.typeId,
        assetId = this.assetId,
        intoAssetId = this.relatedAssetId,
        amount = this.amount.toDoubleOrZero(),
        finalAmount = this.finalAmount.toDoubleOrZero(),
        charge = this.charges.toDoubleOrZero(),
        concessions = this.concessions.toDoubleOrZero(),
        remark = this.remark,
        reimbursable = if (this.reimbursable) SWITCH_INT_ON else SWITCH_INT_OFF,
        recordTime = this.recordTime.parseDateLong(DATE_FORMAT_NO_SECONDS),
    )
}
