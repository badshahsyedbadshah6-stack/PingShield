package com.pingshield.monitor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
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

    val warningEvent = MutableSharedFlow<Int>(extraBufferCapacity = 1)
    val criticalEvent = MutableSharedFlow<Int>(extraBufferCapacity = 1)

    private var scope: CoroutineScope? = null
    private var job: Job? = null
    private var receiver: BroadcastReceiver? = null

    fun start() {
        stop()
        scope = CoroutineScope(Dispatchers.Default + Job())

        val filter = IntentFilter(WifiManager.RSSI_CHANGED_ACTION)
        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val rssiValue = intent.getIntExtra(
                    WifiManager.EXTRA_NEW_RSSI,
                    Int.MIN_VALUE
                )
                if (rssiValue != Int.MIN_VALUE) {
                    updateRssi(rssiValue)
                }
            }
        }
        context.registerReceiver(receiver, filter)

        job = scope?.launch {
            while (isActive) {
                val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                val currentRssi = wm?.connectionInfo?.rssi ?: Int.MIN_VALUE
                if (currentRssi != Int.MIN_VALUE) {
                    updateRssi(currentRssi)
                }
                delay(Constants.SCAN_INTERVAL_MS)
            }
        }
    }

    private suspend fun updateRssi(value: Int) {
        _rssi.value = value
        if (value < Constants.RSSI_CRITICAL_THRESHOLD) {
            criticalEvent.emit(value)
        } else if (value < Constants.RSSI_WARNING_THRESHOLD) {
            warningEvent.emit(value)
        }
    }

    fun stop() {
        job?.cancel()
        scope?.cancel()
        scope = null
        job = null
        try {
            receiver?.let { context.unregisterReceiver(it) }
        } catch (_: Exception) {}
        receiver = null
        _rssi.value = Int.MIN_VALUE
    }
}
