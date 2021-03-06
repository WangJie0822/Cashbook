package cn.wj.android.cashbook.data.source

import androidx.paging.PagingSource
import androidx.paging.PagingState
import cn.wj.android.cashbook.data.entity.DateRecordEntity
import cn.wj.android.cashbook.data.repository.asset.AssetRepository

/**
 * 资产相关记录数据源
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/15
 */
class AssetRecordPagingSource(private val assetId: Long, private val repository: AssetRepository) : PagingSource<Int, DateRecordEntity>() {

    override fun getRefreshKey(state: PagingState<Int, DateRecordEntity>): Int? {
        return null
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, DateRecordEntity> {
        return try {
            val page = params.key ?: 0
            val pageSize = params.loadSize
            val repoItems = repository.getRecordByAssetId(assetId, page, pageSize)
            val prevKey = if (page > 0) page - 1 else null
            val nextKey = if (repoItems.isNotEmpty()) page + 1 else null
            LoadResult.Page(repoItems, prevKey, nextKey)
        } catch (throwable: Throwable) {
            LoadResult.Error(throwable)
        }
    }
}