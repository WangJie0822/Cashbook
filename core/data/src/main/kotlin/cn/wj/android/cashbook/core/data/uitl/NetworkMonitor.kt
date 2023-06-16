package cn.wj.android.cashbook.core.data.uitl

import kotlinx.coroutines.flow.Flow


/**
 * 网络状态监听
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/6/15
 */
interface NetworkMonitor {

    val isOnline: Flow<Boolean>

    val isWifi: Flow<Boolean>
}