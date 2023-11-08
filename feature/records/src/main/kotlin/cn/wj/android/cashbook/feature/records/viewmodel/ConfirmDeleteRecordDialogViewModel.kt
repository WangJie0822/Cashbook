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
import cn.wj.android.cashbook.core.common.ext.logger
import cn.wj.android.cashbook.core.model.model.ResultModel
import cn.wj.android.cashbook.core.ui.runCatchWithProgress
import cn.wj.android.cashbook.domain.usecase.DeleteRecordUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 确认删除记录弹窗 ViewModel
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/7/4
 */
@HiltViewModel
class ConfirmDeleteRecordDialogViewModel @Inject constructor(
    private val deleteRecordUseCase: DeleteRecordUseCase,
) : ViewModel() {

    fun onDeleteRecordConfirm(hintText: String, recordId: Long, onResult: (ResultModel) -> Unit) {
        viewModelScope.launch {
            runCatchWithProgress(hint = hintText, cancelable = false) {
                deleteRecordUseCase(recordId)
                // 删除成功
                onResult.invoke(ResultModel.success())
            }.getOrElse { throwable ->
                this@ConfirmDeleteRecordDialogViewModel.logger()
                    .e(throwable, "onDeleteRecordConfirm(recordId = <$recordId>) failed")
                // 提示
                onResult.invoke(ResultModel.failure(throwable))
            }
        }
    }
}
