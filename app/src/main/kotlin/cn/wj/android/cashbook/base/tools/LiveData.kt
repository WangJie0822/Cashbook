@file:Suppress("unused")
@file:JvmName("LiveDataTools")

package cn.wj.android.cashbook.base.tools

import androidx.annotation.MainThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Observer

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