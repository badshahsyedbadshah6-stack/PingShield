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
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppScanner @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _activeApps = MutableStateFlow<List<String>>(emptyList())
    val activeApps: StateFlow<List<String>> = _activeApps.asStateFlow()

    private var scope: CoroutineScope? = null
    private var job: Job? = null

    fun start() {
        stop()
        scope = CoroutineScope(Dispatchers.IO + Job())
        job = scope?.launch {
            while (isActive) {
                val apps = scanActiveApps()
                _activeApps.value = apps
                delay(Constants.SCAN_INTERVAL_MS)
            }
        }
    }

    fun stop() {
        job?.cancel()
        scope?.cancel()
        scope = null
        job = null
        _activeApps.value = emptyList()
    }

    private fun scanActiveApps(): List<String> {
        return try {
            val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
                ?: return emptyList()

            val endTime = System.currentTimeMillis()
            val startTime = endTime - 5000

            val stats = usm.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                startTime,
                endTime
            )

            stats?.filter { stat ->
                val pkg = stat.packageName
                pkg != null &&
                pkg != Constants.GAME_PACKAGE &&
                !pkg.startsWith("android.") &&
                !pkg.startsWith("com.android.") &&
                !pkg.startsWith("com.google.android.") &&
                !pkg.startsWith("com.samsung.") &&
                stat.totalTimeInForeground > 0
            }?.map { it.packageName }?.distinct() ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
