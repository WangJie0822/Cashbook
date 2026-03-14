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
import cn.wj.android.cashbook.core.common.ext.yearMonth
import cn.wj.android.cashbook.core.common.tools.toMs
import cn.wj.android.cashbook.core.data.repository.RecordRepository
import cn.wj.android.cashbook.core.model.model.RecordViewsModel
import kotlinx.coroutines.withContext
import java.time.LocalDate
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

/**
 * 获取指定时间区间内的记录
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/10/23
 */
class GetRecordViewsBetweenDateUseCase @Inject constructor(
    private val recordRepository: RecordRepository,
    private val recordModelTransToViewsUseCase: RecordModelTransToViewsUseCase,
    @Dispatcher(CashbookDispatchers.IO) private val coroutineContext: CoroutineContext,
) {

    suspend operator fun invoke(
        fromDate: LocalDate,
        toDate: LocalDate?,
        yearSelected: Boolean,
    ): List<RecordViewsModel> = withContext(coroutineContext) {
        val from: Long
        val to: Long
        when {
            yearSelected -> {
                from = LocalDate.of(fromDate.year, 1, 1).toMs()
                // 下一年的第一天零点 - 1ms 即为本年最后一刻
                to = LocalDate.of(fromDate.year + 1, 1, 1).toMs() - 1L
            }

            null == toDate -> {
                from = LocalDate.of(fromDate.year, fromDate.monthValue, 1).toMs()
                // 下月第一天零点 - 1ms
                to = fromDate.yearMonth.atEndOfMonth().plusDays(1).toMs() - 1L
            }

            else -> {
                from = fromDate.toMs()
                // 结束日期的次日零点 - 1ms
                to = toDate.plusDays(1).toMs() - 1L
            }
        }
        recordRepository.queryRecordListBetweenDate(from, to)
            .map {
                recordModelTransToViewsUseCase(it)
            }
    }
}
