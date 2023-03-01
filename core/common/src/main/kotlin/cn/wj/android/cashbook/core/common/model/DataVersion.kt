package cn.wj.android.cashbook.core.common.model

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class DataVersion {

    val version = MutableStateFlow(0)

    inline fun <R> map(crossinline transform: suspend (value: Int) -> R): Flow<R> {
        return version.map(transform)
    }

    fun update() {
        version.value = ++version.value
    }
}