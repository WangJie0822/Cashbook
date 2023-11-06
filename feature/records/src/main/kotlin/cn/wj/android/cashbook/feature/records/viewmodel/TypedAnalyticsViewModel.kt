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
import cn.wj.android.cashbook.core.data.repository.TagRepository
import cn.wj.android.cashbook.core.data.repository.TypeRepository
import cn.wj.android.cashbook.core.model.entity.RecordViewsEntity
import cn.wj.android.cashbook.domain.usecase.GetTagRecordViewsUseCase
import cn.wj.android.cashbook.domain.usecase.GetTypeRecordViewsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

/**
 * 指定类型的分析数据 ViewModel
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/10/27
 */
@HiltViewModel
class TypedAnalyticsViewModel @Inject constructor(
    typeRepository: TypeRepository,
    tagRepository: TagRepository,
    getTypeRecordViewsUseCase: GetTypeRecordViewsUseCase,
    getTagRecordViewsUseCase: GetTagRecordViewsUseCase,
) : ViewModel() {

    /** 需显示详情的记录数据 */
    var viewRecord by mutableStateOf<RecordViewsEntity?>(null)
        private set

    private val _tagIdData = MutableStateFlow(-1L)
    private val _typeIdData = MutableStateFlow(-1L)

    val uiState = combine(_tagIdData, _typeIdData) { tagId, typeId ->
        if (tagId == typeId) {
            TypedAnalyticsUiState.Loading
        } else {
            val isType = typeId != -1L
            TypedAnalyticsUiState.Success(
                isType = isType,
                titleText = if (isType) {
                    typeRepository.getRecordTypeById(typeId)?.name
                } else {
                    tagRepository.getTagById(tagId)?.name
                }.orEmpty(),
            )
        }
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = TypedAnalyticsUiState.Loading,
        )

    /** 记录列表数据 */
    val recordList = combine(_tagIdData, _typeIdData, recordDataVersion) { tagId, typeId, _ ->
        val isType = typeId != -1L
        GetTypedRecordData(
            isType = isType,
            if (isType) typeId else tagId
        )
    }
        .flatMapLatest {
            Pager(
                config = PagingConfig(
                    pageSize = DEFAULT_PAGE_SIZE,
                    initialLoadSize = DEFAULT_PAGE_SIZE
                ),
                pagingSourceFactory = {
                    if (it.isType) {
                        TypeRecordPagingSource(it.id, getTypeRecordViewsUseCase)
                    } else {
                        TagRecordPagingSource(it.id, getTagRecordViewsUseCase)
                    }
                },
            ).flow
        }
        .cachedIn(viewModelScope)

    fun updateId(tagId: Long, typeId: Long) {
        _tagIdData.tryEmit(tagId)
        _typeIdData.tryEmit(typeId)
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
)

sealed interface TypedAnalyticsUiState {
    data object Loading : TypedAnalyticsUiState
    data class Success(
        val isType: Boolean,
        val titleText: String,
    ) : TypedAnalyticsUiState
}

/**
 * Paging 数据仓库
 */
private class TypeRecordPagingSource(
    private val typeId: Long,
    private val getTypeRecordViewsUseCase: GetTypeRecordViewsUseCase
) : PagingSource<Int, RecordViewsEntity>() {
    override fun getRefreshKey(state: PagingState<Int, RecordViewsEntity>): Int? = null

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, RecordViewsEntity> {
        return runCatching {
            val page = params.key ?: 0
            val pageSize = params.loadSize
            val items = getTypeRecordViewsUseCase(typeId, page, pageSize)
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
 * Paging 数据仓库
 */
private class TagRecordPagingSource(
    private val tagId: Long,
    private val getTagRecordViewsUseCase: GetTagRecordViewsUseCase
) : PagingSource<Int, RecordViewsEntity>() {
    override fun getRefreshKey(state: PagingState<Int, RecordViewsEntity>): Int? = null

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, RecordViewsEntity> {
        return runCatching {
            val page = params.key ?: 0
            val pageSize = params.loadSize
            val items = getTagRecordViewsUseCase(tagId, page, pageSize)
            val prevKey = if (page > 0) page - 1 else null
            val nextKey = if (items.isNotEmpty()) page + 1 else null
            LoadResult.Page(items, prevKey, nextKey)
        }.getOrElse { throwable ->
            logger().e(throwable, "load()")
            LoadResult.Error(throwable)
        }
    }
}