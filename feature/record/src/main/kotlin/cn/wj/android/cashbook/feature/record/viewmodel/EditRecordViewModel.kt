package cn.wj.android.cashbook.feature.record.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.wj.android.cashbook.core.data.repository.TypeRepository
import cn.wj.android.cashbook.core.model.entity.RecordEntity
import cn.wj.android.cashbook.core.model.entity.RecordTypeEntity
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.domain.usecase.GetDefaultRecordUseCase
import cn.wj.android.cashbook.domain.usecase.GetRecordTypeListUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class EditRecordViewModel @Inject constructor(
    private val typeRepository: TypeRepository,
    private val getDefaultRecordUseCase: GetDefaultRecordUseCase,
    private val getRecordTypeListUseCase: GetRecordTypeListUseCase,
) : ViewModel() {

    /** 经过修改的记录数据 */
    private val modifiedRecordData: MutableStateFlow<RecordEntity?> = MutableStateFlow(null)

    /** 实际显示数据源 */
    private val recordData =
        combine(getDefaultRecordUseCase(), modifiedRecordData) { default, modified ->
            modified ?: default
        }

    /** 类型标签数据 */
    val typeCategory: StateFlow<RecordTypeCategoryEnum> = recordData
        .map { it.typeCategory }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = RecordTypeCategoryEnum.EXPENDITURE
        )

    /** 金额 */
    val amountData: StateFlow<String> = recordData
        .map { it.amount }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = ""
        )

    /** 标签列表 */
    val typeListData: StateFlow<List<RecordTypeEntity>> = recordData
        .map {
            getRecordTypeListUseCase(it)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = listOf()
        )


    /** 类型分类点击切换为 [typeCategory] */
    fun onTypeCategoryTabSelected(typeCategory: RecordTypeCategoryEnum) {
        viewModelScope.launch {
            modifiedRecordData.value = recordData.first().copy(typeCategory = typeCategory)
        }
    }

    fun onAmountChange(value: String) {
        viewModelScope.launch {
            modifiedRecordData.value = recordData.first().copy(amount = value)
        }
    }

    /** 类型点击切换为 [type] */
    fun onTypeClick(type: RecordTypeEntity) {
        viewModelScope.launch {
            val selected = if (!type.selected) {
                // 当前为选中，更新为选中
                type.copy(selected = true)
            } else {
                // 当前已选中，取消选中
                if (type.parentId == -1L) {
                    // 二级分类，选择父类型
                    (typeListData.first().firstOrNull { it.id == type.parentId }
                        ?: typeListData.first().first()).copy(selected = true)
                } else {
                    // 一级分类，无法取消，不做处理
                    null
                }
            }
            selected?.let {
                modifiedRecordData.value = recordData.first().copy(type = it)
            }
        }
    }
}