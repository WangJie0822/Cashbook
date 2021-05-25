package cn.wj.android.cashbook.data.source

import androidx.paging.PagingSource
import androidx.paging.PagingState

/**
 * 账单数据源
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/5/25
 */
class BillPagingSource() : PagingSource<Int, String>() {

    override fun getRefreshKey(state: PagingState<Int, String>): Int? {
        return null
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, String> {
        return try {
            val page = params.key ?: 1
            val pageSize = params.loadSize
            val repoItems = arrayListOf<String>().apply {
                (0 until pageSize).forEach {
                    add("page: $page - item: $it")
                }
            }
            val prevKey = if (page > 1) page - 1 else null
            val nextKey = if (repoItems.isNotEmpty()) page + 1 else null
            LoadResult.Page(repoItems, prevKey, nextKey)
        } catch (throwable: Throwable) {
            LoadResult.Error(throwable)
        }
    }
}