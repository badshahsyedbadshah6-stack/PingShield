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
    private var job: Job? = null

    fun start() {
        stop()
        scope = CoroutineScope(Dispatchers.IO + Job())

        var lastRxBytes = TrafficStats.getTotalRxBytes()
        var lastTxBytes = TrafficStats.getTotalTxBytes()
        var lastTime = System.currentTimeMillis()

        job = scope?.launch {
            while (isActive) {
                val currentRx = TrafficStats.getTotalRxBytes()
                val currentTx = TrafficStats.getTotalTxBytes()
                val currentTime = System.currentTimeMillis()

                val elapsed = currentTime - lastTime
                if (elapsed > 0) {
                    val rxSpeed = ((currentRx - lastRxBytes) * 1000) / elapsed
                    val txSpeed = ((currentTx - lastTxBytes) * 1000) / elapsed
                    _downloadSpeed.value = rxSpeed
                    _uploadSpeed.value = txSpeed
                }

                lastRxBytes = currentRx
                lastTxBytes = currentTx
                lastTime = currentTime

                delay(1000)
            }
        }
    }

    fun stop() {
        job?.cancel()
        scope?.cancel()
        scope = null
        job = null
        _downloadSpeed.value = 0L
        _uploadSpeed.value = 0L
    }
}
