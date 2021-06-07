@file:Suppress("unused")
@file:JvmName("LiveDataTools")

package cn.wj.android.cashbook.base.tools

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData

inline fun <T> maps(vararg liveDataArray: LiveData<*>, crossinline mapFunction: (Int) -> T): LiveData<T> {
    val result: MediatorLiveData<T> = MediatorLiveData<T>()
    liveDataArray.forEachIndexed { index, source ->
        result.addSource(source) {
            result.value = mapFunction.invoke(index)
        }
    }
    return result
}