package cn.wj.android.cashbook.core.common.model

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

typealias DataVersion = MutableStateFlow<Int>

fun DataVersion.updateVersion() {
    update {
        it + 1
    }
}

fun DataVersion(): MutableStateFlow<Int> = MutableStateFlow(0)