package cn.wj.android.cashbook.core.data.di

import cn.wj.android.cashbook.core.data.repository.AssetRepository
import cn.wj.android.cashbook.core.data.repository.RecordRepository
import cn.wj.android.cashbook.core.data.repository.TagRepository
import cn.wj.android.cashbook.core.data.repository.TypeRepository
import cn.wj.android.cashbook.core.data.repository.impl.AssetRepositoryImpl
import cn.wj.android.cashbook.core.data.repository.impl.RecordRepositoryImpl
import cn.wj.android.cashbook.core.data.repository.impl.TagRepositoryImpl
import cn.wj.android.cashbook.core.data.repository.impl.TypeRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface DataModule {

    @Binds
    fun bindTypeRepository(
        repository: TypeRepositoryImpl
    ): TypeRepository

    @Binds
    fun bindTagRepository(
        repository: TagRepositoryImpl
    ): TagRepository

    @Binds
    fun bindAssetRepository(
        repository: AssetRepositoryImpl
    ): AssetRepository

    @Binds
    fun bindRecordRepository(
        repository: RecordRepositoryImpl
    ): RecordRepository
}