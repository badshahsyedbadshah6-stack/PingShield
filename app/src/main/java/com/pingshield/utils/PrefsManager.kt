package com.pingshield.utils

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PrefsManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("pingshield_prefs", Context.MODE_PRIVATE)

    fun getWhitelistedPackages(): Set<String> {
        return prefs.getStringSet("whitelist", setOf(Constants.GAME_PACKAGE)) ?: setOf(Constants.GAME_PACKAGE)
    }

    fun addWhitelistPackage(pkg: String) {
        val current = getWhitelistedPackages().toMutableSet()
        current.add(pkg)
        prefs.edit().putStringSet("whitelist", current).apply()
    }

    fun removeWhitelistPackage(pkg: String) {
        val current = getWhitelistedPackages().toMutableSet()
        current.remove(pkg)
        prefs.edit().putStringSet("whitelist", current).apply()
    }

    fun isAutoStartEnabled(): Boolean {
        return prefs.getBoolean("auto_start", false)
    }

    fun setAutoStartEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("auto_start", enabled).apply()
    }
}
