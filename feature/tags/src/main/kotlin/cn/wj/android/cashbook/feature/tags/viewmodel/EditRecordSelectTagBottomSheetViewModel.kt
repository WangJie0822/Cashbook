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
import cn.wj.android.cashbook.core.model.model.Selectable
import cn.wj.android.cashbook.core.model.model.TagModel
import cn.wj.android.cashbook.core.ui.DialogState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 编辑记录界面选择标签抽屉 ViewModel
 *
 * @param tagRepository 标签数据仓库
 */
@HiltViewModel
class EditRecordSelectTagBottomSheetViewModel @Inject constructor(
    private val tagRepository: TagRepository,
) : ViewModel() {

    /** 弹窗状态 */
    var dialogState by mutableStateOf<DialogState>(DialogState.Dismiss)
        private set

    /** 已选择标签 id 列表数据 */
    private val _selectedTagIdListData: MutableStateFlow<List<Long>> = MutableStateFlow(emptyList())

    /** 标签数据列表 */
    val tagListData: StateFlow<List<Selectable<TagModel>>> =
        combine(_selectedTagIdListData, tagRepository.tagListData) { ids, list ->
            list.map {
                Selectable(
                    data = it,
                    selected = it.id in ids,
                )
            }
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(),
                initialValue = emptyList(),
            )

    private var firstSet = true

    /** 更新已选择标签数据 */
    fun updateSelectedTags(tagIdList: List<Long>) {
        if (firstSet) {
            firstSet = false
            _selectedTagIdListData.tryEmit(tagIdList)
        }
    }

    /** 更新已选择标签列表 */
    fun updateSelectedTagList(id: Long) {
        viewModelScope.launch {
            val newList = _selectedTagIdListData.first().toMutableList()
            if (newList.contains(id)) {
                newList.remove(id)
            } else {
                newList.add(id)
            }
            _selectedTagIdListData.tryEmit(newList)
        }
    }

    /** 显示添加标签弹窗 */
    fun displayAddTagDialog() {
        dialogState = DialogState.Shown(0)
    }

    /** 隐藏弹窗 */
    fun dismissDialog() {
        dialogState = DialogState.Dismiss
    }
}
