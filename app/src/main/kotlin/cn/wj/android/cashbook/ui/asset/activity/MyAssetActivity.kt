package cn.wj.android.cashbook.ui.asset.activity

import android.os.Bundle
import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.base.ui.BaseActivity
import cn.wj.android.cashbook.data.constants.ROUTE_PATH_MY_ASSET
import cn.wj.android.cashbook.databinding.ActivityMyAssetBinding
import cn.wj.android.cashbook.ui.asset.viewmodel.MyAssetViewModel
import com.alibaba.android.arouter.facade.annotation.Route
import com.gyf.immersionbar.ImmersionBar
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * 我的资产界面
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/3
 */
@Route(path = ROUTE_PATH_MY_ASSET)
class MyAssetActivity : BaseActivity<MyAssetViewModel, ActivityMyAssetBinding>() {

    override val viewModel: MyAssetViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_asset)
    }

    override fun initImmersionbar(immersionBar: ImmersionBar) {
        immersionBar.run {
            transparentStatusBar()
            statusBarDarkFont(true)
            fitsSystemWindows(false)
        }
    }
}