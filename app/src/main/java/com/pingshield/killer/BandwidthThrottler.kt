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

    fun throttleApp(packageName: String) {
        val current = _throttledApps.value.toMutableList()
        if (!current.contains(packageName)) {
            current.add(packageName)
            _throttledApps.value = current
        }
    }

    fun removeThrottle(packageName: String) {
        _throttledApps.value = _throttledApps.value.filter { it != packageName }
    }

    fun clearAll() {
        _throttledApps.value = emptyList()
    }

    fun stop() {
        clearAll()
        scope?.cancel()
        scope = null
    }
}
