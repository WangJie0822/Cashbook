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

package cn.wj.android.cashbook.sync.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkerParameters
import cn.wj.android.cashbook.core.common.annotation.CashbookDispatchers
import cn.wj.android.cashbook.core.common.annotation.Dispatcher
import cn.wj.android.cashbook.core.common.ext.logger
import cn.wj.android.cashbook.domain.usecase.GenerateScheduleRecordsUseCase
import cn.wj.android.cashbook.sync.initializers.syncForegroundInfo
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.withContext
import java.time.Duration
import kotlin.coroutines.CoroutineContext

/**
 * 周期记账生成 Worker
 * - 每天执行一次，检查并生成到期的周期记账记录
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2026/4/20
 */
@HiltWorker
class ScheduleWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val generateScheduleRecordsUseCase: GenerateScheduleRecordsUseCase,
    @Dispatcher(CashbookDispatchers.IO) private val ioDispatcher: CoroutineContext,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun getForegroundInfo(): ForegroundInfo =
        appContext.syncForegroundInfo()

    override suspend fun doWork(): Result = withContext(ioDispatcher) {
        this@ScheduleWorker.logger().i("doWork(), schedule worker")
        try {
            val count = generateScheduleRecordsUseCase()
            this@ScheduleWorker.logger().i("doWork(), generated $count schedule records")
            Result.success()
        } catch (e: Exception) {
            this@ScheduleWorker.logger().e(e, "doWork(), schedule worker failed")
            Result.retry()
        }
    }

    companion object {

        fun startUpPeriodicScheduleWork() =
            PeriodicWorkRequestBuilder<DelegatingWorker>(Duration.ofDays(1))
                .setInputData(ScheduleWorker::class.delegatedData())
                .build()
    }
}
