package cn.wj.android.cashbook.core.data.uitl

import kotlinx.coroutines.flow.Flow

/**
 * 报告同步状态
 */
interface SyncManager {

    val isSyncing: Flow<Boolean>

    fun requestSync()
}
