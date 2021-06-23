package cn.wj.android.cashbook.ui.record.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.base.ext.base.logger
import cn.wj.android.cashbook.base.ext.base.string
import cn.wj.android.cashbook.base.ui.BaseViewModel
import cn.wj.android.cashbook.data.constants.EVENT_TAG_CHANGE
import cn.wj.android.cashbook.data.entity.TagEntity
import cn.wj.android.cashbook.data.event.LifecycleEvent
import cn.wj.android.cashbook.data.model.UiNavigationModel
import cn.wj.android.cashbook.data.store.LocalDataStore
import com.jeremyliao.liveeventbus.LiveEventBus
import kotlinx.coroutines.launch

/**
 * 编辑标签 ViewModel
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/23
 */
class EditTagViewModel(private val local: LocalDataStore) : BaseViewModel() {

    /** 成功事件 */
    val successEvent: LifecycleEvent<TagEntity> = LifecycleEvent()

    /** 编辑对象 */
    val editTag: MutableLiveData<TagEntity> = object : MutableLiveData<TagEntity>(null) {
        override fun setValue(value: TagEntity?) {
            super.setValue(value)
            name.value = value?.name.orEmpty()
        }
    }

    /** 标题文本 */
    val titleStr: LiveData<String> = editTag.map {
        if (null == it) {
            R.string.create_tag
        } else {
            R.string.edit_tag
        }.string
    }

    /** 错误文本 */
    val nameError: MutableLiveData<String> = MutableLiveData()

    /** 标签名称 */
    val name: MutableLiveData<String> = object : MutableLiveData<String>() {
        override fun setValue(value: String?) {
            super.setValue(value)
            nameError.value = ""
        }
    }

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
                val edit = editTag.value
                val result = if (null == edit) {
                    // 新建，保存标签
                    val changed = TagEntity(-1L, name)
                    val id = local.insertTag(changed)
                    changed.copy(id = id)
                } else {
                    // 编辑
                    val changed = edit.copy(name = name)
                    local.updateTag(changed)
                    changed
                }
                LiveEventBus.get(EVENT_TAG_CHANGE).post(result)
                successEvent.value = result
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