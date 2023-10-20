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
import cn.wj.android.cashbook.core.data.repository.SettingRepository
import cn.wj.android.cashbook.sync.initializers.NetworkConstraints
import cn.wj.android.cashbook.sync.initializers.syncForegroundInfo
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.Duration
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext

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
            async { settingRepository.syncChangelog() },
            async { settingRepository.syncPrivacyPolicy() },
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

        /** 使用代理任务启动同步任务，以支持依赖注入 */
        fun startUpOneTimeSyncWork() = OneTimeWorkRequestBuilder<DelegatingWorker>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .setConstraints(NetworkConstraints)
            .setInputData(SyncWorker::class.delegatedData())
            .build()

        fun startUpPeriodicSyncWork() =
            PeriodicWorkRequestBuilder<DelegatingWorker>(Duration.ofDays(1))
                .setConstraints(NetworkConstraints)
                .setInputData(SyncWorker::class.delegatedData())
                .build()
    }
}