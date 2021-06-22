package cn.wj.android.cashbook.ui.main.viewmodel

import androidx.lifecycle.MutableLiveData
import cn.wj.android.cashbook.base.ui.BaseViewModel
import cn.wj.android.cashbook.data.model.UiNavigationModel

/**
 * Markdown ViewModel
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/21
 */
class MarkdownViewModel : BaseViewModel() {

    /** 标题文本 */
    val titleStr: MutableLiveData<String> = MutableLiveData()

    /** 内容文本 */
    val contentStr: MutableLiveData<CharSequence> = MutableLiveData()

    /** 返回点击 */
    val onBackClick: () -> Unit = {
        uiNavigationData.value = UiNavigationModel.builder {
            close()
        }
    }
}