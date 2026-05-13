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
import java.io.BufferedReader
import java.io.InputStreamReader
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
    private var hasIcmp = false

    init {
        try {
            val process = Runtime.getRuntime().exec("/system/bin/ping -c 1 -w 1 1.1.1.1")
            val exitCode = process.waitFor()
            hasIcmp = exitCode == 0 || exitCode == 1
        } catch (_: Exception) {
            hasIcmp = false
        }
    }

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
        if (hasIcmp) {
            val icmpResult = icmpPing(host)
            if (icmpResult >= 0) return icmpResult
        }
        return tcpPing(host, port)
    }

    private fun icmpPing(host: String): Long {
        return try {
            val start = System.nanoTime()
            val process = Runtime.getRuntime().exec("/system/bin/ping -c 1 -W 1 -n $host")
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val line = reader.readLine()
            process.waitFor()
            if (line != null && line.contains("time=")) {
                val timeStr = line.substringAfter("time=").substringBefore(" ms").substringBefore(" ")
                val ms = timeStr.toDoubleOrNull()
                if (ms != null) {
                    val elapsed = (System.nanoTime() - start) / 1_000_000
                    ms.toLong().coerceAtMost(elapsed)
                } else -1L
            } else -1L
        } catch (e: Exception) {
            -1L
        }
    }

    private fun tcpPing(host: String, port: Int): Long {
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
