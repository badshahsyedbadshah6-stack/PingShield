package com.pingshield.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pingshield.core.DnsManager
import com.pingshield.core.PacketLossDetector
import com.pingshield.core.PingEngine
import com.pingshield.core.StabilityEngine
import com.pingshield.monitor.WifiMonitor
import com.pingshield.utils.Constants
import com.pingshield.utils.NetworkUtils
import com.pingshield.vpn.TrafficController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val pingEngine: PingEngine,
    private val stabilityEngine: StabilityEngine,
    private val dnsManager: DnsManager,
    private val packetLossDetector: PacketLossDetector,
    private val wifiMonitor: WifiMonitor,
    private val trafficController: TrafficController
) : ViewModel() {

    val ping: StateFlow<Int> = pingEngine.currentPing
    val spike: StateFlow<Boolean> = pingEngine.spikeDetected
    val currentAction: StateFlow<String> = stabilityEngine.currentAction
    val dns: StateFlow<String> = dnsManager.currentDns
    val loss: StateFlow<Double> = packetLossDetector.lossPercentage
    val rssi: StateFlow<Int> = wifiMonitor.rssi
    val blockedCount: StateFlow<Int> = trafficController.blockedAppCount

    private val _isVpnActive = MutableStateFlow(false)
    val isVpnActive: StateFlow<Boolean> = _isVpnActive.asStateFlow()

    val graphData = mutableListOf<Float>()

    private var graphJob: Job? = null

    init {
        viewModelScope.launch {
            pingEngine.currentPing.collect { value ->
                if (value > 0) {
                    graphData.add(value.toFloat())
                    if (graphData.size > Constants.GRAPH_PING_SIZE) {
                        graphData.removeAt(0)
                    }
                }
            }
        }
    }

    fun startGameMode() {
        _isVpnActive.value = true
        viewModelScope.launch(Dispatchers.IO) {
            delay(500)
            stabilityEngine.start()
        }

        graphJob = viewModelScope.launch {
            while (isActive) {
                delay(Constants.PING_INTERVAL_MS)
            }
        }
    }

    fun stopGameMode() {
        _isVpnActive.value = false
        graphData.clear()
        graphJob?.cancel()
        stabilityEngine.stop()
    }

    override fun onCleared() {
        super.onCleared()
        stabilityEngine.stop()
        graphJob?.cancel()
    }
}
