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

package cn.wj.android.cashbook.feature.types.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.wj.android.cashbook.core.common.model.dataVersion
import cn.wj.android.cashbook.core.common.model.updateVersion
import cn.wj.android.cashbook.core.data.repository.RecordRepository
import cn.wj.android.cashbook.core.data.repository.TypeRepository
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.model.enums.TypeLevelEnum
import cn.wj.android.cashbook.core.model.model.RecordTypeModel
import cn.wj.android.cashbook.core.ui.DialogState
import cn.wj.android.cashbook.feature.types.enums.MyCategoriesBookmarkEnum
import cn.wj.android.cashbook.feature.types.model.ExpandableRecordTypeModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 我的类型 ViewModel
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/9/13
 */
@HiltViewModel
class MyCategoriesViewModel @Inject constructor(
    private val typeRepository: TypeRepository,
    private val recordRepository: RecordRepository,
) : ViewModel() {

    var shouldDisplayBookmark by mutableStateOf(MyCategoriesBookmarkEnum.DISMISS)

    var dialogState: DialogState by mutableStateOf(DialogState.Dismiss)

    private val _selectedTabData = MutableStateFlow(RecordTypeCategoryEnum.EXPENDITURE)

    private val _currentTypeList = _selectedTabData.flatMapLatest { typeCategory ->
        when (typeCategory) {
            RecordTypeCategoryEnum.EXPENDITURE -> typeRepository.firstExpenditureTypeListData
            RecordTypeCategoryEnum.INCOME -> typeRepository.firstIncomeTypeListData
            RecordTypeCategoryEnum.TRANSFER -> typeRepository.firstTransferTypeListData
        }
    }

    private val _dataVersion = dataVersion()

    val uiState = combine(_dataVersion, _currentTypeList) { _, typeList ->
        val firstTypeList = typeList.map { first ->
            ExpandableRecordTypeModel(
                data = first,
                list = typeRepository.getSecondRecordTypeListByParentId(first.id)
                    .map { second ->
                        ExpandableRecordTypeModel(
                            data = second,
                            list = emptyList(),
                            reimburseType = typeRepository.isReimburseType(second.id),
                            refundType = typeRepository.isRefundType(second.id),
                            creditCardPaymentType = typeRepository.isCreditPaymentType(second.id),
                        )
                    },
                reimburseType = typeRepository.isReimburseType(first.id),
                refundType = typeRepository.isRefundType(first.id),
                creditCardPaymentType = typeRepository.isCreditPaymentType(first.id),
            )
        }
        MyCategoriesUiState.Success(
            selectedTab = _selectedTabData.first(),
            typeList = firstTypeList,
        )
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = MyCategoriesUiState.Loading,
        )

    fun selectTypeCategory(category: RecordTypeCategoryEnum) {
        _selectedTabData.tryEmit(category)
    }

    fun dismissBookmark() {
        shouldDisplayBookmark = MyCategoriesBookmarkEnum.DISMISS
    }

    fun dismissDialog() {
        dialogState = DialogState.Dismiss
    }

    fun requestChangeFirstTypeToSecond(id: Long) {
        viewModelScope.launch {
            if (typeRepository.getSecondRecordTypeListByParentId(id).isNotEmpty()) {
                // 一级分类下有二级分类，提示
                shouldDisplayBookmark = MyCategoriesBookmarkEnum.CHANGE_FIRST_TYPE_HAS_CHILD
            } else {
                // 可以修改，显示弹窗选择一级分类
                val typeListExcludeCurrent = _currentTypeList.first().filter { it.id != id }
                dialogState = DialogState.Shown(
                    MyCategoriesDialogData.SelectFirstType(
                        id = id,
                        typeList = typeListExcludeCurrent,
                    ),
                )
            }
        }
    }

    fun changeTypeToSecond(id: Long, parentId: Long) {
        viewModelScope.launch {
            typeRepository.changeTypeToSecond(id = id, parentId = parentId)
            dismissDialog()
        }
    }

    fun changeSecondTypeToFirst(id: Long) {
        viewModelScope.launch {
            typeRepository.changeSecondTypeToFirst(id = id)
        }
    }

    fun requestMoveSecondTypeToAnother(id: Long, parentId: Long) {
        viewModelScope.launch {
            // 显示弹窗选择一级分类
            val typeListExcludeCurrent = _currentTypeList.first().filter { it.id != parentId }
            dialogState = DialogState.Shown(
                MyCategoriesDialogData.SelectFirstType(
                    id = id,
                    typeList = typeListExcludeCurrent,
                ),
            )
        }
    }

    fun requestDeleteType(id: Long) {
        viewModelScope.launch {
            typeRepository.getRecordTypeById(id)?.let { type ->
                when {
                    type.protected -> {
                        // 受保护类型，提示
                        shouldDisplayBookmark = MyCategoriesBookmarkEnum.PROTECTED_TYPE
                    }

                    type.typeLevel == TypeLevelEnum.FIRST &&
                        typeRepository.getSecondRecordTypeListByParentId(id)
                            .isNotEmpty() -> {
                        // 一级分类下有二级分类，提示
                        shouldDisplayBookmark = MyCategoriesBookmarkEnum.DELETE_FIRST_TYPE_HAS_CHILD
                    }

                    else -> {
                        val recordSize = recordRepository.queryByTypeId(id).size
                        if (recordSize <= 0) {
                            // 直接删除
                            typeRepository.deleteById(id)
                        } else {
                            // 分类下有记录，提示移动到其它分类
                            dialogState = DialogState.Shown(
                                MyCategoriesDialogData.DeleteType(
                                    id = id,
                                    recordSize = recordSize,
                                    expandableTypeList = _currentTypeList.first()
                                        .filter { it.id != id }
                                        .map { first ->
                                            ExpandableRecordTypeModel(
                                                data = first,
                                                list = typeRepository.getSecondRecordTypeListByParentId(
                                                    first.id,
                                                )
                                                    .filter { it.id != id }
                                                    .map { second ->
                                                        ExpandableRecordTypeModel(
                                                            data = second,
                                                            list = emptyList(),
                                                            reimburseType = false,
                                                            refundType = false,
                                                            creditCardPaymentType = false,
                                                        )
                                                    },
                                                reimburseType = false,
                                                refundType = false,
                                                creditCardPaymentType = false,
                                            )
                                        },
                                ),
                            )
                        }
                    }
                }
            }
        }
    }

    fun changeRecordTypeBeforeDeleteType(id: Long, toId: Long) {
        viewModelScope.launch {
            recordRepository.changeRecordTypeBeforeDeleteType(fromId = id, toId = toId)
            typeRepository.deleteById(id)
            dismissDialog()
        }
    }

    fun requestEditType(id: Long, parentId: Long) {
        viewModelScope.launch {
            dialogState = DialogState.Shown(
                MyCategoriesDialogData.EditType(
                    type = typeRepository.getRecordTypeById(id),
                    parentType = typeRepository.getRecordTypeById(parentId),
                ),
            )
        }
    }

    fun saveRecordType(id: Long, parentId: Long, name: String, iconName: String) {
        viewModelScope.launch {
            var count = typeRepository.countByName(name)
            val recordTypeById = typeRepository.getRecordTypeById(id)
            if (recordTypeById?.name == name) {
                count--
            }
            if (count > 0) {
                // 类型名称不能相同
                shouldDisplayBookmark = MyCategoriesBookmarkEnum.DUPLICATE_TYPE_NAME
            } else {
                // 更新类型数据
                val model = (
                    recordTypeById ?: RecordTypeModel(
                        id = id,
                        parentId = parentId,
                        name = name,
                        iconName = iconName,
                        typeLevel = if (parentId == -1L) TypeLevelEnum.FIRST else TypeLevelEnum.SECOND,
                        typeCategory = _selectedTabData.first(),
                        protected = false,
                        sort = typeRepository.generateSortById(id, parentId),
                        needRelated = false,
                    )
                    ).copy(
                    id = id,
                    parentId = parentId,
                    name = name,
                    iconName = iconName,
                )
                typeRepository.update(model)
            }
            dismissDialog()
        }
    }

    fun setReimburseType(id: Long) {
        viewModelScope.launch {
            typeRepository.setReimburseType(id)
            _dataVersion.updateVersion()
        }
    }

    fun setRefundType(id: Long) {
        viewModelScope.launch {
            typeRepository.setRefundType(id)
            _dataVersion.updateVersion()
        }
    }

    fun setCreditCardPaymentType(id: Long) {
        viewModelScope.launch {
            typeRepository.setCreditPaymentType(id)
            _dataVersion.updateVersion()
        }
    }
}

sealed interface MyCategoriesDialogData {
    data class SelectFirstType(
        val id: Long,
        val typeList: List<RecordTypeModel>,
    ) : MyCategoriesDialogData

    data class DeleteType(
        val id: Long,
        val recordSize: Int,
        val expandableTypeList: List<ExpandableRecordTypeModel>,
    ) : MyCategoriesDialogData

    data class EditType(
        val type: RecordTypeModel?,
        val parentType: RecordTypeModel?,
    ) : MyCategoriesDialogData
}

sealed interface MyCategoriesUiState {
    data object Loading : MyCategoriesUiState
    data class Success(
        val selectedTab: RecordTypeCategoryEnum,
        val typeList: List<ExpandableRecordTypeModel>,
    ) : MyCategoriesUiState
}
