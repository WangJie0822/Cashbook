package cn.wj.android.cashbook.sync.di

import cn.wj.android.cashbook.core.data.uitl.AppUpgradeManager
import cn.wj.android.cashbook.sync.util.WorkManagerAppUpgradeManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface SyncModule {

    @Binds
    fun bindAppUpgradeManager(
        workManagerAppUpgradeManager: WorkManagerAppUpgradeManager,
    ): AppUpgradeManager
}
