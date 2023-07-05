package cn.wj.android.cashbook.feature.types.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.domain.usecase.GetRecordTypeListUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class EditRecordTypeListViewModel @Inject constructor(
    getRecordTypeListUseCase: GetRecordTypeListUseCase
) : ViewModel() {

    private val typeCategoryData: MutableStateFlow<RecordTypeCategoryEnum> =
        MutableStateFlow(RecordTypeCategoryEnum.EXPENDITURE)

    private val selectedTypeIdData: MutableStateFlow<Long> = MutableStateFlow(-1L)

    val typeListData =
        combine(typeCategoryData, selectedTypeIdData) { typeCategory, selectedTypeId ->
            getRecordTypeListUseCase(typeCategory, selectedTypeId)
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(),
                initialValue = listOf()
            )

    fun update(typeCategory: RecordTypeCategoryEnum, selectedTypeId: Long) {
        typeCategoryData.tryEmit(typeCategory)
        selectedTypeIdData.tryEmit(selectedTypeId)
    }
}