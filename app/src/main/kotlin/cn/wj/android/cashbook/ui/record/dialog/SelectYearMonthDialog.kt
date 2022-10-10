package cn.wj.android.cashbook.ui.record.dialog

import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentManager
import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.base.ext.base.runIfNotNullAndBlank
import cn.wj.android.cashbook.base.ui.BaseDialog
import cn.wj.android.cashbook.databinding.DialogSelectYearMonthBinding
import cn.wj.android.cashbook.ui.record.viewmodel.SelectYearMonthViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * 年月选择弹窗
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/1
 */
class SelectYearMonthDialog : BaseDialog<SelectYearMonthViewModel, DialogSelectYearMonthBinding>() {

    override val themeId: Int = R.style.Theme_Cashbook_Dialog_Alert

    override val layoutResId: Int = R.layout.dialog_select_year_month

    override val viewModel: SelectYearMonthViewModel by viewModel()

    private var onDateClickCallback: ((String) -> Unit)? = null

    override fun initView() {
        arguments?.run {
            getString(SELECTED_DATE, "").runIfNotNullAndBlank {
                val splits = split("-")
                if (splits.size == 2) {
                    viewModel.tempYear = splits.first().toInt()
                    viewModel.selectedMonth.value = splits.last().toInt()
                }
            }
        }
    }

    override fun doObserve() {
        // 月份点击
        viewModel.monthSelectedEvent.observe(this) { date ->
            onDateClickCallback?.invoke(date)
            dismiss()
        }
    }

    companion object {

        private const val SELECTED_DATE = "selected_date"

        fun actionShow(manager: FragmentManager, date: String, callback: (String) -> Unit) {
            SelectYearMonthDialog().apply {
                arguments = bundleOf(
                    SELECTED_DATE to date
                )
                onDateClickCallback = callback
            }.show(manager)
        }
    }
}