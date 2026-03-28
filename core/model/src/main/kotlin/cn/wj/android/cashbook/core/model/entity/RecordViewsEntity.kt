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

package cn.wj.android.cashbook.core.model.entity

import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.model.model.ImageModel
import cn.wj.android.cashbook.core.model.model.RECORD_TYPE_BALANCE_EXPENDITURE
import cn.wj.android.cashbook.core.model.model.RECORD_TYPE_BALANCE_INCOME
import cn.wj.android.cashbook.core.model.model.RecordModel
import cn.wj.android.cashbook.core.model.model.TagModel

/**
 * 记录数据实体类
 *
 * @param id 主键自增长
 * @param amount 记录金额
 * @param charges 转账手续费
 * @param concessions 优惠
 * @param remark 备注
 * @param reimbursable 能否报销
 * @param recordTime 修改时间
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/10
 */
data class RecordViewsEntity(
    val id: Long,
    val typeId: Long,
    val typeCategory: RecordTypeCategoryEnum,
    val typeName: String,
    val typeIconResName: String,
    val assetId: Long?,
    val assetName: String?,
    val assetIconResId: Int?,
    val relatedAssetId: Long?,
    val relatedAssetName: String?,
    val relatedAssetIconResId: Int?,
    /** 金额，单位：分 */
    val amount: Long,
    /** 最终金额，单位：分 */
    val finalAmount: Long,
    /** 手续费，单位：分 */
    val charges: Long,
    /** 优惠，单位：分 */
    val concessions: Long,
    val remark: String,
    val reimbursable: Boolean,
    val relatedTags: List<TagModel>,
    val relatedImage: List<ImageModel>,
    val relatedRecord: List<RecordModel>,
    /** 关联金额，单位：分 */
    val relatedAmount: Long,
    /** 记录时间，毫秒时间戳 */
    val recordTime: Long,
) : RecordViews {
    val isBalanceRecord: Boolean
        get() = typeId == RECORD_TYPE_BALANCE_EXPENDITURE.id ||
            typeId == RECORD_TYPE_BALANCE_INCOME.id
}

interface RecordViews
