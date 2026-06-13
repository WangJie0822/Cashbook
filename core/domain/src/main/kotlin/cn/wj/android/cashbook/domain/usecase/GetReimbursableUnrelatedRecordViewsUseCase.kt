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
import cn.wj.android.cashbook.core.model.entity.RecordViewsEntity
import cn.wj.android.cashbook.core.model.transfer.asEntity
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

/**
 * 获取待报销（可报销且未关联任何报销/退款款）记录视图 + 汇总用例
 *
 * 数据已在 [RecordRepository.getReimbursableUnrelatedRecordList] 的 SQL NOT EXISTS 内过滤未关联，
 * 此处仅批量转换 + 聚合，不再逐条 filter。
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2026/6/13
 */
class GetReimbursableUnrelatedRecordViewsUseCase @Inject constructor(
    private val recordRepository: RecordRepository,
    private val recordModelTransToViewsUseCase: RecordModelTransToViewsUseCase,
    @Dispatcher(CashbookDispatchers.IO) private val coroutineContext: CoroutineContext,
) {

    suspend operator fun invoke(): ReimbursementListData = withContext(coroutineContext) {
        val records: List<RecordViewsEntity> = recordRepository.getReimbursableUnrelatedRecordList()
            .let { recordModelTransToViewsUseCase(it) }
            .map { it.asEntity() }
        ReimbursementListData(
            records = records,
            count = records.size,
            totalAmount = records.sumOf { it.finalAmount },
        )
    }
}

/**
 * 待报销列表数据 + 汇总
 *
 * @param records 待报销记录视图列表（时间倒序）
 * @param count 笔数
 * @param totalAmount 合计金额（单位：分，Σ finalAmount）
 */
data class ReimbursementListData(
    val records: List<RecordViewsEntity>,
    val count: Int,
    val totalAmount: Long,
)
