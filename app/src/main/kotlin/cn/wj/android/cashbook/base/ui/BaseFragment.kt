@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package cn.wj.android.cashbook.base.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import cn.wj.android.cashbook.BR
import cn.wj.android.cashbook.base.ext.base.logger
import cn.wj.android.cashbook.data.model.SnackbarModel
import com.alibaba.android.arouter.launcher.ARouter
import com.google.android.material.snackbar.Snackbar

/**
 * 应用 [Fragment] 基类
 * - [VM] 为 [BaseViewModel] 泛型，[DB] 为 [ViewDataBinding] 泛型
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/5/28
 */
abstract class BaseFragment<VM : BaseViewModel, DB : ViewDataBinding> : Fragment() {

    /** 布局 id */
    protected abstract val layoutResId: Int

    /** 界面 [BaseViewModel] 对象 */
    protected abstract val viewModel: VM

    /** 界面 [ViewDataBinding] 对象 */
    protected lateinit var binding: DB

    /** 标记 - 第一次加载 */
    protected var firstLoad = true
        private set

    /** 根布局对象 */
    protected var rootView: View? = null

    /** [Snackbar] 转换接口 */
    protected var snackbarTransform: ((SnackbarModel) -> SnackbarModel)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 订阅基本数据
        observeBaseModel()

        // 订阅数据
        observe()
        logger().d("onCreate")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        // 初始化 DataBinding
        binding = DataBindingUtil.inflate(inflater, layoutResId, container, false)

        // 绑定生命周期管理
        binding.lifecycleOwner = this

        // 绑定 ViewModel
        binding.setVariable(BR.viewModel, viewModel)

        // 初始化布局
        initView()

        if (null != rootView) {
            // rootView 不为空时从父布局移除
            (rootView?.parent as? ViewGroup?)?.removeView(rootView)
        }

        rootView = binding.root

        return rootView
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

        // 标记不是第一次加载
        firstLoad = false
    }

    override fun onStop() {
        super.onStop()
        logger().d("onStop")
    }

    override fun onDestroy() {
        super.onDestroy()
        logger().d("onDestroy")
    }


    /** 订阅数据 */
    protected open fun observe() {
    }

    /** 订阅基本数据 */
    private fun observeBaseModel() {
        // snackbar 提示
        viewModel.snackbarData.observe(this, Observer {
            if (it.content.isNullOrBlank()) {
                return@Observer
            }

            // 转换处理
            val model = snackbarTransform?.invoke(it) ?: it
            logger().d("showSnackbar: $model")
            val view = if (model.targetId == 0) {
                binding.root
            } else {
                binding.root.findViewById(model.targetId)
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
        // Ui 界面处理
        viewModel.uiNavigationData.observe(this, {
            logger().d("uiNavigation: $it")
            it.jump?.let { model ->
                ARouter.getInstance().build(model.path).with(model.data).navigation(context)
            }
            it.close?.let { model ->
                if (model.both) {
                    requireActivity().run {
                        if (null == model.result) {
                            setResult(model.resultCode)
                        } else {
                            setResult(model.resultCode, Intent().putExtras(model.result))
                        }
                        finish()
                    }
                }

            }
        })
    }

    /**
     * 初始化布局
     */
    abstract fun initView()
}