package cn.wj.android.cashbook.ui.dialog

import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentManager
import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.base.ext.base.runIfNotNullAndBlank
import cn.wj.android.cashbook.base.ui.BaseDialog
import cn.wj.android.cashbook.databinding.DialogDateTimePickerBinding
import cn.wj.android.cashbook.ui.viewmodel.DateTimePickerViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * 日期时间选择弹窗
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/1
 */
class DateTimePickerDialog : BaseDialog<DateTimePickerViewModel, DialogDateTimePickerBinding>() {

    override val themeId: Int = R.style.Theme_Cashbook_Dialog_Alert

    override val layoutResId: Int = R.layout.dialog_date_time_picker

    override val viewModel: DateTimePickerViewModel by viewModel()

    private var onConfirmClickListener: ((String) -> Unit)? = null

    override fun initView() {
        arguments?.run {
            getString(SELECTED_DATE, "").runIfNotNullAndBlank {
                val splits = split(" ")
                if (splits.size == 2) {
                    viewModel.date.value = splits[0]
                    viewModel.time.value = splits[1]
                }
            }
        }
    }

    override fun observe() {
        // 确认点击
        viewModel.confirmDateData.observe(this, { date ->
            onConfirmClickListener?.invoke(date)
            dismiss()
        })
    }

    fun show(manager: FragmentManager) {
        show(manager, tag)
    }

    class Builder {

        private var date = ""
        private var listener: ((String) -> Unit)? = null

        fun setDate(date: String): Builder {
            this.date = date
            return this
        }

        fun setOnDatePickerListener(listener: (String) -> Unit): Builder {
            this.listener = listener
            return this
        }

        fun build(): DateTimePickerDialog {
            return DateTimePickerDialog().apply {
                arguments = bundleOf(
                    SELECTED_DATE to date
                )
                onConfirmClickListener = listener
            }
        }

        fun show(manager: FragmentManager) {
            build().show(manager)
        }
    }

    companion object {
        private const val SELECTED_DATE = "selected_date"
    }
}