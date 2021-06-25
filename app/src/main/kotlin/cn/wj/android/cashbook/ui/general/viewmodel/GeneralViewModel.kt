package cn.wj.android.cashbook.ui.general.viewmodel

import android.text.SpannableString
import android.view.Gravity
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import androidx.lifecycle.MutableLiveData
import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.base.ext.base.string
import cn.wj.android.cashbook.base.ui.BaseViewModel
import cn.wj.android.cashbook.data.event.LifecycleEvent

/**
 * 通用弹窗 ViewModel
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/5/26
 */
class GeneralViewModel : BaseViewModel() {

    /** 尝试隐藏弹窗事件 */
    val tryDismissEvent = LifecycleEvent<Int>()

    /** 消极按钮点击事件  */
    val negativeClickEvent = LifecycleEvent<Int>()

    /** 积极按钮点击事件  */
    val positiveClickEvent = LifecycleEvent<Int>()

    /** 标题文本  */
    val titleStr: ObservableField<CharSequence> = ObservableField("")

    /** 标题富文本处理 */
    val titleSpan: MutableLiveData<((SpannableString) -> Unit)?> = MutableLiveData(null)

    /** 标记 - 是否显示标题  */
    val showTitle: ObservableBoolean = object : ObservableBoolean(titleStr) {
        override fun get(): Boolean {
            return !titleStr.get().isNullOrBlank()
        }
    }

    /** 副标题文本 */
    val subtitleStr: ObservableField<CharSequence> = ObservableField("")

    /** 副标题富文本处理 */
    val subtitleSpan: MutableLiveData<((SpannableString) -> Unit)?> = MutableLiveData(null)

    /** 标记 - 是否显示副标题  */
    val showSubtitle: ObservableBoolean = object : ObservableBoolean(subtitleStr) {
        override fun get(): Boolean {
            return !subtitleStr.get().isNullOrBlank()
        }
    }

    /** 内容文本  */
    val contentStr: ObservableField<CharSequence> = ObservableField("")

    /** 内容富文本处理 */
    val contentSpan: MutableLiveData<((SpannableString) -> Unit)?> = MutableLiveData(null)

    /** 内容文本重心  */
    val contentGravity: ObservableInt = ObservableInt(Gravity.START or Gravity.CENTER_VERTICAL)

    /** 标记 - 是否显示选择器  */
    val showSelect: ObservableBoolean = ObservableBoolean(false)

    /** 标记 - 选择器是否选中  */
    val checked: ObservableBoolean = ObservableBoolean(false)

    /** 选择器文本 - 默认：不再提示  */
    val selectStr: ObservableField<CharSequence> = ObservableField(R.string.no_longer_prompt.string)

    /** 选择器富文本处理 */
    val selectSpan: MutableLiveData<((SpannableString) -> Unit)?> = MutableLiveData(null)

    /** 标记 - 是否显示消极按钮  */
    val showNegativeButton: ObservableBoolean = ObservableBoolean(true)

    /** 消极按钮文本 - 默认：取消  */
    val negativeButtonStr: ObservableField<String> = ObservableField(R.string.cancel.string)

    /** 标记 - 是否显示积极按钮  */
    val showPositiveButton: ObservableBoolean = ObservableBoolean(true)

    /** 积极按钮文本 - 默认：确认  */
    val positiveButtonStr: ObservableField<String> = ObservableField(R.string.confirm.string)

    /** 背景点击 */
    val onBackgroundClick: () -> Unit = {
        tryDismissEvent.value = 0
    }

    /** 消极按钮点击  */
    val onNegativeClick: () -> Unit = {
        negativeClickEvent.value = 0
    }

    /** 积极按钮点击  */
    val onPositiveClick: () -> Unit = {
        positiveClickEvent.value = 0
    }
}
