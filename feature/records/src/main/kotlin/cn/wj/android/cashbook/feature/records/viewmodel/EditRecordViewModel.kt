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
import cn.wj.android.cashbook.core.common.ext.decimalFormat
import cn.wj.android.cashbook.core.common.ext.logger
import cn.wj.android.cashbook.core.common.ext.toBigDecimalOrZero
import cn.wj.android.cashbook.core.common.ext.toDoubleOrZero
import cn.wj.android.cashbook.core.common.ext.withCNY
import cn.wj.android.cashbook.core.common.tools.DATE_FORMAT_DATE
import cn.wj.android.cashbook.core.common.tools.DATE_FORMAT_NO_SECONDS
import cn.wj.android.cashbook.core.common.tools.dateFormat
import cn.wj.android.cashbook.core.common.tools.parseDateLong
import cn.wj.android.cashbook.core.data.repository.AssetRepository
import cn.wj.android.cashbook.core.data.repository.RecordRepository
import cn.wj.android.cashbook.core.data.repository.SettingRepository
import cn.wj.android.cashbook.core.data.repository.TagRepository
import cn.wj.android.cashbook.core.data.repository.TypeRepository
import cn.wj.android.cashbook.core.model.enums.ImageQualityEnum
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.model.model.RecordModel
import cn.wj.android.cashbook.core.model.model.TagModel
import cn.wj.android.cashbook.core.ui.DialogState
import cn.wj.android.cashbook.core.ui.runCatchWithProgress
import cn.wj.android.cashbook.domain.usecase.GetDefaultRecordUseCase
import cn.wj.android.cashbook.domain.usecase.SaveRecordUseCase
import cn.wj.android.cashbook.feature.records.enums.EditRecordBookmarkEnum
import cn.wj.android.cashbook.feature.records.enums.EditRecordBottomSheetEnum
import cn.wj.android.cashbook.feature.records.model.DateTimePickerModel
import cn.wj.android.cashbook.feature.records.model.ImageViewModel
import cn.wj.android.cashbook.feature.records.model.asModel
import cn.wj.android.cashbook.feature.records.model.asViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

/**
 * 编辑记录 ViewModel
 *
 * @param typeRepository 类型数据仓库
 * @param assetRepository 资产数据仓库
 * @param tagRepository 标签数据仓库
 * @param getDefaultRecordUseCase 获取新建默认记录数据用例
 * @param saveRecordUseCase 保存记录数据用例
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/7/5
 */
