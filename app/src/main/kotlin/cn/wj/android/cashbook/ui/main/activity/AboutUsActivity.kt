package cn.wj.android.cashbook.ui.main.activity

import android.os.Bundle
import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.base.ext.base.md2Spanned
import cn.wj.android.cashbook.base.ext.base.runIfNotNullAndBlank
import cn.wj.android.cashbook.base.ext.base.string
import cn.wj.android.cashbook.base.tools.getSharedBoolean
import cn.wj.android.cashbook.base.tools.isWifiAvailable
import cn.wj.android.cashbook.base.tools.jumpBrowser
import cn.wj.android.cashbook.base.tools.jumpSendEmail
import cn.wj.android.cashbook.base.tools.setSharedBoolean
import cn.wj.android.cashbook.base.ui.BaseActivity
import cn.wj.android.cashbook.data.constants.ROUTE_PATH_ABOUT_US
import cn.wj.android.cashbook.data.constants.SHARED_KEY_MOBILE_NETWORK_DOWNLOAD_ENABLE
import cn.wj.android.cashbook.data.transform.toSnackbarModel
import cn.wj.android.cashbook.databinding.ActivityAboutUsBinding
import cn.wj.android.cashbook.manager.UpdateManager
import cn.wj.android.cashbook.ui.general.dialog.GeneralDialog
import cn.wj.android.cashbook.ui.main.viewmodel.AboutUsViewModel
import com.alibaba.android.arouter.facade.annotation.Route
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * 关于我们界面
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/17
 */
@Route(path = ROUTE_PATH_ABOUT_US)
class AboutUsActivity : BaseActivity<AboutUsViewModel, ActivityAboutUsBinding>() {

    override val viewModel: AboutUsViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about_us)
    }

    override fun observe() {
        // 跳转发送邮件
        viewModel.jumpSendEmailEvent.observe(this, { email ->
            email.runIfNotNullAndBlank {
                jumpSendEmail(this)
            }
        })
        // 跳转浏览器打开
        viewModel.jumpBrowserEvent.observe(this, { url ->
            url.runIfNotNullAndBlank {
                jumpBrowser(this)
            }
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
    }
}