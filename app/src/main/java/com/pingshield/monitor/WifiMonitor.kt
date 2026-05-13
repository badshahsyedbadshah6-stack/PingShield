package com.pingshield.monitor

import android.content.Context
import android.net.wifi.WifiManager
import android.os.PowerManager
import com.pingshield.utils.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WifiMonitor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _rssi = MutableStateFlow(Int.MIN_VALUE)
    val rssi: StateFlow<Int> = _rssi.asStateFlow()

    private val _linkSpeed = MutableStateFlow(0)
    val linkSpeed: StateFlow<Int> = _linkSpeed.asStateFlow()

    private val _frequency = MutableStateFlow(0)
    val frequency: StateFlow<Int> = _frequency.asStateFlow()

    private val _wifiWarning = MutableStateFlow("")
    val wifiWarning: StateFlow<String> = _wifiWarning.asStateFlow()

    val warningEvent = MutableSharedFlow<Int>(extraBufferCapacity = 1)
    val criticalEvent = MutableSharedFlow<Int>(extraBufferCapacity = 1)

    private var scope: CoroutineScope? = null
    private var wifiLock: WifiManager.WifiLock? = null
    private var wakeLock: PowerManager.WakeLock? = null

    fun start() {
        stop()
        scope = CoroutineScope(Dispatchers.Default + Job())

        val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        val pm = context.applicationContext.getSystemService(Context.POWER_SERVICE) as? PowerManager

        wifiLock = wm?.createWifiLock(WifiManager.WIFI_MODE_FULL_LOW_LATENCY, "PingShield:GameLock")
        wifiLock?.acquire()

        wakeLock = pm?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "PingShield:CpuLock")
        wakeLock?.acquire(Constants.WAKELOCK_TIMEOUT_MS)

        scope?.launch {
            while (isActive) {
                val currentWm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                val info = currentWm?.connectionInfo
                if (info != null) {
                    val currentRssi = info.rssi
                    _rssi.value = currentRssi
                    _linkSpeed.value = info.linkSpeed
                    _frequency.value = info.frequency
                    _wifiWarning.value = when {
                        currentRssi < Constants.RSSI_CRITICAL_THRESHOLD -> "Critical signal"
                        currentRssi < Constants.RSSI_WARNING_THRESHOLD -> "Weak signal"
                        else -> "Good signal"
                    }
                    if (currentRssi < Constants.RSSI_CRITICAL_THRESHOLD) {
                        criticalEvent.tryEmit(currentRssi)
                    } else if (currentRssi < Constants.RSSI_WARNING_THRESHOLD) {
                        warningEvent.tryEmit(currentRssi)
                    }
                }
                delay(Constants.SCAN_INTERVAL_MS)
            }
        }
    }

    fun stop() {
        scope?.cancel()
        scope = null
        try { wifiLock?.release() } catch (_: Exception) {}
        wifiLock = null
        try { wakeLock?.release() } catch (_: Exception) {}
        wakeLock = null
        _rssi.value = Int.MIN_VALUE
        _wifiWarning.value = ""
    }
}
