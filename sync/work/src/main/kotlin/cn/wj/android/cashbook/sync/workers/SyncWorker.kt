package cn.wj.android.cashbook.sync.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkerParameters
import cn.wj.android.cashbook.core.common.annotation.CashbookDispatchers
import cn.wj.android.cashbook.core.common.annotation.Dispatcher
import cn.wj.android.cashbook.core.common.ext.logger
import cn.wj.android.cashbook.core.common.tools.DATE_FORMAT_DATE
import cn.wj.android.cashbook.core.common.tools.dateFormat
import cn.wj.android.cashbook.core.data.repository.SettingRepository
import cn.wj.android.cashbook.sync.initializers.SyncConstraints
import cn.wj.android.cashbook.sync.initializers.syncForegroundInfo
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
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
        val currentDate = System.currentTimeMillis().dateFormat(DATE_FORMAT_DATE)
        val syncDate = settingRepository.appDataMode.first().syncDate
        if (syncDate.isNotBlank() && currentDate == syncDate) {
            // 今天未同步
            val syncedSuccessfully = awaitAll(
                async { settingRepository.syncChangelog() },
                async { settingRepository.syncPrivacyPolicy() },
                async { settingRepository.syncLatestVersion() },
            ).all { it }
            if (syncedSuccessfully) {
                settingRepository.updateSyncDate(currentDate)
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
        } else {
            // 今天已同步，返回成功
            this@SyncWorker.logger().i("doWork(), sync already")
            Result.success()
        }
    }

    companion object {

        /** 使用代理任务启动同步任务，以支持依赖注入 */
        fun startUpSyncWork() = OneTimeWorkRequestBuilder<DelegatingWorker>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .setConstraints(SyncConstraints)
            .setInputData(SyncWorker::class.delegatedData())
            .build()
    }
}