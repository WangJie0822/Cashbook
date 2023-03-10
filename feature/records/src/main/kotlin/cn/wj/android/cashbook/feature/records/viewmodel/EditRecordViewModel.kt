@file:OptIn(ExperimentalCoroutinesApi::class)

package cn.wj.android.cashbook.feature.records.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.wj.android.cashbook.core.common.Symbol
import cn.wj.android.cashbook.core.common.ext.decimalFormat
import cn.wj.android.cashbook.core.common.ext.toBigDecimalOrZero
import cn.wj.android.cashbook.core.common.ext.toDoubleOrZero
import cn.wj.android.cashbook.core.data.repository.AssetRepository
import cn.wj.android.cashbook.core.data.repository.TypeRepository
import cn.wj.android.cashbook.core.model.entity.AssetEntity
import cn.wj.android.cashbook.core.model.entity.RecordEntity
import cn.wj.android.cashbook.core.model.entity.RecordTypeEntity
import cn.wj.android.cashbook.core.model.entity.TagEntity
import cn.wj.android.cashbook.core.model.enums.ClassificationTypeEnum
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.model.model.ResultModel
import cn.wj.android.cashbook.core.model.model.ResultModel.Failure.Companion.FAILURE_EDIT_RECORD_AMOUNT_MUST_NOT_BE_ZERO
import cn.wj.android.cashbook.core.model.model.ResultModel.Failure.Companion.FAILURE_EDIT_RECORD_TYPE_MUST_NOT_BE_NULL
import cn.wj.android.cashbook.core.model.model.ResultModel.Failure.Companion.FAILURE_EDIT_RECORD_TYPE_NOT_MATCH_CATEGORY
import cn.wj.android.cashbook.core.model.transfer.asEntity
import cn.wj.android.cashbook.domain.usecase.GetDefaultRecordUseCase
import cn.wj.android.cashbook.domain.usecase.GetDefaultTagListUseCase
import cn.wj.android.cashbook.domain.usecase.SaveRecordUseCase
import cn.wj.android.cashbook.domain.usecase.asEntity
import cn.wj.android.cashbook.feature.records.enums.EditRecordBottomSheetEnum
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class EditRecordViewModel @Inject constructor(
    assetRepository: AssetRepository,
    typeRepository: TypeRepository,
    getDefaultRecordUseCase: GetDefaultRecordUseCase,
    getDefaultTagListUseCase: GetDefaultTagListUseCase,
    private val saveRecordUseCase: SaveRecordUseCase,
) : ViewModel() {

    /** 显示底部弹窗数据 */
    val bottomSheetData: MutableStateFlow<EditRecordBottomSheetEnum> =
        MutableStateFlow(EditRecordBottomSheetEnum.NONE)

    /** 记录 id 数据 */
    val recordIdData: MutableStateFlow<Long> = MutableStateFlow(-1L)

    /** 默认记录数据 */
    private val defaultRecordData: Flow<RecordEntity> = recordIdData.mapLatest {
        getDefaultRecordUseCase(it)
    }

    /** 经过修改的记录数据 */
    private val mutableRecordData: MutableStateFlow<RecordEntity?> = MutableStateFlow(null)

    /** 实际显示数据源 */
    private val recordData: Flow<RecordEntity> =
        combine(defaultRecordData, mutableRecordData) { default, modified ->
            modified ?: default
        }

    /** 金额 */
    val amountData: StateFlow<String> = recordData
        .mapLatest { it.amount }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = ""
        )

    /** 选中类型数据 */
    val selectedTypeData: StateFlow<RecordTypeEntity?> = recordData
        .mapLatest { typeRepository.getNoNullRecordTypeById(it.typeId).asEntity() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = null
        )

    private val defaultTypeCategory: Flow<RecordTypeCategoryEnum> = selectedTypeData
        .mapLatest {
            it?.typeCategory ?: RecordTypeCategoryEnum.EXPENDITURE
        }

    private val mutableTypeCategory: MutableStateFlow<RecordTypeCategoryEnum?> =
        MutableStateFlow(null)

    /** 类型标签数据 */
    val typeCategory: StateFlow<RecordTypeCategoryEnum> =
        combine(defaultTypeCategory, mutableTypeCategory) { default, mutable ->
            mutable ?: default
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(),
                initialValue = RecordTypeCategoryEnum.EXPENDITURE
            )

    /** 备注文本 */
    val remarkData: StateFlow<String> = recordData
        .mapLatest { it.remark }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = ""
        )

    /** 资产文本 */
    val assetData: StateFlow<String> = recordData
        .mapLatest {
            val asset = assetRepository.getAssetById(it.assetId)?.asEntity()
            if (null == asset) {
                ""
            } else {
                "${asset.name}(${Symbol.rmb} ${asset.displayBalance})"
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = ""
        )

    /** 关联资产文本 */
    val relatedAssetData: StateFlow<String> = recordData
        .mapLatest {
            val asset = assetRepository.getAssetById(it.relatedAssetId)?.asEntity()
            if (null == asset) {
                ""
            } else {
                "${asset.name}(${Symbol.rmb} ${asset.displayBalance})"
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = ""
        )

    /** 时间文本 */
    val dateTimeData: StateFlow<String> = recordData
        .mapLatest {
            it.recordTime
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = ""
        )

    /** 默认标签数据 */
    private val defaultTagsData: Flow<List<TagEntity>> = recordIdData.mapLatest {
        getDefaultTagListUseCase(it)
    }

    /** 可修改的标签数据 */
    private val mutableTagsData: MutableStateFlow<List<TagEntity>?> = MutableStateFlow(null)

    /** 最终标签数据 */
    private val tagsData: StateFlow<List<TagEntity>> =
        combine(defaultTagsData, mutableTagsData) { default, mutable ->
            mutable ?: default
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(),
                initialValue = listOf()
            )

    /** 标签 id 列表 */
    val tagsIdData: StateFlow<List<Long>> = tagsData
        .mapLatest { list ->
            list.map { it.id }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = listOf()
        )

    /** 标签文本 */
    val tagsTextData: StateFlow<String> = tagsData
        .mapLatest { list ->
            StringBuilder().run {
                list.forEach { tag ->
                    if (!isBlank()) {
                        append(",")
                    }
                    append(tag.name)
                }
                toString()
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = ""
        )

    /** 手续费文本 */
    val chargesData: StateFlow<String> = recordData
        .mapLatest {
            if (it.charges.toDoubleOrZero() == 0.0) {
                ""
            } else {
                it.charges
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = ""
        )

    /** 优惠文本 */
    val concessionsData: StateFlow<String> = recordData
        .mapLatest {
            if (it.concessions.toDoubleOrZero() == 0.0) {
                ""
            } else {
                it.concessions
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = ""
        )

    /** 是否可报销 */
    val reimbursableData: StateFlow<Boolean> = recordData
        .mapLatest { it.reimbursable }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = false
        )

    fun onBottomSheetAction(action: EditRecordBottomSheetEnum) {
        bottomSheetData.value = action
    }

    /** 金额变化 */
    fun onAmountChanged(amount: String) {
        viewModelScope.launch {
            mutableRecordData.value = recordData.first().copy(amount = amount)
        }
    }

    /** 类型分类点击切换为 [typeCategory] */
    fun onTypeCategoryTabSelected(typeCategory: RecordTypeCategoryEnum) {
        viewModelScope.launch {
            mutableTypeCategory.value = typeCategory
        }
    }

    /** 类型点击切换为 [type] */
    fun onTypeClick(type: RecordTypeEntity?) {
        viewModelScope.launch {
            type?.let {
                mutableRecordData.value = recordData.first().copy(typeId = it.id)
            }
        }
    }

    /** 备注文本变化为 [remark] */
    fun onRemarkTextChanged(remark: String) {
        viewModelScope.launch {
            mutableRecordData.value = recordData.first().copy(remark = remark)
        }
    }

    fun onAssetItemClick(item: AssetEntity?) {
        viewModelScope.launch {
            mutableRecordData.value = recordData.first().copy(assetId = item?.id ?: -1L)
        }
    }

    fun onRelatedAssetItemClick(item: AssetEntity?) {
        viewModelScope.launch {
            mutableRecordData.value = recordData.first().copy(relatedAssetId = item?.id ?: -1L)
        }
    }

    fun onDateTimePicked(dateTime: String) {
        viewModelScope.launch {
            mutableRecordData.value = recordData.first().copy(recordTime = dateTime)
        }
    }

    fun onTagItemClick(item: TagEntity) {
        viewModelScope.launch {
            val list = arrayListOf<TagEntity>()
            var needAdd = true
            tagsData.first().forEach {
                if (it.id != item.id) {
                    list.add(it)
                } else {
                    needAdd = false
                }
            }
            if (needAdd) {
                list.add(item)
            }
            mutableTagsData.value = list
        }
    }

    fun onReimbursableClick() {
        viewModelScope.launch {
            mutableRecordData.value =
                recordData.first().copy(reimbursable = !reimbursableData.value)
        }
    }

    fun onChargesChanged(charges: String) {
        viewModelScope.launch {
            mutableRecordData.value = recordData.first().copy(charges = charges)
        }
    }

    fun onConcessionsChanged(concessions: String) {
        viewModelScope.launch {
            mutableRecordData.value = recordData.first().copy(concessions = concessions)
        }
    }

    /** 尝试保存记录 */
    suspend fun trySaveRecord(): ResultModel {
        val recordEntity = recordData.first()
        if (recordEntity.amount.toDoubleOrZero() == 0.0) {
            // 记录金额不能为 0
            return ResultModel.failure(FAILURE_EDIT_RECORD_AMOUNT_MUST_NOT_BE_ZERO)
        }
        // 检查类型数据，类型不能为空
        val typeEntity =
            selectedTypeData.value ?: return ResultModel.failure(
                FAILURE_EDIT_RECORD_TYPE_MUST_NOT_BE_NULL
            )
        // 支出分类
        val typeCategory = typeCategory.value
        if (typeEntity.typeCategory != typeCategory) {
            // 类型与支出类型不匹配
            return ResultModel.failure(FAILURE_EDIT_RECORD_TYPE_NOT_MATCH_CATEGORY)
        }

        // TODO 关联记录
        return try {
            saveRecordUseCase(
                recordEntity.copy(
                    relatedAssetId = if (typeCategory != RecordTypeCategoryEnum.TRANSFER) -1L else recordEntity.relatedAssetId,
                    concessions = if (typeCategory == RecordTypeCategoryEnum.INCOME) "" else recordEntity.concessions,
                    reimbursable = if (typeCategory != RecordTypeCategoryEnum.EXPENDITURE) false else recordEntity.reimbursable,
                ),
                tagsData.value,
            )
            ResultModel.success()
        } catch (throwable: Throwable) {
            ResultModel.failure(throwable)
        }
    }
}

private val AssetEntity.displayBalance: String
    get() = if (type == ClassificationTypeEnum.CREDIT_CARD_ACCOUNT) {
        (totalAmount.toBigDecimalOrZero() - balance.toBigDecimalOrZero()).decimalFormat()
    } else {
        balance
    }