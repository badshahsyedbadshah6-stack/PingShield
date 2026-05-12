package com.pingshield.utils

object Constants {
    const val GAME_PACKAGE = "com.tencent.ig"
    const val DNS_PRIMARY = "1.1.1.1"
    const val DNS_SECONDARY = "8.8.8.8"
    const val PING_TARGET = "8.8.8.8"
    const val PING_PORT = 53
    const val PING_INTERVAL_MS = 200L
    const val ROLLING_PING_SIZE = 10
    const val GRAPH_PING_SIZE = 60
    const val SPIKE_THRESHOLD_MS = 30
    const val SCAN_INTERVAL_MS = 500L
    const val RSSI_WARNING_THRESHOLD = -70
    const val RSSI_CRITICAL_THRESHOLD = -80
    const val BLOCK_DURATION_MS = 30_000L
    const val PACKET_LOSS_TEST_COUNT = 20
    const val PACKET_LOSS_TIMEOUT_MS = 500L
    const val PACKET_LOSS_INTERVAL_MS = 2000L
    const val LOSS_THRESHOLD_PERCENT = 2.0
    const val VPN_MTU = 1500
    const val VPN_ADDRESS = "10.0.0.2"
    const val VPN_PREFIX_LENGTH = 32
    const val VPN_SESSION = "PingShield"
}
