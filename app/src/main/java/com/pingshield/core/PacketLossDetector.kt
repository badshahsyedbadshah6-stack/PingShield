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
class PacketLossDetector @Inject constructor() {

    private val _lossPercentage = MutableStateFlow(0.0)
    val lossPercentage: StateFlow<Double> = _lossPercentage.asStateFlow()

    private var scope: CoroutineScope? = null
    private var job: Job? = null

    fun start() {
        stop()
        scope = CoroutineScope(Dispatchers.IO + Job())
        job = scope?.launch {
            while (isActive) {
                val loss = measureLoss()
                _lossPercentage.value = loss
                delay(Constants.PACKET_LOSS_INTERVAL_MS)
            }
        }
    }

    fun stop() {
        job?.cancel()
        scope?.cancel()
        scope = null
        job = null
        _lossPercentage.value = 0.0
    }

    private suspend fun measureLoss(): Double {
        var received = 0
        val sent = Constants.PACKET_LOSS_TEST_COUNT
        for (i in 0 until sent) {
            val success = withTimeoutOrNull(Constants.PACKET_LOSS_TIMEOUT_MS) {
                try {
                    val addr = InetAddress.getByName(Constants.DNS_PRIMARY)
                    addr.isReachable(Constants.PACKET_LOSS_TIMEOUT_MS.toInt())
                } catch (e: Exception) {
                    false
                }
            } ?: false
            if (success) received++
        }
        return if (sent > 0) {
            ((sent - received).toDouble() / sent) * 100.0
        } else 0.0
    }
}