@HiltViewModel
class EditRecordViewModel @Inject constructor(
    private val typeRepository: TypeRepository,
    assetRepository: AssetRepository,
    tagRepository: TagRepository,
    recordRepository: RecordRepository,
    settingRepository: SettingRepository,
    getDefaultRecordUseCase: GetDefaultRecordUseCase,
    private val saveRecordUseCase: SaveRecordUseCase,
) : ViewModel() {

    /** 显示提示类型 */
    var shouldDisplayBookmark by mutableStateOf(EditRecordBookmarkEnum.NONE)
        private set

    /** 底部 sheet 类型 */
    var bottomSheetType by mutableStateOf(EditRecordBottomSheetEnum.NONE)
        private set

    /** 弹窗状态 */
    var dialogState by mutableStateOf<DialogState>(DialogState.Dismiss)
        private set

    /** 记录 id */
    private val _recordIdData = MutableStateFlow(-1L)

    /** 记录数据 */
    private val _mutableRecordData = MutableStateFlow<RecordModel?>(null)
    private val _defaultRecordData = _recordIdData.mapLatest {
        getDefaultRecordUseCase(it)
    }
    private val _displayRecordData =
        combine(_mutableRecordData, _defaultRecordData) { mutable, default ->
            mutable ?: default
        }

    /** 关联图片 */
    private val _mutableImageData = MutableStateFlow<List<ImageViewModel>?>(null)
    private val _defaultImageData = _recordIdData.mapLatest { id ->
        recordRepository.queryImagesByRecordId(id)
            .map { it.asViewModel() }
    }
    val displayImageData =
        combine(_mutableImageData, _defaultImageData) { mutable, default ->
            mutable ?: default
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList(),
            )

    /** 关联记录 */
    private val _mutableRelatedRecordIdData = MutableStateFlow<List<Long>?>(null)
    private val _defaultRelatedRecordIdData = _recordIdData.mapLatest {
        recordRepository.getRelatedIdListById(it)
    }
    private val _relatedRecordIdData =
        combine(_mutableRelatedRecordIdData, _defaultRelatedRecordIdData) { mutable, default ->
            mutable ?: default
        }
    private val _relatedRecordListData = _relatedRecordIdData.mapLatest { ids ->
        ids.mapNotNull {
            recordRepository.queryById(it)
        }
    }
    private val _relatedRecordTotalAmountData = _relatedRecordListData.mapLatest { list ->
        var total = BigDecimal.ZERO
        list.forEach {
            total += (it.amount.toBigDecimalOrZero() + it.charges.toBigDecimalOrZero() - it.concessions.toBigDecimalOrZero())
        }
        total.decimalFormat()
    }

    /** 界面 UI 状态 */
    val uiState =
        combine(
            _displayRecordData,
            _relatedRecordTotalAmountData,
            settingRepository.appSettingsModel,
        ) { record, relatedAmount, model ->
            val assetText = assetRepository.getAssetById(record.assetId)?.let { asset ->
                "${asset.name}(${
                    if (asset.type.isCreditCard) {
                        (asset.totalAmount.toBigDecimalOrZero() - asset.balance.toBigDecimalOrZero()).decimalFormat()
                    } else {
                        asset.balance
                    }.withCNY()
                })"
            }.orEmpty()
            val relatedAssetText =
                assetRepository.getAssetById(record.relatedAssetId)?.let { asset ->
                    "${asset.name}(${
                        if (asset.type.isCreditCard) {
                            (asset.totalAmount.toBigDecimalOrZero() - asset.balance.toBigDecimalOrZero()).decimalFormat()
                        } else {
                            asset.balance
                        }.withCNY()
                    })"
                }.orEmpty()
            val needRelated = typeRepository.needRelated(record.typeId)
            EditRecordUiState.Success(
                amountText = record.amount.ifBlank { "0" },
                chargesText = record.charges.clearZero(),
                concessionsText = record.concessions.clearZero(),
                remarkText = record.remark,
                selectedAssetId = record.assetId,
                assetText = assetText,
                relatedAssetText = relatedAssetText,
                dateTimeText = record.recordTime,
                reimbursable = record.reimbursable,
                selectedTypeId = record.typeId,
                needRelated = needRelated,
                relatedCount = _relatedRecordListData.first().size,
                relatedAmount = relatedAmount,
                imageQuality = model.imageQuality,
            )
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = EditRecordUiState.Loading,
            )

    /** 类型数据 */
    val defaultTypeIdData = _defaultRecordData.mapLatest { it.typeId }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = -1L,
        )
    private val _mutableTypeCategoryData = MutableStateFlow<RecordTypeCategoryEnum?>(null)
    val selectedTypeCategoryData =
        combine(_mutableTypeCategoryData, _defaultRecordData) { mutable, defaultRecord ->
            mutable ?: typeRepository.getRecordTypeById(defaultRecord.typeId)?.typeCategory
                ?: RecordTypeCategoryEnum.EXPENDITURE
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = RecordTypeCategoryEnum.EXPENDITURE,
            )

    /** 可变标签id列表数据 - 用户手动设置 */
    private val _mutableTagIdListData = MutableStateFlow<List<Long>?>(null)

    /** 可变标签信息列表数据 - 根据可变标签id列表数据获取对于数据 */
    private val _mutableTagListData = _mutableTagIdListData.mapLatest { list ->
        if (null == list) {
            null
        } else {
            mutableListOf<TagModel>().apply {
                list.map { tagId ->
                    tagRepository.getTagById(tagId)?.let { tagModel -> add(tagModel) }
                }
            }
        }
    }

    /** 默认标签列表数据 - 已保存的数据，新建记录为空 */
    private val _defaultTagListData = _recordIdData
        .mapLatest {
            tagRepository.getRelatedTag(it)
        }

    /** 最终用于显示的标签数据 */
    private val _displayTagListData =
        combine(_mutableTagListData, _defaultTagListData) { mutable, default ->
            mutable ?: default
        }

    /** 实际显示的标签id列表数据，用于控制选择标签Sheet中标签的选中状态 */
    val displayTagIdListData = _displayTagListData
        .mapLatest { list ->
            list.map {
                it.id
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = emptyList(),
        )

    /** 标签显示文本 */
    val tagTextData = _displayTagListData
        .mapLatest { list ->
            StringBuilder().run {
                list.forEach { tag ->
                    if (isNotBlank()) {
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
            initialValue = "",
        )

    private var amountSheetShowed = false
    private var recordIdInit = false

    /** 更新记录 [id]，刷新界面数据 */
    fun initRecordId(id: Long) {
        if (recordIdInit) {
            return
        }
        recordIdInit = true
        _recordIdData.tryEmit(id)
        if (id == -1L && !amountSheetShowed) {
            // 新建，自动显示输入框
            amountSheetShowed = true
            viewModelScope.launch {
                delay(500L)
                displayAmountSheet()
            }
        }
    }

    private var assetIdInit = false

    /** 更新类型 */
    fun initAssetId(assetId: Long) {
        if (assetIdInit) {
            return
        }
        assetIdInit = true
        if (assetId == -1L) {
            return
        }
        viewModelScope.launch {
            _mutableRecordData.tryEmit(_displayRecordData.first().copy(assetId = assetId))
        }
    }

    /** 更新记录大类为 [typeCategory] */
    fun updateTypeCategory(typeCategory: RecordTypeCategoryEnum) {
        _mutableTypeCategoryData.tryEmit(typeCategory)
    }

    /** 显示金额抽屉 */
    fun displayAmountSheet() {
        bottomSheetType = EditRecordBottomSheetEnum.AMOUNT
    }

    /** 更新金额 */
    fun updateAmount(amount: String) {
        viewModelScope.launch {
            _mutableRecordData.tryEmit(_displayRecordData.first().copy(amount = amount))
            dismissBottomSheet()
        }
    }

    /** 显示手续费抽屉 */
    fun displayChargesSheet() {
        bottomSheetType = EditRecordBottomSheetEnum.CHARGES
    }

    /** 更新手续费 */
    fun updateCharge(charges: String) {
        viewModelScope.launch {
            _mutableRecordData.tryEmit(_displayRecordData.first().copy(charges = charges))
            dismissBottomSheet()
        }
    }

    /** 显示优惠抽屉 */
    fun displayConcessions() {
        bottomSheetType = EditRecordBottomSheetEnum.CONCESSIONS
    }

    /** 更新优惠 */
    fun updateConcessions(concessions: String) {
        viewModelScope.launch {
            _mutableRecordData.tryEmit(_displayRecordData.first().copy(concessions = concessions))
            dismissBottomSheet()
        }
    }

    /** 更新类型 */
    fun updateType(typeId: Long) {
        if (typeId == -1L) {
            return
        }
        viewModelScope.launch {
            _mutableRecordData.tryEmit(_displayRecordData.first().copy(typeId = typeId))
        }
    }

    /** 更新备注 */
    fun updateRemark(remark: String) {
        viewModelScope.launch {
            _mutableRecordData.tryEmit(_displayRecordData.first().copy(remark = remark))
        }
    }

    /** 显示资产抽屉 */
    fun displayAssetSheet() {
        bottomSheetType = EditRecordBottomSheetEnum.ASSETS
    }

    /** 更新资产 */
    fun updateAsset(assetId: Long) {
        viewModelScope.launch {
            _mutableRecordData.tryEmit(_displayRecordData.first().copy(assetId = assetId))
            dismissBottomSheet()
        }
    }

    /** 显示关联资产抽屉 */
    fun displayRelatedAssetSheet() {
        bottomSheetType = EditRecordBottomSheetEnum.RELATED_ASSETS
    }

    /** 更新关联资产 */
    fun updateRelatedAsset(assetId: Long) {
        viewModelScope.launch {
            _mutableRecordData.tryEmit(_displayRecordData.first().copy(relatedAssetId = assetId))
            dismissBottomSheet()
        }
    }

    /** 显示标签抽屉 */
    fun displayTagSheet() {
        bottomSheetType = EditRecordBottomSheetEnum.TAGS
    }

    /** 更新标签 */
    fun updateTag(tags: List<Long>) {
        _mutableTagIdListData.tryEmit(tags)
    }

    /** 显示选择照片抽屉 */
    fun displayImageSheet() {
        bottomSheetType = EditRecordBottomSheetEnum.IMAGES
    }

    fun updateImageData(list: List<ImageViewModel>) {
        dismissBottomSheet()
        _mutableImageData.tryEmit(list)
    }

    fun showImagePreviewDialog(list: List<ImageViewModel>, index: Int) {
        dialogState = DialogState.Shown(ImagePreviewData(list, index))
    }

    /** 切换可报销状态 */
    fun switchReimbursable() {
        viewModelScope.launch {
            val old = _displayRecordData.first()
            _mutableRecordData.tryEmit(old.copy(reimbursable = !old.reimbursable))
        }
    }

    /** 隐藏提示 */
    fun dismissBookmark() {
        shouldDisplayBookmark = EditRecordBookmarkEnum.NONE
    }

    /** 隐藏底部抽屉 */
    fun dismissBottomSheet() {
        bottomSheetType = EditRecordBottomSheetEnum.NONE
    }

    private var inSave = false

    /** 保存记录 */
    fun trySave(hintText: String, onSuccess: () -> Unit) {
        if (inSave) {
            return
        }
        inSave = true
        viewModelScope.launch {
            val recordEntity = _displayRecordData.first()
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
            val result = runCatchWithProgress(hint = hintText, cancelable = false) {
                saveRecordUseCase(
                    recordModel = recordEntity.copy(
                        relatedAssetId = if (typeCategory != RecordTypeCategoryEnum.TRANSFER) -1L else recordEntity.relatedAssetId,
                        concessions = if (typeCategory == RecordTypeCategoryEnum.INCOME) "" else recordEntity.concessions,
                        reimbursable = if (typeCategory != RecordTypeCategoryEnum.EXPENDITURE) false else recordEntity.reimbursable,
                    ),
                    tagIdList = displayTagIdListData.first(),
                    relatedRecordIdList = _relatedRecordIdData.first(),
                    relatedImageList = displayImageData.first().map { it.asModel() },
                )
                Result.success(null)
            }.getOrElse { throwable ->
                // 保存失败
                this@EditRecordViewModel.logger().e(throwable, "onSaveClick()")
                shouldDisplayBookmark = EditRecordBookmarkEnum.SAVE_FAILED
                Result.failure<Any>(throwable)
            }
            if (result.isSuccess) {
                onSuccess.invoke()
            } else {
                inSave = false
            }
        }
    }

    /** 显示选择日期弹窗 */
    fun displayDatePickerDialog() {
        viewModelScope.launch {
            val currentState = uiState.first()
            if (currentState is EditRecordUiState.Success) {
                dialogState =
                    DialogState.Shown(
                        DateTimePickerModel.DatePicker(
                            currentState.dateTimeText.parseDateLong(
                                format = DATE_FORMAT_NO_SECONDS,
                            ),
                        ),
                    )
            }
        }
    }

    /** 日期临时保存，选择时间后才会真正保存 */
    private var dateTemp = ""

    /** 选择日期 */
    fun onDateSelected(dateMs: Long) {
        dateTemp = dateMs.dateFormat(DATE_FORMAT_DATE)
        displayTimePickerDialog()
    }

    /** 显示选择时间弹窗 */
    private fun displayTimePickerDialog() {
        viewModelScope.launch {
            val currentState = uiState.first()
            if (currentState is EditRecordUiState.Success) {
                dialogState =
                    DialogState.Shown(
                        DateTimePickerModel.TimePicker(
                            currentState.dateTimeText.parseDateLong(
                                format = DATE_FORMAT_NO_SECONDS,
                            ),
                        ),
                    )
            }
        }
    }

    /** 选择时间 */
    fun onTimeSelected(time: String) {
        dismissDialog()
        viewModelScope.launch {
            _mutableRecordData.tryEmit(
                _displayRecordData.first().copy(recordTime = "$dateTemp $time"),
            )
        }
    }

    /** 隐藏弹窗 */
    fun dismissDialog() {
        dialogState = DialogState.Dismiss
    }

    fun currentRecord(): Flow<RecordModel> {
        return _displayRecordData
    }

    fun currentRelatedRecord(): Flow<List<Long>> {
        return _relatedRecordIdData
    }

    fun updateRelatedRecord(ids: List<Long>) {
        _mutableRelatedRecordIdData.tryEmit(ids)
    }
}

/** 界面 UI 状态 */
sealed interface EditRecordUiState {
    /** 加载中 */
    data object Loading : EditRecordUiState

    /**
     * 加载完成
     *
     * @param amountText 金额
     * @param chargesText 手续费
     * @param concessionsText 优惠
     * @param remarkText 备注
     * @param selectedAssetId 已选择资产 id
     * @param assetText 资产
     * @param relatedAssetText 关联资产
     * @param dateTimeText 日期时间
     * @param reimbursable 是否可报销
     * @param selectedTypeId 当前选择类型 id
     * @param needRelated 是否需要关联记录
     * @param imageQuality 图片质量
     */
    data class Success(
        val amountText: String,
        val chargesText: String,
        val concessionsText: String,
        val remarkText: String,
        val selectedAssetId: Long,
        val assetText: String,
        val relatedAssetText: String,
        val dateTimeText: String,
        val reimbursable: Boolean,
        val selectedTypeId: Long,
        val needRelated: Boolean,
        val relatedCount: Int,
        val relatedAmount: String,
        val imageQuality: ImageQualityEnum,
    ) : EditRecordUiState
}

data class ImagePreviewData(
    val list: List<ImageViewModel>,
    val index: Int,
)

private fun String.clearZero(): String {
    return if (this.toDoubleOrZero() == 0.0) {
        ""
    } else {
        this
    }
}
