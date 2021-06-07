package cn.wj.android.cashbook.ui.asset.activity

import android.os.Bundle
import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.base.ui.BaseActivity
import cn.wj.android.cashbook.data.constants.ACTION_ASSET
import cn.wj.android.cashbook.data.constants.ROUTE_PATH_ASSET_INFO
import cn.wj.android.cashbook.data.entity.AssetEntity
import cn.wj.android.cashbook.databinding.ActivityAssetInfoBinding
import cn.wj.android.cashbook.ui.asset.viewmodel.AssetInfoViewModel
import com.alibaba.android.arouter.facade.annotation.Route
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * TODO 资产信息界面
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/7
 */
@Route(path = ROUTE_PATH_ASSET_INFO)
class AssetInfoActivity : BaseActivity<AssetInfoViewModel, ActivityAssetInfoBinding>() {

    override val viewModel: AssetInfoViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_asset_info)

        // 获取资产信息
        intent.getParcelableExtra<AssetEntity>(ACTION_ASSET)?.run {
            viewModel.titleStr.value = name
        }
    }
}