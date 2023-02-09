package cn.wj.android.cashbook.ui.main.activity

import android.Manifest
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.GravityCompat
import androidx.documentfile.provider.DocumentFile
import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.base.ext.base.isContentScheme
import cn.wj.android.cashbook.base.ext.base.md2Spanned
import cn.wj.android.cashbook.base.ext.base.string
import cn.wj.android.cashbook.base.ext.scrollToTop
import cn.wj.android.cashbook.base.tools.isWifiAvailable
import cn.wj.android.cashbook.base.ui.BaseActivity
import cn.wj.android.cashbook.data.config.AppConfigs
import cn.wj.android.cashbook.data.constants.ACTION_CONTENT
import cn.wj.android.cashbook.data.constants.EVENT_RECORD_CHANGE
import cn.wj.android.cashbook.data.constants.MAIN_BACK_PRESS_INTERVAL_MS
import cn.wj.android.cashbook.data.constants.ROUTE_PATH_BACKUP
import cn.wj.android.cashbook.data.constants.ROUTE_PATH_RECORD_CALENDAR
import cn.wj.android.cashbook.data.constants.SHORTCUTS_ASSET
import cn.wj.android.cashbook.data.constants.SHORTCUTS_RECORD
import cn.wj.android.cashbook.data.model.NoDataModel
import cn.wj.android.cashbook.data.model.SnackbarModel
import cn.wj.android.cashbook.data.model.UiNavigationModel
import cn.wj.android.cashbook.data.transform.toSnackbarModel
import cn.wj.android.cashbook.databinding.ActivityMainBinding
import cn.wj.android.cashbook.databinding.LayoutNoDataBinding
import cn.wj.android.cashbook.databinding.RecyclerFooterHomepageBinding
import cn.wj.android.cashbook.manager.UpdateManager
import cn.wj.android.cashbook.third.result.createForActivityResultLauncher
import cn.wj.android.cashbook.ui.general.dialog.GeneralDialog
import cn.wj.android.cashbook.ui.main.viewmodel.MainViewModel
import cn.wj.android.cashbook.ui.record.adapter.DateRecordRvAdapter
import cn.wj.android.cashbook.ui.record.dialog.RecordInfoDialog
import cn.wj.android.cashbook.widget.recyclerview.layoutmanager.WrapContentLinearLayoutManager
import com.gyf.immersionbar.ImmersionBar
import com.jeremyliao.liveeventbus.LiveEventBus
import org.koin.androidx.viewmodel.ext.android.viewModel
import kotlin.math.absoluteValue

/**
 * 主界面
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/5/11
 */
class MainActivity : BaseActivity<MainViewModel, ActivityMainBinding>() {

    override val viewModel: MainViewModel by viewModel()

    /** 上次返回点击时间 */
    private var lastBackPressMs = 0L

    /** 权限申请 launcher */
    private val requestPermissionsLauncher =
        createForActivityResultLauncher(ActivityResultContracts.RequestMultiplePermissions())

