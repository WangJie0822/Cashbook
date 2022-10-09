@file:Suppress("unused")

package cn.wj.android.cashbook.ui.asset.dialog

import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentManager
import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.base.ui.BaseDialog
import cn.wj.android.cashbook.data.constants.ACTION_SELECTED
import cn.wj.android.cashbook.data.entity.AssetEntity
import cn.wj.android.cashbook.databinding.DialogAssetMoreInfoBinding
import cn.wj.android.cashbook.ui.asset.viewmodel.AssetMoreInfoViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * 资产更多信息弹窗
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2022/10/9
 */
class AssetMoreInfoDialog
    : BaseDialog<AssetMoreInfoViewModel, DialogAssetMoreInfoBinding>() {

    override val themeId: Int = R.style.Theme_Cashbook_Dialog_Alert

    override val viewModel: AssetMoreInfoViewModel by viewModel()

    override val layoutResId = R.layout.dialog_asset_more_info

    override fun beforeOnCreate() {
        viewModel.assetData.value = requireArguments().getParcelable(ACTION_SELECTED)
    }

    override fun initView() {
    }

    companion object {
        fun actionShow(fm: FragmentManager, asset: AssetEntity) {
            AssetMoreInfoDialog().run {
                arguments = bundleOf(
                    ACTION_SELECTED to asset
                )
                show(fm)
            }
        }
    }
}
