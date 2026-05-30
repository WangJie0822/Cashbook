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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.cachedIn
import androidx.paging.insertSeparators
import androidx.paging.map
import cn.wj.android.cashbook.core.common.DEFAULT_PAGE_SIZE
import cn.wj.android.cashbook.core.common.ext.logger
import cn.wj.android.cashbook.core.common.model.recordDataVersion
import cn.wj.android.cashbook.core.model.entity.DateSelectionEntity
import cn.wj.android.cashbook.core.model.entity.RecordViewsEntity
import cn.wj.android.cashbook.core.model.model.AssetMonthSummaryModel
import cn.wj.android.cashbook.domain.usecase.GetAssetMonthSummaryUseCase
import cn.wj.android.cashbook.domain.usecase.GetAssetRecordViewsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.YearMonth
import javax.inject.Inject

/**
 * 资产信息页记录数据 ViewModel
 *
 * @param getAssetRecordViewsUseCase 获取对应资产记录数据用例
 * @param getAssetMonthSummaryUseCase 获取资产月度收支结余用例
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/7/3
 */
@HiltViewModel
class AssetInfoContentViewModel @Inject constructor(
    getAssetRecordViewsUseCase: GetAssetRecordViewsUseCase,
    private val getAssetMonthSummaryUseCase: GetAssetMonthSummaryUseCase,
) : ViewModel() {

    /** 资产 id 数据 */
    private val _assetIdData = MutableStateFlow(-1L)

    /** 当前资产是否为信用卡（影响收支方向） */
    private val _isCreditCard = MutableStateFlow(false)

    /** 当前月份选择，默认当前月 */
    private val _dateSelection = MutableStateFlow<DateSelectionEntity>(
        DateSelectionEntity.ByMonth(YearMonth.now()),
    )
    val dateSelection: StateFlow<DateSelectionEntity> = _dateSelection

    /** 记录列表数据（按资产 + 当前月份范围分页，并按日插入 [LauncherListItem.DayHeader] 分组） */
    val recordList = combine(_assetIdData, _dateSelection, recordDataVersion) { assetId, selection, _ ->
        assetId to selection.toDateRange()
    }
        .flatMapLatest { (assetId, range) ->
            Pager(
                config = PagingConfig(
                    pageSize = DEFAULT_PAGE_SIZE,
                    initialLoadSize = DEFAULT_PAGE_SIZE,
                ),
                pagingSourceFactory = {
                    AssetRecordPagingSource(
                        assetId = assetId,
                        startDate = range.first,
                        endDate = range.second,
                        getAssetRecordViewsUseCase = getAssetRecordViewsUseCase,
                    )
                },
            ).flow
                .map { pagingData ->
                    pagingData.map { LauncherListItem.Record(it) as LauncherListItem }
                }
                .map { pagingData ->
                    pagingData.insertSeparators { before, after ->
                        recordDaySeparator(before, after)
                    }
                }
        }
        .cachedIn(viewModelScope)

    /** 当前月份的资产收支结余汇总 */
    val summary: StateFlow<AssetMonthSummaryModel> =
        combine(_assetIdData, _isCreditCard, _dateSelection, recordDataVersion) { id, isCreditCard, selection, _ ->
            val (startDate, endDate) = selection.toDateRange()
            getAssetMonthSummaryUseCase(id, isCreditCard, startDate, endDate)
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = AssetMonthSummaryModel(0L, 0L, 0L),
            )

    /** 更新资产 id */
    fun updateAssetId(id: Long) {
        _assetIdData.tryEmit(id)
    }

    /** 更新当前资产是否为信用卡 */
    fun updateIsCreditCard(value: Boolean) {
        _isCreditCard.tryEmit(value)
    }

    /** 更新当前月份选择 */
    fun updateMonth(yearMonth: YearMonth) {
        _dateSelection.tryEmit(DateSelectionEntity.ByMonth(yearMonth))
    }
}

/**
 * Paging 数据仓库
 */
private class AssetRecordPagingSource(
    private val assetId: Long,
    private val startDate: Long,
    private val endDate: Long,
    private val getAssetRecordViewsUseCase: GetAssetRecordViewsUseCase,
) : PagingSource<Int, RecordViewsEntity>() {
    override fun getRefreshKey(state: PagingState<Int, RecordViewsEntity>): Int? = null

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, RecordViewsEntity> {
        return runCatching {
            val page = params.key ?: 0
            val pageSize = params.loadSize
            val items = getAssetRecordViewsUseCase(assetId, startDate, endDate, page, pageSize)
            val prevKey = if (page > 0) page - 1 else null
            val nextKey = if (items.isNotEmpty()) page + 1 else null
            LoadResult.Page(items, prevKey, nextKey)
        }.getOrElse { throwable ->
            logger().e(throwable, "load()")
            LoadResult.Error(throwable)
        }
    }
}
