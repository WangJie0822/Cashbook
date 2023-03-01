package cn.wj.android.cashbook.feature.records.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.wj.android.cashbook.core.common.Symbol
import cn.wj.android.cashbook.core.common.ext.toBigDecimalOrZero
import cn.wj.android.cashbook.core.data.repository.TypeRepository
import cn.wj.android.cashbook.core.model.entity.AssetEntity
import cn.wj.android.cashbook.core.model.entity.RecordEntity
import cn.wj.android.cashbook.core.model.entity.RecordTypeEntity
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.domain.usecase.GetDefaultRecordUseCase
import cn.wj.android.cashbook.domain.usecase.GetRecordTypeListUseCase
import cn.wj.android.cashbook.domain.usecase.GetVisibleAssetListUseCase
import cn.wj.android.cashbook.feature.records.enums.BottomSheetEnum
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.BigDecimal
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
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
    private val getVisibleAssetListUseCase: GetVisibleAssetListUseCase,
) : ViewModel() {

    /** 显示底部弹窗数据 */
    val showBottomSheet: MutableStateFlow<BottomSheetEnum> = MutableStateFlow(BottomSheetEnum.NONE)

    /** 经过修改的记录数据 */
    private val mutableRecordData: MutableStateFlow<RecordEntity?> = MutableStateFlow(null)

    /** 实际显示数据源 */
    private val recordData: Flow<RecordEntity> =
        combine(getDefaultRecordUseCase(), mutableRecordData) { default, modified ->
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

    /** 备注文本 */
    val remarkData: StateFlow<String> = recordData
        .map { it.remark }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = ""
        )

    /** 资产列表 */
    val assetListData: StateFlow<List<AssetEntity>> = getVisibleAssetListUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = listOf()
        )

    /** 资产文本 */
    val assetData: StateFlow<String> = recordData
        .map {
            val asset = it.asset
            if (null == asset) {
                ""
            } else {
                "${asset.name}(${asset.displayBalance})"
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = ""
        )

    /** 关联资产文本 */
    val relatedAssetData: StateFlow<String> = recordData
        .map {
            val asset = it.relatedAsset
            if (null == asset) {
                ""
            } else {
                "${asset.name}(${asset.displayBalance})"
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = ""
        )

    /** 时间文本 */
    val dateTimeData: StateFlow<String> = recordData
        .map {
            it.modifyTime
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = ""
        )

    /** 标签文本 */
    val tagsData: StateFlow<String> = recordData
        .map {
            if (it.tags.isEmpty()) {
                ""
            } else {
                StringBuilder().run {
                    it.tags.forEach { tag ->
                        if (!isBlank()) {
                            append(",")
                        }
                        append(tag.name)
                    }
                    toString()
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = ""
        )

    /** 手续费文本 */
    val chargesData: StateFlow<String> = recordData
        .map {
            if (it.charges.toBigDecimalOrZero() == BigDecimal.ZERO) {
                ""
            } else {
                "${Symbol.rmb}${it.charges}"
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = ""
        )

    /** 优惠文本 */
    val concessionsData: StateFlow<String> = recordData
        .map {
            if (it.concessions.toBigDecimalOrZero() == BigDecimal.ZERO) {
                ""
            } else {
                "${Symbol.rmb}${it.concessions}"
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = ""
        )

    /** 是否可报销 */
    val reimbursableData: StateFlow<Boolean> = recordData
        .map { it.reimbursable }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = false
        )

    /** 类型分类点击切换为 [typeCategory] */
    fun onTypeCategoryTabSelected(typeCategory: RecordTypeCategoryEnum) {
        viewModelScope.launch {
            mutableRecordData.value = recordData.first().copy(typeCategory = typeCategory)
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
                mutableRecordData.value = recordData.first().copy(type = it)
            }
        }
    }

    /** 备注文本变化为 [remark] */
    fun onRemarkTextChanged(remark: String) {
        viewModelScope.launch {
            mutableRecordData.value = recordData.first().copy(remark = remark)
        }
    }

    fun onBottomSheetAction(action: BottomSheetEnum) {
        showBottomSheet.value = action
    }

    fun onAssetItemClick(item: AssetEntity?) {
        viewModelScope.launch {
            mutableRecordData.value = recordData.first().copy(asset = item)
        }
    }

    /** TODO 尝试保存记录 */
    fun trySaveRecord() {
    }
}