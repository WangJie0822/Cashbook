package cn.wj.android.cashbook.ui.asset.viewmodel

import androidx.lifecycle.MutableLiveData
import cn.wj.android.cashbook.base.ui.BaseViewModel

/**
 * 资产长按菜单 ViewModel
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/7
 */
class AssetLongClickMenuViewModel : BaseViewModel() {

    /** 编辑点击数据 */
    val editClickData: MutableLiveData<Int> = MutableLiveData()

    /** 隐藏点击数据 */
    val hiddenClickData: MutableLiveData<Int> = MutableLiveData()

    /** 编辑点击 */
    val onEditClick: () -> Unit = {
        editClickData.value = 0
    }

    /** 隐藏点击 */
    val onHiddenClick: () -> Unit = {
        hiddenClickData.value = 0
    }
}