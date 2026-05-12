package com.pingshield.ui

import androidx.lifecycle.ViewModel
import com.pingshield.killer.AppKiller
import com.pingshield.monitor.AppScanner
import com.pingshield.utils.PrefsManager
import com.pingshield.vpn.TrafficController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class AppsViewModel @Inject constructor(
    private val appKiller: AppKiller,
    private val appScanner: AppScanner,
    private val trafficController: TrafficController,
    private val prefsManager: PrefsManager
) : ViewModel() {

    val blockedApps = appKiller.killedApps
    val activeApps = appScanner.activeApps

    private val _whitelistedPackages = MutableStateFlow<Set<String>>(emptySet())
    val whitelistedPackages: StateFlow<Set<String>> = _whitelistedPackages.asStateFlow()

    init {
        _whitelistedPackages.value = prefsManager.getWhitelistedPackages()
        appScanner.start()
    }

    fun blockApp(pkg: String) = appKiller.killApp(pkg)

    fun releaseApp(pkg: String) {
        appKiller.releaseApp(pkg)
    }

    fun removeWhitelist(pkg: String) {
        trafficController.removeFromWhitelist(pkg)
        _whitelistedPackages.value = prefsManager.getWhitelistedPackages()
    }

    override fun onCleared() { appScanner.stop() }
}
