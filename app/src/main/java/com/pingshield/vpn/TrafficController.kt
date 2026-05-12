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

    private val tempBlocklist = mutableSetOf<String>()
    private val _tempBlockedApps = MutableStateFlow<List<String>>(emptyList())
    val tempBlockedApps: StateFlow<List<String>> = _tempBlockedApps.asStateFlow()

    fun loadWhitelist() {
        whitelist.clear()
        whitelist.addAll(prefsManager.getWhitelistedPackages())
    }

    fun addToWhitelist(pkg: String) {
        whitelist.add(pkg)
        prefsManager.addWhitelistPackage(pkg)
    }

    fun removeFromWhitelist(pkg: String) {
        whitelist.remove(pkg)
        prefsManager.removeWhitelistPackage(pkg)
    }

    fun isWhitelisted(pkg: String): Boolean {
        return whitelist.contains(pkg)
    }

    fun addToTempBlocked(pkg: String) {
        tempBlocklist.add(pkg)
        _tempBlockedApps.value = tempBlocklist.toList()
        _blockedAppCount.value = tempBlocklist.size
    }

    fun removeFromTempBlocked(pkg: String) {
        tempBlocklist.remove(pkg)
        _tempBlockedApps.value = tempBlocklist.toList()
        _blockedAppCount.value = tempBlocklist.size
    }

    fun shouldAllowPacket(uid: Int): Boolean {
        return true
    }

    fun clearAll() {
        tempBlocklist.clear()
        _tempBlockedApps.value = emptyList()
        _blockedAppCount.value = 0
    }
}
