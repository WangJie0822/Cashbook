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
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import cn.wj.android.cashbook.core.common.annotation.CashbookDispatchers
import cn.wj.android.cashbook.core.common.annotation.Dispatcher
import cn.wj.android.cashbook.core.common.ext.logger
import cn.wj.android.cashbook.core.data.repository.SettingRepository
import cn.wj.android.cashbook.core.model.enums.AutoBackupModeEnum
import cn.wj.android.cashbook.sync.initializers.AutoBackupWorkName
import cn.wj.android.cashbook.sync.initializers.SyncWorkName
import cn.wj.android.cashbook.sync.initializers.syncForegroundInfo
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.Duration
import kotlin.coroutines.CoroutineContext

/**
 * 初始化 [CoroutineWorker]，按照用户配置执行不同的任务
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/8/8
 */
@HiltWorker
class InitWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val settingRepository: SettingRepository,
    @Dispatcher(CashbookDispatchers.IO) private val ioDispatcher: CoroutineContext,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun getForegroundInfo(): ForegroundInfo =
        appContext.syncForegroundInfo()

    override suspend fun doWork(): Result = withContext(ioDispatcher) {
        this@InitWorker.logger().i("doWork(), init worker")
        settingRepository.appSettingsModel.first().let { appDateModel ->
            WorkManager.getInstance(appContext).apply {
                // 执行数据同步
                if (appDateModel.autoCheckUpdate) {
                    // 自动检测更新
                    enqueueUniquePeriodicWork(
                        SyncWorkName,
                        ExistingPeriodicWorkPolicy.UPDATE,
                        SyncWorker.startUpPeriodicSyncWork(),
                    )
                }
                // 自动备份
                when (appDateModel.autoBackup) {
                    AutoBackupModeEnum.WHEN_LAUNCH -> {
                        // 每次启动，先取消任务，再重新启动
                        cancelUniqueWork(AutoBackupWorkName)
                        enqueueUniqueWork(
                            AutoBackupWorkName,
                            ExistingWorkPolicy.KEEP,
                            AutoBackupWorker.startUpOneTimeBackupWork(),
                        )
                    }

                    AutoBackupModeEnum.EACH_DAY -> {
                        // 每天
                        enqueueUniquePeriodicWork(
                            AutoBackupWorkName,
                            ExistingPeriodicWorkPolicy.UPDATE,
                            AutoBackupWorker.startUpPeriodicBackupWork(Duration.ofDays(1)),
                        )
                    }

                    AutoBackupModeEnum.EACH_WEEK -> {
                        // 每周
                        enqueueUniquePeriodicWork(
                            AutoBackupWorkName,
                            ExistingPeriodicWorkPolicy.UPDATE,
                            AutoBackupWorker.startUpPeriodicBackupWork(Duration.ofDays(7)),
                        )
                    }

                    AutoBackupModeEnum.CLOSE -> {
                        // 关闭，清除任务
                        cancelUniqueWork(AutoBackupWorkName)
                    }
                }
            }
        }
        Result.success()
    }

    companion object {

        /** 使用代理任务启动初始化任务，以支持依赖注入 */
        fun startUpInitWork() = OneTimeWorkRequestBuilder<DelegatingWorker>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .setInputData(InitWorker::class.delegatedData())
            .build()
    }
}
