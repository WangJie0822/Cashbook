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

package cn.wj.android.cashbook.domain.usecase

import cn.wj.android.cashbook.core.common.annotation.CashbookDispatchers
import cn.wj.android.cashbook.core.common.annotation.Dispatcher
import cn.wj.android.cashbook.core.common.ext.decimalFormat
import cn.wj.android.cashbook.core.common.ext.toBigDecimalOrZero
import cn.wj.android.cashbook.core.data.repository.AssetRepository
import cn.wj.android.cashbook.core.data.repository.RecordRepository
import cn.wj.android.cashbook.core.model.model.AssetModel
import cn.wj.android.cashbook.core.model.model.RECORD_TYPE_BALANCE_EXPENDITURE
import cn.wj.android.cashbook.core.model.model.RECORD_TYPE_BALANCE_INCOME
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlin.math.absoluteValue

class SaveAssetUseCase @Inject constructor(
    private val recordRepository: RecordRepository,
    private val assetRepository: AssetRepository,
    @Dispatcher(CashbookDispatchers.IO) private val coroutineContext: CoroutineContext,
) {

    suspend operator fun invoke(assetModel: AssetModel) = withContext(coroutineContext) {
        val oldAsset = assetRepository.getAssetById(assetModel.id)
        if (null != oldAsset) {
            // 已有资产，先更新除金额外的数据
            with(assetModel) {
                assetRepository.updateAsset(
                    oldAsset.copy(
                        name = name,
                        totalAmount = totalAmount,
                        openBank = openBank,
                        cardNo = cardNo,
                        billingDate = billingDate,
                        repaymentDate = repaymentDate,
                        invisible = invisible,
                        remark = remark,
                    ),
                )
            }
            // 修改资产，计算差额
            val diffBalance =
                (assetModel.balance.toBigDecimalOrZero() - oldAsset.balance.toBigDecimalOrZero()).toDouble()
            if (diffBalance != 0.0) {
                // 余额有变化
                val typeId = if (assetModel.type.isCreditCard) {
                    // 信用卡类型
                    if (diffBalance > 0.0) {
                        // 已使用额度增加，支出
                        RECORD_TYPE_BALANCE_EXPENDITURE
                    } else {
                        // 收入
                        RECORD_TYPE_BALANCE_INCOME
                    }
                } else {
                    // 其它类型
                    if (diffBalance > 0.0) {
                        // 余额增加，收入
                        RECORD_TYPE_BALANCE_INCOME
                    } else {
                        // 支出
                        RECORD_TYPE_BALANCE_EXPENDITURE
                    }
                }.id
                val record = recordRepository.getDefaultRecord(typeId)
                    .copy(
                        assetId = assetModel.id,
                        amount = diffBalance.absoluteValue.decimalFormat(),
                        remark = "金额变动，自动生成",
                    )
                // 插入记录
                recordRepository.updateRecord(
                    record = record,
                    tagIdList = emptyList(),
                    needRelated = false,
                    relatedRecordIdList = emptyList(),
                )
            }
        } else {
            // 新增资产
            assetRepository.updateAsset(assetModel)
        }
    }
}
