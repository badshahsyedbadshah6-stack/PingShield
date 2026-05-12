package com.pingshield.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

data class SpeedTestResult(
    val downloadMbps: Double = 0.0,
    val uploadMbps: Double = 0.0,
    val latencyMs: Int = 0,
    val isRunning: Boolean = false,
    val error: String = ""
)

@Singleton
class SpeedTestEngine @Inject constructor() {

    private val _result = MutableStateFlow(SpeedTestResult())
    val result: StateFlow<SpeedTestResult> = _result.asStateFlow()

    private val testFileUrl = "https://speed.cloudflare.com/__down?bytes=%d"
    private val uploadUrl = "https://speed.cloudflare.com/__up"
    private val testSizes = listOf(25_000, 100_000, 500_000)

    suspend fun runTest() {
        if (_result.value.isRunning) return
        _result.value = SpeedTestResult(isRunning = true)

        withContext(Dispatchers.IO) {
            try {
                val downloadMbps = testDownload()
                val uploadMbps = testUpload()
                val latency = testLatency()

                _result.value = SpeedTestResult(
                    downloadMbps = downloadMbps,
                    uploadMbps = uploadMbps,
                    latencyMs = latency
                )
            } catch (e: Exception) {
                _result.value = SpeedTestResult(error = e.message ?: "Speed test failed")
            }
        }
    }

    private fun testDownload(): Double {
        val rates = testSizes.map { size ->
            val url = URL(String.format(testFileUrl, size))
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout = 10000
            conn.instanceFollowRedirects = true

            val start = System.nanoTime()
            var totalRead = 0L
            val buf = ByteArray(4096)

            try {
                conn.inputStream.use { stream ->
                    while (true) {
                        val read = stream.read(buf)
                        if (read < 0) break
                        totalRead += read
                    }
                }
            } catch (_: Exception) {}

            val elapsed = System.nanoTime() - start
            conn.disconnect()

            if (elapsed > 0) {
                (totalRead.toDouble() * 8.0) / (elapsed / 1_000_000_000.0) / 1_000_000.0
            } else 0.0
        }

        return rates.maxOrNull()?.let { "%.1f".format(it).toDouble() } ?: 0.0
    }

    private fun testUpload(): Double {
        return try {
            val data = ByteArray(250_000) { it.toByte() }
            val conn = URL(uploadUrl).openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.connectTimeout = 5000
            conn.readTimeout = 10000
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/octet-stream")

            val start = System.nanoTime()
            conn.outputStream.use { it.write(data) }
            val response = conn.responseCode
            val elapsed = System.nanoTime() - start
            conn.disconnect()

            if (response == 200 && elapsed > 0) {
                (data.size.toDouble() * 8.0) / (elapsed / 1_000_000_000.0) / 1_000_000.0
            } else 0.0
        } catch (_: Exception) { 0.0 }
    }

    private fun testLatency(): Int {
        val times = mutableListOf<Long>()
        repeat(3) {
            try {
                val conn = URL("https://1.1.1.1").openConnection() as HttpURLConnection
                conn.connectTimeout = 2000
                conn.readTimeout = 2000
                val start = System.nanoTime()
                conn.connect()
                conn.inputStream.read()
                val elapsed = (System.nanoTime() - start) / 1_000_000
                conn.disconnect()
                times.add(elapsed)
            } catch (_: Exception) {}
        }
        return if (times.isNotEmpty()) times.min().toInt() else 0
    }

    fun reset() {
        _result.value = SpeedTestResult()
    }
}
