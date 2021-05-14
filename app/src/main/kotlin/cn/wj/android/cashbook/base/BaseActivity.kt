@file:Suppress("MemberVisibilityCanBePrivate")

package cn.wj.android.cashbook.base

import android.content.Context
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.SkinAppCompatDelegateImpl
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import cn.wj.android.cashbook.BR

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

    override fun getDelegate(): AppCompatDelegate {
        // 支持 SkinSupport 换肤
        return SkinAppCompatDelegateImpl.get(this, this)
    }
}