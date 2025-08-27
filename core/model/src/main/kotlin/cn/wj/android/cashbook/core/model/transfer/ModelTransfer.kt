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

package cn.wj.android.cashbook.core.model.transfer

import cn.wj.android.cashbook.core.model.entity.RecordEntity
import cn.wj.android.cashbook.core.model.entity.RecordTypeEntity
import cn.wj.android.cashbook.core.model.entity.RecordViewsEntity
import cn.wj.android.cashbook.core.model.model.RecordModel
import cn.wj.android.cashbook.core.model.model.RecordTypeModel
import cn.wj.android.cashbook.core.model.model.RecordViewsModel

fun RecordModel.asEntity(): RecordEntity {
    return RecordEntity(
        id = this.id,
        booksId = this.booksId,
        typeId = this.typeId,
        assetId = this.assetId,
        relatedAssetId = this.relatedAssetId,
        amount = this.amount,
        finalAmount = this.finalAmount,
        charges = this.charges,
        concessions = this.concessions,
        remark = this.remark,
        reimbursable = this.reimbursable,
        recordTime = this.recordTime,
    )
}

fun RecordEntity.asModel(): RecordModel {
    return RecordModel(
        id = this.id,
        booksId = this.booksId,
        typeId = this.typeId,
        assetId = this.assetId,
        relatedAssetId = this.relatedAssetId,
        amount = this.amount,
        finalAmount = this.finalAmount,
        charges = this.charges,
        concessions = this.concessions,
        remark = this.remark,
        reimbursable = this.reimbursable,
        recordTime = this.recordTime,
    )
}

fun RecordTypeModel.asEntity(
    child: List<RecordTypeEntity> = emptyList(),
    selected: Boolean = false,
): RecordTypeEntity {
    return RecordTypeEntity(
        id = this.id,
        parentId = this.parentId,
        name = this.name,
        iconResName = this.iconName,
        sort = this.sort,
        typeCategory = this.typeCategory,
        child = child,
        selected = selected,
        shapeType = -1,
        needRelated = needRelated,
    )
}

fun RecordViewsModel.asEntity(): RecordViewsEntity {
    return RecordViewsEntity(
        id = this.id,
        typeCategory = this.type.typeCategory,
        typeName = this.type.name,
        typeIconResName = this.type.iconName,
        assetId = this.asset?.id,
        assetName = this.asset?.name,
        assetIconResId = this.asset?.iconResId,
        relatedAssetId = this.relatedAsset?.id,
        relatedAssetName = this.relatedAsset?.name,
        relatedAssetIconResId = this.relatedAsset?.iconResId,
        amount = this.amount,
        finalAmount = this.finalAmount,
        charges = this.charges,
        concessions = this.concessions,
        remark = this.remark,
        reimbursable = this.reimbursable,
        relatedTags = this.relatedTags,
        relatedImage = this.relatedImage,
        relatedRecord = this.relatedRecord,
        relatedAmount = this.relatedAmount,
        recordTime = this.recordTime,
    )
}
