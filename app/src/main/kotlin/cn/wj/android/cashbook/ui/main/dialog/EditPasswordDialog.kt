@file:Suppress("unused")

package cn.wj.android.cashbook.ui.main.dialog

import androidx.fragment.app.FragmentManager
import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.base.ui.BaseDialog
import cn.wj.android.cashbook.databinding.DialogEditPasswordBinding
import cn.wj.android.cashbook.ui.main.viewmodel.EditPasswordViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * 编辑密码弹窗
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2022/10/10
 */
class EditPasswordDialog
    : BaseDialog<EditPasswordViewModel, DialogEditPasswordBinding>() {

    override val themeId: Int = R.style.Theme_Cashbook_Dialog_Alert

    override val viewModel: EditPasswordViewModel by viewModel()

    override val layoutResId = R.layout.dialog_edit_password

    override fun initView() {
    }

    companion object {
        fun actionShow(fm: FragmentManager) {
            EditPasswordDialog().show(fm)
        }
    }
}
