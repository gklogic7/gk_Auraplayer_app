package com.example.pinning

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.example.MainActivity
import com.example.R
import com.example.data.TrackEntity
import com.example.widget.AuraCapsuleWidgetProvider

object HomePinningManager {

    const val ACTION_PLAY_PINNED_TRACK = "com.example.action.PLAY_PINNED_TRACK"
    const val EXTRA_TRACK_ID = "extra_pinned_track_id"

    /**
     * Requests the Android Launcher to pin a direct one-tap Track Shortcut onto the Home Screen.
     * Also pushes a dynamic shortcut so long-pressing the app icon lists pinned tracks.
     */
    fun pinTrackShortcutToHomeScreen(context: Context, track: TrackEntity): Boolean {
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_PLAY_PINNED_TRACK
            putExtra(EXTRA_TRACK_ID, track.id)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val iconRes = if (track.artworkResName == "cover_piche_tere") {
            R.drawable.cover_piche_tere
        } else {
            R.mipmap.ic_launcher_round
        }

        val shortcutInfo = ShortcutInfoCompat.Builder(context, "aura_track_${track.id}")
            .setShortLabel(track.title.take(18))
            .setLongLabel("${track.title} • ${track.artist}".take(42))
            .setIcon(IconCompat.createWithResource(context, iconRes))
            .setIntent(launchIntent)
            .build()

        // Always register in dynamic shortcuts for launcher quick-access
        try {
            ShortcutManagerCompat.pushDynamicShortcut(context, shortcutInfo)
        } catch (_: Exception) {
        }

        return try {
            if (ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
                ShortcutManagerCompat.requestPinShortcut(context, shortcutInfo, null)
            } else {
                true
            }
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Requests the Android Launcher to pin the resizable Aura Capsule AppWidget to the Home Screen.
     */
    fun requestPinCapsuleWidget(context: Context, currentTrack: TrackEntity?, isPlaying: Boolean): Boolean {
        AuraCapsuleWidgetProvider.updateAllWidgets(
            context = context,
            title = currentTrack?.title ?: "Piche Tere",
            artist = currentTrack?.artist ?: "Kunwarr, Dishant",
            isPlaying = isPlaying
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val provider = ComponentName(context, AuraCapsuleWidgetProvider::class.java)
            if (appWidgetManager.isRequestPinAppWidgetSupported) {
                return try {
                    appWidgetManager.requestPinAppWidget(provider, null, null)
                } catch (_: Exception) {
                    false
                }
            }
        }
        return false
    }
}
