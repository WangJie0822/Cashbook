package cn.wj.android.cashbook.ui.asset.viewmodel

import androidx.lifecycle.MutableLiveData
import cn.wj.android.cashbook.base.ui.BaseViewModel
import cn.wj.android.cashbook.data.constants.ROUTE_PATH_INVISIBLE_ASSET
import cn.wj.android.cashbook.data.model.UiNavigationModel

/**
 * 资产更多菜单 ViewModel
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/7
 */
class AssetMoreMenuViewModel : BaseViewModel() {

    /** 显示选择资产类型弹窗 */
    val showSelectAssetClassificationData: MutableLiveData<Int> = MutableLiveData()

    /** 关闭点击 */
    val onCloseClick: () -> Unit = {
        uiNavigationData.value = UiNavigationModel.builder {
            close()
        }
    }

    /** 添加点击 */
    val onAddClick: () -> Unit = {
        showSelectAssetClassificationData.value = 0
    }

    /** 隐藏资产点击 */
    val onInvisibleAssetClick: () -> Unit = {
        uiNavigationData.value = UiNavigationModel.builder {
            jump(ROUTE_PATH_INVISIBLE_ASSET)
            close()
        }
    }
}