@file:Suppress("MemberVisibilityCanBePrivate")

package cn.wj.android.cashbook.base.ui

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Observer
import cn.wj.android.cashbook.BR
import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.base.ext.base.logger
import cn.wj.android.cashbook.data.model.SnackbarModel
import cn.wj.android.cashbook.manager.ProgressDialogManager
import com.alibaba.android.arouter.facade.Postcard
import com.alibaba.android.arouter.facade.callback.NavigationCallback
import com.alibaba.android.arouter.launcher.ARouter
import com.google.android.material.snackbar.Snackbar

/**
 * 应用 [DialogFragment] 基类
 * - [VM] 为 [BaseViewModel] 泛型，[DB] 为 [ViewDataBinding] 泛型
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 20201/3/8
 */
abstract class BaseDialog<VM : BaseViewModel, DB : ViewDataBinding> : AppCompatDialogFragment() {

    /** 布局 id */
    protected abstract val layoutResId: Int

    /** 界面 [BaseViewModel] 对象 */
    protected abstract val viewModel: VM

    /** 标记 - 是否从 Activity 获取 ViewModel */
    protected open val activityViewModel: Boolean = false

    /** 主题 id，按需重写 */
    protected open val themeId: Int = R.style.Theme_Cashbook_Dialog

    /** Dialog 宽度，按需重写 单位：px  */
    protected open val dialogWidth: Int = WindowManager.LayoutParams.WRAP_CONTENT

    /** Dialog 高度，按需重写 单位：px */
    protected open val dialogHeight: Int = WindowManager.LayoutParams.WRAP_CONTENT

    /** Dialog 重心 [Gravity]，按需重写 */
    protected open val gravity: Int = Gravity.CENTER

    /** 界面 [ViewDataBinding] 对象 */
    protected lateinit var binding: DB

    /** 根布局对象 */
    protected var rootView: View? = null

    /** [Snackbar] 转换接口 */
    protected var snackbarTransform: ((SnackbarModel) -> SnackbarModel)? = null

    /** Dialog 隐藏回调 */
    private var onDialogDismissListener: OnDialogDismissListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        beforeOnCreate()
        super.onCreate(savedInstanceState)

        // 设置样式
        setStyle(STYLE_NO_TITLE, themeId)

        // 订阅基本数据
        if (!activityViewModel) {
            observeBaseModel(viewModel)
        }

        // 订阅数据
        doObserve()
        logger().d("onCreate")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        // 加载布局
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 配置 Dialog 宽高、重心
        val layoutParams = dialog?.window?.attributes
        layoutParams?.width = dialogWidth
        layoutParams?.height = dialogHeight
        layoutParams?.gravity = gravity
        dialog?.window?.attributes = layoutParams
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

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        onDialogDismissListener?.invoke()
    }

    override fun setCancelable(cancelable: Boolean) {
        super.setCancelable(cancelable)
        dialog?.setCanceledOnTouchOutside(cancelable)
    }

    /** 设置 Dialog 隐藏回调 [listener] */
    fun setOnDialogDismissListener(listener: OnDialogDismissListener?) {
        onDialogDismissListener = listener
    }


    /** [onCreate] 之前执行 */
    protected open fun beforeOnCreate() {
    }

    /** 订阅数据 */
    protected open fun doObserve() {
    }

    /** 订阅基本数据 */
    protected fun observeBaseModel(viewModel: BaseViewModel) {
        // 进度弹窗
        viewModel.progressEvent.observe(this, Observer {
            if (null == it) {
                ProgressDialogManager.dismiss()
                return@Observer
            }
            ProgressDialogManager.show(requireActivity(), it.cancelable, it.hint)
        })
        // snackbar 提示
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
        viewModel.uiNavigationEvent.observe(this) {
            logger().d("uiNavigation: $it")
            it.jump?.let { model ->
                ARouter.getInstance().build(model.path).with(model.data)
                    .navigation(context, object : NavigationCallback {
                        override fun onFound(postcard: Postcard?) {
                        }

                        override fun onLost(postcard: Postcard?) {
                        }

                        override fun onArrival(postcard: Postcard?) {
                            model.onArrival?.invoke()
                        }

                        override fun onInterrupt(postcard: Postcard?) {
                            model.onIntercept?.invoke()
                        }

                    })
            }
            it.close?.let { model ->
                dismiss()
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
        }
    }

    fun show(manager: FragmentManager) {
        show(manager, tag)
    }

    /**
     * 初始化布局
     */
    protected abstract fun initView()
}

/** 弹窗隐藏回调 */
typealias OnDialogDismissListener = () -> Unit