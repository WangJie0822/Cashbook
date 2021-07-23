package cn.wj.android.cashbook.ui.asset.viewmodel

import cn.wj.android.cashbook.base.ui.BaseViewModel
import cn.wj.android.cashbook.data.constants.ROUTE_PATH_ASSET_INVISIBLE
import cn.wj.android.cashbook.data.event.LifecycleEvent
import cn.wj.android.cashbook.data.model.UiNavigationModel

/**
 * 资产更多菜单 ViewModel
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/7
 */
class AssetMoreMenuViewModel : BaseViewModel() {

    /** 显示选择资产类型事件 */
    val showSelectAssetClassificationData: LifecycleEvent<Int> = LifecycleEvent()

    /** 关闭点击 */
    val onCloseClick: () -> Unit = {
        uiNavigationEvent.value = UiNavigationModel.builder {
            close()
        }
    }

    /** 添加点击 */
    val onAddClick: () -> Unit = {
        showSelectAssetClassificationData.value = 0
    }

    /** 隐藏资产点击 */
    val onInvisibleAssetClick: () -> Unit = {
        uiNavigationEvent.value = UiNavigationModel.builder {
            jump(ROUTE_PATH_ASSET_INVISIBLE)
            close()
        }
    }
}