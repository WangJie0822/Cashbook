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
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

/**
 * 我的标签 ViewModel
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/7/11
 */
@HiltViewModel
class MyTagsViewModel @Inject constructor(
    tagRepository: TagRepository,
) : ViewModel() {

    var dialogState by mutableStateOf<DialogState>(DialogState.Dismiss)
        private set

    /** 标签数据列表 */
    val tagListData = tagRepository.tagListData
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = emptyList(),
        )

    fun showEditTagDialog(tag: TagModel? = null) {
        dialogState = DialogState.Shown(TagDialogState.Edit(tag))
    }

    fun showDeleteTagDialog(tag: TagModel) {
        dialogState = DialogState.Shown(TagDialogState.Delete(tag))
    }

    fun dismissDialog() {
        dialogState = DialogState.Dismiss
    }
}