package com.pingshield.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager

object NetworkUtils {

    fun getActiveNetworkInfo(context: Context): ConnectivityManager? {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        return cm
    }

    fun isWifiConnected(context: Context): Boolean {
        val cm = getActiveNetworkInfo(context) ?: return false
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    fun isMobileDataConnected(context: Context): Boolean {
        val cm = getActiveNetworkInfo(context) ?: return false
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
    }

    fun getWifiRssi(context: Context): Int {
        val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        val info = wm?.connectionInfo ?: return Int.MIN_VALUE
        return info.rssi
    }

    fun isVpnActive(context: Context): Boolean {
        val cm = getActiveNetworkInfo(context) ?: return false
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
    }
}
