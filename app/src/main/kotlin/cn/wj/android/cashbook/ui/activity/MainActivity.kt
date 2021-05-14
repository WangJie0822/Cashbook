package cn.wj.android.cashbook.ui.activity

import android.os.Bundle
import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.base.BaseActivity
import cn.wj.android.cashbook.databinding.ActivityMainBinding
import cn.wj.android.cashbook.ui.viewmodel.MainViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * 主界面
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/5/11
 */
class MainActivity : BaseActivity<MainViewModel, ActivityMainBinding>() {

    override val viewModel: MainViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }
}