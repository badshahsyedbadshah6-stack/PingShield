package com.pingshield.monitor

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
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class CpuStats(
    val freqMHz: Int = 0,
    val coreCount: Int = 0,
    val thermalCelsius: Float = 0f,
    val gov: String = ""
)

@Singleton
class CpuMonitor @Inject constructor() {

    private val _stats = MutableStateFlow(CpuStats())
    val stats: StateFlow<CpuStats> = _stats.asStateFlow()

    private val _isThrottling = MutableStateFlow(false)
    val isThrottling: StateFlow<Boolean> = _isThrottling.asStateFlow()

    private var scope: CoroutineScope? = null

    fun start() {
        stop()
        scope = CoroutineScope(Dispatchers.IO + Job())
        scope?.launch {
            while (isActive) {
                val freq = readCpuFreq()
                val cores = countCores()
                val temp = readThermal()
                val gov = readGovernor()
                _stats.value = CpuStats(freqMHz = freq, coreCount = cores, thermalCelsius = temp, gov = gov)
                _isThrottling.value = temp > 65f || freq < 500
                delay(2000)
            }
        }
    }

    private fun readCpuFreq(): Int {
        for (i in 0..7) {
            val path = "/sys/devices/system/cpu/cpu$i/cpufreq/scaling_cur_freq"
            try {
                val content = File(path).readText().trim()
                val khz = content.toIntOrNull() ?: continue
                return khz / 1000
            } catch (_: Exception) { continue }
        }
        return 0
    }

    private fun countCores(): Int {
        return try {
            val cpuDir = File("/sys/devices/system/cpu")
            cpuDir.listFiles()?.count { it.name.startsWith("cpu") && it.name[3].isDigit() } ?: 0
        } catch (_: Exception) { 0 }
    }

    private fun readThermal(): Float {
        val thermalDirs = listOf(
            "/sys/class/thermal/thermal_zone0/temp",
            "/sys/class/thermal/thermal_message/temp",
            "/sys/devices/virtual/thermal/thermal_zone0/temp"
        )
        for (path in thermalDirs) {
            try {
                val raw = File(path).readText().trim()
                val millideg = raw.toIntOrNull() ?: continue
                return millideg / 1000f
            } catch (_: Exception) { continue }
        }
        return 0f
    }

    private fun readGovernor(): String {
        return try {
            File("/sys/devices/system/cpu/cpu0/cpufreq/scaling_governor").readText().trim()
        } catch (_: Exception) { "" }
    }

    fun stop() {
        scope?.cancel()
        scope = null
        _stats.value = CpuStats()
        _isThrottling.value = false
    }
}
