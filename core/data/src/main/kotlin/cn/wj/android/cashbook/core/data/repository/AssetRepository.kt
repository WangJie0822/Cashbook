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
import cn.wj.android.cashbook.core.common.tools.dateFormat
import cn.wj.android.cashbook.core.common.tools.parseDateLong
import cn.wj.android.cashbook.core.data.helper.AssetHelper
import cn.wj.android.cashbook.core.database.table.AssetTable
import cn.wj.android.cashbook.core.model.enums.AssetClassificationEnum
import cn.wj.android.cashbook.core.model.enums.ClassificationTypeEnum
import cn.wj.android.cashbook.core.model.model.AssetModel
import cn.wj.android.cashbook.core.model.model.AssetTypeViewsModel
import kotlinx.coroutines.flow.Flow

/**
 * 资产类型数据仓库
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/2/20
 */
interface AssetRepository {

    /** 当前可见资产列表数据 */
    val currentVisibleAssetListData: Flow<List<AssetModel>>

    /** 当前可见资产大类数据 */
    val currentVisibleAssetTypeData: Flow<List<AssetTypeViewsModel>>

    /** 当前不可见资产列表数据 */
    val currentInvisibleAssetListData: Flow<List<AssetModel>>

    /** 当前不可见资产大类数据 */
    val currentInvisibleAssetTypeData: Flow<List<AssetTypeViewsModel>>

    val topUpInTotalData: Flow<Boolean>

    /** 根据资产id [assetId] 获取对应资产数据并返回 */
    suspend fun getAssetById(assetId: Long): AssetModel?

    suspend fun getVisibleAssetsByBookId(bookId: Long): List<AssetModel>

    suspend fun getInvisibleAssetsByBookId(bookId: Long): List<AssetModel>

    suspend fun updateAsset(asset: AssetModel)

    suspend fun deleteById(assetId: Long)

    suspend fun visibleAssetById(id: Long)

    suspend fun updateTopUpInTotal(topUpInTotal: Boolean)
}

internal fun AssetTable.asModel(): AssetModel {
    val classification = AssetClassificationEnum.ordinalOf(this.classification)
    return AssetModel(
        id = this.id ?: -1L,
        booksId = this.booksId,
        name = this.name,
        iconResId = AssetHelper.getIconResIdByType(classification),
        totalAmount = this.totalAmount.toString(),
        billingDate = this.billingDate,
        repaymentDate = this.repaymentDate,
        type = ClassificationTypeEnum.ordinalOf(this.type),
        classification = classification,
        invisible = this.invisible == SWITCH_INT_ON,
        openBank = this.openBank,
        cardNo = this.cardNo,
        remark = this.remark,
        sort = this.sort,
        modifyTime = this.modifyTime.dateFormat(),
        balance = this.balance.toString(),
    )
}

internal fun AssetModel.asTable(): AssetTable {
    return AssetTable(
        id = if (this.id == -1L) null else this.id,
        booksId = this.booksId,
        name = this.name,
        totalAmount = this.totalAmount.toDoubleOrZero(),
        billingDate = this.billingDate,
        repaymentDate = this.repaymentDate,
        type = this.type.ordinal,
        classification = this.classification.ordinal,
        invisible = if (this.invisible) SWITCH_INT_ON else SWITCH_INT_OFF,
        openBank = this.openBank,
        cardNo = this.cardNo,
        remark = this.remark,
        sort = this.sort,
        modifyTime = this.modifyTime.parseDateLong(),
        balance = this.balance.toDoubleOrZero(),
    )
}
