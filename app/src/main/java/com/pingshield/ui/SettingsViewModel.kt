package com.pingshield.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pingshield.utils.PrefsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefsManager: PrefsManager
) : ViewModel() {

    private val _autoStart = MutableStateFlow(false)
    val autoStart: StateFlow<Boolean> = _autoStart.asStateFlow()

    init {
        _autoStart.value = prefsManager.isAutoStartEnabled()
    }

    fun toggleAutoStart(enabled: Boolean) {
        _autoStart.value = enabled
        viewModelScope.launch {
            prefsManager.setAutoStartEnabled(enabled)
        }
    }
}
