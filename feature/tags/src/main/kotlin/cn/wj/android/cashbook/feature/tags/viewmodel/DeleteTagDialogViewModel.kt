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
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

/**
 * 删除标签弹窗 ViewModel
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/10/18
 */
@HiltViewModel
class DeleteTagDialogViewModel @Inject constructor(
    private val tagRepository: TagRepository,
) : ViewModel() {

    private var progressDialogHintText = ""

    var bookmark by mutableStateOf(false)
        private set

    fun setProgressDialogHintText(text: String) {
        progressDialogHintText = text
    }

    fun dismissBookmark() {
        bookmark = false
    }

    fun deleteTag(tag: TagModel, dismissDialog: () -> Unit) {
        viewModelScope.launch {
            runCatchWithProgress(
                hint = progressDialogHintText,
                cancelable = false,
            ) {
                tagRepository.deleteTag(tag)
                dismissDialog()
            }.getOrElse { throwable ->
                this@DeleteTagDialogViewModel.logger().e(throwable, "deleteTag(tag = <$tag>)")
                // 错误提示
                bookmark = true
            }
        }
    }
}