package com.pingshield.core

import com.pingshield.utils.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.net.InetAddress
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DnsManager @Inject constructor() {

    data class DomainResult(val domain: String, val ip: String)

    private val resolvedCache = mutableMapOf<String, List<String>>()

    private val _currentDns = MutableStateFlow(Constants.DNS_PRIMARY)
    val currentDns: StateFlow<String> = _currentDns.asStateFlow()

    private val _resolvedDomains = MutableStateFlow<Map<String, String>>(emptyMap())
    val resolvedDomains: StateFlow<Map<String, String>> = _resolvedDomains.asStateFlow()

    private val _lastFlushTime = MutableStateFlow(0L)
    val lastFlushTime: StateFlow<Long> = _lastFlushTime.asStateFlow()

    suspend fun preResolveDomains() {
        withContext(Dispatchers.IO) {
            resolvedCache.clear()
            val resultMap = mutableMapOf<String, String>()
            for (domain in Constants.PUBG_DOMAINS) {
                try {
                    val addrs = InetAddress.getAllByName(domain)
                    val ips = addrs.map { it.hostAddress }.filterNotNull()
                    resolvedCache[domain] = ips
                    resultMap[domain] = ips.firstOrNull() ?: "unresolved"
                } catch (e: Exception) {
                    resultMap[domain] = "failed"
                }
            }
            _resolvedDomains.value = resultMap
        }
    }

    suspend fun activate() {
        withContext(Dispatchers.IO) {
            try {
                InetAddress.getByName(Constants.DNS_PRIMARY)
                _currentDns.value = Constants.DNS_PRIMARY
            } catch (e: Exception) {
                _currentDns.value = Constants.DNS_SECONDARY
            }
        }
    }

    suspend fun flush() {
        withContext(Dispatchers.IO) {
            _lastFlushTime.value = System.currentTimeMillis()
            resolvedCache.clear()
            try {
                InetAddress.getByName(Constants.DNS_PRIMARY).isReachable(1000)
                _currentDns.value = Constants.DNS_PRIMARY
            } catch (e: Exception) {
                try {
                    InetAddress.getByName(Constants.DNS_SECONDARY).isReachable(1000)
                    _currentDns.value = Constants.DNS_SECONDARY
                } catch (e2: Exception) {}
            }
            preResolveDomains()
        }
    }

    fun reset() {
        _currentDns.value = Constants.DNS_PRIMARY
        resolvedCache.clear()
        _resolvedDomains.value = emptyMap()
        _lastFlushTime.value = 0L
    }
}
