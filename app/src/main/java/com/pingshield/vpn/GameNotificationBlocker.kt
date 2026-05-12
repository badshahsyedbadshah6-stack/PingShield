package com.pingshield.vpn

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.pingshield.utils.Constants

class GameNotificationBlocker : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (isActive && shouldBlock(sbn)) {
            cancelNotification(sbn.key)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {}

    override fun onListenerConnected() {
        super.onListenerConnected()
        if (isActive) {
            activeNotifications?.forEach { sbn ->
                if (shouldBlock(sbn)) cancelNotification(sbn.key)
            }
        }
    }

    private fun shouldBlock(sbn: StatusBarNotification): Boolean {
        val pkg = sbn.packageName
        return pkg != Constants.GAME_PACKAGE &&
               pkg != "com.android.systemui" &&
               pkg != "android" &&
               !pkg.startsWith("com.pingshield")
    }

    companion object {
        @Volatile var isActive = false
    }
}
