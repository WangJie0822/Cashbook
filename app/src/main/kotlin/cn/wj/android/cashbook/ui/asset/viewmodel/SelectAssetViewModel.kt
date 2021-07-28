package cn.wj.android.cashbook.ui.asset.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import cn.wj.android.cashbook.base.ext.base.logger
import cn.wj.android.cashbook.base.ui.BaseViewModel
import cn.wj.android.cashbook.data.entity.AssetEntity
import cn.wj.android.cashbook.data.event.LifecycleEvent
import cn.wj.android.cashbook.data.model.UiNavigationModel
import cn.wj.android.cashbook.data.repository.asset.AssetRepository
import cn.wj.android.cashbook.interfaces.AssetListClickListener
import kotlinx.coroutines.launch

/**
 * 选择资产账户 ViewModel
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/1
 */
class SelectAssetViewModel(private val repository: AssetRepository) : BaseViewModel(), AssetListClickListener {

    /** 显示选择资产类别弹窗事件 */
    val showSelectAssetTypeEvent: LifecycleEvent<Int> = LifecycleEvent()

    /** 选中的资产数据 */
    val selectedAssetEvent: LifecycleEvent<AssetEntity> = LifecycleEvent()

    /** 资产列表数据 */
    val assetListData: MutableLiveData<List<AssetEntity>> = object : MutableLiveData<List<AssetEntity>>() {
        override fun onActive() {
            loadVisibleAssetData()
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
        showSelectAssetTypeEvent.value = 0
    }

    /** 资产 item 点击 */
    override val onAssetItemClick: (AssetEntity) -> Unit = { item ->
        selectedAssetEvent.value = item
    }

    /** 资产 item 长点击 */
    override val onAssetItemLongClick: (AssetEntity) -> Unit get() = {}

    /** 加载未隐藏资产数据 */
    private fun loadVisibleAssetData() {
        viewModelScope.launch {
            try {
                assetListData.value = repository.getVisibleAssetListSortByRecord()
            } catch (throwable: Throwable) {
                logger().e(throwable, "loadVisibleAssetData")
            }
        }
    }
}