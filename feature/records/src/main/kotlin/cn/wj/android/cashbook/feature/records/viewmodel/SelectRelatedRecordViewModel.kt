package cn.wj.android.cashbook.feature.records.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.wj.android.cashbook.core.data.repository.TypeRepository
import cn.wj.android.cashbook.core.model.entity.RecordViewsEntity
import cn.wj.android.cashbook.core.model.model.RecordModel
import cn.wj.android.cashbook.core.model.model.RecordTypeModel
import cn.wj.android.cashbook.domain.usecase.GetRecordViewsUseCase
import cn.wj.android.cashbook.domain.usecase.GetRelatedRecordViewsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 选择关联记录界面 ViewModel
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/3/14
 */
@HiltViewModel
class SelectRelatedRecordViewModel @Inject constructor(
    typeRepository: TypeRepository,
    getRecordViewsUseCase: GetRecordViewsUseCase,
    getRelatedRecordViewsUseCase: GetRelatedRecordViewsUseCase
) : ViewModel() {

    /** 搜索关键字数据 */
    private val _keywordData: MutableStateFlow<String> = MutableStateFlow("")

    private val _currentRecordData: MutableStateFlow<RecordModel?> = MutableStateFlow(null)

    /** 当前记录类型数据 */
    private val _currentTypeIdData = _currentRecordData.mapLatest {
        it?.typeId ?: -1L
    }
    private val _currentTypeData: Flow<RecordTypeModel?> = _currentTypeIdData.mapLatest {
        typeRepository.getRecordTypeById(it)
    }

    /** 关联记录 */
    private val _mutableRelatedRecordIdData = MutableStateFlow<List<Long>>(emptyList())
    private val _relatedRecordListData = _mutableRelatedRecordIdData.mapLatest { ids ->
        ids.mapNotNull {
            getRecordViewsUseCase(it)
        }
    }

    private val _recordListData = combine(
        _keywordData,
        _currentTypeData,
        _mutableRelatedRecordIdData
    ) { keyword, type, relatedIdList ->
        getRelatedRecordViewsUseCase(keyword, type).filter {
            !relatedIdList.contains(it.id)
        }
    }

    val uiState =
        combine(_relatedRecordListData, _recordListData) { relatedRecordList, recordList ->
            SelectRelatedRecordUiState.Success(
                relatedRecordList = relatedRecordList,
                recordList = recordList
            )
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(),
                initialValue = SelectRelatedRecordUiState.Loading,
            )

    private var initialized = false

    fun updateData(recordData: Flow<RecordModel>, relatedRecordListData: Flow<List<Long>>) {
        if (initialized) {
            return
        }
        initialized = true
        viewModelScope.launch {
            _currentRecordData.tryEmit(recordData.first())
            _mutableRelatedRecordIdData.tryEmit(relatedRecordListData.first())
        }
    }

    fun addToRelated(id: Long) {
        viewModelScope.launch {
            val ls = ArrayList(_mutableRelatedRecordIdData.first())
            if (!ls.contains(id)) {
                ls.add(id)
            }
            _mutableRelatedRecordIdData.tryEmit(ls)
        }
    }

    fun removeFromRelated(id: Long) {
        viewModelScope.launch {
            val ls = ArrayList(_mutableRelatedRecordIdData.first())
            if (ls.contains(id)) {
                ls.remove(id)
            }
            _mutableRelatedRecordIdData.tryEmit(ls)
        }
    }

    fun onKeywordsChanged(keyword: String) {
        _keywordData.tryEmit(keyword)
    }
}

sealed interface SelectRelatedRecordUiState {
    data object Loading : SelectRelatedRecordUiState
    data class Success(
        val relatedRecordList: List<RecordViewsEntity>,
        val recordList: List<RecordViewsEntity>,
    ) : SelectRelatedRecordUiState
}