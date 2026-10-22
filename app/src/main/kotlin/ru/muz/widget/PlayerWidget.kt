package ru.muz.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.view.KeyEvent
import android.widget.RemoteViews
import androidx.media3.session.MediaButtonReceiver
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.muz.MainActivity
import ru.muz.R

class PlayerWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        WidgetUpdater.refresh(context)
    }
}

class PlayerWidgetLarge : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        WidgetUpdater.refresh(context)
    }
}

object WidgetUpdater {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var coverJob: Job? = null
    private var coverUrl: String? = null
    private var cover: Bitmap? = null

    private data class State(val title: String, val artist: String, val artwork: String?, val playing: Boolean)

    private var state: State? = null

    fun push(context: Context, title: String, artist: String, artwork: String?, playing: Boolean) {
        val app = context.applicationContext
        val next = State(title, artist, artwork, playing)
        if (next == state) return
        state = next
        app.getSharedPreferences("widget", Context.MODE_PRIVATE).edit()
            .putString("title", title)
            .putString("artist", artist)
            .putString("artwork", artwork)
            .apply()
        if (artwork != coverUrl) {
            coverUrl = artwork
            cover = null
            coverJob?.cancel()
            if (!artwork.isNullOrBlank()) {
                coverJob = scope.launch {
                    cover = loadCover(app, artwork)
                    render(app)
                }
            }
        }
        render(app)
    }

    fun refresh(context: Context) {
        val app = context.applicationContext
        if (state == null) {
            val prefs = app.getSharedPreferences("widget", Context.MODE_PRIVATE)
            val title = prefs.getString("title", null)
            if (title != null) {
                push(app, title, prefs.getString("artist", null).orEmpty(), prefs.getString("artwork", null), false)
                return
            }
        }
        render(app)
    }

    private suspend fun loadCover(context: Context, url: String): Bitmap? = withContext(Dispatchers.IO) {
        val request = ImageRequest.Builder(context).data(url).size(256).allowHardware(false).build()
        ((context.imageLoader.execute(request) as? SuccessResult)?.drawable as? BitmapDrawable)?.bitmap
    }

    private fun render(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        listOf(
            PlayerWidget::class.java to R.layout.widget_player,
            PlayerWidgetLarge::class.java to R.layout.widget_player_large,
        ).forEach { (provider, layout) ->
            val ids = manager.getAppWidgetIds(ComponentName(context, provider))
            if (ids.isEmpty()) return@forEach
            val views = RemoteViews(context.packageName, layout)
            val current = state
            views.setTextViewText(R.id.widget_title, current?.title?.ifBlank { null } ?: context.getString(R.string.widget_idle_title))
            views.setTextViewText(R.id.widget_artist, current?.artist?.ifBlank { null } ?: context.getString(R.string.widget_idle_artist))
            val bitmap = cover
            if (bitmap != null) views.setImageViewBitmap(R.id.widget_cover, bitmap)
            else views.setImageViewResource(R.id.widget_cover, R.mipmap.ic_launcher)
            views.setImageViewResource(
                R.id.widget_play,
                if (current?.playing == true) R.drawable.ic_widget_pause else R.drawable.ic_widget_play,
            )
            views.setOnClickPendingIntent(R.id.widget_play, mediaButton(context, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE))
            views.setOnClickPendingIntent(R.id.widget_next, mediaButton(context, KeyEvent.KEYCODE_MEDIA_NEXT))
            views.setOnClickPendingIntent(R.id.widget_prev, mediaButton(context, KeyEvent.KEYCODE_MEDIA_PREVIOUS))
            val open = PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            views.setOnClickPendingIntent(R.id.widget_cover, open)
            views.setOnClickPendingIntent(R.id.widget_title, open)
            ids.forEach { manager.updateAppWidget(it, views) }
        }
    }

    private fun mediaButton(context: Context, keyCode: Int): PendingIntent {
        val intent = Intent(Intent.ACTION_MEDIA_BUTTON)
            .setComponent(ComponentName(context, MediaButtonReceiver::class.java))
            .putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
        return PendingIntent.getBroadcast(context, keyCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }
}
