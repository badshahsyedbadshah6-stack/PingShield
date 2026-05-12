package com.pingshield.monitor

import android.net.TrafficStats
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BandwidthMonitor @Inject constructor() {
    private val _downloadSpeed = MutableStateFlow(0L)
    val downloadSpeed: StateFlow<Long> = _downloadSpeed.asStateFlow()
    private val _uploadSpeed = MutableStateFlow(0L)
    val uploadSpeed: StateFlow<Long> = _uploadSpeed.asStateFlow()
    private var scope: CoroutineScope? = null

    fun start() {
        stop()
        scope = CoroutineScope(Dispatchers.IO + Job())
        var lastRx = TrafficStats.getTotalRxBytes()
        var lastTx = TrafficStats.getTotalTxBytes()
        var lastTime = System.currentTimeMillis()
        scope?.launch {
            while (isActive) {
                val rx = TrafficStats.getTotalRxBytes()
                val tx = TrafficStats.getTotalTxBytes()
                val now = System.currentTimeMillis()
                val elapsed = now - lastTime
                if (elapsed > 0) {
                    _downloadSpeed.value = ((rx - lastRx) * 1000) / elapsed
                    _uploadSpeed.value = ((tx - lastTx) * 1000) / elapsed
                }
                lastRx = rx; lastTx = tx; lastTime = now
                delay(1000)
            }
        }
    }

    fun stop() {
        scope?.cancel(); scope = null
        _downloadSpeed.value = 0L; _uploadSpeed.value = 0L
    }
}
