package cn.wj.android.cashbook.ui.record.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import cn.wj.android.cashbook.base.ext.base.logger
import cn.wj.android.cashbook.base.ext.base.toNewList
import cn.wj.android.cashbook.base.ui.BaseViewModel
import cn.wj.android.cashbook.data.constants.EVENT_TAG_DELETE
import cn.wj.android.cashbook.data.entity.TagEntity
import cn.wj.android.cashbook.data.event.LifecycleEvent
import cn.wj.android.cashbook.data.model.ProgressModel
import cn.wj.android.cashbook.data.model.UiNavigationModel
import cn.wj.android.cashbook.data.repository.record.RecordRepository
import com.jeremyliao.liveeventbus.LiveEventBus
import kotlinx.coroutines.launch

/**
 * 标签管理 ViewModel
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2022/8/8
 */
class TagManagerViewModel(private val repository: RecordRepository) : BaseViewModel() {

    /** 显示创建标签弹窗 */
    val showCreateTagDialogEvent: LifecycleEvent<Int> = LifecycleEvent()

    /** 显示菜单事件 */
    val showMenuEvent: LifecycleEvent<TagEntity> = LifecycleEvent()

    /** 标签列表数据 */
    val tagListData: MutableLiveData<ArrayList<TagEntity>> = MutableLiveData()

    /** 返回按钮点击 */
    val onBackClick: () -> Unit = {
        // 退出当前界面
        uiNavigationEvent.value = UiNavigationModel.builder {
            close()
        }
    }

    /** 添加按钮点击 */
    val onAddClick: () -> Unit = {
        showCreateTagDialogEvent.value = 0
    }

    /** 标签 Item 点击 */
    val onTagItemClick: (TagEntity) -> Unit = {
        showMenuEvent.value = it
    }

    /**
     * 刷新标签列表
     */
    fun refreshTagList() {
        viewModelScope.launch {
            try {
                progressEvent.value = ProgressModel()
                val result = repository.getTagList()
                tagListData.value = ArrayList(result)
            } catch (throwable: Throwable) {
                logger().e(throwable, "refreshTagList")
            } finally {
                progressEvent.value = null
            }
        }
    }

    /** 插入新标签 [tag] */
    fun insertTag(tag: TagEntity) {
        val list = tagListData.value.toNewList()
        list.add(tag)
        tagListData.value = list
    }

    /** 删除标签 */
    fun deleteTag(tag: TagEntity) {
        viewModelScope.launch {
            try {
                // 删除标签
                repository.deleteTag(tag)
                // 移除已删除标签
                val list = tagListData.value.toNewList()
                list.remove(tag)
                tagListData.value = list
                LiveEventBus.get<TagEntity>(EVENT_TAG_DELETE).post(tag)
            } catch (throwable: Throwable) {
                logger().e(throwable, "deleteTag")
            }
        }
    }

    /** 更新标签 [tag] */
    fun updateTag(tag: TagEntity) {
        val list = tagListData.value.toNewList()
        val index = list.indexOfFirst { it.id == tag.id }
        if (index >= 0) {
            list[index] = tag
        }
        tagListData.value = list
    }
}