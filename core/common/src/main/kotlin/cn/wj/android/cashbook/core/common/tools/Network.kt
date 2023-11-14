/*
 * Copyright 2021 The Cashbook Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
            // 移动数据
            return if (mWiFiNetworkInfo.type == ConnectivityManager.TYPE_WIFI) {
                // WIFI
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
                // 移动数据
                return if (nc.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    // WIFI
                    true
                } else {
                    nc.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                }
            }
        }
    }
    return false
}
