package cn.wj.android.cashbook.core.network.di

import cn.wj.android.cashbook.core.network.datasource.OfflineDataSource
import cn.wj.android.cashbook.core.network.datasource.RemoteDataSource
import cn.wj.android.cashbook.core.network.util.OfflineWebDAVHandler
import cn.wj.android.cashbook.core.network.util.WebDAVHandler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Suppress("unused")
@Module
@InstallIn(SingletonComponent::class)
interface DataSourceModule {

    @Binds
    fun bindRemoteDataSource(
        offlineDataSource: OfflineDataSource
    ): RemoteDataSource

    @Binds
    fun bindWebDAVHandler(
        offlineWebDAVHandler: OfflineWebDAVHandler
    ): WebDAVHandler
}