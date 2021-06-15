package cn.wj.android.cashbook.ui.asset.activity

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.base.ui.BaseActivity
import cn.wj.android.cashbook.data.constants.ACTION_ASSET
import cn.wj.android.cashbook.data.constants.EVENT_RECORD_CHANGE
import cn.wj.android.cashbook.data.constants.ROUTE_PATH_ASSET_INFO
import cn.wj.android.cashbook.databinding.ActivityAssetInfoBinding
import cn.wj.android.cashbook.ui.asset.viewmodel.AssetInfoViewModel
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_asset_info)

        // 配置 RecyclerView
        binding.rv.run {
            layoutManager = WrapContentLinearLayoutManager()
            adapter = pagingAdapter
        }
    }

    override fun beforeOnCreate() {
        // 获取资产信息
        viewModel.assetData.value = intent.getParcelableExtra(ACTION_ASSET)
    }

    override fun observe() {
        // 数据列表
        viewModel.recordListData.observe(this, { list ->
            lifecycleScope.launchWhenCreated {
                pagingAdapter.submitData(list)
                viewModel.refreshing.value = false
            }
        })
        // 刷新数据
        viewModel.refreshing.observe(this, {
            if (it) {
                pagingAdapter.refresh()
            }
        })
        // 显示记录详情弹窗
        viewModel.showRecordDetailsDialogData.observe(this, { record ->
            RecordInfoDialog.actionShow(supportFragmentManager, record)
        })
        // 记录变化监听
        LiveEventBus.get(EVENT_RECORD_CHANGE).observe(this, {
            pagingAdapter.refresh()
        })
    }
}