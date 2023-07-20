package cn.wj.android.cashbook.core.data.uitl

import kotlinx.coroutines.flow.Flow

interface WebDAVManager {

    val isConnected: Flow<Boolean>

    fun requestConnected()
}