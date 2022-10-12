@file:Suppress("unused")

package cn.wj.android.cashbook.ui.main.dialog

import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentManager
import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.base.ui.BaseDialog
import cn.wj.android.cashbook.data.constants.ACTION_CONTENT
import cn.wj.android.cashbook.databinding.DialogVerifyPasswordBinding
import cn.wj.android.cashbook.ui.main.viewmodel.VerifyPasswordViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * 验证密码弹窗
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2022/10/10
 */
class VerifyPasswordDialog
    : BaseDialog<VerifyPasswordViewModel, DialogVerifyPasswordBinding>() {

    override val themeId: Int = R.style.Theme_Cashbook_Dialog_Alert

    override val viewModel: VerifyPasswordViewModel by viewModel()

    override val layoutResId = R.layout.dialog_verify_password

    /** 验证成功回调 */
    private var onSuccess: (() -> Unit)? = null

    /** 取消回调 */
    private var onCancel: (() -> Unit)? = null

    override fun initView() {
        viewModel.hintStr.value = requireArguments().getString(ACTION_CONTENT)

        isCancelable = false
        dialog?.setCancelable(false)
    }

    override fun doObserve() {
        viewModel.verifySuccessEvent.observe(this) {
            onSuccess?.invoke()
        }
    }

    companion object {
        fun actionShow(
            fm: FragmentManager,
            hint: String,
            onSuccess: () -> Unit,
            onCancel: (() -> Unit)? = null
        ) {
            VerifyPasswordDialog().run {
                arguments = bundleOf(
                    ACTION_CONTENT to hint
                )
                this.onSuccess = onSuccess
                this.onCancel = onCancel
                show(fm)
            }
        }
    }
}
