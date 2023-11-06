package cn.wj.android.cashbook.core.common.ext

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first

suspend fun <T> MutableSharedFlow<T>.tryEmitNoRepeat(value: T): Boolean {
    if (first() == value) {
        return false
    }
    return tryEmit(value)
}