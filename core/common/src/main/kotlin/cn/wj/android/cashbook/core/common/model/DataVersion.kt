package cn.wj.android.cashbook.core.common.model

import cn.wj.android.cashbook.core.common.ext.logger
import kotlinx.coroutines.flow.MutableStateFlow

typealias DataVersion = MutableStateFlow<Int>

fun DataVersion.updateVersion() {
    val oldValue = value
    val newValue = oldValue + 1
    val result = tryEmit(newValue)
    logger().i("updateVersion() oldValue = <$oldValue>, newValue = <$newValue>, result = <$result>")
}

private fun dataVersion(): MutableStateFlow<Int> = MutableStateFlow(0)

val recordDataVersion: DataVersion = dataVersion()
val assetDataVersion: DataVersion = dataVersion()
val bookDataVersion: DataVersion = dataVersion()
val typeDataVersion: DataVersion = dataVersion()
val tagDataVersion: DataVersion = dataVersion()