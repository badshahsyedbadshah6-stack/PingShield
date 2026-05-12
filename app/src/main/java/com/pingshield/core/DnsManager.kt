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

    private val _currentDns = MutableStateFlow(Constants.DNS_PRIMARY)
    val currentDns: StateFlow<String> = _currentDns.asStateFlow()

    private var dnsResolved = false

    suspend fun activate() {
        withContext(Dispatchers.IO) {
            try {
                InetAddress.getByName(Constants.DNS_PRIMARY)
                _currentDns.value = Constants.DNS_PRIMARY
                dnsResolved = true
            } catch (e: Exception) {
                _currentDns.value = Constants.DNS_SECONDARY
                dnsResolved = true
            }
        }
    }

    suspend fun flush() {
        withContext(Dispatchers.IO) {
            try {
                InetAddress.getByName(Constants.DNS_PRIMARY).isReachable(1000)
                _currentDns.value = Constants.DNS_PRIMARY
            } catch (e: Exception) {
                try {
                    InetAddress.getByName(Constants.DNS_SECONDARY).isReachable(1000)
                    _currentDns.value = Constants.DNS_SECONDARY
                } catch (e2: Exception) {
                    // DNS unreachable, keep current
                }
            }
        }
    }

    fun reset() {
        _currentDns.value = Constants.DNS_PRIMARY
        dnsResolved = false
    }
}
