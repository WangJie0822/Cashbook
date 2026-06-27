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
import androidx.paging.insertSeparators
import androidx.paging.map
import cn.wj.android.cashbook.core.common.DEFAULT_PAGE_SIZE
import cn.wj.android.cashbook.core.common.ext.logger
import cn.wj.android.cashbook.core.common.model.recordDataVersion
import cn.wj.android.cashbook.core.data.repository.SettingRepository
import cn.wj.android.cashbook.core.data.repository.TagRepository
import cn.wj.android.cashbook.core.data.repository.TypeRepository
import cn.wj.android.cashbook.core.model.entity.DateSelectionEntity
import cn.wj.android.cashbook.core.model.entity.RecordViewsEntity
import cn.wj.android.cashbook.core.model.entity.normalizeMonthStartDay
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.model.model.AssetMonthSummaryModel
import cn.wj.android.cashbook.domain.usecase.GetTagRecordViewsUseCase
import cn.wj.android.cashbook.domain.usecase.GetTypeRecordViewsUseCase
import cn.wj.android.cashbook.domain.usecase.GetTypedMonthSummaryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.YearMonth
import javax.inject.Inject

/**
 * 指定类型 / 标签的分析数据 ViewModel：月份切换器 + 收支结余汇总 + 按日分组。
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/10/27
 */
@HiltViewModel
class TypedAnalyticsViewModel @Inject constructor(
    private val typeRepository: TypeRepository,
    private val tagRepository: TagRepository,
    private val settingRepository: SettingRepository,
    private val getTypeRecordViewsUseCase: GetTypeRecordViewsUseCase,
    private val getTagRecordViewsUseCase: GetTagRecordViewsUseCase,
    private val getTypedMonthSummaryUseCase: GetTypedMonthSummaryUseCase,
) : ViewModel() {

    /** 需显示详情的记录数据 */
    var viewRecord by mutableStateOf<RecordViewsEntity?>(null)
        private set

    private val _tagIdData = MutableStateFlow(-1L)
    private val _typeIdData = MutableStateFlow(-1L)
    private val _includeChildTypes = MutableStateFlow(true)
    private val _dateSelection = MutableStateFlow<DateSelectionEntity>(
        DateSelectionEntity.ByMonth(YearMonth.now()),
    )
    val dateSelection: StateFlow<DateSelectionEntity> = _dateSelection

    /** 日期选择 Popup 是否展开（VM 持有，对齐 AnalyticsViewModel 范式） */
    var showDatePopup by mutableStateOf(false)
        private set

    /** 已应用的入口参数 key：避免 Route 重组时 apply{updateData} 重复执行把用户翻月结果重置回入口周期 */
    private var appliedDateKey: String? = null

    /** 月起始日（响应式）。归一化到 1..28，默认 1=自然月 */
    private val _monthStartDay = settingRepository.recordSettingsModel
        .map { normalizeMonthStartDay(it.monthStartDay) }
        .distinctUntilChanged()

    /** 月起始日（归一化后）供 UI 计算日期头是否跨自然月 */
    val monthStartDay: StateFlow<Int> = _monthStartDay
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), 1)

    /** 选择 + 月起始日 → (选择, [start,end) 毫秒区间)；列表与汇总同源，保证同口径（C1） */
    private val _selectionRange: kotlinx.coroutines.flow.Flow<Pair<DateSelectionEntity, Pair<Long, Long>>> =
        combine(_dateSelection, _monthStartDay) { selection, d -> selection to selection.toDateRange(d) }

    val uiState = combine(_tagIdData, _typeIdData) { tagId, typeId ->
        if (tagId == typeId) {
            TypedAnalyticsUiState.Loading
        } else {
            val isType = typeId != -1L
            val type = if (isType) typeRepository.getRecordTypeById(typeId) else null
            TypedAnalyticsUiState.Success(
                isType = isType,
                titleText = (if (isType) type?.name else tagRepository.getTagById(tagId)?.name).orEmpty(),
                isTransferType = isType && type?.typeCategory == RecordTypeCategoryEnum.TRANSFER,
            )
        }
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = TypedAnalyticsUiState.Loading,
        )

    /** 记录列表数据（按周期分页，按日插入 [LauncherListItem.DayHeader] 分组） */
    val recordList = combine(
        _tagIdData,
        _typeIdData,
        _includeChildTypes,
        _selectionRange,
        recordDataVersion,
    ) { tagId, typeId, includeChild, selRange, _ ->
        val isType = typeId != -1L
        val (start, end) = selRange.second
        GetTypedRecordData(
            isType = isType,
            id = if (isType) typeId else tagId,
            startDate = start,
            endDate = end,
            includeChildTypes = includeChild,
        )
    }
        .flatMapLatest { data ->
            Pager(
                config = PagingConfig(
                    pageSize = DEFAULT_PAGE_SIZE,
                    initialLoadSize = DEFAULT_PAGE_SIZE,
                ),
                pagingSourceFactory = {
                    if (data.isType) {
                        TypeRecordPagingSource(data.id, data.startDate, data.endDate, data.includeChildTypes, getTypeRecordViewsUseCase)
                    } else {
                        TagRecordPagingSource(data.id, data.startDate, data.endDate, getTagRecordViewsUseCase)
                    }
                },
            ).flow
                .map { paging -> paging.map { LauncherListItem.Record(it) as LauncherListItem } }
                .map { paging -> paging.insertSeparators { before, after -> recordDaySeparator(before, after) } }
        }
        .cachedIn(viewModelScope)

    /** 当前周期的收支结余汇总 */
    val summary: StateFlow<AssetMonthSummaryModel> = combine(
        _tagIdData,
        _typeIdData,
        _includeChildTypes,
        _selectionRange,
        recordDataVersion,
    ) { tagId, typeId, includeChild, selRange, _ ->
        val isType = typeId != -1L
        val id = if (isType) typeId else tagId
        if (id == -1L) {
            AssetMonthSummaryModel(0L, 0L, 0L)
        } else {
            val (start, end) = selRange.second
            getTypedMonthSummaryUseCase(isType, id, start, end, includeChild)
        }
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = AssetMonthSummaryModel(0L, 0L, 0L),
        )

    fun updateData(tagId: Long, typeId: Long, date: String, includeChildTypes: Boolean = true) {
        _tagIdData.tryEmit(tagId)
        _typeIdData.tryEmit(typeId)
        _includeChildTypes.tryEmit(includeChildTypes)
        // 仅在入口参数变化时初始化周期；重组导致的重复 updateData（相同入口）不重置，保住用户翻月结果
        val key = "$tagId|$typeId|$date"
        if (key != appliedDateKey) {
            appliedDateKey = key
            _dateSelection.tryEmit(
                DateSelectionEntity.fromDisplayTextOrNull(date) ?: DateSelectionEntity.ByMonth(YearMonth.now()),
            )
        }
        logger().i("updateData(tagId=<$tagId>, typeId=<$typeId>, date=<$date>, includeChildTypes=<$includeChildTypes>)")
    }

    /** 更新当前月份选择（月份模式翻月） */
    fun updateMonth(yearMonth: YearMonth) {
        _dateSelection.tryEmit(DateSelectionEntity.ByMonth(yearMonth))
    }

    /** 显示日期选择 Popup */
    fun displayDatePopup() {
        showDatePopup = true
    }

    /** 隐藏日期选择 Popup */
    fun dismissDatePopup() {
        showDatePopup = false
    }

    /** 更新日期选择（全部/年/月/时间段/按日） */
    fun updateDateSelection(selection: DateSelectionEntity) {
        _dateSelection.tryEmit(selection)
    }

    fun showRecordDetailsSheet(item: RecordViewsEntity) {
        viewRecord = item
    }

    fun dismissRecordDetailSheet() {
        viewRecord = null
    }
}

