package cn.wj.android.cashbook.feature.tags.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.wj.android.cashbook.core.data.repository.TagRepository
import cn.wj.android.cashbook.core.model.entity.TagEntity
import cn.wj.android.cashbook.core.model.transfer.asEntity
import cn.wj.android.cashbook.core.model.transfer.asModel
import cn.wj.android.cashbook.feature.tags.model.TagDialogState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SelectTagViewModel @Inject constructor(
    private val tagRepository: TagRepository,
) : ViewModel() {

    /** 弹窗状态 */
    var dialogState by mutableStateOf<TagDialogState>(TagDialogState.Dismiss)
        private set

    /** 已选择标签 id 列表数据 */
    val selectedTagIds: MutableStateFlow<List<Long>> = MutableStateFlow(listOf())

    /** 标签数据列表 */
    val tagListData: StateFlow<List<TagEntity>> =
        combine(selectedTagIds, tagRepository.tagListData) { ids, list ->
            list.map {
                it.asEntity(selected = it.id in ids)
            }
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(),
                initialValue = listOf()
            )

    fun onAddClick() {
        dialogState = TagDialogState.Edit(null)
    }

    fun dismissDialog() {
        dialogState = TagDialogState.Dismiss
    }

    fun addTag(tag: TagEntity) {
        viewModelScope.launch {
            tagRepository.updateTag(tag.asModel())
            dismissDialog()
        }
    }
}