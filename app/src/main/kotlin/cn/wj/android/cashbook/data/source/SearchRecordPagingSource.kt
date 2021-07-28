package cn.wj.android.cashbook.data.source

import androidx.paging.PagingSource
import androidx.paging.PagingState
import cn.wj.android.cashbook.data.entity.RecordEntity
import cn.wj.android.cashbook.data.repository.record.RecordRepository

/**
 * 搜索记录数据源
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/7/23
 */
class SearchRecordPagingSource(private val keywords: String, private val repository: RecordRepository) : PagingSource<Int, RecordEntity>() {

    override fun getRefreshKey(state: PagingState<Int, RecordEntity>): Int? {
        return null
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, RecordEntity> {
        return try {
            val page = params.key ?: 0
            val pageSize = params.loadSize
            val repoItems = if (keywords.isBlank()) {
                arrayListOf()
            } else {
                repository.getRecordByKeywords("%$keywords%", page, pageSize)
            }
            val prevKey = if (page > 0) page - 1 else null
            val nextKey = if (repoItems.isNotEmpty()) page + 1 else null
            LoadResult.Page(repoItems, prevKey, nextKey)
        } catch (throwable: Throwable) {
            LoadResult.Error(throwable)
        }
    }
}