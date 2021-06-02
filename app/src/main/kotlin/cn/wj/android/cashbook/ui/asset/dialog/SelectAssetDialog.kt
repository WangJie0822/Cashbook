package cn.wj.android.cashbook.ui.asset.dialog

import android.view.Gravity
import android.view.WindowManager
import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.base.ext.base.tag
import cn.wj.android.cashbook.base.ui.BaseDialog
import cn.wj.android.cashbook.databinding.DialogSelectAssetBinding
import cn.wj.android.cashbook.ui.asset.dialog.SelectAssetClassificationDialog
import cn.wj.android.cashbook.ui.asset.viewmodel.SelectAssetViewModel
import com.gyf.immersionbar.ktx.immersionBar
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * 选择资产账户弹窗
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/1
 */
class SelectAssetDialog : BaseDialog<SelectAssetViewModel, DialogSelectAssetBinding>() {

    override val themeId: Int = R.style.Theme_Cashbook_Dialog_BottomSheet

    override val layoutResId: Int = R.layout.dialog_select_asset

    override val viewModel: SelectAssetViewModel by viewModel()

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

    override fun observe() {
        // 显示选择资产类型弹窗
        viewModel.showSelectAssetTypeData.observe(this, {
            dismiss()
            SelectAssetClassificationDialog().show(requireActivity().supportFragmentManager)
        })
    }
}