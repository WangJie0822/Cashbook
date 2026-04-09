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

import cn.wj.android.cashbook.core.common.NO_ASSET_ID
import cn.wj.android.cashbook.core.common.annotation.CashbookDispatchers
import cn.wj.android.cashbook.core.common.annotation.Dispatcher
import cn.wj.android.cashbook.core.data.repository.RecordRepository
import cn.wj.android.cashbook.core.data.repository.TypeRepository
import cn.wj.android.cashbook.core.model.model.ImageModel
import cn.wj.android.cashbook.core.model.model.RecordModel
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class SaveRecordUseCase @Inject constructor(
    private val recordRepository: RecordRepository,
    private val typeRepository: TypeRepository,
    @Dispatcher(CashbookDispatchers.IO) private val coroutineContext: CoroutineContext,
) {

    suspend operator fun invoke(
        recordModel: RecordModel,
        tagIdList: List<Long>,
        relatedRecordIdList: List<Long>,
        relatedImageList: List<ImageModel>,
    ) = withContext(coroutineContext) {
        // 输入校验
        require(recordModel.amount > 0) { "金额必须大于 0" }
        require(recordModel.charges >= 0) { "手续费不能为负数" }
        require(recordModel.concessions >= 0) { "优惠不能为负数" }
        require(recordModel.recordTime > 0) { "记录时间无效" }
        // 处理无效 assetId：历史版本升级可能导致 assetId 为 0 或其他无效值
        // 将其统一设为 NO_ASSET_ID（-1L），表示不关联资产
        val normalizedRecord = if (recordModel.assetId > 0 || recordModel.assetId == NO_ASSET_ID) {
            recordModel
        } else {
            recordModel.copy(assetId = NO_ASSET_ID)
        }
        // 向数据库内更新最新记录信息及关联信息
        val needRelated = typeRepository.needRelated(normalizedRecord.typeId)
        recordRepository.updateRecord(
            record = normalizedRecord,
            tagIdList = tagIdList,
            needRelated = needRelated,
            relatedRecordIdList = relatedRecordIdList,
            relatedImageList = relatedImageList,
        )
    }
}
