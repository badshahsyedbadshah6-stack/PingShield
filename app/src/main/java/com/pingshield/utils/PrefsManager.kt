package com.pingshield.utils

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PrefsManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("pingshield_prefs", Context.MODE_PRIVATE)

    fun getWhitelistedPackages(): Set<String> =
        prefs.getStringSet("whitelist", setOf(Constants.GAME_PACKAGE)) ?: setOf(Constants.GAME_PACKAGE)

    fun addWhitelistPackage(pkg: String) {
        val current = getWhitelistedPackages().toMutableSet()
        current.add(pkg)
        prefs.edit().putStringSet("whitelist", current).apply()
    }

    fun removeWhitelistPackage(pkg: String) {
        val current = getWhitelistedPackages().toMutableSet()
        current.remove(pkg)
        prefs.edit().putStringSet("whitelist", current).apply()
    }

    fun isAutoStartEnabled(): Boolean = prefs.getBoolean("auto_start", false)
    fun setAutoStartEnabled(enabled: Boolean) = prefs.edit().putBoolean("auto_start", enabled).apply()

    fun getPingInterval(): Long = prefs.getLong("ping_interval", Constants.PING_INTERVAL_MS)
    fun setPingInterval(ms: Long) = prefs.edit().putLong("ping_interval", ms).apply()

    fun getSpikeSensitivity(): String = prefs.getString("spike_sensitivity", "Medium") ?: "Medium"
    fun setSpikeSensitivity(level: String) = prefs.edit().putString("spike_sensitivity", level).apply()

    fun getDnsPrimary(): String = prefs.getString("dns_primary", Constants.DNS_PRIMARY) ?: Constants.DNS_PRIMARY
    fun setDnsPrimary(dns: String) = prefs.edit().putString("dns_primary", dns).apply()

    fun isAutoFlushDns(): Boolean = prefs.getBoolean("auto_flush_dns", true)
    fun setAutoFlushDns(enabled: Boolean) = prefs.edit().putBoolean("auto_flush_dns", enabled).apply()

    fun isPreResolveEnabled(): Boolean = prefs.getBoolean("pre_resolve", true)
    fun setPreResolveEnabled(enabled: Boolean) = prefs.edit().putBoolean("pre_resolve", enabled).apply()

    fun isWifiLowLatencyEnabled(): Boolean = prefs.getBoolean("wifi_low_latency", true)
    fun setWifiLowLatencyEnabled(enabled: Boolean) = prefs.edit().putBoolean("wifi_low_latency", enabled).apply()

    fun isWakeLockEnabled(): Boolean = prefs.getBoolean("wake_lock", true)
    fun setWakeLockEnabled(enabled: Boolean) = prefs.edit().putBoolean("wake_lock", enabled).apply()

    fun isPerformanceModeEnabled(): Boolean = prefs.getBoolean("performance_mode", true)
    fun setPerformanceModeEnabled(enabled: Boolean) = prefs.edit().putBoolean("performance_mode", enabled).apply()

    fun isAutoSwitchMobile(): Boolean = prefs.getBoolean("auto_switch_mobile", false)
    fun setAutoSwitchMobile(enabled: Boolean) = prefs.edit().putBoolean("auto_switch_mobile", enabled).apply()

    fun isDscpMarkingEnabled(): Boolean = prefs.getBoolean("dscp_marking", true)
    fun setDscpMarkingEnabled(enabled: Boolean) = prefs.edit().putBoolean("dscp_marking", enabled).apply()

    fun isTcpPshForcingEnabled(): Boolean = prefs.getBoolean("tcp_psh", true)
    fun setTcpPshForcingEnabled(enabled: Boolean) = prefs.edit().putBoolean("tcp_psh", enabled).apply()

    fun getLossReconnectThreshold(): Float = prefs.getFloat("loss_reconnect", 2.0f)
    fun setLossReconnectThreshold(pct: Float) = prefs.edit().putFloat("loss_reconnect", pct).apply()

    fun isAutoReconnectBurst(): Boolean = prefs.getBoolean("auto_reconnect_burst", true)
    fun setAutoReconnectBurst(enabled: Boolean) = prefs.edit().putBoolean("auto_reconnect_burst", enabled).apply()

    fun showPingInNotification(): Boolean = prefs.getBoolean("notif_ping", true)
    fun setShowPingInNotification(enabled: Boolean) = prefs.edit().putBoolean("notif_ping", enabled).apply()

    fun showSpikeAlerts(): Boolean = prefs.getBoolean("notif_spike", true)
    fun setShowSpikeAlerts(enabled: Boolean) = prefs.edit().putBoolean("notif_spike", enabled).apply()

    fun showScoreInNotification(): Boolean = prefs.getBoolean("notif_score", true)
    fun setShowScoreInNotification(enabled: Boolean) = prefs.edit().putBoolean("notif_score", enabled).apply()

    fun killOnLaunch(pkg: String): Boolean = prefs.getBoolean("kill_launch_$pkg", true)
    fun setKillOnLaunch(pkg: String, enabled: Boolean) = prefs.edit().putBoolean("kill_launch_$pkg", enabled).apply()

    fun killOnSpike(pkg: String): Boolean = prefs.getBoolean("kill_spike_$pkg", true)
    fun setKillOnSpike(pkg: String, enabled: Boolean) = prefs.edit().putBoolean("kill_spike_$pkg", enabled).apply()
}
