package cn.wj.android.cashbook.ui.record.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.base.ext.base.logger
import cn.wj.android.cashbook.base.ext.base.string
import cn.wj.android.cashbook.base.ui.BaseViewModel
import cn.wj.android.cashbook.data.entity.TagEntity
import cn.wj.android.cashbook.data.model.UiNavigationModel
import cn.wj.android.cashbook.data.store.LocalDataStore
import kotlinx.coroutines.launch

/**
 * 新建标签 ViewModel
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/23
 */
class CreateTagViewModel(private val local: LocalDataStore) : BaseViewModel() {

    /** 错误文本 */
    val nameError: MutableLiveData<String> = MutableLiveData()

    /** 标签名称 */
    val name: MutableLiveData<String> = MutableLiveData()

    /** 取消点击 */
    val onCancelClick: () -> Unit = {
        uiNavigationEvent.value = UiNavigationModel.builder {
            close()
        }
    }

    /** 确认点击 */
    val onConfirmClick: () -> Unit = {
        trySaveTag()
    }

    /** 保存标签 */
    private fun trySaveTag() {
        val name = name.value.orEmpty()
        if (name.isBlank()) {
            nameError.value = R.string.tag_name_must_not_be_blank.string
            return
        }
        viewModelScope.launch {
            try {
                if (local.queryTagByName(name).isNotEmpty()) {
                    // 已有标签
                    nameError.value = R.string.same_tag_exist.string
                    return@launch
                }
                // 保存标签
                local.insertTag(TagEntity(-1L, name))
                // 保存成功，关闭弹窗
                uiNavigationEvent.value = UiNavigationModel.builder {
                    close()
                }
            } catch (throwable: Throwable) {
                logger().e(throwable, "trySaveTag")
            }
        }
    }
}