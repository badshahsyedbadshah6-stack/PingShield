package com.pingshield.vpn

import com.pingshield.utils.Constants
import com.pingshield.utils.PrefsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrafficController @Inject constructor(
    private val prefsManager: PrefsManager
) {
    private val whitelist = mutableSetOf(Constants.GAME_PACKAGE)
    private val _blockedAppCount = MutableStateFlow(0)
    val blockedAppCount: StateFlow<Int> = _blockedAppCount.asStateFlow()

    fun loadWhitelist() {
        whitelist.clear()
        whitelist.addAll(prefsManager.getWhitelistedPackages())
        whitelist.add(Constants.GAME_PACKAGE)
    }

    fun getWhitelist(): Set<String> = whitelist.toSet()

    fun addToWhitelist(pkg: String) {
        whitelist.add(pkg)
        prefsManager.addWhitelistPackage(pkg)
    }

    fun removeFromWhitelist(pkg: String) {
        whitelist.remove(pkg)
        prefsManager.removeWhitelistPackage(pkg)
    }

    fun isWhitelisted(pkg: String): Boolean = whitelist.contains(pkg)

    fun clearAll() {
        _blockedAppCount.value = 0
    }
}
