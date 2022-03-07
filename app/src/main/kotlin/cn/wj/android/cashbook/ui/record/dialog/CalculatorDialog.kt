package cn.wj.android.cashbook.ui.record.dialog

import android.view.Gravity
import android.view.WindowManager
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.base.ext.base.tag
import cn.wj.android.cashbook.base.ui.BaseDialog
import cn.wj.android.cashbook.databinding.DialogCalculatorBinding
import cn.wj.android.cashbook.ui.record.viewmodel.CalculatorViewModel
import cn.wj.android.cashbook.ui.record.viewmodel.EditRecordViewModel
import com.gyf.immersionbar.ktx.immersionBar
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * 计算器弹窗
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/9
 */
class CalculatorDialog : BaseDialog<CalculatorViewModel, DialogCalculatorBinding>() {

    override val themeId: Int = R.style.Theme_Cashbook_Dialog_BottomSheet

    override val layoutResId: Int = R.layout.dialog_calculator

    override val viewModel: CalculatorViewModel by viewModel()

    override val gravity: Int = Gravity.BOTTOM

    override val dialogWidth: Int = WindowManager.LayoutParams.MATCH_PARENT

    override val dialogHeight: Int = WindowManager.LayoutParams.WRAP_CONTENT

    private val parentViewModel: EditRecordViewModel by activityViewModels()

    override fun initView() {
        // 更新状态栏相关状态
        immersionBar {
            getTag(requireActivity().tag)
            fitsSystemWindows(true)
        }

        // 绑定 Activity 的 ViewModel
        binding.parentViewModel = parentViewModel

        // 更新金额
        viewModel.calculatorStr.set(parentViewModel.calculatorStr.get())
    }

    override fun observe() {
        // 确认点击
        viewModel.confirmClickEvent.observe(this) {
            // 更新金额
            parentViewModel.calculatorStr.set(viewModel.calculatorStr.get())
            dismiss()
        }
    }

    companion object {
        fun actionShow(manager: FragmentManager) {
            CalculatorDialog().show(manager)
        }
    }
}