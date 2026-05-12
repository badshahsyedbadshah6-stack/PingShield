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
import kotlinx.coroutines.withTimeoutOrNull
import java.net.InetAddress
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PacketLossAnalyzer @Inject constructor() {

    data class LossEvent(val timestamp: Long, val burstLength: Int)

    private val history = ArrayDeque<Boolean>(Constants.LOSS_HISTORY_SIZE)
    private val bursts = mutableListOf<LossEvent>()

    private val _lossPercent = MutableStateFlow(0.0)
    val lossPercent: StateFlow<Double> = _lossPercent.asStateFlow()

    private val _lossType = MutableStateFlow("NONE")
    val lossType: StateFlow<String> = _lossType.asStateFlow()

    private val _burstCount = MutableStateFlow(0)
    val burstCount: StateFlow<Int> = _burstCount.asStateFlow()

    private var scope: CoroutineScope? = null

    fun start() {
        stop()
        scope = CoroutineScope(Dispatchers.IO + Job())
        scope?.launch {
            while (isActive) {
                measureLoss()
                delay(Constants.PACKET_LOSS_INTERVAL_MS)
            }
        }
    }

    private suspend fun measureLoss() {
        val sent = Constants.PACKET_LOSS_TEST_COUNT
        var received = 0
        for (i in 0 until sent) {
            val ok = withTimeoutOrNull(Constants.PACKET_LOSS_TIMEOUT_MS) {
                try {
                    InetAddress.getByName(Constants.DNS_PRIMARY)
                        .isReachable(Constants.PACKET_LOSS_TIMEOUT_MS.toInt())
                } catch (_: Exception) { false }
            } ?: false

            history.addLast(ok)
            if (history.size > Constants.LOSS_HISTORY_SIZE) {
                history.removeFirst()
            }
            if (ok) received++
        }

        val lossPct = if (sent > 0) ((sent - received).toDouble() / sent) * 100.0 else 0.0
        _lossPercent.value = lossPct

        detectBursts()
    }

    private fun detectBursts() {
        val currentBursts = mutableListOf<LossEvent>()
        var consecutive = 0
        for (result in history) {
            if (!result) {
                consecutive++
            } else if (consecutive > 0) {
                currentBursts.add(LossEvent(System.currentTimeMillis(), consecutive))
                consecutive = 0
            }
        }
        if (consecutive > 0) {
            currentBursts.add(LossEvent(System.currentTimeMillis(), consecutive))
        }

        bursts.clear()
        bursts.addAll(currentBursts.takeLast(10))
        _burstCount.value = currentBursts.size

        val avgBurst = if (currentBursts.isNotEmpty())
            currentBursts.map { it.burstLength }.average() else 0.0

        _lossType.value = when {
            avgBurst > 3.0 -> "BURST_LOSS"
            avgBurst > 1.0 -> "RANDOM_LOSS"
            else -> "NONE"
        }
    }

    fun stop() {
        scope?.cancel()
        scope = null
        history.clear()
        bursts.clear()
        _lossPercent.value = 0.0
        _lossType.value = "NONE"
        _burstCount.value = 0
    }
}
