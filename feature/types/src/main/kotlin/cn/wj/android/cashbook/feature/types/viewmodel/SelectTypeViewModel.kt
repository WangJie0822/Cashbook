package cn.wj.android.cashbook.feature.types.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.wj.android.cashbook.core.model.entity.RecordTypeEntity
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.domain.usecase.GetRecordTypeListUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class SelectTypeViewModel @Inject constructor(
    getRecordTypeListUseCase: GetRecordTypeListUseCase
) : ViewModel() {

    val typeCategoryData: MutableStateFlow<RecordTypeCategoryEnum> =
        MutableStateFlow(RecordTypeCategoryEnum.EXPENDITURE)

    val selectedTypeData: MutableStateFlow<RecordTypeEntity?> = MutableStateFlow(null)

    val typeListData: StateFlow<List<RecordTypeEntity>> =
        combine(typeCategoryData, selectedTypeData) { typeCategory, selectedType ->
            getRecordTypeListUseCase(typeCategory, selectedType)
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(),
                initialValue = listOf()
            )

    fun update(typeCategory: RecordTypeCategoryEnum, selectedType: RecordTypeEntity?) {
        typeCategoryData.value = typeCategory
        selectedTypeData.value = selectedType
    }
}