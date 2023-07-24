package cn.wj.android.cashbook.sync.util

import android.content.Context
import androidx.lifecycle.asFlow
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import cn.wj.android.cashbook.core.data.uitl.SyncManager
import cn.wj.android.cashbook.sync.initializers.SyncWorkName
import cn.wj.android.cashbook.sync.workers.SyncWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.map

/**
 * 同步状态管理
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/7/14
 */
class WorkManagerSyncManager @Inject constructor(
    @ApplicationContext private val context: Context
) : SyncManager {

    override val isSyncing: Flow<Boolean> =
        WorkManager.getInstance(context).getWorkInfosForUniqueWorkLiveData(SyncWorkName)
            .asFlow()
            .map(List<WorkInfo>::anyRunning)
            .conflate()

    override fun requestSync() {
        val workManager = WorkManager.getInstance(context)
        workManager.enqueueUniqueWork(
            SyncWorkName,
            ExistingWorkPolicy.KEEP,
            SyncWorker.startUpSyncWork(),
        )
    }
}

private fun List<WorkInfo>.anyRunning() = any { it.state == WorkInfo.State.RUNNING }