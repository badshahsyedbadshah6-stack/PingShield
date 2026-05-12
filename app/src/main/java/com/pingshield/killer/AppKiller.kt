package com.pingshield.killer

import com.pingshield.utils.Constants
import com.pingshield.vpn.TrafficController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppKiller @Inject constructor(
    private val trafficController: TrafficController
) {
    private val _killedApps = MutableStateFlow<List<String>>(emptyList())
    val killedApps: StateFlow<List<String>> = _killedApps.asStateFlow()

    private var scope: CoroutineScope? = null
    private val activeTimers = mutableMapOf<String, Job>()

    fun killApp(packageName: String) {
        if (packageName == Constants.GAME_PACKAGE) return
        if (_killedApps.value.contains(packageName)) return

        trafficController.addToTempBlocked(packageName)
        _killedApps.value = _killedApps.value + packageName

        val timerJob = CoroutineScope(Dispatchers.Default + Job()).launch {
            delay(Constants.BLOCK_DURATION_MS)
            releaseApp(packageName)
        }
        activeTimers[packageName] = timerJob
    }

    private fun releaseApp(packageName: String) {
        trafficController.removeFromTempBlocked(packageName)
        _killedApps.value = _killedApps.value.filter { it != packageName }
        activeTimers.remove(packageName)
    }

    fun clearAll() {
        activeTimers.values.forEach { it.cancel() }
        activeTimers.clear()
        trafficController.clearAll()
        _killedApps.value = emptyList()
    }

    fun stop() {
        clearAll()
        scope?.cancel()
        scope = null
    }
}
