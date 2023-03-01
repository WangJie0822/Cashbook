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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class MyTagsViewModel @Inject constructor(
    private val tagRepository: TagRepository
) : ViewModel() {

    var dialogState by mutableStateOf<TagDialogState>(TagDialogState.Dismiss)
        private set

    /** 标签数据列表 */
    val tagListData = tagRepository.tagListData
        .map { list ->
            list.map { it.asEntity() }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = listOf()
        )

    fun showEditTagDialog(tag: TagEntity? = null) {
        dialogState = TagDialogState.Edit(tag)
    }

    fun showDeleteTagDialog(tag: TagEntity) {
        dialogState = TagDialogState.Delete(tag)
    }

    fun dismissDialog() {
        dialogState = TagDialogState.Dismiss
    }

    fun modifyTag(tag: TagEntity) {
        viewModelScope.launch {
            tagRepository.updateTag(tag.asModel())
            dismissDialog()
        }
    }

    fun deleteTag(tag: TagEntity) {
        viewModelScope.launch {
            tagRepository.deleteTag(tag.asModel())
            dismissDialog()
        }
    }
}