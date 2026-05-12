package com.pingshield.killer

import android.content.ContentResolver
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncBlocker @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var isBlocked = false

    fun blockSync() {
        if (isBlocked) return
        try {
            ContentResolver.setMasterSyncAutomatically(false)
            isBlocked = true
        } catch (_: Exception) {}
    }

    fun restoreSync() {
        if (!isBlocked) return
        try {
            ContentResolver.setMasterSyncAutomatically(true)
            isBlocked = false
        } catch (_: Exception) {}
    }
}
