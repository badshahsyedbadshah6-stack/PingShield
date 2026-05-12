package com.pingshield.utils

object Constants {
    const val GAME_PACKAGE = "com.tencent.ig"
    const val DNS_PRIMARY = "1.1.1.1"
    const val DNS_SECONDARY = "8.8.8.8"
    const val DNS_TERTIARY = "9.9.9.9"
    const val PING_INTERVAL_MS = 200L
    const val ROLLING_PING_SIZE = 150
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
    const val EWMA_ALPHA = 0.125
    const val EWMA_BETA = 0.25
    const val JITTER_WINDOW = 20
    const val LOSS_HISTORY_SIZE = 100
    const val GRAPH_SIZE = 150
    const val CHANNEL_SCAN_INTERVAL_MS = 30_000L
    const val WAKELOCK_TIMEOUT_MS = 30 * 60 * 1000L
    const val TARGET_FRAME_NANOS = 16_666_666L
    const val RECONNECT_COOLDOWN_MS = 5000L
    const val KILL_COOLDOWN_MS = 3000L
    const val DNS_FLUSH_COOLDOWN_MS = 2000L
    const val SWITCH_COOLDOWN_MS = 10000L

    val PROACTIVE_KILL_LIST = listOf(
        "com.google.android.gms",
        "com.google.android.googlequicksearchbox",
        "com.google.android.youtube",
        "com.facebook.katana",
        "com.facebook.orca",
        "com.instagram.android",
        "com.whatsapp",
        "com.spotify.music",
        "com.snapchat.android",
        "com.twitter.android",
        "com.tiktok.android",
        "com.netflix.mediaclient",
        "com.microsoft.teams",
        "com.discord",
        "com.google.android.apps.photos",
        "com.amazon.mShop.android.shopping"
    )

    val PUBG_DOMAINS = listOf(
        "prod-live-front.ol.epicgames.com",
        "gateway.ol.epicgames.com",
        "1.1.1.1",
        "8.8.8.8"
    )

    val PING_TARGETS = listOf(
        PingTarget("1.1.1.1", 53, "Cloudflare"),
        PingTarget("8.8.8.8", 53, "Google"),
        PingTarget("9.9.9.9", 53, "Quad9")
    )

    data class PingTarget(val host: String, val port: Int, val label: String)
}
