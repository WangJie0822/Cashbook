package cn.wj.android.cashbook.ui.asset.dialog

import android.view.Gravity
import android.view.WindowManager
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.ConcatAdapter
import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.base.ext.base.tag
import cn.wj.android.cashbook.base.ui.BaseDialog
import cn.wj.android.cashbook.data.entity.AssetEntity
import cn.wj.android.cashbook.databinding.DialogSelectAssetBinding
import cn.wj.android.cashbook.ui.asset.viewmodel.SelectAssetViewModel
import cn.wj.android.cashbook.ui.general.adapter.OneItemAdapter
import cn.wj.android.cashbook.widget.recyclerview.adapter.simple.SimpleRvListAdapter
import cn.wj.android.cashbook.widget.recyclerview.layoutmanager.WrapContentLinearLayoutManager
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

    /** 选中回调 */
    private var onSelectedListener: ((AssetEntity?) -> Unit)? = null

    /** 资产列表适配器 */
    private val assetAdapter: SimpleRvListAdapter<AssetEntity> by lazy {
        SimpleRvListAdapter<AssetEntity>(R.layout.recycler_item_asset_list).apply {
            this.viewModel = this@SelectAssetDialog.viewModel
        }
    }

    override fun initView() {
        // 更新状态栏相关状态
        immersionBar {
            getTag(requireActivity().tag)
            fitsSystemWindows(true)
        }

        // 配置 RecyclerView
        binding.rvAsset.run {
            layoutManager = WrapContentLinearLayoutManager()
            adapter = ConcatAdapter(
                OneItemAdapter(R.layout.recycler_header_asset_no_select) {
                    viewModel.selectedAssetEvent.value = null
                },
                assetAdapter
            )
        }
    }

    override fun observe() {
        // 显示选择资产类型弹窗
        viewModel.showSelectAssetTypeEvent.observe(this, {
            SelectAssetClassificationDialog().show(requireActivity().supportFragmentManager)
            dismiss()
        })
        // 资产列表
        viewModel.assetListData.observe(this, { list ->
            assetAdapter.submitList(list)
        })
        // 选中资产
        viewModel.selectedAssetEvent.observe(this, { selected ->
            onSelectedListener?.invoke(selected)
            dismiss()
        })
    }

    companion object {
        fun actionShow(manager: FragmentManager, onSelected: (AssetEntity?) -> Unit) {
            SelectAssetDialog().run {
                onSelectedListener = onSelected
                show(manager)
            }
        }
    }
}