package cn.wj.android.cashbook.core.common.model

import cn.wj.android.cashbook.core.common.ext.logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

typealias DataVersion = MutableStateFlow<Int>

fun StateFlow<Int>.updateVersion() {
    if (this is DataVersion) {
        val oldValue = value
        val newValue = oldValue + 1
        val result = tryEmit(newValue)
        logger().i("updateVersion() oldValue = <$oldValue>, newValue = <$newValue>, result = <$result>")
    }
}

private fun dataVersion(): MutableStateFlow<Int> = MutableStateFlow(0)

val recordDataVersion: StateFlow<Int> = dataVersion()
val assetDataVersion: StateFlow<Int> = dataVersion()
val bookDataVersion: StateFlow<Int> = dataVersion()
val typeDataVersion: StateFlow<Int> = dataVersion()
val tagDataVersion: StateFlow<Int> = dataVersion()