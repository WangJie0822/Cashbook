@file:Suppress("unused")
@file:JvmName("NetworkTools")

package cn.wj.android.cashbook.core.common.tools

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.annotation.RequiresPermission


/** 通过 [context] 对象获取并返回 Wi-Fi 是否可用 */
@Suppress("DEPRECATION")
@RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
fun isWifiAvailable(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val wifiNi = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI) ?: return false
    return wifiNi.isConnected && wifiNi.isAvailable
}

/** 通过 [context] 对话判断网络是否可用 */
@SuppressLint("ObsoleteSdkInt")
@Suppress("DEPRECATION")
@RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
fun isNetAvailable(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    if (Build.VERSION.SDK_INT < 23) {
        val mWiFiNetworkInfo = cm.activeNetworkInfo
        if (mWiFiNetworkInfo != null) {
            //移动数据
            return if (mWiFiNetworkInfo.type == ConnectivityManager.TYPE_WIFI) {
                //WIFI
                true
            } else {
                mWiFiNetworkInfo.type == ConnectivityManager.TYPE_MOBILE
            }
        }
    } else {
        val network = cm.activeNetwork
        if (network != null) {
            val nc = cm.getNetworkCapabilities(network)
            if (nc != null) {
                //移动数据
                return if (nc.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    //WIFI
                    true
                } else {
                    nc.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                }
            }
        }
    }
    return false
}