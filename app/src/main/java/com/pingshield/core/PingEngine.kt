package com.pingshield.core

import com.pingshield.utils.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
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
class PingEngine @Inject constructor(
    private val ewmaEstimator: EwmaPingEstimator,
    private val jitterAnalyzer: JitterAnalyzer
) {
    private val _currentPing = MutableStateFlow(0L)
    val currentPing: StateFlow<Long> = _currentPing.asStateFlow()

    private val _isSpike = MutableStateFlow(false)
    val isSpike: StateFlow<Boolean> = _isSpike.asStateFlow()

    private val _pingHistory = MutableStateFlow<List<Long>>(emptyList())
    val pingHistory: StateFlow<List<Long>> = _pingHistory.asStateFlow()

    private val history = mutableListOf<Long>()
    private var scope: CoroutineScope? = null

    fun start() {
        stop()
        history.clear()
        _currentPing.value = 0L
        _isSpike.value = false

        scope = CoroutineScope(Dispatchers.IO + Job())
        scope?.launch {
            while (isActive) {
                val deferred = Constants.PING_TARGETS.map { target ->
                    async { measureTarget(target.host, target.port) }
                }
                val results = deferred.map { it.await() }
                val validResults = results.filter { it >= 0 }
                if (validResults.isNotEmpty()) {
                    val minPing = validResults.min()
                    _currentPing.value = minPing

                    history.add(minPing)
                    if (history.size > Constants.GRAPH_SIZE) {
                        history.removeAt(0)
                    }
                    _pingHistory.value = history.toList()

                    val minDouble = minPing.toDouble()
                    ewmaEstimator.update(minDouble)
                    jitterAnalyzer.addSample(minDouble)
                    _isSpike.value = ewmaEstimator.isSpike(minDouble)
                }
                kotlinx.coroutines.delay(Constants.PING_INTERVAL_MS)
            }
        }
    }

    private fun measureTarget(host: String, port: Int): Long {
        return try {
            val socket = Socket()
            val start = System.nanoTime()
            socket.connect(InetSocketAddress(host, port), 500)
            val end = System.nanoTime()
            socket.close()
            (end - start) / 1_000_000
        } catch (e: Exception) {
            -1L
        }
    }

    fun stop() {
        scope?.cancel()
        scope = null
        history.clear()
        _pingHistory.value = emptyList()
        _currentPing.value = 0L
        _isSpike.value = false
        ewmaEstimator.reset()
        jitterAnalyzer.reset()
    }
}
