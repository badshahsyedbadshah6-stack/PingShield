package com.pingshield.killer

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BandwidthThrottler @Inject constructor() {
    private val _throttledApps = MutableStateFlow<List<String>>(emptyList())
    val throttledApps: StateFlow<List<String>> = _throttledApps.asStateFlow()
    private var scope: CoroutineScope? = null

    fun throttleApp(pkg: String) {
        val cur = _throttledApps.value.toMutableList()
        if (!cur.contains(pkg)) { cur.add(pkg); _throttledApps.value = cur }
    }

    fun removeThrottle(pkg: String) {
        _throttledApps.value = _throttledApps.value.filter { it != pkg }
    }

    fun clearAll() { _throttledApps.value = emptyList() }
    fun stop() { clearAll(); scope?.cancel(); scope = null }
}
