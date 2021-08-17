package cn.wj.android.cashbook.ui.general.dialog

import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.base.ui.BaseDialog
import cn.wj.android.cashbook.databinding.DialogProgressBinding
import cn.wj.android.cashbook.ui.general.viewmodel.ProgressViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * 进度弹窗
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/8/17
 */
class ProgressDialog : BaseDialog<ProgressViewModel, DialogProgressBinding>(), LifecycleEventObserver {

    override val themeId: Int = R.style.Theme_Cashbook_Dialog_Transparent

    override val viewModel: ProgressViewModel by viewModel()

    override val layoutResId = R.layout.dialog_progress

    override fun initView() {
        // 从 argument 中获取数据
        requireArguments().let { args ->
            // 能否取消
            isCancelable = args.getBoolean(ACTION_EVENT_CANCELABLE, true)
            // 提示文本
            val hint = args.getString(ACTION_EVENT_HINT, "").orEmpty()
            if (hint.isNotBlank()) {
                viewModel.hintStr.value = hint
            }
        }

    }

    override fun observe() {
        // 空白处点击
        viewModel.blankClickEvent.observe(this, {
            if (isCancelable) {
                dismiss()
            }
        })
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        if (event == Lifecycle.Event.ON_DESTROY) {
            // 界面退出自动隐藏销毁弹窗
            dismiss()
            source.lifecycle.removeObserver(this)
        }
    }

    companion object {

        /** 能否取消 */
        private const val ACTION_EVENT_CANCELABLE = "action_event_cancelable"

        /** 提示文本 */
        private const val ACTION_EVENT_HINT = "action_event_hint"

        /**
         * 使用 [activity] 创建显示并返回 [ProgressDialog]，传递参数能否取消[cancelable]&提示文本[hint]
         * > [cancelable] 默认 `true` [hint] 默认 `""`
         */
        fun actionShow(activity: FragmentActivity, cancelable: Boolean = true, hint: String = ""): ProgressDialog {
            return ProgressDialog().apply {
                arguments = bundleOf(
                    ACTION_EVENT_CANCELABLE to cancelable,
                    ACTION_EVENT_HINT to hint
                )
                activity.lifecycle.addObserver(this)
                show(activity.supportFragmentManager)
            }
        }
    }
}