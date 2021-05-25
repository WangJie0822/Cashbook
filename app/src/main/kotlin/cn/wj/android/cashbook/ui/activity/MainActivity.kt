package cn.wj.android.cashbook.ui.activity

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.base.ext.base.string
import cn.wj.android.cashbook.base.ui.BaseActivity
import cn.wj.android.cashbook.data.constants.AROUTER_PATH_MAIN
import cn.wj.android.cashbook.data.constants.MAIN_BACK_PRESS_INTERVAL_MS
import cn.wj.android.cashbook.data.transform.toSnackbarModel
import cn.wj.android.cashbook.databinding.ActivityMainBinding
import cn.wj.android.cashbook.ui.adapter.BillListRvAdapter
import cn.wj.android.cashbook.ui.viewmodel.MainViewModel
import cn.wj.android.cashbook.widget.recyclerview.layoutmanager.WrapContentLinearLayoutManager
import com.alibaba.android.arouter.facade.annotation.Route
import com.gyf.immersionbar.ImmersionBar
import kotlin.math.absoluteValue
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * 主界面
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/5/11
 */
@Route(path = AROUTER_PATH_MAIN)
class MainActivity : BaseActivity<MainViewModel, ActivityMainBinding>() {

    override val viewModel: MainViewModel by viewModel()

    /** 上次返回点击时间 */
    private var lastBackPressMs = 0L

    private val billListRvAdapter = BillListRvAdapter()

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
            adapter = billListRvAdapter
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
            viewModel.snackbarData.value = R.string.press_again_to_exit.string.toSnackbarModel()
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
        // 列表刷新状态
        viewModel.refreshing.observe(this, { refreshing ->
            if (refreshing) {
                billListRvAdapter.refresh()
            }
        })
        // 账单列表
        viewModel.billListData.observe(this, { pagingData ->
            lifecycleScope.launchWhenCreated {
                billListRvAdapter.submitData(pagingData)
                viewModel.refreshing.value = false
            }
        })
    }
}