package cn.wj.android.cashbook.ui.record.activity

import android.os.Bundle
import androidx.recyclerview.widget.RecyclerView
import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.base.ui.BaseActivity
import cn.wj.android.cashbook.data.constants.EVENT_RECORD_CHANGE
import cn.wj.android.cashbook.data.constants.ROUTE_PATH_RECORD_SEARCH
import cn.wj.android.cashbook.databinding.ActivitySearchRecordBinding
import cn.wj.android.cashbook.ui.record.adapter.SearchRecordPagingRvAdapter
import cn.wj.android.cashbook.ui.record.dialog.RecordInfoDialog
import cn.wj.android.cashbook.ui.record.viewmodel.SearchRecordViewModel
import cn.wj.android.cashbook.widget.recyclerview.layoutmanager.WrapContentLinearLayoutManager
import com.alibaba.android.arouter.facade.annotation.Route
import com.jeremyliao.liveeventbus.LiveEventBus
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * 搜索记录界面
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/7/23
 */
@Route(path = ROUTE_PATH_RECORD_SEARCH)
class SearchRecordActivity : BaseActivity<SearchRecordViewModel, ActivitySearchRecordBinding>() {

    override val viewModel: SearchRecordViewModel by viewModel()

    /** 适配器对象 */
    private val adapter: SearchRecordPagingRvAdapter by lazy {
        SearchRecordPagingRvAdapter().apply {
            this.viewModel = this@SearchRecordActivity.viewModel
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search_record)

        // 配置 RecyclerView
        binding.rv.run {
            layoutManager = WrapContentLinearLayoutManager()
            adapter = this@SearchRecordActivity.adapter.apply {
                registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {

                    override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                        this@SearchRecordActivity.viewModel.showNoData.value = this@apply.itemCount <= 0
                    }

                    override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
                        this@SearchRecordActivity.viewModel.showNoData.value = this@apply.itemCount <= 0
                    }
                })
            }
        }
    }

    override fun doObserve() {
        // 列表数据
        viewModel.listData.observe(this) { list ->
            adapter.submitData(this.lifecycle, list)
            viewModel.refreshing.value = false
        }
        // 刷新数据
        viewModel.refreshing.observe(this) {
            if (it) {
                adapter.refresh()
            }
        }
        // 显示记录详情弹窗
        viewModel.showRecordDetailsDialogEvent.observe(this) { record ->
            RecordInfoDialog.actionShow(supportFragmentManager, record)
        }
        // 记录变化监听
        LiveEventBus.get<Int>(EVENT_RECORD_CHANGE).observe(this) {
            adapter.refresh()
        }
    }
}