package cn.wj.android.cashbook.biometric

import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.base.ui.BaseDialog
import cn.wj.android.cashbook.databinding.DialogBiometricBinding
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * 生物识别弹窗
 *
 * - 创建时间：2021/1/11
 *
 * @author 王杰
 */
class BiometricDialog
    : BaseDialog<BiometricViewModel, DialogBiometricBinding>() {

    override val themeId: Int = R.style.Theme_Cashbook_Dialog_Alert

    override val viewModel: BiometricViewModel by viewModel()

    override val layoutResId: Int = R.layout.dialog_biometric

    /** 取消回调 */
    private var onCancel: (() -> Unit)? = null

    override fun beforeOnCreate() {
        requireArguments().let { args ->
            viewModel.title.set(args.getString(TITLE, "").orEmpty())
            viewModel.subTitle.set(args.getString(SUB_TITLE, "").orEmpty())
            viewModel.hint.set(args.getString(HINT, "").orEmpty())
            viewModel.negative.set(args.getString(NEGATIVE, "").orEmpty())
        }
    }

    override fun initView() {
    }

    override fun doObserve() {
        // 取消
        viewModel.cancelData.observe(this) {
            onCancel?.invoke()
        }
    }

    fun setHint(hint: String) {
        viewModel.hint.set(hint)
    }

    fun show(activity: AppCompatActivity) {
        show(activity.supportFragmentManager, "BiometricDialog")
    }

    companion object {

        private const val TITLE = "title"
        private const val SUB_TITLE = "sub_title"
        private const val HINT = "hint"
        private const val NEGATIVE = "negative"

        /**
         * 创建并返回 [BiometricDialog] 弹窗
         */
        fun actionCreate(
            title: String,
            subTitle: String,
            hint: String,
            negative: String,
            onCancel: () -> Unit
        ): BiometricDialog {
            return BiometricDialog().apply {
                arguments = bundleOf(
                    TITLE to title,
                    SUB_TITLE to subTitle,
                    HINT to hint,
                    NEGATIVE to negative
                )
                this.onCancel = onCancel
            }
        }
    }
}