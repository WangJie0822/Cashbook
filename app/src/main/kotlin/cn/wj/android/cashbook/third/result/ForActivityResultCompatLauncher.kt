@file:Suppress("unused")

package cn.wj.android.cashbook.third.result

import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.app.ActivityOptionsCompat

/**
 * For Activity Result API 封装
 *
 * @param caller 调用者，[ActivityResultCaller] 对象
 * @param contract 功能协议
 * @param callback Result 回调
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/8/5
 */
class ForActivityResultCompatLauncher<I, O>(
    caller: ActivityResultCaller,
    contract: ActivityResultContract<I, O>,
    callback: ActivityResultCallback<O>?
) : ActivityResultLauncher<I>() {

    /**
     * 兼容 Kotlin lambda 构造方法
     *
     * @param caller 调用者，[ActivityResultCaller] 对象
     * @param contract 功能协议
     * @param callback Result 回调
     */
    constructor(
        caller: ActivityResultCaller,
        contract: ActivityResultContract<I, O>,
        callback: ((O) -> Unit)? = null
    ) : this(caller, contract, if (null == callback) null else ActivityResultCallback { callback.invoke(it) })

    /** 内部回调，用于缓存回调 */
    private var internalCallback: ActivityResultCallback<O>? = callback

    /** 实际调用 launcher 对象 */
    private val launcher = caller.registerForActivityResult(contract) {
        internalCallback?.onActivityResult(it)
    }

    override fun unregister() {
        launcher.unregister()
    }

    override fun getContract(): ActivityResultContract<I, *> {
        return launcher.contract
    }

    override fun launch(input: I, options: ActivityOptionsCompat?) {
        launcher.launch(input, options)
    }

    /** 使用数据 [input] 发起请求并回调到 [callback] */
    fun launch(input: I, callback: ActivityResultCallback<O>) {
        internalCallback = callback
        launch(input)
    }

    /** 使用数据 [input] 发起请求并回调到 [callback] */
    fun launch(input: I, options: ActivityOptionsCompat?, callback: ActivityResultCallback<O>) {
        internalCallback = callback
        launch(input, options)
    }
}

fun <I, O> ActivityResultCaller.createForActivityResultLauncher(
    contract: ActivityResultContract<I, O>,
    callback: ActivityResultCallback<O>?
): ForActivityResultCompatLauncher<I, O> {
    return ForActivityResultCompatLauncher(this, contract, callback)
}

fun <I, O> ActivityResultCaller.createForActivityResultLauncher(
    contract: ActivityResultContract<I, O>,
    callback: ((O) -> Unit)? = null
): ForActivityResultCompatLauncher<I, O> {
    return ForActivityResultCompatLauncher(this, contract, callback)
}