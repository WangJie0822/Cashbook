@file:Suppress("MemberVisibilityCanBePrivate")

package cn.wj.android.cashbook.base.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.Observer
import cn.wj.android.cashbook.BR
import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.base.ext.base.logger
import cn.wj.android.cashbook.base.ext.base.tag
import cn.wj.android.cashbook.base.ext.firstVisibleFragmentOrNull
import cn.wj.android.cashbook.base.ext.hideSoftKeyboard
import cn.wj.android.cashbook.base.tools.shouldHideInput
import cn.wj.android.cashbook.data.constants.ACTIVITY_ANIM_DURATION
import cn.wj.android.cashbook.data.model.SnackbarModel
import cn.wj.android.cashbook.manager.ProgressDialogManager
import com.alibaba.android.arouter.launcher.ARouter
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.transition.platform.MaterialSharedAxis
import com.gyf.immersionbar.ImmersionBar
import com.gyf.immersionbar.ktx.immersionBar

/**
 * 应用 [AppCompatActivity] 基类
 * - [VM] 为 [BaseViewModel] 泛型，[DB] 为 [ViewDataBinding] 泛型
 *
 * > [jiewang41](mailto:jiewang41@iflytek.com) 创建于 20201/3/8
 */
abstract class BaseActivity<VM : BaseViewModel, DB : ViewDataBinding> :
    AppCompatActivity() {

    /** 当前界面对应 [Context] 对象 */
    protected val context: Context
        get() = this

    /** 界面 [BaseViewModel] 对象 */
    protected abstract val viewModel: VM

    /** 界面 [ViewDataBinding] 对象 */
    protected lateinit var binding: DB

    /** [Snackbar] 转换接口 */
    protected var snackbarTransform: ((SnackbarModel) -> SnackbarModel)? = null

    /** 标记 - 触摸输入框以外范围是否隐藏软键盘*/
    protected var touchToHideInput = true

    override fun onCreate(savedInstanceState: Bundle?) {
        beforeOnCreate()
        super.onCreate(savedInstanceState)

        // 初始化状态栏工具
        initImmersionbar()

        // 订阅基本数据
        observeBaseModel()

        // 订阅数据
        observe()
        logger().d("onCreate")
    }

    override fun onStart() {
        super.onStart()
        logger().d("onStart")
    }

    override fun onResume() {
        super.onResume()
        logger().d("onResume")
    }

    override fun onPause() {
        super.onPause()
        logger().d("onPause")
    }

    override fun onStop() {
        super.onStop()
        logger().d("onStop")
    }

    override fun onDestroy() {
        super.onDestroy()
        logger().d("onDestroy")
    }

    override fun setContentView(layoutResID: Int) {
        // 初始化 DataBinding
        binding = DataBindingUtil.inflate(
            LayoutInflater.from(context),
            layoutResID, null, false
        )

        // 绑定生命周期管理
        binding.lifecycleOwner = this

        // 绑定 ViewModel
        binding.setVariable(BR.viewModel, viewModel)

        // 设置布局
        super.setContentView(binding.root)
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (touchToHideInput) {
            if (ev.action == MotionEvent.ACTION_DOWN) {
                if (shouldHideInput(currentFocus, ev)) {
                    // 需要隐藏软键盘
                    currentFocus?.hideSoftKeyboard()
                }
                return super.dispatchTouchEvent(ev)
            }
            if (window.superDispatchTouchEvent(ev)) {
                return true
            }
            return onTouchEvent(ev)
        } else {
            return super.dispatchTouchEvent(ev)
        }
    }

    override fun onBackPressed() {
        val visibleFragment = supportFragmentManager.firstVisibleFragmentOrNull()
        if (null == visibleFragment) {
            // 没有子 Fragment，返回当前处理
            super.onBackPressed()
        } else {
            // 有子 Fragment
            if (visibleFragment is OnBackPressedWatcher) {
                // 关注返回事件，有处理返回逻辑
                if (!visibleFragment.handleOnBackPressed()) {
                    // 子 Fragment 未消费，返回当前处理
                    super.onBackPressed()
                }
            } else {
                // 子 Fragment 没有返回逻辑，返回当前处理
                super.onBackPressed()
            }
        }
    }

    /** [onCreate] 之前执行，可用于配置动画 */
    protected open fun beforeOnCreate() {
        window.run {
            enterTransition = MaterialSharedAxis(MaterialSharedAxis.X, true).apply {
                duration = ACTIVITY_ANIM_DURATION
            }
            exitTransition = MaterialSharedAxis(MaterialSharedAxis.X, false).apply {
                duration = ACTIVITY_ANIM_DURATION
            }
        }
    }

    /** 订阅数据 */
    protected open fun observe() {
    }

    /** 初始化状态栏相关配置 */
    protected open fun initImmersionbar(immersionBar: ImmersionBar) {
    }

    /** 初始化状态栏相关配置 */
    private fun initImmersionbar() {
        immersionBar {
            statusBarColor(R.color.color_primary)
            fitsSystemWindows(true)
            initImmersionbar(this)
            addTag(tag)
        }
    }

    /** 订阅基本数据 */
    private fun observeBaseModel() {
        // 进度弹窗
        viewModel.progressEvent.observe(this, Observer {
            if (null == it) {
                ProgressDialogManager.dismiss()
                return@Observer
            }
            ProgressDialogManager.show(this, it.cancelable, it.hint)
        })
        // Snackbar 提示
        viewModel.snackbarEvent.observe(this, Observer {
            if (it.content.isNullOrBlank()) {
                return@Observer
            }

            // 转换处理
            val model = snackbarTransform?.invoke(it) ?: it
            logger().d("showSnackbar: $model")
            val view = if (model.targetId == 0) {
                binding.root
            } else {
                findViewById(model.targetId)
            }
            val snackBar = Snackbar.make(view, model.content.orEmpty(), model.duration)
            snackBar.setTextColor(model.contentColor)
            snackBar.setBackgroundTint(model.contentBgColor)
            if (model.actionText != null && model.onAction != null) {
                snackBar.setAction(model.actionText, model.onAction)
                snackBar.setActionTextColor(model.actionColor)
            }
            if (model.onCallback != null) {
                snackBar.addCallback(model.onCallback)
            }
            snackBar.show()
        })
        // UI 界面处理
        viewModel.uiNavigationEvent.observe(this, {
            logger().d("uiNavigation: $it")
            it.jump?.let { model ->
                ARouter.getInstance().build(model.path).with(model.data).navigation(context)
            }
            it.close?.let { model ->
                if (null == model.result) {
                    setResult(model.resultCode)
                } else {
                    setResult(model.resultCode, Intent().putExtras(model.result))
                }
                finish()
            }
        })
    }
}