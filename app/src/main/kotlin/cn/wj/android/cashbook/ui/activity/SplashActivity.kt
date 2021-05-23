package cn.wj.android.cashbook.ui.activity

import android.os.Bundle
import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.base.ui.BaseActivity
import cn.wj.android.cashbook.data.constants.ACTIVITY_ANIM_DURATION
import cn.wj.android.cashbook.databinding.ActivitySplashBinding
import cn.wj.android.cashbook.ui.viewmodel.SplashViewModel
import com.google.android.material.transition.platform.MaterialFadeThrough
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * 闪屏界面
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/5/16
 */
class SplashActivity : BaseActivity<SplashViewModel, ActivitySplashBinding>() {

    override val viewModel: SplashViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // 初始化数据
        viewModel.init()
    }

    override fun beforeOnCreate() {
        window.run {
            enterTransition = MaterialFadeThrough().apply {
                duration = ACTIVITY_ANIM_DURATION
            }
            exitTransition = MaterialFadeThrough().apply {
                duration = ACTIVITY_ANIM_DURATION
            }
        }
    }
}