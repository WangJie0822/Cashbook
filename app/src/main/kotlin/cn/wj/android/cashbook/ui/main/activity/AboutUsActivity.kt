package cn.wj.android.cashbook.ui.main.activity

import android.os.Bundle
import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.base.ext.base.runIfNotNullAndBlank
import cn.wj.android.cashbook.base.tools.jumpBrowser
import cn.wj.android.cashbook.base.tools.jumpSendEmail
import cn.wj.android.cashbook.base.ui.BaseActivity
import cn.wj.android.cashbook.data.constants.ROUTE_PATH_ABOUT_US
import cn.wj.android.cashbook.databinding.ActivityAboutUsBinding
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
        viewModel.jumpSendEmailData.observe(this, { email ->
            email.runIfNotNullAndBlank {
                jumpSendEmail(this)
            }
        })
        // 跳转浏览器打开
        viewModel.jumpBrowserData.observe(this, { url ->
            url.runIfNotNullAndBlank {
                jumpBrowser(this)
            }
        })
    }
}