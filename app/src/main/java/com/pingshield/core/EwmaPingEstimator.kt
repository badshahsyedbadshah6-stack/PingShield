package com.pingshield.core

import com.pingshield.utils.Constants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EwmaPingEstimator @Inject constructor() {

    private var alpha = Constants.EWMA_ALPHA
    private var beta = Constants.EWMA_BETA
    private var srtt = 0.0
    private var rttvar = 0.0
    private var hasFirstSample = false

    private val _smoothedPing = MutableStateFlow(0.0)
    val smoothedPing: StateFlow<Double> = _smoothedPing.asStateFlow()

    private val _spikeThreshold = MutableStateFlow(0.0)
    val spikeThreshold: StateFlow<Double> = _spikeThreshold.asStateFlow()

    fun setAlpha(newAlpha: Double) {
        alpha = newAlpha
    }

    fun update(measuredRtt: Double) {
        if (!hasFirstSample) {
            srtt = measuredRtt
            rttvar = measuredRtt / 2.0
            hasFirstSample = true
        } else {
            rttvar = (1.0 - beta) * rttvar + beta * kotlin.math.abs(srtt - measuredRtt)
            srtt = (1.0 - alpha) * srtt + alpha * measuredRtt
        }
        _smoothedPing.value = srtt
        _spikeThreshold.value = srtt + 4.0 * rttvar
    }

    fun isSpike(measured: Double): Boolean {
        if (!hasFirstSample) return false
        return measured > srtt + 4.0 * rttvar
    }

    fun reset() {
        srtt = 0.0
        rttvar = 0.0
        hasFirstSample = false
        _smoothedPing.value = 0.0
        _spikeThreshold.value = 0.0
    }
}
