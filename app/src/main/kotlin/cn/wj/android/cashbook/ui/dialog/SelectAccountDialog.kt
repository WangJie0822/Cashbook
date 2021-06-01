package cn.wj.android.cashbook.ui.dialog

import android.view.Gravity
import android.view.WindowManager
import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.base.ext.base.tag
import cn.wj.android.cashbook.base.ui.BaseDialog
import cn.wj.android.cashbook.databinding.DialogSelectAccountBinding
import cn.wj.android.cashbook.ui.viewmodel.SelectAccountViewModel
import com.gyf.immersionbar.ktx.immersionBar
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * 选择账户弹窗
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/1
 */
class SelectAccountDialog : BaseDialog<SelectAccountViewModel, DialogSelectAccountBinding>() {

    override val themeId: Int = R.style.Theme_Cashbook_Dialog_BottomSheet

    override val layoutResId: Int = R.layout.dialog_select_account

    override val viewModel: SelectAccountViewModel by viewModel()

    override val gravity: Int = Gravity.BOTTOM

    override val dialogWidth: Int = WindowManager.LayoutParams.MATCH_PARENT

    override val dialogHeight: Int = WindowManager.LayoutParams.MATCH_PARENT

    override fun initView() {
        // 更新状态栏相关状态
        immersionBar {
            getTag(requireActivity().tag)
            fitsSystemWindows(true)
        }
    }
}