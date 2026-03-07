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

package cn.wj.android.cashbook.core.testing.data

import cn.wj.android.cashbook.core.model.enums.AssetClassificationEnum
import cn.wj.android.cashbook.core.model.enums.ClassificationTypeEnum
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.model.enums.TypeLevelEnum
import cn.wj.android.cashbook.core.model.model.AssetModel
import cn.wj.android.cashbook.core.model.model.BooksModel
import cn.wj.android.cashbook.core.model.model.ImageModel
import cn.wj.android.cashbook.core.model.model.RecordModel
import cn.wj.android.cashbook.core.model.model.RecordTypeModel
import cn.wj.android.cashbook.core.model.model.RecordViewsModel
import cn.wj.android.cashbook.core.model.model.TagModel

/**
 * 提供默认参数的工厂方法，用于简化测试数据创建
 */
fun createRecordModel(
    id: Long = -1L,
    booksId: Long = 1L,
    typeId: Long = 1L,
    assetId: Long = -1L,
    relatedAssetId: Long = -1L,
    amount: String = "0",
    finalAmount: String = "0",
    charges: String = "0",
    concessions: String = "0",
    remark: String = "",
    reimbursable: Boolean = false,
    recordTime: String = "2024-01-01 00:00",
): RecordModel = RecordModel(
    id = id,
    booksId = booksId,
    typeId = typeId,
    assetId = assetId,
    relatedAssetId = relatedAssetId,
    amount = amount,
    finalAmount = finalAmount,
    charges = charges,
    concessions = concessions,
    remark = remark,
    reimbursable = reimbursable,
    recordTime = recordTime,
)

fun createAssetModel(
    id: Long = -1L,
    booksId: Long = 1L,
    name: String = "测试资产",
    iconResId: Int = 0,
    totalAmount: String = "0",
    billingDate: String = "",
    repaymentDate: String = "",
    type: ClassificationTypeEnum = ClassificationTypeEnum.CAPITAL_ACCOUNT,
    classification: AssetClassificationEnum = AssetClassificationEnum.CASH,
    invisible: Boolean = false,
    openBank: String = "",
    cardNo: String = "",
    remark: String = "",
    sort: Int = 0,
    modifyTime: String = "",
    balance: String = "0",
): AssetModel = AssetModel(
    id = id,
    booksId = booksId,
    name = name,
    iconResId = iconResId,
    totalAmount = totalAmount,
    billingDate = billingDate,
    repaymentDate = repaymentDate,
    type = type,
    classification = classification,
    invisible = invisible,
    openBank = openBank,
    cardNo = cardNo,
    remark = remark,
    sort = sort,
    modifyTime = modifyTime,
    balance = balance,
)

fun createRecordTypeModel(
    id: Long = 1L,
    parentId: Long = -1L,
    name: String = "测试类型",
    iconName: String = "vector_test",
    typeLevel: TypeLevelEnum = TypeLevelEnum.FIRST,
    typeCategory: RecordTypeCategoryEnum = RecordTypeCategoryEnum.EXPENDITURE,
    protected: Boolean = false,
    sort: Int = 0,
    needRelated: Boolean = false,
): RecordTypeModel = RecordTypeModel(
    id = id,
    parentId = parentId,
    name = name,
    iconName = iconName,
    typeLevel = typeLevel,
    typeCategory = typeCategory,
    protected = protected,
    sort = sort,
    needRelated = needRelated,
)

fun createTagModel(
    id: Long = 1L,
    name: String = "测试标签",
    invisible: Boolean = false,
): TagModel = TagModel(
    id = id,
    name = name,
    invisible = invisible,
)

fun createBooksModel(
    id: Long = 1L,
    name: String = "测试账本",
    description: String = "",
    bgUri: String = "",
    modifyTime: Long = 0L,
): BooksModel = BooksModel(
    id = id,
    name = name,
    description = description,
    bgUri = bgUri,
    modifyTime = modifyTime,
)

fun createImageModel(
    id: Long = -1L,
    recordId: Long = -1L,
    path: String = "",
    bytes: ByteArray = byteArrayOf(),
): ImageModel = ImageModel(
    id = id,
    recordId = recordId,
    path = path,
    bytes = bytes,
)

fun createRecordViewsModel(
    id: Long = -1L,
    booksId: Long = 1L,
    type: RecordTypeModel = createRecordTypeModel(),
    asset: AssetModel? = null,
    relatedAsset: AssetModel? = null,
    amount: String = "0",
    finalAmount: String = "0",
    charges: String = "0",
    concessions: String = "0",
    remark: String = "",
    reimbursable: Boolean = false,
    relatedTags: List<TagModel> = emptyList(),
    relatedImage: List<ImageModel> = emptyList(),
    relatedRecord: List<RecordModel> = emptyList(),
    relatedAmount: String = "0",
    recordTime: String = "2024-01-01 00:00",
): RecordViewsModel = RecordViewsModel(
    id = id,
    booksId = booksId,
    type = type,
    asset = asset,
    relatedAsset = relatedAsset,
    amount = amount,
    finalAmount = finalAmount,
    charges = charges,
    concessions = concessions,
    remark = remark,
    reimbursable = reimbursable,
    relatedTags = relatedTags,
    relatedImage = relatedImage,
    relatedRecord = relatedRecord,
    relatedAmount = relatedAmount,
    recordTime = recordTime,
)
