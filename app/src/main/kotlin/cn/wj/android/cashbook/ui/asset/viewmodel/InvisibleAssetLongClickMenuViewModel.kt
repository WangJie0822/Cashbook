package cn.wj.android.cashbook.ui.asset.viewmodel

import androidx.lifecycle.MutableLiveData
import cn.wj.android.cashbook.base.ui.BaseViewModel

/**
 * 隐藏资产长按菜单 ViewModel
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/7
 */
class InvisibleAssetLongClickMenuViewModel : BaseViewModel() {

    /** 隐藏点击数据 */
    val cancelHiddenClickData: MutableLiveData<Int> = MutableLiveData()

    /** 隐藏点击 */
    val onCancelHiddenClick: () -> Unit = {
        cancelHiddenClickData.value = 0
    }
}