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
import androidx.annotation.VisibleForTesting
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.ListenableWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkerParameters
import cn.wj.android.cashbook.core.common.annotation.CashbookDispatchers
import cn.wj.android.cashbook.core.common.annotation.Dispatcher
import cn.wj.android.cashbook.core.common.ext.logger
import cn.wj.android.cashbook.core.data.uitl.BackupRecoveryManager
import cn.wj.android.cashbook.core.data.uitl.BackupRecoveryState
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
        val state = withContext(ioDispatcher) {
            delay(2_000L)
            backupRecoveryManager.requestAutoBackup()
        }
        // 据备份终态返回:成功→success,WebDAV 网络类→retry,配置类→failure。
        // 不附带 outputData(避免备份路径/异常信息泄露)。
        return mapBackupStateToResult(state)
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

/**
 * 将备份终态映射为 WorkManager 执行结果（抽为顶层纯函数以便纯 JVM 单测，无需 Robolectric）。
 * - 成功 → success；
 * - WebDAV 网络类失败 → retry（WorkManager 默认指数退避，30s 起）；
 * - 配置类失败（空路径/未授权/格式错等）→ failure（不重试，需用户修复配置）。
 *
 * 已知局限（#10a）：startBackup 的 catch-all 把瞬时 IO 异常也归 FAILED_BACKUP_PATH_UNAUTHORIZED，
 * 这类会被判 failure 不重试；本次不细化异常分类（范围控制）。
 */
@VisibleForTesting
internal fun mapBackupStateToResult(state: BackupRecoveryState): ListenableWorker.Result = when {
    state is BackupRecoveryState.Success -> ListenableWorker.Result.success()
    state is BackupRecoveryState.Failed &&
        state.code == BackupRecoveryState.FAILED_BACKUP_WEBDAV -> ListenableWorker.Result.retry()
    else -> ListenableWorker.Result.failure()
}
