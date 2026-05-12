package com.pingshield.ui

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pingshield.core.AdaptiveResponseEngine
import com.pingshield.core.DnsManager
import com.pingshield.core.EwmaPingEstimator
import com.pingshield.core.JitterAnalyzer
import com.pingshield.core.PacketLossAnalyzer
import com.pingshield.core.PingEngine
import com.pingshield.core.StabilityEngine
import com.pingshield.killer.AppKiller
import com.pingshield.monitor.WifiChannelAnalyzer
import com.pingshield.monitor.WifiMonitor
import com.pingshield.utils.Constants
import com.pingshield.vpn.PingShieldVpn
import com.pingshield.vpn.TrafficController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val pingEngine: PingEngine,
    private val stabilityEngine: StabilityEngine,
    private val dnsManager: DnsManager,
    private val packetLossAnalyzer: PacketLossAnalyzer,
    private val wifiMonitor: WifiMonitor,
    private val trafficController: TrafficController,
    private val appKiller: AppKiller,
    private val adaptiveEngine: AdaptiveResponseEngine,
    private val jitterAnalyzer: JitterAnalyzer,
    private val ewmaEstimator: EwmaPingEstimator,
    private val channelAnalyzer: WifiChannelAnalyzer
) : ViewModel() {

    val ping: StateFlow<Long> = pingEngine.currentPing
    val spike: StateFlow<Boolean> = pingEngine.isSpike
    val pingHistory: StateFlow<List<Long>> = pingEngine.pingHistory
    val currentAction: StateFlow<String> = stabilityEngine.actionText
    val dns: StateFlow<String> = dnsManager.currentDns
    val loss: StateFlow<Double> = packetLossAnalyzer.lossPercent
    val lossType: StateFlow<String> = packetLossAnalyzer.lossType
    val rssi: StateFlow<Int> = wifiMonitor.rssi
    val linkSpeed: StateFlow<Int> = wifiMonitor.linkSpeed
    val frequency: StateFlow<Int> = wifiMonitor.frequency
    val blockedCount: StateFlow<Int> = trafficController.blockedAppCount
    val killedApps: StateFlow<List<String>> = appKiller.killedApps
    val jitter: StateFlow<Double> = jitterAnalyzer.ipdv
    val stabilityLabel: StateFlow<String> = jitterAnalyzer.stabilityLabel
    val smoothedPing: StateFlow<Double> = ewmaEstimator.smoothedPing
    val networkScore: StateFlow<Int> = adaptiveEngine.networkScore
    val scoreBreakdown = adaptiveEngine.scoreBreakdown
    val channelReport = channelAnalyzer.channelReport
    val interferenceLevel = channelAnalyzer.interferenceLevel
    val wifiWarning: StateFlow<String> = wifiMonitor.wifiWarning

    private val _isVpnActive = MutableStateFlow(false)
    val isVpnActive: StateFlow<Boolean> = _isVpnActive.asStateFlow()

    fun startVpn(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            appKiller.proactiveKill()
            dnsManager.preResolveDomains()
            dnsManager.activate()
            channelAnalyzer.start()
            PingShieldVpn.startVpn(context)
            _isVpnActive.value = true
        }
    }

    fun startVpnAndLaunchGame(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            appKiller.proactiveKill()
            dnsManager.preResolveDomains()
            dnsManager.activate()
            channelAnalyzer.start()
            PingShieldVpn.startVpn(context)
            _isVpnActive.value = true
            launchPubg(context)
        }
    }

    fun onStop(context: Context) {
        PingShieldVpn.stopVpn(context)
        _isVpnActive.value = false
    }

    private fun launchPubg(context: Context) {
        try {
            val intent = context.packageManager.getLaunchIntentForPackage(Constants.GAME_PACKAGE)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
        } catch (_: Exception) {}
    }

    override fun onCleared() {
        super.onCleared()
    }
}
