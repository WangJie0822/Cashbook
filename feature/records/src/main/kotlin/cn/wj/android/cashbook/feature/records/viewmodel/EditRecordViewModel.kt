package cn.wj.android.cashbook.feature.records.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.wj.android.cashbook.core.common.ext.decimalFormat
import cn.wj.android.cashbook.core.common.ext.logger
import cn.wj.android.cashbook.core.common.ext.toBigDecimalOrZero
import cn.wj.android.cashbook.core.common.ext.toDoubleOrZero
import cn.wj.android.cashbook.core.common.ext.withCNY
import cn.wj.android.cashbook.core.data.repository.AssetRepository
import cn.wj.android.cashbook.core.data.repository.TagRepository
import cn.wj.android.cashbook.core.data.repository.TypeRepository
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.model.model.RecordModel
import cn.wj.android.cashbook.core.model.model.TagModel
import cn.wj.android.cashbook.domain.usecase.GetDefaultRecordUseCase
import cn.wj.android.cashbook.domain.usecase.SaveRecordUseCase
import cn.wj.android.cashbook.feature.records.enums.EditRecordBookmarkEnum
import cn.wj.android.cashbook.feature.records.enums.EditRecordBottomSheetEnum
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 编辑记录 ViewModel
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/7/5
 */
@HiltViewModel
class EditRecordViewModel @Inject constructor(
    private val typeRepository: TypeRepository,
    assetRepository: AssetRepository,
    tagRepository: TagRepository,
    getDefaultRecordUseCase: GetDefaultRecordUseCase,
    private val saveRecordUseCase: SaveRecordUseCase,
) : ViewModel() {

    /** 显示提示类型 */
    var shouldDisplayBookmark by mutableStateOf(EditRecordBookmarkEnum.NONE)
        private set

    /** 底部 sheet 类型 */
    var bottomSheetType by mutableStateOf(EditRecordBottomSheetEnum.NONE)
        private set

    /** 记录 id */
    private val recordIdData = MutableStateFlow(-1L)

    /** 记录数据 */
    private val mutableRecordData = MutableStateFlow<RecordModel?>(null)
    private val defaultRecordData = recordIdData.mapLatest {
        getDefaultRecordUseCase(it)
    }
    private val displayRecordData =
        combine(mutableRecordData, defaultRecordData) { mutable, default ->
            mutable ?: default
        }

    val uiState = displayRecordData.mapLatest { record ->
        val assetText = assetRepository.getAssetById(record.assetId)?.let { asset ->
            "${asset.name}(${
                if (asset.type.isCreditCard()) {
                    (asset.totalAmount.toBigDecimalOrZero() - asset.balance.toBigDecimalOrZero()).decimalFormat()
                } else {
                    asset.balance
                }.withCNY()
            })"
        }.orEmpty()
        val relatedAssetText = assetRepository.getAssetById(record.relatedAssetId)?.let { asset ->
            "${asset.name}(${
                if (asset.type.isCreditCard()) {
                    (asset.totalAmount.toBigDecimalOrZero() - asset.balance.toBigDecimalOrZero()).decimalFormat()
                } else {
                    asset.balance
                }.withCNY()
            })"
        }.orEmpty()
        EditRecordUiState.Success(
            amountText = record.amount.ifBlank { "0" },
            chargesText = record.charges.clearZero(),
            concessionsText = record.concessions.clearZero(),
            remarkText = record.remark,
            assetText = assetText,
            relatedAssetText = relatedAssetText,
            dateTimeText = record.recordTime,
            reimbursable = record.reimbursable,
            selectedTypeId = record.typeId,
        )
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = EditRecordUiState.Loading,
        )

    /** 类型数据 */
    private val mutableTypeCategoryData = MutableStateFlow<RecordTypeCategoryEnum?>(null)
    val selectedTypeCategoryData =
        combine(mutableTypeCategoryData, defaultRecordData) { mutable, defaultRecord ->
            mutable ?: typeRepository.getRecordTypeById(defaultRecord.typeId)?.typeCategory
            ?: RecordTypeCategoryEnum.EXPENDITURE
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = RecordTypeCategoryEnum.EXPENDITURE,
            )


    /** 标签数据 */
    private val mutableTagIdListData = MutableStateFlow<List<Long>>(listOf())
    private val tagListData = mutableTagIdListData.mapLatest { list ->
        mutableListOf<TagModel>().apply {
            list.map { tagId ->
                tagRepository.getTagById(tagId)?.let { tagModel -> add(tagModel) }
            }
        }
    }
    private val defaultTagListData = recordIdData
        .mapLatest {
            tagRepository.getRelatedTag(it)
        }
    private val displayTagListData = combine(tagListData, defaultTagListData) { mutable, default ->
        if (mutable.isEmpty()) {
            default
        } else {
            mutable
        }
    }
    val displayTagIdListData = displayTagListData
        .mapLatest { list ->
            list.map {
                it.id
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = listOf(),
        )

    val tagTextData = displayTagListData
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

    fun updateRecordId(id: Long) {
        recordIdData.tryEmit(id)
    }

    fun onTypeCategorySelect(typeCategory: RecordTypeCategoryEnum) {
        mutableTypeCategoryData.tryEmit(typeCategory)
    }

    fun onAmountClick() {
        bottomSheetType = EditRecordBottomSheetEnum.AMOUNT
    }

    fun onAmountChange(amount: String) {
        viewModelScope.launch {
            mutableRecordData.tryEmit(displayRecordData.first().copy(amount = amount))
            dismissBottomSheet()
        }
    }

    fun onChargesClick() {
        bottomSheetType = EditRecordBottomSheetEnum.CHARGES
    }

    fun onChargeChange(charges: String) {
        viewModelScope.launch {
            mutableRecordData.tryEmit(displayRecordData.first().copy(charges = charges))
            dismissBottomSheet()
        }
    }

    fun onConcessionsClick() {
        bottomSheetType = EditRecordBottomSheetEnum.CONCESSIONS
    }

    fun onConcessionsChange(concessions: String) {
        viewModelScope.launch {
            mutableRecordData.tryEmit(displayRecordData.first().copy(concessions = concessions))
            dismissBottomSheet()
        }
    }

    fun onTypeSelect(typeId: Long) {
        if (typeId == -1L) {
            return
        }
        viewModelScope.launch {
            mutableRecordData.tryEmit(displayRecordData.first().copy(typeId = typeId))
        }
    }

    fun onRemarkChange(remark: String) {
        viewModelScope.launch {
            mutableRecordData.tryEmit(displayRecordData.first().copy(remark = remark))
        }
    }

    fun onAssetClick() {
        bottomSheetType = EditRecordBottomSheetEnum.ASSETS
    }

    fun onAssetChange(assetId: Long) {
        viewModelScope.launch {
            mutableRecordData.tryEmit(displayRecordData.first().copy(assetId = assetId))
            dismissBottomSheet()
        }
    }

    fun onRelatedAssetClick() {
        bottomSheetType = EditRecordBottomSheetEnum.RELATED_ASSETS
    }

    fun onRelatedAssetChange(assetId: Long) {
        viewModelScope.launch {
            mutableRecordData.tryEmit(displayRecordData.first().copy(relatedAssetId = assetId))
            dismissBottomSheet()
        }
    }

    fun onDateTimeChange(dateTime: String) {
        viewModelScope.launch {
            mutableRecordData.tryEmit(displayRecordData.first().copy(recordTime = dateTime))
            dismissBottomSheet()
        }
    }

    fun onTagClick() {
        bottomSheetType = EditRecordBottomSheetEnum.TAGS
    }

    fun onTagChange(tags: List<Long>) {
        mutableTagIdListData.tryEmit(tags)
    }

    fun onReimbursableClick() {
        viewModelScope.launch {
            val old = displayRecordData.first()
            mutableRecordData.tryEmit(old.copy(reimbursable = !old.reimbursable))
        }
    }

    fun dismissBookmark() {
        shouldDisplayBookmark = EditRecordBookmarkEnum.NONE
    }

    fun dismissBottomSheet() {
        bottomSheetType = EditRecordBottomSheetEnum.NONE
    }

    fun onSaveClick(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val recordEntity = displayRecordData.first()
            if (recordEntity.amount.toDoubleOrZero() == 0.0) {
                // 记录金额不能为 0
                shouldDisplayBookmark = EditRecordBookmarkEnum.AMOUNT_MUST_NOT_BE_ZERO
                return@launch
            }
            // 支出分类
            val typeCategory = selectedTypeCategoryData.first()
            if (typeRepository.getNoNullRecordTypeById(recordEntity.typeId).typeCategory != typeCategory) {
                // 类型与支出类型不匹配
                shouldDisplayBookmark = EditRecordBookmarkEnum.TYPE_NOT_MATCH_CATEGORY
                return@launch
            }
            // TODO 关联记录
            try {
                saveRecordUseCase(
                    recordModel = recordEntity.copy(
                        relatedAssetId = if (typeCategory != RecordTypeCategoryEnum.TRANSFER) -1L else recordEntity.relatedAssetId,
                        concessions = if (typeCategory == RecordTypeCategoryEnum.INCOME) "" else recordEntity.concessions,
                        reimbursable = if (typeCategory != RecordTypeCategoryEnum.EXPENDITURE) false else recordEntity.reimbursable,
                    ),
                    tagIdList = displayTagIdListData.first(),
                )
                onSuccess.invoke()
            } catch (throwable: Throwable) {
                // 保存失败
                this@EditRecordViewModel.logger().e(throwable, "onSaveClick()")
                shouldDisplayBookmark = EditRecordBookmarkEnum.SAVE_FAILED
            }
        }
    }
}

sealed class EditRecordUiState(open val selectedTypeId: Long = -1L) {
    object Loading : EditRecordUiState()

    data class Success(
        val amountText: String,
        val chargesText: String,
        val concessionsText: String,
        val remarkText: String,
        val assetText: String,
        val relatedAssetText: String,
        val dateTimeText: String,
        val reimbursable: Boolean,
        override val selectedTypeId: Long,
    ) : EditRecordUiState()
}

private fun String.clearZero(): String {
    return if (this.toDoubleOrZero() == 0.0) {
        ""
    } else {
        this
    }
}
