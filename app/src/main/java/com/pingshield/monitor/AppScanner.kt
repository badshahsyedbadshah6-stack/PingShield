package com.pingshield.monitor

import android.app.usage.UsageStatsManager
import android.content.Context
import com.pingshield.utils.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppScanner @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _activeApps = MutableStateFlow<List<String>>(emptyList())
    val activeApps: StateFlow<List<String>> = _activeApps.asStateFlow()

    private var scope: CoroutineScope? = null

    fun start() {
        stop()
        scope = CoroutineScope(Dispatchers.IO + Job())
        scope?.launch {
            while (isActive) {
                val apps = scanActiveApps()
                _activeApps.value = apps
                delay(Constants.SCAN_INTERVAL_MS)
            }
        }
    }

    private fun scanActiveApps(): List<String> {
        return try {
            val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
                ?: return emptyList()
            val end = System.currentTimeMillis()
            val start = end - 5000
            val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, start, end)
            stats?.filter { stat ->
                val pkg = stat.packageName
                pkg != null && pkg != Constants.GAME_PACKAGE &&
                !pkg.startsWith("android.") && !pkg.startsWith("com.android.") &&
                !pkg.startsWith("com.google.android.") && !pkg.startsWith("com.samsung.") &&
                stat.totalTimeInForeground > 0
            }?.map { it.packageName }?.distinct() ?: emptyList()
        } catch (_: Exception) { emptyList() }
    }

    fun stop() {
        scope?.cancel()
        scope = null
        _activeApps.value = emptyList()
    }
}
