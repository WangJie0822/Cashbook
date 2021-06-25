@file:Suppress("unused")

package cn.wj.android.cashbook.ui.general.dialog

import android.text.SpannableString
import android.view.Gravity
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Observer
import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.base.ext.base.string
import cn.wj.android.cashbook.base.ui.BaseDialog
import cn.wj.android.cashbook.base.ui.OnDialogDismissListener
import cn.wj.android.cashbook.databinding.DialogGeneralBinding
import cn.wj.android.cashbook.ui.general.viewmodel.GeneralViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * 通用弹窗
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/5/26
 */
class GeneralDialog
    : BaseDialog<GeneralViewModel, DialogGeneralBinding>() {

    override val themeId: Int = R.style.Theme_Cashbook_Dialog_Alert

    /** 消极按钮点击  */
    private var onNegativeClick: OnDialogClickListener? = null

    /** 积极按钮点击  */
    private var onPositiveClick: OnDialogClickListener? = null

    /** 消极按钮点击  */
    private var onNegativeAction: OnDialogActionListener? = null

    /** 积极按钮点击  */
    private var onPositiveAction: OnDialogActionListener? = null

    /** 标题富文本处理 */
    private var titleSpan: ((SpannableString) -> Unit)? = null

    /** 副标题富文本处理 */
    private var subtitleSpan: ((SpannableString) -> Unit)? = null

    /** 内容富文本处理 */
    private var contentSpan: ((SpannableString) -> Unit)? = null

    /** 选择器富文本处理 */
    private var selectSpan: ((SpannableString) -> Unit)? = null

    /** Builder 对象  */
    var builder: Builder? = null

    override val viewModel: GeneralViewModel by viewModel()

    override val layoutResId = R.layout.dialog_general

    override fun initView() {
        // 从 argument 中获取数据
        val arguments = arguments ?: return
        // 设置 UI 显示
        viewModel.showTitle.set(arguments.getBoolean(ACTION_SHOW_TITLE, false))
        viewModel.titleStr.set(arguments.getCharSequence(ACTION_TITLE_STR, ""))
        viewModel.showSubtitle.set(arguments.getBoolean(ACTION_SHOW_SUBTITLE, false))
        viewModel.subtitleStr.set(arguments.getCharSequence(ACTION_SUBTITLE_STR, ""))
        viewModel.contentStr.set(arguments.getCharSequence(ACTION_CONTENT_STR, ""))
        viewModel.contentGravity.set(arguments.getInt(ACTION_CONTENT_GRAVITY, 0))
        viewModel.showSelect.set(arguments.getBoolean(ACTION_SHOW_SELECT, false))
        viewModel.checked.set(arguments.getBoolean(ACTION_CHECKED, false))
        viewModel.selectStr.set(arguments.getCharSequence(ACTION_SELECT_STR, ""))
        viewModel.showNegativeButton.set(arguments.getBoolean(ACTION_SHOW_NEGATIVE_BUTTON, true))
        viewModel.negativeButtonStr.set(arguments.getString(ACTION_NEGATIVE_BUTTON_STR, ""))
        viewModel.showPositiveButton.set(arguments.getBoolean(ACTION_SHOW_POSITIVE_BUTTON, true))
        viewModel.positiveButtonStr.set(arguments.getString(ACTION_POSITIVE_BUTTON_STR))
        viewModel.titleSpan.value = titleSpan
        viewModel.subtitleSpan.value = subtitleSpan
        viewModel.contentSpan.value = contentSpan
        viewModel.selectSpan.value = selectSpan
    }

    override fun observe() {
        // 背景点击
        viewModel.tryDismissEvent.observe(this, {
            if (isCancelable) {
                dismiss()
            }
        })
        // 消极按钮点击
        viewModel.negativeClickEvent.observe(this, Observer {
            // 选中状态
            val checked = viewModel.checked.get()

            if (null != onNegativeAction) {
                // 优先处理 Action
                onNegativeAction?.invoke(checked)
                // 隐藏弹窗
                dismiss()
                return@Observer
            }

            if (null != onNegativeClick) {
                // 处理 Click
                onNegativeClick?.invoke(this, checked)
                return@Observer
            }

            // 未设置监听，默认隐藏
            dismiss()
        })

        // 积极按钮点击
        viewModel.positiveClickEvent.observe(this, Observer {
            // 选中状态
            val checked = viewModel.checked.get()

            if (null != onPositiveAction) {
                // 优先处理 Action
                onPositiveAction?.invoke(checked)
                // 隐藏弹窗
                dismiss()
                return@Observer
            }

            if (null != onPositiveClick) {
                // 处理 Click
                onPositiveClick?.invoke(this, checked)
                return@Observer
            }

            // 未设置监听，默认隐藏
            dismiss()
        })
    }

    /** 设置消极按钮点击事件 [onNegativeClick] */
    private fun setOnNegativeClick(onNegativeClick: OnDialogClickListener?) {
        this.onNegativeClick = onNegativeClick
    }

    /** 设置积极按钮点击事件 [onPositiveClick] */
    private fun setOnPositiveClick(onPositiveClick: OnDialogClickListener?) {
        this.onPositiveClick = onPositiveClick
    }

    /**
     * 设置消极按钮点击事件 [onNegativeAction]
     * > 按钮点击后自动隐藏弹窗
     */
    private fun setOnNegativeAction(onNegativeAction: OnDialogActionListener?) {
        this.onNegativeAction = onNegativeAction
    }

    /**
     * 设置积极按钮点击事件 [onPositiveAction]
     * > 按钮点击后自动隐藏弹窗
     */
    private fun setOnPositiveAction(onPositiveAction: OnDialogActionListener?) {
        this.onPositiveAction = onPositiveAction
    }

    /**
     * [GeneralDialog] 通用弹窗建造者对象
     * > [builder] 可选参数，可空
     */
    class Builder(builder: Builder?) {

        /** 标记 - 能否取消  */
        private var cancelable = false

        /** 标记 - 是否显示标题  */
        private var showTitle = false

        /** 标记 - 是否显示副标题  */
        private var showSubtitle = false

        /** 标题文本  */
        private var titleStr: CharSequence = ""

        /** 副标题文本  */
        private var subtitleStr: CharSequence = ""

        /** 内容文本  */
        private var contentStr: CharSequence = ""

        /** 内容文本重心  */
        private var contentGravity = Gravity.START or Gravity.CENTER_VERTICAL

        /** 标记 - 是否显示选择器  */
        private var showSelect = false

        /** 标记 - 选择器是否选中  */
        private var selected = false

        /** 选择器文本 - 默认：不再提示  */
        private var selectStr: CharSequence = R.string.no_longer_prompt.string

        /** 标题富文本处理 */
        private var titleSpan: ((SpannableString) -> Unit)? = null

        /** 副标题富文本处理 */
        private var subtitleSpan: ((SpannableString) -> Unit)? = null

        /** 内容富文本处理 */
        private var contentSpan: ((SpannableString) -> Unit)? = null

        /** 选择器富文本处理 */
        private var selectSpan: ((SpannableString) -> Unit)? = null

        /** 标记 - 是否显示消极按钮  */
        private var showNegativeButton = true

        /** 消极按钮文本 - 默认：取消  */
        private var negativeButtonStr = R.string.cancel.string

        /** 标记 - 是否显示积极按钮  */
        private var showPositiveButton = true

        /** 积极按钮文本 - 默认：确认  */
        private var positiveButtonStr = R.string.confirm.string

        /** 消极按钮点击  */
        private var onNegativeClick: OnDialogClickListener? = null

        /** 积极按钮点击  */
        private var onPositiveClick: OnDialogClickListener? = null

        /** 消极按钮点击  */
        private var onNegativeAction: OnDialogActionListener? = null

        /** 积极按钮点击  */
        private var onPositiveAction: OnDialogActionListener? = null

        /** 隐藏回调 */
        private var onDialogDismissListener: OnDialogDismissListener? = null

        init {
            if (null != builder) {
                // 已有 builder 不为空，复制属性
                cancelable = builder.cancelable
                showTitle = builder.showTitle
                titleStr = builder.titleStr
                subtitleStr = builder.subtitleStr
                contentStr = builder.contentStr
                contentGravity = builder.contentGravity
                showSelect = builder.showSelect
                selected = builder.selected
                selectStr = builder.selectStr
                titleSpan = builder.titleSpan
                subtitleSpan = builder.subtitleSpan
                contentSpan = builder.contentSpan
                selectSpan = builder.selectSpan
                showNegativeButton = builder.showNegativeButton
                negativeButtonStr = builder.negativeButtonStr
                showPositiveButton = builder.showPositiveButton
                positiveButtonStr = builder.positiveButtonStr
                onNegativeClick = builder.onNegativeClick
                onPositiveClick = builder.onPositiveClick
                onNegativeAction = builder.onNegativeAction
                onPositiveAction = builder.onPositiveAction
                onDialogDismissListener = builder.onDialogDismissListener
            }
        }

        /** 设置 [GeneralDialog] 能否被取消 [cancelable] 并返回 [Builder] 对象 */
        fun setCancelable(cancelable: Boolean): Builder {
            this.cancelable = cancelable
            return this
        }

        /** 设置 [GeneralDialog] 是否显示标题 [showTitle] 并返回 [Builder] 对象 */
        fun showTitle(showTitle: Boolean): Builder {
            this.showTitle = showTitle
            return this
        }

        /** 设置 [GeneralDialog] 标题文本 [titleStr] 并返回 [Builder] 对象 */
        fun titleStr(titleStr: CharSequence, span: ((SpannableString) -> Unit)? = null): Builder {
            this.titleStr = titleStr
            this.titleSpan = span
            return this
        }

        /** 设置 [GeneralDialog] 副标题文本 [subtitleStr] 并返回 [Builder] 对象 */
        fun subtitleStr(subtitleStr: CharSequence, span: ((SpannableString) -> Unit)? = null): Builder {
            this.subtitleStr = subtitleStr
            this.subtitleSpan = span
            return this
        }

        /** 设置 [GeneralDialog] 是否显示标题 [showTitle] 并返回 [Builder] 对象 */
        fun showSubtitle(showSubtitle: Boolean): Builder {
            this.showSubtitle = showSubtitle
            return this
        }

        /** 设置 [GeneralDialog] 内容文本 并返回 [Builder] 对象 */
        fun contentStr(contentStr: CharSequence, span: ((SpannableString) -> Unit)? = null): Builder {
            this.contentStr = contentStr
            this.contentSpan = span
            return this
        }

        /**
         * 设置 [GeneralDialog] 内容文本重心 [gravity] 并返回 [Builder] 对象
         * > [gravity] 参见 [Gravity]
         */
        fun contentGravity(gravity: Int): Builder {
            this.contentGravity = gravity
            return this
        }

        /** 设置 [GeneralDialog] 是否显示选择器 [showSelect] 并返回 [Builder] 对象 */
        fun showSelect(showSelect: Boolean): Builder {
            this.showSelect = showSelect
            return this
        }

        /** 设置 [GeneralDialog] 选择器默认选择状态 [selected] 并返回 [Builder] 对象 */
        fun defaultSelected(selected: Boolean): Builder {
            this.selected = selected
            return this
        }

        /** 设置 [GeneralDialog] 选择器提示文本 [selectStr] 并返回 [Builder] 对象 */
        fun selectTipsStr(selectStr: CharSequence, span: ((SpannableString) -> Unit)? = null): Builder {
            this.selectStr = selectStr
            this.selectSpan = span
            return this
        }

        /** 设置 [GeneralDialog] 是否显示消极按钮 [showNegativeButton] 并返回 [Builder] 对象 */
        fun showNegativeButton(showNegativeButton: Boolean): Builder {
            this.showNegativeButton = showNegativeButton
            return this
        }

        /** 设置 [GeneralDialog] 消极按钮显示文本 [negativeButtonStr] 并返回 [Builder] 对象 */
        fun negativeButtonStr(negativeButtonStr: String): Builder {
            this.negativeButtonStr = negativeButtonStr
            return this
        }

        /** 设置 [GeneralDialog] 是否显示积极按钮 [showPositiveButton]  并返回 [Builder] 对象 */
        fun showPositiveButton(showPositiveButton: Boolean): Builder {
            this.showPositiveButton = showPositiveButton
            return this
        }

        /** 设置 [GeneralDialog] 积极按钮显示文本 [positiveButtonStr] 并返回 [Builder] 对象 */
        fun positiveButtonStr(positiveButtonStr: String): Builder {
            this.positiveButtonStr = positiveButtonStr
            return this
        }

        /** 设置 [GeneralDialog] 消极按钮点击事件 [onNegativeClick] 并返回 [Builder] 对象 */
        fun setOnNegativeClick(onNegativeClick: OnDialogClickListener): Builder {
            this.onNegativeClick = onNegativeClick
            return this
        }

        /** 设置 [GeneralDialog] 积极按钮点击事件 [onPositiveClick] 并返回 [Builder] 对象 */
        fun setOnPositiveClick(onPositiveClick: OnDialogClickListener): Builder {
            this.onPositiveClick = onPositiveClick
            return this
        }

        /**
         * 设置 [GeneralDialog] 消极按钮点击事件 [onNegativeAction] 并返回 [Builder] 对象
         * > 按钮点击后自动隐藏弹窗
         */
        fun setOnNegativeAction(onNegativeAction: OnDialogActionListener): Builder {
            this.onNegativeAction = onNegativeAction
            return this
        }

        /**
         * 设置 [GeneralDialog] 积极按钮点击事件 [onPositiveAction] 并返回 [Builder] 对象
         * > 按钮点击后自动隐藏弹窗
         */
        fun setOnPositiveAction(onPositiveAction: OnDialogActionListener): Builder {
            this.onPositiveAction = onPositiveAction
            return this
        }

        /**
         * 设置 [GeneralDialog] 消极按钮文本 [negativeButtonStr] 以及消极按钮点击事件 [onNegativeClick]
         * 并返回 [Builder] 对象
         */
        fun setNegativeButton(negativeButtonStr: String, onNegativeClick: OnDialogClickListener): Builder {
            this.negativeButtonStr = negativeButtonStr
            this.onNegativeClick = onNegativeClick
            return this
        }

        /**
         * 设置 [GeneralDialog] 设置积极按钮文本 [positiveButtonStr] 以及积极按钮点击事件 [onPositiveClick]
         * 并返回 [Builder] 对象
         */
        fun setPositiveButton(positiveButtonStr: String, onPositiveClick: OnDialogClickListener): Builder {
            this.positiveButtonStr = positiveButtonStr
            this.onPositiveClick = onPositiveClick
            return this
        }

        /**
         * 设置 [GeneralDialog] 消极按钮文本 [negativeButtonStr] 以及消极按钮点击事件 [onNegativeAction]
         * 并返回 [Builder] 对象
         * > 按钮点击后自动隐藏弹窗
         */
        fun setNegativeAction(negativeButtonStr: String, onNegativeAction: OnDialogActionListener): Builder {
            this.negativeButtonStr = negativeButtonStr
            this.onNegativeAction = onNegativeAction
            return this
        }

        /**
         * 设置 [GeneralDialog] 设置积极按钮文本 [positiveButtonStr] 以及积极按钮点击事件 [onPositiveAction]
         * 并返回 [Builder] 对象
         * > 按钮点击后自动隐藏弹窗
         */
        fun setPositiveAction(positiveButtonStr: String, onPositiveAction: OnDialogActionListener): Builder {
            this.positiveButtonStr = positiveButtonStr
            this.onPositiveAction = onPositiveAction
            return this
        }

        /** 设置 [GeneralDialog] 弹窗隐藏回调 [listener] */
        fun setOnDismissListener(listener: OnDialogDismissListener): Builder {
            this.onDialogDismissListener = listener
            return this
        }

        /** 使用 [fm] 创建、显示并返回 [GeneralDialog] */
        fun show(fm: FragmentManager): GeneralDialog {
            return build().apply {
                show(fm)
            }
        }

        /** 建造并返回 [GeneralDialog] 对象 */
        private fun build(): GeneralDialog {
            return GeneralDialog().apply {
                isCancelable = cancelable
                arguments = bundleOf(
                    ACTION_SHOW_TITLE to showTitle,
                    ACTION_TITLE_STR to titleStr,
                    ACTION_SHOW_SUBTITLE to showSubtitle,
                    ACTION_SUBTITLE_STR to subtitleStr,
                    ACTION_CONTENT_STR to contentStr,
                    ACTION_CONTENT_GRAVITY to contentGravity,
                    ACTION_SHOW_SELECT to showSelect,
                    ACTION_CHECKED to selected,
                    ACTION_SELECT_STR to selectStr,
                    ACTION_SHOW_NEGATIVE_BUTTON to showNegativeButton,
                    ACTION_NEGATIVE_BUTTON_STR to negativeButtonStr,
                    ACTION_SHOW_POSITIVE_BUTTON to showPositiveButton,
                    ACTION_POSITIVE_BUTTON_STR to positiveButtonStr
                )
                this.onNegativeClick = this@Builder.onNegativeClick
                this.onNegativeAction = this@Builder.onNegativeAction
                this.onPositiveClick = this@Builder.onPositiveClick
                this.onPositiveAction = this@Builder.onPositiveAction
                this.titleSpan = this@Builder.titleSpan
                this.subtitleSpan = this@Builder.subtitleSpan
                this.contentSpan = this@Builder.contentSpan
                this.selectSpan = this@Builder.selectSpan
                this.setOnDialogDismissListener(this@Builder.onDialogDismissListener)
                builder = this@Builder
            }
        }
    }

    companion object {

        /** 参数 Key - 是否显示标题  */
        private const val ACTION_SHOW_TITLE = "action_show_title"

        /** 参数 Key - 标题文本  */
        private const val ACTION_TITLE_STR = "action_title_str"

        /** 参数 Key - 是否显示副标题  */
        private const val ACTION_SHOW_SUBTITLE = "action_show_subtitle"

        /** 参数 Key - 副标题文本 */
        private const val ACTION_SUBTITLE_STR = "action_subtitle_str"

        /** 参数 Key - 内容文本  */
        private const val ACTION_CONTENT_STR = "action_content_str"

        /** 参数 Key - 内容文本重心  */
        private const val ACTION_CONTENT_GRAVITY = "action_content_gravity"

        /** 参数 Key - 是否显示选择器  */
        private const val ACTION_SHOW_SELECT = "action_show_select"

        /** 参数 Key - 选择器是否选中  */
        private const val ACTION_CHECKED = "action_checked"

        /** 参数 Key - 选择器文本  */
        private const val ACTION_SELECT_STR = "ACTION_SELECT_STR"

        /** 参数 Key - 是否显示消极按钮  */
        private const val ACTION_SHOW_NEGATIVE_BUTTON = "action_show_negative_button"

        /** 参数 Key - 消极按钮文本  */
        private const val ACTION_NEGATIVE_BUTTON_STR = "action_negative_button_str"

        /** 参数 Key - 是否显示积极按钮  */
        private const val ACTION_SHOW_POSITIVE_BUTTON = "action_show_positive_button"

        /** 参数 Key - 积极按钮文本  */
        private const val ACTION_POSITIVE_BUTTON_STR = "action_positive_button_str"

        /** 创建并返回 [Builder] 建造者对象 */
        fun newBuilder(): Builder {
            return Builder(null)
        }

        /** 在 [builder] 的基础上创建并返回 [Builder] 建造者对象 */
        fun newBuilder(builder: Builder): Builder {
            return Builder(builder)
        }
    }
}

/**
 * Dialog 按钮点击回调接口
 * > 入参为([GeneralDialog], [Boolean])，无返回类型
 */
typealias OnDialogClickListener = (GeneralDialog, Boolean) -> Unit

/**
 * Dialog 按钮点击回调接口
 * > 按钮点击后自动隐藏弹窗
 *
 * > 入参为([Boolean])，无返回类型
 */
typealias OnDialogActionListener = (Boolean) -> Unit
