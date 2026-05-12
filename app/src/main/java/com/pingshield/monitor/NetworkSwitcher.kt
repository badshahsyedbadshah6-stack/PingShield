package com.pingshield.monitor

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import com.pingshield.utils.Constants
import com.pingshield.utils.NetworkUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkSwitcher @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _currentNetwork = MutableStateFlow("WiFi")
    val currentNetwork: StateFlow<String> = _currentNetwork.asStateFlow()

    private var mobileNetwork: Network? = null
    private var wifiNetwork: Network? = null
    private var isGameMode = false

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            val caps = getConnectivityManager().getNetworkCapabilities(network)
            when {
                caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> {
                    wifiNetwork = network
                }
                caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> {
                    mobileNetwork = network
                }
            }
        }

        override fun onLost(network: Network) {
            if (network == wifiNetwork) wifiNetwork = null
            if (network == mobileNetwork) mobileNetwork = null
        }
    }

    fun start() {
        val cm = getConnectivityManager()
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        cm.registerNetworkCallback(request, networkCallback)
    }

    fun switchToMobile() {
        if (isGameMode) return
        isGameMode = true

        val mobileNet = mobileNetwork
        if (mobileNet != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val cm = getConnectivityManager()
            cm.bindProcessToNetwork(mobileNet)
            _currentNetwork.value = "Mobile Data"
        }
    }

    fun restoreWifi() {
        if (!isGameMode) return
        isGameMode = false

        val wifiNet = wifiNetwork
        if (wifiNet != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val cm = getConnectivityManager()
            cm.bindProcessToNetwork(wifiNet)
            _currentNetwork.value = "WiFi"
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val cm = getConnectivityManager()
            cm.bindProcessToNetwork(null)
            _currentNetwork.value = "WiFi"
        }
    }

    fun stop() {
        try {
            getConnectivityManager().unregisterNetworkCallback(networkCallback)
        } catch (_: Exception) {}
        wifiNetwork = null
        mobileNetwork = null
        isGameMode = false
        _currentNetwork.value = "WiFi"
    }

    private fun getConnectivityManager(): ConnectivityManager {
        return context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }
}
