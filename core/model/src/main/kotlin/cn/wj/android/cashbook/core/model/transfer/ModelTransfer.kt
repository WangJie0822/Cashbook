package cn.wj.android.cashbook.core.model.transfer

import cn.wj.android.cashbook.core.model.entity.TagEntity
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