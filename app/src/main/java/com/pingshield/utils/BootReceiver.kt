package com.pingshield.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.pingshield.vpn.PingShieldVpn
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var prefsManager: PrefsManager

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED && prefsManager.isAutoStartEnabled()) {
            val vpnIntent = PingShieldVpn.prepare(context)
            if (vpnIntent == null) {
                PingShieldVpn.startVpn(context)
            }
        }
    }
}
