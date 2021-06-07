@file:Suppress("unused")

package cn.wj.android.cashbook.ui.asset.dialog

import android.view.WindowManager
import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.base.ext.base.tag
import cn.wj.android.cashbook.base.ui.BaseDialog
import cn.wj.android.cashbook.databinding.DialogAssetMoreMenuBinding
import cn.wj.android.cashbook.ui.asset.viewmodel.AssetMoreMenuViewModel
import com.gyf.immersionbar.ktx.immersionBar
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * 资产更多菜单弹窗
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/7
 */
class AssetMoreMenuDialog : BaseDialog<AssetMoreMenuViewModel, DialogAssetMoreMenuBinding>() {

    override val themeId: Int = R.style.Theme_Cashbook_Dialog_Transparent

    override val viewModel: AssetMoreMenuViewModel by viewModel()

    override val layoutResId = R.layout.dialog_asset_more_menu

    override val dialogWidth: Int = WindowManager.LayoutParams.MATCH_PARENT

    override val dialogHeight: Int = WindowManager.LayoutParams.MATCH_PARENT

    override fun initView() {
        // 更新状态栏相关状态
        immersionBar {
            getTag(requireActivity().tag)
            fitsSystemWindows(true)
        }
    }

    override fun observe() {
        // 显示选择资产类型弹窗
        viewModel.showSelectAssetClassificationData.observe(this, {
            SelectAssetClassificationDialog().show(requireActivity().supportFragmentManager)
            dismiss()
        })
    }
}
