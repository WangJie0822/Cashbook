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
import cn.wj.android.cashbook.core.data.repository.SettingRepository
import cn.wj.android.cashbook.sync.initializers.NetworkConstraints
import cn.wj.android.cashbook.sync.initializers.syncForegroundInfo
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import java.time.Duration
import kotlin.coroutines.CoroutineContext

/**
 * 数据同步 Worker
 * - 有网络时每天执行一次数据同步
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/7/18
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val settingRepository: SettingRepository,
    @Dispatcher(CashbookDispatchers.IO) private val ioDispatcher: CoroutineContext,
) : CoroutineWorker(appContext, workerParams) {

    private var retryCount = 0

    override suspend fun getForegroundInfo(): ForegroundInfo =
        appContext.syncForegroundInfo()

    override suspend fun doWork(): Result = withContext(ioDispatcher) {
        this@SyncWorker.logger().i("doWork(), sync data")
        val syncedSuccessfully = awaitAll(
            async { settingRepository.syncLatestVersion() },
        ).all { it }
        if (syncedSuccessfully) {
            this@SyncWorker.logger().i("doWork(), sync success")
            Result.success()
        } else {
            retryCount++
            if (retryCount >= 5) {
                this@SyncWorker.logger().i("doWork(), sync failed, finish")
                Result.failure()
            } else {
                this@SyncWorker.logger().i("doWork(), sync failed, retry $retryCount")
                Result.retry()
            }
        }
    }

    companion object {

        fun startUpPeriodicSyncWork() =
            PeriodicWorkRequestBuilder<DelegatingWorker>(Duration.ofDays(1))
                .setConstraints(NetworkConstraints)
                .setInputData(SyncWorker::class.delegatedData())
                .build()
    }
}