    /** 列表适配器对象 */
    private val adapter: DateRecordRvAdapter by lazy {
        DateRecordRvAdapter().apply {
            this.viewModel = this@MainActivity.viewModel
            setEmptyView(LayoutNoDataBinding.inflate(LayoutInflater.from(context)).apply {
                viewModel = NoDataModel(
                    R.string.homepage_no_record_hint,
                    showButton = true,
                    buttonTextResId = R.string.click_to_see_other_month
                ) {
                    // 跳转日历
                    this@MainActivity.viewModel.uiNavigationEvent.value =
                        UiNavigationModel.builder {
                            jump(ROUTE_PATH_RECORD_CALENDAR)
                        }
                }
            }.root)
            addFooterView(RecyclerFooterHomepageBinding.inflate(LayoutInflater.from(context)).root.apply {
                setOnClickListener {
                    // 跳转日历
                    this@MainActivity.viewModel.uiNavigationEvent.value =
                        UiNavigationModel.builder {
                            jump(ROUTE_PATH_RECORD_CALENDAR)
                        }
                }
            })
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
            adapter = this@MainActivity.adapter
        }

        // 检查更新
        viewModel.checkUpdate()
        // 自动备份
        viewModel.autoBackup()

        intent.getStringExtra(ACTION_CONTENT)?.let { action ->
            when (action) {
                SHORTCUTS_RECORD -> {
                    // 记一笔
                    viewModel.onAddClick.invoke()
                }

                SHORTCUTS_ASSET -> {
                    // 资产
                    viewModel.onMyAssetClick.invoke()
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()

        if (binding.dlRoot.isDrawerOpen(GravityCompat.START)) {
            // 抽屉开启时关闭
            binding.dlRoot.closeDrawer(GravityCompat.START)
        }
    }

    override fun onBackPressed() {
        if (binding.dlRoot.isDrawerOpen(GravityCompat.START)) {
            // 抽屉开启时关闭
            binding.dlRoot.closeDrawer(GravityCompat.START)
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

    override fun doObserve() {
        // 首页列表
        viewModel.listData.observe(this) { list ->
            adapter.submitList(list)
            binding.includeContent.rv.scrollToTop()
        }
        // 显示记录详情弹窗
        viewModel.showRecordDetailsDialogEvent.observe(this) { record ->
            RecordInfoDialog.actionShow(supportFragmentManager, record)
        }
        // 升级提示弹窗
        viewModel.showUpdateDialogEvent.observe(this) { info ->
            GeneralDialog.newBuilder()
                .contentStr(info.versionInfo.md2Spanned())
                .showSelect(true)
                .selectTipsStr(R.string.ignore_update_hint.string)
                .setPositiveAction(R.string.update.string) {
                    // 下载升级
                    if (isWifiAvailable() || AppConfigs.mobileNetworkDownloadEnable) {
                        // WIFI 可用或允许使用流量下载，直接开始下载
                        UpdateManager.startDownload(info)
                        viewModel.snackbarEvent.value =
                            R.string.start_background_download.string.toSnackbarModel()
                    } else {
                        // 未连接 WIFI 且未允许流量下载，弹窗提示
                        GeneralDialog.newBuilder()
                            .contentStr(R.string.no_wifi_available_hint.string)
                            .showSelect(true)
                            .setOnPositiveAction {
                                // 保存用户选择
                                AppConfigs.mobileNetworkDownloadEnable = it
                                // 开始下载
                                UpdateManager.startDownload(info)
                                viewModel.snackbarEvent.value =
                                    R.string.start_background_download.string.toSnackbarModel()
                            }
                            .show(supportFragmentManager)
                    }
                }
                .setOnNegativeAction {
                    if (it) {
                        // 忽略该版本
                        AppConfigs.ignoreVersion = info.versionName
                    }
                }
                .show(supportFragmentManager)
        }
        // 检查备份路径
        viewModel.checkBackupPathEvent.observe(this, fun(path) {
            if (path.isNullOrBlank()) {
                showPathErrorHint()
                return
            }
            if (path.isContentScheme()) {
                if (DocumentFile.fromTreeUri(this, Uri.parse(path))?.canRead() == true) {
                    // 有权限，开始备份
                    viewModel.tryBackup()
                } else {
                    // 没有权限，提示
                    showPathErrorHint()
                }
            } else {
                // 申请权限
                requestPermissionsLauncher.launch(
                    arrayOf(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )
                ) { map ->
                    if (map.values.contains(false)) {
                        // 有未同意权限
                        showPathErrorHint()
                        return@launch
                    }
                    // 开始备份
                    viewModel.tryBackup()
                }
            }
        })
        // 记录变化监听
        LiveEventBus.get<Int>(EVENT_RECORD_CHANGE).observe(this) {
            viewModel.refreshing.value = true
        }
    }

    /** 显示备份路径异常提示 */
    private fun showPathErrorHint() {
        viewModel.snackbarEvent.value = R.string.auto_backup_exception.string.toSnackbarModel(
            duration = SnackbarModel.LENGTH_INDEFINITE,
            actionText = R.string.view.string,
            onAction = {
                // 点击跳转备份界面
                viewModel.uiNavigationEvent.value = UiNavigationModel.builder {
                    jump(ROUTE_PATH_BACKUP)
                }
            })
    }
}