package cn.wj.android.cashbook.sync.initializers

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import cn.wj.android.cashbook.sync.workers.SyncWorker

/**
 * 数据同步对外接口
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/7/12
 */
object Sync {

    /** 初始化数据同步 */
    fun initialize(context: Context) {
        WorkManager.getInstance(context).apply {
            // 执行同步任务
            enqueueUniqueWork(
                SyncWorkName,
                ExistingWorkPolicy.KEEP,
                SyncWorker.startUpSyncWork(),
            )
        }
    }
}

internal const val SyncWorkName = "SyncWorkName"
