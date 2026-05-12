package com.pingshield.core

import androidx.compose.ui.graphics.Color
import com.pingshield.utils.Constants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JitterAnalyzer @Inject constructor() {

    private val window = mutableListOf<Double>()

    private val _jitter = MutableStateFlow(0.0)
    val jitter: StateFlow<Double> = _jitter.asStateFlow()

    private val _ipdv = MutableStateFlow(0.0)
    val ipdv: StateFlow<Double> = _ipdv.asStateFlow()

    private val _stabilityLabel = MutableStateFlow("N/A")
    val stabilityLabel: StateFlow<String> = _stabilityLabel.asStateFlow()

    private val _stabilityColor = MutableStateFlow(Color.Gray)
    val stabilityColor: StateFlow<Color> = _stabilityColor.asStateFlow()

    fun addSample(sample: Double) {
        window.add(sample)
        if (window.size > Constants.JITTER_WINDOW) {
            window.removeAt(0)
        }
        recalculate()
    }

    private fun recalculate() {
        if (window.size < 2) return

        val mean = window.average()
        val mad = window.map { kotlin.math.abs(it - mean) }.average()
        _jitter.value = mad

        var ipdvSum = 0.0
        for (i in 1 until window.size) {
            ipdvSum += kotlin.math.abs(window[i] - window[i - 1])
        }
        val ipdvVal = ipdvSum / (window.size - 1)
        _ipdv.value = ipdvVal

        when {
            ipdvVal < 3.0 -> {
                _stabilityLabel.value = "Excellent"
                _stabilityColor.value = Color(0xFF00E676)
            }
            ipdvVal < 8.0 -> {
                _stabilityLabel.value = "Good"
                _stabilityColor.value = Color(0xFFCDDC39)
            }
            ipdvVal < 15.0 -> {
                _stabilityLabel.value = "Unstable"
                _stabilityColor.value = Color(0xFFFF9800)
            }
            else -> {
                _stabilityLabel.value = "Critical"
                _stabilityColor.value = Color(0xFFFF1744)
            }
        }
    }

    fun reset() {
        window.clear()
        _jitter.value = 0.0
        _ipdv.value = 0.0
        _stabilityLabel.value = "N/A"
        _stabilityColor.value = Color.Gray
    }
}
