/*
 * Copyright 2021 The Cashbook Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.wj.android.cashbook.feature.records.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.cachedIn
import cn.wj.android.cashbook.core.common.DEFAULT_PAGE_SIZE
import cn.wj.android.cashbook.core.common.ext.logger
import cn.wj.android.cashbook.core.common.model.recordDataVersion
import cn.wj.android.cashbook.core.data.repository.RecordRepository
import cn.wj.android.cashbook.core.model.entity.RecordViewsEntity
import cn.wj.android.cashbook.domain.usecase.GetSearchRecordViewsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 搜索 ViewModel
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/10/19
 */
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val recordRepository: RecordRepository,
    private val getSearchRecordViewsUseCase: GetSearchRecordViewsUseCase,
) : ViewModel() {

    /** 需显示详情的记录数据 */
    var viewRecordData by mutableStateOf<RecordViewsEntity?>(null)
        private set

    private val _keywordData = MutableStateFlow("")

    val searchHistoryListData = recordRepository.searchHistoryListData
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = emptyList(),
        )

    /** 记录列表数据 */
    val recordListData = combine(_keywordData, recordDataVersion) { keyword, _ ->
        keyword
    }
        .flatMapLatest {
            Pager(
                config = PagingConfig(
                    pageSize = DEFAULT_PAGE_SIZE,
                    initialLoadSize = DEFAULT_PAGE_SIZE,
                ),
                pagingSourceFactory = {
                    SearchRecordPagingSource(it, getSearchRecordViewsUseCase)
                },
            ).flow
        }
        .cachedIn(viewModelScope)

    fun onKeywordChange(keyword: String) {
        viewModelScope.launch {
            _keywordData.tryEmit(keyword)
            delay(500L)
            recordRepository.addSearchHistory(keyword)
        }
    }

    fun showRecordDetailSheet(item: RecordViewsEntity) {
        viewRecordData = item
    }

    fun dismissRecordDetailSheet() {
        viewRecordData = null
    }

    fun clearSearchHistory() {
        viewModelScope.launch {
            recordRepository.clearSearchHistory()
        }
    }
}

/**
 * Paging 数据仓库
 */
private class SearchRecordPagingSource(
    private val keyword: String,
    private val getSearchRecordViewsUseCase: GetSearchRecordViewsUseCase,
) : PagingSource<Int, RecordViewsEntity>() {
    override fun getRefreshKey(state: PagingState<Int, RecordViewsEntity>): Int? = null

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, RecordViewsEntity> {
        return runCatching {
            val page = params.key ?: 0
            val pageSize = params.loadSize
            val items = getSearchRecordViewsUseCase(keyword, page, pageSize)
            val prevKey = if (page > 0) page - 1 else null
            val nextKey = if (items.isNotEmpty()) page + 1 else null
            LoadResult.Page(items, prevKey, nextKey)
        }.getOrElse { throwable ->
            logger().e(throwable, "load()")
            LoadResult.Error(throwable)
        }
    }
}
