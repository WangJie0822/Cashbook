package cn.wj.android.cashbook.ui.asset.dialog

import android.view.Gravity
import android.view.WindowManager
import androidx.fragment.app.FragmentManager
import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.base.ext.base.tag
import cn.wj.android.cashbook.base.ui.BaseDialog
import cn.wj.android.cashbook.databinding.DialogAssetLongClickMenuBinding
import cn.wj.android.cashbook.ui.asset.viewmodel.AssetLongClickMenuViewModel
import com.gyf.immersionbar.ktx.immersionBar
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * 资产长按菜单弹窗
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/7
 */
class AssetLongClickMenuDialog : BaseDialog<AssetLongClickMenuViewModel, DialogAssetLongClickMenuBinding>() {

    override val themeId: Int = R.style.Theme_Cashbook_Dialog_BottomSheet

    override val layoutResId: Int = R.layout.dialog_asset_long_click_menu

    override val viewModel: AssetLongClickMenuViewModel by viewModel()

    override val gravity: Int = Gravity.BOTTOM

    override val dialogWidth: Int = WindowManager.LayoutParams.MATCH_PARENT

    override val dialogHeight: Int = WindowManager.LayoutParams.WRAP_CONTENT

    private var onEditClickListener: (() -> Unit)? = null
    private var onSortClickListener: (() -> Unit)? = null
    private var onHiddenClickListener: (() -> Unit)? = null

    override fun initView() {
        // 更新状态栏相关状态
        immersionBar {
            getTag(requireActivity().tag)
            fitsSystemWindows(true)
        }
    }

    override fun observe() {
        // 点击事件
        viewModel.editClickEvent.observe(this) {
            onEditClickListener?.invoke()
            dismiss()
        }
        viewModel.sortClickEvent.observe(this) {
            onSortClickListener?.invoke()
            dismiss()
        }
        viewModel.hiddenClickEvent.observe(this) {
            onHiddenClickListener?.invoke()
            dismiss()
        }
    }

    companion object {
        fun actionShow(manager: FragmentManager, onEditClick: () -> Unit, onSortClick: () -> Unit, onHiddenClick: () -> Unit) {
            AssetLongClickMenuDialog().run {
                onEditClickListener = onEditClick
                onSortClickListener = onSortClick
                onHiddenClickListener = onHiddenClick
                show(manager)
            }
        }
    }
}