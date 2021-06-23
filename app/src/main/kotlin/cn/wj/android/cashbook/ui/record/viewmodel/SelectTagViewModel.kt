package cn.wj.android.cashbook.ui.record.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import cn.wj.android.cashbook.base.ext.base.logger
import cn.wj.android.cashbook.base.ext.base.orFalse
import cn.wj.android.cashbook.base.ui.BaseViewModel
import cn.wj.android.cashbook.data.entity.TagEntity
import cn.wj.android.cashbook.data.event.LifecycleEvent
import cn.wj.android.cashbook.data.model.UiNavigationModel
import cn.wj.android.cashbook.data.store.LocalDataStore
import kotlinx.coroutines.launch

/**
 * 选择标签 ViewModel
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/23
 */
class SelectTagViewModel(private val local: LocalDataStore) : BaseViewModel() {

    /** 确认点击事件 */
    val confirmClickEvent: LifecycleEvent<List<TagEntity>> = LifecycleEvent()

    /** 显示创建标签弹窗 */
    val showCreateTagDialogEvent: LifecycleEvent<Int> = LifecycleEvent()

    /** 已选中数据 */
    val selectedTags: MutableLiveData<List<TagEntity>> = MutableLiveData()

    /** 标签数据列表 */
    val tagListData: MutableLiveData<List<TagEntity>> = object : MutableLiveData<List<TagEntity>>() {
        override fun onActive() {
            // 自动获取数据
            refreshTagList()
        }
    }

    /** 底部隐藏 */
    val onBottomSheetHidden: () -> Unit = {
        uiNavigationEvent.value = UiNavigationModel.builder {
            close()
        }
    }

    /** 添加点击 */
    val onAddClick: () -> Unit = {
        showCreateTagDialogEvent.value = 0
        uiNavigationEvent.value = UiNavigationModel.builder {
            close()
        }
    }

    /** 确认点击 */
    val onConfirmClick: () -> Unit = {
        // 回调已选中数据
        confirmClickEvent.value = tagListData.value?.filter {
            it.selected.get()
        }.orEmpty()
    }

    /** 刷新标签数据 */
    private fun refreshTagList() {
        viewModelScope.launch {
            try {
                // 获取标签列表
                val list = local.getTagList()
                val selectedTagIds = selectedTags.value?.map {
                    it.id
                }.orEmpty()
                // 更新选中状态
                list.forEach {
                    it.selected.set(selectedTagIds.contains(it.id).orFalse())
                }
                tagListData.value = list
            } catch (throwable: Throwable) {
                logger().e(throwable, "refreshTag")
            }
        }
    }
}