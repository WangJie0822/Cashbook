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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.wj.android.cashbook.domain.usecase.UpdateRecordReimbursedUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 记录详情弹窗 ViewModel：承载「标记已报销 / 改回待报销」写动作。
 *
 * 写库后由 Repository bump 全局 recordDataVersion，触发待报销列表/主列表/详情自动刷新。
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2026/6/24
 */
@HiltViewModel
class RecordDetailsSheetViewModel @Inject constructor(
    private val updateRecordReimbursedUseCase: UpdateRecordReimbursedUseCase,
) : ViewModel() {

    fun markReimbursed(recordId: Long, reimbursed: Boolean) {
        viewModelScope.launch {
            updateRecordReimbursedUseCase(recordId, reimbursed)
        }
    }
}
