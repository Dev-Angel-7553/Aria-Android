package ru.muz.tile

import android.content.ComponentName
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.view.KeyEvent
import androidx.media3.session.MediaButtonReceiver
import ru.muz.R

class PlayTileService : TileService() {
    override fun onStartListening() {
        super.onStartListening()
        val tile = qsTile ?: return
        tile.label = "Aria"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) tile.subtitle = "Играть / пауза"
        tile.icon = Icon.createWithResource(this, R.drawable.ic_stat_aria)
        tile.state = Tile.STATE_ACTIVE
        tile.updateTile()
    }

    override fun onClick() {
        super.onClick()
        val intent = Intent(Intent.ACTION_MEDIA_BUTTON)
            .setComponent(ComponentName(this, MediaButtonReceiver::class.java))
            .putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE))
        sendBroadcast(intent)
    }
}
