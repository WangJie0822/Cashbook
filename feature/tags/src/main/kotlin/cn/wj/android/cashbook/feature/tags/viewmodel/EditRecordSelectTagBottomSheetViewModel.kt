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

@HiltViewModel
class EditRecordSelectTagBottomSheetViewModel @Inject constructor(
    private val tagRepository: TagRepository,
) : ViewModel() {

    /** 弹窗状态 */
    var dialogState by mutableStateOf<DialogState>(DialogState.Dismiss)
        private set

    /** 已选择标签 id 列表数据 */
    private val selectedTagIdListData: MutableStateFlow<List<Long>> = MutableStateFlow(listOf())

    /** 标签数据列表 */
    val tagListData: StateFlow<List<Selectable<TagModel>>> =
        combine(selectedTagIdListData, tagRepository.tagListData) { ids, list ->
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
                initialValue = listOf()
            )

    fun updateSelectedTags(tagIdList: List<Long>) {
        selectedTagIdListData.tryEmit(tagIdList)
    }

    fun onTagItemClick(id: Long, onResult: (List<Long>) -> Unit) {
        viewModelScope.launch {
            val newList = selectedTagIdListData.first().toMutableList()
            if (newList.contains(id)) {
                newList.remove(id)
            } else {
                newList.add(id)
            }
            selectedTagIdListData.tryEmit(newList)
            onResult(newList)
        }
    }

    fun onAddClick() {
        dialogState = DialogState.Shown(0)
    }

    fun dismissDialog() {
        dialogState = DialogState.Dismiss
    }

    fun addTag(tag: TagModel) {
        viewModelScope.launch {
            tagRepository.updateTag(tag)
            dismissDialog()
        }
    }
}