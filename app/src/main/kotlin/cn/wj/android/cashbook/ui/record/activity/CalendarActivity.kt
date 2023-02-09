package cn.wj.android.cashbook.ui.record.activity

import android.os.Bundle
import android.view.LayoutInflater
import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.base.ui.BaseActivity
import cn.wj.android.cashbook.data.constants.EVENT_RECORD_CHANGE
import cn.wj.android.cashbook.data.model.NoDataModel
import cn.wj.android.cashbook.databinding.ActivityCalendarBinding
import cn.wj.android.cashbook.databinding.LayoutNoDataBinding
import cn.wj.android.cashbook.ui.record.adapter.DateRecordRvAdapter
import cn.wj.android.cashbook.ui.record.dialog.RecordInfoDialog
import cn.wj.android.cashbook.ui.record.dialog.SelectYearMonthDialog
import cn.wj.android.cashbook.ui.record.viewmodel.CalendarViewModel
import cn.wj.android.cashbook.widget.recyclerview.layoutmanager.WrapContentLinearLayoutManager
import com.jeremyliao.liveeventbus.LiveEventBus
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * 日历界面
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/7/15
 */
class CalendarActivity : BaseActivity<CalendarViewModel, ActivityCalendarBinding>() {

    override val viewModel: CalendarViewModel by viewModel()

    /** 列表适配器对象 */
    private val adapter: DateRecordRvAdapter by lazy {
        DateRecordRvAdapter().apply {
            this.viewModel = this@CalendarActivity.viewModel
            setEmptyView(
                LayoutNoDataBinding.inflate(
                    LayoutInflater.from(context),
                    binding.rv,
                    false
                ).apply {
                    viewModel = NoDataModel(R.string.no_data_record)
                }.root
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calendar)

        // 配置 RecyclerView
        binding.rv.run {
            layoutManager = WrapContentLinearLayoutManager()
            adapter = this@CalendarActivity.adapter
        }
    }

    override fun doObserve() {
        // 列表数据
        viewModel.listData.observe(this) { list ->
            adapter.submitList(list)
        }
        // 显示记录详情弹窗
        viewModel.showRecordDetailsDialogEvent.observe(this) { record ->
            RecordInfoDialog.actionShow(supportFragmentManager, record)
        }
        // 显示选择年月弹窗
        viewModel.showSelectYearMonthDialogEvent.observe(this) { callback ->
            SelectYearMonthDialog.actionShow(
                supportFragmentManager,
                viewModel.titleStr.value.orEmpty(),
                callback
            )
        }
        // 记录变化监听
        LiveEventBus.get<Int>(EVENT_RECORD_CHANGE).observe(this) {
            viewModel.refresh()
        }
    }
}