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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.wj.android.cashbook.core.data.repository.TypeRepository
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.domain.usecase.GetRecordTypeListUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditRecordTypeListViewModel @Inject constructor(
    typeRepository: TypeRepository,
    getRecordTypeListUseCase: GetRecordTypeListUseCase,
) : ViewModel() {

    private val _mutableTypeCategoryData: MutableStateFlow<RecordTypeCategoryEnum> =
        MutableStateFlow(RecordTypeCategoryEnum.EXPENDITURE)
    val currentTypeCategoryData: StateFlow<RecordTypeCategoryEnum> = _mutableTypeCategoryData

    private val _mutableExpenditureTypeIdData: MutableStateFlow<Long> = MutableStateFlow(-1L)
    private val _expenditureTypeListData = _mutableExpenditureTypeIdData.mapLatest {
        if (it == -1L) {
            _mutableExpenditureTypeIdData.tryEmit(
                typeRepository.firstExpenditureTypeListData.first().first().id,
            )
            emptyList()
        } else {
            getRecordTypeListUseCase(RecordTypeCategoryEnum.EXPENDITURE, it)
        }
    }

    private val _mutableIncomeTypeIdData: MutableStateFlow<Long> = MutableStateFlow(-1L)
    private val _incomeTypeListData = _mutableIncomeTypeIdData.mapLatest {
        if (it == -1L) {
            _mutableIncomeTypeIdData.tryEmit(
                typeRepository.firstIncomeTypeListData.first().first().id,
            )
            emptyList()
        } else {
            getRecordTypeListUseCase(RecordTypeCategoryEnum.INCOME, it)
        }
    }

    private val _mutableTransferTypeIdData: MutableStateFlow<Long> = MutableStateFlow(-1L)
    private val _transferTypeListData = _mutableTransferTypeIdData.mapLatest {
        if (it == -1L) {
            _mutableTransferTypeIdData.tryEmit(
                typeRepository.firstTransferTypeListData.first().first().id,
            )
            emptyList()
        } else {
            getRecordTypeListUseCase(RecordTypeCategoryEnum.TRANSFER, it)
        }
    }

    val typeListData = _mutableTypeCategoryData.flatMapLatest {
        when (_mutableTypeCategoryData.first()) {
            RecordTypeCategoryEnum.EXPENDITURE -> _expenditureTypeListData
            RecordTypeCategoryEnum.INCOME -> _incomeTypeListData
            RecordTypeCategoryEnum.TRANSFER -> _transferTypeListData
        }
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = emptyList(),
        )

    val currentSelectedTypeId = combine(
        _mutableTypeCategoryData,
        _mutableExpenditureTypeIdData,
        _mutableIncomeTypeIdData,
        _mutableTransferTypeIdData,
    ) { typeCategory, expenditureTypeId, incomeTypeId, transferTypeId ->
        when (typeCategory) {
            RecordTypeCategoryEnum.EXPENDITURE -> expenditureTypeId
            RecordTypeCategoryEnum.INCOME -> incomeTypeId
            RecordTypeCategoryEnum.TRANSFER -> transferTypeId
        }
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = -1L,
        )

    private var defaultTypeIdSet = false

    fun update(typeCategory: RecordTypeCategoryEnum, defaultTypeId: Long) {
        _mutableTypeCategoryData.tryEmit(typeCategory)
        if (defaultTypeIdSet) {
            return
        }
        defaultTypeIdSet = true
        when (typeCategory) {
            RecordTypeCategoryEnum.EXPENDITURE -> _mutableExpenditureTypeIdData
            RecordTypeCategoryEnum.INCOME -> _mutableIncomeTypeIdData
            RecordTypeCategoryEnum.TRANSFER -> _mutableTransferTypeIdData
        }.tryEmit(defaultTypeId)
    }

    fun updateTypeId(id: Long) {
        viewModelScope.launch {
            when (_mutableTypeCategoryData.first()) {
                RecordTypeCategoryEnum.EXPENDITURE -> _mutableExpenditureTypeIdData
                RecordTypeCategoryEnum.INCOME -> _mutableIncomeTypeIdData
                RecordTypeCategoryEnum.TRANSFER -> _mutableTransferTypeIdData
            }.tryEmit(id)
        }
    }
}
