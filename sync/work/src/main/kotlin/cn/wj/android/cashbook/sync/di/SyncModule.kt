package cn.wj.android.cashbook.sync.di

import cn.wj.android.cashbook.core.data.uitl.SyncManager
import cn.wj.android.cashbook.sync.status.WorkManagerSyncManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface SyncModule {

    @Binds
    fun bindSyncManager(
        syncManager: WorkManagerSyncManager,
    ): SyncManager
}
