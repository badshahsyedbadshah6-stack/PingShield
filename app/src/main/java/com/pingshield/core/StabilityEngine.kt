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
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StabilityEngine @Inject constructor(
    private val pingEngine: PingEngine,
    private val wifiMonitor: WifiMonitor,
    private val packetLossDetector: PacketLossDetector,
    private val dnsManager: DnsManager,
    private val appScanner: AppScanner,
    private val appKiller: AppKiller,
    private val syncBlocker: SyncBlocker,
    private val networkSwitcher: NetworkSwitcher
) {

    private val _currentAction = MutableStateFlow("Initializing...")
    val currentAction: StateFlow<String> = _currentAction.asStateFlow()

    private var scope: CoroutineScope? = null

    fun start() {
        scope = CoroutineScope(Dispatchers.Default + Job())

        pingEngine.start()
        wifiMonitor.start()
        packetLossDetector.start()
        appScanner.start()
        syncBlocker.blockSync()

        scope?.launch {
            pingEngine.spikeDetected.collect { spike ->
                if (spike) {
                    val apps = appScanner.activeApps.value
                    val backgroundApp = apps.firstOrNull { it != com.pingshield.utils.Constants.GAME_PACKAGE }
                    if (backgroundApp != null) {
                        appKiller.killApp(backgroundApp)
                        _currentAction.value = "Killed: $backgroundApp"
                    }

                    if (dnsManager.currentDns.value != com.pingshield.utils.Constants.DNS_PRIMARY) {
                        dnsManager.flush()
                        _currentAction.value = "DNS Flushed"
                    }
                }
            }
        }

        scope?.launch {
            wifiMonitor.rssi.collect { rssi ->
                if (rssi < com.pingshield.utils.Constants.RSSI_WARNING_THRESHOLD && rssi > Int.MIN_VALUE) {
                    networkSwitcher.switchToMobile()
                    _currentAction.value = "Switched to Mobile Data"
                }
            }
        }

        scope?.launch {
            packetLossDetector.lossPercentage.collect { loss ->
                if (loss > com.pingshield.utils.Constants.LOSS_THRESHOLD_PERCENT) {
                    dnsManager.flush()
                    _currentAction.value = "Packet loss ${String.format("%.1f", loss)}% - DNS flushed"
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
                    _currentAction.value = "Sync blocked for ${syncApps.size} app(s)"
                }
            }
        }
    }

    fun stop() {
        pingEngine.stop()
        wifiMonitor.stop()
        packetLossDetector.stop()
        appScanner.stop()
        syncBlocker.restoreSync()
        networkSwitcher.restoreWifi()
        appKiller.clearAll()
        scope?.cancel()
        scope = null
        _currentAction.value = "Stopped"
    }
}
