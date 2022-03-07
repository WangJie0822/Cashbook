package cn.wj.android.cashbook.ui.asset.dialog

import android.view.Gravity
import android.view.WindowManager
import androidx.recyclerview.widget.ConcatAdapter
import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.base.ext.base.tag
import cn.wj.android.cashbook.base.ui.BaseDialog
import cn.wj.android.cashbook.data.enums.AssetClassificationEnum
import cn.wj.android.cashbook.data.enums.ClassificationTypeEnum
import cn.wj.android.cashbook.databinding.DialogSelectAssetClassificationBinding
import cn.wj.android.cashbook.ui.asset.activity.EditAssetActivity
import cn.wj.android.cashbook.ui.asset.adapter.AssetClassificationGroupRvAdapter
import cn.wj.android.cashbook.ui.asset.viewmodel.SelectAssetClassificationViewModel
import cn.wj.android.cashbook.widget.recyclerview.adapter.simple.SimpleRvListAdapter
import cn.wj.android.cashbook.widget.recyclerview.layoutmanager.WrapContentLinearLayoutManager
import com.gyf.immersionbar.ktx.immersionBar
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * 选择资产账户分类弹窗
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/1
 */
class SelectAssetClassificationDialog : BaseDialog<SelectAssetClassificationViewModel, DialogSelectAssetClassificationBinding>() {

    override val themeId: Int = R.style.Theme_Cashbook_Dialog_BottomSheet

    override val layoutResId: Int = R.layout.dialog_select_asset_classification

    override val viewModel: SelectAssetClassificationViewModel by viewModel()

    override val gravity: Int = Gravity.BOTTOM

    override val dialogWidth: Int = WindowManager.LayoutParams.MATCH_PARENT

    override val dialogHeight: Int = WindowManager.LayoutParams.MATCH_PARENT

    private var onClassificationSelectListener: ((ClassificationTypeEnum, AssetClassificationEnum) -> Unit)? = null

    override fun initView() {
        // 更新状态栏相关状态
        immersionBar {
            getTag(requireActivity().tag)
            fitsSystemWindows(true)
        }

        // 配置 RecyclerView
        binding.rvAssetClassification.layoutManager = WrapContentLinearLayoutManager()
        binding.rvBanks.layoutManager = WrapContentLinearLayoutManager()
    }

    override fun observe() {
        // 分类列表数据
        viewModel.assetClassificationListData.observe(this) { list ->
            binding.rvAssetClassification.adapter = ConcatAdapter().apply {
                list.forEach {
                    addAdapter(AssetClassificationGroupRvAdapter(it.classificationType.titleResId))
                    addAdapter(SimpleRvListAdapter<AssetClassificationEnum>(R.layout.recycler_item_asset_classification).apply {
                        viewModel = this@SelectAssetClassificationDialog.viewModel
                        submitList(it.classifications)
                    })
                }
            }
        }
        // 银行列表
        viewModel.bankListData.observe(this) { list ->
            binding.rvBanks.adapter = SimpleRvListAdapter<AssetClassificationEnum>(R.layout.recycler_item_asset_classification).apply {
                viewModel = this@SelectAssetClassificationDialog.viewModel
                submitList(list)
            }
        }
        // 选中的数据
        viewModel.selectedAssetClassificationData.observe(this) { selected ->
            onClassificationSelectListener?.invoke(viewModel.classificationType, selected)
            if (!selected.needSelectBank) {
                // 不需要选择银行
                if (null == onClassificationSelectListener) {
                    // 没有回调，跳转新增资产
                    EditAssetActivity.actionStart(requireActivity(), viewModel.classificationType, selected)
                }
                // 隐藏弹窗
                dismiss()
            }
        }
    }

    /** 设置选中类型回调 [listener] */
    fun setOnClassificationSelectListener(listener: (ClassificationTypeEnum, AssetClassificationEnum) -> Unit): SelectAssetClassificationDialog {
        this.onClassificationSelectListener = listener
        return this
    }
}