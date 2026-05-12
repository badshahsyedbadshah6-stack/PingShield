package com.pingshield.ui

import androidx.lifecycle.ViewModel
import com.pingshield.core.AdaptiveResponseEngine
import com.pingshield.core.EwmaPingEstimator
import com.pingshield.core.JitterAnalyzer
import com.pingshield.core.PacketLossAnalyzer
import com.pingshield.core.PingEngine
import com.pingshield.killer.AppKiller
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val pingEngine: PingEngine,
    private val jitterAnalyzer: JitterAnalyzer,
    private val packetLossAnalyzer: PacketLossAnalyzer,
    private val appKiller: AppKiller,
    private val adaptiveEngine: AdaptiveResponseEngine,
    private val ewmaEstimator: EwmaPingEstimator
) : ViewModel() {
    val ping: StateFlow<Long> = pingEngine.currentPing
    val jitter: StateFlow<Double> = jitterAnalyzer.ipdv
    val loss: StateFlow<Double> = packetLossAnalyzer.lossPercent
    val lossType: StateFlow<String> = packetLossAnalyzer.lossType
    val burstCount: StateFlow<Int> = packetLossAnalyzer.burstCount
    val killedApps: StateFlow<List<String>> = appKiller.killedApps
    val networkScore: StateFlow<Int> = adaptiveEngine.networkScore
    val smoothedPing: StateFlow<Double> = ewmaEstimator.smoothedPing
    val spikeThreshold: StateFlow<Double> = ewmaEstimator.spikeThreshold
}
