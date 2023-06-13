package cn.wj.android.cashbook.core.model.transfer

import cn.wj.android.cashbook.core.model.entity.AssetEntity
import cn.wj.android.cashbook.core.model.entity.BooksEntity
import cn.wj.android.cashbook.core.model.entity.RecordEntity
import cn.wj.android.cashbook.core.model.entity.RecordTypeEntity
import cn.wj.android.cashbook.core.model.entity.RecordViewsEntity
import cn.wj.android.cashbook.core.model.entity.TagEntity
import cn.wj.android.cashbook.core.model.model.AssetModel
import cn.wj.android.cashbook.core.model.model.BooksModel
import cn.wj.android.cashbook.core.model.model.RecordModel
import cn.wj.android.cashbook.core.model.model.RecordTypeModel
import cn.wj.android.cashbook.core.model.model.RecordViewsModel
import cn.wj.android.cashbook.core.model.model.TagModel

fun TagModel.asEntity(selected: Boolean = false): TagEntity {
    return TagEntity(
        id = this.id,
        name = this.name,
        selected = selected,
    )
}

fun TagEntity.asModel(): TagModel {
    return TagModel(
        this.id,
        this.name,
    )
}

fun AssetModel.asEntity(): AssetEntity {
    return AssetEntity(
        id = this.id,
        booksId = this.booksId,
        name = this.name,
        iconResId = this.iconResId,
        totalAmount = this.totalAmount,
        billingDate = this.billingDate,
        repaymentDate = this.repaymentDate,
        type = this.type,
        classification = this.classification,
        invisible = this.invisible,
        openBank = this.openBank,
        cardNo = this.cardNo,
        remark = this.remark,
        sort = this.sort,
        modifyTime = this.modifyTime,
        balance = this.balance,
    )
}

fun AssetEntity.asModel(): AssetModel {
    return AssetModel(
        id = this.id,
        booksId = this.booksId,
        name = this.name,
        iconResId = this.iconResId,
        totalAmount = this.totalAmount,
        billingDate = this.billingDate,
        repaymentDate = this.repaymentDate,
        type = this.type,
        classification = this.classification,
        invisible = this.invisible,
        openBank = this.openBank,
        cardNo = this.cardNo,
        remark = this.remark,
        sort = this.sort,
        modifyTime = this.modifyTime,
        balance = this.balance,
    )
}

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

fun BooksModel.asEntity(): BooksEntity {
    return BooksEntity(
        id = this.id,
        name = this.name,
        description = this.description,
        modifyTime = this.modifyTime
    )
}

fun RecordTypeModel.asEntity(
    child: List<RecordTypeEntity> = listOf()
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

//fun RecordViewsModel.asEntity(): RecordViewsEntity {
//    return RecordViewsEntity(
//        this.id,
//        this.booksId,
//        this.type.asEntity(),
//        this.asset?.asEntity(),
//        this.relatedAsset?.asEntity(),
//        this.amount,
//        this.charges,
//        this.concessions,
//        this.remark,
//        this.reimbursable,
//        this.relatedTags.map { it.asEntity() },
//        this.relatedRecord.map { it.asEntity() },
//        this.recordTime,
//    )
//}