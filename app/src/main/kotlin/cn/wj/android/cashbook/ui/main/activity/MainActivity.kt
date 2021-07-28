package cn.wj.android.cashbook.ui.main.activity

import android.os.Bundle
import android.view.LayoutInflater
import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.base.ext.base.md2Spanned
import cn.wj.android.cashbook.base.ext.base.string
import cn.wj.android.cashbook.base.tools.getSharedBoolean
import cn.wj.android.cashbook.base.tools.isWifiAvailable
import cn.wj.android.cashbook.base.tools.setSharedBoolean
import cn.wj.android.cashbook.base.ui.BaseActivity
import cn.wj.android.cashbook.data.constants.EVENT_RECORD_CHANGE
import cn.wj.android.cashbook.data.constants.EVENT_TYPE_CHANGE
import cn.wj.android.cashbook.data.constants.MAIN_BACK_PRESS_INTERVAL_MS
import cn.wj.android.cashbook.data.constants.ROUTE_PATH_MAIN
import cn.wj.android.cashbook.data.constants.SHARED_KEY_MOBILE_NETWORK_DOWNLOAD_ENABLE
import cn.wj.android.cashbook.data.model.NoDataModel
import cn.wj.android.cashbook.data.transform.toSnackbarModel
import cn.wj.android.cashbook.databinding.ActivityMainBinding
import cn.wj.android.cashbook.databinding.LayoutNoDataBinding
import cn.wj.android.cashbook.manager.UpdateManager
import cn.wj.android.cashbook.ui.general.dialog.GeneralDialog
import cn.wj.android.cashbook.ui.main.viewmodel.MainViewModel
import cn.wj.android.cashbook.ui.record.adapter.DateRecordRvAdapter
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
                setEmptyView(LayoutNoDataBinding.inflate(LayoutInflater.from(context)).apply {
                    viewModel = NoDataModel(R.string.no_bill_hint)
                }.root)
            }
        }

        // 检查更新
        viewModel.checkUpdate()
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
        // 升级提示弹窗
        viewModel.showUpdateDialogEvent.observe(this, { info ->
            GeneralDialog.newBuilder()
                .contentStr(info.versionInfo.md2Spanned())
                .setPositiveAction(R.string.update.string) {
                    // 下载升级
                    if (isWifiAvailable() || getSharedBoolean(SHARED_KEY_MOBILE_NETWORK_DOWNLOAD_ENABLE)) {
                        // WIFI 可用或允许使用流量下载，直接开始下载
                        UpdateManager.startDownload(info)
                        viewModel.snackbarEvent.value = R.string.start_background_download.string.toSnackbarModel()
                    } else {
                        // 未连接 WIFI 且未允许流量下载，弹窗提示
                        GeneralDialog.newBuilder()
                            .contentStr(R.string.no_wifi_available_hint.string)
                            .showSelect(true)
                            .setOnPositiveAction {
                                // 保存用户选择
                                setSharedBoolean(SHARED_KEY_MOBILE_NETWORK_DOWNLOAD_ENABLE, it)
                                // 开始下载
                                UpdateManager.startDownload(info)
                                viewModel.snackbarEvent.value = R.string.start_background_download.string.toSnackbarModel()
                            }
                            .show(supportFragmentManager)
                    }
                }
                .show(supportFragmentManager)
        })
        // 记录变化监听
        LiveEventBus.get(EVENT_RECORD_CHANGE).observe(this, {
            viewModel.refreshing.value = true
        })
        // 分类数据变化
        LiveEventBus.get(EVENT_TYPE_CHANGE).observe(this, {
            viewModel.refreshing.value = true
        })
    }
}