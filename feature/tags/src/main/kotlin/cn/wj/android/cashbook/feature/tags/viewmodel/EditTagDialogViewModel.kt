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

package cn.wj.android.cashbook.feature.tags.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.wj.android.cashbook.core.common.ext.logger
import cn.wj.android.cashbook.core.data.repository.TagRepository
import cn.wj.android.cashbook.core.model.model.TagModel
import cn.wj.android.cashbook.core.ui.runCatchWithProgress
import cn.wj.android.cashbook.feature.tags.enums.EditTagDialogBookmarkEnum
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 编辑标签弹窗 ViewModel
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/10/18
 */
@HiltViewModel
class EditTagDialogViewModel @Inject constructor(
    private val tagRepository: TagRepository,
) : ViewModel() {

    private var progressDialogHintText = ""

    var bookmark by mutableStateOf(EditTagDialogBookmarkEnum.DISMISS)
        private set

    fun setProgressDialogHintText(text: String) {
        progressDialogHintText = text
    }

    fun dismissBookmark() {
        bookmark = EditTagDialogBookmarkEnum.DISMISS
    }

    fun saveTag(tag: TagModel, dismissDialog: () -> Unit) {
        viewModelScope.launch {
            if (tagRepository.countTagByName(tag.name) > 0) {
                // 已存在相同名称标签
                bookmark = EditTagDialogBookmarkEnum.NAME_EXIST
                return@launch
            }
            runCatchWithProgress(
                hint = progressDialogHintText,
                cancelable = false,
            ) {
                tagRepository.updateTag(tag)
                dismissDialog()
            }.getOrElse { throwable ->
                this@EditTagDialogViewModel.logger().e(throwable, "saveTag(tag = <$tag>)")
                // 错误提示
                bookmark = EditTagDialogBookmarkEnum.SAVE_FAILED
            }
        }
    }
}