data class GetTypedRecordData(
    val isType: Boolean,
    val id: Long,
    val startDate: Long,
    val endDate: Long,
    val includeChildTypes: Boolean,
)

sealed interface TypedAnalyticsUiState {
    data object Loading : TypedAnalyticsUiState
    data class Success(
        val isType: Boolean,
        val titleText: String,
        val isTransferType: Boolean,
    ) : TypedAnalyticsUiState
}

/**
 * Paging 数据仓库 - 按类型
 */
private class TypeRecordPagingSource(
    private val typeId: Long,
    private val startDate: Long,
    private val endDate: Long,
    private val includeChildTypes: Boolean,
    private val getTypeRecordViewsUseCase: GetTypeRecordViewsUseCase,
) : PagingSource<Int, RecordViewsEntity>() {
    override fun getRefreshKey(state: PagingState<Int, RecordViewsEntity>): Int? = null

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, RecordViewsEntity> {
        return runCatching {
            val page = params.key ?: 0
            val pageSize = params.loadSize
            // 统一走毫秒半开区间（含可配置月起始日），与汇总同口径（C1）；All 态区间为 [0, Long.MAX_VALUE)
            val items = getTypeRecordViewsUseCase(typeId, startDate, endDate, page, pageSize, includeChildTypes)
            val prevKey = if (page > 0) page - 1 else null
            val nextKey = if (items.isNotEmpty()) page + 1 else null
            LoadResult.Page(items, prevKey, nextKey)
        }.getOrElse { throwable ->
            logger().e(throwable, "load()")
            LoadResult.Error(throwable)
        }
    }
}

/**
 * Paging 数据仓库 - 按标签
 */
private class TagRecordPagingSource(
    private val tagId: Long,
    private val startDate: Long,
    private val endDate: Long,
    private val getTagRecordViewsUseCase: GetTagRecordViewsUseCase,
) : PagingSource<Int, RecordViewsEntity>() {
    override fun getRefreshKey(state: PagingState<Int, RecordViewsEntity>): Int? = null

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, RecordViewsEntity> {
        return runCatching {
            val page = params.key ?: 0
            val pageSize = params.loadSize
            val items = getTagRecordViewsUseCase(tagId, startDate, endDate, page, pageSize)
            val prevKey = if (page > 0) page - 1 else null
            val nextKey = if (items.isNotEmpty()) page + 1 else null
            LoadResult.Page(items, prevKey, nextKey)
        }.getOrElse { throwable ->
            logger().e(throwable, "load()")
            LoadResult.Error(throwable)
        }
    }
}
