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
import cn.wj.android.cashbook.core.data.repository.ScheduleRepository
import cn.wj.android.cashbook.core.model.model.ScheduleModel
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class SaveScheduleUseCase @Inject constructor(
    private val scheduleRepository: ScheduleRepository,
    @Dispatcher(CashbookDispatchers.IO) private val coroutineContext: CoroutineContext,
) {

    suspend operator fun invoke(schedule: ScheduleModel) = withContext(coroutineContext) {
        require(schedule.amount > 0) { "金额必须大于 0" }
        require(schedule.typeId > 0) { "必须选择类型" }
        require(schedule.startDate > 0) { "开始日期无效" }
        require(schedule.recordTime > 0) { "记账时间无效" }
        scheduleRepository.saveSchedule(schedule)
    }
}
