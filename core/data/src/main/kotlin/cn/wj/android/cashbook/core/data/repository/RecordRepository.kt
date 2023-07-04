package cn.wj.android.cashbook.core.data.repository

import cn.wj.android.cashbook.core.common.SWITCH_INT_OFF
import cn.wj.android.cashbook.core.common.SWITCH_INT_ON
import cn.wj.android.cashbook.core.common.ext.toDoubleOrZero
import cn.wj.android.cashbook.core.common.tools.DATE_FORMAT_NO_SECONDS
import cn.wj.android.cashbook.core.common.tools.dateFormat
import cn.wj.android.cashbook.core.common.tools.parseDateLong
import cn.wj.android.cashbook.core.database.table.RecordTable
import cn.wj.android.cashbook.core.model.entity.RecordViewsEntity
import cn.wj.android.cashbook.core.model.model.RecordModel
import cn.wj.android.cashbook.core.model.model.TagModel
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow

interface RecordRepository {

    val currentMonthRecordListData: Flow<List<RecordModel>>

    suspend fun queryById(recordId: Long): RecordModel?

    suspend fun queryRelatedById(recordId: Long): List<RecordModel>

    suspend fun updateRecord(record: RecordModel, tags: List<TagModel>)

    suspend fun deleteRecord(recordId: Long)

    suspend fun queryExpenditureRecordAfterDate(
        reimburse: Boolean,
        dataTime: Long
    ): List<RecordModel>

    suspend fun queryExpenditureRecordByAmountOrRemark(keyword: String): List<RecordViewsEntity>

    suspend fun queryPagingRecordListByAssetId(
        assetId: Long,
        page: Int,
        pageSize: Int,
        coroutineContext: CoroutineContext = Dispatchers.IO
    ): List<RecordModel>
}

internal fun RecordTable.asModel(): RecordModel {
    return RecordModel(
        id = this.id ?: -1L,
        booksId = this.booksId,
        typeId = this.typeId,
        assetId = this.assetId,
        relatedAssetId = this.intoAssetId,
        amount = this.amount.toString(),
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
        charge = this.charges.toDoubleOrZero(),
        concessions = this.concessions.toDoubleOrZero(),
        remark = this.remark,
        reimbursable = if (this.reimbursable) SWITCH_INT_ON else SWITCH_INT_OFF,
        recordTime = this.recordTime.parseDateLong(DATE_FORMAT_NO_SECONDS),
    )
}