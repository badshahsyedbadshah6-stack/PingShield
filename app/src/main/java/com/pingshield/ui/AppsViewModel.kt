package com.pingshield.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pingshield.killer.AppKiller
import com.pingshield.monitor.AppScanner
import com.pingshield.utils.PrefsManager
import com.pingshield.vpn.TrafficController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppsViewModel @Inject constructor(
    private val appKiller: AppKiller,
    private val appScanner: AppScanner,
    private val trafficController: TrafficController,
    private val prefsManager: PrefsManager
) : ViewModel() {

    val blockedApps: StateFlow<List<String>> = appKiller.killedApps
    val activeApps: StateFlow<List<String>> = appScanner.activeApps

    private val _whitelistedPackages = MutableStateFlow<Set<String>>(emptySet())
    val whitelistedPackages: StateFlow<Set<String>> = _whitelistedPackages.asStateFlow()

    init {
        _whitelistedPackages.value = prefsManager.getWhitelistedPackages()
        if (activeApps.value.isEmpty()) {
            appScanner.start()
        }
    }

    fun blockApp(pkg: String) {
        viewModelScope.launch {
            appKiller.killApp(pkg)
        }
    }

    fun releaseApp(pkg: String) {
        trafficController.removeFromTempBlocked(pkg)
    }

    fun addWhitelist(pkg: String) {
        trafficController.addToWhitelist(pkg)
        _whitelistedPackages.value = prefsManager.getWhitelistedPackages()
    }

    fun removeWhitelist(pkg: String) {
        trafficController.removeFromWhitelist(pkg)
        _whitelistedPackages.value = prefsManager.getWhitelistedPackages()
    }

    override fun onCleared() {
        super.onCleared()
        appScanner.stop()
    }
}
