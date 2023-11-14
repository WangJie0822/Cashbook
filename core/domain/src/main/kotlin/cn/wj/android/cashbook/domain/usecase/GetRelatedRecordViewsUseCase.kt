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
import cn.wj.android.cashbook.core.data.repository.RecordRepository
import cn.wj.android.cashbook.core.data.repository.TypeRepository
import cn.wj.android.cashbook.core.model.entity.RecordViewsEntity
import cn.wj.android.cashbook.core.model.model.RecordTypeModel
import cn.wj.android.cashbook.core.model.transfer.asEntity
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

/**
 * 获取关联记录数据用例
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/2/22
 */
class GetRelatedRecordViewsUseCase @Inject constructor(
    private val recordRepository: RecordRepository,
    private val typeRepository: TypeRepository,
    private val recordModelTransToViewsUseCase: RecordModelTransToViewsUseCase,
    @Dispatcher(CashbookDispatchers.IO) private val coroutineContext: CoroutineContext,
) {

    suspend operator fun invoke(
        keyword: String,
        recordTypeModel: RecordTypeModel?,
    ): List<RecordViewsEntity> = withContext(coroutineContext) {
        if (recordTypeModel == null) {
            emptyList()
        } else {
            val reimburse = typeRepository.isReimburseType(recordTypeModel.id)
            if (keyword.isBlank()) {
                // 没有关键字，获取最近三个月的数据
                if (reimburse) {
                    recordRepository.getLastThreeMonthReimbursableRecordList()
                } else {
                    recordRepository.getLastThreeMonthRefundableRecordList()
                }
            } else {
                if (reimburse) {
                    recordRepository.getLastThreeMonthReimbursableRecordListByKeyword("%$keyword%")
                } else {
                    recordRepository.getLastThreeMonthRefundableRecordListByKeyword("%$keyword%")
                }
            }.map {
                recordModelTransToViewsUseCase(it).asEntity()
            }
        }
    }
}
