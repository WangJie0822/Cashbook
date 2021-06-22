package cn.wj.android.cashbook.ui.main.activity

import android.os.Bundle
import android.view.LayoutInflater
import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.base.ext.base.string
import cn.wj.android.cashbook.base.ui.BaseActivity
import cn.wj.android.cashbook.data.constants.EVENT_RECORD_CHANGE
import cn.wj.android.cashbook.data.constants.MAIN_BACK_PRESS_INTERVAL_MS
import cn.wj.android.cashbook.data.constants.ROUTE_PATH_MAIN
import cn.wj.android.cashbook.data.model.NoDataModel
import cn.wj.android.cashbook.data.transform.toSnackbarModel
import cn.wj.android.cashbook.databinding.ActivityMainBinding
import cn.wj.android.cashbook.databinding.LayoutNoDataRecordBinding
import cn.wj.android.cashbook.ui.record.adapter.DateRecordRvAdapter
import cn.wj.android.cashbook.ui.main.viewmodel.MainViewModel
import cn.wj.android.cashbook.ui.record.dialog.RecordInfoDialog
import cn.wj.android.cashbook.widget.recyclerview.layoutmanager.WrapContentLinearLayoutManager
import com.alibaba.android.arouter.facade.annotation.Route
import com.gyf.immersionbar.ImmersionBar
import com.jeremyliao.liveeventbus.LiveEventBus
import kotlin.math.absoluteValue
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * 主界面
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/5/11
 */
@Route(path = ROUTE_PATH_MAIN)
class MainActivity : BaseActivity<MainViewModel, ActivityMainBinding>() {

    override val viewModel: MainViewModel by viewModel()

    /** 上次返回点击时间 */
    private var lastBackPressMs = 0L

    /** 列表适配器对象 */
    private val adapter: DateRecordRvAdapter by lazy {
        DateRecordRvAdapter().apply {
            this.viewModel = this@MainActivity.viewModel
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        snackbarTransform = {
            // 使用 CoordinatorLayout 显示 Snackbar
            it.copy(targetId = binding.includeContent.clContent.id)
        }

        // 配置 RecyclerView
        binding.includeContent.rv.run {
            layoutManager = WrapContentLinearLayoutManager()
            adapter = this@MainActivity.adapter.apply {
                setEmptyView(LayoutNoDataRecordBinding.inflate(LayoutInflater.from(context)).apply {
                    viewModel = NoDataModel(R.string.no_bill_hint)
                }.root)
            }
        }
    }

    override fun onStop() {
        super.onStop()

        if (binding.dlRoot.isOpen) {
            // 抽屉开启时关闭
            binding.dlRoot.close()
        }
    }

    override fun onBackPressed() {
        if (binding.dlRoot.isOpen) {
            // 抽屉开启时关闭
            binding.dlRoot.close()
            return
        }
        // 当前时间
        val currentBackPressMs = System.currentTimeMillis()
        if ((currentBackPressMs - lastBackPressMs).absoluteValue > MAIN_BACK_PRESS_INTERVAL_MS) {
            // 间隔时间外，提示
            viewModel.snackbarEvent.value = R.string.press_again_to_exit.string.toSnackbarModel()
            // 保存时间
            lastBackPressMs = currentBackPressMs
        } else {
            // 间隔时间内，退到后台
            moveTaskToBack(true)
        }
    }

    override fun initImmersionbar(immersionBar: ImmersionBar) {
        immersionBar.run {
            transparentStatusBar()
            fitsSystemWindows(false)
        }
    }

    override fun observe() {
        // 首页列表
        viewModel.listData.observe(this, { list ->
            adapter.submitList(list)
        })
        // 显示记录详情弹窗
        viewModel.showRecordDetailsDialogEvent.observe(this, { record ->
            RecordInfoDialog.actionShow(supportFragmentManager, record)
        })
        // 记录变化监听
        LiveEventBus.get(EVENT_RECORD_CHANGE).observe(this, {
            viewModel.refreshing.value = true
        })
    }
}