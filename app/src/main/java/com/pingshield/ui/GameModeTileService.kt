package com.pingshield.ui

import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.Q)
class GameModeTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTile(false)
    }

    override fun onClick() {
        super.onClick()
        val isActive = qsTile?.state == Tile.STATE_ACTIVE
        if (isActive) {
            updateTile(false)
        } else {
            updateTile(true)
            val intent = packageManager.getLaunchIntentForPackage("com.pingshield")
            if (intent != null) {
                startActivityAndCollapse(intent)
            }
        }
    }

    private fun updateTile(active: Boolean) {
        val tile = qsTile ?: return
        tile.state = if (active) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = if (active) "Game Mode ON" else "Game Mode"
        tile.updateTile()
    }
}
