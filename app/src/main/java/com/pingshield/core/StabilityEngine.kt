package com.pingshield.core

import com.pingshield.killer.AppKiller
import com.pingshield.killer.SyncBlocker
import com.pingshield.monitor.AppScanner
import com.pingshield.monitor.NetworkSwitcher
import com.pingshield.monitor.WifiMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StabilityEngine @Inject constructor(
    private val pingEngine: PingEngine,
    private val wifiMonitor: WifiMonitor,
    private val packetLossAnalyzer: PacketLossAnalyzer,
    private val dnsManager: DnsManager,
    private val appScanner: AppScanner,
    private val appKiller: AppKiller,
    private val syncBlocker: SyncBlocker,
    private val networkSwitcher: NetworkSwitcher,
    private val adaptiveEngine: AdaptiveResponseEngine,
    private val jitterAnalyzer: JitterAnalyzer
) {
    private val _actionText = MutableStateFlow("Initializing...")
    val actionText: StateFlow<String> = _actionText.asStateFlow()

    private var scope: CoroutineScope? = null

    fun start() {
        scope = CoroutineScope(Dispatchers.Default + Job())
        pingEngine.start()
        wifiMonitor.start()
        packetLossAnalyzer.start()
        appScanner.start()
        syncBlocker.blockSync()

        scope?.launch {
            kotlinx.coroutines.flow.combine(
                pingEngine.currentPing,
                jitterAnalyzer.ipdv,
                packetLossAnalyzer.lossPercent,
                packetLossAnalyzer.lossType,
                wifiMonitor.rssi
            ) { ping, jitter, lossPct, lossType, rssi ->
                adaptiveEngine.evaluate(
                    ping = ping.toDouble(),
                    jitter = jitter,
                    lossPercent = lossPct,
                    lossType = lossType,
                    rssi = rssi
                )
            }.collect { action ->
                when (action) {
                    NetworkAction.FLUSH_DNS -> {
                        dnsManager.flush()
                        _actionText.value = "DNS Flushed"
                    }
                    NetworkAction.KILL_BACKGROUND_APPS -> {
                        val apps = appScanner.activeApps.value
                        val target = apps.firstOrNull { it != com.pingshield.utils.Constants.GAME_PACKAGE }
                        if (target != null) {
                            appKiller.killApp(target)
                            _actionText.value = "Killed: $target"
                        }
                    }
                    NetworkAction.RECONNECT_TUNNEL -> {
                        _actionText.value = "Reconnecting tunnel..."
                    }
                    NetworkAction.SWITCH_TO_MOBILE -> {
                        networkSwitcher.switchToMobile()
                        _actionText.value = "Switched to Mobile Data"
                    }
                    NetworkAction.STABLE -> {
                        _actionText.value = "Stable"
                    }
                    else -> {}
                }
            }
        }

        scope?.launch {
            appScanner.activeApps.collect { apps ->
                val syncApps = apps.filter {
                    it.contains("sync") || it.contains("backup") || it.contains("drive")
                }
                if (syncApps.isNotEmpty()) {
                    syncBlocker.blockSync()
                }
            }
        }
    }

    fun stop() {
        pingEngine.stop()
        wifiMonitor.stop()
        packetLossAnalyzer.stop()
        appScanner.stop()
        syncBlocker.restoreSync()
        networkSwitcher.restoreWifi()
        appKiller.clearAll()
        adaptiveEngine.reset()
        scope?.cancel()
        scope = null
        _actionText.value = "Stopped"
    }
}
