package com.pingshield.core

import com.pingshield.utils.Constants
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
import java.net.InetSocketAddress
import java.net.Socket
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PingEngine @Inject constructor() {

    private val _currentPing = MutableStateFlow(0)
    val currentPing: StateFlow<Int> = _currentPing.asStateFlow()

    private val _spikeDetected = MutableStateFlow(false)
    val spikeDetected: StateFlow<Boolean> = _spikeDetected.asStateFlow()

    private val rollingPings = mutableListOf<Long>()
    private var scope: CoroutineScope? = null
    private var job: Job? = null

    fun start() {
        stop()
        rollingPings.clear()
        _currentPing.value = 0
        _spikeDetected.value = false

        scope = CoroutineScope(Dispatchers.IO + Job())
        job = scope?.launch {
            while (isActive) {
                val pingMs = measurePing()
                if (pingMs >= 0) {
                    rollingPings.add(pingMs)
                    if (rollingPings.size > Constants.ROLLING_PING_SIZE) {
                        rollingPings.removeAt(0)
                    }
                    _currentPing.value = pingMs.toInt()

                    if (rollingPings.size >= 3) {
                        val avg = rollingPings.average()
                        if (pingMs > avg + Constants.SPIKE_THRESHOLD_MS) {
                            _spikeDetected.value = true
                        } else {
                            _spikeDetected.value = false
                        }
                    }
                }
                delay(Constants.PING_INTERVAL_MS)
            }
        }
    }

    fun getRollingAverage(): Double {
        if (rollingPings.isEmpty()) return 0.0
        return rollingPings.average()
    }

    fun getHistory(): List<Long> = rollingPings.toList()

    fun stop() {
        job?.cancel()
        scope?.cancel()
        scope = null
        job = null
        rollingPings.clear()
        _currentPing.value = 0
        _spikeDetected.value = false
    }

    private fun measurePing(): Long {
        return try {
            val socket = Socket()
            val start = System.nanoTime()
            socket.connect(InetSocketAddress(Constants.PING_TARGET, Constants.PING_PORT), 1000)
            val end = System.nanoTime()
            socket.close()
            (end - start) / 1_000_000
        } catch (e: Exception) {
            -1L
        }
    }
}
