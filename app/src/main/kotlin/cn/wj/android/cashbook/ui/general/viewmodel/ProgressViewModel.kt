package cn.wj.android.cashbook.ui.general.viewmodel

import androidx.lifecycle.MutableLiveData
import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.base.ext.base.string
import cn.wj.android.cashbook.base.ui.BaseViewModel
import cn.wj.android.cashbook.data.event.LifecycleEvent

/**
 * 进度弹窗 ViewModel
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/8/17
 */
class ProgressViewModel : BaseViewModel() {

    /** 空白点击数据 */
    val blankClickEvent: LifecycleEvent<Int> = LifecycleEvent()

    /** 提示文本 */
    val hintStr: MutableLiveData<String> = MutableLiveData(R.string.in_request.string)

    /** 空白点击事件 */
    val onBlankClick: () -> Unit = {
        blankClickEvent.value = 0
    }
}