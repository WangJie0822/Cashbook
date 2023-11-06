package cn.wj.android.cashbook.core.common.di

import cn.wj.android.cashbook.core.common.ApplicationCoroutineScope
import cn.wj.android.cashbook.core.common.annotation.CashbookDispatchers
import cn.wj.android.cashbook.core.common.annotation.Dispatcher
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.Dispatchers

@Module
@InstallIn(SingletonComponent::class)
object DispatchersModule {

    @Provides
    @Dispatcher(CashbookDispatchers.Default)
    fun providesDefaultCoroutineContext(): CoroutineContext = Dispatchers.Default

    @Provides
    @Dispatcher(CashbookDispatchers.IO)
    fun providesIOCoroutineContext(): CoroutineContext = Dispatchers.IO

    @Provides
    @Singleton
    fun providesApplicationCoroutineScope(): ApplicationCoroutineScope = ApplicationCoroutineScope()
}
