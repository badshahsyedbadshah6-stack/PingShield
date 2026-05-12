package com.pingshield.killer

import com.pingshield.utils.Constants
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
class AppKiller @Inject constructor() {

    private val _killedApps = MutableStateFlow<List<String>>(emptyList())
    val killedApps: StateFlow<List<String>> = _killedApps.asStateFlow()

    private val _blockedSince = MutableStateFlow<Map<String, Long>>(emptyMap())
    val blockedSince: StateFlow<Map<String, Long>> = _blockedSince.asStateFlow()

    private val blockedList = mutableMapOf<String, Long>()
    private val blockTimers = mutableMapOf<String, Job>()
    private var scope: CoroutineScope? = null

    fun proactiveKill(): List<String> {
        val killed = mutableListOf<String>()
        for (pkg in Constants.PROACTIVE_KILL_LIST) {
            if (!blockedList.containsKey(pkg)) {
                blockedList[pkg] = System.currentTimeMillis()
                killed.add(pkg)
            }
        }
        _killedApps.value = blockedList.keys.toList()
        _blockedSince.value = blockedList.toMap()
        return killed
    }

    fun killApp(packageName: String) {
        if (packageName == Constants.GAME_PACKAGE) return
        if (blockedList.containsKey(packageName)) return

        blockedList[packageName] = System.currentTimeMillis()
        _killedApps.value = blockedList.keys.toList()
        _blockedSince.value = blockedList.toMap()

        val timer = CoroutineScope(Dispatchers.Default + Job()).launch {
            delay(Constants.BLOCK_DURATION_MS)
            releaseApp(packageName)
        }
        blockTimers[packageName] = timer
    }

    fun releaseApp(packageName: String) {
        blockedList.remove(packageName)
        blockTimers.remove(packageName)
        _killedApps.value = blockedList.keys.toList()
        _blockedSince.value = blockedList.toMap()
    }

    fun getBlockedPackages(): Set<String> = blockedList.keys

    fun clearAll() {
        blockTimers.values.forEach { it.cancel() }
        blockTimers.clear()
        blockedList.clear()
        _killedApps.value = emptyList()
        _blockedSince.value = emptyMap()
    }

    fun stop() {
        clearAll()
        scope?.cancel()
        scope = null
    }
}
