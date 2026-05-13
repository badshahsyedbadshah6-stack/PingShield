package com.pingshield.vpn

import com.pingshield.core.DnsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DnsInterceptor @Inject constructor(
    private val dnsManager: DnsManager
) {
    private var scope: CoroutineScope? = null

    fun isKnownDomain(domain: String): Boolean {
        val cached = dnsManager.resolvedDomains.value
        return cached.containsKey(domain) &&
               cached[domain] != "failed" &&
               cached[domain] != "unresolved"
    }

    fun getCachedIp(domain: String): String? {
        val ip = dnsManager.resolvedDomains.value[domain]
        return if (ip == "failed" || ip == "unresolved") null else ip
    }

    fun start() {
        stop()
        scope = CoroutineScope(Dispatchers.IO + Job())
        scope?.launch {
            dnsManager.preResolveDomains()
        }
    }

    fun stop() {
        scope?.cancel()
        scope = null
    }
}
