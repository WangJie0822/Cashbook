@file:Suppress("unused")

package cn.wj.android.cashbook.ui.main.dialog

import androidx.fragment.app.FragmentManager
import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.base.ui.BaseDialog
import cn.wj.android.cashbook.databinding.DialogClearPasswordBinding
import cn.wj.android.cashbook.ui.main.viewmodel.ClearPasswordViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * 清除密码弹窗
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2022/10/10
 */
class ClearPasswordDialog
    : BaseDialog<ClearPasswordViewModel, DialogClearPasswordBinding>() {

    override val themeId: Int = R.style.Theme_Cashbook_Dialog_Alert

    override val viewModel: ClearPasswordViewModel by viewModel()

    override val layoutResId = R.layout.dialog_clear_password

    override fun initView() {
    }

    companion object {
        fun actionShow(fm: FragmentManager) {
            ClearPasswordDialog().show(fm)
        }
    }
}
