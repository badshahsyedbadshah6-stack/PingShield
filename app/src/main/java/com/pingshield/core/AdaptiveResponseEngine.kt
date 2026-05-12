package com.pingshield.core

import com.pingshield.utils.Constants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class NetworkScore(
    val pingScore: Int,
    val jitterScore: Int,
    val lossScore: Int,
    val rssiScore: Int
) {
    val composite: Int get() =
        (pingScore * 0.25 + jitterScore * 0.40 + lossScore * 0.25 + rssiScore * 0.10).toInt()
}

enum class NetworkAction {
    STABLE, FLUSH_DNS, KILL_BACKGROUND_APPS, RECONNECT_TUNNEL,
    SWITCH_TO_MOBILE, MONITOR, WAIT
}

@Singleton
class AdaptiveResponseEngine @Inject constructor() {

    private val _currentAction = MutableStateFlow(NetworkAction.MONITOR)
    val currentAction: StateFlow<NetworkAction> = _currentAction.asStateFlow()

    private val _networkScore = MutableStateFlow(0)
    val networkScore: StateFlow<Int> = _networkScore.asStateFlow()

    private val _scoreBreakdown = MutableStateFlow(NetworkScore(0, 0, 0, 0))
    val scoreBreakdown: StateFlow<NetworkScore> = _scoreBreakdown.asStateFlow()

    private var lastReconnectTime = 0L
    private var lastKillTime = 0L
    private var lastFlushTime = 0L
    private var lastSwitchTime = 0L

    fun evaluate(
        ping: Double,
        jitter: Double,
        lossPercent: Double,
        lossType: String,
        rssi: Int
    ): NetworkAction {
        val pingScore = (100.0 - (ping.coerceIn(0.0, 200.0) / 2.0)).toInt().coerceIn(0, 100)
        val jitterScore = (100.0 - (jitter * 5.0).coerceIn(0.0, 100.0)).toInt().coerceIn(0, 100)
        val lossScore = (100.0 - (lossPercent * 10.0).coerceIn(0.0, 100.0)).toInt().coerceIn(0, 100)
        val rssiScore = if (rssi > Int.MIN_VALUE) {
            ((rssi.coerceIn(-100, -40) + 100) * 1.67).toInt().coerceIn(0, 100)
        } else 50

        val score = NetworkScore(pingScore, jitterScore, lossScore, rssiScore)
        _scoreBreakdown.value = score
        _networkScore.value = score.composite

        val now = System.currentTimeMillis()

        val action = when {
            score.composite > 80 -> NetworkAction.STABLE
            lossType == "BURST_LOSS" && now - lastReconnectTime > Constants.RECONNECT_COOLDOWN_MS -> {
                lastReconnectTime = now
                NetworkAction.RECONNECT_TUNNEL
            }
            jitterScore < 40 && now - lastKillTime > Constants.KILL_COOLDOWN_MS -> {
                lastKillTime = now
                NetworkAction.KILL_BACKGROUND_APPS
            }
            pingScore < 50 && now - lastFlushTime > Constants.DNS_FLUSH_COOLDOWN_MS -> {
                lastFlushTime = now
                NetworkAction.FLUSH_DNS
            }
            rssi < -80 && now - lastSwitchTime > Constants.SWITCH_COOLDOWN_MS -> {
                lastSwitchTime = now
                NetworkAction.SWITCH_TO_MOBILE
            }
            lossType == "RANDOM_LOSS" && now - lastFlushTime > Constants.DNS_FLUSH_COOLDOWN_MS -> {
                lastFlushTime = now
                NetworkAction.FLUSH_DNS
            }
            else -> NetworkAction.MONITOR
        }

        _currentAction.value = action
        return action
    }

    fun reset() {
        _currentAction.value = NetworkAction.MONITOR
        _networkScore.value = 0
        _scoreBreakdown.value = NetworkScore(0, 0, 0, 0)
        lastReconnectTime = 0L
        lastKillTime = 0L
        lastFlushTime = 0L
        lastSwitchTime = 0L
    }
}
