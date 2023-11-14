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

import cn.wj.android.cashbook.core.data.repository.AssetRepository
import cn.wj.android.cashbook.core.data.repository.RecordRepository
import cn.wj.android.cashbook.core.data.repository.TypeRepository
import cn.wj.android.cashbook.core.model.enums.ClassificationTypeEnum
import cn.wj.android.cashbook.core.model.model.AssetModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * 根据不同类型展示不同资产列表
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/10/10
 */
class GetAssetListUseCase @Inject constructor(
    private val assetRepository: AssetRepository,
    private val recordRepository: RecordRepository,
    private val typeRepository: TypeRepository,
) {

    operator fun invoke(
        currentTypeId: Long,
        selectedAssetId: Long,
        isRelated: Boolean,
    ): Flow<List<AssetModel>> {
        return assetRepository.currentVisibleAssetListData
            .map { list ->
                // 首先按照最近使用情况进行排序
                var result = list
                    .map {
                        it.copy(sort = recordRepository.getLastThreeMonthRecordCountByAssetId(it.id))
                    }
                    .sortedBy { it.sort }
                    .reversed()
                if (isRelated && typeRepository.isCreditPaymentType(currentTypeId)) {
                    // 关联资产并且当前是还信用卡类型，只显示信用卡
                    result = result.filter { it.type == ClassificationTypeEnum.CREDIT_CARD_ACCOUNT }
                }
                if (selectedAssetId > 0L) {
                    // 移除已选择资产
                    result = result.filter { it.id != selectedAssetId }
                }
                result
            }
    }
}
