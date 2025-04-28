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

package cn.wj.android.cashbook.core.data.uitl.impl

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.core.content.getSystemService
import cn.wj.android.cashbook.core.common.ext.logger
import cn.wj.android.cashbook.core.data.uitl.NetworkMonitor
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import javax.inject.Inject

/**
 * 网络状态监听
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/6/15
 */
class ConnectivityManagerNetworkMonitor @Inject constructor(
    @ApplicationContext private val context: Context,
) : NetworkMonitor {

    private val _isWifi = MutableStateFlow(false)

    override val isOnline: Flow<Boolean> = callbackFlow {
        val connectivityManager = context.getSystemService<ConnectivityManager>()
        if (connectivityManager == null) {
            this@ConnectivityManagerNetworkMonitor.logger()
                .i("isOnline, getConnectivityManager false")
            channel.trySend(false)
            _isWifi.tryEmit(false)
            channel.close()
            return@callbackFlow
        }

        /**
         * The callback's methods are invoked on changes to *any* network matching the [NetworkRequest],
         * not just the active network. So we can simply track the presence (or absence) of such [Network].
         */
        val callback = object : ConnectivityManager.NetworkCallback() {

            private val networks = mutableSetOf<Network>()

            override fun onAvailable(network: Network) {
                this@ConnectivityManagerNetworkMonitor.logger()
                    .i("isOnline, onAvailable(network = <$network>)")
                networks += network
                channel.trySend(true)
            }

            override fun onLost(network: Network) {
                networks -= network
                val hasNetwork = networks.isNotEmpty()
                channel.trySend(hasNetwork)
                this@ConnectivityManagerNetworkMonitor.logger()
                    .i("isOnline, onLost(network = <$network>), hasNetwork = <$hasNetwork>")
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities,
            ) {
                this@ConnectivityManagerNetworkMonitor.logger()
                    .i("isOnline, onCapabilitiesChanged(network = <$network>, networkCapabilities = <$networkCapabilities>)")
                if (networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) {
                    if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                        this@ConnectivityManagerNetworkMonitor.logger()
                            .i("isOnline, onCapabilitiesChanged(network, networkCapabilities), WIFI connected")
                        _isWifi.tryEmit(true)
                    } else {
                        this@ConnectivityManagerNetworkMonitor.logger()
                            .i("isOnline, onCapabilitiesChanged(network, networkCapabilities), WIFI disconnected")
                        _isWifi.tryEmit(false)
                    }
                }
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(request, callback)

        /**
         * Sends the latest connectivity status to the underlying channel.
         */
        val currentlyConnected = connectivityManager.isCurrentlyConnected()
        val currentlyIsWifi = connectivityManager.isCurrentlyWifi()
        channel.trySend(currentlyConnected)
        _isWifi.tryEmit(currentlyIsWifi)
        this@ConnectivityManagerNetworkMonitor.logger()
            .i("currently network state, isConnected = <$currentlyConnected>, isWifi = <$currentlyIsWifi>")

        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }
        .conflate()

    override val isWifi: Flow<Boolean> = _isWifi

    private fun ConnectivityManager.isCurrentlyConnected() = activeNetwork
        ?.let(::getNetworkCapabilities)
        ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true

    private fun ConnectivityManager.isCurrentlyWifi() = activeNetwork
        ?.let(::getNetworkCapabilities)
        ?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
}
