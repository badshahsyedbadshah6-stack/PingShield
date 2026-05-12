package com.pingshield

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class PingShieldApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            VPN_CHANNEL_ID,
            "PingShield VPN Service",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "PingShield VPN foreground service notification"
            setShowBadge(false)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val VPN_CHANNEL_ID = "pingshield_vpn_channel"
    }
}
