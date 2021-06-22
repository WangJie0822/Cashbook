package cn.wj.android.cashbook.ui.asset.dialog

import android.view.Gravity
import android.view.WindowManager
import androidx.fragment.app.FragmentManager
import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.base.ext.base.tag
import cn.wj.android.cashbook.base.ui.BaseDialog
import cn.wj.android.cashbook.databinding.DialogInvisibleAssetLongClickMenuBinding
import cn.wj.android.cashbook.ui.asset.viewmodel.InvisibleAssetLongClickMenuViewModel
import com.gyf.immersionbar.ktx.immersionBar
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * 隐藏资产长按菜单弹窗
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/7
 */
class InvisibleAssetLongClickMenuDialog : BaseDialog<InvisibleAssetLongClickMenuViewModel, DialogInvisibleAssetLongClickMenuBinding>() {

    override val themeId: Int = R.style.Theme_Cashbook_Dialog_BottomSheet

    override val layoutResId: Int = R.layout.dialog_invisible_asset_long_click_menu

    override val viewModel: InvisibleAssetLongClickMenuViewModel by viewModel()

    override val gravity: Int = Gravity.BOTTOM

    override val dialogWidth: Int = WindowManager.LayoutParams.MATCH_PARENT

    override val dialogHeight: Int = WindowManager.LayoutParams.WRAP_CONTENT

    private var onCancelHiddenClickListener: (() -> Unit)? = null

    override fun initView() {
        // 更新状态栏相关状态
        immersionBar {
            getTag(requireActivity().tag)
            fitsSystemWindows(true)
        }
    }

    override fun observe() {
        // 点击事件
        viewModel.cancelHiddenClickEvent.observe(this, {
            onCancelHiddenClickListener?.invoke()
            dismiss()
        })
    }

    companion object {
        fun actionShow(manager: FragmentManager, onCancelHiddenClick: () -> Unit) {
            InvisibleAssetLongClickMenuDialog().run {
                onCancelHiddenClickListener = onCancelHiddenClick
                show(manager)
            }
        }
    }
}