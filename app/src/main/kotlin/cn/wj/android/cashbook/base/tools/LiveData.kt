@file:Suppress("unused")
@file:JvmName("LiveDataTools")

package cn.wj.android.cashbook.base.tools

import androidx.annotation.MainThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer

/** 将多个 [LiveData] [sourceArray] 作为数据源，经过 [mapFunction] 转换后生成新的 [LiveData] 返回 */
@MainThread
inline fun <T> maps(vararg sourceArray: LiveData<*>, crossinline mapFunction: (Int) -> T): LiveData<T> {
    val result: MediatorLiveData<T> = MediatorLiveData<T>()
    sourceArray.forEachIndexed { index, source ->
        result.addSource(source) {
            result.value = mapFunction.invoke(index)
        }
    }
    return result
}

/** 将多个 [LiveData] [sourceArray] 作为数据源，经过 [switchMapFunction] 转换后生成新的 [LiveData] 返回 */
@MainThread
fun <T> switchMaps(
    vararg sourceArray: LiveData<*>,
    switchMapFunction: (Int) -> LiveData<T>
): LiveData<T> {
    val result = MediatorLiveData<T>()
    sourceArray.forEachIndexed { index, source ->
        result.addSource(source, object : Observer<Any> {
            var mSource: LiveData<T>? = null
            override fun onChanged(x: Any) {
                val newLiveData = switchMapFunction.invoke(index)
                if (mSource === newLiveData) {
                    return
                }
                if (mSource != null) {
                    result.removeSource(mSource!!)
                }
                mSource = newLiveData
                if (mSource != null) {
                    result.addSource(mSource!!) { y -> result.value = y }
                }
            }
        })
    }
    return result
}

/** 未设置数据标记 */
val MUTABLE_NO_SET = Any()

/** 获取默认值为 [default]，回调 [onActive]、[onSet] 的可变 [MutableLiveData] 对象 */
inline fun <reified T> mutableLiveDataOf(
    default: Any? = MUTABLE_NO_SET,
    crossinline onActive: MutableLiveData<T>.() -> Unit = {},
    crossinline onSet: MutableLiveData<T>.() -> Unit = {}
): MutableLiveData<T> = if (default == MUTABLE_NO_SET) {
    object : MutableLiveData<T>() {
        override fun onActive() {
            onActive.invoke(this)
        }

        override fun setValue(value: T?) {
            super.setValue(value)
            onSet.invoke(this)
        }
    }
} else {
    object : MutableLiveData<T>(default as? T) {
        override fun onActive() {
            onActive.invoke(this)
        }

        override fun setValue(value: T?) {
            super.setValue(value)
            onSet.invoke(this)
        }
    }
}