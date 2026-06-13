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
import cn.wj.android.cashbook.core.common.model.recordDataVersion
import cn.wj.android.cashbook.core.model.entity.RecordViewsEntity
import cn.wj.android.cashbook.domain.usecase.GetReimbursableUnrelatedRecordViewsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * 待报销管理界面 ViewModel
 *
 * 监听全局 [recordDataVersion]，记录增删改后自动重查，保证详情弹窗内操作后列表刷新。
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2026/6/13
 */
@HiltViewModel
class ReimbursementViewModel @Inject constructor(
    private val getReimbursableUnrelatedRecordViewsUseCase: GetReimbursableUnrelatedRecordViewsUseCase,
) : ViewModel() {

    /** 需显示详情的记录数据 */
    var viewRecord by mutableStateOf<RecordViewsEntity?>(null)
        private set

    val uiState: StateFlow<ReimbursementUiState> = recordDataVersion
        .mapLatest {
            val data = getReimbursableUnrelatedRecordViewsUseCase()
            ReimbursementUiState.Success(
                records = data.records,
                count = data.count,
                totalAmount = data.totalAmount,
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = ReimbursementUiState.Loading,
        )

    fun showRecordDetailsSheet(item: RecordViewsEntity) {
        viewRecord = item
    }

    fun dismissRecordDetailSheet() {
        viewRecord = null
    }
}

sealed interface ReimbursementUiState {
    data object Loading : ReimbursementUiState
    data class Success(
        val records: List<RecordViewsEntity>,
        val count: Int,
        val totalAmount: Long,
    ) : ReimbursementUiState
}
