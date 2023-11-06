package cn.wj.android.cashbook.sync.di

import cn.wj.android.cashbook.core.data.uitl.AppUpgradeManager
import cn.wj.android.cashbook.core.data.uitl.SyncManager
import cn.wj.android.cashbook.sync.util.WorkManagerAppUpgradeManager
import cn.wj.android.cashbook.sync.util.WorkManagerSyncManager
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

    @Binds
    fun bindAppUpgradeManager(
        upgradeManager: WorkManagerAppUpgradeManager,
    ): AppUpgradeManager
}
