package com.pingshield.monitor

import android.content.Context
import android.net.wifi.WifiManager
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

data class ChannelReport(
    val myChannel: Int,
    val interferingAPs: Int,
    val band: String,
    val recommendation: String
)

@Singleton
class WifiChannelAnalyzer @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _channelReport = MutableStateFlow(ChannelReport(0, 0, "N/A", "Scanning..."))
    val channelReport: StateFlow<ChannelReport> = _channelReport.asStateFlow()

    private val _interferenceLevel = MutableStateFlow("Unknown")
    val interferenceLevel: StateFlow<String> = _interferenceLevel.asStateFlow()

    private var scope: CoroutineScope? = null

    fun start() {
        stop()
        scope = CoroutineScope(Dispatchers.IO + Job())
        scope?.launch {
            while (isActive) {
                scanChannels()
                delay(Constants.CHANNEL_SCAN_INTERVAL_MS)
            }
        }
    }

    private fun scanChannels() {
        try {
            val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            val info = wm?.connectionInfo ?: return
            val freq = info.frequency
            val bssid = info.bssid ?: return

            val myChannel = when {
                freq in 2412..2484 -> (freq - 2407) / 5
                freq in 5170..5825 -> (freq - 5000) / 5
                else -> 0
            }
            val band = if (freq < 5000) "2.4GHz" else "5GHz"

            var interferingAPs = 0
            val results = wm.scanResults
            if (results != null) {
                for (ap in results) {
                    if (ap.BSSID == bssid) continue
                    val apFreq = ap.frequency
                    val apChannel = when {
                        apFreq in 2412..2484 -> (apFreq - 2407) / 5
                        apFreq in 5170..5825 -> (apFreq - 5000) / 5
                        else -> continue
                    }
                    if (kotlin.math.abs(apChannel - myChannel) <= 2) {
                        interferingAPs++
                    }
                }
            }

            val recommendation = when {
                interferingAPs > 5 ->
                    "High interference — switch to 5GHz or change router channel to 1, 6, or 11"
                interferingAPs > 2 ->
                    "Moderate interference on this channel"
                else -> "Channel clear"
            }

            _channelReport.value = ChannelReport(myChannel, interferingAPs, band, recommendation)
            _interferenceLevel.value = when {
                interferingAPs > 5 -> "HIGH"
                interferingAPs > 2 -> "MODERATE"
                else -> "CLEAR"
            }
        } catch (e: Exception) {
            // scan failed, keep previous values
        }
    }

    fun stop() {
        scope?.cancel()
        scope = null
    }
}
