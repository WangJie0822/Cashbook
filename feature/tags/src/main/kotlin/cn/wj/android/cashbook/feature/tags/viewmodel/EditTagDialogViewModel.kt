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
import javax.inject.Inject
import kotlinx.coroutines.launch

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