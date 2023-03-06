package cn.wj.android.cashbook.core.data.repository

import cn.wj.android.cashbook.core.common.SWITCH_INT_ON
import cn.wj.android.cashbook.core.common.tools.dateFormat
import cn.wj.android.cashbook.core.database.table.RecordTable
import cn.wj.android.cashbook.core.model.model.RecordModel
import cn.wj.android.cashbook.core.model.model.RecordTypeModel

interface RecordRepository {

    suspend fun queryById(recordId: Long): RecordModel?
}

internal fun RecordTable.asModel(): RecordModel {
    return RecordModel(
        id = this.id ?: -1L,
        booksId = this.booksId,
        type = this.typeId,
        asset = this.assetId,
        intoAsset = this.intoAssetId,
        amount = this.amount.toString(),
        charge = this.charge.toString(),
        concessions = this.concessions.toString(),
        remark = this.remark,
        reimbursable = this.reimbursable == SWITCH_INT_ON,
        modifyTime = this.modifyTime.dateFormat(),
    )
}