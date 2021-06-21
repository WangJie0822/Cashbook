@file:Suppress("unused")
@file:JvmName("NetworkTools")

package cn.wj.android.cashbook.base.tools

import android.content.Context
import android.net.ConnectivityManager
import cn.wj.android.cashbook.manager.AppManager


/** 通过 [context] 对象获取并返回 Wi-Fi 是否可用 */
@Suppress("DEPRECATION")
fun isWifiAvailable(context: Context = AppManager.getContext()): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val wifiNi = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI) ?: return false
    return wifiNi.isConnected && wifiNi.isAvailable
}