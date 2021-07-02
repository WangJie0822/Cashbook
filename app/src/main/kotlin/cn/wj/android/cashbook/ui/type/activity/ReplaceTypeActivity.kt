package cn.wj.android.cashbook.ui.type.activity

import android.os.Bundle
import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.base.ui.BaseActivity
import cn.wj.android.cashbook.data.constants.ROUTE_PATH_TYPE_REPLACE
import cn.wj.android.cashbook.databinding.ActivityReplaceTypeBinding
import cn.wj.android.cashbook.ui.type.viewmodel.ReplaceTypeViewModel
import com.alibaba.android.arouter.facade.annotation.Route
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * 替换分类
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/7/2
 */
@Route(path = ROUTE_PATH_TYPE_REPLACE)
class ReplaceTypeActivity : BaseActivity<ReplaceTypeViewModel, ActivityReplaceTypeBinding>() {

    override val viewModel: ReplaceTypeViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_replace_type)
    }

}