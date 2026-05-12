package com.pingshield

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.pingshield.utils.Constants
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class PingShieldApp : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        val manager = getSystemService(NotificationManager::class.java)

        val vpnChannel = NotificationChannel(
            VPN_CHANNEL_ID,
            "PingShield VPN Service",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "PingShield VPN foreground service"
            setShowBadge(false)
        }
        manager.createNotificationChannel(vpnChannel)

        val liveChannel = NotificationChannel(
            Constants.NOTIF_CHANNEL_LIVE,
            Constants.NOTIF_CHANNEL_LIVE_NAME,
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Live ping, jitter and score data"
            setShowBadge(false)
        }
        manager.createNotificationChannel(liveChannel)

        val blockerChannel = NotificationChannel(
            Constants.NOTIF_CHANNEL_BLOCKER,
            Constants.NOTIF_CHANNEL_BLOCKER_NAME,
            NotificationManager.IMPORTANCE_MIN
        ).apply {
            description = "Notification blocker service"
            setShowBadge(false)
        }
        manager.createNotificationChannel(blockerChannel)
    }

    companion object {
        const val VPN_CHANNEL_ID = "pingshield_vpn"
    }
}
