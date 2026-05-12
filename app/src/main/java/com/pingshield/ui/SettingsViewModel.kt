package com.pingshield.ui

import androidx.lifecycle.ViewModel
import com.pingshield.utils.PrefsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefsManager: PrefsManager
) : ViewModel() {

    val autoStart = MutableStateFlow(prefsManager.isAutoStartEnabled())
    val pingInterval = MutableStateFlow(prefsManager.getPingInterval())
    val spikeSensitivity = MutableStateFlow(prefsManager.getSpikeSensitivity())
    val dnsPrimary = MutableStateFlow(prefsManager.getDnsPrimary())
    val autoFlushDns = MutableStateFlow(prefsManager.isAutoFlushDns())
    val preResolve = MutableStateFlow(prefsManager.isPreResolveEnabled())
    val wifiLowLatency = MutableStateFlow(prefsManager.isWifiLowLatencyEnabled())
    val wakeLock = MutableStateFlow(prefsManager.isWakeLockEnabled())
    val perfMode = MutableStateFlow(prefsManager.isPerformanceModeEnabled())
    val autoSwitchMobile = MutableStateFlow(prefsManager.isAutoSwitchMobile())
    val dscpMarking = MutableStateFlow(prefsManager.isDscpMarkingEnabled())
    val tcpPsh = MutableStateFlow(prefsManager.isTcpPshForcingEnabled())
    val lossThreshold = MutableStateFlow(prefsManager.getLossReconnectThreshold())
    val autoReconnectBurst = MutableStateFlow(prefsManager.isAutoReconnectBurst())
    val notifPing = MutableStateFlow(prefsManager.showPingInNotification())
    val notifSpike = MutableStateFlow(prefsManager.showSpikeAlerts())
    val notifScore = MutableStateFlow(prefsManager.showScoreInNotification())
    val notifBlocker = MutableStateFlow(prefsManager.isNotifBlockerEnabled())

    fun toggleAutoStart(v: Boolean) { autoStart.value = v; prefsManager.setAutoStartEnabled(v) }
    fun toggleAutoFlushDns(v: Boolean) { autoFlushDns.value = v; prefsManager.setAutoFlushDns(v) }
    fun togglePreResolve(v: Boolean) { preResolve.value = v; prefsManager.setPreResolveEnabled(v) }
    fun toggleWifiLowLatency(v: Boolean) { wifiLowLatency.value = v; prefsManager.setWifiLowLatencyEnabled(v) }
    fun toggleWakeLock(v: Boolean) { wakeLock.value = v; prefsManager.setWakeLockEnabled(v) }
    fun togglePerfMode(v: Boolean) { perfMode.value = v; prefsManager.setPerformanceModeEnabled(v) }
    fun toggleAutoSwitchMobile(v: Boolean) { autoSwitchMobile.value = v; prefsManager.setAutoSwitchMobile(v) }
    fun toggleDscpMarking(v: Boolean) { dscpMarking.value = v; prefsManager.setDscpMarkingEnabled(v) }
    fun toggleTcpPsh(v: Boolean) { tcpPsh.value = v; prefsManager.setTcpPshForcingEnabled(v) }
    fun toggleAutoReconnectBurst(v: Boolean) { autoReconnectBurst.value = v; prefsManager.setAutoReconnectBurst(v) }
    fun toggleNotifPing(v: Boolean) { notifPing.value = v; prefsManager.setShowPingInNotification(v) }
    fun toggleNotifSpike(v: Boolean) { notifSpike.value = v; prefsManager.setShowSpikeAlerts(v) }
    fun toggleNotifScore(v: Boolean) { notifScore.value = v; prefsManager.setShowScoreInNotification(v) }
    fun toggleNotifBlocker(v: Boolean) { notifBlocker.value = v; prefsManager.setNotifBlockerEnabled(v) }
    fun setSpikeSensitivity(v: String) { spikeSensitivity.value = v; prefsManager.setSpikeSensitivity(v) }
    fun setDnsPrimary(v: String) { dnsPrimary.value = v; prefsManager.setDnsPrimary(v) }
    fun setLossThreshold(v: Float) { lossThreshold.value = v; prefsManager.setLossReconnectThreshold(v) }
}
