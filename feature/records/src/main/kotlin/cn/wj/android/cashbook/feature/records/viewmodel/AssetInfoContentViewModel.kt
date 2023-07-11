package cn.wj.android.cashbook.feature.records.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.cachedIn
import cn.wj.android.cashbook.core.common.model.recordDataVersion
import cn.wj.android.cashbook.core.model.entity.RecordViewsEntity
import cn.wj.android.cashbook.domain.usecase.GetAssetRecordViewsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest

/**
 * 资产信息页记录数据 ViewModel
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/7/3
 */
@HiltViewModel
class AssetInfoContentViewModel @Inject constructor(
    getAssetRecordViewsUseCase: GetAssetRecordViewsUseCase
) : ViewModel() {

    private val assetIdData = MutableStateFlow(-1L)

    val recordList = combine(assetIdData, recordDataVersion) { assetId, _ ->
        assetId
    }.flatMapLatest {
        Pager(
            config = PagingConfig(pageSize = 20, initialLoadSize = 20),
            pagingSourceFactory = {
                AssetRecordPagingSource(it, getAssetRecordViewsUseCase)
            },
        ).flow.cachedIn(viewModelScope)
    }

    fun updateAssetId(id: Long) {
        assetIdData.tryEmit(id)
    }
}

private class AssetRecordPagingSource(
    private val assetId: Long,
    private val getAssetRecordViewsUseCase: GetAssetRecordViewsUseCase
) : PagingSource<Int, RecordViewsEntity>() {
    override fun getRefreshKey(state: PagingState<Int, RecordViewsEntity>): Int? = null

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, RecordViewsEntity> {
        return try {
            val page = params.key ?: 0
            val pageSize = params.loadSize
            val items = getAssetRecordViewsUseCase(assetId, page, pageSize)
            val prevKey = if (page > 0) page - 1 else null
            val nextKey = if (items.isNotEmpty()) page + 1 else null
            LoadResult.Page(items, prevKey, nextKey)
        } catch (throwable: Throwable) {
            LoadResult.Error(throwable)
        }
    }
}