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

package cn.wj.android.cashbook.feature.schedule.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.wj.android.cashbook.core.common.ext.toAmountCent
import cn.wj.android.cashbook.core.common.ext.toMoneyFormat
import cn.wj.android.cashbook.core.data.repository.AssetRepository
import cn.wj.android.cashbook.core.data.repository.ScheduleRepository
import cn.wj.android.cashbook.core.data.repository.SettingRepository
import cn.wj.android.cashbook.core.data.repository.TagRepository
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.model.enums.ScheduleFrequencyEnum
import cn.wj.android.cashbook.core.model.model.ScheduleModel
import cn.wj.android.cashbook.core.ui.DialogState
import cn.wj.android.cashbook.core.ui.ProgressDialogController
import cn.wj.android.cashbook.core.ui.runCatchWithProgress
import cn.wj.android.cashbook.domain.usecase.GenerateScheduleRecordsUseCase
import cn.wj.android.cashbook.domain.usecase.SaveScheduleUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

/**
 * 编辑周期规则 ViewModel
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2026/4/20
 */
@HiltViewModel
class EditScheduleViewModel @Inject constructor(
    private val scheduleRepository: ScheduleRepository,
    private val saveScheduleUseCase: SaveScheduleUseCase,
    private val generateScheduleRecordsUseCase: GenerateScheduleRecordsUseCase,
    private val settingRepository: SettingRepository,
    private val assetRepository: AssetRepository,
    private val tagRepository: TagRepository,
) : ViewModel() {

    /** 弹窗状态 */
    var dialogState by mutableStateOf<DialogState>(DialogState.Dismiss)
        private set

    /** 底部 sheet 类型 */
    var bottomSheetType by mutableStateOf(EditScheduleBottomSheetEnum.NONE)
        private set

    /** 记录 id */
    private val _scheduleIdData = MutableStateFlow(-1L)

    /** 记录数据 */
    private val _mutableScheduleData = MutableStateFlow<ScheduleModel?>(null)
    private val _defaultScheduleData = _scheduleIdData.mapLatest { getDefaultSchedule(it) }
    private val _displayScheduleData =
        combine(_mutableScheduleData, _defaultScheduleData) { mutable, default ->
            mutable ?: default
        }

    /** 界面 UI 状态 */
    val uiState: Flow<EditScheduleUiState> = _displayScheduleData
        .mapLatest { schedule ->
            val assetText = assetRepository.getAssetById(schedule.assetId)?.name.orEmpty()
            val tagText = schedule.tagIdList.mapNotNull {
                tagRepository.getTagById(it)?.name
            }.joinToString(",")
            EditScheduleUiState.Success(
                amountText = schedule.amount.toMoneyFormat(),
                chargesText = schedule.charges.toMoneyFormat(),
                concessionsText = schedule.concessions.toMoneyFormat(),
                typeId = schedule.typeId,
                typeCategory = schedule.typeCategory,
                assetId = schedule.assetId,
                assetText = assetText,
                frequency = schedule.frequency,
                startDate = schedule.startDate,
                endDate = schedule.endDate,
                recordTime = schedule.recordTime,
                remark = schedule.remark,
                enabled = schedule.enabled,
                reimbursable = schedule.reimbursable,
                tagIdList = schedule.tagIdList,
                tagText = tagText,
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = EditScheduleUiState.Loading,
        )

    /** 更新周期规则 id */
    fun updateScheduleId(id: Long) {
        _scheduleIdData.tryEmit(id)
        if (id != -1L) {
            viewModelScope.launch {
                scheduleRepository.queryById(id)?.let {
                    _mutableScheduleData.tryEmit(it)
                }
            }
        }
    }

    /** 更新金额 */
    fun updateAmount(amount: String) {
        viewModelScope.launch {
            _mutableScheduleData.tryEmit(
                _displayScheduleData.first().copy(amount = amount.toAmountCent()),
            )
        }
    }

    /** 更新手续费 */
    fun updateCharges(charges: String) {
        viewModelScope.launch {
            _mutableScheduleData.tryEmit(
                _displayScheduleData.first().copy(charges = charges.toAmountCent()),
            )
        }
    }

    /** 更新优惠 */
    fun updateConcessions(concessions: String) {
        viewModelScope.launch {
            _mutableScheduleData.tryEmit(
                _displayScheduleData.first().copy(concessions = concessions.toAmountCent()),
            )
        }
    }

    /** 更新类型 */
    fun updateType(typeId: Long, typeCategory: RecordTypeCategoryEnum) {
        viewModelScope.launch {
            _mutableScheduleData.tryEmit(
                _displayScheduleData.first().copy(
                    typeId = typeId,
                    typeCategory = typeCategory,
                ),
            )
        }
    }

    /** 更新类型大类（Tab 切换） */
    fun updateTypeCategory(typeCategory: RecordTypeCategoryEnum) {
        viewModelScope.launch {
            _mutableScheduleData.tryEmit(
                _displayScheduleData.first().copy(
                    typeId = -1L,
                    typeCategory = typeCategory,
                ),
            )
        }
    }

    /** 更新资产 */
    fun updateAsset(assetId: Long) {
        viewModelScope.launch {
            _mutableScheduleData.tryEmit(
                _displayScheduleData.first().copy(assetId = assetId),
            )
        }
    }

    /** 更新频率 */
    fun updateFrequency(frequency: ScheduleFrequencyEnum) {
        viewModelScope.launch {
            _mutableScheduleData.tryEmit(
                _displayScheduleData.first().copy(frequency = frequency),
            )
        }
    }

    /** 更新开始日期 */
    fun updateStartDate(startDate: Long) {
        viewModelScope.launch {
            _mutableScheduleData.tryEmit(
                _displayScheduleData.first().copy(startDate = startDate),
            )
        }
    }

    /** 更新结束日期 */
    fun updateEndDate(endDate: Long?) {
        viewModelScope.launch {
            _mutableScheduleData.tryEmit(
                _displayScheduleData.first().copy(endDate = endDate),
            )
        }
    }

    /** 更新记账时间 */
    fun updateRecordTime(recordTime: Long) {
        viewModelScope.launch {
            _mutableScheduleData.tryEmit(
                _displayScheduleData.first().copy(recordTime = recordTime),
            )
        }
    }

    /** 更新备注 */
    fun updateRemark(remark: String) {
        viewModelScope.launch {
            _mutableScheduleData.tryEmit(
                _displayScheduleData.first().copy(remark = remark),
            )
        }
    }

    /** 更新启用状态 */
    fun updateEnabled(enabled: Boolean) {
        viewModelScope.launch {
            _mutableScheduleData.tryEmit(
                _displayScheduleData.first().copy(enabled = enabled),
            )
        }
    }

    /** 更新可报销状态 */
    fun updateReimbursable(reimbursable: Boolean) {
        viewModelScope.launch {
            _mutableScheduleData.tryEmit(
                _displayScheduleData.first().copy(reimbursable = reimbursable),
            )
        }
    }

    /** 更新标签列表 */
    fun updateTagIdList(tagIdList: List<Long>) {
        viewModelScope.launch {
            _mutableScheduleData.tryEmit(
                _displayScheduleData.first().copy(tagIdList = tagIdList),
            )
        }
    }

    /** 显示选择类型 sheet */
    fun showSelectTypeSheet() {
        bottomSheetType = EditScheduleBottomSheetEnum.TYPE
    }

    /** 显示选择资产 sheet */
    fun showSelectAssetSheet() {
        bottomSheetType = EditScheduleBottomSheetEnum.ASSET
    }

    /** 显示选择频率 sheet */
    fun showSelectFrequencySheet() {
        bottomSheetType = EditScheduleBottomSheetEnum.FREQUENCY
    }

    /** 显示金额计算器 sheet */
    fun showAmountSheet() {
        bottomSheetType = EditScheduleBottomSheetEnum.AMOUNT
    }

    /** 显示手续费计算器 sheet */
    fun showChargesSheet() {
        bottomSheetType = EditScheduleBottomSheetEnum.CHARGES
    }

    /** 显示优惠计算器 sheet */
    fun showConcessionsSheet() {
        bottomSheetType = EditScheduleBottomSheetEnum.CONCESSIONS
    }

    /** 显示选择标签 sheet */
    fun showSelectTagSheet() {
        bottomSheetType = EditScheduleBottomSheetEnum.TAG
    }

    /** 隐藏 sheet */
    fun dismissBottomSheet() {
        bottomSheetType = EditScheduleBottomSheetEnum.NONE
    }

    /** 隐藏弹窗 */
    fun dismissDialog() {
        dialogState = DialogState.Dismiss
    }

    /** 保存 */
    fun trySave(
        controller: ProgressDialogController,
        hintText: String,
        onSuccess: () -> Unit,
    ) {
        viewModelScope.launch {
            runCatchWithProgress(controller, hintText) {
                val schedule = _displayScheduleData.first()
                saveScheduleUseCase(schedule)
                // 保存成功后，立即检查并生成逾期记录
                if (schedule.enabled) {
                    generateScheduleRecordsUseCase()
                }
                onSuccess()
            }
        }
    }

    private suspend fun getDefaultSchedule(scheduleId: Long): ScheduleModel {
        val appData = settingRepository.recordSettingsModel.first()
        val calendar = Calendar.getInstance()
        // 今天零点作为开始日期
        val startDate = calendar.apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        return ScheduleModel(
            id = scheduleId,
            booksId = appData.currentBookId,
            typeId = -1L,
            assetId = appData.lastAssetId,
            amount = 0L,
            charges = 0L,
            concessions = 0L,
            remark = "",
            typeCategory = RecordTypeCategoryEnum.EXPENDITURE,
            frequency = ScheduleFrequencyEnum.MONTHLY,
            startDate = startDate,
            endDate = null,
            recordTime = System.currentTimeMillis(),
            lastExecutedDate = null,
            enabled = true,
            reimbursable = false,
            tagIdList = emptyList(),
        )
    }
}

sealed interface EditScheduleUiState {
    data object Loading : EditScheduleUiState
    data class Success(
        val amountText: String,
        val chargesText: String,
        val concessionsText: String,
        val typeId: Long,
        val typeCategory: RecordTypeCategoryEnum,
        val assetId: Long,
        val assetText: String,
        val frequency: ScheduleFrequencyEnum,
        val startDate: Long,
        val endDate: Long?,
        val recordTime: Long,
        val remark: String,
        val enabled: Boolean,
        val reimbursable: Boolean,
        val tagIdList: List<Long>,
        val tagText: String,
    ) : EditScheduleUiState
}

enum class EditScheduleBottomSheetEnum {

    NONE,

    TYPE,

    ASSET,

    FREQUENCY,

    AMOUNT,

    CHARGES,

    CONCESSIONS,

    TAG,
    ;

    /** 是否为计算器类型 */
    val isCalculator: Boolean
        get() = this == AMOUNT || this == CHARGES || this == CONCESSIONS
}
