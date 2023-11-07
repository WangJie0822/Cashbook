package cn.wj.android.cashbook.core.network.di

import cn.wj.android.cashbook.core.network.datasource.NetworkDataSource
import cn.wj.android.cashbook.core.network.datasource.RemoteDataSource
import cn.wj.android.cashbook.core.network.util.OkHttpWebDAVHandler
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
    fun bindNetworkMonitor(
        networkDataSource: NetworkDataSource
    ): RemoteDataSource

    @Binds
    fun bindWebDAVHandler(
        okhttpWebDAVHandler: OkHttpWebDAVHandler
    ): WebDAVHandler
}