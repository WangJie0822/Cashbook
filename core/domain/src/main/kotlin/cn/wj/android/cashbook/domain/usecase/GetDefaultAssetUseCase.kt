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

import android.content.Context
import cn.wj.android.cashbook.core.common.ext.string
import cn.wj.android.cashbook.core.common.tools.dateFormat
import cn.wj.android.cashbook.core.data.helper.iconResId
import cn.wj.android.cashbook.core.data.helper.nameResId
import cn.wj.android.cashbook.core.data.repository.AssetRepository
import cn.wj.android.cashbook.core.model.enums.AssetClassificationEnum
import cn.wj.android.cashbook.core.model.enums.ClassificationTypeEnum
import cn.wj.android.cashbook.core.model.model.AssetModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Date
import javax.inject.Inject

/**
 * 获取默认记录数据用例
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/2/22
 */
class GetDefaultAssetUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val assetRepository: AssetRepository,
) {

    suspend operator fun invoke(assetId: Long): AssetModel {
        // appData 获取默认参数
        return assetRepository.getAssetById(assetId) ?: AssetModel(
            id = assetId,
            booksId = -1L,
            name = AssetClassificationEnum.CASH.nameResId.string(context),
            iconResId = AssetClassificationEnum.CASH.iconResId,
            totalAmount = "",
            billingDate = "",
            repaymentDate = "",
            type = ClassificationTypeEnum.CAPITAL_ACCOUNT,
            classification = AssetClassificationEnum.CASH,
            invisible = false,
            openBank = "",
            cardNo = "",
            remark = "",
            sort = 0,
            modifyTime = Date().dateFormat(),
            balance = "",
        )
    }
}
