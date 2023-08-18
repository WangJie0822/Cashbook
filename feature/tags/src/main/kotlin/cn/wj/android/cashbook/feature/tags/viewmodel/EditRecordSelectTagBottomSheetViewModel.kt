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
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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

    /** 更新已选择标签数据 */
    fun updateSelectedTags(tagIdList: List<Long>) {
        _selectedTagIdListData.tryEmit(tagIdList)
    }

    /** 更新已选择标签列表 */
    fun updateSelectedTagList(id: Long, onResult: (List<Long>) -> Unit) {
        viewModelScope.launch {
            val newList = _selectedTagIdListData.first().toMutableList()
            if (newList.contains(id)) {
                newList.remove(id)
            } else {
                newList.add(id)
            }
            _selectedTagIdListData.tryEmit(newList)
            onResult(newList)
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

    /** 添加标签 */
    fun addTag(tag: TagModel) {
        viewModelScope.launch {
            tagRepository.updateTag(tag)
            dismissDialog()
        }
    }
}