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
import cn.wj.android.cashbook.core.data.repository.TagRepository
import cn.wj.android.cashbook.core.model.model.TagModel
import cn.wj.android.cashbook.core.ui.DialogState
import cn.wj.android.cashbook.feature.tags.model.TagDialogState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 我的标签 ViewModel
 *
 * @param tagRepository 标签数据仓库
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/7/11
 */
@HiltViewModel
class MyTagsViewModel @Inject constructor(
    private val tagRepository: TagRepository,
) : ViewModel() {

    /** 弹窗状态 */
    var dialogState by mutableStateOf<DialogState>(DialogState.Dismiss)
        private set

    /** 标签数据列表 */
    val tagListData = tagRepository.tagListData
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = emptyList(),
        )

    /** 切换标签 [tag] 的隐藏状态 */
    fun switchInvisibleState(tag: TagModel) {
        viewModelScope.launch {
            tagRepository.updateTag(tag.copy(invisible = !tag.invisible))
        }
    }

    /** 显示编辑标签弹窗，[tag] 为 `null` 是为创建标签 */
    fun showEditTagDialog(tag: TagModel?) {
        dialogState = DialogState.Shown(TagDialogState.Edit(tag))
    }

    /** 显示删除标签 [tag] 的确认弹窗 */
    fun showDeleteTagDialog(tag: TagModel) {
        dialogState = DialogState.Shown(TagDialogState.Delete(tag))
    }

    /** 隐藏弹窗 */
    fun dismissDialog() {
        dialogState = DialogState.Dismiss
    }
}
