package cn.wj.android.cashbook.core.model.transfer

import cn.wj.android.cashbook.core.model.entity.AssetEntity
import cn.wj.android.cashbook.core.model.entity.TagEntity
import cn.wj.android.cashbook.core.model.model.AssetModel
import cn.wj.android.cashbook.core.model.model.TagModel

fun TagModel.asEntity(): TagEntity {
    return TagEntity(
        this.id,
        this.name,
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

fun AssetEntity.asModel():  AssetModel{
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