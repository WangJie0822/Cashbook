package cn.wj.android.cashbook.ui.asset.activity

import android.os.Bundle
import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.base.ext.base.string
import cn.wj.android.cashbook.base.ui.BaseActivity
import cn.wj.android.cashbook.data.constants.ACTION_ASSET
import cn.wj.android.cashbook.data.constants.EVENT_RECORD_CHANGE
import cn.wj.android.cashbook.data.constants.ROUTE_PATH_ASSET_INFO
import cn.wj.android.cashbook.databinding.ActivityAssetInfoBinding
import cn.wj.android.cashbook.ui.asset.viewmodel.AssetInfoViewModel
import cn.wj.android.cashbook.ui.general.dialog.GeneralDialog
import cn.wj.android.cashbook.ui.record.adapter.DateRecordPagingRvAdapter
import cn.wj.android.cashbook.ui.record.dialog.RecordInfoDialog
import cn.wj.android.cashbook.widget.recyclerview.layoutmanager.WrapContentLinearLayoutManager
import com.alibaba.android.arouter.facade.annotation.Route
import com.jeremyliao.liveeventbus.LiveEventBus
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * 资产信息界面
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/7
 */
@Route(path = ROUTE_PATH_ASSET_INFO)
class AssetInfoActivity : BaseActivity<AssetInfoViewModel, ActivityAssetInfoBinding>() {

    override val viewModel: AssetInfoViewModel by viewModel()

    private val pagingAdapter: DateRecordPagingRvAdapter by lazy {
        DateRecordPagingRvAdapter().apply {
            this.viewModel = this@AssetInfoActivity.viewModel
        }
    }

    override fun beforeOnCreate() {
        super.beforeOnCreate()

        // 获取资产信息
        viewModel.assetData.value = intent.getParcelableExtra(ACTION_ASSET)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_asset_info)

        // 配置 RecyclerView
        binding.rv.run {
            layoutManager = WrapContentLinearLayoutManager()
            adapter = pagingAdapter
        }
    }

    override fun observe() {
        // 数据列表
        viewModel.recordListData.observe(this) { list ->
            pagingAdapter.submitData(this.lifecycle, list)
            viewModel.refreshing.value = false
        }
        // 刷新数据
        viewModel.refreshing.observe(this) {
            if (it) {
                pagingAdapter.refresh()
            }
        }
        // 跳转编辑
        viewModel.jumpEditAssetEvent.observe(this) { asset ->
            EditAssetActivity.actionStart(context, asset)
        }
        // 显示记录详情弹窗
        viewModel.showRecordDetailsDialogEvent.observe(this) { record ->
            RecordInfoDialog.actionShow(supportFragmentManager, record)
        }
        // 显示删除确认弹窗
        viewModel.showDeleteConfirmDialogEvent.observe(this) {
            GeneralDialog.newBuilder()
                .contentStr(R.string.delete_asset_hint.string)
                .setOnPositiveAction {
                    // 删除账本
                    viewModel.deleteAsset()
                }.show(supportFragmentManager)

        }
        // 记录变化监听
        LiveEventBus.get<Int>(EVENT_RECORD_CHANGE).observe(this) {
            pagingAdapter.refresh()
            viewModel.refreshAsset()
        }
    }
}