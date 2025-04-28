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
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkerParameters
import cn.wj.android.cashbook.core.common.annotation.CashbookDispatchers
import cn.wj.android.cashbook.core.common.annotation.Dispatcher
import cn.wj.android.cashbook.core.common.ext.logger
import cn.wj.android.cashbook.core.data.uitl.BackupRecoveryManager
import cn.wj.android.cashbook.sync.initializers.syncForegroundInfo
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.time.Duration
import kotlin.coroutines.CoroutineContext

/**
 * 自动备份任务
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/8/8
 */
@HiltWorker
class AutoBackupWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val backupRecoveryManager: BackupRecoveryManager,
    @Dispatcher(CashbookDispatchers.IO) private val ioDispatcher: CoroutineContext,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun getForegroundInfo(): ForegroundInfo =
        appContext.syncForegroundInfo()

    override suspend fun doWork(): Result {
        this@AutoBackupWorker.logger().i("doWork(), requestAutoBackup")
        withContext(ioDispatcher) {
            delay(2_000L)
            backupRecoveryManager.requestAutoBackup()
        }
        return Result.success()
    }

    companion object {

        /** 启动单次备份任务 */
        fun startUpOneTimeBackupWork() = OneTimeWorkRequestBuilder<DelegatingWorker>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .setInputData(AutoBackupWorker::class.delegatedData())
            .build()

        /** 启动周期备份任务 */
        fun startUpPeriodicBackupWork(repeatInterval: Duration) =
            PeriodicWorkRequestBuilder<DelegatingWorker>(repeatInterval)
                .setInputData(AutoBackupWorker::class.delegatedData())
                .build()
    }
}
