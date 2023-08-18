package cn.wj.android.cashbook.core.model.transfer

import cn.wj.android.cashbook.core.model.entity.RecordEntity
import cn.wj.android.cashbook.core.model.entity.RecordTypeEntity
import cn.wj.android.cashbook.core.model.model.RecordModel
import cn.wj.android.cashbook.core.model.model.RecordTypeModel

fun RecordModel.asEntity(): RecordEntity {
    return RecordEntity(
        this.id,
        this.booksId,
        this.typeId,
        this.assetId,
        this.relatedAssetId,
        this.amount,
        this.charges,
        this.concessions,
        this.remark,
        this.reimbursable,
        this.recordTime
    )
}

fun RecordEntity.asModel(): RecordModel {
    return RecordModel(
        this.id,
        this.booksId,
        this.typeId,
        this.assetId,
        this.relatedAssetId,
        this.amount,
        this.charges,
        this.concessions,
        this.remark,
        this.reimbursable,
        this.recordTime
    )
}

fun RecordTypeModel.asEntity(
    child: List<RecordTypeEntity> = emptyList()
): RecordTypeEntity {
    return RecordTypeEntity(
        id = this.id,
        parentId = this.parentId,
        name = this.name,
        iconResName = this.iconName,
        sort = this.sort,
        typeCategory = this.typeCategory,
        child = child,
        selected = false,
        shapeType = -1,
        needRelated = needRelated,
    )
}