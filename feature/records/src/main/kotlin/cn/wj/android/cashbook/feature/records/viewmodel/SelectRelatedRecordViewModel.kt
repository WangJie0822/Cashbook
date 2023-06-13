package cn.wj.android.cashbook.feature.records.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.wj.android.cashbook.core.model.entity.RecordTypeEntity
import cn.wj.android.cashbook.domain.usecase.GetRelatedRecordViewsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * 选择关联记录界面 ViewModel
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/3/14
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SelectRelatedRecordViewModel @Inject constructor(
    getRelatedRecordViewsUseCase: GetRelatedRecordViewsUseCase
) : ViewModel() {

    /** 搜索关键字数据 */
    private val keywordData: MutableStateFlow<String> = MutableStateFlow("")

    /** 当前记录类型数据 */
    val currentTypeData: MutableStateFlow<RecordTypeEntity?> = MutableStateFlow(null)

    val recordListData = combine(keywordData, currentTypeData) { keyword, type ->
        getRelatedRecordViewsUseCase(keyword, type)
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = listOf()
        )

    fun onKeywordsChanged(keyword: String) {
        keywordData.value = keyword
    }
}